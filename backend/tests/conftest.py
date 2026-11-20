import os
import sys
from pathlib import Path
from uuid import uuid4
import pytest
from psycopg.conninfo import conninfo_to_dict, make_conninfo
from fastapi.testclient import TestClient

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
# Tests always use isolated local behavior, even when .env is configured for HTTPS.
os.environ.update(
    APP_ENV="test",
    PAYMENT_MODE="sandbox",
    PUBLIC_WEB_URL="http://testserver",
    WEB_ORIGINS="http://testserver",
    SMTP_HOST="",
)
import db as owner_db
from app.main import app
from app.core.settings import settings
from app.core.db import call
from app.core.security import passwords


@pytest.fixture(scope="session")
def database(tmp_path_factory):
    name = "chamba_http_test_" + uuid4().hex[:12]
    owner_db.sql(f"CREATE DATABASE {name}", capture=True)
    old = {
        role: getattr(settings, f"database_{role}_url")
        for role in ("api", "auth", "admin", "billing", "worker")
    }
    old_dir = settings.data_dir
    try:
        owner_db.migrate(name, quiet=True)
        for role, dsn in old.items():
            params = conninfo_to_dict(dsn)
            params["dbname"] = name
            setattr(settings, f"database_{role}_url", make_conninfo(**params))
        settings.data_dir = tmp_path_factory.mktemp("private_files")
        yield name
    finally:
        for role, dsn in old.items():
            setattr(settings, f"database_{role}_url", dsn)
        settings.data_dir = old_dir
        owner_db.sql(f"DROP DATABASE {name} WITH (FORCE)", capture=True)


@pytest.fixture(scope="session")
def client(database):
    with TestClient(app) as c:
        yield c


@pytest.fixture
def actors(client, database):
    owner_db.sql("DELETE FROM app.http_rate_limits", database=database, capture=True)
    users = {}
    for role in ("customer", "professional", "admin", "outsider"):
        email = f"{uuid4().hex}@example.com"
        password = "Test-only-password-2026!"
        ident = call(
            "register_user",
            {"email": email, "name": role.title() + " Test", "argon2id_hash": passwords.hash(password)},
            role="auth",
        )
        users[role] = {"id": str(ident), "email": email, "password": password}
    owner_db.sql(
        f"INSERT INTO app.user_roles(user_id,role_code) VALUES('{users['admin']['id']}','superadmin')",
        database=database,
        capture=True,
    )
    for role, u in users.items():
        res = client.post(
            "/api/v1/auth/login", json={"email": u["email"], "password": u["password"], "client": "native"}
        )
        assert res.status_code == 200, res.text
        u.update(res.json()["data"])
        u["headers"] = {"Authorization": "Bearer " + u["access_token"]}
    return users
