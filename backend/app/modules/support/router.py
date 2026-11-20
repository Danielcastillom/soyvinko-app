import base64
from datetime import datetime, timedelta, timezone
import hashlib
import io
import json
import re
from typing import Literal
from uuid import uuid4
from fastapi import Depends, File, Form, HTTPException, Request, UploadFile
import jwt
from PIL import Image, UnidentifiedImageError
import qrcode
from app.core.db import call
from app.core.routing import mount
from app.core.security import kyc_principal, limit, principal
from app.core.settings import settings

import logging

logger = logging.getLogger(__name__)
router = mount("support")

MAX_BYTES = 10 * 1024 * 1024


from app.modules.support.kyc_ocr import parse_venezuelan_id

def _save_kyc_file(file_or_bytes: UploadFile | bytes, user_id: str, default_name: str = "doc.jpg") -> str:
    if isinstance(file_or_bytes, bytes):
        raw = file_or_bytes
        filename = default_name
    else:
        raw = file_or_bytes.file.read(MAX_BYTES + 1)
        filename = file_or_bytes.filename or default_name

    if not raw or len(raw) > MAX_BYTES:
        raise HTTPException(413, f"El archivo {filename} excede el límite de 10 MB.")
    if raw.startswith(b"%PDF-"):
        mime, extension, clean = "application/pdf", ".pdf", raw
    else:
        try:
            source = Image.open(io.BytesIO(raw))
            if (
                source.format not in ("JPEG", "PNG", "WEBP")
                or source.width * source.height > Image.MAX_IMAGE_PIXELS
            ):
                raise ValueError()
            source.load()
            output = io.BytesIO()
            source.convert("RGB").save(output, format="JPEG", quality=90)
            clean, mime, extension = output.getvalue(), "image/jpeg", ".jpg"
        except (
            UnidentifiedImageError,
            OSError,
            ValueError,
            Image.DecompressionBombError,
            Image.DecompressionBombWarning,
        ):
            raise HTTPException(415, "Formato de imagen no soportado. Usa JPG, PNG o WEBP.") from None

    key = str(uuid4()) + extension
    folder = settings.data_dir / "files"
    folder.mkdir(parents=True, exist_ok=True, mode=0o700)
    path = folder / key
    path.touch(mode=0o600)
    path.write_bytes(clean)

    ident = call(
        "register_upload",
        {
            "owner": user_id,
            "object_key": key,
            "mime": mime,
            "bytes": len(clean),
            "sha256": hashlib.sha256(clean).hexdigest(),
            "purpose": "kyc",
            "exif_removed": True,
        },
        role="worker",
    )
    return ident


@router.post("/api/v1/support/kyc-ocr-scan")
def kyc_ocr_scan(
    request: Request,
    file: UploadFile = File(...),
    user=Depends(kyc_principal),
):
    """Realiza un pre-escaneo OCR Tesseract de la cédula venezolana para retroalimentación instantánea."""
    # Síncrona a propósito: Tesseract y PIL bloquean; FastAPI la ejecuta en el threadpool.
    limit(request, "kyc-ocr", 10, 300, str(user["id"]))
    content = file.file.read(MAX_BYTES + 1)
    if not content or len(content) > MAX_BYTES:
        raise HTTPException(413, "El archivo seleccionado supera el límite de 10 MB.")

    ocr_result = parse_venezuelan_id(content)
    return {"data": ocr_result}


