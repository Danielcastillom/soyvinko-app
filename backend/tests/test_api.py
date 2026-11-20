import hashlib
import hmac
import io
import json
import time
from uuid import uuid4
from datetime import datetime, timedelta, timezone
from pathlib import Path
import pytest
import pyotp
from PIL import Image
from app.core.db import call, pools, SPECS
from app.core.settings import settings
from app.worker import tick


def invoke(client, user, name, params=None, expected=200):
    op = SPECS[name]
    kwargs = {"headers": user["headers"]}
    kwargs["params" if op["method"] == "GET" else "json"] = params or {}
    r = client.request(op["method"], op["path"], **kwargs)
    assert r.status_code == expected, (name, r.status_code, r.text)
    return r.json().get("data")


def test_health_public_and_private_routes(client):
    assert client.get("/api/health").json()["database"] == "connected"
    assert len(client.get("/api/v1/public/catalog").json()["data"]) >= 10
    assert client.get("/api/v1/account/my-account").status_code == 401
    assert client.get("/api/v1/public/discover?limit=10000").status_code == 422
    assert client.get("/api/v1/public/discover?function=auth_account").status_code == 422
    paths = client.get("/api/openapi.json").json()["paths"]
    for spec in SPECS.values():
        assert (spec["path"] in paths) == (spec["module"] is not None)
    for role, pool in pools.items():
        with pool.connection() as conn:
            assert (
                conn.execute("SELECT rolsuper FROM pg_roles WHERE rolname=current_user").fetchone()[
                    "rolsuper"
                ]
                is False
            )
            with pytest.raises(Exception):
                with conn.transaction():
                    conn.execute("SELECT * FROM app.credentials")


def test_registration_web_csrf_rotation_reuse_logout(client, database):
    import db

    db.sql("DELETE FROM app.http_rate_limits", database=database, capture=True)
    email = f"{uuid4().hex}@example.com"
    response = client.post(
        "/api/v1/auth/register",
        json={"email": email, "name": "Nuevo Cliente", "password": "Test-passphrase-long!", "client": "web"},
    )
    assert response.status_code == 201, response.text
    first = response.json()["data"]
    assert "refresh_token" not in first
    assert "HttpOnly" in response.headers.get("set-cookie", "")
    old = client.cookies.get("chamba_refresh")
    assert client.post("/api/v1/auth/refresh", json={"client": "web"}).status_code == 403
    rotated = client.post(
        "/api/v1/auth/refresh",
        json={"client": "web"},
        headers={"X-CSRF-Token": client.cookies.get("chamba_csrf")},
    )
    assert rotated.status_code == 200
    assert (
        client.get(
            "/api/v1/auth/me", headers={"Authorization": "Bearer " + first["access_token"]}
        ).status_code
        == 401
    )
    second = rotated.json()["data"]
    assert (
        client.get(
            "/api/v1/auth/me", headers={"Authorization": "Bearer " + second["access_token"]}
        ).status_code
        == 200
    )
    reused = client.post("/api/v1/auth/refresh", json={"client": "native", "refresh_token": old})
    assert reused.status_code == 401
    assert (
        client.get(
            "/api/v1/auth/me", headers={"Authorization": "Bearer " + second["access_token"]}
        ).status_code
        == 401
    )
    login = client.post(
        "/api/v1/auth/login", json={"email": email, "password": "Test-passphrase-long!", "client": "web"}
    )
    assert login.status_code == 200
    assert (
        client.post(
            "/api/v1/auth/logout",
            json={"client": "web"},
            headers={"X-CSRF-Token": client.cookies.get("chamba_csrf")},
        ).status_code
        == 200
    )
    assert (
        client.get(
            "/api/v1/auth/me", headers={"Authorization": "Bearer " + login.json()["data"]["access_token"]}
        ).status_code
        == 401
    )


def test_admin_roles_suspension_and_safe_views(client, actors):
    customer, admin = actors["customer"], actors["admin"]
    assert (
        client.post(
            "/api/v1/admin/assign-role",
            json={
                "user": customer["id"],
                "role": "superadmin",
                "enable": True,
                "reason": "Escalamiento no permitido",
            },
            headers=customer["headers"],
        ).status_code
        == 403
    )
    for resource in [
        "users",
        "requests",
        "reviews",
        "files",
        "kyc",
        "plans",
        "fees",
        "tickets",
        "withdrawals",
        "audit",
    ]:
        invoke(client, admin, "admin_list", {"resource": resource})
    result = invoke(client, customer, "my_account")
    assert "password_hash" not in json.dumps(result)
    invoke(
        client,
        admin,
        "set_user_status",
        {
            "user": customer["id"],
            "status": "suspended",
            "reason": "Suspensión para prueba de sesión revocada",
        },
    )
    assert client.get("/api/v1/account/my-account", headers=customer["headers"]).status_code == 401


