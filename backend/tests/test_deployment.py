"""Public deployments keep HTTPS sessions without enabling real payments."""
import pytest
from fastapi import Response
from pydantic import ValidationError

from app.core.settings import Settings, settings
from app.modules.auth.router import cookies


def configured(**changes):
    return Settings.model_validate({**settings.model_dump(), **changes})


@pytest.mark.parametrize("environment", ["staging", "production"])
def test_public_deployments_require_https(environment):
    with pytest.raises(ValidationError, match="requieren HTTPS"):
        configured(app_env=environment, public_web_url="http://contrata.contupana.pro")


def test_sandbox_is_available_in_staging_but_rejected_in_production():
    assert configured(
        app_env="staging", payment_mode="sandbox", public_web_url="https://contrata.contupana.pro"
    ).secure_cookies
    with pytest.raises(ValidationError, match="pagos deshabilitados"):
        configured(
            app_env="production", payment_mode="sandbox", public_web_url="https://contrata.contupana.pro"
        )


def test_https_session_cookies_are_secure_and_scoped(monkeypatch):
    monkeypatch.setattr(settings, "app_env", "staging")
    monkeypatch.setattr(settings, "public_web_url", "https://contrata.contupana.pro")
    response = Response()
    cookies(response, "test-refresh")
    refresh, csrf = response.headers.getlist("set-cookie")
    assert "Secure" in refresh and "Secure" in csrf
    assert "HttpOnly" in refresh and "HttpOnly" not in csrf
    assert "Path=/api/v1/auth" in refresh
    assert "SameSite=strict" in refresh and "SameSite=strict" in csrf


def test_loopback_development_remains_usable_over_http(monkeypatch):
    monkeypatch.setattr(settings, "app_env", "development")
    monkeypatch.setattr(settings, "public_web_url", "http://127.0.0.1:8180")
    response = Response()
    cookies(response, "test-refresh")
    assert all("Secure" not in cookie for cookie in response.headers.getlist("set-cookie"))
