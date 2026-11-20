#!/usr/bin/env python3
"""Datos ficticios únicamente en development/sandbox; contraseñas privadas aleatorias."""
import json
import os
from pathlib import Path
import secrets
import sys
from uuid import uuid4
from datetime import datetime, timedelta, timezone
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'backend'))
import db
import argparse
from psycopg.conninfo import conninfo_to_dict, make_conninfo
from app.core.db import call, start_pools, stop_pools
from app.core.security import passwords
from app.core.settings import settings
from app.modules.integrations.router import SandboxProvider

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--database')
parser.add_argument('--access-file')
args=parser.parse_args()
if args.database:
    for role in ('api','auth','admin','billing','worker'):
        key=f'database_{role}_url'
        params=conninfo_to_dict(getattr(settings,key)); params['dbname']=args.database
        setattr(settings,key,make_conninfo(**params))

if settings.production or settings.payment_mode!='sandbox':
    raise SystemExit('Semilla permitida solo en modo local sandbox.')
access=Path(args.access_file) if args.access_file else ROOT/'.local-access.md'
if access.exists():
    raise SystemExit('Las cuentas de demostración ya fueron creadas; se conservan.')
start_pools()
accounts=[]
try:
    for key,name in [('admin','Administración Demo'),('cliente','Andrea Demo'),('plomero','Mateo · Soluciones para tu hogar'),
                     ('tecnico','Daniel · Soporte digital'),('disenadora','Lucía · Estudio creativo')]:
        email=f'{key}@demo.example.com'
        password=secrets.token_urlsafe(18)
        ident=call('register_user',{'email':email,'name':name,'argon2id_hash':passwords.hash(password)},role='auth')
        accounts.append({'key':key,'id':str(ident),'email':email,'password':password})
    admin=accounts[0]['id']; customer=accounts[1]['id']
    db.sql(f"INSERT INTO app.user_roles(user_id,role_code) VALUES('{admin}','superadmin');",database=args.database,capture=True)
    # Documents/reviews are never invented as approved KYC.
    plan=call('configure_plan',{'code':'DEMO-CRECE-VE-USD','name':'Crece · demostración','credits':15,'price':1000,'currency':'USD',
        'weight':10,'positioning_days':30,'marketplace_days':30,'active':True},actor=admin,role='admin')
    call('configure_withdrawal_fee',{'currency':'USD','basis_points':200,'reason':'Tarifa ficticia del entorno DEMO: 2%, no es política comercial de producción.'},actor=admin,role='admin')
    categories={c['slug']:c['id'] for c in call('catalog')}
    parish=call('geo_parish',{'parish':'VE010109'})
    bios=['Soluciones de plomería, reparación de fugas e instalaciones. Atención cuidadosa y trabajo explicado paso a paso.',
          'Diagnóstico y reparación de computadores. Te ayudo a recuperar la tranquilidad y a poner tus equipos a punto.',
          'Identidades visuales con personalidad. Diseño logos, piezas digitales y presentaciones que cuentan tu historia.']
    for i,(account,category,title,price) in enumerate(zip(accounts[2:],['plomeria','informatica','diseno-grafico'],
            ['Reparaciones e instalaciones de plomería','Diagnóstico y reparación de computadores','Diseño de identidad visual'],[1300,1600,5000])):
        actor=account['id']
        call('activate_professional',{'slug':account['key']+'-demo','business_name':account['key'].capitalize()+' · Demo','bio':bios[i]},actor=actor)
        call('update_professional',{'name': ['Mateo Rodríguez','Daniel Torres','Lucía Martínez'][i], 'bio':bios[i],'years':[8,6,5][i],'accepts':True},actor=actor)
        offer=call('add_offering',{'category':categories[category],'title':title,'currency':'USD','price_minor':price,'description':bios[i]},actor=actor)
        if i==2:
            call('set_offering_delivery',{'offering':offer,'mode':'remote'},actor=actor)
        else:
            branch=call('add_branch_ve',{'name':'Sede de ejemplo en Caracas','country':'VE','state':'VE01','municipality':'VE0101','parish':'VE010109','line':'Dirección ficticia de demostración',
                'lon':parish['longitude'],'lat':parish['latitude'],'public_address':False},actor=actor)
            call('set_coverage',{'branch':branch,'category':categories[category],'radius_m':25000},actor=actor)
        if i==1:
            purchase=call('start_purchase',{'plan':plan,'key':uuid4()},actor=actor)
            record=next(p for p in call('my_credit_purchases',actor=actor) if p['id']==str(purchase))
            SandboxProvider().settle_purchase(record)
            store=call('activate_store',{'slug':'tecnologia-demo','name':'Daniel · Tecnología','description':'Productos de ejemplo. Datos ficticios.'},actor=actor)
            for sku,name,description,amount in [('SSD','Unidad SSD 480 GB','Almacenamiento para darle nueva vida a tu equipo.',3500),('KIT','Kit de limpieza','Cuida tu computador y sus accesorios.',1000)]:
                product=call('add_product',{'store':store,'sku':sku,'name':name,'description':description,'price':amount,'currency':'USD','stock':10},actor=actor)
                call('update_product',{'product':product,'name':name,'description':description,'price':amount,'stock':10,'status':'active'},actor=actor)
    call('add_address_ve',{'label':'Casa de ejemplo','country':'VE','state':'VE01','municipality':'VE0101','parish':'VE010109','line':'Dirección ficticia · entorno de prueba','lon':parish['longitude'],'lat':parish['latitude']},actor=customer)
    fd=os.open(access,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'w') as f:
        f.write('# Accesos locales · solo datos ficticios\n\nWeb: http://127.0.0.1:8180\nAPI: http://127.0.0.1:8181/api/docs\n\n')
        for a in accounts:
            f.write(f'- {a["key"]}: `{a["email"]}` — contraseña: `{a["password"]}`\n')
    print('5 cuentas ficticias y catálogo demo creados. Accesos en .local-access.md (0600).')
finally:
    stop_pools()
