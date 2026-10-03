import hashlib
import io
from pathlib import Path
from typing import Literal
from uuid import UUID, uuid4
from fastapi import APIRouter, Depends, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import FileResponse
from PIL import Image, UnidentifiedImageError
from app.core.db import call
from app.core.security import principal, admin_principal, limit
from app.core.settings import settings

router = APIRouter(prefix="/api/v1/files", tags=["files"])
Image.MAX_IMAGE_PIXELS = 20_000_000
MAX_BYTES = 10 * 1024 * 1024


@router.post("/upload", status_code=201)
def upload(
    request: Request,
    file: UploadFile = File(),
    purpose: Literal["avatar", "portfolio", "request", "chat", "product", "kyc", "evidence"] = Form(),
    user=Depends(principal),
):
    limit(request, "upload", 60, 3600, str(user["id"]))
    raw = file.file.read(MAX_BYTES + 1)
    if not raw or len(raw) > MAX_BYTES:
        raise HTTPException(413, "El archivo debe tener entre 1 byte y 10 MB.")
    if raw.startswith(b"%PDF-") and purpose in ("kyc", "evidence", "chat"):
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
            raise HTTPException(415, "Usa imágenes JPG, PNG o WEBP; PDF solo para documentos.") from None
    key = str(uuid4()) + extension
    folder = settings.data_dir / "files"
    folder.mkdir(parents=True, exist_ok=True, mode=0o700)
    path = folder / key
    path.touch(mode=0o600)
    path.write_bytes(clean)
    try:
        ident = call(
            "register_upload",
            {
                "owner": user["id"],
                "object_key": key,
                "mime": mime,
                "bytes": len(clean),
                "sha256": hashlib.sha256(clean).hexdigest(),
                "purpose": purpose,
                "exif_removed": True,
            },
            role="worker",
        )
    except Exception:
        path.unlink(missing_ok=True)
        raise
    return {"data": {"id": ident, "mime_type": mime, "bytes": len(clean), "moderation_status": "pending"}}


@router.get("/{file_id}")
def download(file_id: UUID, user=Depends(principal)):
    key = call("file_object_key", {"file": file_id}, actor=user["id"])
    if not key:
        raise HTTPException(404, "Archivo no disponible.")
    folder = (settings.data_dir / "files").resolve()
    path = (folder / key).resolve()
    if path.parent != folder or not path.is_file():
        raise HTTPException(404, "Archivo no disponible.")
    return FileResponse(
        path,
        filename=f"{file_id}{path.suffix}",
        headers={
            "Cache-Control": "private, no-store",
            "X-Content-Type-Options": "nosniff",
            "Content-Security-Policy": "sandbox; default-src 'none'",
        },
    )


from pydantic import BaseModel, Field
import base64


class ModerationAccess(BaseModel):
    reason: str = Field(min_length=10, max_length=1000)


@router.post("/moderation/{file_id}")
def moderation_download(file_id: UUID, body: ModerationAccess, user=Depends(admin_principal)):
    key = call("admin_file_key", {"file": file_id, "reason": body.reason}, actor=user["id"], role="admin")
    if not key:
        raise HTTPException(404, "Archivo no disponible.")
    folder = (settings.data_dir / "files").resolve()
    path = (folder / key).resolve()
    if path.parent != folder or not path.is_file():
        raise HTTPException(404, "Archivo no disponible.")
    return {
        "data": {
            "content": base64.b64encode(path.read_bytes()).decode(),
            "mime": "application/pdf" if path.suffix == ".pdf" else "image/jpeg",
        }
    }