def test_remote_lifecycle_credits_wallet_release_withdrawal(client, actors):
    customer, professional, admin, outsider = (
        actors[x] for x in ["customer", "professional", "admin", "outsider"]
    )
    cats = client.get("/api/v1/public/catalog").json()["data"]
    category = next(c["id"] for c in cats if c["slug"] == "diseno-grafico")
    invoke(
        client,
        professional,
        "activate_professional",
        {"slug": "pro-" + uuid4().hex, "business_name": "Estudio de pruebas", "bio": "Diseño profesional"},
    )
    invoke(
        client,
        professional,
        "activate_professional",
        {"slug": "pro-" + uuid4().hex, "business_name": "Estudio de pruebas", "bio": "Diseño profesional"},
    )
    assert invoke(client, professional, "my_wallet")["balance"] == 3
    offering = invoke(
        client,
        professional,
        "add_offering",
        {"category": category, "title": "Diseño remoto", "currency": "VES", "price_minor": 30000},
    )
    invoke(client, professional, "set_offering_delivery", {"offering": offering, "mode": "remote"})
    r = invoke(
        client,
        customer,
        "create_remote_request",
        {
            "category": category,
            "mode": "direct",
            "target": professional["id"],
            "title": "Diseñar un logotipo",
            "requirements": "Entregar tres variantes y archivos finales.",
            "currency": "VES",
            "offered_price": 30000,
            "expires_at": (datetime.now(timezone.utc) + timedelta(days=7)).isoformat(),
            "key": str(uuid4()),
        },
    )
    invoke(
        client,
        admin,
        "moderate_request",
        {"request": r, "approve": True, "reason": "Contenido revisado sin datos personales."},
    )
    invoke(client, customer, "publish_request", {"request": r})
    feed = invoke(client, professional, "service_feed", {"delivery_mode": "remote"})
    assert customer["id"] not in json.dumps(feed)
    contract = invoke(client, professional, "accept_direct_request", {"request": r})
    assert invoke(client, professional, "my_wallet")["balance"] == 2
    invoke(client, outsider, "contract_detail", {"contract": contract}, expected=403)
    invoke(client, professional, "advance_contract", {"contract": contract, "action": "start"}, expected=409)
    t = invoke(client, customer, "create_topup", {"currency": "VES", "amount": 100000, "key": str(uuid4())})
    assert invoke(client, customer, "money_wallet", {"currency": "VES"})["available_minor"] == 0
    assert (
        client.post(f"/api/v1/integrations/sandbox/topup/{t}/settle", headers=outsider["headers"]).status_code
        == 404
    )
    for _ in range(2):
        settled = client.post(f"/api/v1/integrations/sandbox/topup/{t}/settle", headers=customer["headers"])
        assert settled.status_code == 200, settled.text
    assert invoke(client, customer, "money_wallet", {"currency": "VES"})["available_minor"] == 100000
    quote = invoke(client, customer, "quote_contract_funding", {"contract": contract})
    invoke(client, customer, "fund_contract", {"quote": quote["id"], "key": str(uuid4())})
    assert invoke(client, professional, "escrow_details", {"contract": contract})["funds_guaranteed"] is True
    key = str(uuid4())
    for _ in range(2):
        invoke(
            client,
            customer,
            "send_message",
            {"contract": contract, "body": "Empecemos el proyecto", "key": key},
        )
    assert (
        len(
            [
                m
                for m in invoke(client, professional, "chat_messages", {"contract": contract})
                if m["body"] == "Empecemos el proyecto"
            ]
        )
        == 1
    )
    invoke(client, professional, "advance_contract", {"contract": contract, "action": "start"})
    delivery = invoke(
        client,
        professional,
        "submit_delivery",
        {
            "contract": contract,
            "summary": "Entrego las tres variantes acordadas.",
            "key": str(uuid4()),
            "files": [],
        },
    )
    invoke(
        client,
        professional,
        "release_contract_funds",
        {"contract": contract, "delivery": delivery, "key": str(uuid4())},
        expected=403,
    )
    invoke(
        client,
        customer,
        "request_delivery_changes",
        {"delivery": delivery, "reason": "Ajustar colores según los requisitos."},
    )
    delivery = invoke(
        client,
        professional,
        "submit_delivery",
        {"contract": contract, "summary": "Variantes corregidas y listas.", "key": str(uuid4()), "files": []},
    )
    release_key = str(uuid4())
    for _ in range(2):
        invoke(
            client,
            customer,
            "release_contract_funds",
            {"contract": contract, "delivery": delivery, "key": release_key},
        )
    assert invoke(client, professional, "money_wallet", {"currency": "VES"})["available_minor"] == 30000
    assert invoke(client, customer, "money_wallet", {"currency": "VES"})["available_minor"] == 70000
    kyc = str(call("submit_kyc", {"provider": "manual", "external_reference": None}, actor=professional["id"]))
    invoke(
        client,
        admin,
        "review_kyc",
        {
            "case": kyc,
            "status": "approved",
            "reason": "Prueba de identidad ficticia en base desechable.",
            "expires_at": (datetime.now(timezone.utc) + timedelta(days=30)).isoformat(),
        },
    )
    method = client.post(
        "/api/v1/integrations/payout-methods",
        json={"label": "Cuenta ficticia", "currency": "VES"},
        headers=professional["headers"],
    ).json()["data"]
    invoke(
        client,
        admin,
        "configure_withdrawal_fee",
        {"currency": "VES", "basis_points": 200, "reason": "Dos por ciento solo al retiro, prueba."},
    )
    quote = invoke(
        client, professional, "quote_withdrawal", {"currency": "VES", "method": method, "amount": 10000}
    )
    assert quote["fee_minor"] == 200 and quote["net_minor"] == 9800
    w = invoke(client, professional, "request_withdrawal", {"quote": quote["id"], "key": str(uuid4())})
    assert (
        client.post(
            f"/api/v1/integrations/sandbox/withdrawals/{w}/settle", headers=professional["headers"]
        ).status_code
        == 403
    )
    res = client.post(f"/api/v1/integrations/sandbox/withdrawals/{w}/settle", headers=admin["headers"])
    assert res.status_code == 200, res.text
    assert invoke(client, professional, "money_wallet", {"currency": "VES"})["available_minor"] == 20000
    assert invoke(client, professional, "my_withdrawals")[0]["status"] == "paid"
    invoke(
        client,
        customer,
        "rate_contract",
        {"contract": contract, "rating": 5, "recommends": True, "comment": "Buen trabajo de prueba."},
    )
    tick()
    assert invoke(client, professional, "my_notifications")


