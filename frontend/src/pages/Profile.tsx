import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  UserRound,
  Handshake,
  BriefcaseBusiness,
  MapPin,
  ShieldCheck,
  Paperclip,
  Plus,
  Download,
  KeyRound,
  ArrowRight,
  CheckCircle2,
  Clock,
  AlertTriangle,
  FileText,
  RefreshCw,
  Shield,
  Award,
  Sparkles,
  ExternalLink,
  Copy,
  Check,
  Star,
  Globe,
  Building2,
  Phone,
  MessageSquare,
  Zap,
  Coins,
  Inbox,
  FileCheck,
  Activity,
  Tag,
  TrendingUp,
} from "lucide-react";
import { RequestImages } from "../RequestPhotos";
import { PersonalKycWizard } from "../PersonalKycWizard";
import { api, request, downloadFile, money, date, type Data } from "../api";
import { useAuth, useToast } from "../context";
import { professionalSlug } from "../professionalSlug";
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
  currencyField,
  Modal,
  ErrorBox,
  type FieldSpec,
} from "../components";
const contactFields: FieldSpec[] = [
  {
    name: "kind",
    label: "Tipo",
    options: [
      { value: "phone", label: "Teléfono" },
      { value: "whatsapp", label: "WhatsApp" },
      { value: "email", label: "Correo electrónico" },
    ],
  },
  { name: "label", label: "Nombre del contacto" },
  { name: "value", label: "Número o correo" },
];
import { AddAddressButton } from "../AddressWizard";
function AccountTab() {
  const { reload } = useAuth();
  const data = useData(() => api("my_account"), []),
    prefs = useData(() => api("payment_preferences"), []);
  const toast = useToast();
  return (
    <div className="two-col">
      <div>
        {data.data?.account_type === "juridico" ? (
          <Section
            title="Información de la empresa"
            action={
              data.data?.rif && (
                <span className="badge badge-green">
                  RIF: {data.data.rif}
                </span>
              )
            }
          >
            {data.data && (
              <FieldsForm
                fields={[
                  {
                    name: "business_name",
                    label: "Razón Social",
                    value: data.data.business_name || data.data.name,
                    minLength: 2,
                    maxLength: 160,
                    hint: "Nombre legal de la empresa en Venezuela",
                  },
                  {
                    name: "rif",
                    label: "RIF de la empresa",
                    value: data.data.rif,
                    minLength: 6,
                    maxLength: 20,
                    hint: "Ejemplo: J-12345678-9",
                  },
                  {
                    name: "contact_name",
                    label: "Persona de contacto / Representante",
                    value: data.data.contact_name,
                    minLength: 2,
                    maxLength: 120,
                  },
                ]}
                submit="Guardar datos de empresa"
                onSubmit={async (v) => {
                  await api("update_account", {
                    name: v.business_name,
                    business_name: v.business_name,
                    rif: v.rif?.toUpperCase(),
                    contact_name: v.contact_name,
                  });
                  data.reload();
                  await reload();
                  toast("Datos de la empresa actualizados.");
                }}
              />
            )}
            <p className="muted">
              Correo corporativo: <strong>{data.data?.email}</strong>
            </p>
          </Section>
        ) : (
          <Section title="Información personal">
            {data.data && (
              <FieldsForm
                fields={[
                  {
                    name: "name",
                    label: "Nombre completo",
                    value: data.data.name,
                  },
                ]}
                submit="Guardar cambios"
                onSubmit={async (v) => {
                  await api("update_account", v);
                  data.reload();
                  await reload();
                  toast("Perfil actualizado.");
                }}
              />
            )}
            <p className="muted">{data.data?.email}</p>
          </Section>
        )}
        <Section
          title="Mis contactos"
          action={
            <Action
              title="Añadir contacto"
              fields={[
                ...contactFields,
                {
                  name: "share",
                  label: "Compartir después de contratar",
                  type: "checkbox",
                  value: true,
                },
              ]}
              action={(v) => api("save_contact", v)}
              after={data.reload}
            />
          }
        >
          {data.data?.contacts?.map((c: Data) => (
            <div className="list-row" key={c.id}>
              <div>
                <strong>{c.label}</strong>
                <p>{c.value}</p>
                <small>
                  {c.share_on_contract
                    ? "Compartido solo en contratos"
                    : "Privado"}
                </small>
              </div>
              <RunButton
                run={() => api("remove_contact", { contact: c.id })}
                after={data.reload}
              >
                Eliminar
              </RunButton>
            </div>
          ))}
          {!data.data?.contacts?.length && (
            <p className="muted">
              Añade teléfonos, correos o WhatsApp para tus servicios.
            </p>
          )}
        </Section>
        <Section
          title="Direcciones de servicio"
          action={<AddAddressButton after={data.reload} />}
        >
          {data.data?.addresses?.map((a: Data) => (
            <div className="list-row" key={a.id}>
              <div>
                <h3>
                  <MapPin size={16} /> {a.label}
                </h3>
                <p>
                  {a.address_line} ·{" "}
                  {[a.parish_name, a.municipality_name ?? a.city, a.state_name]
                    .filter(Boolean)
                    .join(", ")}
                </p>
                <small>Ubicación privada</small>
              </div>
              <RunButton
                run={() => api("archive_address", { address: a.id })}
                after={data.reload}
              >
                Archivar
              </RunButton>
            </div>
          ))}
        </Section>
      </div>
      <aside>
        <Section title="Preferencias de pago">
          <ShieldCheck size={28} className="green" />
          <p>
            Al exigir pago protegido, los servicios que contrates o prestes
            deben financiarse en SoyVinko.
          </p>
          {prefs.data && (
            <FieldsForm
              key={JSON.stringify(prefs.data)}
              fields={[
                {
                  name: "required",
                  label: "Quiero trabajar con pago protegido",
                  type: "checkbox",
                  value: prefs.data.requires_platform_payment,
                },
              ]}
              submit="Guardar preferencia"
              onSubmit={async (v) => {
                await api("set_payment_preference", v);
                prefs.reload();
                toast("Preferencia guardada.");
              }}
            />
          )}
          <p className="info-note">
            En remoto el pago siempre es protegido. El dinero se libera cuando
            el cliente valida la actividad.
          </p>
        </Section>
      </aside>
    </div>
  );
}
function ProfessionalTab() {
  const { user, reload } = useAuth(),
    navigate = useNavigate(),
    toast = useToast();
  const [copiedSlug, setCopiedSlug] = useState(false);
  const [proSubTab, setProSubTab] = useState<
    "services" | "incoming" | "contracts" | "identity" | "coverage" | "portfolio" | "contacts" | "activity"
  >("services");

  const data = useData(() => api("my_professional"), []),
    cats = useData(() => api<Data[]>("catalog"), []),
    assets = useData(() => api("professional_assets"), []),
    files = useData(() => api<Data[]>("my_files"), []),
    contracts = useData(() => api<Data[]>("my_contracts"), [], 10000),
    incomingOnsite = useData(
      () => api<Data[]>("service_feed", { delivery_mode: "onsite" }),
      [],
      10000
    ),
    incomingRemote = useData(
      () => api<Data[]>("service_feed", { delivery_mode: "remote" }),
      [],
      10000
    ),
    proposals = useData(() => api<Data[]>("my_proposals"), [], 10000),
    credits = useData(() => api<Data>("my_wallet"), []);

  const categories = (cats.data ?? []).map((c) => ({
      value: c.id,
      label: c.name,
    })),
    p = data.data;
  const branches = (p?.branches ?? []).map((b: Data) => ({
    value: b.id,
    label: b.name,
  }));
  const refresh = () => {
    data.reload();
    assets.reload();
    contracts.reload();
    incomingOnsite.reload();
    incomingRemote.reload();
    proposals.reload();
    credits.reload();
  };

  const handleAcceptDirect = async (requestId: string) => {
    try {
      const contractId = await api("accept_direct_request", { request: requestId });
      toast("¡Contrato Digital Formalizado! Redirigiendo al contrato...");
      refresh();
      navigate(`/contracts/${contractId}`);
    } catch (e: any) {
      toast(e?.message || "Error al formalizar el contrato digital.", true);
    }
  };

  const copySlug = () => {
    if (!p?.slug) return;
    const url = `https://soyvinko.com/p/${p.slug}`;
    navigator.clipboard.writeText(url);
    setCopiedSlug(true);
    toast("Enlace público copiado al portapapeles.");
    setTimeout(() => setCopiedSlug(false), 2500);
  };

  const toggleAvailability = async () => {
    if (!p) return;
    try {
      await api("update_professional", {
        name: p.business_name,
        bio: p.bio,
        years: p.years_experience ?? 0,
        accepts: !p.accepts_requests,
      });
      refresh();
      toast(
        !p.accepts_requests
          ? "¡Disponibilidad activada! Ya puedes recibir nuevas solicitudes."
          : "Disponibilidad pausada temporalmente."
      );
    } catch (e: any) {
      toast(e?.message || "Error al actualizar disponibilidad.", true);
    }
  };

  if (data.loading) return <Loading />;

  if (!p)
    return (
      <div className="two-col">
        <Section title="Haz de tu talento un negocio independiente">
          <p>
            Crea tu perfil profesional y publica tus servicios de forma 100%
            autónoma. No dependes de aprobaciones ni de tiempos de espera para
            empezar a trabajar. Recibe 3 Vinkos de bienvenida al activarte.
          </p>
          <FieldsForm
            fields={[
              {
                name: "business_name",
                label: "Nombre comercial o profesional",
                value: user?.name,
                hint: "El nombre con el que tus clientes te identificarán.",
              },
              {
                name: "slug",
                label: "Enlace único para tu perfil público",
                hint: "Ejemplo: servicios-caracas → soyvinko.com/p/servicios-caracas.",
                normalize: professionalSlug,
                maxLength: 80,
                autoComplete: "off",
              },
              {
                name: "bio",
                label: "Descripción de tus servicios y experiencia",
                type: "textarea",
                hint: "Describe tu especialidad, experiencia y lo que ofreces.",
              },
            ]}
            submit="Activar perfil y comenzar a publicar"
            onSubmit={async (v) => {
              await api("activate_professional", v);
              await reload();
              data.reload();
              toast("¡Perfil profesional activado con éxito!");
            }}
          />
        </Section>
        <Section title="Total Independencia y Libertad">
          <div className="vinko-quote-box">
            <div className="vinko-quote-item">
              <strong>🚀 Publicación Inmediata</strong>
              <span>Tus servicios van directo al catálogo</span>
            </div>
            <div className="vinko-quote-item">
              <strong>🤝 Contratación Directa</strong>
              <span>Validación de contratos entre cliente y tú</span>
            </div>
            <div className="vinko-quote-item">
              <strong>💳 Modalidades de Pago Claras</strong>
              <span>En custodia digital o directo en persona</span>
            </div>
            <div className="vinko-quote-item">
              <strong>📞 Canales Mutuos Desbloqueados</strong>
              <span>WhatsApp y llamadas directas en contratos</span>
            </div>
          </div>
          <p className="info-note" style={{ marginTop: 16 }}>
            Se consume un crédito cuando una contratación queda aceptada por ambas
            partes y se formaliza el contrato digital.
          </p>
        </Section>
      </div>
    );

  const activeOfferingsCount = (p.offerings ?? []).filter((o: any) => o.active).length;

  const allIncoming: any[] = [
    ...(incomingOnsite.data ?? []).map((x: any) => ({ ...x, delivery_mode: "onsite" })),
    ...(incomingRemote.data ?? []).map((x: any) => ({ ...x, delivery_mode: "remote" })),
  ];
  const incomingRequests: any[] = Array.from(
    new Map(
      allIncoming.map((item: any) => [item.request_id || item.id, item])
    ).values()
  );
  const proContracts = (contracts.data ?? []).filter(
    (c: any) => c.is_professional || !c.is_customer
  );
  const activeContracts = proContracts.filter((c: any) =>
    ["formalized", "in_progress", "awaiting_confirmation"].includes(c.status)
  );

  return (
    <div className="merchant-hub">
      {/* Header Principal de Control del Comerciante */}
      <div className="merchant-header-card">
        <div className="merchant-header-top">
          <div className="merchant-identity">
            <div className="merchant-avatar-monogram">
              {(p.business_name || user?.name || "V")
                .trim()
                .slice(0, 2)
                .toUpperCase()}
            </div>
            <div className="merchant-identity-text">
              <h2>
                {p.business_name}
                <span className="merchant-verified-badge">
                  <ShieldCheck size={14} /> Comerciante Autónomo
                </span>
              </h2>
              <div className="merchant-slug-bar">
                <span>soyvinko.com/p/{p.slug}</span>
                <button
                  type="button"
                  className="merchant-copy-btn"
                  onClick={copySlug}
                  title="Copiar enlace público"
                >
                  {copiedSlug ? <Check size={13} /> : <Copy size={13} />}
                  {copiedSlug ? "Copiado" : "Copiar enlace"}
                </button>
                <Link
                  to={`/professionals/${p.user_id}`}
                  className="merchant-copy-btn"
                  title="Ver perfil como cliente"
                >
                  <ExternalLink size={13} /> Ver público
                </Link>
              </div>
            </div>
          </div>

          <button
            type="button"
            className={`merchant-availability-pill ${p.accepts_requests ? "active" : "paused"}`}
            onClick={toggleAvailability}
            title="Haz clic para alternar tu disponibilidad"
          >
            <span
              className={`status-dot-pulse ${p.accepts_requests ? "green" : "amber"}`}
            />
            {p.accepts_requests
              ? "🟢 Disponible para Nuevas Contrataciones"
              : "⏸️ Disponibilidad en Pausa"}
          </button>
        </div>

        <div className="merchant-autonomy-banner">
          <Zap size={20} className="gold" />
          <div>
            <strong>Perfil 100% Autónomo e Independiente:</strong> Administras
            tus propios servicios, tarifas y acuerdos. Tus servicios se publican
            al instante y los contratos se validan directamente entre el cliente
            y tú, sin necesidad de autorizaciones o demoras administrativas.
          </div>
        </div>
      </div>

      {/* Grid de Métricas KPI Interactivas */}
      <div className="merchant-kpis">
        <div
          className="merchant-kpi-card"
          onClick={() => setProSubTab("services")}
          style={{ cursor: "pointer" }}
          title="Ver mis servicios publicados"
        >
          <div className="merchant-kpi-icon purple">
            <BriefcaseBusiness size={24} />
          </div>
          <div>
            <div className="merchant-kpi-value">{p.offerings?.length ?? 0}</div>
            <div className="merchant-kpi-label">
              Servicios ({activeOfferingsCount} activos)
            </div>
          </div>
        </div>

        <div
          className="merchant-kpi-card"
          onClick={() => setProSubTab("incoming")}
          style={{ cursor: "pointer" }}
          title="Ver solicitudes recibidas de clientes"
        >
          <div className="merchant-kpi-icon emerald">
            <Inbox size={24} />
          </div>
          <div>
            <div className="merchant-kpi-value">{incomingRequests.length}</div>
            <div className="merchant-kpi-label">Solicitudes recibidas</div>
          </div>
        </div>

        <div
          className="merchant-kpi-card"
          onClick={() => setProSubTab("contracts")}
          style={{ cursor: "pointer" }}
          title="Ver contratos digitales"
        >
          <div className="merchant-kpi-icon gold">
            <FileCheck size={24} />
          </div>
          <div>
            <div className="merchant-kpi-value">{proContracts.length}</div>
            <div className="merchant-kpi-label">
              Contratos ({activeContracts.length} en curso)
            </div>
          </div>
        </div>

        <div
          className="merchant-kpi-card"
          onClick={() => setProSubTab("contacts")}
          style={{ cursor: "pointer" }}
          title="Ver canales de contacto para contratos"
        >
          <div className="merchant-kpi-icon blue">
            <Phone size={24} />
          </div>
          <div>
            <div className="merchant-kpi-value">{p.contacts?.length ?? 0}</div>
            <div className="merchant-kpi-label">Canales para contratos</div>
          </div>
        </div>

        <div
          className="merchant-kpi-card"
          onClick={() => setProSubTab("activity")}
          style={{ cursor: "pointer" }}
          title="Ver actividad profesional publicada"
        >
          <div className="merchant-kpi-icon" style={{ background: "linear-gradient(135deg, #f59e0b22, #f59e0b44)", color: "#f59e0b" }}>
            <Activity size={24} />
          </div>
          <div>
            <div className="merchant-kpi-value">{activeOfferingsCount}</div>
            <div className="merchant-kpi-label">Servicios activos publicados</div>
          </div>
        </div>
      </div>

      {/* Barra de Sub-pestañas de Gestión */}
      <div className="merchant-subtabs-nav">
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "services" ? "active" : ""}`}
          onClick={() => setProSubTab("services")}
        >
          <BriefcaseBusiness size={16} /> Mis Servicios ({p.offerings?.length ?? 0})
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "incoming" ? "active" : ""}`}
          onClick={() => setProSubTab("incoming")}
        >
          <Inbox size={16} /> Solicitudes Recibidas
          {incomingRequests.length > 0 && (
            <span className="merchant-subtab-badge">{incomingRequests.length}</span>
          )}
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "contracts" ? "active" : ""}`}
          onClick={() => setProSubTab("contracts")}
        >
          <FileCheck size={16} /> Contratos Digitales
          {proContracts.length > 0 && (
            <span className="merchant-subtab-badge" style={{ background: "#6F3DFF" }}>
              {proContracts.length}
            </span>
          )}
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "identity" ? "active" : ""}`}
          onClick={() => setProSubTab("identity")}
        >
          <Sparkles size={16} /> Datos del Negocio
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "contacts" ? "active" : ""}`}
          onClick={() => setProSubTab("contacts")}
        >
          <Phone size={16} /> Canales para Contratos ({p.contacts?.length ?? 0})
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "coverage" ? "active" : ""}`}
          onClick={() => setProSubTab("coverage")}
        >
          <MapPin size={16} /> Sedes y Cobertura ({p.branches?.length ?? 0})
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "portfolio" ? "active" : ""}`}
          onClick={() => setProSubTab("portfolio")}
        >
          <Award size={16} /> Portafolio y Credenciales
        </button>
        <button
          type="button"
          className={`merchant-subtab-btn ${proSubTab === "activity" ? "active" : ""}`}
          onClick={() => setProSubTab("activity")}
          style={proSubTab === "activity" ? {} : { borderColor: "#f59e0b44", color: "#f59e0b" }}
        >
          <Activity size={16} /> Actividad Profesional
          {activeOfferingsCount > 0 && (
            <span className="merchant-subtab-badge" style={{ background: "#f59e0b", color: "#000" }}>
              {activeOfferingsCount}
            </span>
          )}
        </button>
      </div>

      {/* Vista de Sub-pestaña: Servicios */}
      {proSubTab === "services" && (
        <Section
          title="Catálogo de Servicios y Ofertas Publicadas"
          action={
            <Action
              title="+ Publicar nuevo servicio"
              fields={[
                { name: "category", label: "Categoría", options: categories },
                {
                  name: "title",
                  label: "Título del servicio",
                  hint: "Ejemplo: Instalación y Reparación de Aires Acondicionados",
                },
                {
                  name: "description",
                  label: "Descripción detallada del servicio",
                  type: "textarea",
                  hint: "Explica qué incluye el servicio, herramientas, garantías y alcances.",
                },
                currencyField,
                {
                  name: "amount",
                  label: "Precio base desde",
                  type: "number",
                  min: 0,
                  required: false,
                  hint: "Opcional. Deja vacío si el precio se acuerda a cotizar por solicitud.",
                },
                {
                  name: "mode",
                  label: "Modalidad de prestación",
                  options: [
                    { value: "onsite", label: "Presencial (en la sede o ubicación del cliente)" },
                    { value: "remote", label: "Remoto (a distancia / online)" },
                  ],
                  value: "onsite",
                },
              ]}
              action={async (v) => {
                const id = await api("add_offering", {
                  category: v.category,
                  title: v.title,
                  description: v.description,
                  currency: v.currency,
                  price_minor:
                    v.amount == null ? null : Math.round(v.amount * 100),
                });
                await api("set_offering_delivery", {
                  offering: id,
                  mode: v.mode,
                });
                toast("¡Servicio publicado y disponible de inmediato en búsquedas!");
              }}
              after={refresh}
            />
          }
        >
          <div className="merchant-autonomy-banner" style={{ marginTop: 0, marginBottom: 16 }}>
            <Zap size={18} className="gold" />
            <div>
              <strong>Publicación Directa e Instantánea:</strong> Todos los servicios que agregas se publican de forma inmediata en el catálogo público sin necesidad de aprobación previa.
            </div>
          </div>

          {p.offerings?.length ? (
            <div className="merchant-services-grid">
              {p.offerings.map((o: Data) => (
                <div className="service-card-modern" key={o.id}>
                  <div>
                    <div className="service-card-header">
                      <div>
                        <h3 className="service-card-title">{o.title}</h3>
                        <span className="service-card-category">
                          {categories.find((c) => c.value === o.category_id)?.label ||
                            "Servicio profesional"}
                        </span>
                      </div>
                      <div className="service-card-badges">
                        {o.delivery_mode === "remote" ? (
                          <span className="badge-mode-remote">
                            <Globe size={13} /> Remoto
                          </span>
                        ) : (
                          <span className="badge-mode-onsite">
                            <MapPin size={13} /> Presencial
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="service-card-price">
                      {o.price_min_minor != null
                        ? money(o.price_min_minor, o.currency)
                        : "Precio a convenir"}
                    </div>

                    <p className="service-card-desc">{o.description}</p>
                  </div>

                  <div className="service-card-footer">
                    <span
                      className={`service-status-pill ${o.active ? "active" : "inactive"}`}
                    >
                      <span
                        className={`status-dot-pulse ${o.active ? "green" : "amber"}`}
                        style={{ width: 8, height: 8 }}
                      />
                      {o.active ? "Activo" : "Pausado"}
                    </span>

                    <div className="row" style={{ gap: 8 }}>
                      <RunButton
                        className="link-button small"
                        run={async () => {
                          await api("update_offering", {
                            offering: o.id,
                            title: o.title,
                            description: o.description,
                            price: o.price_min_minor,
                            active: !o.active,
                          });
                          toast(
                            !o.active
                              ? "Servicio activado en el catálogo."
                              : "Servicio pausado."
                          );
                        }}
                        after={refresh}
                      >
                        {o.active ? "Pausar" : "Activar"}
                      </RunButton>

                      <Action
                        title="Editar servicio"
                        fields={[
                          { name: "title", label: "Título", value: o.title },
                          {
                            name: "description",
                            label: "Descripción",
                            type: "textarea",
                            value: o.description,
                          },
                          {
                            name: "amount",
                            label: "Precio desde",
                            type: "number",
                            min: 0,
                            value: o.price_min_minor ? o.price_min_minor / 100 : "",
                          },
                          {
                            name: "active",
                            label: "Servicio activo en búsquedas",
                            type: "checkbox",
                            value: o.active,
                          },
                          {
                            name: "mode",
                            label: "Modalidad",
                            value: o.delivery_mode,
                            options: [
                              { value: "onsite", label: "Presencial" },
                              { value: "remote", label: "Remoto" },
                            ],
                          },
                        ]}
                        action={async (v) => {
                          await api("update_offering", {
                            offering: o.id,
                            title: v.title,
                            description: v.description,
                            price: v.amount ? Math.round(v.amount * 100) : null,
                            active: v.active,
                          });
                          await api("set_offering_delivery", {
                            offering: o.id,
                            mode: v.mode,
                          });
                        }}
                        after={refresh}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Empty
              title="¿Qué servicios deseas ofrecer?"
              text="Añade tu primer servicio profesional para aparecer de inmediato en las búsquedas y recibir propuestas directas de clientes."
            />
          )}
        </Section>
      )}

      {/* Vista de Sub-pestaña: Solicitudes Recibidas */}
      {proSubTab === "incoming" && (
        <Section
          title="Solicitudes Recibidas de Clientes"
          action={
            <button
              type="button"
              className="link-button small"
              onClick={refresh}
              title="Actualizar solicitudes"
            >
              <RefreshCw size={14} /> Actualizar
            </button>
          }
        >
          <div className="merchant-autonomy-banner" style={{ marginTop: 0, marginBottom: 16 }}>
            <Inbox size={18} className="gold" />
            <div>
              <strong>Validación Directa de Contratos:</strong> Cuando recibes una solicitud directa de un cliente, puedes validarla con un clic. Se formalizará de inmediato el <strong>Contrato Digital</strong> y se desbloquearán mutuamente los canales de WhatsApp y teléfono.
            </div>
          </div>

          {incomingRequests.length ? (
            <div className="merchant-requests-grid">
              {incomingRequests.map((r: any) => {
                const reqId = r.request_id || r.id;
                const hasProposal = (proposals.data ?? []).some(
                  (prop: any) => prop.request_id === reqId
                );

                return (
                  <div className="pro-request-card" key={reqId}>
                    <div className="pro-request-header">
                      <div className="row wrap" style={{ gap: 8, alignItems: "center" }}>
                        {r.mode === "direct" ? (
                          <span className="badge-direct-hire">
                            ⚡ Contratación directa para ti
                          </span>
                        ) : (
                          <span className="badge-broadcast-hire">
                            📢 Solicitud en tu categoría
                          </span>
                        )}
                        {r.delivery_mode === "remote" ? (
                          <span className="badge-mode-remote">
                            <Globe size={13} /> Remoto
                          </span>
                        ) : (
                          <span className="badge-mode-onsite">
                            <MapPin size={13} /> Presencial
                          </span>
                        )}
                      </div>
                      <span className="pro-request-expiry">
                        Expira: {date(r.expires_at)}
                      </span>
                    </div>

                    <h3 className="pro-request-title">{r.title}</h3>
                    <p className="pro-request-desc">{r.description}</p>

                    {r.attachments?.length > 0 && (
                      <div style={{ margin: "10px 0" }}>
                        <RequestImages photos={r.attachments} />
                      </div>
                    )}

                    <div className="pro-request-meta-row">
                      <div className="pro-request-price">
                        <small>Presupuesto del cliente</small>
                        <strong>
                          {r.budget_max_minor != null || r.offered_price_minor != null
                            ? money(r.budget_max_minor ?? r.offered_price_minor, r.currency)
                            : `A cotizar en ${r.currency === "VES" ? "Bs" : "USD"}`}
                        </strong>
                      </div>
                      <div className="pro-request-payment-pill">
                        {r.requires_platform_payment
                          ? "🔒 Custodia digital (Plataforma)"
                          : "🤝 Pago directo / En persona"}
                      </div>
                    </div>

                    <div className="pro-request-actions">
                      {hasProposal ? (
                        <p className="info-note" style={{ margin: 0, width: "100%" }}>
                          ✅ Ya enviaste una propuesta para esta solicitud.
                        </p>
                      ) : r.mode === "direct" ? (
                        <>
                          <RunButton
                            className="button primary"
                            confirm="¿Validar esta contratación y formalizar el Contrato Digital? Se creará el contrato digital oficial, se desbloquearán WhatsApp y llamadas directas con el cliente, y comenzará la ejecución del trabajo."
                            run={() => handleAcceptDirect(reqId)}
                          >
                            <Handshake size={16} /> Validar y Formalizar Contrato Digital
                          </RunButton>
                          <Action
                            title="Cotizar precio diferente"
                            fields={[
                              {
                                name: "message",
                                label: "Mensaje explicativo para el cliente",
                                type: "textarea",
                                hint: "Indica tu propuesta de valor, materiales o ajustes al alcance.",
                              },
                              {
                                name: "amount",
                                label: `Precio cotizado en ${r.currency === "VES" ? "Bs" : "USD"}`,
                                type: "number",
                                min: 0.01,
                              },
                            ]}
                            action={async (v) => {
                              await api("submit_proposal", {
                                request: reqId,
                                message: v.message,
                                price_minor: Math.round(v.amount * 100),
                                expires_at: new Date(
                                  Math.min(Date.now() + 7 * 86400000, Date.parse(r.expires_at))
                                ).toISOString(),
                              });
                              toast("Cotización enviada al cliente.");
                            }}
                            after={refresh}
                          />
                        </>
                      ) : (
                        <Action
                          title="Enviar propuesta de servicio"
                          fields={[
                            {
                              name: "message",
                              label: "Tu propuesta y experiencia para este trabajo",
                              type: "textarea",
                              hint: "Explica cómo resolverás la necesidad y detalles de tu trabajo.",
                            },
                            {
                              name: "amount",
                              label: `Precio cotizado en ${r.currency === "VES" ? "Bs" : "USD"}`,
                              type: "number",
                              min: 0.01,
                            },
                          ]}
                          action={async (v) => {
                            await api("submit_proposal", {
                              request: reqId,
                              message: v.message,
                              price_minor: Math.round(v.amount * 100),
                              expires_at: new Date(
                                Math.min(Date.now() + 7 * 86400000, Date.parse(r.expires_at))
                              ).toISOString(),
                            });
                            toast("Propuesta enviada al cliente.");
                          }}
                          after={refresh}
                        />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <Empty
              title="No hay solicitudes pendientes en este momento"
              text="Cuando un cliente te envíe una invitación de contratación o publique una necesidad en tus áreas de cobertura, aparecerá aquí al instante para tu validación."
            />
          )}
        </Section>
      )}

      {/* Vista de Sub-pestaña: Contratos Digitales */}
      {proSubTab === "contracts" && (
        <Section
          title="Contratos Digitales de Servicios"
          action={
            <button
              type="button"
              className="link-button small"
              onClick={refresh}
              title="Actualizar contratos"
            >
              <RefreshCw size={14} /> Actualizar
            </button>
          }
        >
          <div className="merchant-autonomy-banner" style={{ marginTop: 0, marginBottom: 16 }}>
            <FileCheck size={18} className="gold" />
            <div>
              <strong>Control Digital de Acuerdos y Pagos:</strong> Cada contrato formalizado cuenta con folio oficial, canales de contacto directo (WhatsApp y llamadas), registro de avance y mecanismo de liberación de fondos o confirmación de pago.
            </div>
          </div>

          {proContracts.length ? (
            <div className="merchant-contracts-grid">
              {proContracts.map((c: any) => (
                <div className="pro-contract-card" key={c.id}>
                  <div className="pro-contract-header">
                    <div>
                      <span className="pro-contract-folio">
                        #VNK-{(c.folio || c.id).slice(0, 8).toUpperCase()}
                      </span>
                      <h3 className="pro-contract-title">
                        {c.title || c.offering_title || c.work_requirements || "Servicio Contratado"}
                      </h3>
                    </div>
                    <div className="row wrap" style={{ gap: 6, alignItems: "center" }}>
                      <Badge value={c.status} />
                      {c.delivery_mode === "remote" ? (
                        <span className="badge-mode-remote">
                          <Globe size={13} /> Remoto
                        </span>
                      ) : (
                        <span className="badge-mode-onsite">
                          <MapPin size={13} /> Presencial
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="pro-contract-body">
                    <div className="pro-contract-party">
                      <UserRound size={16} color="#8B5CF6" />
                      <span>
                        Cliente: <strong>{c.customer_name || c.customer_email || "Cliente SoyVinko"}</strong>
                      </span>
                    </div>

                    <div className="pro-contract-payment-row">
                      <span
                        className={`pro-contract-badge ${
                          c.requires_platform_payment ? "custody" : "in-person"
                        }`}
                      >
                        {c.requires_platform_payment
                          ? "🔒 Custodia digital (Plataforma)"
                          : "🤝 Cancelación directa en persona"}
                      </span>
                      <strong className="pro-contract-amount">
                        {money(c.total_minor ?? c.agreed_price_minor, c.currency)}
                      </strong>
                    </div>
                  </div>

                  <div className="pro-contract-footer">
                    <span className="pro-contract-date">
                      Formalizado: {date(c.created_at || c.accepted_at)}
                    </span>
                    <Link
                      to={`/contracts/${c.id}`}
                      className="button primary small"
                      style={{ textDecoration: "none" }}
                    >
                      Abrir Contrato Digital <ArrowRight size={15} />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Empty
              title="Aún no tienes contratos formalizados"
              text="Cuando valides una solicitud directa o el cliente acepte tu cotización, se creará el contrato digital y podrás gestionarlo desde aquí."
            />
          )}
        </Section>
      )}

      {/* Vista de Sub-pestaña: Datos del Negocio */}
      {proSubTab === "identity" && (
        <Section title="Presentación y Datos del Negocio">
          <FieldsForm
            key={p.user_id}
            fields={[
              {
                name: "name",
                label: "Nombre comercial o profesional",
                value: p.business_name,
              },
              {
                name: "bio",
                label: "Acerca de ti y tu experiencia de trabajo",
                type: "textarea",
                value: p.bio,
              },
              {
                name: "years",
                label: "Años de experiencia en el oficio",
                type: "number",
                min: 0,
                max: 90,
                value: p.years_experience ?? 0,
              },
            ]}
            submit="Guardar presentación"
            onSubmit={async (v) => {
              await api("update_professional", {
                ...v,
                accepts: p.accepts_requests,
              });
              refresh();
              toast("Presentación profesional actualizada.");
            }}
          />
        </Section>
      )}

      {/* Vista de Sub-pestaña: Canales de Contacto */}
      {proSubTab === "contacts" && (
        <Section
          title="Canales de Contacto Directo para Contratos"
          action={
            <Action
              title="Añadir contacto"
              fields={[
                ...contactFields,
                {
                  name: "branch",
                  label: "Sede asociada (opcional)",
                  required: false,
                  options: branches,
                },
              ]}
              action={(v) => api("add_professional_contact", v)}
              after={refresh}
            />
          }
        >
          <div className="unlocked-contacts-card" style={{ margin: "0 0 16px 0", padding: "14px 18px" }}>
            <div className="unlocked-contacts-head">
              <Phone size={18} color="#059669" />
              <h3 style={{ fontSize: 14 }}>Desbloqueo Seguro en Contratos</h3>
            </div>
            <p className="muted small" style={{ margin: 0 }}>
              Tus números de WhatsApp, teléfono y correo se comparten
              automáticamente de manera mutua con el cliente cuando se formaliza un
              contrato digital para coordinar la ejecución del servicio.
            </p>
          </div>

          {p.contacts?.map((c: Data) => (
            <div className="list-row" key={c.id}>
              <div>
                <strong>{c.label}</strong>
                <p>{c.value}</p>
                <small className="muted">Canal oficial para clientes contratantes</small>
              </div>
            </div>
          ))}
          {!p.contacts?.length && (
            <p className="muted">
              Añade al menos un número de WhatsApp o teléfono para que tus clientes
              puedan contactarte al iniciar un contrato.
            </p>
          )}
        </Section>
      )}

      {/* Vista de Sub-pestaña: Sedes y Cobertura */}
      {proSubTab === "coverage" && (
        <div className="two-col">
          <Section
            title="Sedes de Atención Presencial"
            action={<AddAddressButton kind="branch" after={refresh} />}
          >
            {p.branches?.map((b: Data) => (
              <div className="branch-card" key={b.id}>
                <h3>
                  <MapPin size={18} /> {b.name}
                </h3>
                <p>
                  {b.address_line} ·{" "}
                  {[b.parish_name, b.municipality_name ?? b.city, b.state_name]
                    .filter(Boolean)
                    .join(", ")}
                </p>
                <Action
                  title="Añadir horario de atención"
                  fields={[
                    {
                      name: "weekday",
                      label: "Día de la semana",
                      options: [
                        "Domingo",
                        "Lunes",
                        "Martes",
                        "Miércoles",
                        "Jueves",
                        "Viernes",
                        "Sábado",
                      ].map((day, i) => ({ value: String(i), label: day })),
                    },
                    { name: "opens", label: "Abre a las", type: "time" },
                    { name: "closes", label: "Cierra a las", type: "time" },
                  ]}
                  action={(v) =>
                    api("set_branch_hours", {
                      branch: b.id,
                      ...v,
                      weekday: Number(v.weekday),
                    })
                  }
                  after={refresh}
                />
                {assets.data?.hours
                  ?.filter((h: Data) => h.branch_id === b.id)
                  .map((h: Data) => (
                    <p key={h.id} className="small muted">
                      {
                        ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"][
                          h.weekday
                        ]
                      }{" "}
                      · {h.opens_at}–{h.closes_at}
                    </p>
                  ))}
              </div>
            ))}
            {!p.branches?.length && (
              <p className="muted">
                Si ofreces servicios presenciales, registra la sede o taller desde
                donde operas.
              </p>
            )}
          </Section>

          <Section
            title="Zonas de Cobertura Geográfica"
            action={
              <Action
                title="Configurar radio de cobertura"
                fields={[
                  { name: "branch", label: "Sede", options: branches },
                  { name: "category", label: "Categoría", options: categories },
                  {
                    name: "radius",
                    label: "Radio de atención en kilómetros",
                    type: "number",
                    min: 0.1,
                    max: 200,
                    step: "0.1",
                    value: 10,
                  },
                ]}
                action={(v) =>
                  api("set_coverage", {
                    branch: v.branch,
                    category: v.category,
                    radius_m: Math.round(v.radius * 1000),
                  })
                }
                after={refresh}
              />
            }
          >
            {p.coverage?.map((c: Data) => (
              <div className="list-row" key={c.id}>
                <div>
                  <h3>
                    {categories.find((x) => x.value === c.category_id)?.label}
                  </h3>
                  <p>
                    Sede: {branches.find((x: Data) => x.value === c.branch_id)?.label}
                  </p>
                </div>
                <strong>{c.radius_m / 1000} km</strong>
              </div>
            ))}
            <p className="muted small">
              Los clientes cercanos a tus sedes verán tus servicios presenciales
              según el radio configurado.
            </p>
          </Section>
        </div>
      )}

      {/* Vista de Sub-pestaña: Portafolio */}
      {proSubTab === "portfolio" && (
        <Section title="Portafolio de Trabajos y Credenciales">
          <div className="row wrap" style={{ gap: 12, marginBottom: 16 }}>
            <Action
              title="+ Añadir trabajo al portafolio"
              fields={[
                { name: "title", label: "Título del proyecto o trabajo" },
                { name: "description", label: "Descripción del resultado", type: "textarea" },
                {
                  name: "file",
                  label: "Imagen del trabajo",
                  options: (files.data ?? [])
                    .filter((f) => f.purpose === "portfolio")
                    .map((f) => ({
                      value: f.id,
                      label: `Imagen · ${date(f.created_at)}`,
                    })),
                },
              ]}
              action={(v) => api("add_portfolio", v)}
              after={refresh}
            />
            <Action
              title="+ Añadir certificación o credencial"
              fields={[
                { name: "title", label: "Título o certificación" },
                { name: "issuer", label: "Institución u organización emisora" },
                {
                  name: "issued",
                  label: "Fecha de expedición",
                  type: "date",
                  required: false,
                },
                {
                  name: "expires",
                  label: "Fecha de vencimiento",
                  type: "date",
                  required: false,
                },
                {
                  name: "file",
                  label: "Documento probatorio",
                  required: false,
                  options: (files.data ?? [])
                    .filter((f) => f.purpose === "evidence")
                    .map((f) => ({
                      value: f.id,
                      label: `Documento · ${date(f.created_at)}`,
                    })),
                },
              ]}
              action={(v) => api("add_credential", v)}
              after={refresh}
            />
          </div>
          <p className="muted small">
            Puedes subir fotografías de tus trabajos concluidos desde la pestaña Archivos con finalidad «Portafolio».
          </p>
          {assets.data?.portfolio?.map((item: Data) => (
            <div key={item.id} className="list-row">
              <div>
                <strong>{item.title}</strong>
                <p>{item.description}</p>
              </div>
            </div>
          ))}
          {assets.data?.credentials?.map((cred: Data) => (
            <div key={cred.id} className="list-row">
              <div>
                <strong>{cred.title}</strong>
                <p>{cred.issuer}</p>
              </div>
            </div>
          ))}
        </Section>
      )}

      {/* Vista de Sub-pestaña: Actividad Profesional */}
      {proSubTab === "activity" && (
        <div className="pro-activity-section">
          {/* Banner explicativo */}
          <div className="pro-activity-banner">
            <div className="pro-activity-banner-left">
              <div className="pro-activity-icon-wrap">
                <Activity size={28} />
              </div>
              <div>
                <h3 className="pro-activity-banner-title">Actividad Profesional Publicada</h3>
                <p className="pro-activity-banner-sub">
                  Esta sección muestra todos los servicios que tienes publicados en el catálogo público de SoyVinko.
                  Los clientes pueden verlos, contactarte y solicitar contratación directa desde tu perfil.
                </p>
              </div>
            </div>
            <div className="pro-activity-stats-row">
              <div className="pro-activity-stat">
                <TrendingUp size={16} />
                <span><strong>{activeOfferingsCount}</strong> activos</span>
              </div>
              <div className="pro-activity-stat">
                <Tag size={16} />
                <span><strong>{(p.offerings ?? []).length - activeOfferingsCount}</strong> pausados</span>
              </div>
            </div>
          </div>

          {/* Feed de ofertas publicadas */}
          {(p.offerings ?? []).length > 0 ? (
            <div className="pro-activity-feed">
              {(p.offerings ?? []).map((o: Data, idx: number) => (
                <div
                  className={`pro-activity-card ${o.active ? "active" : "paused"}`}
                  key={o.id}
                >
                  <div className="pro-activity-card-number">{String(idx + 1).padStart(2, "0")}</div>

                  <div className="pro-activity-card-body">
                    <div className="pro-activity-card-top">
                      <div>
                        <div className="pro-activity-card-badges">
                          <span
                            className={`pro-activity-status-pill ${o.active ? "active" : "paused"}`}
                          >
                            <span className={`status-dot-pulse ${o.active ? "green" : "amber"}`} style={{ width: 7, height: 7 }} />
                            {o.active ? "Publicado" : "Pausado"}
                          </span>
                          {o.delivery_mode === "remote" ? (
                            <span className="badge-mode-remote">
                              <Globe size={12} /> Remoto
                            </span>
                          ) : (
                            <span className="badge-mode-onsite">
                              <MapPin size={12} /> Presencial
                            </span>
                          )}
                          <span className="pro-activity-category-badge">
                            {categories.find((c) => c.value === o.category_id)?.label || "Servicio profesional"}
                          </span>
                        </div>
                        <h3 className="pro-activity-card-title">{o.title}</h3>
                      </div>
                      <div className="pro-activity-price-block">
                        <span className="pro-activity-price-label">Desde</span>
                        <span className="pro-activity-price-value">
                          {o.price_min_minor != null
                            ? money(o.price_min_minor, o.currency)
                            : "A convenir"}
                        </span>
                      </div>
                    </div>

                    {o.description && (
                      <p className="pro-activity-card-desc">{o.description}</p>
                    )}

                    <div className="pro-activity-card-footer">
                      <div className="pro-activity-visibility-note">
                        {o.active ? (
                          <>
                            <CheckCircle2 size={14} className="green" />
                            <span>Visible en búsquedas y en tu perfil público</span>
                          </>
                        ) : (
                          <>
                            <Clock size={14} style={{ color: "#f59e0b" }} />
                            <span style={{ color: "#f59e0b" }}>Oculto temporalmente del catálogo</span>
                          </>
                        )}
                      </div>
                      <div className="row" style={{ gap: 8 }}>
                        <RunButton
                          className="link-button small"
                          run={async () => {
                            await api("update_offering", {
                              offering: o.id,
                              title: o.title,
                              description: o.description,
                              price: o.price_min_minor,
                              active: !o.active,
                            });
                            toast(
                              !o.active
                                ? "✅ Servicio publicado en el catálogo."
                                : "Servicio pausado temporalmente."
                            );
                          }}
                          after={refresh}
                        >
                          {o.active ? "Pausar" : "Publicar"}
                        </RunButton>
                        <Action
                          title="Editar servicio"
                          fields={[
                            { name: "title", label: "Título del servicio", value: o.title },
                            {
                              name: "description",
                              label: "Descripción",
                              type: "textarea",
                              value: o.description,
                            },
                            {
                              name: "amount",
                              label: "Precio desde",
                              type: "number",
                              min: 0,
                              value: o.price_min_minor ? o.price_min_minor / 100 : "",
                            },
                            {
                              name: "mode",
                              label: "Modalidad",
                              value: o.delivery_mode,
                              options: [
                                { value: "onsite", label: "Presencial" },
                                { value: "remote", label: "Remoto" },
                              ],
                            },
                          ]}
                          action={async (v) => {
                            await api("update_offering", {
                              offering: o.id,
                              title: v.title,
                              description: v.description,
                              price: v.amount ? Math.round(v.amount * 100) : null,
                              active: o.active,
                            });
                            await api("set_offering_delivery", {
                              offering: o.id,
                              mode: v.mode,
                            });
                          }}
                          after={refresh}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="pro-activity-empty">
              <Activity size={40} style={{ opacity: 0.3 }} />
              <h3>Sin actividad publicada aún</h3>
              <p className="muted">
                Ve a la pestaña <strong>Mis Servicios</strong> para publicar tu primera oferta.
                Aparecerá aquí automáticamente una vez publicada.
              </p>
              <button
                type="button"
                className="button primary"
                onClick={() => setProSubTab("services")}
              >
                <Plus size={16} /> Publicar mi primer servicio
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
function VerificationTab() {
  const account = useData(() => api<Data>("my_account"), []),
    data = useData(() => api<Data[]>("my_kyc"), []),
    files = useData(() => api<Data[]>("my_files"), []);
  const [showWizard, setShowWizard] = useState(false);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const toast = useToast();

  const docKindLabels: Record<string, string> = {
    identity_front: "Cédula / Pasaporte (Frente)",
    identity_back: "Cédula (Reverso)",
    selfie: "Selfie con documento en mano",
    proof_of_address: "Comprobante de dirección / RIF",
    other: "Documento adicional",
  };

  const handleDownloadDoc = async (fileId: string) => {
    setDownloadingId(fileId);
    try {
      await downloadFile(fileId);
    } catch (e: any) {
      toast(e?.message || "Error al descargar el documento.");
    } finally {
      setDownloadingId(null);
    }
  };

  const kycList = data.data ?? [];
  const latest = kycList[0];

  // Si no hay verificación o el usuario abrió explícitamente el asistente
  if (showWizard || (!latest && !kycList.length)) {
    const userPhone =
      account.data?.contacts?.find(
        (c: any) => c.kind === "phone" || c.kind === "whatsapp"
      )?.value || "";

    return (
      <div className="two-col kyc-two-col">
        <PersonalKycWizard
          initialName={account.data?.name || ""}
          initialPhone={userPhone}
          onSuccess={() => {
            setShowWizard(false);
            data.reload();
            account.reload();
            toast("¡Expediente de verificación enviado con éxito!");
          }}
          onCancel={latest ? () => setShowWizard(false) : undefined}
        />

        <div className="kyc-sidebar-guide">
          <Section title="¿Por qué verificar tu identidad?">
            <div className="kyc-benefits-list">
              <div className="kyc-benefit-item">
                <div className="kyc-benefit-icon-circle green">
                  <ShieldCheck size={22} />
                </div>
                <div>
                  <strong>Retiros a Bancos Venezolanos</strong>
                  <p className="muted small">
                    Habilita retiros de tus ganancias vía Pago Móvil y transferencia bancaria directa (Banesco, Mercantil, BDV, etc.).
                  </p>
                </div>
              </div>

              <div className="kyc-benefit-item">
                <div className="kyc-benefit-icon-circle purple">
                  <Award size={22} />
                </div>
                <div>
                  <strong>Insignia de Perfil Verificado</strong>
                  <p className="muted small">
                    Genera 3x más confianza en contratantes y clientes destacando con el sello de identidad validada.
                  </p>
                </div>
              </div>

              <div className="kyc-benefit-item">
                <div className="kyc-benefit-icon-circle blue">
                  <Shield size={22} />
                </div>
                <div>
                  <strong>Lectura Inteligente con OCR</strong>
                  <p className="muted small">
                    Extracción instantánea de tus datos con Tesseract OCR para garantizar máxima velocidad y precisión.
                  </p>
                </div>
              </div>
            </div>
          </Section>

          <Section title="Requisitos importantes de la foto">
            <ol className="steps">
              <li>
                <strong>Cédula de identidad vigente (V o E):</strong> El documento no puede estar vencido.
              </li>
              <li>
                <strong>Solo foto frontal (anverso):</strong> No es necesario fotografiar el reverso.
              </li>
              <li>
                <strong>Buena iluminación:</strong> Sin flash que refleje sobre tu número o nombre.
              </li>
              <li>
                <strong>Selfie nítida:</strong> Sostén la cédula junto a tu rostro sin tapar datos.
              </li>
            </ol>
          </Section>
        </div>
      </div>
    );
  }

  let ocrMeta: any = null;
  try {
    if (latest.external_reference?.startsWith("{")) {
      ocrMeta = JSON.parse(latest.external_reference);
    }
  } catch {}

  return (
    <div className="two-col kyc-two-col">
      <div>
        {data.error && <ErrorBox message={data.error} />}

        {/* Estado APROBADO */}
        {latest.status === "approved" && (
          <div className="kyc-status-card approved">
            <div className="kyc-status-header">
              <div className="kyc-status-icon approved">
                <ShieldCheck size={32} />
              </div>
              <div>
                <div className="row between align-center">
                  <h3 style={{ margin: 0 }}>Identidad Oficialmente Verificada</h3>
                  <Badge value="approved" />
                </div>
                <p className="muted small" style={{ marginTop: 4 }}>
                  Tu cuenta cumple con los estándares oficiales de validación KYC para Venezuela.
                </p>
              </div>
            </div>

            <div className="kyc-verified-profile-box">
              <div className="kyc-verified-field">
                <span className="muted small">Titular Verificado:</span>
                <strong>{ocrMeta?.full_name || account.data?.name}</strong>
              </div>
              <div className="kyc-verified-field">
                <span className="muted small">Documento Oficial:</span>
                <strong>{ocrMeta ? `${ocrMeta.document_type}-${ocrMeta.document_number}` : account.data?.rif || "V-Registrado"}</strong>
              </div>
              {ocrMeta?.birthdate && (
                <div className="kyc-verified-field">
                  <span className="muted small">Nacimiento:</span>
                  <strong>{ocrMeta.birthdate}</strong>
                </div>
              )}
              {latest.expires_at && (
                <div className="kyc-verified-field">
                  <span className="muted small">Válido hasta:</span>
                  <strong>{date(latest.expires_at)}</strong>
                </div>
              )}
            </div>

            <div className="kyc-status-perks">
              <div className="kyc-perk-item">
                <CheckCircle2 size={16} className="green" />
                <span>Retiros bancarios nacionales y Pago Móvil en VES habilitados</span>
              </div>
              <div className="kyc-perk-item">
                <CheckCircle2 size={16} className="green" />
                <span>Distintivo de verificación oficial activo en tu perfil</span>
              </div>
              <div className="kyc-perk-item">
                <CheckCircle2 size={16} className="green" />
                <span>Límites de custodia y transferencias ampliados</span>
              </div>
            </div>

            {latest.documents?.length > 0 && (
              <div style={{ marginTop: 18 }}>
                <h4 style={{ fontSize: 13, marginBottom: 8 }}>Documentos en expediente:</h4>
                <div className="kyc-doc-chips">
                  {latest.documents.map((d: any) => (
                    <div key={d.file_id} className="kyc-doc-chip">
                      <FileText size={14} />
                      <span>{docKindLabels[d.kind] || d.kind}</span>
                      <button
                        type="button"
                        className="kyc-chip-download-btn"
                        title="Descargar copia"
                        disabled={downloadingId === d.file_id}
                        onClick={() => handleDownloadDoc(d.file_id)}
                      >
                        <Download size={13} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Estado PENDIENTE / EN REVISIÓN */}
        {(latest.status === "pending" || latest.status === "in_review") && (
          <div className="kyc-status-card pending">
            <div className="kyc-status-header">
              <div className="kyc-status-icon pending">
                <Clock size={32} />
              </div>
              <div>
                <div className="row between align-center">
                  <h3 style={{ margin: 0 }}>Verificación en Revisión</h3>
                  <Badge value={latest.status} />
                </div>
                <p className="muted small" style={{ marginTop: 4 }}>
                  Hemos recibido tus datos y documentos. El equipo de cumplimiento está procesando tu expediente.
                </p>
              </div>
            </div>

            <div className="kyc-status-details">
              <div>
                <span className="muted small">Fecha de envío:</span>
                <strong>{date(latest.submitted_at)}</strong>
              </div>
              <div>
                <span className="muted small">Tiempo estimado:</span>
                <strong>24 a 48 horas hábiles</strong>
              </div>
              <div>
                <span className="muted small">Expediente ID:</span>
                <code style={{ fontSize: 11 }}>{latest.id.slice(0, 8)}...</code>
              </div>
            </div>

            {latest.documents?.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <h4 style={{ fontSize: 13, marginBottom: 8 }}>Documentos en revisión ({latest.documents.length}):</h4>
                <div className="kyc-doc-chips">
                  {latest.documents.map((d: any) => (
                    <div key={d.file_id} className="kyc-doc-chip">
                      <FileText size={14} />
                      <span>{docKindLabels[d.kind] || d.kind}</span>
                      <button
                        type="button"
                        className="kyc-chip-download-btn"
                        title="Descargar copia"
                        disabled={downloadingId === d.file_id}
                        onClick={() => handleDownloadDoc(d.file_id)}
                      >
                        <Download size={13} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="kyc-pending-note" style={{ marginTop: 14 }}>
              <p className="muted small">
                Te notificaremos por correo electrónico una vez que tu documentación sea evaluada. No necesitas realizar ninguna acción adicional.
              </p>
            </div>

            {/* Opción para adjuntar documento adicional si faltó algo */}
            <div style={{ marginTop: 16, borderTop: "1px dashed var(--line)", paddingTop: 14 }}>
              <Action
                title="Adjuntar documento adicional"
                fields={[
                  {
                    name: "file",
                    label: "Archivo disponible",
                    options: (files.data ?? [])
                      .filter((f) => f.purpose === "kyc")
                      .map((f) => ({
                        value: f.id,
                        label: `Documento · ${date(f.created_at)} · ${f.moderation_status}`,
                      })),
                  },
                  {
                    name: "kind",
                    label: "Tipo de documento",
                    options: [
                      { value: "proof_of_address", label: "Comprobante de Domicilio / RIF" },
                      { value: "identity_back", label: "Cédula · Reverso" },
                      { value: "other", label: "Otro documento adicional" },
                    ],
                  },
                ]}
                action={(v) => api("attach_kyc_file", { case: latest.id, ...v })}
                after={data.reload}
              />
            </div>
          </div>
        )}

        {/* Estado RECHAZADO */}
        {latest.status === "rejected" && (
          <div className="kyc-status-card rejected">
            <div className="kyc-status-header">
              <div className="kyc-status-icon rejected">
                <AlertTriangle size={32} />
              </div>
              <div>
                <div className="row between align-center">
                  <h3 style={{ margin: 0 }}>Verificación no aprobada</h3>
                  <Badge value="rejected" />
                </div>
                <p className="muted small" style={{ marginTop: 4 }}>
                  Tu solicitud anterior no pudo ser aprobada por el equipo de cumplimiento.
                </p>
              </div>
            </div>

            {latest.reason && (
              <div className="kyc-rejection-box">
                <strong>Motivo indicado:</strong>
                <p>{latest.reason}</p>
              </div>
            )}

            <div style={{ marginTop: 18 }}>
              <button
                type="button"
                className="button primary"
                onClick={() => setShowWizard(true)}
              >
                <RefreshCw size={16} /> Reenviar verificación con nuevos datos y fotos
              </button>
            </div>
          </div>
        )}

        {/* Historial si hay más de 1 caso */}
        {kycList.length > 1 && (
          <div style={{ marginTop: 24 }}>
            <Section title="Historial de solicitudes anteriores">
              <div className="kyc-history-list">
                {kycList.slice(1).map((k) => (
                  <div key={k.id} className="kyc-history-item">
                    <div>
                      <strong>Solicitud del {date(k.submitted_at)}</strong>
                      {k.reason && <p className="muted small">{k.reason}</p>}
                    </div>
                    <Badge value={k.status} />
                  </div>
                ))}
              </div>
            </Section>
          </div>
        )}
      </div>

      <div>
        <Section title="Requisitos de verificación">
          <ol className="steps">
            <li>
              <strong>Documento de identidad:</strong> Cédula de identidad venezolana (V o E) vigente o pasaporte.
            </li>
            <li>
              <strong>Calidad de imagen:</strong> Fotos legibles, sin flash que tape datos, tomadas en fondo plano con los 4 bordes visibles.
            </li>
            <li>
              <strong>Selfie de autenticidad:</strong> Sostén tu documento a la altura de tu pecho, mirando directamente a la cámara.
            </li>
            <li>
              <strong>Confidencialidad:</strong> Tus datos solo son revisados por personal de cumplimiento autorizado para prevenir suplantaciones y fraudes.
            </li>
          </ol>
          <p className="info-note">
            Una vez aprobada tu cuenta, podrás solicitar retiros directos a tu banco venezolano o Pago Móvil en bolívares (VES) y USDT.
          </p>
        </Section>
      </div>
    </div>
  );
}
export function FilesTab() {
  const data = useData(() => api<Data[]>("my_files"), []),
    [purpose, setPurpose] = useState("evidence"),
    [busy, setBusy] = useState(false),
    toast = useToast();
  const titles: Record<string, string> = {
    avatar: "Foto de perfil",
    portfolio: "Portafolio",
    request: "Referencia de solicitud",
    chat: "Adjunto de conversación",
    product: "Imagen de producto",
    kyc: "Verificación de identidad",
    evidence: "Evidencia de trabajo",
  };
  return (
    <Section title="Mis archivos privados">
      <div className="row wrap">
        <select
          aria-label="Finalidad del archivo"
          value={purpose}
          onChange={(e) => setPurpose(e.target.value)}
        >
          {Object.entries(titles).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
        <label className="button primary">
          <Paperclip size={17} />
          {busy ? "Cargando…" : "Cargar archivo"}
          <input
            hidden
            disabled={busy}
            type="file"
            accept="image/jpeg,image/png,image/webp,application/pdf"
            onChange={async (e) => {
              const f = e.target.files?.[0];
              if (!f) return;
              setBusy(true);
              try {
                const body = new FormData();
                body.set("file", f);
                body.set("purpose", purpose);
                await request("/api/v1/files/upload", "POST", body);
                data.reload();
                toast("Archivo cargado para revisión.");
              } catch (err) {
                toast((err as Error).message, true);
              } finally {
                setBusy(false);
                e.target.value = "";
              }
            }}
          />
        </label>
      </div>
      <p className="muted small">
        Hasta 10 MB. JPG, PNG, WEBP y documentos PDF. Las imágenes se procesan
        para quitar metadatos de ubicación.
      </p>
      {data.error && <ErrorBox message={data.error} />}{" "}
      {data.data?.length ? (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Finalidad</th>
                <th>Fecha</th>
                <th>Tamaño</th>
                <th>Estado</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.data.map((f) => (
                <tr key={f.id}>
                  <td>{titles[f.purpose]}</td>
                  <td>{date(f.created_at)}</td>
                  <td>{Math.ceil(f.bytes / 1024)} KB</td>
                  <td>
                    <Badge value={f.moderation_status} />
                  </td>
                  <td>
                    <RunButton
                      className="link-button"
                      run={() => downloadFile(f.id)}
                    >
                      <Download size={16} /> Descargar
                    </RunButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <Empty
          title="Documentos en un lugar seguro"
          text="Carga referencias, evidencia, imágenes de productos o documentos de identidad."
        />
      )}
    </Section>
  );
}
function SecurityTab() {
  const { logout } = useAuth(),
    toast = useToast();
  const sessions = useData(() => api<Data[]>("my_sessions"), []),
    mfa = useData(() => request("/api/v1/auth/mfa"), []),
    [enrollment, setEnrollment] = useState<Data | null>(null);
  return (
    <div className="two-col">
      <div>
        <Section title="Correo electrónico">
          <RunButton
            run={() => request("/api/v1/auth/verification-email", "POST")}
          >
            Enviar enlace de verificación
          </RunButton>
          <p className="muted small">
            Recibirás un enlace en tu correo electrónico para verificar tu cuenta.
          </p>
        </Section>
        <Section title="Cambiar contraseña">
          <FieldsForm
            fields={[
              {
                name: "current_password",
                label: "Contraseña actual",
                type: "password",
              },
              {
                name: "new_password",
                label: "Nueva contraseña",
                type: "password",
                minLength: 12,
                hint: "Al menos 12 caracteres. Se cerrarán todas tus sesiones.",
              },
            ]}
            submit="Actualizar contraseña"
            onSubmit={async (v) => {
              await request("/api/v1/auth/change-password", "POST", v);
              await logout();
              toast("Contraseña actualizada. Inicia sesión nuevamente.");
            }}
          />
        </Section>
      </div>
      <div>
        <Section title="Autenticación en dos pasos">
          <p>
            Usa una aplicación de autenticación para añadir un código temporal a
            tu inicio de sesión.
          </p>
          <Badge value={mfa.data?.enabled ? "active" : "inactive"} />
          <div className="spaced">
            {mfa.data?.enabled ? (
              <Action
                title="Desactivar MFA"
                fields={[
                  { name: "password", label: "Contraseña", type: "password" },
                  { name: "otp", label: "Código del autenticador" },
                ]}
                action={(v) => request("/api/v1/auth/mfa/disable", "POST", v)}
                after={mfa.reload}
              />
            ) : (
              <Action
                title="Configurar autenticador"
                fields={[
                  { name: "password", label: "Contraseña", type: "password" },
                ]}
                action={async (v) =>
                  setEnrollment(
                    await request("/api/v1/auth/mfa/enroll", "POST", v),
                  )
                }
              />
            )}
          </div>
          {enrollment && (
            <>
              <p>Agrega esta clave a tu autenticador como cuenta SoyVinko:</p>
              <code className="secret-key">{enrollment.secret}</code>
              <FieldsForm
                fields={[
                  {
                    name: "password",
                    label: "Confirma tu contraseña",
                    type: "password",
                  },
                  { name: "otp", label: "Código de seis dígitos" },
                ]}
                submit="Activar MFA"
                onSubmit={async (v) => {
                  await request("/api/v1/auth/mfa/confirm", "POST", v);
                  setEnrollment(null);
                  mfa.reload();
                }}
              />
            </>
          )}
        </Section>
        <Section title="Dispositivos conectados">
          {sessions.data?.map((s) => (
            <div className="list-row" key={s.id}>
              <div>
                <strong className="clamp-2">
                  {s.device_label ?? "Dispositivo"}
                </strong>
                <p>{date(s.created_at)}</p>
              </div>
              <RunButton
                run={() => api("revoke_session", { session: s.id })}
                after={sessions.reload}
              >
                Revocar
              </RunButton>
            </div>
          ))}
        </Section>
      </div>
    </div>
  );
}
export default function Profile() {
  const [params, setParams] = useSearchParams(),
    tab = params.get("tab") ?? "account";
  const tabs = [
    ["account", "Mi cuenta"],
    ["professional", "Perfil profesional"],
    ["verification", "Verificación"],
    ["files", "Archivos"],
    ["security", "Seguridad"],
  ];
  return (
    <>
      <PageTitle
        eyebrow="TU IDENTIDAD EN SOYVINKO"
        title="Mi perfil"
        description="Mantén tu información al día y decide cómo quieres trabajar."
      />
      <div className="tabs">
        {tabs.map(([key, title]) => (
          <button
            key={key}
            className={tab === key ? "active" : ""}
            onClick={() => setParams({ tab: key })}
          >
            {title}
          </button>
        ))}
      </div>
      {tab === "professional" ? (
        <ProfessionalTab />
      ) : tab === "verification" ? (
        <VerificationTab />
      ) : tab === "files" ? (
        <FilesTab />
      ) : tab === "security" ? (
        <SecurityTab />
      ) : (
        <AccountTab />
      )}
    </>
  );
}
