#!/usr/bin/env python3
"""Añade muestras sandbox sin duplicar ni alterar perfiles existentes.

Por defecto solo informa del plan. --apply ejecuta una transacción auditable.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import sys
from uuid import uuid4

import psycopg
from psycopg import sql
from psycopg.rows import dict_row
from argon2 import PasswordHasher

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "backend"))
import db
from app.core.settings import settings


def invoke(conn, name, *args, actor=None):
    conn.execute("SELECT set_config('app.user_id', %s, true)", (str(actor) if actor else "",))
    query = sql.SQL("SELECT api.{}({}) AS value").format(
        sql.Identifier(name), sql.SQL(",").join(sql.Placeholder() for _ in args)
    )
    return conn.execute(query, args).fetchone()["value"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--database")
    parser.add_argument("--access-file")
    args = parser.parse_args()
    if settings.app_env not in {"development", "staging", "test"} or settings.payment_mode != "sandbox":
        raise SystemExit("Las muestras requieren un entorno de pruebas con pagos sandbox.")
    samples = json.loads((ROOT / "scripts/demo_services.json").read_text())
    if any(len(rows) < 3 for rows in samples.values()):
        raise SystemExit("Cada categoría debe incluir al menos tres muestras.")
    cfg = db.config()
    access_path = Path(args.access_file) if args.access_file else ROOT / ".demo-services-access.json"
    access = json.loads(access_path.read_text()) if access_path.exists() else {}
    created, preserved, temporary = [], [], None
    passwords = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=2)
    with psycopg.connect(
        host="127.0.0.1", port=cfg["POSTGRES_PORT"], dbname=args.database or cfg["POSTGRES_DB"],
        user=cfg["POSTGRES_USER"], password=cfg["POSTGRES_PASSWORD"], row_factory=dict_row,
    ) as conn:
        with conn.transaction():
            if args.apply:
                conn.execute("SELECT pg_advisory_xact_lock(790230411)")
                conn.execute("SELECT set_config('app.correlation_id', %s, true)", ("demo-services-" + str(uuid4()),))
            else:
                conn.execute("SET TRANSACTION READ ONLY")
            categories = {row["slug"]: row["id"] for row in conn.execute(
                "SELECT id, slug FROM app.categories WHERE active"
            )}
            if set(categories) != set(samples):
                raise ValueError("El catálogo y las muestras no coinciden; revisar antes de continuar.")
            for category, rows in samples.items():
                for position, sample in enumerate(rows):
                    slug = sample["slug"]
                    current = conn.execute(
                        "SELECT p.user_id, p.status, u.email FROM app.professionals p "
                        "JOIN app.users u ON u.id=p.user_id WHERE p.slug=%s", (slug,)
                    ).fetchone()
                    if current:
                        offering = conn.execute(
                            "SELECT id FROM app.offerings WHERE professional_id=%s "
                            "AND category_id=%s AND active AND delivery_mode=%s",
                            (current["user_id"], categories[category], sample["mode"]),
                        ).fetchone()
                        expected_email = (slug.removesuffix("-demo") if sample.get("existing") else slug) + "@demo.example.com"
                        if not offering or current["status"] != "active" or current["email"] != expected_email:
                            raise ValueError(f"El perfil {slug} necesita revisión; no se modifica automáticamente.")
                        preserved.append(slug)
                        continue
                    if sample.get("existing"):
                        raise ValueError(f"Falta el perfil original {slug}; ejecutar primero seed_demo.py.")
                    created.append(slug)
                    if not args.apply:
                        continue
                    email, password = slug + "@demo.example.com", secrets.token_urlsafe(24)
                    user = invoke(conn, "register_user", email, sample["name"], passwords.hash(password))
                    invoke(conn, "activate_professional", slug, sample["name"], sample["description"], actor=user)
                    invoke(conn, "update_professional", sample["name"], sample["description"], sample["years"], True, actor=user)
                    offer = invoke(conn, "add_offering", categories[category], sample["title"], sample["currency"],
                                   sample["price_minor"], sample["description"], actor=user)
                    invoke(conn, "set_offering_delivery", offer, sample["mode"], actor=user)
                    if sample["mode"] == "onsite":
                        parish = conn.execute("SELECT p.*,m.state_code FROM app.geo_parishes p JOIN app.geo_municipalities m ON m.code=p.municipality_code WHERE p.code=%s", (sample['parish_code'],)).fetchone()
                        branch = invoke(conn, "add_branch_ve", "Sede de ejemplo en " + sample['city'], "VE", parish['state_code'], parish['municipality_code'], parish['code'],
                                        "Dirección ficticia de demostración", parish['longitude'], parish['latitude'], False, actor=user)
                        invoke(conn, "set_coverage", branch, categories[category], 25000, actor=user)
                    access[slug] = {"email": email, "password": password, "user_id": str(user)}
            if args.apply and created:
                temporary = access_path.with_name(access_path.name + "." + uuid4().hex + ".tmp")
                fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
                with os.fdopen(fd, "w") as stream:
                    json.dump(access, stream, ensure_ascii=False, indent=2)
                    stream.write("\n")
        # Exit the connection context commits the outer transaction.
    if temporary:
        os.replace(temporary, access_path)
    print(json.dumps({"mode": "applied" if args.apply else "preview", "categories": len(samples),
                      "created" if args.apply else "to_create": len(created), "preserved": len(preserved)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