def test_private_files_moderation_ownership_and_support(client, actors):
    customer, outsider, admin = (actors[x] for x in ["customer", "outsider", "admin"])
    img = Image.new("RGB", (20, 20), "blue")
    buff = io.BytesIO()
    img.save(buff, format="JPEG", exif=b"Exif\x00\x00test")
    upload = client.post(
        "/api/v1/files/upload",
        headers=customer["headers"],
        data={"purpose": "kyc"},
        files={"file": ("id.jpg", buff.getvalue(), "image/jpeg")},
    )
    assert upload.status_code == 201, upload.text
    ident = upload.json()["data"]["id"]
    downloaded = client.get(f"/api/v1/files/{ident}", headers=customer["headers"])
    assert downloaded.status_code == 200
    assert not Image.open(io.BytesIO(downloaded.content)).getexif()
    assert client.get(f"/api/v1/files/{ident}", headers=outsider["headers"]).status_code == 403
    assert client.get(f"/api/v1/files/{ident}").status_code == 401
    assert (
        client.post(
            f"/api/v1/files/moderation/{ident}",
            json={"reason": "Revisión de identidad de prueba."},
            headers=customer["headers"],
        ).status_code
        == 403
    )
    assert (
        client.post(
            f"/api/v1/files/moderation/{ident}",
            json={"reason": "Revisión de identidad de prueba."},
            headers=admin["headers"],
        ).status_code
        == 200
    )
    bad = client.post(
        "/api/v1/files/upload",
        headers=customer["headers"],
        data={"purpose": "avatar"},
        files={"file": ("x.jpg", b"<script>alert(1)</script>", "image/jpeg")},
    )
    assert bad.status_code == 415
    ticket = invoke(
        client,
        customer,
        "open_ticket",
        {"category": "account", "subject": "Ayuda con mi perfil", "description": "Caso ficticio de soporte."},
    )
    invoke(client, outsider, "ticket_thread", {"ticket": ticket}, expected=403)
    invoke(
        client,
        admin,
        "manage_ticket",
        {
            "ticket": ticket,
            "status": "in_progress",
            "assignee": admin["id"],
            "note": "Nota privada que no debe verse por el cliente.",
        },
    )
    invoke(
        client, admin, "reply_ticket", {"ticket": ticket, "body": "Respuesta pública del equipo de soporte."}
    )
    thread = invoke(client, customer, "ticket_thread", {"ticket": ticket})
    assert "Nota privada" not in json.dumps(thread)
    assert "Respuesta pública" in json.dumps(thread, ensure_ascii=False)


