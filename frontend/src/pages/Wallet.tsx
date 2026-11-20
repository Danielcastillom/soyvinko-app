import { useState } from "react";
import {
  Sparkles,
  Coins,
  ShieldCheck,
  TrendingUp,
  CheckCircle2,
  HelpCircle,
  RefreshCw,
  ArrowRight,
  Zap,
  Award,
} from "lucide-react";
import { VinkoIconCoin } from "../VinkoIcons";
import { MoneyWallet } from "../components/MoneyWallet";
import { api, request, newKey, date, type Data } from "../api";
import { useAuth } from "../context";
import {
  useData,
  PageTitle,
  Section,
  Empty,
  Loading,
  Badge,
  RunButton,
  Modal,
  ErrorBox,
} from "../components";

export default function Wallet() {
  const { user } = useAuth();
  const [purchase, setPurchase] = useState<Data | null>(null);
  const [infoModal, setInfoModal] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const credits = useData(() => api("my_wallet"), []);
  const plans = useData(() => api<Data[]>("active_plans"), []);
  const purchases = useData(() => api<Data[]>("my_credit_purchases"), []);
  const config = useData(() => request("/api/v1/config"), []);

  const sandbox = config.data?.payment_mode === "sandbox";
  const vinkoBalance = credits.data?.balance ?? credits.data?.credits ?? 0;

  const refresh = () => {
    credits.reload();
    purchases.reload();
    plans.reload();
    config.reload();
  };

  return (
    <>
      <PageTitle
        eyebrow="MONEDA OFICIAL SOYVINKO"
        title="Mi wallet"
        description="Tu saldo en bolívares y dólares, la compra de Vinkos, recargas, retiros e historial en un solo lugar."
      >
        <button
          type="button"
          className="button secondary"
          onClick={() => setInfoModal(true)}
          style={{ display: "inline-flex", alignItems: "center", gap: 7 }}
        >
          <HelpCircle size={16} /> ¿Qué son los Vinkos?
        </button>
      </PageTitle>

      {/* Alerta de acción exitosa */}
      {successMsg && (
        <div className="vinko-alert-success" style={{ marginBottom: 20 }}>
          <CheckCircle2 size={20} color="#16a34a" />
          <div style={{ flex: 1 }}>{successMsg}</div>
          <button
            className="link-button"
            onClick={() => setSuccessMsg(null)}
            style={{ color: "#166534", fontWeight: 700 }}
          >
            ✕
          </button>
        </div>
      )}

      {/* TARJETA DESTACADA: WALLET EXCLUSIVO DE VINKOS */}
      <div className="vinko-hero-card">
        <div className="vinko-hero-header">
          <div className="vinko-brand-badge">
            <div className="vinko-coin-symbol">
              <VinkoIconCoin size={26} strokeWidth={2.4} />
            </div>
            <div className="vinko-title-group">
              <h2>Créditos Vinkos</h2>
              <span>Moneda Oficial de la Plataforma</span>
            </div>
          </div>
          <div className="vinko-status-pill">
            <span className="pulse-dot"></span>
            <span>Cuenta activa para contrataciones</span>
          </div>
        </div>

        <div className="vinko-body-grid">
          <div className="vinko-balance-display">
            <div className="vinko-balance-main">
              <span>{vinkoBalance}</span>
              <span className="vinko-balance-unit">Vinkos</span>
            </div>
            <div className="vinko-balance-tag">
              <span>✦ Saldo oficial disponible para contratar y pactar servicios</span>
            </div>
          </div>

          <div className="vinko-quick-actions">
            <a href="#comprar-vinkos" className="btn-vinko-primary">
              <Coins size={18} strokeWidth={2.4} />
              <span>Comprar Vinkos con mi saldo</span>
            </a>
            <a href="#planes-vinkos" className="btn-vinko-secondary">
              <Award size={17} strokeWidth={2.2} />
              <span>Ver paquetes</span>
            </a>
            <a href="#historial-vinkos" className="btn-vinko-secondary">
              <Sparkles size={17} strokeWidth={2.2} />
              <span>Ver movimientos</span>
            </a>
            <button
              type="button"
              className="btn-vinko-secondary"
              onClick={refresh}
              title="Actualizar saldo"
            >
              <RefreshCw size={15} strokeWidth={2.2} />
              <span>Actualizar</span>
            </button>
          </div>
        </div>
      </div>

      <div className="mw-stack">
        <MoneyWallet vinkoBalance={vinkoBalance} onVinkosChanged={refresh} />
      </div>

      {/* PILARES Y BENEFICIOS DE LOS VINKOS */}
      <div className="vinko-features-grid">
        <div className="vinko-feature-card">
          <div className="vinko-feature-icon">
            <Sparkles size={22} />
          </div>
          <h3>1 Vinko por Contratación</h3>
          <p>
            Cada contratación aceptada o servicio acordado en la plataforma descuenta 1 Vinko. Sin comisiones bancarias ni cobros ocultos.
          </p>
        </div>

        <div className="vinko-feature-card">
          <div className="vinko-feature-icon">
            <ShieldCheck size={22} />
          </div>
          <h3>Garantía y Respaldo</h3>
          <p>
            Tus Vinkos respaldan solicitudes protegidas con verificación de identidad, soporte prioritario y mediación oficial de SoyVinko.
          </p>
        </div>

        <div className="vinko-feature-card">
          <div className="vinko-feature-icon">
            <TrendingUp size={22} />
          </div>
          <h3>Presencia y Posicionamiento</h3>
          <p>
            Los paquetes de Vinkos incluyen beneficios como días de tienda en el marketplace y posicionamiento preferencial en búsquedas.
          </p>
        </div>
      </div>

      {credits.error && <ErrorBox message={credits.error} />}

      {/* HISTORIAL EXCLUSIVO DE MOVIMIENTOS DE VINKOS */}
      <div id="historial-vinkos" style={{ scrollMarginTop: 30, marginBottom: 40 }}>
        <Section
          title="Historial de movimientos de Vinkos"
          action={
            <button
              type="button"
              className="link-button"
              onClick={credits.reload}
              style={{ display: "inline-flex", alignItems: "center", gap: 5 }}
            >
              <RefreshCw size={14} /> Recargar
            </button>
          }
        >
          {credits.loading ? (
            <Loading />
          ) : credits.data?.ledger?.length ? (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Concepto / Detalle</th>
                    <th>Fecha</th>
                    <th>Saldo resultante</th>
                    <th style={{ textAlign: "right" }}>Variación</th>
                  </tr>
                </thead>
                <tbody>
                  {credits.data.ledger.map((l: Data) => (
                    <tr key={l.id}>
                      <td>
                        <strong>{l.reason || "Movimiento de Vinkos"}</strong>
                        <br />
                        <small className="muted">
                          {l.kind === "welcome"
                            ? "Bono de bienvenida"
                            : l.kind === "contract_usage" || l.kind === "service_contact"
                              ? "Contratación de servicio"
                              : l.kind === "package_purchase" || l.kind === "vinko_purchase"
                                ? "Paquete adquirido"
                                : l.kind === "vinko_conversion"
                                  ? "Acreditación directa"
                                  : l.kind.replaceAll("_", " ")}
                        </small>
                      </td>
                      <td>{date(l.created_at)}</td>
                      <td>
                        {l.balance_after != null ? (
                          <span style={{ fontWeight: 600 }}>{l.balance_after} Vinkos</span>
                        ) : (
                          <span className="muted">—</span>
                        )}
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <strong
                          style={{
                            color: l.delta > 0 ? "#16a34a" : "#dc2626",
                            fontSize: 15,
                          }}
                        >
                          {l.delta > 0 ? `+${l.delta}` : l.delta} Vinkos
                        </strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty
              title="Tu saldo de Vinkos está listo"
              text="Aquí aparecerán tus recargas de paquetes, bonificaciones y los Vinkos utilizados en contrataciones."
            />
          )}
        </Section>
      </div>

      {/* PLANES Y PAQUETES OFICIALES DE VINKOS */}
      <div id="planes-vinkos" style={{ scrollMarginTop: 30 }}>
        <div className="section-head spaced">
          <div>
            <div className="eyebrow">PAQUETES OFICIALES</div>
            <h2>Planes y Paquetes de Vinkos</h2>
            <p className="muted">
              Elige el paquete que mejor se adapte a tu volumen de contrataciones y potencia tu alcance en SoyVinko.
            </p>
          </div>
          <div
            className="credit-counter"
            style={{
              background: "linear-gradient(135deg, #FAF7FF 0%, #F1EAFF 100%)",
              borderColor: "#D8C7FF",
            }}
          >
            <VinkoIconCoin size={24} strokeWidth={2.2} />
            <strong style={{ color: "#6F3DFF" }}>{vinkoBalance}</strong>
            <span style={{ color: "#6F3DFF", fontWeight: 600 }}>Vinkos disponibles</span>
          </div>
        </div>

        {plans.loading ? (
          <Loading />
        ) : plans.data?.length ? (
          <div className="plans-grid">
            {plans.data.map((p) => (
              <section className="panel plan-card" key={p.id}>
                <span className="eyebrow">PAQUETE OFICIAL</span>
                <h3 style={{ margin: "6px 0 12px", fontSize: 18 }}>{p.name}</h3>

                <div
                  style={{
                    background: "linear-gradient(135deg, #FAF7FF 0%, #F5EEFF 100%)",
                    border: "1.5px solid #E4DAFB",
                    borderRadius: 12,
                    padding: "16px",
                    margin: "12px 0 18px",
                    textAlign: "center",
                  }}
                >
                  <span
                    style={{
                      fontSize: 11,
                      fontWeight: 700,
                      textTransform: "uppercase",
                      letterSpacing: 1,
                      color: "#6F3DFF",
                    }}
                  >
                    INCLUYE
                  </span>
                  <div
                    style={{
                      fontSize: 32,
                      fontWeight: 800,
                      color: "#6F3DFF",
                      letterSpacing: -1,
                      marginTop: 2,
                    }}
                  >
                    +{p.credits} Vinkos
                  </div>
                  <small style={{ color: "#76708F", fontSize: 12 }}>
                    Para contrataciones y acuerdos
                  </small>
                </div>

                <ul style={{ minHeight: 90 }}>
                  <li style={{ fontWeight: 600, color: "#6F3DFF" }}>
                    ✦ {p.credits} Vinkos oficiales incluidos
                  </li>
                  {p.positioning_days > 0 && (
                    <li>{p.positioning_days} días de posicionamiento destacado</li>
                  )}
                  {p.marketplace_days > 0 && (
                    <li>{p.marketplace_days} días de tienda en el marketplace</li>
                  )}
                  {p.max_products > 0 && (
                    <li>Carga de catálogo (hasta {p.max_products} productos)</li>
                  )}
                </ul>

                <RunButton
                  className="button primary"
                  run={async () => {
                    const id = await api("start_purchase", {
                      plan: p.id,
                      key: newKey(),
                    });
                    setPurchase({ id, plan: p });
                    purchases.reload();
                  }}
                >
                  Elegir plan (+{p.credits} Vinkos)
                </RunButton>
              </section>
            ))}
          </div>
        ) : (
          <Empty title="No hay planes disponibles por el momento." />
        )}

        {/* COMPRAS PENDIENTES DE ACTIVACIÓN */}
        {purchases.data?.some((p) => p.status === "pending") && (
          <div style={{ marginTop: 24 }}>
            <Section title="Paquetes de Vinkos en proceso">
              {purchases.data
                .filter((p) => p.status === "pending")
                .map((p) => (
                  <div className="panel row between" key={p.id}>
                    <div>
                      <strong>Paquete: {p.credits} Vinkos</strong>
                      <p className="muted small" style={{ margin: "2px 0 0" }}>
                        Registrado el {date(p.created_at)}
                      </p>
                    </div>
                    {sandbox ? (
                      <RunButton
                        run={async () => {
                          await request(
                            `/api/v1/integrations/sandbox/purchase/${p.id}/settle`,
                            "POST",
                          );
                          setSuccessMsg(
                            `¡Paquete acreditado! Se han sumado ${p.credits} Vinkos a tu saldo.`,
                          );
                          refresh();
                        }}
                      >
                        Simular activación de Vinkos
                      </RunButton>
                    ) : (
                      <Badge value="pending" />
                    )}
                  </div>
                ))}
            </Section>
          </div>
        )}
      </div>

      {/* MODAL DE COMPRA / ACTIVACIÓN DE PLAN DE VINKOS */}
      {purchase && (
        <Modal
          title="Adquisición de Plan de Vinkos"
          onClose={() => setPurchase(null)}
        >
          <div style={{ textAlign: "center", padding: "10px 0 16px" }}>
            <div
              style={{
                width: 60,
                height: 60,
                borderRadius: "50%",
                background: "linear-gradient(135deg, #FFD76A 0%, #FFA800 100%)",
                color: "#331A00",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 14,
                boxShadow: "0 6px 20px rgba(255, 168, 0, 0.4)",
              }}
            >
              <VinkoIconCoin size={34} strokeWidth={2.4} />
            </div>

            <h3 style={{ margin: "0 0 6px", fontSize: 20 }}>
              {purchase.plan.name}
            </h3>
            <div
              style={{
                fontSize: 32,
                fontWeight: 800,
                color: "#6F3DFF",
                margin: "10px 0",
              }}
            >
              +{purchase.plan.credits} Vinkos
            </div>
            <p className="muted" style={{ margin: "0 auto", maxWidth: 400 }}>
              Este paquete otorgará <strong>{purchase.plan.credits} Vinkos oficiales</strong> directamente a tu saldo para nuevas contrataciones.
            </p>
          </div>

          <div
            style={{
              background: "#FAF8FF",
              border: "1px solid #ECE4FF",
              borderRadius: 12,
              padding: "16px 20px",
              margin: "16px 0 20px",
            }}
          >
            <div style={{ fontSize: 13, fontWeight: 700, color: "#1E1238", marginBottom: 8 }}>
              Beneficios incluidos en este paquete:
            </div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, color: "#504867", lineHeight: 1.6 }}>
              <li>{purchase.plan.credits} Vinkos acreditados de forma inmediata</li>
              {purchase.plan.positioning_days > 0 && (
                <li>{purchase.plan.positioning_days} días de posicionamiento destacado</li>
              )}
              {purchase.plan.marketplace_days > 0 && (
                <li>{purchase.plan.marketplace_days} días de tienda en el marketplace</li>
              )}
            </ul>
          </div>

          {sandbox ? (
            <div style={{ textAlign: "center", width: "100%" }}>
              <RunButton
                className="button primary"
                run={async () => {
                  await request(
                    `/api/v1/integrations/sandbox/purchase/${purchase.id}/settle`,
                    "POST",
                  );
                  setSuccessMsg(
                    `¡Felicidades! Has adquirido el plan exitosamente. Se sumaron ${purchase.plan.credits} Vinkos a tu cuenta.`,
                  );
                  setPurchase(null);
                  refresh();
                }}
              >
                <Sparkles size={17} /> Confirmar y activar {purchase.plan.credits} Vinkos
              </RunButton>
            </div>
          ) : (
            <div style={{ textAlign: "center" }}>
              <p className="info-note">
                El pedido ha sido iniciado. Se acreditarán los Vinkos una vez validado el comprobante.
              </p>
              <button
                type="button"
                className="button primary"
                style={{ width: "100%", marginTop: 10 }}
                onClick={() => setPurchase(null)}
              >
                Entendido
              </button>
            </div>
          )}
        </Modal>
      )}

      {/* MODAL INFORMATIVO: ¿CÓMO FUNCIONAN LOS VINKOS? */}
      {infoModal && (
        <Modal
          title="¿Qué son los Vinkos y cómo utilizarlos?"
          onClose={() => setInfoModal(false)}
        >
          <div className="vinko-info-modal-list">
            <div className="vinko-info-step">
              <div className="vinko-info-step-icon">
                <Coins size={20} />
              </div>
              <div className="vinko-info-step-content">
                <h4>Moneda Oficial Unificada</h4>
                <p>
                  Los Vinkos son el activo interno de SoyVinko, diseñados para que clientes y profesionales gestionen sus acuerdos y servicios de forma directa y sin complicaciones de divisas.
                </p>
              </div>
            </div>

            <div className="vinko-info-step">
              <div className="vinko-info-step-icon">
                <Zap size={20} />
              </div>
              <div className="vinko-info-step-content">
                <h4>1 Vinko = 1 Contratación</h4>
                <p>
                  Cada vez que inicias o aceptas una contratación directa con un cliente o profesional calificado, se descuenta 1 Vinko de tu saldo disponible.
                </p>
              </div>
            </div>

            <div className="vinko-info-step">
              <div className="vinko-info-step-icon">
                <ShieldCheck size={20} />
              </div>
              <div className="vinko-info-step-content">
                <h4>Protección Integral</h4>
                <p>
                  Todas las contrataciones respaldadas con Vinkos cuentan con verificación de identidad, valoraciones reales y mediación del equipo de SoyVinko en caso de incidencias.
                </p>
              </div>
            </div>

            <div className="vinko-info-step">
              <div className="vinko-info-step-icon">
                <Award size={20} />
              </div>
              <div className="vinko-info-step-content">
                <h4>Paquetes y Beneficios Extra</h4>
                <p>
                  Adquiere paquetes oficiales para disfrutar de mayor presencia en el buscador, insignias destacadas y días de vitrina comercial en el marketplace.
                </p>
              </div>
            </div>
          </div>

          <div style={{ marginTop: 22 }}>
            <button
              type="button"
              className="button primary"
              style={{ width: "100%", padding: 12 }}
              onClick={() => setInfoModal(false)}
            >
              Entendido
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
