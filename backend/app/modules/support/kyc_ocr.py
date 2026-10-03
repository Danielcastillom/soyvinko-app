import io
import re
import calendar
from datetime import datetime
from typing import Optional, Dict, Any
from PIL import Image, ImageOps, ImageEnhance
import subprocess
import shutil

import tempfile
import os

def _run_tesseract(img: Image.Image) -> str:
    """Executes tesseract OCR on the given PIL Image using the system binary."""
    tess_cmd = shutil.which("tesseract") or "/usr/bin/tesseract"
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp_path = tf.name
        img.save(tf, format="PNG")
    try:
        proc = subprocess.run(
            [tess_cmd, tmp_path, "stdout", "-l", "spa"],
            capture_output=True,
            timeout=15,
            check=True
        )
        return proc.stdout.decode("utf-8", errors="replace")
    except Exception:
        try:
            proc = subprocess.run(
                [tess_cmd, tmp_path, "stdout", "-l", "eng"],
                capture_output=True,
                timeout=15,
                check=True
            )
            return proc.stdout.decode("utf-8", errors="replace")
        except Exception:
            return ""
    finally:
        if os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
            except Exception:
                pass

def parse_venezuelan_id(image_bytes: bytes) -> Dict[str, Any]:
    """
    Parses a Venezuelan Cédula de Identidad (anverso) using Tesseract OCR.
    Extracts doc_type (V/E), doc_number, first_name, last_name, full_name,
    birthdate (YYYY-MM-DD), expiration_date, and validates whether the ID is expired.
    """
    result: Dict[str, Any] = {
        "success": False,
        "doc_type": None,
        "doc_number": None,
        "first_name": None,
        "last_name": None,
        "full_name": None,
        "birthdate": None,
        "expiration_date": None,
        "is_expired": False,
        "expiration_message": None,
        "raw_text": "",
    }

    try:
        raw_img = Image.open(io.BytesIO(image_bytes))
        # Correct orientation from phone camera EXIF tags
        img = ImageOps.exif_transpose(raw_img)
        if img.mode != "RGB":
            img = img.convert("RGB")
    except Exception:
        result["expiration_message"] = "No se pudo procesar el archivo de imagen. Usa una foto JPG, PNG o WEBP nítida."
        return result

    # First OCR pass on standard RGB image
    text = _run_tesseract(img)

    # If short text, try grayscale with increased contrast
    if len(text.strip()) < 30:
        gray = ImageOps.grayscale(img)
        enhanced = ImageEnhance.Contrast(gray).enhance(1.6)
        second_text = _run_tesseract(enhanced)
        if len(second_text.strip()) > len(text.strip()):
            text = second_text

    result["raw_text"] = text.strip()

    # 1. Extracción de Tipo y Número de Documento (V o E)
    # Busca patrones como: V 17.265.900, V-17265900, V 17265900, E 82.123.456, V17.265.900
    doc_match = re.search(
        r'\b([VE])\s*[-.:]?\s*([\d]{1,2}(?:[\s.]?\d{3}){2})\b',
        text,
        re.IGNORECASE
    )
    if not doc_match:
        # Fallback para números sin puntos: V 17265900 o V-17265900
        doc_match = re.search(r'\b([VE])\s*[-.:]?\s*(\d{6,9})\b', text, re.IGNORECASE)

    if doc_match:
        result["doc_type"] = doc_match.group(1).upper()
        result["doc_number"] = re.sub(r'\D', '', doc_match.group(2))

    # 2. Extracción de Apellidos y Nombres
    # Las cédulas venezolanas tienen el formato:
    # APELLIDOS <APELLIDO1 APELLIDO2>
    # NOMBRES <NOMBRE1 NOMBRE2>
    # Tolerante a ruido tipográfico de OCR (ej. apeLuDos, apelidos, nomeres, nompres)
    apellidos_match = re.search(
        r'ape[a-z0-9]*dos\s*[:\s]\s*([A-ZÁÉÍÓÚÑa-záéíóúñ\s]+)',
        text,
        re.IGNORECASE
    )
    if apellidos_match:
        raw_last = apellidos_match.group(1).strip().split('\n')[0].strip()
        # Limpiar palabras clave residuales
        cleaned_last = re.sub(r'\b(nombres|nombre|firma|titular|venezolano|director)\b.*$', '', raw_last, flags=re.IGNORECASE).strip()
        if len(cleaned_last) >= 2:
            result["last_name"] = cleaned_last.upper()

    nombres_match = re.search(
        r'nom[a-z0-9]*res\s*[:\s]\s*([A-ZÁÉÍÓÚÑa-záéíóúñ\s]+)',
        text,
        re.IGNORECASE
    )
    if nombres_match:
        raw_first = nombres_match.group(1).strip().split('\n')[0].strip()
        cleaned_first = re.sub(r'\b(apellidos|firma|titular|nacimiento|soltero|casado)\b.*$', '', raw_first, flags=re.IGNORECASE).strip()
        if len(cleaned_first) >= 2:
            result["first_name"] = cleaned_first.upper()

    # Construir Nombre Completo
    fn = result["first_name"]
    ln = result["last_name"]
    if fn and ln:
        raw_full = f"{fn} {ln}"
    elif fn:
        raw_full = fn
    elif ln:
        raw_full = ln
    else:
        raw_full = None

    if raw_full:
        # Formatear capitalizado legible (ej. "Dave Alberto Bastidas")
        result["full_name"] = " ".join(w.capitalize() for w in raw_full.split())

    # 3. Fecha de Nacimiento (DD/MM/YYYY)
    # En cédulas venezolanas aparece encima o al lado de "F. NACIMIENTO"
    dates_found = re.findall(r'\b(\d{2}/\d{2}/\d{4})\b', text)
    if dates_found:
        birth_str = dates_found[0]
        try:
            dt = datetime.strptime(birth_str, '%d/%m/%Y')
            result["birthdate"] = dt.strftime('%Y-%m-%d')
        except Exception:
            pass

    # 4. Fecha de Vencimiento y Validación de Expiración
    # En cédulas venezolanas aparece como MM/YYYY (ej: 08/2032) o DD/MM/YYYY (ej: 08/04/2032)
    # Usualmente precedido o sucedido por "F. VENCIMIENTO" o "VENCIMIENTO"
    exp_date_str: Optional[str] = None
    venc_match = re.search(r'VENCIMIENTO.*?(\d{2}/\d{4}|\d{2}/\d{2}/\d{4})', text, re.DOTALL | re.IGNORECASE)
    if venc_match:
        exp_date_str = venc_match.group(1)
    else:
        # Buscar fechas cerca de la palabra VENCIMIENTO o la última fecha del documento
        all_dates = re.findall(r'\b(\d{2}/\d{4}|\d{2}/\d{2}/\d{4})\b', text)
        if len(all_dates) >= 2:
            exp_date_str = all_dates[-1]

    if exp_date_str:
        result["expiration_date"] = exp_date_str
        try:
            now = datetime.now()
            parts = exp_date_str.split('/')
            if len(parts) == 2:
                m, y = int(parts[0]), int(parts[1])
                # Último día del mes a las 23:59:59
                last_day = calendar.monthrange(y, m)[1]
                exp_dt = datetime(y, m, last_day, 23, 59, 59)
            else:
                exp_dt = datetime.strptime(exp_date_str, '%d/%m/%Y')
                exp_dt = exp_dt.replace(hour=23, minute=59, second=59)

            if exp_dt < now:
                result["is_expired"] = True
                result["expiration_message"] = f"La cédula de identidad está vencida desde {exp_date_str}. Debes presentar un documento vigente."
            else:
                result["is_expired"] = False
        except Exception as e:
            # Si no se pudo parsear con certeza, no bloquear por falso positivo pero registrar
            pass

    if result["doc_number"] and (result["first_name"] or result["last_name"]):
        result["success"] = True

    return result