def test_signed_webhook_and_production_sandbox_gate(client, actors):
    customer = actors["customer"]
    t = invoke(client, customer, "create_topup", {"currency": "VES", "amount": 5000, "key": str(uuid4())})
    payload = json.dumps(
        {
            "kind": "topup_settled",
            "reference": t,
            "event": str(uuid4()),
            "payment": str(uuid4()),
            "amount": 5000,
            "currency": "VES",
        }
    ).encode()
    assert client.post("/api/v1/integrations/webhooks/sandbox", content=payload).status_code == 401
    stamp = str(int(time.time()))
    signature = hmac.new(
        settings.webhook_secret.encode(), stamp.encode() + b"." + payload, hashlib.sha256
    ).hexdigest()
    for _ in range(2):
        response = client.post(
            "/api/v1/integrations/webhooks/sandbox",
            content=payload,
            headers={"X-Chamba-Timestamp": stamp, "X-Chamba-Signature": signature},
        )
        assert response.status_code == 200, response.text
    assert invoke(client, customer, "money_wallet", {"currency": "VES"})["available_minor"] == 5000
    previous = settings.app_env
    settings.app_env = "production"
    try:
        assert (
            client.post(
                f"/api/v1/integrations/sandbox/topup/{t}/settle", headers=customer["headers"]
            ).status_code
            == 503
        )
    finally:
        settings.app_env = previous


def test_password_recovery_and_mfa(client, actors):
    user = actors["customer"]
    response = client.post(
        "/api/v1/auth/mfa/enroll", json={"password": user["password"]}, headers=user["headers"]
    )
    assert response.status_code == 200, response.text
    secret = response.json()["data"]["secret"]
    otp = pyotp.TOTP(secret).now()
    assert (
        client.post(
            "/api/v1/auth/mfa/confirm",
            json={"password": user["password"], "otp": otp},
            headers=user["headers"],
        ).status_code
        == 200
    )
    assert (
        client.post(
            "/api/v1/auth/login",
            json={"email": user["email"], "password": user["password"], "client": "native"},
        ).status_code
        == 401
    )
    assert (
        client.post(
            "/api/v1/auth/login",
            json={"email": user["email"], "password": user["password"], "otp": otp, "client": "native"},
        ).status_code
        == 200
    )
    response = client.post("/api/v1/auth/forgot-password", json={"email": user["email"]})
    assert response.status_code == 200 and "token" not in response.text
    paths = list((settings.data_dir / "mail").glob("*.eml"))
    assert paths
    import re
    from email import policy
    from email.parser import BytesParser

    message = BytesParser(policy=policy.default).parsebytes(paths[-1].read_bytes()).get_content()
    token = re.search(r"token=([A-Za-z0-9_-]+)", message).group(1)
    reset = client.post(
        "/api/v1/auth/consume-challenge",
        json={"token": token, "purpose": "password_reset", "password": "Another-long-test-pass!2026"},
    )
    assert reset.status_code == 200, reset.text
    assert client.get("/api/v1/auth/me", headers=user["headers"]).status_code == 401
    assert (
        client.post(
            "/api/v1/auth/consume-challenge",
            json={"token": token, "purpose": "password_reset", "password": "Another-long-test-pass!2026"},
        ).status_code
        == 400
    )


