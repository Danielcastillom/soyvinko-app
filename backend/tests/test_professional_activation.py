"""Normalize profile identifiers without weakening uniqueness or welcome-credit rules."""
from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4
import pytest
from app.core.db import SPECS
from test_venezuela import invoke


@pytest.mark.parametrize('raw,expected', [
    ('Develope Academy','develope-academy'),
    ('  Ñandú & Café 2026  ','nandu-cafe-2026'),
    ('Disen\u0303o---Profesional','diseno-profesional'),
])
def test_profile_slug_normalizes_and_activation_is_idempotent(client, actors, raw, expected):
    user=actors['customer']
    suffix=uuid4().hex[:8]
    body={'slug':raw+' '+suffix,'business_name':'Negocio de prueba','bio':'Desarrollo de servicios de ejemplo.'}
    for _ in range(2):
        assert invoke(client,'activate_professional',body,user)==user['id']
    saved=invoke(client,'my_professional',user=user)
    assert saved['slug']==expected+'-'+suffix
    assert invoke(client,'my_wallet',user=user)['balance']==3


@pytest.mark.parametrize('slug',['!!!','  ',None,'a'*81])
def test_invalid_profile_name_explains_field_and_does_not_activate(client, actors, slug):
    user=actors['customer']
    res=client.post(SPECS['activate_professional']['path'],json={'slug':slug,'business_name':'Prueba'},headers=user['headers'])
    assert res.status_code==422,res.text
    error=res.json()['error']
    assert error['details'][0]['field']=='body.slug'
    assert not invoke(client,'my_professional',user=user)
    assert 'professional' not in invoke(client,'my_account',user=user)['roles']


def test_normalized_name_collision_is_clear_and_does_not_award_credits(client, actors):
    body={'slug':'Academia '+uuid4().hex,'business_name':'Prueba'}
    invoke(client,'activate_professional',body,actors['customer'])
    res=client.post(SPECS['activate_professional']['path'],json={**body,'slug':body['slug'].upper()},headers=actors['outsider']['headers'])
    assert res.status_code==409,res.text
    assert res.json()['error']['code']=='professional_slug_taken'
    assert res.json()['error']['details'][0]['field']=='body.slug'
    assert 'ocupado' in res.json()['error']['message']
    assert not invoke(client,'my_professional',user=actors['outsider'])


def test_two_simultaneous_activations_keep_one_welcome_credit_grant(client, actors):
    user=actors['customer']
    body={'slug':'Concurrente '+uuid4().hex,'business_name':'Prueba concurrente'}
    with ThreadPoolExecutor(max_workers=2) as pool:
        responses=list(pool.map(lambda _: client.post(SPECS['activate_professional']['path'],json=body,headers=user['headers']),range(2)))
    assert all(r.status_code==200 for r in responses),[r.text for r in responses]
    assert {r.json()['data'] for r in responses}=={user['id']}
    assert invoke(client,'my_wallet',user=user)['balance']==3
