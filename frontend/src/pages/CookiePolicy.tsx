import { Link } from "react-router-dom";
import {
  Cookie,
  ShieldCheck,
  Sliders,
  CheckCircle2,
  Lock,
  ArrowLeft,
  Settings2,
  Info,
} from "lucide-react";
import { PageTitle } from "../components";
import { openCookiePreferences } from "../components/CookieConsent";

export default function CookiePolicy() {
  return (
    <div className="legal-page-container">
      <Link to="/explore" className="back-link">
        <ArrowLeft size={16} /> Volver a explorar
      </Link>

      <PageTitle
        eyebrow="POLÍTICA DE COOKIES Y ALMACENAMIENTO"
        title="Política de Cookies de SoyVinko"
        description="Conoce en detalle qué cookies utilizamos, para qué sirven y cómo gestionar tus preferencias en cualquier momento."
      />

      <div className="legal-last-updated">
        <span>Última actualización: 1 de octubre de 2026</span>
        <span>·</span>
        <button
          type="button"
          className="button secondary small"
          onClick={openCookiePreferences}
          style={{ marginLeft: 12 }}
        >
          <Sliders size={14} /> Reconfigurar mis preferencias
        </button>
      </div>

      <div className="legal-layout-grid">
        <aside className="legal-toc-sidebar">
          <div className="legal-toc-card">
            <h4>Índice</h4>
            <nav className="legal-toc-nav">
              <a href="#que-son">1. ¿Qué son las Cookies?</a>
              <a href="#cookies-utilizadas">2. Cookies que Utilizamos</a>
              <a href="#almacenamiento-local">3. Almacenamiento Local (LocalStorage)</a>
              <a href="#terceros">4. Servicios y Cookies de Terceros</a>
              <a href="#control-gestion">5. Cómo Controlar tus Cookies</a>
              <a href="#desactivar-navegador">6. Desactivación por Navegador</a>
            </nav>
          </div>
        </aside>

        <main className="legal-content-main">
          {/* SECCIÓN 1 */}
          <section id="que-son" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">01</div>
              <h2>¿Qué son las Cookies?</h2>
            </div>
            <p>
              Una cookie es un pequeño archivo de texto que un sitio web guarda en tu ordenador o dispositivo móvil cuando lo visitas. Permiten que la plataforma recuerde tus acciones y preferencias (como el inicio de sesión cifrado, moneda de consulta o filtros de búsqueda) durante un periodo de tiempo determinado, para que no tengas que volver a configurarlos cada vez que regresas o navegas de una página a otra.
            </p>
          </section>

          {/* SECCIÓN 2 */}
          <section id="cookies-utilizadas" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">02</div>
              <h2>Cookies que Utilizamos en SoyVinko</h2>
            </div>
            <p>
              En SoyVinko clasificamos las cookies en las siguientes categorías funcionales:
            </p>

            <div className="legal-table-wrapper">
              <table className="legal-table">
                <thead>
                  <tr>
                    <th>Nombre de la Cookie</th>
                    <th>Tipo / Categoría</th>
                    <th>Finalidad</th>
                    <th>Duración</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><code>chamba_csrf</code></td>
                    <td><span className="badge-required">Técnica / Esencial</span></td>
                    <td>Protección de seguridad estricta contra falsificación de peticiones en sitios cruzados (Cross-Site Request Forgery).</td>
                    <td>Sesión del navegador</td>
                  </tr>
                  <tr>
                    <td><code>chamba_session</code> / Token de acceso</td>
                    <td><span className="badge-required">Técnica / Esencial</span></td>
                    <td>Autenticación de cuenta segura mediante encabezados HttpOnly cifrados. Mantiene tu sesión protegida.</td>
                    <td>Persistente / Cierre de sesión</td>
                  </tr>
                  <tr>
                    <td><code>vinko_cookie_consent</code></td>
                    <td><span className="badge badge-plan">Funcional / Preferencias</span></td>
                    <td>Guarda tu selección y consentimiento en el banner de cookies para no mostrártelo de forma intrusiva.</td>
                    <td>1 año</td>
                  </tr>
                  <tr>
                    <td><code>vinko_preferred_currency</code></td>
                    <td><span className="badge badge-plan">Funcional / Preferencias</span></td>
                    <td>Recuerda si prefieres visualizar las tarifas y presupuestos en Bolívares (VES) o Dólares (USD).</td>
                    <td>Persistente</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          {/* SECCIÓN 3 */}
          <section id="almacenamiento-local" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">03</div>
              <h2>Tecnologías de Almacenamiento Local (LocalStorage)</h2>
            </div>
            <p>
              Además de las cookies convencionales, utilizamos <strong>LocalStorage</strong> en el navegador para mejorar sustancialmente la velocidad de carga de la plataforma y evitar consumo innecesario de datos móviles:
            </p>
            <ul className="legal-feature-list">
              <li><strong>Caché de Catálogo y Categorías:</strong> Permite que la lista de oficios y servicios cargue al instante sin esperar peticiones al servidor.</li>
              <li><strong>Preferencias de Búsqueda:</strong> Guarda temporalmente los filtros de modalidad (Presencial o Remoto) seleccionados durante la navegación.</li>
            </ul>
          </section>

          {/* SECCIÓN 4 */}
          <section id="terceros" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">04</div>
              <h2>Servicios y Cookies de Terceros</h2>
            </div>
            <p>
              Para prestar funcionalidades avanzadas de mapas y geografía de Venezuela, colaboramos con proveedores tecnológicos que pueden procesar datos técnicos:
            </p>
            <ul className="legal-feature-list">
              <li>
                <strong>Mapbox Inc. (Mapas Interactivos):</strong> Proporciona la cartografía interactiva para localizar municipios y parroquias. Mapbox puede generar identificadores de sesión técnica para la entrega eficiente de fragmentos de mapa.
              </li>
              <li>
                <strong>Google Fonts:</strong> Tipografías optimizadas para una lectura clara y profesional, servidas de forma estática sin seguimiento de perfiles.
              </li>
            </ul>
            <div className="legal-callout">
              <Info size={18} className="text-accent" />
              <div>
                <strong>Aviso de no seguimiento:</strong> SoyVinko no integra píxeles invasivos de publicidad comportamental ni redes de compra de datos (Data Brokers).
              </div>
            </div>
          </section>

          {/* SECCIÓN 5 */}
          <section id="control-gestion" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">05</div>
              <h2>Cómo Controlar y Modificar tus Preferencias</h2>
            </div>
            <p>
              Puedes cambiar tus opciones de cookies en cualquier momento. Al hacer clic en el siguiente botón se desplegará el panel interactivo de configuración:
            </p>
            <div style={{ margin: "20px 0" }}>
              <button
                type="button"
                className="button primary"
                onClick={openCookiePreferences}
              >
                <Sliders size={16} /> Abrir Centro de Preferencias de Cookies
              </button>
            </div>
          </section>

          {/* SECCIÓN 6 */}
          <section id="desactivar-navegador" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">06</div>
              <h2>Desactivación desde la Configuración de tu Navegador</h2>
            </div>
            <p>
              También puedes restringir, bloquear o borrar las cookies configurando las opciones de tu navegador web:
            </p>
            <ul className="legal-feature-list">
              <li><strong>Google Chrome:</strong> Configuración &gt; Privacidad y seguridad &gt; Cookies y otros datos de sitios.</li>
              <li><strong>Mozilla Firefox:</strong> Ajustes &gt; Privacidad &amp; Seguridad &gt; Cookies y datos del sitio.</li>
              <li><strong>Apple Safari:</strong> Preferencias &gt; Privacidad &gt; Bloquear todas las cookies o gestionar datos del sitio.</li>
              <li><strong>Microsoft Edge:</strong> Configuración &gt; Permisos de cookies y del sitio.</li>
            </ul>
            <p className="muted small">
              Nota: Si bloqueas las cookies técnicas esenciales (`chamba_csrf` o cookies de sesión), es posible que no puedas iniciar sesión ni contratar servicios con custodia de pagos.
            </p>
          </section>
        </main>
      </div>
    </div>
  );
}
