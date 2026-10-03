"""Boundary for trusted payment results. Sandbox routes never exist in production."""

import hashlib
import hmac
import json
import re
import time
from typing import Literal, Protocol
from datetime import datetime, timezone
from uuid import UUID, uuid4
from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, ConfigDict, Field
from app.core.db import call
from app.core.security import principal, admin_principal, require_permission, cipher, limit
from app.core.settings import settings

router = APIRouter(prefix="/api/v1/integrations", tags=["integrations"])


class PayoutProvider(Protocol):
    def settle_topup(self, record: dict) -> UUID: ...
    def settle_purchase(self, record: dict) -> UUID: ...


class SandboxProvider:
    def settle_topup(self, record):
        ident = str(record["id"])
        return call(
            "confirm_topup",
            {
                "topup": ident,
                "provider": "sandbox",
                "event": f"topup:{ident}",
                "payment": f"sandbox:topup:{ident}",
                "amount": record["amount_minor"],
                "currency": record["currency"],
                "hash": hashlib.sha256(ident.encode()).hexdigest(),
            },
            role="billing",
        )

    def settle_purchase(self, record):
        ident = str(record["id"])
        return call(
            "confirm_purchase",
            {
                "purchase": ident,
                "provider": "sandbox",
                "event": f"purchase:{ident}",
                "payment": f"sandbox:purchase:{ident}",
                "amount": record["amount_minor"],
                "currency": record["currency"],
                "payload_hash": hashlib.sha256(ident.encode()).hexdigest(),
            },
            role="billing",
        )


def sandbox():
    if not settings.sandbox_payments:
        raise HTTPException(503, "payment_provider_unavailable")
    return SandboxProvider()


VENEZUELAN_BANKS = {
    "0102": "Banco de Venezuela",
    "0104": "Venezolano de Crédito",
    "0105": "Mercantil Banco",
    "0108": "BBVA Provincial",
    "0114": "Bancaribe",
    "0115": "Banco Exterior",
    "0128": "Banco Caroní",
    "0134": "Banesco",
    "0137": "Banco Sofitasa",
    "0138": "Banco Plaza",
    "0151": "BFC Banco Fondo Común",
    "0156": "100% Banco",
    "0157": "DelSur",
    "0163": "Banco del Tesoro",
    "0166": "Banco Agrícola de Venezuela",
    "0168": "Bancrecer",
    "0169": "Mi Banco",
    "0171": "Banco Activo",
    "0172": "Bancamiga",
    "0174": "Banplus",
    "0175": "Banco Bicentenario",
    "0177": "BANFANB",
    "0191": "Banco Nacional de Crédito (BNC)",
}


class Destination(BaseModel):
    model_config = ConfigDict(extra="ignore")
    method_type: Literal["bank_transfer", "pago_movil", "sandbox"] | None = None
    currency: str = Field(pattern="^(VES|USD)$")
    label: str | None = Field(default=None, max_length=120)

    # Transferencia bancaria
    bank_code: str | None = None
    account_number: str | None = None
    account_type: Literal["corriente", "ahorro"] | None = "corriente"

    # Pago Móvil
    phone_prefix: str | None = None
    phone_number: str | None = None

    # Identificación del titular
    holder_name: str | None = None
    document_type: Literal["V", "E", "J", "G"] | None = "V"
    document_number: str | None = None


def _verified_identities(user_id) -> set[str]:
    """Documentos (p. ej. "V-12345678") respaldados por un KYC aprobado y vigente del usuario."""
    now = datetime.now(timezone.utc)
    approved = [
        case
        for case in call("my_kyc", actor=user_id)
        if case.get("status") == "approved"
        and (not case.get("expires_at") or datetime.fromisoformat(case["expires_at"]) > now)
    ]
    if not approved:
        return set()
    docs = set()
    for case in approved:
        try:
            meta = json.loads(case.get("external_reference") or "")
        except (TypeError, ValueError):
            continue
        if isinstance(meta, dict) and meta.get("document_number"):
            number = re.sub(r"\D", "", str(meta["document_number"]))
            docs.add(f"{str(meta.get('document_type') or 'V').upper()}-{number}")
    # El RIF de la cuenta (empresa o persona) cuenta cuando ya hay identidad aprobada.
    rif = (call("my_account", actor=user_id) or {}).get("rif")
    if rif:
        match = re.match(r"^([VEJGP])-?(\d{5,12})", rif.upper().replace(" ", ""))
        if match:
            docs.add(f"{match.group(1)}-{match.group(2)}")
    return docs


def _masked_document(doc_type: str, doc_num: str) -> str:
    return f"{doc_type}-****{doc_num[-4:]}"


