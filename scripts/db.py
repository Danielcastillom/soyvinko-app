#!/usr/bin/env python3
"""Administración local reproducible; Python estándar + Docker Compose."""
import argparse
import hashlib
import os
from pathlib import Path
import secrets
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def compose(*args, **kwargs):
    return subprocess.run(["docker", "compose", *args], cwd=ROOT, check=True, **kwargs)


def config():
    values = {}
    for line in (ROOT / ".env").read_text().splitlines():
        if line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key] = value
    return values


def sql(statement, *, database=None, capture=False):
    cfg = config()
    return compose("exec", "-T", "db", "psql", "-X", "-v", "ON_ERROR_STOP=1",
                   "-U", cfg["POSTGRES_USER"], "-d", database or cfg["POSTGRES_DB"],
                   "-q", "-A", "-t", input=statement, text=True, capture_output=capture)


def migrate(database=None, quiet=False):
    # Una conexión y un bloqueo: también serializa dos instaladores concurrentes.
    statements = ["BEGIN; SELECT pg_advisory_xact_lock(790230410);",
                  "CREATE SCHEMA IF NOT EXISTS meta;",
                  "CREATE TABLE IF NOT EXISTS meta.migrations (name text PRIMARY KEY, "
                  "sha256 text NOT NULL, applied_at timestamptz NOT NULL DEFAULT now());"]
    for path in sorted((ROOT / "db/migrations").glob("*.sql")):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        name = path.name
        statements += [
            f"DO $$ BEGIN IF EXISTS (SELECT 1 FROM meta.migrations WHERE name='{name}' "
            f"AND sha256 <> '{digest}') THEN RAISE EXCEPTION 'Migracion modificada: {name}'; END IF; END $$;",
            f"SELECT EXISTS (SELECT 1 FROM meta.migrations WHERE name='{name}') AS applied \\gset",
            "\\if :applied", f"\\echo Ya aplicada: {name}", "\\else",
            path.read_text(),
            f"INSERT INTO meta.migrations(name, sha256) VALUES ('{name}', '{digest}');",
            "\\endif",
        ]
    statements.append("COMMIT;")
    sql("\n".join(statements), database=database, capture=quiet)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["init", "up", "migrate", "psql", "test", "status", "backup"])
    args = parser.parse_args()
    if args.action == "init":
        path = ROOT / ".env"
        try:
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        except FileExistsError:
            print(".env ya existe; se conserva.")
        else:
            with os.fdopen(fd, "w") as stream:
                stream.write("POSTGRES_DB=chamba\nPOSTGRES_USER=chamba_owner\n"
                             f"POSTGRES_PASSWORD={secrets.token_urlsafe(36)}\nPOSTGRES_PORT=55432\n")
            print(".env creado con clave aleatoria y permisos 0600.")
    elif args.action == "up":
        compose("up", "-d", "--wait", "db")
    elif args.action == "migrate":
        migrate()
    elif args.action == "psql":
        cfg = config()
        compose("exec", "db", "psql", "-X", "-U", cfg["POSTGRES_USER"], "-d", cfg["POSTGRES_DB"])
    elif args.action == "status":
        compose("ps")
    elif args.action == "test":
        subprocess.run([sys.executable, str(ROOT / "tests/run.py")], cwd=ROOT, check=True)
    elif args.action == "backup":
        from datetime import datetime, timezone
        cfg = config()
        folder = ROOT / "backups"
        folder.mkdir(exist_ok=True, mode=0o700)
        path = folder / (datetime.now(timezone.utc).strftime("chamba-%Y%m%dT%H%M%SZ") + ".dump")
        with path.open("xb") as stream:
            os.chmod(path, 0o600)
            compose("exec", "-T", "db", "pg_dump", "-U", cfg["POSTGRES_USER"], "-d", cfg["POSTGRES_DB"], "-Fc", stdout=stream)
        print(path)


if __name__ == "__main__":
    main()
