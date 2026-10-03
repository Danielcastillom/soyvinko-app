"""Selected offerings, atomic reference photos, quotes and one assignee per customer need."""
from datetime import datetime, timedelta, timezone
from io import BytesIO
from uuid import uuid4
import pytest
from PIL import Image
from app.core.db import SPECS
from test_venezuela import invoke, address_values


@pytest.fixture
def market(client, actors):
    category = next(c['id'] for c in invoke(client, 'catalog') if c['slug'] == 'diseno-grafico')
    offerings = {}
    for role in ('professional', 'outsider'):
        user = actors[role]
        invoke(client, 'activate_professional', {'slug': 'need-' + uuid4().hex, 'business_name': 'Proveedor de prueba'}, user)
        offering = invoke(client, 'add_offering', {'category': category, 'title': 'Diseño de identidad visual', 'currency': 'USD', 'price_minor': 2500}, user)
        invoke(client, 'set_offering_delivery', {'offering': offering, 'mode': 'remote'}, user)
        offerings[role] = offering
    return {'category': category, 'offerings': offerings, **actors}


def direct_values(market, **extra):
    return {'offering': market['offerings']['professional'], 'description': 'Necesito corregir la imagen y los colores de mi negocio.',
            'currency': 'VES', 'address': None, 'key': str(uuid4()),
            'expires_at': (datetime.now(timezone.utc) + timedelta(days=5)).isoformat(), **extra}


def broadcast_values(market, **extra):
    return {k: v for k, v in {**direct_values(market), 'category': market['category'], 'delivery_mode': 'remote',
        'title': 'Necesito un diseño para mi negocio', 'budget': None, 'radius_m': None, **extra}.items() if k != 'offering'}


def publish(client, market, ident, owner=None):
    assert row(client, ident, owner or market['customer'])['status'] == 'open'
    invoke(client, 'publish_request', {'request': ident}, owner or market['customer'])


