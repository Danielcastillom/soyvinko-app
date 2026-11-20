import { Link } from "react-router-dom";
import {
  ShieldCheck,
  Lock,
  Eye,
  FileText,
  UserCheck,
  Database,
  MapPin,
  CheckCircle2,
  Mail,
  Scale,
  Sparkles,
  ArrowLeft,
} from "lucide-react";
import { PageTitle } from "../components";

export default function PrivacyPolicy() {
  return (
    <div className="legal-page-container">
      <Link to="/explore" className="back-link">
        <ArrowLeft size={16} /> Volver a explorar
      </Link>

      <PageTitle
        eyebrow="TRANSPARENCIA Y SEGURIDAD"
        title="Política de Privacidad y Protección de Datos"
        description="En SoyVinko protegemos tu información personal bajo los más altos estándares internacionales (RGPD / GDPR, LGPD y normativas de protección de datos)."
      />

      <div className="legal-last-updated">
        <span>Última actualización: 1 de octubre de 2026</span>
        <span>·</span>
        <span className="badge-legal-status">En vigor</span>
      </div>

      <div className="legal-layout-grid">
        <aside className="legal-toc-sidebar">
          <div className="legal-toc-card">
            <h4>Índice de contenidos</h4>
            <nav className="legal-toc-nav">
              <a href="#responsable">1. Responsable del Tratamiento</a>
              <a href="#datos-recopilados">2. Datos Personales que Tratamos</a>
              <a href="#finalidad-bases">3. Finalidades y Bases Jurídicas</a>
              <a href="#seguridad-cifrado">4. Seguridad, Cifrado y Arquitectura</a>
              <a href="#kyc-identidad">5. Expedientes de Identidad (KYC)</a>
              <a href="#geolocalizacion">6. Geolocalización y Coordenadas</a>
              <a href="#conservacion">7. Plazos de Conservación</a>
              <a href="#destinatarios">8. No Venta de Datos y Destinatarios</a>
              <a href="#derechos-usuario">9. Tus Derechos (ARCO & RGPD)</a>
              <a href="#contacto-dpo">10. Contacto y Delegado de Privacidad</a>
            </nav>
          </div>
        </aside>

        <main className="legal-content-main">
          {/* SECCIÓN 1 */}
          <section id="responsable" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">01</div>
              <h2>Responsable del Tratamiento</h2>
            </div>
            <p>
              El responsable del tratamiento de los datos personales recabados a través del sitio web <strong>soyvinko.com</strong> y sus aplicaciones asociadas es <strong>SoyVinko</strong> (en adelante, “Vinko”, “la Plataforma” o “nosotros”), con domicilio de operaciones en Venezuela y alcance de servicios digitales y remotos.
            </p>
            <p>
              Para cualquier consulta o solicitud relativa a esta Política de Privacidad, puedes dirigirte a nuestro equipo de protección de datos a través de:
            </p>
            <div className="legal-callout">
              <Mail size={18} className="text-accent" />
              <div>
                <strong>Canal directo de Privacidad:</strong>{" "}
                <a href="mailto:privacidad@soyvinko.com">privacidad@soyvinko.com</a> /{" "}
                <a href="mailto:contacto@soyvinko.com">contacto@soyvinko.com</a>
              </div>
            </div>
          </section>

          {/* SECCIÓN 2 */}
          <section id="datos-recopilados" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">02</div>
              <h2>Datos Personales que Tratamos</h2>
            </div>
            <p>
              Recopilamos y tratamos únicamente los datos necesarios para proporcionar nuestros servicios de conexión profesional, mediación de presupuestos y marketplace de productos y artículos:
            </p>
            <ul className="legal-feature-list">
              <li>
                <strong>Datos de Registro e Identificación:</strong> Nombre y apellido o Razón Social (para empresas), dirección de correo electrónico, tipo de cuenta (Persona Natural o Jurídica con RIF) y contraseñas fuertemente hasheadas con algoritmo Argon2id.
              </li>
              <li>
                <strong>Datos de Perfil Profesional:</strong> Título comercial, especialidades técnicas, años de experiencia, biografía laboral, fotos de trabajos realizados y enlaces de portafolio.
              </li>
              <li>
                <strong>Datos de Expediente de Identidad (KYC):</strong> Cédula de identidad, número de documento, fotografías de verificación y selfies con documento en mano (únicamente para profesionales que solicitan la verificación oficial).
              </li>
              <li>
                <strong>Datos de Contacto y Ubicación:</strong> Número telefónico de WhatsApp/llamadas (compartido solo tras la aceptación mutua de un contrato), direcciones y coordenadas de geolocalización aproximadas para búsqueda de profesionales cercanos.
              </li>
              <li>
                <strong>Datos de Transacciones y Marketplace:</strong> Comprobantes de pago reportados (referencia, banco emisor, moneda), cotizaciones acordadas, registro de pedidos de productos/artículos e historial de pagos en custodia (Escrow).
              </li>
              <li>
                <strong>Datos Técnicos y de Conexión:</strong> Identificadores de sesión HttpOnly, tokens anti-falsificación <code>chamba_csrf</code> y registros mínimos de auditoría técnica con marcas de tiempo.
              </li>
            </ul>
          </section>

          {/* SECCIÓN 3 */}
          <section id="finalidad-bases" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">03</div>
              <h2>Finalidades y Bases Jurídicas del Tratamiento</h2>
            </div>
            <p>
              Conforme al <strong>Reglamento General de Protección de Datos (RGPD)</strong> y normativas homólogas, tratamos tus datos bajo las siguientes bases legales:
            </p>
            <div className="legal-table-wrapper">
              <table className="legal-table">
                <thead>
                  <tr>
                    <th>Finalidad del Tratamiento</th>
                    <th>Base Legal Aplicable</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Creación y mantenimiento de tu cuenta de usuario o perfil profesional.</td>
                    <td><strong>Ejecución del contrato</strong> de prestación de servicios (Art. 6.1.b RGPD).</td>
                  </tr>
                  <tr>
                    <td>Conexión entre clientes y técnicos, gestión de presupuestos y custodia de pagos (Escrow).</td>
                    <td><strong>Ejecución del contrato</strong> bilateral acordado.</td>
                  </tr>
                  <tr>
                    <td>Verificación de identidad (KYC) y prevención de fraudes o suplantaciones.</td>
                    <td><strong>Interés legítimo</strong> y <strong>Consentimiento expreso</strong> del usuario.</td>
                  </tr>
                  <tr>
                    <td>Búsqueda geoespacial de técnicos cercanos según el estado y municipio del cliente.</td>
                    <td><strong>Consentimiento</strong> otorgado al permitir la geolocalización en el navegador.</td>
                  </tr>
                  <tr>
                    <td>Facturación de membresías, emisión de comprobantes y trazabilidad contable.</td>
                    <td><strong>Cumplimiento de obligaciones legales</strong> y fiscales aplicables.</td>
                  </tr>
                  <tr>
                    <td>Auditoría de seguridad y resolución de incidencias en soporte.</td>
                    <td><strong>Interés legítimo</strong> en salvaguardar la integridad de la comunidad.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          {/* SECCIÓN 4 */}
          <section id="seguridad-cifrado" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">04</div>
              <h2>Seguridad, Cifrado y Arquitectura de Datos</h2>
            </div>
            <p>
              La seguridad es el pilar estructural de SoyVinko. Hemos implementado medidas técnicas y organizativas de nivel bancario:
            </p>
            <div className="legal-cards-grid">
              <div className="legal-mini-card">
                <Lock size={20} className="text-accent" />
                <h4>Contraseñas con Argon2id</h4>
                <p>
                  Tus contraseñas nunca se almacenan en texto claro. Se procesan con Argon2id, el estándar criptográfico más resistente contra ataques por fuerza bruta.
                </p>
              </div>
              <div className="legal-mini-card">
                <ShieldCheck size={20} className="text-accent" />
                <h4>Cifrado en Tránsito (TLS 1.3)</h4>
                <p>
                  Todas las comunicaciones entre tu navegador y nuestros servidores están cifradas mediante HTTPS con certificados TLS modernos y políticas HSTS forzadas.
                </p>
              </div>
              <div className="legal-mini-card">
                <Database size={20} className="text-accent" />
                <h4>Row Level Security (RLS)</h4>
                <p>
                  Las bases de datos aíslan los datos a nivel de fila y función (`Security Definer`). Ningún usuario puede consultar registros ajenos ni contraseñas.
                </p>
              </div>
            </div>
          </section>

          {/* SECCIÓN 5 */}
          <section id="kyc-identidad" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">05</div>
              <h2>Expedientes de Identidad y Verificación (KYC)</h2>
            </div>
            <p>
              Para garantizar que los clientes contraten con personas reales y de confianza, los profesionales pueden presentar su expediente KYC. Tratamos estos documentos con salvaguardas especiales:
            </p>
            <ul className="legal-feature-list">
              <li>Los documentos de identidad y fotos se almacenan en repositorios aislados con cifrado en reposo.</li>
              <li>El personal administrativo accede únicamente a través de auditoría individual registrada con motivo justificativo.</li>
              <li>Los documentos nunca se publican en el perfil público ni son accesibles para otros usuarios.</li>
            </ul>
          </section>

          {/* SECCIÓN 6 */}
          <section id="geolocalizacion" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">06</div>
              <h2>Geolocalización y Coordenadas</h2>
            </div>
            <p>
              SoyVinko permite encontrar servicios según la cercanía geográfica:
            </p>
            <ul className="legal-feature-list">
              <li>Las coordenadas exactas de los clientes nunca se revelan a terceros de forma pública en el mapa.</li>
              <li>La búsqueda pública utiliza aproximaciones por parroquia o municipio para proteger la privacidad del hogar.</li>
              <li>La ubicación precisa solo se comparte de forma temporal y voluntaria con el profesional contratado una vez iniciado el servicio presencial.</li>
            </ul>
          </section>

          {/* SECCIÓN 7 */}
          <section id="conservacion" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">07</div>
              <h2>Plazos de Conservación de Datos</h2>
            </div>
            <p>
              Conservamos los datos personales durante el tiempo estrictamente necesario para cumplir con las finalidades descritas:
            </p>
            <ul className="legal-feature-list">
              <li><strong>Datos de cuenta activa:</strong> Mientras mantengas tu cuenta abierta en la plataforma.</li>
              <li><strong>Historial de transacciones y contratos:</strong> Por los periodos legalmente exigidos en materia tributaria y mercantil (mínimo 5 años).</li>
              <li><strong>Documentos de verificación aprobados:</strong> Mientras el profesional mantenga su certificación vigente.</li>
              <li><strong>Al solicitar la baja de cuenta:</strong> Los datos personales se anonimizan o eliminan de forma segura, salvo aquellos requeridos por mandatos legales.</li>
            </ul>
          </section>

          {/* SECCIÓN 8 */}
          <section id="destinatarios" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">08</div>
              <h2>No Venta de Datos y Destinatarios</h2>
            </div>
            <p>
              <strong>SoyVinko no comercializa, no alquila y no vende datos personales a terceros con fines publicitarios.</strong>
            </p>
            <p>
              Tus datos solo se comunican a:
            </p>
            <ul className="legal-feature-list">
              <li>El profesional o cliente con quien contrates un servicio o una orden de compra, únicamente para coordinar la entrega o ejecución.</li>
              <li>Proveedores tecnológicos auxiliares necesarios (alojamiento seguro en servidores dedicados, servicios de cartografía Mapbox).</li>
              <li>Autoridades competentes cuando exista un requerimiento judicial vinculante debidamente motivado.</li>
            </ul>
          </section>

          {/* SECCIÓN 9 */}
          <section id="derechos-usuario" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">09</div>
              <h2>Tus Derechos (ARCO & RGPD)</h2>
            </div>
            <p>
              Como titular de los datos, dispones de los siguientes derechos reconocidos internacionalmente:
            </p>
            <div className="legal-rights-grid">
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Derecho de Acceso:</strong> Consultar qué datos tratamos sobre ti y obtener una copia de los mismos.
                </div>
              </div>
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Derecho de Rectificación:</strong> Corregir o actualizar cualquier dato inexacto o desactualizado desde tu perfil.
                </div>
              </div>
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Derecho de Supresión ("Derecho al Olvido"):</strong> Solicitar el borrado definitivo de tu cuenta y datos asociados cuando ya no sean necesarios.
                </div>
              </div>
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Derecho de Oposición:</strong> Oponerte al tratamiento de tus datos para finalidades específicas basadas en interés legítimo.
                </div>
              </div>
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Derecho a la Portabilidad:</strong> Recibir tus datos personales en un formato estructurado, común y legible por máquina.
                </div>
              </div>
              <div className="legal-right-item">
                <CheckCircle2 size={16} className="text-success" />
                <div>
                  <strong>Revocación del Consentimiento:</strong> Retirar en cualquier momento el consentimiento previamente otorgado (como las cookies o geolocalización).
                </div>
              </div>
            </div>
          </section>

          {/* SECCIÓN 10 */}
          <section id="contacto-dpo" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">10</div>
              <h2>Ejercicio de Derechos y Contacto</h2>
            </div>
            <p>
              Para ejercer cualquiera de tus derechos o solicitar la supresión de tu cuenta, puedes enviarnos una solicitud indicando tu nombre y correo registrado a:
            </p>
            <div className="legal-callout">
              <Scale size={20} className="text-accent" />
              <div>
                <strong>Oficina de Privacidad y Cumplimiento:</strong><br />
                Correo electrónico: <a href="mailto:privacidad@soyvinko.com">privacidad@soyvinko.com</a><br />
                Respuesta en un plazo máximo de 72 horas hábiles.
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
