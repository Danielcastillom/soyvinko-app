#!/usr/bin/env python3
"""Regenera el diccionario y la referencia SQL a partir de la instancia local."""
import json
from pathlib import Path
import db

ROOT = Path(__file__).resolve().parents[1]


def fetch(statement):
    return json.loads(db.sql(statement, capture=True).stdout)


def cell(value):
    return str(value if value is not None else "—").replace("|", "\\|").replace("\n", " ")


tables = fetch("""
SELECT jsonb_agg(jsonb_build_object('name',c.relname,
  'columns',(SELECT jsonb_agg(jsonb_build_object('name',a.attname,'type',format_type(a.atttypid,a.atttypmod),
    'nullable',NOT a.attnotnull,'default',pg_get_expr(d.adbin,d.adrelid),'identity',a.attidentity,'generated',a.attgenerated)
    ORDER BY a.attnum) FROM pg_attribute a LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
    WHERE a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped),
  'constraints',(SELECT jsonb_agg(pg_get_constraintdef(co.oid) ORDER BY co.conname) FROM pg_constraint co WHERE co.conrelid=c.oid),
  'indexes',(SELECT jsonb_agg(indexdef ORDER BY indexname) FROM pg_indexes i WHERE i.schemaname='app' AND i.tablename=c.relname))
  ORDER BY c.relname)
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='app' AND c.relkind='r';
""")
lines = ["# Diccionario de datos", "", f"{len(tables)} tablas de negocio del esquema `app`. Generado con `python3 scripts/export_schema_docs.py`.",
         "", "Los tiempos son `timestamptz`; dinero en unidades menores; distancias en metros. `meta.migrations` y las tablas de extensiones no se incluyen.", ""]
for table in tables:
    lines += [f"## {table['name']}", "", "| Columna | Tipo | Acepta NULL | Predeterminado / expresión |", "|---|---|---|---|"]
    for column in table["columns"]:
        default = "IDENTITY" if column["identity"] else column["default"]
        if column["generated"]:
            default = "GENERATED: " + str(default)
        lines.append(f"| `{cell(column['name'])}` | `{cell(column['type'])}` | {'Sí' if column['nullable'] else 'No'} | {cell(default)} |")
    lines += ["", "Restricciones:", ""]
    lines += [f"- `{cell(constraint)}`" for constraint in table["constraints"] or []]
    lines += ["", "Índices:", "", "```sql", *(table["indexes"] or []), "```", ""]
(ROOT / "docs/data-dictionary.md").write_text("\n".join(lines))

functions = fetch("""
SELECT jsonb_agg(jsonb_build_object('name',p.proname,'args',pg_get_function_arguments(p.oid),
  'returns',pg_get_function_result(p.oid),'roles',(
    SELECT jsonb_agg(r.rolname ORDER BY r.rolname) FROM pg_roles r
    WHERE r.rolname IN('chamba_api','chamba_auth','chamba_billing','chamba_admin','chamba_worker')
      AND has_function_privilege(r.oid,p.oid,'EXECUTE'))) ORDER BY p.proname)
FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='api';
""")
lines = ["# Referencia SQL del backend", "", f"{len(functions)} funciones del esquema `api`, generadas desde PostgreSQL.", "",
         "Las funciones no son endpoints HTTP. Consultar [integración](integration.md) para contexto de usuario, roles y responsabilidades del servidor.", "",
         "| Función | Rol autorizado | Devuelve |", "|---|---|---|"]
for function in functions:
    lines.append(f"| [`{function['name']}`](#{function['name']}) | {', '.join(function['roles'] or [])} | `{cell(function['returns'])}` |")
lines.append("")
for function in functions:
    lines += [f"## {function['name']}", "", "```sql", f"api.{function['name']}({function['args']})", "```", "",
              f"Devuelve: `{function['returns']}`. Rol: {', '.join(function['roles'] or [])}.", ""]
(ROOT / "docs/api-reference.md").write_text("\n".join(lines))
print(f"Documentadas {len(tables)} tablas y {len(functions)} funciones.")