@router.post("/payout-methods")
def add_destination(body: Destination, request: Request, user=Depends(principal)):
    limit(request, "payout-method", 5, 3600, str(user["id"]))

    is_bank = body.method_type == "bank_transfer" or (body.method_type is None and bool(body.account_number))
    is_pago_movil = body.method_type == "pago_movil" or (body.method_type is None and bool(body.phone_number) and not body.account_number)

    if is_bank:
        clean_acc = re.sub(r"\D", "", body.account_number or "")
        if len(clean_acc) != 20:
            raise HTTPException(422, "El número de cuenta bancaria debe tener exactamente 20 dígitos.")
        bank_code = body.bank_code or clean_acc[:4]
        if bank_code not in VENEZUELAN_BANKS:
            raise HTTPException(422, f"Código bancario '{bank_code}' no reconocido o no soportado.")
        if not clean_acc.startswith(bank_code):
            raise HTTPException(422, "Los primeros 4 dígitos de la cuenta no coinciden con el banco seleccionado.")
        doc_type = (body.document_type or "V").upper()
        doc_num = re.sub(r"\D", "", body.document_number or "")
        if not (5 <= len(doc_num) <= 12):
            raise HTTPException(422, "Número de documento de identidad inválido (debe tener entre 5 y 12 dígitos).")
        holder = (body.holder_name or "").strip()
        if len(holder) < 3:
            raise HTTPException(422, "Indica el nombre completo o razón social del titular de la cuenta.")
        bank_name = VENEZUELAN_BANKS[bank_code]
        acc_type_label = "Ahorro" if body.account_type == "ahorro" else "Corriente"
        provider = "ve_bank"
        token = "ve_bank:enc:" + cipher.encrypt(f"{bank_code}:{clean_acc}".encode()).decode()
        label = body.label.strip() if (body.label and body.label.strip()) else f"{bank_name} ({acc_type_label})"
        masked = f"{clean_acc[:4]} •••• {clean_acc[-4:]} · {_masked_document(doc_type, doc_num)}"
        currency = body.currency or "VES"

    elif is_pago_movil:
        bank_code = (body.bank_code or "").strip()
        if bank_code not in VENEZUELAN_BANKS:
            raise HTTPException(422, "Selecciona un banco venezolano válido para Pago Móvil.")
        raw_phone = f"{body.phone_prefix or ''}{body.phone_number or ''}" if body.phone_prefix else (body.phone_number or "")
        clean_phone = re.sub(r"\D", "", raw_phone)
        if not re.match(r"^(0414|0424|0412|0416|0426)\d{7}$", clean_phone):
            raise HTTPException(422, "El teléfono para Pago Móvil debe comenzar con 0414, 0424, 0412, 0416 o 0426 y tener 11 dígitos.")
        doc_type = (body.document_type or "V").upper()
        doc_num = re.sub(r"\D", "", body.document_number or "")
        if not (5 <= len(doc_num) <= 12):
            raise HTTPException(422, "Número de documento de identidad inválido (debe tener entre 5 y 12 dígitos).")
        holder = (body.holder_name or "").strip()
        if len(holder) < 3:
            raise HTTPException(422, "Indica el nombre completo o razón social del titular de Pago Móvil.")
        bank_name = VENEZUELAN_BANKS[bank_code]
        provider = "pago_movil"
        token = "pago_movil:enc:" + cipher.encrypt(f"{bank_code}:{clean_phone}".encode()).decode()
        label = body.label.strip() if (body.label and body.label.strip()) else f"Pago Móvil · {bank_name}"
        masked = f"{clean_phone[:4]} •••• {clean_phone[-3:]} · {_masked_document(doc_type, doc_num)}"
        currency = "VES"

    else:
        # Modo sandbox / cuenta de prueba
        sandbox()
        provider = "sandbox"
        token = "sandbox-destination:" + str(uuid4())
        label = (body.label or "").strip() or "Cuenta de prueba"
        masked = "PRUEBA •••• 0000"
        currency = body.currency

    if provider != "sandbox" and not user.get("email_verified"):
        raise HTTPException(
            403, "Confirma tu correo electrónico antes de registrar una cuenta para recibir retiros."
        )
    if provider != "sandbox" and f"{doc_type}-{doc_num}" not in _verified_identities(user["id"]):
        # Una cuenta robada no puede desviar retiros a una cuenta bancaria de terceros.
        raise HTTPException(
            409,
            "El titular de la cuenta debe ser tú: su documento tiene que coincidir con tu verificación de "
            "identidad aprobada. Completa o espera la aprobación de tu KYC e inténtalo de nuevo.",
        )

    ident = call(
        "add_payout_method",
        {
            "provider": provider,
            "token": token,
            "label": label[:80],
            "masked": masked[:80],
            "currency": currency,
        },
        actor=user["id"],
    )
    call(
        "verify_payout_method",
        {"method": ident, "owner": user["id"], "provider": provider, "token": token},
        role="billing",
    )
    return {"data": ident}


