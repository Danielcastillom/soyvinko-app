import { useState } from "react";
import {
  ShieldCheck,
  Users,
  FileCheck,
  Settings2,
  ArrowRight,
  Eye,
  Download,
  Store,
  ShoppingBag,
  Package,
  Briefcase,
  Calculator,
  TrendingUp,
  CheckCircle2,
  Building2,
  Sparkles,
  DollarSign,
  Layers,
  Percent,
  Clock,
  MapPin,
  Laptop,
  Coins,
} from "lucide-react";
import { api, request, money, date, type Data } from "../api";
import { useAuth } from "../context";
import {
  useData,
  PageTitle,
  Section,
  Empty,
  Loading,
  Badge,
  RunButton,
  Action,
  FieldsForm,
  Modal,
  reasonField,
  currencyField,
  ErrorBox,
  type FieldSpec,
} from "../components";

const resources = [
  ["contracts", "Servicios Contratados", "platform.read"],
  ["stores", "Tiendas Marketplace", "platform.read"],
  ["orders", "Pedidos Marketplace", "platform.read"],
  ["products", "Inventario Marketplace", "platform.read"],
  ["plans", "Modelo de Negocio & Planes", "plans.manage"],
  ["requests", "Solicitudes", "content.moderate"],
  ["users", "Usuarios", "users.manage"],
  ["kyc", "Identidad", "kyc.review"],
  ["tickets", "Soporte", "support.read"],
  ["fees", "Comisión de retiro", "money.configure"],
  ["vinko_rates", "Tasa de Cambio Vinkos", "money.configure"],
  ["withdrawals", "Retiros", "money.configure"],
  ["reviews", "Reseñas", "content.moderate"],
  ["files", "Archivos", "content.moderate"],
  ["audit", "Auditoría", "platform.read"],
];

const columns: Record<string, [string, string][]> = {
  contracts: [
    ["request_title", "Servicio Contratado"],
    ["delivery_mode", "Modalidad"],
    ["customer_name", "Cliente"],
    ["professional_name", "Profesional"],
    ["plan_code", "Plan Proveedor"],
    ["agreed_price_minor", "Precio Acordado"],
    ["fee_projected", "Comisión Modelo"],
    ["status", "Estado"],
    ["accepted_at", "Fecha"],
  ],
  stores: [
    ["name", "Comercio"],
    ["owner_name", "Titular"],
    ["plan_code", "Membresía / Plan"],
    ["products_count", "Productos"],
    ["orders_count", "Pedidos"],
    ["status", "Estado"],
    ["created_at", "Fecha Alta"],
  ],
  orders: [
    ["id", "N° Pedido"],
    ["store_name", "Tienda"],
    ["customer_name", "Comprador"],
    ["total_minor", "Total"],
    ["fee_projected", "Comisión Plataforma"],
    ["status", "Estado Pedido"],
    ["payment_status", "Reporte Pago"],
    ["created_at", "Fecha"],
  ],
  products: [
    ["name", "Producto"],
    ["sku", "SKU"],
    ["store_name", "Comercio"],
    ["price_minor", "Precio"],
    ["stock", "Stock"],
    ["status", "Estado"],
    ["created_at", "Publicado"],
  ],
  plans: [
    ["name", "Nombre del Plan"],
    ["affiliate_type", "Tipo Afiliado"],
    ["price_minor", "Costo Mensual"],
    ["fee_summary", "Comisión por Venta / Servicio"],
    ["max_products", "Límite Inventario"],
    ["active", "Activo"],
  ],
  requests: [
    ["title", "Solicitud"],
    ["delivery_mode", "Modalidad"],
    ["status", "Estado"],
    ["moderation_status", "Revisión"],
  ],
  files: [
    ["purpose", "Finalidad"],
    ["mime_type", "Formato"],
    ["bytes", "Bytes"],
    ["moderation_status", "Revisión"],
  ],
  reviews: [
    ["rating", "Puntuación"],
    ["comment", "Comentario"],
    ["moderation_status", "Estado"],
  ],
  kyc: [
    ["provider", "Proceso"],
    ["submitted_at", "Fecha"],
    ["status", "Estado"],
  ],
  users: [
    ["display_name", "Nombre"],
    ["email", "Correo"],
    ["status", "Estado"],
  ],
  tickets: [
    ["subject", "Asunto"],
    ["category", "Tema"],
    ["status", "Estado"],
  ],
  fees: [
    ["currency", "Moneda"],
    ["basis_points", "Comisión (%)"],
    ["active", "Vigente"],
  ],
  vinko_rates: [
    ["currency", "Moneda"],
    ["rate_minor", "Tasa por 1 Vinko"],
    ["status", "Estado"],
    ["reason", "Motivo de la actualización"],
    ["creator_name", "Administrador"],
    ["created_at", "Fecha"],
  ],
  withdrawals: [
    ["created_at", "Fecha"],
    ["amount_minor", "Importe"],
    ["fee_minor", "Comisión"],
    ["net_minor", "Neto"],
    ["status", "Estado"],
  ],
  audit: [
    ["created_at", "Fecha"],
    ["action", "Acción"],
    ["entity_type", "Recurso"],
    ["entity_id", "Referencia"],
  ],
};

const statusFields: FieldSpec[] = [
  {
    name: "status",
    label: "Estado del caso",
    options: [
      ["open", "Abierto"],
      ["in_progress", "En atención"],
      ["waiting_user", "Esperando al usuario"],
      ["resolved", "Resuelto"],
      ["closed", "Cerrado"],
    ].map(([value, label]) => ({ value, label })),
  },
  { name: "note", label: "Nota interna de gestión", type: "textarea" },
];