def test_onsite_manual_selection_location_and_external_payment_history(client, actors):
    customer, professional, admin, outsider = (
        actors[x] for x in ["customer", "professional", "admin", "outsider"]
    )
    category = next(
        c["id"] for c in client.get("/api/v1/public/catalog").json()["data"] if c["slug"] == "plomeria"
    )
    invoke(
        client,
        professional,
        "activate_professional",
        {"slug": "pro-" + uuid4().hex, "business_name": "Plomería de prueba"},
    )
    invoke(
        client,
        professional,
        "add_offering",
        {"category": category, "title": "Reparación de fugas", "currency": "VES", "price_minor": 10000},
    )
    branch = invoke(
        client,
        professional,
        "add_branch_ve",
        {
            "name": "Sede ficticia",
            "country": "VE",
            "state": "VE01",
            "municipality": "VE0101",
            "parish": "VE010109",
            "line": "Ubicación de prueba",
            "lon": -66.877,
            "lat": 10.516,
        },
    )
    invoke(client, professional, "set_coverage", {"branch": branch, "category": category, "radius_m": 20000})
    address = invoke(
        client,
        customer,
        "add_address_ve",
        {
            "label": "Casa ficticia",
            "country": "VE",
            "state": "VE01",
            "municipality": "VE0101",
            "parish": "VE010109",
            "line": "Dirección privada de prueba",
            "lon": -66.8771,
            "lat": 10.5161,
        },
    )
    results = invoke(
        client,
        customer,
        "search_professionals",
        {"address": address, "category": category, "sort": "distance"},
    )
    assert results[0]["professional_id"] == professional["id"] and results[0]["distance_m"] > 0
    invoke(client, outsider, "search_professionals", {"address": address, "category": category}, expected=403)
    r = invoke(
        client,
        customer,
        "create_request",
        {
            "category": category,
            "mode": "broadcast",
            "target": None,
            "title": "Reparación de fuga de prueba",
            "description": "Arreglar la llave de la cocina.",
            "address": address,
            "radius_m": 20000,
            "currency": "VES",
            "budget_min": 5000,
            "budget_max": 30000,
            "expires_at": (datetime.now(timezone.utc) + timedelta(days=7)).isoformat(),
            "key": str(uuid4()),
        },
    )
    invoke(
        client,
        admin,
        "moderate_request",
        {"request": r, "approve": True, "reason": "Revisión ficticia de información pública."},
    )
    invoke(client, customer, "publish_request", {"request": r})
    feed = invoke(client, professional, "service_feed", {"delivery_mode": "onsite"})
    assert "Dirección privada" not in json.dumps(feed) and customer["id"] not in json.dumps(feed)
    proposal = invoke(
        client,
        professional,
        "submit_proposal",
        {
            "request": r,
            "message": "Puedo resolver la fuga mañana.",
            "price_minor": 20000,
            "expires_at": (datetime.now(timezone.utc) + timedelta(days=2)).isoformat(),
        },
    )
    assert invoke(client, professional, "my_wallet")["balance"] == 3
    c = invoke(client, customer, "accept_proposal", {"proposal": proposal})
    invoke(client, professional, "navigate", {"contract": c}, expected=403)
    invoke(client, customer, "share_location", {"contract": c})
    assert (
        invoke(client, professional, "navigate", {"contract": c})["destination"]["address"]
        == "Dirección privada de prueba"
    )
    for action in ["depart", "arrive", "start"]:
        invoke(client, professional, "advance_contract", {"contract": c, "action": action})
    report = invoke(
        client,
        customer,
        "report_external_payment",
        {"contract": c, "order": None, "amount": 20000, "currency": "VES", "method": "Efectivo de prueba"},
    )
    reports = invoke(client, professional, "external_payment_reports", {"contract": c})
    assert reports[0]["id"] == report and reports[0]["confirmed_at"] is None
    invoke(client, outsider, "external_payment_reports", {"contract": c}, expected=403)
    invoke(client, professional, "confirm_external_payment", {"report": report})
    assert invoke(client, customer, "external_payment_reports", {"contract": c})[0]["confirmed_at"]
    invoke(client, professional, "advance_contract", {"contract": c, "action": "finish"})
    invoke(client, customer, "advance_contract", {"contract": c, "action": "finish"})
    invoke(client, professional, "navigate", {"contract": c}, expected=403)


