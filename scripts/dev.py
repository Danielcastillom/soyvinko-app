#!/usr/bin/env python3
"""Instalación y operación del entorno local completo de Chamba."""
import argparse
import os
from pathlib import Path
import subprocess
import sys
import db

ROOT=Path(__file__).resolve().parents[1]
def run(*args,cwd=ROOT,env=None):
    subprocess.run(args,cwd=cwd,env=env,check=True)

def public_url():
    for line in (ROOT/'backend/.env').read_text().splitlines():
        if line.startswith('PUBLIC_WEB_URL='):
            return line.split('=',1)[1].strip().rstrip('/')
    return 'http://127.0.0.1:8180'

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=['setup','up','stop','status','check'])
    args=parser.parse_args()
    if args.action=='setup':
        for action in ('init','up','migrate'):
            run(sys.executable,'scripts/db.py',action)
        env=ROOT/'.env'
        config=env.read_text()
        if 'LOCAL_UID=' not in config:
            with env.open('a') as f:
                f.write(f'LOCAL_UID={os.getuid()}\nLOCAL_GID={os.getgid()}\n')
        run(sys.executable,'scripts/bootstrap_backend.py')
        run(sys.executable,'-m','venv','backend/.venv')
        run(str(ROOT/'backend/.venv/bin/pip'),'install','-r','backend/requirements.txt')
        (ROOT/'backend/data').mkdir(exist_ok=True,mode=0o700)
        if not (ROOT/'.local-access.md').exists():
            run(str(ROOT/'backend/.venv/bin/python'),'scripts/seed_demo.py')
        run(str(ROOT/'backend/.venv/bin/python'),'scripts/seed_service_samples.py','--apply')
        run(str(ROOT/'backend/.venv/bin/python'),'scripts/localize_venezuela_samples.py','--apply')
        run('docker','compose','up','-d','--build','--wait','api','worker','web')
        print(f'Portal: {public_url()} · API: {public_url()}/api/docs')
        print('Accesos ficticios: .local-access.md (privado).')
    elif args.action=='up':
        run(sys.executable,'scripts/db.py','up')
        run(sys.executable,'scripts/db.py','migrate')
        run('docker','compose','up','-d','--build','--wait','api','worker','web')
    elif args.action=='stop':
        run('docker','compose','stop','web','worker','api')
    elif args.action=='status':
        db.compose('ps')
    elif args.action=='check':
        run(sys.executable,'tests/run.py')
        run(str(ROOT/'backend/.venv/bin/pytest'),'-q',cwd=ROOT/'backend')
        run('npm','ci',cwd=ROOT/'frontend')
        run('npm','run','build',cwd=ROOT/'frontend')
        browser_env = dict(os.environ)
        browser_env.setdefault('PLAYWRIGHT_BASE_URL', public_url())
        run('npx','playwright','test',cwd=ROOT/'frontend',env=browser_env)

if __name__=='__main__':
    main()
