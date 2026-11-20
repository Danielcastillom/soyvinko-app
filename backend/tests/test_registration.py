"""Registration explains rejected fields without exposing values or creating users."""

from uuid import uuid4

import pytest


@pytest.mark.parametrize(
    "field,value,message",
    [
        ("password", "short", "Escribe al menos 12 caracteres."),
        ("password", "x" * 129, "Usa como máximo 128 caracteres."),
        ("name", "A", "Escribe al menos 2 caracteres."),
        ("name", "   ", "Escribe al menos 2 caracteres."),
        ("name", "x" * 121, "Usa como máximo 120 caracteres."),
        ("email", "correo-invalido", "Escribe un correo electrónico válido."),
        ("password", None, "Completa este campo."),
    ],
)
def test_registration_invalid_fields_do_not_create_account(client, database, field, value, message):
    import db

    before = db.sql("SELECT count(*) FROM app.users", database=database, capture=True).stdout
    body = {
        "email": f"{uuid4().hex}@example.com",
        "name": "Prueba Registro",
        "password": "Test-password-never-echoed!",
        "client": "web",
        field: value,
    }
    response = client.post("/api/v1/auth/register", json=body)
    assert response.status_code == 422
    error = response.json()["error"]
    assert error["code"] == "validation_error"
    assert error["details"] == [{"field": f"body.{field}", "message": message}]
    assert error["request_id"]
    assert "input" not in response.text
    assert body["email"] not in response.text
    if body["password"]:
        assert body["password"] not in response.text
    assert "set-cookie" not in response.headers
    assert before == db.sql("SELECT count(*) FROM app.users", database=database, capture=True).stdout


def test_registration_minimum_password_and_normalized_name(client, database):
    import db

    db.sql("DELETE FROM app.http_rate_limits", database=database, capture=True)
    email = f"{uuid4().hex}@example.com"
    password = "  abcd1234  "
    assert len(password) == 12
    body = {"email": f" {email} ", "name": "  Ana Prueba  ", "password": password, "client": "web"}
    response = client.post("/api/v1/auth/register", json=body)
    assert response.status_code == 201, response.text
    user = response.json()["data"]["user"]
    assert user["email"] == email
    assert user["name"] == "Ana Prueba"
    assert "refresh_token" not in response.json()["data"]
    assert client.cookies.get("chamba_refresh")
    duplicate = client.post("/api/v1/auth/register", json=body)
    assert duplicate.status_code == 409
    assert duplicate.json()["error"]["code"] == "already_exists"
    # Spaces are significant in passwords, unlike the surrounding spaces in names/emails.
    assert client.post(
        "/api/v1/auth/login", json={"email": email, "password": password.strip()}
    ).status_code == 401
    assert client.post(
        "/api/v1/auth/login", json={"email": email, "password": password}
    ).status_code == 200
