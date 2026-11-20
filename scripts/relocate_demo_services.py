#!/usr/bin/env python3
"""Place only recognized sandbox service samples within 50 km of an owned saved address.

Dry run by default. The anchor coordinates never appear in output or the override file.
"""
import argparse
from datetime import datetime, timezone
import json
import math
import os
from pathlib import Path
import sys
from uuid import UUID

import psycopg
from psycopg.rows import dict_row

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'backend'))
import db
from app.core.settings import settings


def private_json(path, value, *, exclusive=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | (os.O_EXCL if exclusive else os.O_TRUNC), 0o600)
    with os.fdopen(fd, 'w') as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, default=str)
        stream.write('\n')
    os.chmod(path, 0o600)


def relocate(conn, address, owner, samples, *, apply=False, backup=None):
    changes = {'branches': 0, 'coverage': 0, 'demo_addresses': 0}
    with conn.transaction(force_rollback=not apply):
        conn.execute('SELECT pg_advisory_xact_lock(790230411)')
        anchor = conn.execute("SELECT a.id,a.city FROM app.addresses a JOIN app.users u ON u.id=a.user_id WHERE a.id=%s AND a.user_id=%s AND a.archived_at IS NULL AND a.country_code='VE' AND u.status='active' FOR SHARE OF a", (address, owner)).fetchone()
        if not anchor:
            raise ValueError('No se encontró una dirección venezolana activa del propietario indicado.')
        source = [(category, sample) for category, rows in samples.items() for sample in rows if sample['mode'] == 'onsite']
        if len(source) != 30:
            raise ValueError('El catálogo de muestras presenciales cambió; revisar antes de moverlo.')
        branches, coverage, targets, override = [], [], [], {}
        for index, (category, sample) in enumerate(source):
            expected = (sample['slug'].removesuffix('-demo') if sample.get('existing') else sample['slug']) + '@demo.example.com'
            pro = conn.execute('SELECT p.user_id,u.email FROM app.professionals p JOIN app.users u ON u.id=p.user_id WHERE p.slug=%s', (sample['slug'],)).fetchone()
            if not pro or pro['email'] != expected:
                raise ValueError('La identidad de una muestra no coincide; no se modificó el catálogo.')
            offers = conn.execute("SELECT o.id FROM app.offerings o JOIN app.categories c ON c.id=o.category_id WHERE o.professional_id=%s AND c.slug=%s AND o.title=%s AND o.active AND o.delivery_mode='onsite'", (pro['user_id'], category, sample['title'])).fetchall()
            if len(offers) != 1:
                raise ValueError('Oferta de ejemplo ambigua; no se modificó el catálogo.')
            rows = conn.execute('SELECT DISTINCT b.id FROM app.branches b JOIN app.coverage_areas ca ON ca.branch_id=b.id JOIN app.offerings o ON o.professional_id=b.professional_id AND o.category_id=ca.category_id WHERE o.id=%s AND b.active', (offers[0]['id'],)).fetchall()
            if len(rows) != 1:
                raise ValueError('Sede de ejemplo ambigua; no se modificó el catálogo.')
            branch = conn.execute('SELECT to_jsonb(b) data FROM app.branches b WHERE id=%s FOR UPDATE', (rows[0]['id'],)).fetchone()['data']
            if branch['public_address'] or not branch['name'].startswith('Sede de ejemplo en '):
                raise ValueError('La sede ya no tiene la configuración privada de demostración.')
            areas = conn.execute('SELECT to_jsonb(ca) data FROM app.coverage_areas ca WHERE branch_id=%s FOR UPDATE', (branch['id'],)).fetchall()
            if not areas or any(a['data']['polygon'] is not None or not a['data']['active'] for a in areas):
                raise ValueError('Cobertura de muestra personalizada; requiere revisión.')
            branches.append(branch)
            coverage.extend(a['data'] for a in areas)
            distance = 2500 + index * 40000 / (len(source) - 1)
            bearing = math.radians((index * 137.507764) % 360)
            # Deterministic candidates; use the first point inside a catalog parish.
            target = conn.execute("""
                WITH candidates AS (
                  SELECT n,public.ST_Project(a.location,%s::float8,%s::float8+n*0.3490658504) point
                  FROM app.addresses a CROSS JOIN generate_series(0,35) n WHERE a.id=%s
                )
                SELECT p.code parish,p.name parish_name,m.code municipality,m.name city,m.state_code,s.name region,
                  public.ST_X(c.point::public.geometry) lon,public.ST_Y(c.point::public.geometry) lat
                FROM candidates c JOIN app.geo_parishes p ON public.ST_Covers(p.boundary,c.point::public.geometry)
                  JOIN app.geo_municipalities m ON m.code=p.municipality_code JOIN app.geo_states s ON s.code=m.state_code
                WHERE s.country_code='VE' ORDER BY c.n,p.code LIMIT 1
            """, (distance, bearing, address)).fetchone()
            if not target:
                raise ValueError('No se encontró una parroquia dentro del rango de muestra.')
            targets.append((branch['id'], target))
            override[sample['slug']] = branch['id']
        demo_addresses = conn.execute("SELECT to_jsonb(a) data FROM app.addresses a JOIN app.users u ON u.id=a.user_id WHERE u.email='cliente@demo.example.com' AND a.label='Casa de ejemplo' AND a.address_line='Dirección ficticia · entorno de prueba' AND a.archived_at IS NULL FOR UPDATE OF a").fetchall()
        if len(demo_addresses) != 1:
            raise ValueError('La dirección del cliente de demostración es ambigua.')
        if apply:
            if backup is None:
                raise ValueError('Se requiere respaldo antes de aplicar.')
            private_json(backup, {'branches': branches, 'coverage': coverage, 'demo_addresses': [a['data'] for a in demo_addresses]}, exclusive=True)
        for ident, target in targets:
            changes['branches'] += conn.execute("""UPDATE app.branches SET name=%s,country_code='VE',city=%s,state_code=%s,municipality_code=%s,parish_code=%s,
                location=internal.point(%s,%s),address_line=%s,timezone='America/Caracas'
                WHERE id=%s AND (city,parish_code,location::public.geometry) IS DISTINCT FROM (%s,%s,internal.point(%s,%s)::public.geometry)""",
                ('Sede de ejemplo en '+target['city'],target['city'],target['state_code'],target['municipality'],target['parish'],target['lon'],target['lat'],
                 'Ubicación ficticia de muestra en '+target['parish_name'],ident,target['city'],target['parish'],target['lon'],target['lat'])).rowcount
            changes['coverage'] += conn.execute('UPDATE app.coverage_areas SET radius_m=50000 WHERE branch_id=%s AND radius_m<>50000', (ident,)).rowcount
        # Keep the fictional customer near the relocated catalog, away from the real anchor.
        point=targets[0][1]
        changes['demo_addresses'] += conn.execute("""UPDATE app.addresses SET city=%s,region=%s,state_code=%s,municipality_code=%s,parish_code=%s,location=internal.point(%s,%s)
            WHERE id=%s AND (parish_code,location::public.geometry) IS DISTINCT FROM (%s,internal.point(%s,%s)::public.geometry)""",
            (point['city'],point['region'],point['state_code'],point['municipality'],point['parish'],point['lon'],point['lat'],demo_addresses[0]['data']['id'],point['parish'],point['lon'],point['lat'])).rowcount
        ids=list(override.values())
        metrics=conn.execute("""SELECT count(*) total,round(min(public.ST_Distance(a.location,b.location))) min_m,
          round(max(public.ST_Distance(a.location,b.location))) max_m,
          count(*) FILTER(WHERE public.ST_DWithin(a.location,b.location,50000) AND public.ST_Covers(p.boundary,b.location::public.geometry)) valid
          FROM app.branches b JOIN app.geo_parishes p ON p.code=b.parish_code CROSS JOIN app.addresses a
          WHERE a.id=%s AND b.id=ANY(%s::uuid[])""", (address,ids)).fetchone()
        if metrics['total']!=30 or metrics['valid']!=30 or metrics['max_m']>50000:
            raise ValueError('Las distancias o parroquias no pasaron la comprobación final.')
        if any(changes.values()):
            conn.execute("SELECT set_config('app.user_id',%s,true)",(str(owner),))
            conn.execute("SELECT internal.audit('demo.nearby_relocated','demo_catalog',NULL)")
    return {**changes,**metrics,'reference_city':anchor['city']}, {'anchor_address_id':str(address),'owner_id':str(owner),'radius_m':50000,'branches':override}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--address',required=True,type=UUID)
    parser.add_argument('--owner',required=True,type=UUID)
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--database')
    parser.add_argument('--backup',type=Path)
    args=parser.parse_args()
    if settings.app_env not in {'staging','development','test'} or settings.payment_mode!='sandbox':
        raise SystemExit('La reubicación requiere un entorno sandbox.')
    cfg=db.config()
    backup=args.backup or ROOT/'backups'/('demo-relocation-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'.json')
    with psycopg.connect(host='127.0.0.1',port=cfg['POSTGRES_PORT'],dbname=args.database or cfg['POSTGRES_DB'],user=cfg['POSTGRES_USER'],password=cfg['POSTGRES_PASSWORD'],row_factory=dict_row) as conn:
        result,override=relocate(conn,args.address,args.owner,json.loads((ROOT/'scripts/demo_services.json').read_text()),apply=args.apply,backup=backup)
    if args.apply and not args.database:
        private_json(ROOT/'.demo-geography-override.json',override)
    print(json.dumps({'mode':'applied' if args.apply else 'preview',**result},default=str))


if __name__=='__main__':
    main()