@router.post("/sandbox/withdrawals/{reference}/settle")
def settle_withdrawal(reference: UUID, user=Depends(admin_principal)):
    sandbox()
    require_permission(user, "money.configure")
    payout = call("prepare_withdrawal", {"withdrawal": reference}, role="billing")
    if payout["provider"] != "sandbox":
        raise HTTPException(409, "El destino no pertenece al proveedor de prueba.")
    call(
        "confirm_withdrawal",
        {
            "withdrawal": reference,
            "provider": "sandbox",
            "event": f"withdrawal:{reference}",
            "payment": f"sandbox:withdrawal:{reference}",
            "result": "paid",
            "net": payout["net_minor"],
            "currency": payout["currency"],
            "hash": hashlib.sha256(str(reference).encode()).hexdigest(),
        },
        role="billing",
    )
    return {"data": {"status": "paid", "mode": "sandbox"}}


@router.post("/sandbox/{kind}/{reference}/settle")
def settle(kind: Literal["topup", "purchase"], reference: UUID, request: Request, user=Depends(principal)):
    provider = sandbox()
    limit(request, "sandbox", 20, 60, user["id"])
    records = call("my_topups" if kind == "topup" else "my_credit_purchases", actor=user["id"])
    record = next((r for r in records if r["id"] == str(reference)), None)
    if not record:
        raise HTTPException(404, "Operación no encontrada.")
    result = provider.settle_topup(record) if kind == "topup" else provider.settle_purchase(record)
    return {"data": {"id": result, "mode": "sandbox"}}


class Webhook(BaseModel):
    model_config = ConfigDict(extra="forbid")
    kind: Literal["topup_settled", "purchase_settled"]
    reference: UUID
    event: str = Field(min_length=1, max_length=200)
    payment: str = Field(min_length=1, max_length=200)
    amount: int = Field(gt=0, le=9_000_000_000_000)
    currency: str = Field(pattern="^[A-Z]{3}$")


@router.post("/webhooks/sandbox")
async def webhook(request: Request):
    sandbox()
    length = request.headers.get("content-length", "0")
    if not length.isdigit() or int(length) > 16384:
        raise HTTPException(413, "Evento demasiado grande.")
    raw = await request.body()
    if len(raw) > 16384:
        raise HTTPException(413, "Evento demasiado grande.")
    timestamp = request.headers.get("x-chamba-timestamp", "")
    signature = request.headers.get("x-chamba-signature", "")
    if not timestamp.isdigit() or abs(time.time() - int(timestamp)) > 300 or not settings.webhook_secret:
        raise HTTPException(401, "Firma de evento inválida.")
    expected = hmac.new(
        settings.webhook_secret.encode(), timestamp.encode() + b"." + raw, hashlib.sha256
    ).hexdigest()
    if not hmac.compare_digest(expected, signature):
        raise HTTPException(401, "Firma de evento inválida.")
    try:
        body = Webhook.model_validate_json(raw)
    except ValueError:
        raise HTTPException(422, "Evento inválido.") from None
    params = {
        "provider": "sandbox",
        "event": body.event,
        "payment": body.payment,
        "amount": body.amount,
        "currency": body.currency,
    }
    sha = hashlib.sha256(raw).hexdigest()
    if body.kind == "topup_settled":
        params.update(topup=body.reference, hash=sha)
        result = call("confirm_topup", params, role="billing")
    else:
        params.update(purchase=body.reference, payload_hash=sha)
        result = call("confirm_purchase", params, role="billing")
    return {"data": {"id": result}}


class Delivery(BaseModel):
    model_config = ConfigDict(extra="forbid")
    name: str = Field(min_length=2, max_length=120)
    phone: str = Field(min_length=3, max_length=40)
    address: str = Field(min_length=5, max_length=1000)


@router.post("/orders/{order}/delivery")
def share_delivery(order: UUID, body: Delivery, user=Depends(principal)):
    call(
        "share_order_delivery",
        {
            "order": order,
            "name": body.name,
            "phone": body.phone,
            "address_ciphertext": cipher.encrypt(body.address.encode()),
            "key_id": "app-v1",
        },
        actor=user["id"],
    )
    return {"data": None}


@router.get("/orders/{order}/delivery")
def delivery(order: UUID, user=Depends(principal)):
    result = call("order_delivery", {"order": order}, actor=user["id"])
    if result and result.get("address_ciphertext"):
        encoded = result.pop("address_ciphertext")
        result["address"] = cipher.decrypt(bytes.fromhex(encoded.removeprefix("\\x"))).decode()
    return {"data": result}
