import hashlib
import threading
from datetime import datetime, timedelta, timezone
from uuid import UUID
import jwt
from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from argon2 import PasswordHasher
from argon2.exceptions import VerificationError, InvalidHashError
from cryptography.fernet import Fernet
from .settings import settings
from .db import call

class _BoundedPasswordHasher(PasswordHasher):
    """Argon2 usa 64 MB por hash; limitamos la concurrencia para no agotar memoria."""

    _slots = threading.BoundedSemaphore(4)

    def hash(self, password, **kwargs):
        with self._slots:
            return super().hash(password, **kwargs)

    def verify(self, hash, password):
        with self._slots:
            return super().verify(hash, password)


passwords = _BoundedPasswordHasher(time_cost=3, memory_cost=65536, parallelism=2)
dummy_hash = passwords.hash("random-dummy-password-no-user")
cipher = Fernet(settings.encryption_key.encode())
bearer = HTTPBearer(auto_error=False)


def digest(token: str):
    return hashlib.sha256(token.encode()).hexdigest()


def verify_password(encoded, password):
    try:
        return passwords.verify(encoded, password)
    except (VerificationError, InvalidHashError):
        return False


def access_token(user, session):
    now = datetime.now(timezone.utc)
    return jwt.encode(
        {
            "sub": str(user),
            "sid": str(session),
            "iss": "chamba-api",
            "aud": "chamba-clients",
            "iat": now,
            "exp": now + timedelta(minutes=15),
            "type": "access",
        },
        settings.jwt_secret,
        algorithm="HS256",
    )


def _token_principal(token: HTTPAuthorizationCredentials | None, scopes: set[str | None]):
    if not token:
        raise HTTPException(401, "Inicia sesión para continuar.")
    try:
        payload = jwt.decode(
            token.credentials,
            settings.jwt_secret,
            algorithms=["HS256"],
            audience="chamba-clients",
            issuer="chamba-api",
            options={"require": ["exp", "sub", "sid", "iat"]},
        )
        if payload.get("type") != "access" or payload.get("scope") not in scopes:
            raise ValueError()
        user, session = UUID(payload["sub"]), UUID(payload["sid"])
    except (jwt.PyJWTError, ValueError, KeyError):
        raise HTTPException(401, "Sesión no válida o vencida.") from None
    result = call("auth_principal", {"session": session, "user": user}, role="auth")
    if not result:
        raise HTTPException(401, "Tu sesión ha terminado. Vuelve a iniciar sesión.")
    result["session_id"] = str(session)
    return result


def principal(token: HTTPAuthorizationCredentials | None = Depends(bearer)):
    # Los tokens con alcance restringido (p. ej. traspaso KYC al móvil) no abren el resto de la API.
    return _token_principal(token, {None})


def kyc_principal(token: HTTPAuthorizationCredentials | None = Depends(bearer)):
    return _token_principal(token, {None, "kyc_mobile"})


def admin_principal(user=Depends(principal)):
    if not set(user["roles"]) & {"admin", "superadmin", "support"}:
        raise HTTPException(403, "No tienes permisos administrativos.")
    return user


def require_permission(user, permission):
    if permission not in user["permissions"]:
        raise HTTPException(403, "No tienes permiso para esta operación.")


def limit(request: Request, action: str, maximum=30, seconds=60, subject=""):
    # No confiamos en X-Forwarded-For entregado por el cliente.
    ip = request.client.host if request.client else "unknown"
    bucket = digest(f"{action}:{ip}:{subject}")
    if not call("auth_rate_limit", {"bucket": bucket, "limit": maximum, "seconds": seconds}, role="auth"):
        raise HTTPException(429, "Demasiados intentos. Espera antes de volver a intentar.")
