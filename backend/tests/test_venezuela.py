"""Country hierarchy, private address storage, geographic matching and two independent currencies."""
import json
from uuid import uuid4
import pytest

from app.core.db import SPECS, call


def invoke(client, name, values=None, user=None, status=200):
    spec = SPECS[name]
    kwargs = {"headers": user["headers"]} if user else {}
    kwargs["params" if spec["method"] == "GET" else "json"] = values or {}
    response = client.request(spec["method"], spec["path"], **kwargs)
    assert response.status_code == status, response.text
    return response.json().get("data")


def address_values(client):
    parish = invoke(client, "geo_parish", {"parish": "VE010109"})
    return {"label": "Casa de prueba", "country": "VE", "state": "VE01", "municipality": "VE0101",
            "parish": parish["code"], "line": "Dirección privada de prueba", "lon": parish["longitude"], "lat": parish["latitude"]}


def test_complete_pinned_catalog_and_relations(client, database):
    import db
    config = client.get("/api/v1/config").json()["data"]
    assert config["countries"] == [{"code": "VE", "name": "Venezuela", "active": True}]
    assert {c["code"] for c in config["currencies"]} == {"VES", "USD"}
    states = invoke(client, "geo_states")
    assert len(states) == 25
    assert sum(s["kind"] == "state" for s in states) == 23
    municipalities, parishes = [], []
    for state in states:
        rows = invoke(client, "geo_municipalities", {"state": state["code"]})
        assert rows and all(m["state_code"] == state["code"] for m in rows)
        municipalities.extend(rows)
    assert len(municipalities) == 336
    assert sum(m["kind"] == "municipality" for m in municipalities) == 335
    # The entire hierarchy is verified without 336 repetitive HTTP round trips.
    counts = db.sql("SELECT count(*),count(DISTINCT municipality_code) FROM app.geo_parishes",database=database,capture=True).stdout.strip()
    assert counts == "1135|336"
    rows = invoke(client, "geo_parishes", {"municipality": "VE0101"})
    assert len(rows) == 22 and all(p["municipality_code"] == "VE0101" for p in rows)
    assert all("boundary" not in p for p in rows)
    assert invoke(client, "geo_municipalities", {"state": "CO"}) == []
    assert invoke(client, "geo_parishes", {"municipality": "VE010109"}) == []


def test_location_resolution_uses_post_and_does_not_persist_browser_position(client, database):
    import db
    v = address_values(client)
    before = db.sql("SELECT count(*) FROM app.addresses",database=database,capture=True).stdout
    result = invoke(client, "locate_parish", {"lon": v["lon"], "lat": v["lat"]})
    assert result["parish_code"] == "VE010109" and result["state_code"] == "VE01"
    assert invoke(client, "locate_parish", {"lon": -74.06, "lat": 4.65}) is None
    assert client.get(SPECS["locate_parish"]["path"],params={"lat":v['lat'],"lon":v['lon']}).status_code == 405
    assert before == db.sql("SELECT count(*) FROM app.addresses",database=database,capture=True).stdout


@pytest.mark.parametrize("override,code", [
    ({"country":"CO"},"venezuela_only"),
    ({"state":"VE08"},"invalid_address_hierarchy"),
    ({"municipality":"VE0814"},"invalid_address_hierarchy"),
    ({"parish":"VE081407"},"invalid_address_hierarchy"),
    ({"lat":4.65,"lon":-74.06},"point_outside_parish"),
    ({"label":" "},"invalid_address_details"),
])
def test_rejects_wrong_country_hierarchy_and_map_point(client, actors, override, code):
    user = actors["customer"]
    response = client.post(SPECS["add_address_ve"]["path"],json={**address_values(client),**override},headers=user["headers"])
    assert response.status_code == 409
    assert response.json()["error"]["code"] == code
    assert not invoke(client,"my_account",user=user)["addresses"]