export default function Admin() {
  const { user } = useAuth(),
    allowed = resources.filter((r) => user?.permissions.includes(r[2])),
    [resource, setResource] = useState(allowed[0]?.[0] ?? ""),
    [page, setPage] = useState(0),
    [ticket, setTicket] = useState<Data | null>(null),
    [kyc, setKyc] = useState<Data | null>(null),
    [contractDetail, setContractDetail] = useState<Data | null>(null),
    [orderDetail, setOrderDetail] = useState<Data | null>(null),
    [storeDetail, setStoreDetail] = useState<Data | null>(null),
    [activeBusinessTab, setActiveBusinessTab] = useState<"stores" | "providers">("stores");

  const canStats = user?.permissions.includes("platform.read");
  const stats = useData(
    () => (canStats ? api("admin_overview") : Promise.resolve(null)),
    [canStats],
  );
  const rows = useData(
    () =>
      resource
        ? api<Data[]>("admin_list", {
            resource,
            limit: 25,
            offset: page * 25,
          })
        : Promise.resolve([]),
    [resource, page],
  );
  const config = useData(() => request("/api/v1/config"), []);
  const cats = useData(() => api<Data[]>("catalog"), []);

  const refresh = () => {
    rows.reload();
    stats.reload();
  };

  const viewTicket = async (id: string) =>
    setTicket(
      await api("support_ticket", {
        ticket: id,
        reason: "Consulta para atender el caso desde el panel de soporte.",
      }),
    );

  function renderValue(row: Data, key: string) {
    const v = row[key];
    if (key === "rate_minor" && row.rate_minor !== undefined) {
      return (
        <span style={{ fontWeight: 700, color: "#8D55F9", fontSize: 15 }}>
          {money(v, row.currency)}
        </span>
      );
    }
    if (key === "status" && resource === "vinko_rates") {
      return v === "active" ? (
        <span style={{ background: "#EBFBEE", color: "#166534", padding: "4px 10px", borderRadius: 20, fontSize: 12, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 5 }}>
          <span style={{ width: 7, height: 7, borderRadius: "50%", background: "#22C55E" }}></span>
          Vigente
        </span>
      ) : (
        <span style={{ background: "#F1F5F9", color: "#64748B", padding: "4px 10px", borderRadius: 20, fontSize: 12 }}>
          Histórica
        </span>
      );
    }
    if (key === "id" && typeof v === "string") {
      return (
        <span style={{ fontFamily: "monospace", fontSize: 12 }}>
          #{v.slice(0, 8)}
        </span>
      );
    }
    if (key === "delivery_mode") {
      return v === "onsite" ? (
        <span className="badge badge-onsite">📍 Presencial</span>
      ) : (
        <span className="badge badge-remote">💻 Remoto</span>
      );
    }
    if (key === "plan_code") {
      const planLabels: Record<string, string> = {
        "STORE-PROMO": "Promo Entrada (0-3m)",
        "STORE-STANDARD-30": "Estándar $30 (30 prods)",
        "STORE-PLUS-50": "Plus $50 (70 prods)",
        "STORE-BASE": "Base 10% s/v",
        "PRO-PROMO": "Promo Entrada (0-3m)",
        "PRO-MEMBERSHIP-15": "Membresía Pro $15 (3%/6%)",
        "PRO-BASE": "Base 0 USD (8%/12%)",
      };
      return <span className="badge badge-plan">{planLabels[v] || v}</span>;
    }
    if (key === "fee_projected") {
      // Para Contratos de Servicios
      if (row.delivery_mode) {
        const mode = row.delivery_mode;
        const plan = row.plan_code;
        if (plan === "PRO-MEMBERSHIP-15") {
          return mode === "onsite" ? "3% Presencial" : "6% Remoto";
        } else if (plan === "PRO-BASE") {
          return mode === "onsite" ? "8% Presencial" : "12% Remoto";
        } else {
          return "0% - 3% (Promo Entrada)";
        }
      }
      // Para Pedidos de Marketplace
      if (row.store_name) {
        return "4% - 10% según membresía";
      }
      return "Tarifa fijada";
    }
    if (key === "affiliate_type") {
      return v === "store" ? (
        <span className="badge badge-store">🏪 Comercio</span>
      ) : (
        <span className="badge badge-provider">🛠️ Proveedor</span>
      );
    }
    if (v == null) return "—";
    if (key.includes("status")) return <Badge value={v} />;
    if (key.endsWith("_at")) return date(v);
    if (key.endsWith("_minor")) return money(v, row.currency ?? "USD");
    if (key === "basis_points") return `${v / 100}%`;
    if (key === "max_products") return v > 0 ? `${v} productos` : "Sin límite / No aplica";
    if (typeof v === "boolean") return v ? "Sí" : "No";
    return String(v);
  }

  return (
    <>
      <PageTitle
        eyebrow="PANEL DE CONTROL GENERAL"
        title="Administración & Modelo de Negocio"
        description="Supervisa servicios contratados, comercios del marketplace, inventarios, planes de membresía y comisiones."
      />

      {stats.data && (
        <div className="stats-grid">
          {[
            ["contracts", "Servicios Contratados", <Briefcase size={16} key="c" />],
            ["active_contracts", "Servicios en curso", <ShieldCheck size={16} key="ac" />],
            ["stores", "Comercios Marketplace", <Store size={16} key="st" />],
            ["orders", "Pedidos Marketplace", <ShoppingBag size={16} key="od" />],
            ["products", "Inventario activo", <Package size={16} key="pr" />],
            ["professionals", "Profesionales", <Users size={16} key="pf" />],
            ["users", "Usuarios totales", <Users size={16} key="us" />],
            ["pending_requests", "Solicitudes en revisión", <FileCheck size={16} key="rq" />],
            ["open_tickets", "Casos abiertos", <Settings2 size={16} key="tk" />],
          ].map(([key, title, icon]) => (
            <div className="stat" key={key as string}>
              <div className="stat-label">
                {icon} <small>{title as string}</small>
              </div>
              <strong>{stats.data[key as string] ?? 0}</strong>
            </div>
          ))}
        </div>
      )}

      {/* VISTA ESPECIAL DEL MODELO DE NEGOCIO Y TARIFAS OFICIALES */}
      {resource === "plans" && (
        <div className="business-model-section">
          <div className="business-model-header">
            <div className="business-model-title-group">
              <h2>
                <Sparkles size={20} className="text-accent" />
                Modelo de Negocio Oficial de Vinko
              </h2>
              <p>
                Estructura de membresías mensuales, comisiones por venta/servicio y beneficios para Comercios y Proveedores.
              </p>
            </div>
            <div className="business-model-tabs-toggle">
              <button
                className={activeBusinessTab === "stores" ? "active" : ""}
                onClick={() => setActiveBusinessTab("stores")}
              >
                <Store size={15} /> Comercios y Vendedores (Tiendas / Marketplace)
              </button>
              <button
                className={activeBusinessTab === "providers" ? "active" : ""}
                onClick={() => setActiveBusinessTab("providers")}
              >
                <Briefcase size={15} /> Proveedores (Servicios)
              </button>
            </div>
          </div>

          {activeBusinessTab === "stores" ? (
            <div className="business-plans-grid">
              <div className="business-plan-card">
                <span className="business-plan-badge-top">Meses 1 al 3</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Comercios</span>
                  <h3>Plan Promocional Entrada</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $0,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Venta:</strong>
                  <span>0% (Primer mes) / 3% (Meses 2 y 3)</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Registro gratis de la tienda en marketplace.</li>
                  <li><CheckCircle2 size={15} /> Indexación en motor de búsqueda instantánea.</li>
                  <li><CheckCircle2 size={15} /> Carga de inventario de hasta 15 productos.</li>
                </ul>
              </div>

              <div className="business-plan-card featured">
                <span className="business-plan-badge-top">Desde Mes 4</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Membresía Estándar</span>
                  <h3>Membresía Comercio Estándar</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $30,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Venta:</strong>
                  <span>Tasa Mínima 4%</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Mayor visibilidad en búsquedas y destacados.</li>
                  <li><CheckCircle2 size={15} /> Botón de compra exprés directa de productos.</li>
                  <li><CheckCircle2 size={15} /> Acceso a panel de estadísticas de ventas.</li>
                  <li><CheckCircle2 size={15} /> Carga de inventario de hasta 30 productos.</li>
                </ul>
              </div>

              <div className="business-plan-card featured">
                <span className="business-plan-badge-top">Desde Mes 4</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Membresía Plus</span>
                  <h3>Membresía Comercio Plus</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $50,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Venta:</strong>
                  <span>Tasa Mínima 4%</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Máxima visibilidad en catálogo y búsquedas.</li>
                  <li><CheckCircle2 size={15} /> Botón de compra exprés directa de productos.</li>
                  <li><CheckCircle2 size={15} /> Acceso a estadísticas de ventas avanzadas.</li>
                  <li><CheckCircle2 size={15} /> Carga de inventario extendido de hasta 70 productos.</li>
                </ul>
              </div>

              <div className="business-plan-card">
                <span className="business-plan-badge-top">Desde Mes 4</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Sin Suscripción</span>
                  <h3>Modalidad Base Sin Costo Fijo</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $0,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Venta:</strong>
                  <span>10% por venta</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Permanencia en la app sin costo fijo mensual.</li>
                  <li><CheckCircle2 size={15} /> Ideal para comercios con muy bajo volumen inicial.</li>
                  <li><CheckCircle2 size={15} /> Carga de inventario básico (hasta 10 productos).</li>
                </ul>
              </div>
            </div>
          ) : (
            <div className="business-plans-grid">
              <div className="business-plan-card">
                <span className="business-plan-badge-top">Meses 1 al 3</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Proveedores</span>
                  <h3>Plan Promocional Entrada</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $0,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Servicio:</strong>
                  <span>0% (Primer mes) / 3% (Meses 2 y 3)</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Certificación de perfil profesional gratis.</li>
                  <li><CheckCircle2 size={15} /> Acceso completo a la cartelera de solicitudes.</li>
                  <li><CheckCircle2 size={15} /> Recepción de solicitudes directas de clientes.</li>
                </ul>
              </div>

              <div className="business-plan-card featured">
                <span className="business-plan-badge-top">Desde Mes 4</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Membresía Profesional</span>
                  <h3>Membresía Profesional</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $15,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Servicio:</strong>
                  <span>Tasa Mínima: 3% Presencial / 6% Remoto</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> Prioridad exclusiva para recibir solicitudes directas.</li>
                  <li><CheckCircle2 size={15} /> Descuento en comisión para mitigar gastos presenciales (gasolina, viáticos).</li>
                  <li><CheckCircle2 size={15} /> Mayor posicionamiento en el directorio de profesionales.</li>
                </ul>
              </div>

              <div className="business-plan-card">
                <span className="business-plan-badge-top">Desde Mes 4</span>
                <div className="business-plan-header">
                  <span className="business-plan-tag">Sin Suscripción</span>
                  <h3>Modalidad Base Sin Mensualidad</h3>
                </div>
                <div className="business-plan-pricing">
                  <div className="business-plan-cost">
                    $0,00 <small>USD / mes</small>
                  </div>
                </div>
                <div className="business-plan-commission-box">
                  <strong>Comisión por Servicio:</strong>
                  <span>8% Presencial / 12% Remoto</span>
                </div>
                <ul className="business-plan-benefits-list">
                  <li><CheckCircle2 size={15} /> No paga mensualidad ni tarifa fija.</li>
                  <li><CheckCircle2 size={15} /> Ideal para trabajadores ocasionales o remotos con menos costos físicos.</li>
                  <li><CheckCircle2 size={15} /> Cobro únicamente al concertar y liquidar el servicio.</li>
                </ul>
              </div>
            </div>
          )}

          {/* SIMULADOR INTERACTIVO DE COMISIONES */}
          <CommissionSimulator />
        </div>
      )}

      <div className="tabs admin-tabs">
        {allowed.map(([key, title]) => (
          <button
            className={resource === key ? "active" : ""}
            key={key}
            onClick={() => {
              setResource(key);
              setPage(0);
            }}
          >
            {title}
          </button>
        ))}
      </div>

      <Section
        title={allowed.find((r) => r[0] === resource)?.[1] ?? "Gestión"}
        action={
          <div className="row wrap">
            {resource === "plans" && (
              <Action
                title="Configurar plan"
                fields={[
                  { name: "code", label: "Código comercial (ej: STORE-CUSTOM)" },
                  { name: "name", label: "Nombre del plan" },
                  {
                    name: "credits",
                    label: "Créditos incluidos",
                    type: "number",
                    min: 0,
                    value: 0,
                  },
                  currencyField,
                  { name: "amount", label: "Precio mensual", type: "number", min: 0 },
                  {
                    name: "weight",
                    label: "Prioridad de posicionamiento",
                    type: "number",
                    min: 0,
                    value: 0,
                  },
                  {
                    name: "positioning_days",
                    label: "Días de posicionamiento",
                    type: "number",
                    min: 0,
                    value: 30,
                  },
                  {
                    name: "marketplace_days",
                    label: "Días de marketplace",
                    type: "number",
                    min: 0,
                    value: 30,
                  },
                  {
                    name: "active",
                    label: "Plan disponible",
                    type: "checkbox",
                    value: true,
                  },
                ]}
                action={(v) =>
                  api("configure_plan", {
                    ...Object.fromEntries(
                      Object.entries(v).filter(([k]) => k !== "amount"),
                    ),
                    price: Math.round(v.amount * 100),
                  })
                }
                after={refresh}
              />
            )}
            {resource === "fees" && (
              <Action
                title="Configurar comisión"
                fields={[
                  currencyField,
                  {
                    name: "percentage",
                    label: "Porcentaje de retiro",
                    type: "number",
                    min: 0,
                    max: 99,
                    step: "0.01",
                  },
                  reasonField,
                ]}
                action={(v) =>
                  api("configure_withdrawal_fee", {
                    currency: v.currency,
                    basis_points: Math.round(v.percentage * 100),
                    reason: v.reason,
                  })
                }
                after={refresh}
                description="Solo aplica al retiro de saldo. Recargas, uso, conservación y liberación del pago siguen sin comisión."
              />
            )}
            {resource === "vinko_rates" && (
              <Action
                title="Personalizar Tasa de Cambio Vinkos"
                fields={[
                  currencyField,
                  {
                    name: "rate",
                    label: "Valor de 1 Vinko en moneda seleccionada (ej: 50.00 Bs ó 1.00 USD)",
                    type: "number",
                    min: 0.01,
                    max: 10000000,
                    step: "0.01",
                    required: true,
                  },
                  reasonField,
                ]}
                action={(v) =>
                  api("configure_vinko_exchange_rate", {
                    currency: v.currency,
                    rate_minor: Math.round(Number(v.rate) * 100),
                    reason: v.reason,
                  })
                }
                after={refresh}
                description="Establece la tasa oficial de conversión para créditos Vinkos. Toda la plataforma, recargas, conversiones y wallet actualizarán sus valores en tiempo real."
              />
            )}
            <button className="link-button" onClick={refresh}>
              Actualizar
            </button>
          </div>
        }
      >
        {resource === "vinko_rates" && (
          <div
            className="vinko-admin-overview"
            style={{
              marginBottom: 24,
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
              gap: 16,
            }}
          >
            {["VES", "USD"].map((curr) => {
              const activeRate = rows.data?.find(
                (r: Data) => r.currency === curr && r.active,
              );
              return (
                <div
                  key={curr}
                  style={{
                    background:
                      "linear-gradient(135deg, #1C0F38 0%, #2E155C 100%)",
                    color: "#fff",
                    padding: "20px 24px",
                    borderRadius: 12,
                    border: "1px solid rgba(255,255,255,0.1)",
                    boxShadow: "0 8px 24px rgba(21,13,43,0.18)",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: 8,
                    }}
                  >
                    <span
                      style={{
                        fontSize: 13,
                        textTransform: "uppercase",
                        letterSpacing: 1,
                        color: "#C6B2FF",
                        fontWeight: 600,
                        display: "flex",
                        alignItems: "center",
                        gap: 6,
                      }}
                    >
                      <Coins size={16} /> Tasa Oficial{" "}
                      {curr === "VES" ? "Bolívares (VES)" : "Dólares (USD)"}
                    </span>
                    <span
                      style={{
                        background: "#22C55E22",
                        color: "#4ADE80",
                        border: "1px solid #22C55E55",
                        padding: "2px 8px",
                        borderRadius: 12,
                        fontSize: 11,
                        fontWeight: 700,
                      }}
                    >
                      Vigente
                    </span>
                  </div>
                  <div
                    style={{
                      fontSize: 30,
                      fontWeight: 800,
                      fontFamily: "Manrope",
                      color: "#FFD76A",
                      display: "flex",
                      alignItems: "baseline",
                      gap: 8,
                    }}
                  >
                    <span style={{ fontSize: 18, color: "#fff" }}>1 Vinko =</span>
                    <span>
                      {activeRate
                        ? money(activeRate.rate_minor, curr)
                        : curr === "VES"
                          ? "Bs 50,00"
                          : "$ 1,00"}
                    </span>
                  </div>
                  <p
                    style={{
                      margin: "8px 0 0",
                      fontSize: 12,
                      color: "#C6B2FF",
                      opacity: 0.9,
                    }}
                  >
                    {activeRate?.reason
                      ? `Motivo: ${activeRate.reason}`
                      : "Tasa personalizada configurada para el ecosistema SoyVinko"}
                  </p>
                </div>
              );
            })}
          </div>
        )}
        {rows.loading ? (
          <Loading />
        ) : rows.error ? (
          <ErrorBox message={rows.error} retry={rows.reload} />
        ) : rows.data?.length ? (
          <div className="table-scroll">
            <table className="admin-table">
              <thead>
                <tr>
                  {columns[resource]?.map(([key, title]) => (
                    <th key={key}>{title}</th>
                  ))}
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {rows.data.map((r, i) => (
                  <tr key={r.id ?? i}>
                    {columns[resource]?.map(([key]) => (
                      <td key={key}>{renderValue(r, key)}</td>
                    ))}
                    <td>
                      <div className="row wrap admin-actions">
                        {/* ACCIONES PARA SERVICIOS CONTRATADOS */}
                        {resource === "contracts" && (
                          <>
                            <button
                              className="button small secondary"
                              onClick={() => setContractDetail(r)}
                            >
                              <Eye size={15} /> Ver expediente
                            </button>
                            <Action
                              title="Asignar plan proveedor"
                              fields={[
                                {
                                  name: "plan",
                                  label: "Plan del Modelo de Negocio",
                                  value: r.plan_code || "PRO-PROMO",
                                  options: [
                                    {
                                      value: "PRO-PROMO",
                                      label: "Plan Promocional (0-3 meses · 0%-3%)",
                                    },
                                    {
                                      value: "PRO-MEMBERSHIP-15",
                                      label: "Membresía Profesional ($15 USD · 3% presencial / 6% remoto)",
                                    },
                                    {
                                      value: "PRO-BASE",
                                      label: "Modalidad Base ($0 USD · 8% presencial / 12% remoto)",
                                    },
                                  ],
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("set_professional_plan", {
                                  professional: r.professional_id,
                                  plan: v.plan,
                                  reason: v.reason,
                                })
                              }
                              after={refresh}
                              description="Ajusta el plan del profesional para calcular sus comisiones presenciales o remotas."
                            />
                          </>
                        )}

                        {/* ACCIONES PARA TIENDAS MARKETPLACE */}
                        {resource === "stores" && (
                          <>
                            <button
                              className="button small secondary"
                              onClick={() => setStoreDetail(r)}
                            >
                              <Eye size={15} /> Ver comercio
                            </button>
                            <Action
                              title="Cambiar membresía"
                              fields={[
                                {
                                  name: "plan",
                                  label: "Plan del Comercio",
                                  value: r.plan_code || "STORE-PROMO",
                                  options: [
                                    {
                                      value: "STORE-PROMO",
                                      label: "Plan Promocional Entrada ($0 USD · 0%-3% · 15 prods)",
                                    },
                                    {
                                      value: "STORE-STANDARD-30",
                                      label: "Membresía Estándar ($30 USD · 4% mín · 30 prods)",
                                    },
                                    {
                                      value: "STORE-PLUS-50",
                                      label: "Membresía Plus ($50 USD · 4% mín · 70 prods)",
                                    },
                                    {
                                      value: "STORE-BASE",
                                      label: "Modalidad Base ($0 USD · 10% por venta · 10 prods)",
                                    },
                                  ],
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("set_store_plan", {
                                  store: r.id,
                                  plan: v.plan,
                                  reason: v.reason,
                                })
                              }
                              after={refresh}
                              description="Actualiza el plan de membresía del comercio conforme a su volumen y antigüedad."
                            />
                            <Action
                              title="Cambiar estado"
                              fields={[
                                {
                                  name: "status",
                                  label: "Estado de la tienda",
                                  value: r.status,
                                  options: [
                                    { value: "active", label: "Activa" },
                                    { value: "suspended", label: "Suspendida" },
                                    { value: "closed", label: "Cerrada" },
                                  ],
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("set_store_status", {
                                  store: r.id,
                                  status: v.status,
                                  reason: v.reason,
                                })
                              }
                              after={refresh}
                            />
                          </>
                        )}

                        {/* ACCIONES PARA PEDIDOS MARKETPLACE */}
                        {resource === "orders" && (
                          <>
                            <button
                              className="button small secondary"
                              onClick={() => setOrderDetail(r)}
                            >
                              <Eye size={15} /> Ver pedido
                            </button>
                            <Action
                              title="Intervención orden"
                              fields={[
                                {
                                  name: "action",
                                  label: "Acción administrativa",
                                  options: [
                                    { value: "accept", label: "Forzar aceptación del comercio" },
                                    { value: "dispatch", label: "Marcar como despachado" },
                                    { value: "receive", label: "Confirmar entrega final al comprador" },
                                    { value: "cancel", label: "Cancelar orden y reintegrar inventario" },
                                  ],
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("admin_advance_order", {
                                  order: r.id,
                                  action: v.action,
                                  reason: v.reason,
                                })
                              }
                              after={refresh}
                              description="Permite al soporte avanzar o cancelar el pedido ante incidencias entre comprador y comercio."
                            />
                          </>
                        )}

                        {/* ACCIONES PARA SOLICITUDES */}
                        {resource === "requests" &&
                          ["draft", "open"].includes(r.status) && (
                            <Action
                              title={
                                r.moderation_status === "approved"
                                  ? "Desautorizar"
                                  : "Autorizar publicación"
                              }
                              fields={[reasonField]}
                              action={(v) =>
                                api("moderate_request", {
                                  request: r.id,
                                  approve: r.moderation_status !== "approved",
                                  ...v,
                                })
                              }
                              after={refresh}
                            />
                          )}

                        {/* ACCIONES PARA ARCHIVOS */}
                        {resource === "files" && (
                          <>
                            <RunButton
                              run={() =>
                                downloadModeration(
                                  r.id,
                                  "Revisión de archivo en cola de moderación.",
                                )
                              }
                            >
                              Ver archivo
                            </RunButton>
                            <Action
                              title="Moderar"
                              fields={[
                                {
                                  name: "approve",
                                  label: "Aprobar archivo",
                                  type: "checkbox",
                                  value: true,
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("moderate_file", { file: r.id, ...v })
                              }
                              after={refresh}
                            />
                          </>
                        )}

                        {/* ACCIONES PARA RESEÑAS */}
                        {resource === "reviews" && (
                          <Action
                            title="Moderar reseña"
                            fields={[
                              {
                                name: "status",
                                label: "Visibilidad",
                                options: [
                                  { value: "published", label: "Publicar" },
                                  { value: "hidden", label: "Ocultar" },
                                ],
                              },
                              reasonField,
                            ]}
                            action={(v) =>
                              api("moderate_review", { review: r.id, ...v })
                            }
                            after={refresh}
                            description="La puntuación de la reseña no se puede cambiar."
                          />
                        )}

                        {/* ACCIONES PARA IDENTIDAD (KYC) */}
                        {resource === "kyc" && (
                          <>
                            <RunButton
                              run={async () =>
                                setKyc(
                                  await api("kyc_case", {
                                    case: r.id,
                                    reason:
                                      "Revisión del expediente para verificar identidad.",
                                  }),
                                )
                              }
                            >
                              Abrir expediente
                            </RunButton>
                            <Action
                              title="Resolver verificación"
                              fields={[
                                {
                                  name: "status",
                                  label: "Resultado",
                                  options: [
                                    { value: "approved", label: "Aprobada" },
                                    { value: "rejected", label: "Rechazada" },
                                    { value: "revoked", label: "Revocada" },
                                  ],
                                },
                                reasonField,
                                {
                                  name: "expires",
                                  label: "Válida hasta (si se aprueba)",
                                  type: "date",
                                  required: false,
                                },
                              ]}
                              action={(v) =>
                                api("review_kyc", {
                                  case: r.id,
                                  status: v.status,
                                  reason: v.reason,
                                  expires_at: v.expires
                                    ? new Date(
                                        v.expires + "T23:59:59Z",
                                      ).toISOString()
                                    : null,
                                })
                              }
                              after={refresh}
                            />
                          </>
                        )}

                        {/* ACCIONES PARA USUARIOS */}
                        {resource === "users" && (
                          <>
                            <Action
                              title="Cambiar estado"
                              fields={[
                                {
                                  name: "status",
                                  label: "Estado",
                                  value: r.status,
                                  options: [
                                    { value: "active", label: "Activo" },
                                    { value: "suspended", label: "Suspendido" },
                                    { value: "closed", label: "Cerrado" },
                                  ],
                                },
                                reasonField,
                              ]}
                              action={(v) =>
                                api("set_user_status", { user: r.id, ...v })
                              }
                              after={refresh}
                            />
                            {user?.permissions.includes("roles.manage") && (
                              <Action
                                title="Asignar rol"
                                fields={[
                                  {
                                    name: "role",
                                    label: "Rol",
                                    options: [
                                      "support",
                                      "admin",
                                      "superadmin",
                                    ].map((x) => ({
                                      value: x,
                                      label:
                                        x === "support"
                                          ? "Soporte"
                                          : x === "admin"
                                            ? "Administrador"
                                            : "Superadministrador",
                                    })),
                                  },
                                  {
                                    name: "enable",
                                    label: "Habilitar rol",
                                    type: "checkbox",
                                    value: true,
                                  },
                                  reasonField,
                                ]}
                                action={(v) =>
                                  api("assign_role", { user: r.id, ...v })
                                }
                                after={refresh}
                              />
                            )}
                          </>
                        )}

                        {/* ACCIONES PARA CASOS DE SOPORTE */}
                        {resource === "tickets" && (
                          <RunButton run={() => viewTicket(r.id)}>
                            Atender caso
                          </RunButton>
                        )}

                        {/* ACCIONES PARA RETIROS */}
                        {resource === "withdrawals" &&
                          ["pending", "processing"].includes(r.status) &&
                          config.data?.payment_mode === "sandbox" && (
                            <RunButton
                              confirm="Esta acción simula la confirmación bancaria del retiro. No mueve dinero real."
                              run={() =>
                                request(
                                  `/api/v1/integrations/sandbox/withdrawals/${r.id}/settle`,
                                  "POST",
                                )
                              }
                              after={refresh}
                            >
                              Simular retiro pagado
                            </RunButton>
                          )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title="La cola está al día"
            text="No hay elementos para mostrar en esta página."
          />
        )}
        <div className="pagination">
          <button
            className="button secondary"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Anterior
          </button>
          <span>Página {page + 1}</span>
          <button
            className="button secondary"
            disabled={(rows.data?.length ?? 0) < 25}
            onClick={() => setPage((p) => p + 1)}
          >
            Siguiente
          </button>
        </div>
      </Section>

      {/* MODAL DETALLE DE SERVICIO CONTRATADO */}
      {contractDetail && (
        <Modal
          title={`Expediente de Servicio #${contractDetail.id.slice(0, 8)}`}
          onClose={() => setContractDetail(null)}
        >
          <div className="admin-modal-card">
            <div className="admin-financial-box">
              <div>
                <small className="muted">PRECIO PACTADO</small>
                <div style={{ fontSize: 22, fontWeight: 800 }}>
                  {money(contractDetail.agreed_price_minor, contractDetail.currency)}
                </div>
              </div>
              <div>
                <small className="muted">ESTADO DEL SERVICIO</small>
                <div><Badge value={contractDetail.status} /></div>
              </div>
              <div>
                <small className="muted">MODALIDAD</small>
                <div>
                  {contractDetail.delivery_mode === "onsite" ? (
                    <span className="badge badge-onsite">📍 Presencial</span>
                  ) : (
                    <span className="badge badge-remote">💻 Remoto</span>
                  )}
                </div>
              </div>
            </div>

            <div className="admin-detail-grid">
              <div className="admin-detail-item">
                <span className="label">Requerimiento / Solicitud</span>
                <span className="value">{contractDetail.request_title}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Categoría</span>
                <span className="value">{contractDetail.category_name}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Protección de Pago</span>
                <span className="value">
                  {contractDetail.payment_protection === "guaranteed"
                    ? "🛡️ Custodia en Escrow"
                    : "Pago Directo Externo"}
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Cliente</span>
                <span className="value">
                  {contractDetail.customer_name} ({contractDetail.customer_email})
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Profesional Prestador</span>
                <span className="value">
                  {contractDetail.professional_name} ({contractDetail.professional_email})
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Plan Activo del Proveedor</span>
                <span className="value">
                  <span className="badge badge-plan">{contractDetail.plan_code}</span>
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Comisión Estimada</span>
                <span className="value" style={{ color: "#6F3DFF" }}>
                  {contractDetail.plan_code === "PRO-MEMBERSHIP-15"
                    ? contractDetail.delivery_mode === "onsite"
                      ? "3% Presencial (Tasa mínima)"
                      : "6% Remoto (Tasa mínima)"
                    : contractDetail.plan_code === "PRO-BASE"
                      ? contractDetail.delivery_mode === "onsite"
                        ? "8% Presencial"
                        : "12% Remoto"
                      : "0% - 3% (Promocional Entrada)"}
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Fecha Aceptado</span>
                <span className="value">{date(contractDetail.accepted_at)}</span>
              </div>
              {contractDetail.completed_at && (
                <div className="admin-detail-item">
                  <span className="label">Fecha Completado</span>
                  <span className="value">{date(contractDetail.completed_at)}</span>
                </div>
              )}
              {contractDetail.cancelled_at && (
                <div className="admin-detail-item" style={{ gridColumn: "1 / -1" }}>
                  <span className="label" style={{ color: "#DC2626" }}>Cancelado</span>
                  <span className="value">{contractDetail.cancellation_reason || "Sin motivo registrado"}</span>
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL DETALLE DE PEDIDO MARKETPLACE */}
      {orderDetail && (
        <Modal
          title={`Detalle de Pedido #${orderDetail.id.slice(0, 8)}`}
          onClose={() => setOrderDetail(null)}
        >
          <div className="admin-modal-card">
            <div className="admin-financial-box">
              <div>
                <small className="muted">TOTAL PEDIDO</small>
                <div style={{ fontSize: 22, fontWeight: 800 }}>
                  {money(orderDetail.total_minor, orderDetail.currency)}
                </div>
              </div>
              <div>
                <small className="muted">ESTADO DE ORDEN</small>
                <div><Badge value={orderDetail.status} /></div>
              </div>
              <div>
                <small className="muted">REPORTE DE PAGO</small>
                <div><Badge value={orderDetail.payment_status} /></div>
              </div>
            </div>

            <div className="admin-detail-grid">
              <div className="admin-detail-item">
                <span className="label">Comercio Vendedor</span>
                <span className="value">{orderDetail.store_name}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Titular de la Tienda</span>
                <span className="value">
                  {orderDetail.seller_name} ({orderDetail.seller_email})
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Comprador</span>
                <span className="value">
                  {orderDetail.customer_name} ({orderDetail.customer_email})
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Artículos Incluidos</span>
                <span className="value">{orderDetail.items_count} producto(s)</span>
              </div>
              <div className="admin-detail-item" style={{ gridColumn: "1 / -1" }}>
                <span className="label">Datos de Despacho / Entrega</span>
                <span className="value">{orderDetail.delivery_contact || "No especificado"}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Fecha del Pedido</span>
                <span className="value">{date(orderDetail.created_at)}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">ID de Transacción</span>
                <span className="value" style={{ fontFamily: "monospace", fontSize: 11 }}>
                  {orderDetail.id}
                </span>
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL DETALLE DE TIENDA */}
      {storeDetail && (
        <Modal
          title={`Comercio: ${storeDetail.name}`}
          onClose={() => setStoreDetail(null)}
        >
          <div className="admin-modal-card">
            <div className="admin-financial-box">
              <div>
                <small className="muted">PRODUCTOS ACTIVOS</small>
                <div style={{ fontSize: 20, fontWeight: 700 }}>
                  {storeDetail.products_count} productos
                </div>
              </div>
              <div>
                <small className="muted">PEDIDOS GENERADOS</small>
                <div style={{ fontSize: 20, fontWeight: 700 }}>
                  {storeDetail.orders_count} órdenes
                </div>
              </div>
              <div>
                <small className="muted">ESTADO</small>
                <div><Badge value={storeDetail.status} /></div>
              </div>
            </div>

            <div className="admin-detail-grid">
              <div className="admin-detail-item">
                <span className="label">Titular / Profesional</span>
                <span className="value">
                  {storeDetail.owner_name} ({storeDetail.owner_email})
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Enlace / Slug</span>
                <span className="value">/marketplace?store={storeDetail.slug}</span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Plan de Membresía Asignado</span>
                <span className="value">
                  <span className="badge badge-plan">{storeDetail.plan_code}</span>
                </span>
              </div>
              <div className="admin-detail-item">
                <span className="label">Fecha de Apertura</span>
                <span className="value">{date(storeDetail.created_at)}</span>
              </div>
              {storeDetail.description && (
                <div className="admin-detail-item" style={{ gridColumn: "1 / -1" }}>
                  <span className="label">Descripción de la Tienda</span>
                  <span className="value">{storeDetail.description}</span>
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}

      {user?.permissions.includes("catalog.manage") && (
        <Section
          title="Categorías de servicios"
          action={
            <Action
              title="Guardar categoría"
              fields={[
                { name: "slug", label: "Código único de categoría" },
                { name: "name", label: "Nombre" },
                {
                  name: "parent",
                  label: "Categoría superior",
                  required: false,
                  options: (cats.data ?? []).map((c) => ({
                    value: c.id,
                    label: c.name,
                  })),
                },
                {
                  name: "active",
                  label: "Categoría activa",
                  type: "checkbox",
                  value: true,
                },
              ]}
              action={(v) => api("configure_category", v)}
              after={cats.reload}
            />
          }
        >
          <div className="category-tags">
            {cats.data?.map((c) => (
              <span className="badge" key={c.id}>
                {c.name}
              </span>
            ))}
          </div>
        </Section>
      )}

      {ticket && (
        <Modal title={ticket.subject} onClose={() => setTicket(null)}>
          <p>{ticket.description}</p>
          <Badge value={ticket.status} />
          <div className="support-messages">
            {ticket.messages?.map((m: Data) => (
              <div key={m.id} className="support-message">
                <small>
                  {m.internal_only ? "Nota interna" : "Mensaje compartido"}
                </small>
                <p>{m.body}</p>
              </div>
            ))}
          </div>
          <FieldsForm
            fields={[
              { name: "body", label: "Respuesta al usuario", type: "textarea" },
            ]}
            submit="Enviar respuesta"
            onSubmit={async (v) => {
              await api("reply_ticket", { ticket: ticket.id, ...v });
              await viewTicket(ticket.id);
            }}
          />
          <div className="spaced">
            <Action
              title="Gestionar caso"
              fields={statusFields}
              action={(v) =>
                api("manage_ticket", {
                  ticket: ticket.id,
                  assignee: user?.id,
                  ...v,
                })
              }
              after={() => {
                void viewTicket(ticket.id);
                refresh();
              }}
            />
          </div>
          {ticket.contract_id && (
            <div className="row wrap spaced">
              {user?.permissions.includes("credits.refund") && (
                <Action
                  title="Devolver crédito"
                  fields={[reasonField]}
                  action={(v) =>
                    api("refund_contract_credit", {
                      contract: ticket.contract_id,
                      ticket: ticket.id,
                      ...v,
                    })
                  }
                  after={refresh}
                />
              )}{" "}
              {user?.permissions.includes("money.refund") && (
                <Action
                  title="Resolver devolución de saldo"
                  fields={[reasonField]}
                  action={(v) =>
                    api("resolve_escrow_refund", {
                      contract: ticket.contract_id,
                      ticket: ticket.id,
                      ...v,
                    })
                  }
                  after={refresh}
                />
              )}
            </div>
          )}
        </Modal>
      )}

      {kyc && (
        <Modal title="Expediente de identidad" onClose={() => setKyc(null)}>
          <div className="kyc-dossier-header" style={{ marginBottom: 12 }}>
            <p>
              Estado: <Badge value={kyc.status} />
            </p>
            <p>Presentado: {date(kyc.submitted_at)}</p>
            {kyc.reviewed_at && (
              <p>Revisado: {date(kyc.reviewed_at)}</p>
            )}
            {kyc.expires_at && (
              <p>Válido hasta: {date(kyc.expires_at)}</p>
            )}
            {kyc.reason && (
              <p className="info-note">Motivo: {kyc.reason}</p>
            )}
          </div>
          {(() => {
            let meta: any = null;
            try {
              if (kyc.external_reference?.startsWith("{")) {
                meta = JSON.parse(kyc.external_reference);
              }
            } catch {}
            if (!meta) return null;
            return (
              <div className="panel" style={{ margin: "14px 0", padding: "14px 16px" }}>
                <h3 style={{ fontSize: 14, marginBottom: 8 }}>Datos del Titular</h3>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, fontSize: 13 }}>
                  <div>
                    <span className="muted">Documento:</span>{" "}
                    <strong>{meta.document_type}-{meta.document_number}</strong>
                  </div>
                  <div>
                    <span className="muted">Nombre:</span>{" "}
                    <strong>{meta.full_name}</strong>
                  </div>
                  <div>
                    <span className="muted">Nacimiento:</span>{" "}
                    <strong>{meta.birthdate}</strong>
                  </div>
                  <div>
                    <span className="muted">Teléfono:</span>{" "}
                    <strong>{meta.phone}</strong>
                  </div>
                  {meta.address && (
                    <div style={{ gridColumn: "1 / -1" }}>
                      <span className="muted">Dirección:</span>{" "}
                      <span>{meta.address}</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })()}
          <h3 style={{ fontSize: 14, margin: "12px 0 8px" }}>Documentos Adjuntos ({kyc.documents?.length ?? 0})</h3>
          {kyc.documents?.length ? (
            kyc.documents.map((d: Data) => {
              const docLabels: Record<string, string> = {
                identity_front: "Cédula / Documento (Frente)",
                identity_back: "Cédula (Reverso)",
                selfie: "Selfie con Documento en Mano",
                proof_of_address: "Comprobante de Domicilio / RIF",
                other: "Documento Adicional",
              };
              return (
                <div className="list-row" key={d.file_id}>
                  <div>
                    <strong>{docLabels[d.kind] || d.kind}</strong>
                    <p className="muted small">ID: {d.file_id}</p>
                  </div>
                  <RunButton
                    run={() =>
                      downloadModeration(
                        d.file_id,
                        "Consulta de documento del expediente KYC.",
                      )
                    }
                  >
                    <Download size={16} /> Consultar documento
                  </RunButton>
                </div>
              );
            })
          ) : (
            <p className="muted">El usuario aún no adjuntó documentos.</p>
          )}
        </Modal>
      )}
    </>
  );
}

// COMPONENTE: CALCULADORA Y SIMULADOR DE COMISIONES DEL MODELO DE NEGOCIO
function CommissionSimulator() {
  const [calcType, setCalcType] = useState<"store" | "service">("service");
  const [amount, setAmount] = useState<number>(100);
  const [serviceMode, setServiceMode] = useState<"onsite" | "remote">("onsite");
  const [servicePlan, setServicePlan] = useState<"promo_m1" | "promo_m2" | "pro" | "base">("pro");
  const [storePlan, setStorePlan] = useState<"promo_m1" | "promo_m2" | "standard" | "plus" | "base">("standard");

  let feePercent = 0;
  let label = "";

  if (calcType === "service") {
    if (servicePlan === "promo_m1") {
      feePercent = 0;
      label = "Plan Promocional Entrada (Mes 1 · 0%)";
    } else if (servicePlan === "promo_m2") {
      feePercent = 3;
      label = "Plan Promocional Entrada (Meses 2 y 3 · 3%)";
    } else if (servicePlan === "pro") {
      feePercent = serviceMode === "onsite" ? 3 : 6;
      label = `Membresía Profesional ($15,00 USD) · ${feePercent}% (${serviceMode === "onsite" ? "Presencial" : "Remoto"})`;
    } else {
      feePercent = serviceMode === "onsite" ? 8 : 12;
      label = `Modalidad Base Sin Suscripción · ${feePercent}% (${serviceMode === "onsite" ? "Presencial" : "Remoto"})`;
    }
  } else {
    if (storePlan === "promo_m1") {
      feePercent = 0;
      label = "Plan Promocional Entrada (Mes 1 · 0%)";
    } else if (storePlan === "promo_m2") {
      feePercent = 3;
      label = "Plan Promocional Entrada (Meses 2 y 3 · 3%)";
    } else if (storePlan === "standard") {
      feePercent = 4;
      label = "Membresía Comercio Estándar ($30,00 USD) · Tasa Mínima 4%";
    } else if (storePlan === "plus") {
      feePercent = 4;
      label = "Membresía Comercio Plus ($50,00 USD) · Tasa Mínima 4%";
    } else {
      feePercent = 10;
      label = "Modalidad Base Sin Suscripción · 10% por venta";
    }
  }

  const feeAmount = (amount * feePercent) / 100;
  const netAmount = amount - feeAmount;

  return (
    <div className="commission-simulator-card">
      <div className="sim-header">
        <div className="sim-title">
          <Calculator size={18} style={{ color: "#6F3DFF" }} />
          <h3>Simulador Interactivo del Modelo de Negocio</h3>
        </div>
        <p className="muted" style={{ margin: 0, fontSize: 13 }}>
          Calcula en tiempo real las retenciones de la plataforma y el neto a percibir por el afiliado.
        </p>
      </div>

      <div className="sim-controls-grid">
        <div className="sim-form-group">
          <label>Operación a simular</label>
          <div className="pill-group">
            <button
              type="button"
              className={calcType === "service" ? "pill-btn active" : "pill-btn"}
              onClick={() => setCalcType("service")}
            >
              🛠️ Servicio Contratado
            </button>
            <button
              type="button"
              className={calcType === "store" ? "pill-btn active" : "pill-btn"}
              onClick={() => setCalcType("store")}
            >
              🏪 Venta Marketplace (Cualquier Producto)
            </button>
          </div>
        </div>

        <div className="sim-form-group">
          <label>Monto de la Transacción ($ USD)</label>
          <input
            type="number"
            min="1"
            value={amount}
            onChange={(e) => setAmount(Math.max(0, Number(e.target.value)))}
            className="input"
            style={{ fontWeight: 700, fontSize: 16 }}
          />
        </div>

        {calcType === "service" ? (
          <>
            <div className="sim-form-group">
              <label>Modalidad de Ejecución</label>
              <div className="pill-group">
                <button
                  type="button"
                  className={serviceMode === "onsite" ? "pill-btn active" : "pill-btn"}
                  onClick={() => setServiceMode("onsite")}
                >
                  <MapPin size={13} style={{ display: "inline", marginRight: 4 }} /> Presencial
                </button>
                <button
                  type="button"
                  className={serviceMode === "remote" ? "pill-btn active" : "pill-btn"}
                  onClick={() => setServiceMode("remote")}
                >
                  <Laptop size={13} style={{ display: "inline", marginRight: 4 }} /> Remoto
                </button>
              </div>
            </div>

            <div className="sim-form-group">
              <label>Plan del Proveedor</label>
              <select
                className="input"
                value={servicePlan}
                onChange={(e) => setServicePlan(e.target.value as any)}
              >
                <option value="promo_m1">Plan Promocional (Mes 1 · 0%)</option>
                <option value="promo_m2">Plan Promocional (Meses 2 y 3 · 3%)</option>
                <option value="pro">Membresía Profesional ($15 USD · 3% pres / 6% rem)</option>
                <option value="base">Modalidad Base Sin Suscripción ($0 USD · 8% pres / 12% rem)</option>
              </select>
            </div>
          </>
        ) : (
          <div className="sim-form-group" style={{ gridColumn: "span 2" }}>
            <label>Plan del Comercio / Tienda</label>
            <select
              className="input"
              value={storePlan}
              onChange={(e) => setStorePlan(e.target.value as any)}
            >
              <option value="promo_m1">Plan Promocional (Mes 1 · 0%)</option>
              <option value="promo_m2">Plan Promocional (Meses 2 y 3 · 3%)</option>
              <option value="standard">Membresía Comercio Estándar ($30 USD · Tasa Mínima 4%)</option>
              <option value="plus">Membresía Comercio Plus ($50 USD · Tasa Mínima 4%)</option>
              <option value="base">Modalidad Base Sin Suscripción ($0 USD · 10% por venta)</option>
            </select>
          </div>
        )}
      </div>

      <div className="sim-result-box">
        <div className="sim-result-col">
          <small>Tasa de Comisión</small>
          <strong>{feePercent}%</strong>
          <span className="caption">{label}</span>
        </div>
        <div className="sim-result-col accent-result">
          <small>Ingreso Plataforma (Vinko)</small>
          <strong>${feeAmount.toFixed(2)} USD</strong>
          <span className="caption">Comisión retenida por intermediación</span>
        </div>
        <div className="sim-result-col success-result">
          <small>Neto para el Afiliado</small>
          <strong>${netAmount.toFixed(2)} USD</strong>
          <span className="caption">Saldo neto liquidable o cobrado</span>
        </div>
      </div>
    </div>
  );
}

async function downloadModeration(id: string, reason: string) {
  const result = await request(`/api/v1/files/moderation/${id}`, "POST", {
    reason,
  });
  const bytes = Uint8Array.from(atob(result.content), (c: string) =>
    c.charCodeAt(0),
  );
  const url = URL.createObjectURL(new Blob([bytes], { type: result.mime }));
  const a = document.createElement("a");
  a.href = url;
  a.download = `documento${result.mime === "application/pdf" ? ".pdf" : ".jpg"}`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