@router.post("/api/v1/support/kyc-personal")
def submit_personal_kyc(
    request: Request,
    identity_front: UploadFile = File(...),
    selfie: UploadFile = File(...),
    phone: str = Form(...),
    address: str | None = Form(default=None),
    document_type: Literal["V", "E", "P"] = Form("V"),
    document_number: str = Form(default=""),
    full_name: str = Form(default=""),
    birthdate: str = Form(default=""),
    proof_of_address: UploadFile | None = File(default=None),
    user=Depends(kyc_principal),
):
    """
    Recibe la cédula venezolana (anverso) y selfie con documento.
    Ejecuta OCR con Tesseract, valida vigencia (rechaza si está vencido),
    actualiza los datos oficiales del usuario y registra el expediente.
    """
    # 1. Leer bytes de la cédula frontal para OCR y guardado
    limit(request, "kyc-personal", 5, 3600, str(user["id"]))
    front_bytes = identity_front.file.read(MAX_BYTES + 1)
    if not front_bytes or len(front_bytes) > MAX_BYTES:
        raise HTTPException(413, "El archivo de la cédula excede el límite de 10 MB.")

    ocr_result = parse_venezuelan_id(front_bytes)

    # 2. Validación estricta de vencimiento
    if ocr_result.get("is_expired"):
        msg = ocr_result.get("expiration_message") or (
            f"El documento de identidad está vencido (Fecha: {ocr_result.get('expiration_date')}). "
            "Debes presentar una cédula vigente para poder verificar tu cuenta."
        )
        raise HTTPException(422, msg)

    # 3. Consolidar datos oficiales (prioridad OCR si fue exitoso)
    final_doc_type = ocr_result.get("doc_type") or document_type or "V"
    raw_doc_num = ocr_result.get("doc_number") or re.sub(r"\D", "", document_number)
    final_doc_num = re.sub(r"\D", "", raw_doc_num)

    if not (5 <= len(final_doc_num) <= 12):
        raise HTTPException(
            422,
            "No se detectó un número de cédula válido en la foto. "
            "Asegúrate de que la foto sea nítida, iluminada y sin reflejos.",
        )

    final_name = ocr_result.get("full_name") or full_name.strip()
    if len(final_name) < 3:
        raise HTTPException(
            422,
            "No se pudo leer el nombre completo del titular en el documento. "
            "Verifica la calidad y nitidez de la foto.",
        )

    final_birthdate = ocr_result.get("birthdate") or birthdate.strip()
    if final_birthdate and not re.match(r"^\d{4}-\d{2}-\d{2}$", final_birthdate):
        final_birthdate = None

    clean_phone = phone.strip()
    if len(clean_phone) < 7:
        raise HTTPException(422, "Ingresa un número de teléfono de contacto válido.")

    # 4. Actualizar datos oficiales en la cuenta del usuario para uso en todo el sistema
    official_rif = f"{final_doc_type}-{final_doc_num}"
    try:
        call(
            "update_account",
            {
                "name": final_name,
                "rif": official_rif,
            },
            actor=user["id"],
        )
    except Exception as exc:
        logger.warning(f"Error updating user account profile: {exc}")

    # 5. Guardar archivos procesados (sanitizados sin EXIF)
    front_id = _save_kyc_file(front_bytes, user["id"], identity_front.filename or "cedula_anverso.jpg")
    selfie_id = _save_kyc_file(selfie, user["id"], selfie.filename or "selfie.jpg")
    proof_id = (
        _save_kyc_file(proof_of_address, user["id"], proof_of_address.filename or "comprobante.jpg")
        if proof_of_address
        else None
    )

    metadata = {
        "id": str(uuid4()),
        "document_type": final_doc_type,
        "document_number": final_doc_num,
        "full_name": final_name,
        "birthdate": final_birthdate,
        "expiration_date": ocr_result.get("expiration_date"),
        "is_expired": False,
        "phone": clean_phone,
        "address": address.strip() if address else None,
        "account_type": "natural",
        "ocr_success": ocr_result.get("success", False),
    }

    # 6. Crear caso KYC en la base de datos
    case_id = call(
        "submit_kyc",
        {
            "provider": "personal_ve",
            "external_reference": json.dumps(metadata),
        },
        actor=user["id"],
    )

    # 7. Asociar archivos al expediente
    call(
        "attach_kyc_file",
        {"case": case_id, "file": front_id, "kind": "identity_front"},
        actor=user["id"],
    )

    call(
        "attach_kyc_file",
        {"case": case_id, "file": selfie_id, "kind": "selfie"},
        actor=user["id"],
    )

    if proof_id:
        call(
            "attach_kyc_file",
            {"case": case_id, "file": proof_id, "kind": "proof_of_address"},
            actor=user["id"],
        )

    return {
        "data": {
            "case_id": case_id,
            "status": "pending",
            "metadata": metadata,
            "ocr": ocr_result,
        }
    }


@router.get("/api/v1/support/kyc-mobile-session")
def get_kyc_mobile_session(user=Depends(principal)):
    """Generates a secure, short-lived mobile handover token and QR code for KYC mobile verification."""
    now = datetime.now(timezone.utc)
    token = jwt.encode(
        {
            "sub": str(user["id"]),
            "sid": str(user["session_id"]),
            "iss": "chamba-api",
            "aud": "chamba-clients",
            "iat": now,
            "exp": now + timedelta(minutes=15),
            "type": "access",
            "scope": "kyc_mobile",
        },
        settings.jwt_secret,
        algorithm="HS256",
    )
    # El token va en el fragmento (#): el navegador no lo envía al servidor ni aparece en logs o Referer.
    mobile_url = f"{settings.public_web_url.rstrip('/')}/verificacion-movil#token={token}"

    qr_img = qrcode.make(mobile_url, box_size=8, border=2)
    buf = io.BytesIO()
    qr_img.save(buf, format="PNG")
    b64 = base64.b64encode(buf.getvalue()).decode("ascii")
    qr_data_url = f"data:image/png;base64,{b64}"

    return {
        "data": {
            "token": token,
            "url": mobile_url,
            "qr_data_url": qr_data_url,
            "expires_at": (now + timedelta(minutes=15)).isoformat(),
        }
    }



