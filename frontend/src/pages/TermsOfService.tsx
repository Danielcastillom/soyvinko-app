import { Link } from "react-router-dom";
import {
  FileText,
  ShieldCheck,
  Scale,
  Handshake,
  DollarSign,
  AlertTriangle,
  HelpCircle,
  ArrowLeft,
  CheckCircle2,
} from "lucide-react";
import { PageTitle } from "../components";

export default function TermsOfService() {
  return (
    <div className="legal-page-container">
      <Link to="/explore" className="back-link">
        <ArrowLeft size={16} /> Volver a explorar
      </Link>

      <PageTitle
        eyebrow="MARCO LEGAL Y CONTRACTUAL"
        title="Términos y Condiciones de Uso de SoyVinko"
        description="Reglas claras, derechos y obligaciones para contratar servicios, comercializar toda clase de productos y operar con garantía en SoyVinko."
      />

      <div className="legal-last-updated">
        <span>Última actualización: 1 de octubre de 2026</span>
        <span>·</span>
        <span className="badge-legal-status">Versión 2.0 Vigente</span>
      </div>

      <div className="legal-layout-grid">
        <aside className="legal-toc-sidebar">
          <div className="legal-toc-card">
            <h4>Índice de Secciones</h4>
            <nav className="legal-toc-nav">
              <a href="#objeto">1. Objeto y Rol de SoyVinko</a>
              <a href="#registro">2. Cuentas y Tipos de Usuario</a>
              <a href="#contratacion-escrow">3. Contratación y Pagos en Custodia</a>
              <a href="#marketplace">4. Marketplace de Productos y Artículos</a>
              <a href="#modelo-tarifario">5. Modelo de Negocio y Comisiones</a>
              <a href="#conducta">6. Normas de Conducta y Prohibiciones</a>
              <a href="#disputas">7. Mediación de Disputas y Soporte</a>
              <a href="#responsabilidad">8. Limitación de Responsabilidad</a>
              <a href="#modificaciones">9. Modificaciones y Ley Aplicable</a>
            </nav>
          </div>
        </aside>

        <main className="legal-content-main">
          {/* SECCIÓN 1 */}
          <section id="objeto" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">01</div>
              <h2>Objeto y Rol de SoyVinko</h2>
            </div>
            <p>
              <strong>SoyVinko</strong> es una plataforma tecnológica que facilita el encuentro directo entre usuarios que requieren servicios para el hogar, empresas o proyectos digitales ("Clientes"), profesionales independientes o empresas prestadoras de servicios ("Profesionales") y comercios proveedores de productos, artículos, herramientas y bienes de cualquier categoría ("Comercios").
            </p>
            <p>
              Salvo que se indique expresamente lo contrario en contratos corporativos específicos, <strong>SoyVinko actúa como intermediario tecnológico</strong> y proveedor del software de presupuestos, trazabilidad y custodia de pagos. La relación contractual sustantiva de la obra o servicio se establece directamente entre el Cliente y el Profesional.
            </p>
          </section>

          {/* SECCIÓN 2 */}
          <section id="registro" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">02</div>
              <h2>Cuentas y Tipos de Usuario</h2>
            </div>
            <p>
              Para acceder a la contratación y publicación de servicios, el usuario debe crear una cuenta:
            </p>
            <ul className="legal-feature-list">
              <li>
                <strong>Persona Natural:</strong> Usuarios individuales que solicitan servicios o profesionales independientes. Se requiere mayoría de edad legal (18 años) y correo electrónico verificado.
              </li>
              <li>
                <strong>Empresa / Persona Jurídica:</strong> Sociedades mercantiles de Venezuela debidamente registradas con Registro de Información Fiscal (RIF) y persona de contacto autorizada.
              </li>
              <li>
                <strong>Verificación de Identidad (KYC):</strong> Los profesionales que deseen la insignia de verificación oficial deben consignar documento de identidad y fotografía de confirmación en la sección de expediente.
              </li>
            </ul>
          </section>

          {/* SECCIÓN 3 */}
          <section id="contratacion-escrow" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">03</div>
              <h2>Contratación de Servicios y Pagos Protegidos en Custodia (Escrow)</h2>
            </div>
            <p>
              Para erradicar la incertidumbre en los pagos y garantizar la entrega conforme del trabajo:
            </p>
            <ul className="legal-feature-list">
              <li>
                <strong>Acuerdo de Presupuesto:</strong> El contrato se formaliza cuando ambas partes convienen el precio, modalidad (Presencial o Remota) y los requerimientos del trabajo.
              </li>
              <li>
                <strong>Depósito en Custodia (Escrow):</strong> En servicios garantizados, los fondos acordados quedan bloqueados en custodia en la plataforma. Ni el cliente puede retirar los fondos unilateralmente sin justificación, ni el profesional los cobra hasta la finalización satisfactoria.
              </li>
              <li>
                <strong>Liberación de Fondos:</strong> Una vez entregado el trabajo y confirmado por el cliente (o transcurrido el plazo de confirmación de entrega), los fondos se liberan de inmediato a la billetera del profesional.
              </li>
            </ul>
          </section>

          {/* SECCIÓN 4 */}
          <section id="marketplace" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">04</div>
              <h2>Marketplace de Productos y Artículos Generales</h2>
            </div>
            <p>
              El Marketplace permite a los comercios registrados ofrecer cualquier clase de producto, artículo, herramienta, insumo, repuesto o tecnología de comercio lícito:
            </p>
            <ul className="legal-feature-list">
              <li>Los comercios garantizan que los productos publicados disponen de stock físico real y especificaciones fidedignas.</li>
              <li>Las órdenes se generan de forma idempotente para evitar duplicidades de cobro.</li>
              <li>El comercio es responsable de coordinar el embalaje y despacho conforme a los datos de entrega aportados por el comprador.</li>
            </ul>
          </section>

          {/* SECCIÓN 5 */}
          <section id="modelo-tarifario" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">05</div>
              <h2>Modelo de Negocio, Membresías y Comisiones Oficiales</h2>
            </div>
            <p>
              Las tarifas aplicables en la plataforma se rigen por la estructura oficial del modelo de negocio de SoyVinko:
            </p>

            <h3>A. Para Comercios y Vendedores (Tiendas / Productos Generales)</h3>
            <div className="legal-table-wrapper">
              <table className="legal-table">
                <thead>
                  <tr>
                    <th>Plan</th>
                    <th>Costo Mensual</th>
                    <th>Comisión por Venta</th>
                    <th>Inventario</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><strong>Plan Promocional Entrada</strong> (Meses 1 al 3)</td>
                    <td>$0,00 USD</td>
                    <td>0% (Mes 1) / 3% (Meses 2 y 3)</td>
                    <td>Hasta 15 productos</td>
                  </tr>
                  <tr>
                    <td><strong>Membresía Comercio Estándar</strong> (Desde Mes 4)</td>
                    <td>$30,00 USD</td>
                    <td>Tasa Mínima 4%</td>
                    <td>Hasta 30 productos</td>
                  </tr>
                  <tr>
                    <td><strong>Membresía Comercio Plus</strong> (Desde Mes 4)</td>
                    <td>$50,00 USD</td>
                    <td>Tasa Mínima 4%</td>
                    <td>Hasta 70 productos</td>
                  </tr>
                  <tr>
                    <td><strong>Modalidad Base Sin Suscripción</strong> (Desde Mes 4)</td>
                    <td>$0,00 USD</td>
                    <td>10% por venta</td>
                    <td>Hasta 10 productos</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3>B. Para Proveedores (Servicios Contratados)</h3>
            <div className="legal-table-wrapper">
              <table className="legal-table">
                <thead>
                  <tr>
                    <th>Plan</th>
                    <th>Costo Mensual</th>
                    <th>Comisión por Servicio</th>
                    <th>Beneficios</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><strong>Plan Promocional Entrada</strong> (Meses 1 al 3)</td>
                    <td>$0,00 USD</td>
                    <td>0% (Mes 1) / 3% (Meses 2 y 3)</td>
                    <td>Certificación de perfil gratis y acceso a cartelera</td>
                  </tr>
                  <tr>
                    <td><strong>Membresía Profesional</strong> (Desde Mes 4)</td>
                    <td>$15,00 USD</td>
                    <td>Tasa Mínima 3% Presencial / 6% Remoto</td>
                    <td>Prioridad en solicitudes y descuento de viáticos</td>
                  </tr>
                  <tr>
                    <td><strong>Modalidad Base Sin Suscripción</strong> (Desde Mes 4)</td>
                    <td>$0,00 USD</td>
                    <td>8% Presencial / 12% Remoto</td>
                    <td>Sin pago mensual, ideal para trabajos ocasionales</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="muted small">
              Nota: Las recargas de saldo a favor de clientes no tienen comisión alguna.
            </p>
          </section>

          {/* SECCIÓN 6 */}
          <section id="conducta" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">06</div>
              <h2>Normas de Conducta y Prohibiciones</h2>
            </div>
            <p>
              Queda terminantemente prohibido a cualquier usuario:
            </p>
            <ul className="legal-feature-list">
              <li>Publicar servicios ilícitos, peligrosos, falsificados o contrarios al orden público.</li>
              <li>Intentar evadir los mecanismos de custodia y seguridad de la plataforma para defraudar a la contraparte.</li>
              <li>Suplantar la identidad de terceros o utilizar documentos de identificación ajenos o alterados.</li>
              <li>Manipular las reseñas y valoraciones mediante cuentas ficticias o acuerdos colusorios.</li>
            </ul>
          </section>

          {/* SECCIÓN 7 */}
          <section id="disputas" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">07</div>
              <h2>Mediación de Disputas y Centro de Soporte</h2>
            </div>
            <p>
              Si surge una desavenencia respecto a la calidad o cumplimiento del servicio:
            </p>
            <ol className="legal-ordered-list">
              <li>Cualquiera de las partes puede abrir una disputa desde el detalle del contrato en la app.</li>
              <li>El equipo de mediación de SoyVinko revisará las pruebas aportadas (mensajes en la plataforma, fotografías de entrega y requerimientos).</li>
              <li>El mediador emitirá una resolución vinculante ordenando la liberación total, parcial o el reembolso del saldo en custodia al cliente.</li>
            </ol>
          </section>

          {/* SECCIÓN 8 */}
          <section id="responsabilidad" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">08</div>
              <h2>Limitación de Responsabilidad</h2>
            </div>
            <p>
              SoyVinko responde del correcto funcionamiento técnico de su infraestructura digital. No responde por daños fortuitos, fuerza mayor o vicios ocultos derivados de la ejecución física de las obras realizadas por los profesionales independientes, más allá de la cobertura del importe en custodia acordado.
            </p>
          </section>

          {/* SECCIÓN 9 */}
          <section id="modificaciones" className="legal-section">
            <div className="legal-section-header">
              <div className="legal-section-num">09</div>
              <h2>Modificaciones y Ley Aplicable</h2>
            </div>
            <p>
              Estos Términos se rigen por las leyes de la República Bolivariana de Venezuela y los principios generales del comercio electrónico internacional. Cualquier controversia se someterá prioritariamente al proceso de mediación interna y, subsidiariamente, a los tribunales competentes.
            </p>
          </section>
        </main>
      </div>
    </div>
  );
}