def quote(client, market, ident, role='professional', price=123450):
    return invoke(client, 'submit_proposal', {'request': ident, 'message': 'Puedo realizar el trabajo con los requisitos indicados.',
        'price_minor': price, 'expires_at': (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()}, market[role])


def row(client, ident, owner):
    return next(r for r in invoke(client, 'my_requests', user=owner) if r['id'] == ident)


def upload(client, user, name='referencia.png', purpose='request'):
    data = BytesIO()
    Image.new('RGB', (24, 24), '#608050').save(data, format='PNG')
    response = client.post('/api/v1/files/upload', headers=user['headers'], data={'purpose': purpose},
                           files={'file': (name, data.getvalue(), 'image/png')})
    assert response.status_code == 201, response.text
    return response.json()['data']['id']


def rejected(client, operation, payload, user, code, status=409):
    res = client.post(SPECS[operation]['path'], json=payload, headers=user['headers'])
    assert res.status_code == status, res.text
    assert res.json()['error']['code'] == code, res.text


def test_selected_service_is_derived_and_retries_preserve_context(client, market):
    values = direct_values(market)
    ident = invoke(client, 'request_offering', values, market['customer'])
    saved = row(client, ident, market['customer'])
    assert saved['title'] == 'Diseño de identidad visual'
    assert saved['category_id'] == market['category'] and saved['delivery_mode'] == 'remote'
    assert saved['offering_id'] == values['offering'] and saved['mode'] == 'direct' and saved['requires_quote']
    assert saved['currency'] == 'VES' and saved['budget_max_minor'] is None and saved['offered_price_minor'] is None
    assert saved['offering_snapshot']['currency'] == 'USD' and saved['offering_snapshot']['price_min_minor'] == 2500
    assert saved['search_radius_m'] is None and saved['requires_platform_payment']
    assert 'creation_payload' not in saved
    assert saved['status'] == 'open' and saved['moderation_status'] == 'approved'
    assert invoke(client, 'request_offering', values, market['customer']) == ident
    rejected(client, 'request_offering', {**values, 'description': 'He cambiado los requisitos de esta solicitud.'}, market['customer'], 'idempotency_conflict')
    # The client cannot override the selected category, title, price or modality.
    for key, value in [('title', 'Otro servicio'), ('category', market['category']), ('offered_price', 1), ('delivery_mode', 'onsite')]:
        res = client.post(SPECS['request_offering']['path'], json={**values, key: value}, headers=market['customer']['headers'])
        assert res.status_code == 422


def test_direct_quote_requires_customer_confirmation_and_currency_is_not_converted(client, market):
    ident = invoke(client, 'request_offering', direct_values(market), market['customer'])
    publish(client, market, ident)
    rejected(client, 'accept_direct_request', {'request': ident}, market['professional'], 'customer_quote_confirmation_required')
    rejected(client, 'submit_proposal', {'request': ident, 'message': 'Propuesta no autorizada', 'price_minor': 500,
        'expires_at': (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()}, market['outsider'], 'request_unavailable')
    proposal = quote(client, market, ident)
    assert row(client, ident, market['customer'])['status'] == 'open'
    assert invoke(client, 'my_contracts', user=market['professional']) == []
    rejected(client, 'accept_proposal', {'proposal': proposal}, market['outsider'], 'request_not_owned', 403)
    contract = invoke(client, 'accept_proposal', {'proposal': proposal}, market['customer'])
    assert invoke(client, 'accept_proposal', {'proposal': proposal}, market['customer']) == contract
    c = invoke(client, 'my_contracts', user=market['professional'])[0]
    assert c['agreed_price_minor'] == 123450 and c['currency'] == 'VES' and c['payment_protection'] == 'guaranteed'
    assert row(client, ident, market['customer'])['status'] == 'matched'
    assert row(client, ident, market['customer'])['contract_id'] == contract
    assert invoke(client, 'my_wallet', user=market['professional'])['balance'] == 2
    profile = invoke(client, 'professional_profile', {'professional': market['professional']['id']}, market['customer'])
    assert any(o['id'] == market['offerings']['professional'] for o in profile['offerings'])


def test_broadcast_has_multiple_proposals_one_assignee_and_professional_can_take_more_work(client, market):
    ident = invoke(client, 'create_customer_request', broadcast_values(market), market['customer'])
    publish(client, market, ident)
    first = quote(client, market, ident)
    other = quote(client, market, ident, 'outsider', 150000)
    proposals = invoke(client, 'request_proposals', {'request': ident}, market['customer'])
    assert len(proposals) == 2 and all(p['status'] == 'pending' for p in proposals)
    contract = invoke(client, 'accept_proposal', {'proposal': first}, market['customer'])
    rejected(client, 'accept_proposal', {'proposal': other}, market['customer'], 'request_already_assigned')
    for role in ('professional', 'outsider'):
        assert ident not in [r['request_id'] for r in invoke(client, 'service_feed', {'delivery_mode': 'remote'}, market[role])]
        rejected(client, 'submit_proposal', {'request': ident, 'message': 'Propuesta tardía', 'price_minor': 200,
            'expires_at': (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()}, market[role], 'request_unavailable')
    proposals = invoke(client, 'request_proposals', {'request': ident}, market['customer'])
    assert {p['id']: p['status'] for p in proposals} == {first: 'accepted', other: 'rejected'}
    # A different customer can assign a second concurrent job to the same professional.
    second = invoke(client, 'request_offering', direct_values(market, currency='USD'), market['admin'])
    publish(client, market, second, owner=market['admin'])
    second_quote = quote(client, market, second, price=3500)
    second_contract = invoke(client, 'accept_proposal', {'proposal': second_quote}, market['admin'])
    assert second_contract != contract
    assert len(invoke(client, 'my_contracts', user=market['professional'])) == 2
    assert invoke(client, 'my_wallet', user=market['professional'])['balance'] == 1
    assert invoke(client, 'my_professional', user=market['professional'])['offerings'][0]['active']


def test_reference_photos_are_atomic_private_and_follow_request_access(client, market):
    photo = upload(client, market['customer'])
    foreign = upload(client, market['outsider'])
    assert client.get(f'/api/v1/files/{photo}', headers=market['professional']['headers']).status_code == 403
    before = len(invoke(client, 'my_requests', user=market['customer']))
    rejected(client, 'request_offering', direct_values(market, files=[photo, foreign]), market['customer'], 'invalid_request_images')
    assert len(invoke(client, 'my_requests', user=market['customer'])) == before
    values = direct_values(market, files=[photo])
    ident = invoke(client, 'request_offering', values, market['customer'])
    assert invoke(client, 'request_offering', values, market['customer']) == ident
    saved = row(client, ident, market['customer'])
    assert saved['status'] == 'open' and saved['moderation_status'] == 'approved'
    assert len(saved['attachments']) == 1 and saved['attachments'][0]['moderation_status'] == 'approved'
    assert client.get(f'/api/v1/files/{photo}', headers=market['customer']['headers']).status_code == 200
    invoke(client, 'publish_request', {'request': ident}, market['customer'])
    feed = invoke(client, 'service_feed', {'delivery_mode': 'remote'}, market['professional'])
    assert next(r for r in feed if r['request_id'] == ident)['attachments'][0]['file_id'] == photo
    assert client.get(f'/api/v1/files/{photo}', headers=market['professional']['headers']).status_code == 200
    assert client.get(f'/api/v1/files/{photo}', headers=market['outsider']['headers']).status_code == 403
    proposal = quote(client, market, ident)
    contract = invoke(client, 'accept_proposal', {'proposal': proposal}, market['customer'])
    assert invoke(client, 'contract_detail', {'contract': contract}, market['professional'])['reference_photos'][0]['file_id'] == photo
    response = client.get(SPECS['contract_detail']['path'], params={'contract': contract}, headers=market['outsider']['headers'])
    assert response.status_code == 403
    assert client.get(f'/api/v1/files/{photo}', headers=market['professional']['headers']).status_code == 200
    assert client.get(f'/api/v1/files/{photo}', headers=market['outsider']['headers']).status_code == 403
    rejected(client, 'remove_request_image', {'request': ident, 'file': photo}, market['customer'], 'request_not_editable')


def test_photo_removal_and_validation_for_customer_publication(client, market):
    photos = [upload(client, market['customer'], name=f'referencia-{i}.png') for i in range(2)]
    # The legacy editor still supports drafts; the current form submits all photos atomically.
    ident = invoke(client, 'create_remote_request', {
        'category': market['category'], 'mode': 'broadcast', 'target': None, 'title': 'Borrador de prueba',
        'requirements': 'Referencia para un diseño de prueba', 'currency': 'USD', 'offered_price': 2500,
        'expires_at': direct_values(market)['expires_at'], 'key': str(uuid4()),
    }, market['customer'])
    for photo in photos:
        invoke(client, 'attach_request_file', {'request': ident, 'file': photo}, market['customer'])
    assert len(row(client, ident, market['customer'])['attachments']) == 2
    rejected(client, 'remove_request_image', {'request': ident, 'file': photos[0]}, market['outsider'], 'request_not_editable')
    invoke(client, 'remove_request_image', {'request': ident, 'file': photos[0]}, market['customer'])
    assert [f['file_id'] for f in row(client, ident, market['customer'])['attachments']] == [photos[1]]
    rejected(client, 'create_customer_request', broadcast_values(market, files=[photos[0], photos[0]]), market['customer'], 'invalid_request_images')
    rejected(client, 'create_customer_request', broadcast_values(market, files=[str(uuid4()) for _ in range(7)]), market['customer'], 'invalid_request_images')
    wrong_purpose = upload(client, market['customer'], purpose='portfolio')
    rejected(client, 'create_customer_request', broadcast_values(market, files=[wrong_purpose]), market['customer'], 'invalid_request_images')
    invoke(client, 'moderate_request', {'request': ident, 'approve': False, 'reason': 'Retirada administrativa de prueba.'}, market['admin'])
    invoke(client, 'remove_request_image', {'request': ident, 'file': photos[1]}, market['customer'])
    invoke(client, 'attach_request_file', {'request': ident, 'file': photos[0]}, market['customer'])
    assert row(client, ident, market['customer'])['moderation_status'] == 'rejected'
    rejected(client, 'publish_request', {'request': ident}, market['customer'], 'request_not_publishable')


def test_administrator_can_revoke_and_restore_without_customer_retry_bypassing_it(client, market, database):
    import db
    photo = upload(client, market['customer'])
    values = direct_values(market, files=[photo])
    ident = invoke(client, 'request_offering', values, market['customer'])
    proposal = quote(client, market, ident)
    invoke(client, 'moderate_request', {'request': ident, 'approve': False, 'reason': 'Publicación desautorizada en prueba.'}, market['admin'])
    assert ident not in [r['request_id'] for r in invoke(client, 'service_feed', {'delivery_mode': 'remote'}, market['professional'])]
    assert client.get(f'/api/v1/files/{photo}', headers=market['professional']['headers']).status_code == 403
    assert invoke(client, 'request_offering', values, market['customer']) == ident
    assert row(client, ident, market['customer'])['moderation_status'] == 'rejected'
    rejected(client, 'publish_request', {'request': ident}, market['customer'], 'request_not_publishable')
    rejected(client, 'accept_proposal', {'proposal': proposal}, market['customer'], 'request_unavailable')
    rejected(client, 'moderate_request', {'request': ident, 'approve': True, 'reason': 'No puede autorizarse a sí mismo.'}, market['customer'], 'http_403', 403)
    invoke(client, 'moderate_request', {'request': ident, 'approve': True, 'reason': 'Restauración administrativa de prueba.'}, market['admin'])
    assert ident in [r['request_id'] for r in invoke(client, 'service_feed', {'delivery_mode': 'remote'}, market['professional'])]
    assert client.get(f'/api/v1/files/{photo}', headers=market['professional']['headers']).status_code == 200
    assert invoke(client, 'request_offering', values, market['customer']) == ident
    count = db.sql(f"SELECT count(*) FROM app.outbox WHERE topic='request.available' AND aggregate_id='{ident}'", database=database, capture=True).stdout.strip()
    assert count == '1'
    invoke(client, 'cancel_request', {'request': ident, 'reason': 'Cierre de solicitud de prueba.'}, market['customer'])
    assert invoke(client, 'request_offering', values, market['customer']) == ident
    assert row(client, ident, market['customer'])['status'] == 'cancelled'


def test_rejected_or_unsafe_photos_cannot_be_automatically_approved(client, market, database):
    import db
    photo = upload(client, market['customer'])
    invoke(client, 'moderate_file', {'file': photo, 'approve': False, 'reason': 'Archivo desautorizado de prueba.'}, market['admin'])
    rejected(client, 'request_offering', direct_values(market, files=[photo]), market['customer'], 'invalid_request_images')
    unsafe = upload(client, market['customer'])
    db.sql(f"UPDATE app.files SET bytes=10485761 WHERE id='{unsafe}'", database=database, capture=True)
    before = len(invoke(client, 'my_requests', user=market['customer']))
    rejected(client, 'request_offering', direct_values(market, files=[unsafe]), market['customer'], 'unsafe_attachment')
    assert len(invoke(client, 'my_requests', user=market['customer'])) == before
    for purpose in ('kyc', 'portfolio'):
        private = upload(client, market['customer'], purpose=purpose)
        state = db.sql(f"SELECT moderation_status FROM app.files WHERE id='{private}'", database=database, capture=True).stdout.strip()
        # KYC siempre se modera; el portafolio se aprueba al subir (040), pero sigue privado hasta vincularse.
        assert state == ('pending' if purpose == 'kyc' else 'approved')
        assert client.get(f'/api/v1/files/{private}', headers=market['professional']['headers']).status_code == 403


def test_direct_onsite_keeps_offering_and_checks_real_coverage_without_customer_radius(client, market):
    pro = market['professional']
    offering = market['offerings']['professional']
    invoke(client, 'set_offering_delivery', {'offering': offering, 'mode': 'onsite'}, pro)
    values = address_values(client)
    branch = invoke(client, 'add_branch_ve', {**{k: v for k, v in values.items() if k != 'label'}, 'name': 'Sede de prueba'}, pro)
    invoke(client, 'set_coverage', {'branch': branch, 'category': market['category'], 'radius_m': 25000}, pro)
    address = invoke(client, 'add_address_ve', values, market['customer'])
    ident = invoke(client, 'request_offering', direct_values(market, address=address), market['customer'])
    saved = row(client, ident, market['customer'])
    assert saved['delivery_mode'] == 'onsite' and saved['offering_id'] == offering
    rejected(client, 'request_offering', direct_values(market, address=address), market['outsider'], 'address_not_owned', 403)
    invoke(client, 'set_offering_delivery', {'offering': offering, 'mode': 'remote'}, pro)
    rejected(client, 'request_offering', direct_values(market, address=address), market['customer'], 'remote_location_not_allowed')


def test_inactive_selected_offering_cannot_be_replaced_by_another_service(client, market, database):
    import db
    db.sql(f"UPDATE app.offerings SET active=false WHERE id='{market['offerings']['professional']}'", database=database, capture=True)
    rejected(client, 'request_offering', direct_values(market), market['customer'], 'offering_unavailable')
