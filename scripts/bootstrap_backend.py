#!/usr/bin/env python3
"""Aprovisiona cuentas técnicas restringidas; nunca entrega el propietario al servidor."""
import os
import secrets
from pathlib import Path
import db

root = Path(__file__).resolve().parents[1]
env = root / 'backend/.env'
if env.exists():
    print('backend/.env existe; se conservan las claves.')
    raise SystemExit(0)
cfg = db.config()
lines = ['APP_ENV=development', 'PAYMENT_MODE=sandbox', 'API_PORT=8181',
         'WEB_ORIGINS=http://127.0.0.1:8180,http://localhost:8180,http://127.0.0.1:5180,http://localhost:5180',
         'PUBLIC_WEB_URL=http://127.0.0.1:8180', f'JWT_SECRET={secrets.token_urlsafe(64)}']
import base64
lines += [f'ENCRYPTION_KEY={base64.urlsafe_b64encode(secrets.token_bytes(32)).decode()}',
          f'WEBHOOK_SECRET={secrets.token_urlsafe(48)}']
statements = ['BEGIN;']
for role in ['api','auth','admin','billing','worker']:
    login = f'chamba_http_{role}'
    password = secrets.token_urlsafe(40)
    statements += [f"CREATE ROLE {login} LOGIN PASSWORD '{password}' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;",
                   f'GRANT chamba_{role} TO {login};',
                   f"ALTER ROLE {login} SET statement_timeout='10s';",
                   f"ALTER ROLE {login} SET idle_in_transaction_session_timeout='15s';"]
    lines += [f'DATABASE_{role.upper()}_URL=postgresql://{login}:{password}@127.0.0.1:{cfg["POSTGRES_PORT"]}/{cfg["POSTGRES_DB"]}']
statements += ['COMMIT;']
db.sql('\n'.join(statements), capture=True)
fd = os.open(env, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as f:
    f.write('\n'.join(lines) + '\n')
print('Cinco logins restringidos y backend/.env (0600) creados.')
