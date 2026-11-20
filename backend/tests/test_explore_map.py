"""Map markers preserve branch privacy and the current search scope."""
import json
from uuid import uuid4

import pytest

from test_venezuela import address_values, invoke


def test_markers_use_parish_for_private_branches_and_exact_public_points(client, actors, database):
    import db

    pro = actors["professional"]
    category = next(c["id"] for c in invoke(client, "catalog") if c["slug"] == "carpinteria")
    invoke(client, "activate_professional", {"slug": "map-" + uuid4().hex, "business_name": "Mapa prueba"}, pro)
    invoke(client, "add_offering", {"category": category, "title": "Servicio de mapa", "currency": "USD", "price_minor": 2500}, pro)
    reference = address_values(client)
    values = {**reference, "lat": reference["lat"] + 0.0002}
    branch = invoke(client, "add_branch_ve", {
        **{k: v for k, v in values.items() if k != "label"}, "name": "Nombre privado de sede",
    }, pro)
    invoke(client, "set_coverage", {"branch": branch, "category": category, "radius_m": 5000}, pro)
    query = {"professionals": [pro["id"]], "category": category, "lat": values["lat"], "lon": values["lon"]}
    markers = invoke(client, "professional_map_markers", query)
    assert len(markers) == 1
    assert markers[0]["professional_id"] == pro["id"]
    assert markers[0]["approximate"] is True
    assert (markers[0]["lat"], markers[0]["lon"]) == (reference["lat"], reference["lon"])
    assert markers[0]["lat"] != values["lat"]
    assert "Dirección privada" not in json.dumps(markers)
    assert "Nombre privado" not in json.dumps(markers)
    assert invoke(client, "professional_map_markers", {"professionals": []}) == []
    assert invoke(client, "professional_map_markers", {**query, "professionals": [actors["customer"]["id"]]}) == []
    assert invoke(client, "professional_map_markers", {**query, "category": str(uuid4())}) == []
    far = invoke(client, "geo_parish", {"parish": "VE231314"})
    assert invoke(client, "professional_map_markers", {**query, "lat": far["latitude"], "lon": far["longitude"]}) == []

    db.sql(f"UPDATE app.branches SET public_address=true WHERE id='{branch}'", database=database, capture=True)
    public = invoke(client, "professional_map_markers", query)
    assert public[0]["approximate"] is False
    assert (public[0]["lat"], public[0]["lon"]) == (values["lat"], values["lon"])
    assert public[0]["label"] == values["line"]

    db.sql(f"UPDATE app.branches SET active=false WHERE id='{branch}'", database=database, capture=True)
    assert invoke(client, "professional_map_markers", query) == []
    db.sql(f"UPDATE app.branches SET active=true, public_address=false, state_code=NULL, municipality_code=NULL, parish_code=NULL WHERE id='{branch}'", database=database, capture=True)
    assert invoke(client, "professional_map_markers", query) == []
    db.sql(f"UPDATE app.branches SET active=false WHERE id='{branch}'", database=database, capture=True)


def test_remote_only_profiles_have_no_map_marker(client, actors):
    pro = actors["professional"]
    category = next(c["id"] for c in invoke(client, "catalog") if c["slug"] == "programacion")
    invoke(client, "activate_professional", {"slug": "remote-map-" + uuid4().hex, "business_name": "Remoto prueba"}, pro)
    offering = invoke(client, "add_offering", {"category": category, "title": "Servicio remoto", "currency": "USD", "price_minor": 2500}, pro)
    invoke(client, "set_offering_delivery", {"offering": offering, "mode": "remote"}, pro)
    values = address_values(client)
    branch = invoke(client, "add_branch_ve", {**{k: v for k, v in values.items() if k != "label"}, "name": "Oficina pública", "public_address": True}, pro)
    invoke(client, "set_coverage", {"branch": branch, "category": category, "radius_m": 5000}, pro)
    assert invoke(client, "professional_map_markers", {"professionals": [pro["id"]]}) == []


@pytest.mark.parametrize("query", [
    {"professionals": None},
    {"professionals": [str(uuid4())] * 101},
    {"professionals": [], "lat": 10},
    {"professionals": [], "radius_m": 0},
])
def test_map_rejects_invalid_filters(client, query):
    invoke(client, "professional_map_markers", query, status=409)
