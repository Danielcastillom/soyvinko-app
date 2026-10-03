from pathlib import Path
from typing import Literal
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import model_validator

ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=ROOT / ".env", extra="ignore")
    app_env: Literal["development", "test", "staging", "production"] = "development"
    payment_mode: Literal["sandbox", "disabled"] = "disabled"
    jwt_secret: str
    encryption_key: str
    webhook_secret: str = ""
    database_api_url: str
    database_auth_url: str
    database_admin_url: str
    database_billing_url: str
    database_worker_url: str
    db_host: str | None = None
    web_origins: str = "http://127.0.0.1:8180"
    public_web_url: str = "http://127.0.0.1:8180"
    data_dir: Path = ROOT / "data"
    smtp_host: str = ""
    smtp_port: int = 587
    smtp_user: str = ""
    smtp_password: str = ""
    smtp_from: str = ""
    mapbox_public_token: str = ""
    # Pagos simulados en un dominio público: solo con reconocimiento explícito (portal de demostración).
    allow_public_sandbox: bool = False
    # Swagger/OpenAPI; por defecto desactivado en producción.
    api_docs: bool | None = None

    @property
    def origins(self):
        return self.web_origins.split(",")

    @property
    def production(self):
        return self.app_env == "production"

    @property
    def public_deployment(self):
        host = self.public_web_url.split("://")[-1].split("/")[0].split(":")[0]
        return host not in {"127.0.0.1", "localhost", "testserver"}

    @property
    def sandbox_payments(self):
        return (
            self.payment_mode == "sandbox"
            and not self.production
            and (self.allow_public_sandbox or not self.public_deployment)
        )

    @property
    def docs_enabled(self):
        return (not self.production) if self.api_docs is None else self.api_docs

    @property
    def secure_cookies(self):
        return self.public_web_url.startswith("https://")

    @model_validator(mode="after")
    def validate_deployment(self):
        if self.mapbox_public_token and not self.mapbox_public_token.startswith("pk."):
            raise ValueError("MAPBOX_PUBLIC_TOKEN debe ser un token público pk.")
        if len(self.jwt_secret) < 48:
            raise ValueError("JWT_SECRET debe contener al menos 48 caracteres aleatorios")
        if self.app_env in {"staging", "production"} and not self.secure_cookies:
            raise ValueError("Los despliegues públicos requieren HTTPS")
        if self.production and self.payment_mode == "sandbox":
            raise ValueError("Producción requiere proveedor real o pagos deshabilitados")
        return self


settings = Settings()
