import { Link } from "react-router-dom";
import { Scale, ShieldCheck, Mail, ArrowLeft, Building2 } from "lucide-react";
import { PageTitle } from "../components";

export default function LegalNotice() {
  return (
    <div className="legal-page-container">
      <Link to="/explore" className="back-link">
        <ArrowLeft size={16} /> Volver a explorar
      </Link>

      <PageTitle
        eyebrow="INFORMACIÓN CORPORATIVA Y LEGAL"
        title="Aviso Legal y Condiciones Generales"
        description="Información identificativa de SoyVinko, titularidad de los derechos de propiedad intelectual y condiciones de uso del portal."
      />

      <div className="legal-last-updated">
        <span>Última actualización: 1 de octubre de 2026</span>
        <span>·</span>
        <span className="badge-legal-status">Vigente</span>
      </div>

      <div className="legal-layout-grid">
        <aside className="legal-toc-sidebar">
          <div className="legal-toc-card">
            <h4>Índice</h4>
            <nav className="legal-toc-nav">
              <a href="#titular">1. Datos Identificativos</a>
              <a href="#propiedad">2. Propiedad Intelectual e Industrial</a>
              <a href="#condiciones">3. Condiciones de Acceso y Uso</a>
              <a href="#responsabilidad">4. Exclusión de Responsabilidad</a>
              <a href="#jurisdiccion">5. Ley Aplicable y Jurisdicción</a>
            </nav>
          </div>
        </aside>

        <main className="legal-content-main">
          <section id="titular" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">01</div>
              <h2>Datos Identificativos</h2>
            </div>
            <p>
              En cumplimiento del deber de información y transparencia para servicios de la sociedad de la información y comercio electrónico:
            </p>
            <ul className="legal-feature-list">
              <li><strong>Denominación comercial:</strong> SoyVinko ("Vinko")</li>
              <li><strong>Dominio web:</strong> <code>https://soyvinko.com</code></li>
              <li><strong>Actividad principal:</strong> Portal tecnológico de intermediación, contratación de servicios y marketplace de productos, herramientas, insumos y artículos de cualquier categoría.</li>
              <li><strong>Correo de contacto oficial:</strong> <a href="mailto:contacto@soyvinko.com">contacto@soyvinko.com</a></li>
              <li><strong>Atención al usuario y soporte:</strong> Disponible a través del Centro de Soporte en plataforma.</li>
            </ul>
          </section>

          <section id="propiedad" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">02</div>
              <h2>Propiedad Intelectual e Industrial</h2>
            </div>
            <p>
              Todos los elementos que forman parte de este sitio web —incluyendo el código fuente, la arquitectura de software, bases de datos, logotipos, marcas comerciales, iconografías, diseño gráfico y textos— son propiedad exclusiva de <strong>SoyVinko</strong> o de terceros que han autorizado legítimamente su inclusión.
            </p>
            <p>
              Queda expresamente prohibida la reproducción, distribución, comunicación pública, ingeniería inversa o transformación no autorizada de cualquier elemento del portal sin el previo consentimiento por escrito de SoyVinko.
            </p>
          </section>

          <section id="condiciones" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">03</div>
              <h2>Condiciones de Acceso y Uso</h2>
            </div>
            <p>
              El acceso a soyvinko.com atribuye la condición de usuario e implica la aceptación plena de este Aviso Legal, los <Link to="/terminos">Términos de Servicio</Link> y la <Link to="/privacidad">Política de Privacidad</Link>.
            </p>
            <p>
              El usuario se compromete a hacer un uso lícito y diligente de los contenidos y servicios ofrecidos, absteniéndose de realizar actividades lesivas o contrarias a la buena fe comercial.
            </p>
          </section>

          <section id="responsabilidad" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">04</div>
              <h2>Exclusión de Responsabilidad</h2>
            </div>
            <p>
              SoyVinko no garantiza la disponibilidad ininterrumpida del sitio ante eventuales caídas de telecomunicaciones ajenas a su control directo, si bien implementa las mejores prácticas operativas y de respaldo para mantener la máxima disponibilidad del servicio.
            </p>
          </section>

          <section id="jurisdiccion" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">05</div>
              <h2>Ley Aplicable y Jurisdicción</h2>
            </div>
            <p>
              Para la resolución de cualquier conflicto relativo al presente sitio web o a las actividades en él desarrolladas, será de aplicación la legislación venezolana y los estándares internacionales de comercio electrónico, sometiéndose las partes a los tribunales competentes.
            </p>
          </section>
        </main>
      </div>
    </div>
  );
}
