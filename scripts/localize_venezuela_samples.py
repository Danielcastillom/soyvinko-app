#!/usr/bin/env python3
"""Localiza únicamente las muestras conocidas. No cambia usuarios reales ni historiales monetarios.

--apply confirma la transacción; --database permite verificar en una copia de pruebas.
Los importes de ejemplo son independientes: no representan una tasa de cambio.
"""
import argparse
import json
from pathlib import Path
import sys

import psycopg
from psycopg.rows import dict_row

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "backend"))
import db
from app.core.settings import settings
from seed_service_samples import invoke

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--apply", action="store_true")
parser.add_argument("--database")
args = parser.parse_args()
if settings.app_env not in {"staging", "development", "test"} or settings.payment_mode != "sandbox":
    raise SystemExit("La localización de muestras requiere sandbox.")
cfg = db.config()
samples = json.loads((ROOT / "scripts/demo_services.json").read_text())
override_file = ROOT / '.demo-geography-override.json'
overrides = json.loads(override_file.read_text()).get('branches', {}) if override_file.exists() and not args.database else {}
counts = {"offerings": 0, "branches": 0, "addresses": 0, "products": 0, "plans": 0, "fees": 0}
with psycopg.connect(host="127.0.0.1",port=cfg["POSTGRES_PORT"],dbname=args.database or cfg["POSTGRES_DB"],
                    user=cfg["POSTGRES_USER"],password=cfg["POSTGRES_PASSWORD"],row_factory=dict_row) as conn:
    with conn.transaction(force_rollback=not args.apply):
        conn.execute("SELECT pg_advisory_xact_lock(790230411)")
        conn.execute("SELECT set_config('app.correlation_id','venezuela-demo-localization',true)")
        admin = conn.execute("SELECT id FROM app.users WHERE email='admin@demo.example.com'").fetchone()
        if not admin:
            raise SystemExit("No hay administrador de demostración; se conserva la base.")
        for category, rows in samples.items():
            for sample in rows:
                slug=sample['slug']
                expected=(slug.removesuffix('-demo') if sample.get('existing') else slug)+'@demo.example.com'
                pro=conn.execute("SELECT p.user_id,u.email FROM app.professionals p JOIN app.users u ON u.id=p.user_id WHERE p.slug=%s",(slug,)).fetchone()
                if not pro or pro['email'] != expected:
                    raise ValueError(f"La muestra {slug} no coincide; transacción cancelada.")
                offer=conn.execute("SELECT o.id FROM app.offerings o JOIN app.categories c ON c.id=o.category_id WHERE o.professional_id=%s AND c.slug=%s AND o.title=%s AND o.active",(pro['user_id'],category,sample['title'])).fetchall()
                if len(offer)!=1:
                    raise ValueError(f"Servicio ambiguo en {slug}; transacción cancelada.")
                counts['offerings'] += conn.execute("UPDATE app.offerings SET currency=%s,price_min_minor=%s WHERE id=%s AND (currency,price_min_minor) IS DISTINCT FROM (%s,%s)",
                    (sample['currency'],sample['price_minor'],offer[0]['id'],sample['currency'],sample['price_minor'])).rowcount
                if sample['mode']=='onsite':
                    if slug in overrides:
                        if not conn.execute('SELECT 1 FROM app.branches WHERE id=%s AND professional_id=%s AND active', (overrides[slug], pro['user_id'])).fetchone():
                            raise ValueError('La sede reubicada no coincide; se conserva la base.')
                        continue
                    p=conn.execute("SELECT p.*,m.state_code FROM app.geo_parishes p JOIN app.geo_municipalities m ON m.code=p.municipality_code WHERE p.code=%s",(sample['parish_code'],)).fetchone()
                    branches=conn.execute("SELECT DISTINCT b.id FROM app.branches b JOIN app.coverage_areas ca ON ca.branch_id=b.id JOIN app.offerings o ON o.category_id=ca.category_id AND o.professional_id=b.professional_id WHERE o.id=%s AND b.name IN ('Sede Bogotá','Sede de ejemplo en Bogotá',%s)",(offer[0]['id'],'Sede de ejemplo en '+sample['city'])).fetchall()
                    if len(branches)!=1:
                        raise ValueError(f"Sede ambigua en {slug}; transacción cancelada.")
                    counts['branches'] += conn.execute("UPDATE app.branches SET name=%s,country_code='VE',city=%s,state_code=%s,municipality_code=%s,parish_code=%s,location=internal.point(%s,%s),timezone='America/Caracas' WHERE id=%s AND (country_code,parish_code) IS DISTINCT FROM ('VE',%s)",
                        ('Sede de ejemplo en '+sample['city'],sample['city'],p['state_code'],p['municipality_code'],p['code'],p['longitude'],p['latitude'],branches[0]['id'],p['code'])).rowcount
        counts['addresses'] += conn.execute("UPDATE app.addresses a SET country_code='VE',region='Distrito Capital',city='Caracas',state_code='VE01',municipality_code='VE0101',parish_code='VE010109',location=internal.point(p.longitude,p.latitude) FROM app.users u, app.geo_parishes p WHERE a.user_id=u.id AND u.email='cliente@demo.example.com' AND a.label='Casa de ejemplo' AND a.address_line='Dirección ficticia · entorno de prueba' AND a.country_code='CO' AND p.code='VE010109'").rowcount
        for sku,price in [('SSD',3500),('KIT',1000)]:
            counts['products'] += conn.execute("UPDATE app.products p SET currency='USD',price_minor=%s FROM app.stores s,app.users u WHERE p.store_id=s.id AND s.professional_id=u.id AND u.email='tecnico@demo.example.com' AND s.slug='tecnologia-demo' AND p.sku=%s AND p.currency='COP'",(price,sku)).rowcount
        conn.execute("UPDATE app.plans SET active=false WHERE code='DEMO-CRECE' AND currency='COP' AND active")
        for currency,price in [('USD',1000),('VES',100000)]:
            code='DEMO-CRECE-VE-'+currency
            if not conn.execute("SELECT 1 FROM app.plans WHERE code=%s",(code,)).fetchone():
                invoke(conn,'configure_plan',code,'Crece · demostración '+('Bs' if currency=='VES' else 'USD'),15,price,currency,10,30,30,True,actor=admin['id'])
                counts['plans']+=1
            if not conn.execute("SELECT 1 FROM app.withdrawal_fee_policies WHERE currency=%s AND active",(currency,)).fetchone():
                invoke(conn,'configure_withdrawal_fee',currency,200,'Tarifa ficticia sandbox: 2% solo al retiro; sin conversión de divisas.',actor=admin['id'])
                counts['fees']+=1
        conn.execute("SELECT set_config('app.user_id',%s,true)",(str(admin['id']),))
        if any(counts.values()): conn.execute("SELECT internal.audit('demo.venezuela_localized','demo_catalog',NULL)")
print(json.dumps({"mode":"applied" if args.apply else "preview",**counts}))