def test_marketplace_inventory_and_delivery_encryption(client, actors):
    customer, professional, admin, outsider = (
        actors[x] for x in ["customer", "professional", "admin", "outsider"]
    )
    invoke(
        client,
        professional,
        "activate_professional",
        {"slug": "store-" + uuid4().hex, "business_name": "Tienda ficticia"},
    )
    plan = invoke(
        client,
        admin,
        "configure_plan",
        {
            "code": uuid4().hex,
            "name": "Plan de prueba",
            "credits": 5,
            "price": 10000,
            "currency": "VES",
            "weight": 0,
            "positioning_days": 0,
            "marketplace_days": 30,
            "active": True,
        },
    )
    p = invoke(client, professional, "start_purchase", {"plan": plan, "key": str(uuid4())})
    assert (
        client.post(
            f"/api/v1/integrations/sandbox/purchase/{p}/settle", headers=professional["headers"]
        ).status_code
        == 200
    )
    store = invoke(
        client, professional, "activate_store", {"slug": "store-" + uuid4().hex, "name": "Tienda de prueba"}
    )
    product = invoke(
        client,
        professional,
        "add_product",
        {
            "store": store,
            "sku": "TEST-1",
            "name": "Producto ficticio",
            "description": "Prueba de inventario",
            "price": 1000,
            "currency": "VES",
            "stock": 5,
        },
    )
    invoke(
        client,
        professional,
        "update_product",
        {
            "product": product,
            "name": "Producto ficticio",
            "description": "Descripción actualizada",
            "price": 1200,
            "stock": 8,
            "status": "active",
        },
    )
    order = invoke(
        client,
        customer,
        "create_order",
        {"store": store, "items": [{"product_id": product, "quantity": 2}], "key": str(uuid4())},
    )
    invoke(client, professional, "advance_order", {"order": order, "action": "accept"})
    response = client.post(
        f"/api/v1/integrations/orders/{order}/delivery",
        json={"name": "Persona de prueba", "phone": "555000000", "address": "Dirección ficticia privada"},
        headers=customer["headers"],
    )
    assert response.status_code == 200, response.text
    response = client.get(f"/api/v1/integrations/orders/{order}/delivery", headers=professional["headers"])
    assert response.status_code == 200, response.text
    assert response.json()["data"]["address"] == "Dirección ficticia privada"
    assert "address_ciphertext" not in response.text
    assert (
        client.get(f"/api/v1/integrations/orders/{order}/delivery", headers=outsider["headers"]).status_code
        == 403
    )
    assert invoke(client, professional, "my_products")[0]["stock"] == 6
    invoke(client, customer, "advance_order", {"order": order, "action": "cancel"})
    assert invoke(client, professional, "my_products")[0]["stock"] == 8


def test_withdrawal_history_preserves_currency(client, actors):
    user, admin = actors["customer"], actors["admin"]
    topup = invoke(client, user, "create_topup", {"currency": "USD", "amount": 50000, "key": str(uuid4())})
    response = client.post(f"/api/v1/integrations/sandbox/topup/{topup}/settle", headers=user["headers"])
    assert response.status_code == 200, response.text
    kyc = str(call("submit_kyc", {"provider": "manual", "external_reference": None}, actor=user["id"]))
    invoke(
        client,
        admin,
        "review_kyc",
        {
            "case": kyc,
            "status": "approved",
            "reason": "Verificación ficticia para prueba multimoneda.",
            "expires_at": (datetime.now(timezone.utc) + timedelta(days=30)).isoformat(),
        },
    )
    method = client.post(
        "/api/v1/integrations/payout-methods",
        json={"label": "Cuenta USD ficticia", "currency": "USD"},
        headers=user["headers"],
    ).json()["data"]
    invoke(
        client,
        admin,
        "configure_withdrawal_fee",
        {"currency": "USD", "basis_points": 200, "reason": "Tarifa ficticia de retiro USD para prueba."},
    )
    quote = invoke(client, user, "quote_withdrawal", {"currency": "USD", "method": method, "amount": 10000})
    withdrawal = invoke(client, user, "request_withdrawal", {"quote": quote["id"], "key": str(uuid4())})
    own = invoke(client, user, "my_withdrawals")[0]
    assert own["currency"] == "USD" and own["net_minor"] == 9800
    queue = invoke(client, admin, "admin_list", {"resource": "withdrawals"})
    record = next(r for r in queue if r["id"] == withdrawal)
    assert record["currency"] == "USD" and record["owner_name"] == "Customer Test"
    assert "destination_token" not in own and "destination_token" not in record
