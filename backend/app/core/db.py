import json
from contextvars import ContextVar
from pathlib import Path
from uuid import UUID
from psycopg import sql
from psycopg.conninfo import conninfo_to_dict, make_conninfo
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb
from psycopg_pool import ConnectionPool
from .settings import settings

SPECS = {s["name"]: s for s in json.loads((Path(__file__).parents[1] / "operations.json").read_text())}
pools: dict[str, ConnectionPool] = {}
correlation_id = ContextVar("correlation_id", default="")


def start_pools():
    for role in ("api", "auth", "admin", "billing", "worker"):
        dsn = getattr(settings, f"database_{role}_url")
        if settings.db_host:
            params = conninfo_to_dict(dsn)
            params.update(host=settings.db_host, port="5432")
            dsn = make_conninfo(**params)
        pools[role] = ConnectionPool(
            dsn, min_size=1, max_size=8, timeout=5, kwargs={"row_factory": dict_row}, open=True
        )
        pools[role].wait(timeout=10)


def stop_pools():
    for pool in pools.values():
        pool.close()
    pools.clear()


def call(name: str, params: dict | None = None, *, actor=None, role="api"):
    spec = SPECS[name]
    if f"chamba_{role}" not in (spec["roles"] or []):
        raise RuntimeError("Invalid module/database role mapping")
    values = params or {}
    unknown = set(values) - {a["name"][2:] for a in spec["args"]}
    if unknown:
        raise ValueError("Unknown operation fields")
    parts, args = [], []
    for arg in spec["args"]:
        key = arg["name"][2:]
        if key not in values:
            continue
        value = values[key]
        if arg["type"] == "jsonb" and value is not None:
            value = Jsonb(value)
        parts.append(sql.SQL("{} => %s::{}").format(sql.Identifier(arg["name"]), sql.SQL(arg["type"])))
        args.append(value)
    query = sql.SQL("SELECT * FROM api.{}({})").format(sql.Identifier(name), sql.SQL(",").join(parts))
    with pools[role].connection() as conn:
        with conn.transaction():
            if correlation_id.get():
                conn.execute("SELECT set_config('app.correlation_id',%s,true)", (correlation_id.get(),))
            if actor:
                conn.execute("SELECT set_config('app.user_id',%s,true)", (str(actor),))
            rows = conn.execute(query, args).fetchall()
    # TABLE functions have named outputs; scalar/setof jsonb uses function name.
    if spec["returns"] == "record":
        return rows if spec["set"] else (rows[0] if rows else None)
    result = [next(iter(row.values())) for row in rows]
    if spec["returns"] == "void":
        return None
    return result if spec["set"] else (result[0] if result else None)