def test_named_address_is_saved_exactly_and_is_private(client, actors):
    user=actors["customer"]
    values=address_values(client)
    ident=invoke(client,"add_address_ve",{**values,"label":"  Mi trabajo  ","instructions":"Entrada de prueba"},user)
    rows=invoke(client,"my_account",user=user)["addresses"]
    saved=next(a for a in rows if a["id"]==ident)
    assert saved["label"] == "Mi trabajo"
    assert saved["parish_name"] == "El Recreo" and saved["state_name"] == "Distrito Capital"
    assert saved["latitude"] == values["lat"] and saved["longitude"] == values["lon"]
    assert not invoke(client,"my_account",user=actors["outsider"])["addresses"]
    assert client.post(SPECS["add_address_ve"]["path"],json=values).status_code==401


def test_browser_nearby_search_respects_coverage_and_does_not_expose_private_address(client,actors):
    pro=actors['professional']
    category=next(c['id'] for c in invoke(client,'catalog') if c['slug']=='carpinteria')
    invoke(client,'activate_professional',{'slug':'geo-'+uuid4().hex,'business_name':'Profesional de prueba'},pro)
    invoke(client,'add_offering',{'category':category,'title':'Servicio cercano','currency':'USD','price_minor':2500},pro)
    values=address_values(client)
    branch=invoke(client,'add_branch_ve',{**{k:v for k,v in values.items() if k!='label'},'name':'Sede privada de prueba'},pro)
    invoke(client,'set_coverage',{'branch':branch,'category':category,'radius_m':5000},pro)
    nearby=invoke(client,'nearby_professionals',{'lat':values['lat'],'lon':values['lon'],'category':category})
    assert [p['id'] for p in nearby] == [pro['id']]
    assert nearby[0]['distance_m']==0 and nearby[0]['offerings'][0]['currency']=='USD'
    for private in ('Dirección privada de prueba','latitude','longitude','state_code','parish_code','Sede privada'):
        assert private not in json.dumps(nearby)
    assert invoke(client,'nearby_professionals',{'lat':values['lat'],'lon':values['lon'],'category':category,'verified_only':True}) == []
    far=invoke(client,'geo_parish',{'parish':'VE081407'})
    assert invoke(client,'nearby_professionals',{'lat':far['latitude'],'lon':far['longitude'],'category':category}) == []
    invoke(client,'update_professional',{'name':'Profesional de prueba','bio':'Prueba','years':1,'accepts':False},pro)
    assert invoke(client,'nearby_professionals',{'lat':values['lat'],'lon':values['lon'],'category':category}) == []


def test_only_bs_and_usd_for_new_operations(client,actors):
    user=actors['customer']
    for currency in ('VES','USD'):
        result=invoke(client,'money_wallet',{'currency':currency},user)
        assert result['currency']==currency and result['available_minor']==0
    for currency in ('COP','EUR'):
        invoke(client,'money_wallet',{'currency':currency},user,status=409)
        invoke(client,'create_topup',{'currency':currency,'amount':10000,'key':str(uuid4())},user,status=422)
        response=client.post('/api/v1/integrations/payout-methods',json={'label':'Prueba','currency':currency},headers=user['headers'])
        assert response.status_code == 422


def test_legacy_wallet_remains_in_its_original_currency(client,actors,database):
    import db
    user=actors['customer']
    ident=db.sql(f"INSERT INTO app.money_wallets(user_id,currency) VALUES('{user['id']}','COP') RETURNING id",database=database,capture=True).stdout.strip()
    wallet=invoke(client,'money_wallet',{'currency':'COP'},user)
    assert wallet['currency']=='COP'
    assert db.sql(f"SELECT currency FROM app.money_wallets WHERE id='{ident}'",database=database,capture=True).stdout.strip()=='COP'
    invoke(client,'create_topup',{'currency':'COP','amount':10000,'key':str(uuid4())},user,status=422)
