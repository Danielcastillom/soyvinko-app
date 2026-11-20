import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
  Cookie,
  ShieldCheck,
  Settings2,
  Check,
  X,
  Lock,
  Sliders,
  Sparkles,
} from "lucide-react";

export type CookiePreferences = {
  necessary: boolean;
  preferences: boolean;
  analytics: boolean;
  timestamp: string;
};

const COOKIE_STORAGE_KEY = "vinko_cookie_consent";

export function getCookieConsent(): CookiePreferences | null {
  try {
    const raw = localStorage.getItem(COOKIE_STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveCookieConsent(prefs: Partial<CookiePreferences>) {
  const full: CookiePreferences = {
    necessary: true,
    preferences: prefs.preferences ?? true,
    analytics: prefs.analytics ?? false,
    timestamp: new Date().toISOString(),
  };
  localStorage.setItem(COOKIE_STORAGE_KEY, JSON.stringify(full));
  window.dispatchEvent(new CustomEvent("vinko-cookie-consent-updated", { detail: full }));
  return full;
}

export function openCookiePreferences() {
  window.dispatchEvent(new CustomEvent("open-cookie-preferences"));
}

export default function CookieConsent() {
  const [visible, setVisible] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [prefs, setPrefs] = useState<{ preferences: boolean; analytics: boolean }>({
    preferences: true,
    analytics: false,
  });

  useEffect(() => {
    const existing = getCookieConsent();
    if (!existing) {
      // Delay slightly for natural entry animation
      const timer = setTimeout(() => setVisible(true), 800);
      return () => clearTimeout(timer);
    } else {
      setPrefs({
        preferences: existing.preferences,
        analytics: existing.analytics,
      });
    }

    const handleOpen = () => {
      const current = getCookieConsent();
      if (current) {
        setPrefs({
          preferences: current.preferences,
          analytics: current.analytics,
        });
      }
      setModalOpen(true);
    };

    window.addEventListener("open-cookie-preferences", handleOpen);
    return () => window.removeEventListener("open-cookie-preferences", handleOpen);
  }, []);

  const handleAcceptAll = () => {
    saveCookieConsent({ preferences: true, analytics: true });
    setVisible(false);
    setModalOpen(false);
  };

  const handleAcceptNecessary = () => {
    saveCookieConsent({ preferences: false, analytics: false });
    setVisible(false);
    setModalOpen(false);
  };

  const handleSavePreferences = () => {
    saveCookieConsent({
      preferences: prefs.preferences,
      analytics: prefs.analytics,
    });
    setVisible(false);
    setModalOpen(false);
  };

  return (
    <>
      {/* BANNER INFERIOR DE CONSENTIMIENTO */}
      {visible && !modalOpen && (
        <div className="cookie-banner-wrapper" role="region" aria-label="Aviso de privacidad y cookies">
          <div className="cookie-banner-content">
            <div className="cookie-banner-icon">
              <Cookie size={28} className="text-accent" />
            </div>
            <div className="cookie-banner-text">
              <div className="cookie-banner-title">
                <strong>Privacidad, Cookies y Seguridad en SoyVinko</strong>
                <span className="badge-compliance">Cumplimiento RGPD & LGPD</span>
              </div>
              <p>
                Utilizamos cookies técnicas necesarias para la autenticación segura (sesión y protección anti-CSRF) y, con tu consentimiento, cookies funcionales para recordar tus preferencias de moneda y ubicación. No vendemos tus datos a terceros. Consulta nuestra{" "}
                <Link to="/privacidad" className="cookie-link">
                  Política de Privacidad
                </Link>{" "}
                y{" "}
                <Link to="/cookies" className="cookie-link">
                  Política de Cookies
                </Link>.
              </p>
            </div>
            <div className="cookie-banner-actions">
              <button
                type="button"
                className="button secondary small"
                onClick={() => setModalOpen(true)}
              >
                <Sliders size={15} /> Personalizar
              </button>
              <button
                type="button"
                className="button secondary small"
                onClick={handleAcceptNecessary}
              >
                Solo necesarias
              </button>
              <button
                type="button"
                className="button primary small"
                onClick={handleAcceptAll}
              >
                <Check size={16} /> Aceptar todas
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL DE CONFIGURACIÓN GRANULAR DE COOKIES */}
      {modalOpen && (
        <div className="modal-backdrop" onClick={() => setModalOpen(false)}>
          <div
            className="modal-card cookie-modal-card"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="cookie-modal-title"
          >
            <div className="modal-header">
              <div className="cookie-modal-title-group">
                <Cookie size={22} className="text-accent" />
                <h2 id="cookie-modal-title">Centro de Preferencias de Privacidad</h2>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setModalOpen(false)}
                aria-label="Cerrar modal"
              >
                <X size={18} />
              </button>
            </div>

            <div className="cookie-modal-body">
              <p className="muted small">
                En SoyVinko nos tomamos tu privacidad con el máximo rigor. Aquí puedes revisar y elegir qué categorías de cookies y tecnologías de almacenamiento local permites en tu navegador.
              </p>

              <div className="cookie-category-item">
                <div className="cookie-category-header">
                  <div className="cookie-category-title">
                    <ShieldCheck size={18} className="text-success" />
                    <strong>1. Cookies Técnicas y Esenciales (Obligatorias)</strong>
                  </div>
                  <span className="badge-required">Siempre activas</span>
                </div>
                <p className="cookie-category-desc">
                  Imprescindibles para el inicio de sesión cifrado, token anti-falsificación <code>chamba_csrf</code>, prevención de fraude en contratos de custodia (Escrow) y navegación protegida. No se pueden desactivar.
                </p>
              </div>

              <div className="cookie-category-item">
                <div className="cookie-category-header">
                  <div className="cookie-category-title">
                    <Sliders size={18} className="text-accent" />
                    <strong>2. Cookies de Preferencias y Funcionalidad</strong>
                  </div>
                  <label className="switch-toggle" aria-label="Permitir cookies de preferencias">
                    <input
                      type="checkbox"
                      checked={prefs.preferences}
                      onChange={(e) =>
                        setPrefs({ ...prefs, preferences: e.target.checked })
                      }
                    />
                    <span className="switch-slider"></span>
                  </label>
                </div>
                <p className="cookie-category-desc">
                  Permiten recordar tus filtros de búsqueda de técnicos, selección de moneda preferida (Bolívares / Dólares) y estado de navegación rápida para que no tengas que reconfigurarlos en cada visita.
                </p>
              </div>

              <div className="cookie-category-item">
                <div className="cookie-category-header">
                  <div className="cookie-category-title">
                    <Sparkles size={18} className="text-purple" />
                    <strong>3. Métricas y Rendimiento Agregado</strong>
                  </div>
                  <label className="switch-toggle" aria-label="Permitir métricas y rendimiento">
                    <input
                      type="checkbox"
                      checked={prefs.analytics}
                      onChange={(e) =>
                        setPrefs({ ...prefs, analytics: e.target.checked })
                      }
                    />
                    <span className="switch-slider"></span>
                  </label>
                </div>
                <p className="cookie-category-desc">
                  Nos ayudan a entender de forma completamente agregada y anónima qué secciones son más útiles y a medir tiempos de carga para optimizar la velocidad en conexiones lentas.
                </p>
              </div>

              <div className="cookie-links-note">
                <p>
                  Para más detalles, consulta nuestra{" "}
                  <Link to="/privacidad" onClick={() => setModalOpen(false)}>
                    Política de Privacidad
                  </Link>{" "}
                  y{" "}
                  <Link to="/cookies" onClick={() => setModalOpen(false)}>
                    Política de Cookies
                  </Link>.
                </p>
              </div>
            </div>

            <div className="modal-actions-footer">
              <button
                type="button"
                className="button secondary"
                onClick={handleAcceptNecessary}
              >
                Rechazar no esenciales
              </button>
              <button
                type="button"
                className="button primary"
                onClick={handleSavePreferences}
              >
                Guardar mis preferencias
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
