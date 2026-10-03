import importlib
import logging
import time
from contextlib import asynccontextmanager
from uuid import uuid4
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.middleware.trustedhost import TrustedHostMiddleware
from app.core.db import start_pools, stop_pools, pools, correlation_id
from app.core.errors import install_handlers
from app.core.settings import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")


@asynccontextmanager
async def lifespan(app):
    start_pools()
    yield
    stop_pools()


app = FastAPI(
    title="Chamba API",
    version="1.0.0",
    description="Portal de profesionales. Importes en unidades menores; créditos y dinero separados.",
    docs_url="/api/docs" if settings.docs_enabled else None,
    openapi_url="/api/openapi.json" if settings.docs_enabled else None,
    redoc_url="/api/redoc" if settings.docs_enabled else None,
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-CSRF-Token"],
    expose_headers=["X-Request-ID"],
)
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["127.0.0.1", "localhost", "api", "testserver"]
    + [origin.split("://")[-1].split(":")[0] for origin in settings.origins],
)


@app.middleware("http")
async def metadata(request: Request, call_next):
    request.state.request_id = str(uuid4())
    correlation_id.set(request.state.request_id)
    length = request.headers.get("content-length")
    chunked_body = length is None and "chunked" in request.headers.get("transfer-encoding", "").lower()
    # Sin Content-Length (chunked) el límite se saltaría: exigimos tamaño declarado.
    if chunked_body or (length is not None and (not length.isdigit() or int(length) > 11 * 1024 * 1024)):
        return JSONResponse(
            status_code=413,
            content={
                "error": {"code": "body_too_large", "message": "La solicitud excede el tamaño permitido."}
            },
        )
    started = time.monotonic()
    response = await call_next(request)
    response.headers["X-Request-ID"] = request.state.request_id
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "same-origin"
    response.headers["Cache-Control"] = "no-store"
    # Log path (never query strings, payloads, cookies or tokens).
    logging.getLogger("chamba.http").info(
        "%s %s %s %.0fms id=%s",
        request.method,
        request.url.path,
        response.status_code,
        (time.monotonic() - started) * 1000,
        request.state.request_id,
    )
    return response


install_handlers(app)
for module in [
    "auth",
    "public",
    "account",
    "professionals",
    "requests",
    "contracts",
    "wallet",
    "marketplace",
    "support",
    "admin",
    "files",
    "integrations",
]:
    app.include_router(importlib.import_module(f"app.modules.{module}.router").router)


@app.get("/api/health", tags=["system"])
def health():
    with pools["api"].connection() as conn:
        conn.execute("SELECT 1")
    return {"status": "ok", "version": "1.0.0", "database": "connected"}


@app.get("/api/v1/config", tags=["system"])
def config():
    vinko_rates = {}
    try:
        with pools["api"].connection() as conn:
            row = conn.execute("SELECT api.get_vinko_exchange_rates()").fetchone()
            if row:
                vinko_rates = next(iter(row.values())) or {}
    except Exception:
        pass
    return {
        "data": {
            "environment": settings.app_env,
            "payment_mode": "sandbox" if settings.sandbox_payments else "disabled",
            "currency": "VES",
            "currencies": [{"code": "VES", "label": "Bolívares", "symbol": "Bs"}, {"code": "USD", "label": "Dólares", "symbol": "$"}],
            "countries": [{"code": "VE", "name": "Venezuela", "active": True}],
            "locale": "es-VE",
            "timezone": "America/Caracas",
            "mapbox_public_token": settings.mapbox_public_token,
            "money_minor_factor": 100,
            "withdrawal_fee_only": True,
            "welcome_credits": 3,
            "vinko_rates": vinko_rates,
            "api_version": "v1",
        }
    }
