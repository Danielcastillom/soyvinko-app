from datetime import datetime, date, time
from typing import Any, Annotated
from uuid import UUID
from fastapi import APIRouter, Body, Depends, Query
from pydantic import BeforeValidator, ConfigDict, Field, create_model
from .profile import normalize_professional_slug
from .db import SPECS, call
from .security import principal, admin_principal

TYPES = {
    "uuid": UUID,
    "text": str,
    "integer": int,
    "bigint": int,
    "smallint": int,
    "boolean": bool,
    "numeric": float,
    "double precision": float,
    "timestamp with time zone": datetime,
    "date": date,
    "time without time zone": time,
    "uuid[]": list[UUID],
    "jsonb": Any,
    "bytea": bytes,
}


def mount(module: str):
    router = APIRouter(tags=[module])
    for name, spec in SPECS.items():
        if spec["module"] != module:
            continue
        fields = {}
        for a in spec["args"]:
            key = a["name"][2:]
            if name == "activate_professional" and key == "slug":
                fields[key] = (
                    Annotated[str, BeforeValidator(normalize_professional_slug)],
                    Field(min_length=1, max_length=80, pattern=r"^[a-z0-9]+(-[a-z0-9]+)*$"),
                )
                continue
            typ = TYPES[a["type"]]
            kwargs = {}
            if typ is str:
                kwargs["max_length"] = 10000
            if key == "currency" and spec["method"] == "POST" and name != "report_external_payment":
                kwargs["pattern"] = "^(VES|USD)$"
            if key == "limit":
                kwargs.update(ge=1, le=100)
            if key == "offset":
                kwargs["ge"] = 0
            if key in ("lon", "lat"):
                kwargs.update(ge=-180 if key == "lon" else -90, le=180 if key == "lon" else 90)
            # NULL is deliberate for optional SQL fields; omitted fields use SQL defaults.
            fields[key] = (typ | None, Field(default=... if a["required"] else None, **kwargs))
        model = create_model(
            "".join(w.title() for w in name.split("_")) + "Input",
            __config__=ConfigDict(extra="forbid"),
            **fields,
        )
        dependency = admin_principal if module == "admin" else principal

        def build_handler(fn, mdl, method, group, dep):
            if method == "GET":

                def endpoint(payload: mdl = Query(), user=Depends(dep)):
                    return {
                        "data": call(
                            fn,
                            payload.model_dump(exclude_unset=True),
                            actor=user["id"],
                            role="admin" if group == "admin" else "api",
                        )
                    }
            else:

                def endpoint(payload: mdl = Body(), user=Depends(dep)):
                    return {
                        "data": call(
                            fn,
                            payload.model_dump(exclude_unset=True),
                            actor=user["id"],
                            role="admin" if group == "admin" else "api",
                        )
                    }

            if group == "public":
                if method == "POST":
                    def endpoint(payload: mdl = Body()):
                        return {"data": call(fn, payload.model_dump(exclude_unset=True))}
                else:
                    def endpoint(payload: mdl = Query()):
                        return {"data": call(fn, payload.model_dump(exclude_unset=True))}

            endpoint.__name__ = fn
            return endpoint

        router.add_api_route(
            spec["path"],
            build_handler(name, model, spec["method"], module, dependency),
            methods=[spec["method"]],
            operation_id=name,
            summary=name.replace("_", " ").capitalize(),
        )
    return router
