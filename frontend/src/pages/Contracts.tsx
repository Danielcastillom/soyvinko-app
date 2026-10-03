import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  Send,
  MapPin,
  ShieldCheck,
  ArrowRight,
  Paperclip,
  Navigation,
  CheckCircle2,
  Wallet,
  MessageCircle,
  Mail,
  Phone,
  Globe,
  Star,
  UserRound,
  BriefcaseBusiness,
  Handshake,
  Check,
} from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { mapboxTiles } from "../mapbox";
import { useDeviceLocation } from "../location";
import { RequestImages } from "../RequestPhotos";
import {
  api,
  newKey,
  money,
  date,
  request,
  downloadFile,
  type Data,
} from "../api";
import { useAuth, useToast } from "../context";
import {
  useData,
  PageTitle,
  Section,
  Loading,
  Empty,
  Badge,
  RunButton,
  Action,
  FieldsForm,
  Modal,
  reasonField,
  ErrorBox,
  ExternalPayments,
} from "../components";
export function Contracts() {
  const data = useData(() => api<Data[]>("my_contracts"), [], 10000);
  return (
    <>
      <PageTitle
        eyebrow="TUS CONEXIONES"
        title="Servicios contratados"
        description="Acuerdos, conversaciones y entregas, en un solo lugar."
      />
      {data.loading ? (
        <Loading />
      ) : data.error ? (
        <ErrorBox message={data.error} />
      ) : data.data?.length ? (
        <div className="request-grid">
          {data.data.map((c) => (
            <Link
              className="panel request-card"
              to={`/contracts/${c.id}`}
              key={c.id}
            >
              <div className="row between">
                <Badge value={c.status} />
                <Badge value={c.delivery_mode} />
              </div>
              <h2>
                {c.work_requirements?.slice(0, 75) || "Servicio contratado"}
              </h2>
              <p>{date(c.accepted_at)}</p>
              <div className="row between">
                <strong>{money(c.agreed_price_minor, c.currency)}</strong>
                <ArrowRight size={21} />
              </div>
              <Badge value={c.payment_protection} />
            </Link>
          ))}
        </div>
      ) : (
        <Empty
          title="Aquí empieza el trabajo en equipo"
          text="Cuando ambas partes acepten una solicitud, encontrarás el servicio y su chat aquí."
        >
          <Link to="/explore" className="button primary">
            Explorar profesionales
          </Link>
        </Empty>
      )}
    </>
  );
}
function ServiceMap({
  contract,
  professional,
}: {
  contract: string;
  professional: boolean;
}) {
  const { config } = useDeviceLocation();
  const [enabled, setEnabled] = useState(false),
    [tracking, setTracking] = useState(false),
    [trackError, setTrackError] = useState("");
  const data = useData(
    () => (enabled ? api("navigate", { contract }) : Promise.resolve(null)),
    [contract, enabled],
    enabled ? 10000 : 0,
  );
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!enabled || !data.data || !ref.current || !config?.mapbox_public_token)
      return;
    const destination = data.data.destination,
      position = data.data.professional_position;
    const map = L.map(ref.current, { scrollWheelZoom: false }).setView(
      [destination.latitude, destination.longitude],
      14,
    );
    mapboxTiles(config.mapbox_public_token).addTo(map);
    L.circleMarker([destination.latitude, destination.longitude], {
      radius: 10,
      color: "#153d32",
      fillOpacity: 1,
    })
      .addTo(map)
      .bindTooltip("Lugar del servicio");
    if (position) {
      L.circleMarker([position.latitude, position.longitude], {
        radius: 8,
        color: "#ef895c",
        fillOpacity: 1,
      })
        .addTo(map)
        .bindTooltip("Profesional");
      map.fitBounds(
        [
          [destination.latitude, destination.longitude],
          [position.latitude, position.longitude],
        ],
        { padding: [35, 35], maxZoom: 15 },
      );
    }
    return () => {
      map.remove();
    };
  }, [data.data, enabled, config?.mapbox_public_token]);
  useEffect(() => {
    if (!tracking) return;
    if (!navigator.geolocation) {
      setTrackError("Tu navegador no permite geolocalización.");
      return;
    }
    const watch = navigator.geolocation.watchPosition(
      (p) => {
        api("report_position", {
          contract,
          lat: p.coords.latitude,
          lon: p.coords.longitude,
          accuracy: Math.round(p.coords.accuracy),
        })
          .then(() => data.reload())
          .catch((e) => {
            setTrackError(e.message);
            setTracking(false);
          });
      },
      (e) => {
        setTrackError(e.message);
        setTracking(false);
      },
      { enableHighAccuracy: true, maximumAge: 10000 },
    );
    return () => navigator.geolocation.clearWatch(watch);
  }, [tracking, contract]);
  return (
    <>
      <RunButton
        run={async () => {
          setEnabled(!enabled);
        }}
        className="button secondary"
      >
        <MapPin size={16} />
        {enabled ? "Ocultar mapa" : "Ver ubicación compartida"}
      </RunButton>
      {professional && (
        <button
          className="button secondary"
          onClick={() => setTracking(!tracking)}
        >
          <Navigation size={16} />
          {tracking ? "Detener mi ubicación" : "Compartir mi trayecto"}
        </button>
      )}
      {trackError && <ErrorBox message={trackError} />}{" "}
      {enabled &&
        (data.error ? (
          <p className="info-note">
            La ubicación todavía no se ha compartido, venció o fue revocada.
          </p>
        ) : (
          data.data && (
            <>
              <div ref={ref} className="service-map" />
              <p>{data.data.destination.address}</p>
              <p className="muted small">
                {data.data.destination.instructions}
              </p>
              <a
                className="link-button"
                href={`https://www.google.com/maps/dir/?api=1&destination=${data.data.destination.latitude},${data.data.destination.longitude}`}
                target="_blank"
                rel="noreferrer"
              >
                Abrir ruta en mi navegador de mapas ↗
              </a>
            </>
          )
        ))}
    </>
  );
}
export function ContractDetail() {
  const { id = "" } = useParams(),
    { user } = useAuth(),
    toast = useToast();
  const [message, setMessage] = useState(""),
    [sending, setSending] = useState(false),
    [quote, setQuote] = useState<Data | null>(null),
    [deliveryFiles, setDeliveryFiles] = useState<string[]>([]);
  const files = useData(() => api<Data[]>("my_files"), []);
  const contract = useData(
      () => api("contract_detail", { contract: id }),
      [id],
      5000,
    ),
    chat = useData(
      () => api<Data[]>("chat_messages", { contract: id }),
      [id],
      3000,
    ),
    escrow = useData(() => api("escrow_details", { contract: id }), [id], 5000),
    contacts = useData(() => api("contract_contacts", { contract: id }), [id], 5000);
  const refresh = () => {
    contract.reload();
    chat.reload();
    escrow.reload();
    contacts.reload();
  };
  const c = contract.data,
    e = escrow.data,
    ctData = contacts.data;
  const bottom = useRef<HTMLDivElement>(null);
  useEffect(() => {
    bottom.current?.scrollIntoView({ block: "nearest" });
  }, [chat.data?.length]);
  if (contract.loading) return <Loading />;
  if (contract.error) return <ErrorBox message={contract.error} />;
  if (!c) return <Empty title="Servicio no disponible" />;
  const isCustomer = c.customer_id === user?.id,
    isProfessional = c.professional_id === user?.id,
    active = !["completed", "cancelled", "disputed"].includes(c.status),
    protectedPayment = c.payment_protection === "guaranteed";

  // Contactos desbloqueados del contrato
  const allContacts = [
    ...(ctData?.contacts ?? []),
    ...(ctData?.professional_contacts ?? []),
  ];
  const rawWhatsapp =
    allContacts.find((x: any) => x.kind === "whatsapp")?.value ||
    ctData?.primary_whatsapp;
  const rawPhone =
    allContacts.find((x: any) => x.kind === "phone")?.value ||
    ctData?.primary_phone ||
    rawWhatsapp;
  const otherPartyEmail = ctData?.email;

  const cleanWaNumber = (rawWhatsapp || rawPhone || "").replace(/\D/g, "");
  const waPhone = cleanWaNumber
    ? cleanWaNumber.startsWith("0")
      ? "58" + cleanWaNumber.slice(1)
      : cleanWaNumber.startsWith("58")
        ? cleanWaNumber
        : "58" + cleanWaNumber
    : null;

  // Calificación del usuario actual si ya calificó
  const myReview = (c.reviews ?? []).find((r: any) => r.author_id === user?.id);

  async function send(event: React.FormEvent) {
    event.preventDefault();
    if (!message.trim()) return;
    setSending(true);
    try {
      await api("send_message", { contract: id, body: message, key: newKey() });
      setMessage("");
      chat.reload();
    } catch (err) {
      toast((err as Error).message, true);
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      <Link to="/contracts" className="back-link">
        ← Mis contrataciones y servicios
      </Link>

      {/* Cabecera Oficial del Contrato Digital */}
      <div className="digital-contract-header">
        <div className="digital-contract-topline">
          <div className="row" style={{ gap: 10, alignItems: "center" }}>
            <ShieldCheck size={22} color="#F59E0B" />
            <span className="digital-contract-folio">
              CONTRATO DIGITAL: #VNK-{id.slice(0, 8).toUpperCase()}
            </span>
          </div>
          <div className="row" style={{ gap: 8, alignItems: "center" }}>
            <Badge value={c.status} />
            {c.delivery_mode === "remote" ? (
              <span className="badge-mode-remote">
                <Globe size={13} /> Proyecto Remoto
              </span>
            ) : (
              <span className="badge-mode-onsite">
                <MapPin size={13} /> Servicio Presencial
              </span>
            )}
          </div>
        </div>

        <div className="digital-contract-title-area">
          <h1>Contrato Digital de Prestación de Servicios</h1>
          <p style={{ color: "#E0D7F5", margin: "4px 0 0 0", fontSize: 15 }}>
            {c.title || c.work_requirements?.slice(0, 90) || "Servicio acordado"}
          </p>
        </div>

        <div className="digital-contract-parties-grid">
          <div className="contract-party-box">
            <UserRound size={22} color="#A78BFA" />
            <div>
              <div className="contract-party-role">Cliente Contratante</div>
              <div className="contract-party-name">{c.customer_name}</div>
            </div>
          </div>
          <div className="contract-party-box">
            <BriefcaseBusiness size={22} color="#F59E0B" />
            <div>
              <div className="contract-party-role">Comerciante / Prestador</div>
              <div className="contract-party-name">{c.professional_name}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Modalidad de Pago Estipulada en el Contrato */}
      <div
        className={`contract-payment-card ${protectedPayment ? "custody" : "in-person"}`}
      >
        <div className="contract-payment-header">
          <div className="contract-payment-title">
            {protectedPayment ? (
              <>
                <ShieldCheck size={26} color="#6F3DFF" />
                <span>
                  Modalidad: Cancelación por la Plataforma (Custodia Digital)
                </span>
              </>
            ) : (
              <>
                <Handshake size={26} color="#0284C7" />
                <span>
                  Modalidad: Cancelación Directa en Persona (Acuerdo entre Partes)
                </span>
              </>
            )}
          </div>
          <div className="row" style={{ gap: 10, alignItems: "center" }}>
            <span style={{ fontSize: 13, color: "#4B5563" }}>
              Importe acordado:
            </span>
            <strong
              style={{
                fontSize: 20,
                color: protectedPayment ? "#6F3DFF" : "#0284C7",
              }}
            >
              {money(c.agreed_price_minor, c.currency)}
            </strong>
          </div>
        </div>

        <p className="contract-payment-desc">
          {protectedPayment
            ? "El cliente cancela por medio de la plataforma. Los fondos se mantienen bajo Custodia Digital Garantizada de SoyVinko y se liberarán directamente a la billetera del profesional en cuanto el cliente valide la entrega conforme del servicio."
            : "Las partes acordaron la cancelación en persona directamente al momento de realizarse el trabajo (Efectivo, Pago Móvil o transferencia directa). La plataforma no retiene fondos; al concluir el servicio, ambas partes confirman la realización del trabajo y el pago recibido."}
        </p>

        {protectedPayment && (
          <div
            className="row between wrap"
            style={{
              background: "rgba(255,255,255,0.75)",
              padding: "12px 16px",
              borderRadius: 12,
              border: "1px solid rgba(139,92,246,0.25)",
              marginTop: 4,
            }}
          >
            <div className="row" style={{ gap: 8, alignItems: "center" }}>
              <span
                className={`status-dot-pulse ${
                  e?.funds_guaranteed || e?.status === "released"
                    ? "green"
                    : "amber"
                }`}
              />
              <span style={{ fontSize: 13, fontWeight: 700, color: "#1F1438" }}>
                Estado de Fondos:{" "}
                {e?.status === "released"
                  ? "✅ Pago liberado al profesional"
                  : e?.funds_guaranteed
                    ? "🔒 Fondos en custodia digital reservada"
                    : "⏳ Pendiente de reservar saldo en custodia"}
              </span>
            </div>

            {isCustomer && e?.status === "unfunded" && active && (
              <RunButton
                className="button primary"
                run={async () =>
                  setQuote(
                    await api("quote_contract_funding", { contract: id }),
                  )
                }
              >
                <Wallet size={16} /> Reservar saldo en custodia
              </RunButton>
            )}
          </div>
        )}
      </div>

      {/* Canales de Contacto Directo Mutuo Desbloqueados */}
      <div className="unlocked-contacts-card">
        <div className="unlocked-contacts-head">
          <Phone size={22} color="#059669" />
          <div>
            <h3>Canales de Contacto Directo Desbloqueados</h3>
            <p style={{ margin: "2px 0 0 0", fontSize: 13, color: "#065F46" }}>
              {isCustomer
                ? `Contacto directo habilitado con ${c.professional_name}`
                : `Contacto directo habilitado con el cliente ${c.customer_name}`}
            </p>
          </div>
        </div>

        <div className="unlocked-contacts-buttons">
          {waPhone && (
            <a
              href={`https://wa.me/${waPhone}?text=${encodeURIComponent(
                `Hola ${ctData?.name || ""}, te contacto acerca del Contrato Digital #${id.slice(0, 8).toUpperCase()} en SoyVinko.`
              )}`}
              target="_blank"
              rel="noreferrer"
              className="btn-whatsapp-direct"
              title="Abrir conversación en WhatsApp"
            >
              <MessageCircle size={18} /> Chatear por WhatsApp (
              {rawWhatsapp || rawPhone})
            </a>
          )}
          {rawPhone && (
            <a
              href={`tel:${rawPhone}`}
              className="btn-phone-direct"
              title="Llamar directamente"
            >
              <Phone size={18} /> Llamar ({rawPhone})
            </a>
          )}
          {otherPartyEmail && (
            <a
              href={`mailto:${otherPartyEmail}?subject=${encodeURIComponent(
                `Contrato SoyVinko #${id.slice(0, 8).toUpperCase()}`
              )}`}
              className="btn-email-direct"
              title="Enviar correo electrónico"
            >
              <Mail size={18} /> Enviar correo ({otherPartyEmail})
            </a>
          )}
        </div>
      </div>

      {/* Tracker del Ciclo del Contrato Digital */}
      <div className="contract-lifecycle-tracker">
        <div className="step-item completed">
          <div className="step-circle"><Check size={16} /></div>
          <span>1. Contrato Formalizado</span>
        </div>
        <div className="step-item completed">
          <div className="step-circle"><Check size={16} /></div>
          <span>2. Contacto Directo</span>
        </div>
        <div className={`step-item ${["in_progress", "awaiting_confirmation", "completed"].includes(c.status) ? "completed" : "active"}`}>
          <div className="step-circle">
            {["in_progress", "awaiting_confirmation", "completed"].includes(c.status) ? <Check size={16} /> : "3"}
          </div>
          <span>3. Ejecución del Servicio</span>
        </div>
        <div className={`step-item ${c.status === "completed" ? "completed" : c.status === "awaiting_confirmation" ? "active" : ""}`}>
          <div className="step-circle">
            {c.status === "completed" ? <Check size={16} /> : "4"}
          </div>
          <span>4. Validación y Entrega</span>
        </div>
        <div className={`step-item ${c.status === "completed" ? "completed" : ""}`}>
          <div className="step-circle">
            {c.status === "completed" ? <Check size={16} /> : "5"}
          </div>
          <span>5. Calificación y Pago</span>
        </div>
      </div>

      <div className="contract-layout">
        <main>
          {/* Validación y Liberación de Pago para el Cliente (Pago Protegido) */}
          {isCustomer && protectedPayment && ["in_progress", "awaiting_confirmation"].includes(c.status) && (
            <div className="release-funds-action-card">
              <div className="row between wrap" style={{ alignItems: "center", gap: 16 }}>
                <div>
                  <div className="row" style={{ gap: 8, alignItems: "center", marginBottom: 4 }}>
                    <ShieldCheck size={22} color="#6F3DFF" />
                    <h3 style={{ margin: 0 }}>¿El trabajo fue realizado conforme a lo acordado?</h3>
                  </div>
                  <p className="muted small" style={{ margin: 0, maxWidth: 520 }}>
                    Al confirmar, se liberará de inmediato el pago de <strong>{money(c.agreed_price_minor, c.currency)}</strong> retenido en custodia a la billetera de {c.professional_name}.
                  </p>
                </div>
                <RunButton
                  className="button primary"
                  confirm={`¿Confirmar que el servicio fue entregado a satisfacción y liberar ${money(c.agreed_price_minor, c.currency)} a ${c.professional_name}?`}
                  run={async () => {
                    const lastDelivery = e?.deliveries?.[e.deliveries.length - 1]?.id;
                    await api("release_contract_funds", {
                      contract: id,
                      delivery: lastDelivery,
                      key: newKey(),
                    });
                    toast("¡Pago liberado exitosamente al profesional!");
                    refresh();
                  }}
                >
                  <CheckCircle2 size={18} /> Validar y Liberar Pago
                </RunButton>
              </div>
            </div>
          )}

          {/* Confirmación para Pago en Persona */}
          {!protectedPayment && ["in_progress", "awaiting_confirmation"].includes(c.status) && (
            <div className="release-funds-action-card external">
              <div className="row between wrap" style={{ alignItems: "center", gap: 16 }}>
                <div>
                  <div className="row" style={{ gap: 8, alignItems: "center", marginBottom: 4 }}>
                    <CheckCircle2 size={22} color="#0284C7" />
                    <h3 style={{ margin: 0 }}>Finalización y Pago en Persona</h3>
                  </div>
                  <p className="muted small" style={{ margin: 0, maxWidth: 520 }}>
                    Servicio con cancelación directa en persona ({money(c.agreed_price_minor, c.currency)}). Al terminar la labor y recibir/entregar el pago acordado, confirmen la finalización.
                  </p>
                </div>
                <RunButton
                  className="button primary"
                  confirm="¿Confirmar que el servicio concluyó y el pago acordado en persona fue realizado?"
                  run={async () => {
                    await api("advance_contract", { contract: id, action: "finish" });
                    toast("¡Servicio confirmado y finalizado!");
                    refresh();
                  }}
                >
                  Confirmar Finalización
                </RunButton>
              </div>
            </div>
          )}

          {/* Acuerdo y Condiciones */}
          <Section title="Alcance y Términos del Acuerdo">
            <div className="row between" style={{ marginBottom: 12 }}>
              <div>
                <small className="muted">Importe acordado</small>
                <h2 className="amount" style={{ margin: 0 }}>
                  {money(c.agreed_price_minor, c.currency)}
                </h2>
              </div>
              <Badge value={c.payment_protection} />
            </div>

            {/* Datos de ubicación si es presencial */}
            {c.delivery_mode === "onsite" && c.onsite_location && (
              <div className="onsite-location-box" style={{ marginBottom: 16 }}>
                <div className="row" style={{ gap: 10, alignItems: "center" }}>
                  <MapPin size={20} color="#6F3DFF" />
                  <div>
                    <strong>Lugar de prestación del servicio:</strong>
                    <p style={{ margin: "2px 0 0 0", color: "#18112C" }}>
                      {c.onsite_location.address_line}
                      {c.onsite_location.city ? ` · ${c.onsite_location.city}` : ""}
                    </p>
                  </div>
                </div>
                {c.onsite_location.instructions && (
                  <p className="muted small" style={{ marginTop: 8, marginBottom: 0 }}>
                    <strong>Indicaciones de llegada:</strong> {c.onsite_location.instructions}
                  </p>
                )}
              </div>
            )}

            <p className="preserve">{c.work_requirements}</p>
            <RequestImages photos={c.reference_photos} />
            {c.payment_terms && (
              <p style={{ fontStyle: "italic", color: "#4B5563" }}>
                Condiciones: {c.payment_terms}
              </p>
            )}

            <div className="row wrap" style={{ marginTop: 16 }}>
              {active && (
                <Action
                  title="Proponer nuevas condiciones"
                  fields={[
                    {
                      name: "amount",
                      label: "Importe",
                      type: "number",
                      min: 1,
                      value: (c.agreed_price_minor ?? 0) / 100,
                    },
                    {
                      name: "method",
                      label: "Forma de pago",
                      value: protectedPayment
                        ? "wallet"
                        : (c.payment_method ?? "A convenir"),
                    },
                    {
                      name: "terms",
                      label: "Condiciones y alcance",
                      type: "textarea",
                    },
                  ]}
                  action={(v) =>
                    api("propose_terms", {
                      contract: id,
                      price: Math.round(v.amount * 100),
                      method: protectedPayment ? "wallet" : v.method,
                      terms: v.terms,
                    })
                  }
                  after={refresh}
                />
              )}
            </div>

            {c.terms?.map((t: Data) => (
              <div className="terms-card" key={t.id}>
                <strong>Condiciones · versión {t.version}</strong>
                <p>{t.payment_terms}</p>
                <p>
                  {money(t.price_minor, t.currency)} · {t.payment_method}
                </p>
                {((isCustomer && !t.customer_accepted_at) ||
                  (isProfessional && !t.professional_accepted_at)) &&
                  t.version ===
                    Math.max(...c.terms.map((x: Data) => x.version)) && (
                    <RunButton
                      confirm="¿Aceptas estas condiciones y el precio indicado?"
                      run={() => api("accept_terms", { terms: t.id })}
                      after={refresh}
                    >
                      Aceptar condiciones
                    </RunButton>
                  )}
                {t.customer_accepted_at && t.professional_accepted_at && (
                  <Badge value="accepted" />
                )}
              </div>
            ))}
          </Section>

          {/* Chat Interno en Vivo */}
          <section className="panel chat-panel">
            <div className="section-head">
              <h2>
                <MessageCircle size={20} /> Conversación y Mensajes del Contrato
              </h2>
            </div>

            <div className="chat-messages">
              {chat.error && <ErrorBox message={chat.error} />}
              {chat.data?.length ? (
                [...chat.data].reverse().map((m) => (
                  <div
                    key={m.id}
                    className={`message ${m.sender_id === user?.id ? "mine" : ""}`}
                  >
                    <p>{m.body}</p>
                    {m.file_id && (
                      <RunButton
                        className="link-button"
                        run={() => downloadFile(m.file_id)}
                      >
                        <Paperclip size={14} /> Descargar adjunto
                      </RunButton>
                    )}
                    <time>
                      {new Date(m.created_at).toLocaleTimeString("es-VE", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </time>
                  </div>
                ))
              ) : (
                <div className="chat-welcome">
                  <ShieldCheck />
                  <h3>Canal oficial de coordinación</h3>
                  <p>
                    Confirmen detalles, compartan especificaciones y mantengan el
                    registro de su acuerdo aquí.
                  </p>
                </div>
              )}
              <div ref={bottom} />
            </div>

            {active && (
              <form className="chat-composer" onSubmit={send}>
                <input
                  aria-label="Mensaje"
                  placeholder="Escribe un mensaje al profesional o cliente…"
                  value={message}
                  onChange={(ev) => setMessage(ev.target.value)}
                  maxLength={10000}
                />
                <button
                  className="button primary"
                  aria-label="Enviar mensaje"
                  disabled={sending || !message.trim()}
                >
                  <Send size={18} />
                </button>
              </form>
            )}
          </section>

          {/* Entregas y Liberación de Pago */}
          {protectedPayment && (
            <Section title="Entregas del Trabajo y Liberación de Pago">
              {isProfessional && c.status === "in_progress" && (
                <Action
                  title="Presentar entrega de servicio"
                  fields={[
                    {
                      name: "summary",
                      label: "Describe el trabajo concluido y entregado",
                      type: "textarea",
                    },
                    {
                      name: "files",
                      label: "Archivos de evidencia",
                      required: false,
                      hint: "Opcional. Selecciona evidencias; puedes cargar más desde Perfil → Archivos.",
                      multiple: true,
                      options: (files.data ?? [])
                        .filter(
                          (f) =>
                            ["chat", "evidence", "portfolio"].includes(
                              f.purpose,
                            ) && f.moderation_status === "approved",
                        )
                        .map((f) => ({
                          value: f.id,
                          label: `Evidencia · ${date(f.created_at)} · ${Math.ceil(
                            f.bytes / 1024,
                          )} KB`,
                        })),
                    },
                  ]}
                  action={(v) =>
                    api("submit_delivery", {
                      contract: id,
                      summary: v.summary,
                      key: newKey(),
                      files: v.files ?? [],
                    })
                  }
                  after={refresh}
                />
              )}

              {e?.deliveries?.length ? (
                e.deliveries.map((d: Data) => (
                  <div key={d.id} className="delivery-card">
                    <div className="row between">
                      <h3>Entrega {d.version}</h3>
                      <Badge value={d.status} />
                    </div>
                    <p>{d.summary}</p>
                    {d.files?.map((file: string) => (
                      <RunButton
                        key={file}
                        className="link-button"
                        run={() => downloadFile(file)}
                      >
                        <Paperclip size={14} /> Descargar evidencia
                      </RunButton>
                    ))}
                    {isCustomer &&
                      c.status === "awaiting_confirmation" &&
                      d.id === e.deliveries[e.deliveries.length - 1].id && (
                        <div className="row wrap" style={{ marginTop: 14 }}>
                          <RunButton
                            className="button primary"
                            confirm={`Confirmo que el trabajo cumple todos los requisitos. Liberar ${money(
                              c.agreed_price_minor,
                              c.currency,
                            )} al profesional y finalizar el contrato digital.`}
                            run={() =>
                              api("release_contract_funds", {
                                contract: id,
                                delivery: d.id,
                                key: newKey(),
                              })
                            }
                            after={refresh}
                          >
                            <CheckCircle2 size={16} /> Validar entrega y liberar pago
                          </RunButton>
                          <Action
                            title="Solicitar correcciones"
                            fields={[reasonField]}
                            action={(v) =>
                              api("request_delivery_changes", {
                                delivery: d.id,
                                ...v,
                              })
                            }
                            after={refresh}
                          />
                        </div>
                      )}
                  </div>
                ))
              ) : (
                <p className="muted">
                  Las entregas del profesional y las validaciones de pago
                  aparecerán en esta sección.
                </p>
              )}
            </Section>
          )}

          {/* Calificación Inmediata y Reseña */}
          {c.status === "completed" && (
            <Section title="Calificación del Servicio y Experiencia">
              {myReview ? (
                <div className="instant-rating-box">
                  <div className="row between">
                    <div className="row" style={{ gap: 8, alignItems: "center" }}>
                      <Star size={24} color="#F59E0B" fill="#F59E0B" />
                      <strong style={{ fontSize: 16 }}>Tu Calificación Registrada</strong>
                    </div>
                    <span className="merchant-verified-badge">
                      Publicada de inmediato
                    </span>
                  </div>
                  <div className="row" style={{ gap: 4, margin: "10px 0" }}>
                    {[1, 2, 3, 4, 5].map((s) => (
                      <span
                        key={s}
                        style={{
                          color: s <= myReview.rating ? "#F59E0B" : "#D1D5DB",
                          fontSize: 24,
                        }}
                      >
                        ★
                      </span>
                    ))}
                    <strong style={{ marginLeft: 8, alignSelf: "center", color: "#1F1438" }}>
                      {myReview.rating} de 5 estrellas
                    </strong>
                  </div>
                  {myReview.comment && (
                    <p style={{ fontStyle: "italic", margin: "6px 0", color: "#374151" }}>
                      "{myReview.comment}"
                    </p>
                  )}
                  <small className="muted">
                    {myReview.recommends
                      ? "✓ Recomendaste a esta persona"
                      : "No recomendaste a esta persona"}
                  </small>
                </div>
              ) : (
                <>
                  <Action
                    title="Calificar servicio realizado"
                    fields={[
                      {
                        name: "rating",
                        label: "Calificación en estrellas",
                        options: [5, 4, 3, 2, 1].map((n) => ({
                          value: String(n),
                          label: `${"★".repeat(n)} · ${n} ${n === 1 ? "estrella" : "estrellas"}`,
                        })),
                      },
                      {
                        name: "recommends",
                        label: "Recomiendo a esta persona",
                        type: "checkbox",
                        value: true,
                      },
                      {
                        name: "comment",
                        label: "Cuéntanos sobre el trabajo realizado",
                        type: "textarea",
                      },
                    ]}
                    action={(v) =>
                      api("rate_contract", {
                        contract: id,
                        rating: Number(v.rating),
                        recommends: v.recommends,
                        comment: v.comment,
                      })
                    }
                    after={refresh}
                  />
                  <p className="muted small" style={{ marginTop: 8 }}>
                    Tu calificación se publica de inmediato en el perfil para premiar
                    el servicio cumplido y fortalecer la reputación de la comunidad.
                  </p>
                </>
              )}
            </Section>
          )}
        </main>

        {/* Barra Lateral de Acciones y Coordenadas */}
        <aside>
          <Section title="Acciones y Estado del Servicio">
            <div className="vertical-actions">
              {isCustomer && c.delivery_mode === "onsite" && active && (
                <>
                  <RunButton
                    confirm="¿Compartir con este profesional tu dirección y punto de llegada para este servicio?"
                    run={() =>
                      api("share_location", { contract: id, hours: 12 })
                    }
                    after={refresh}
                  >
                    <MapPin size={17} /> Compartir ubicación
                  </RunButton>
                  <RunButton
                    run={() => api("revoke_location", { contract: id })}
                    after={refresh}
                  >
                    Dejar de compartir ubicación
                  </RunButton>
                </>
              )}

              {isProfessional &&
                active &&
                ((c.delivery_mode === "remote" && c.status === "accepted") ||
                  (c.delivery_mode === "onsite" &&
                    ["accepted", "en_route", "arrived"].includes(
                      c.status,
                    ))) && (
                  <RunButton
                    className="button primary"
                    run={() =>
                      api("advance_contract", {
                        contract: id,
                        action:
                          c.delivery_mode === "remote"
                            ? "start"
                            : c.status === "accepted"
                              ? "depart"
                              : c.status === "en_route"
                                ? "arrive"
                                : "start",
                      })
                    }
                    after={refresh}
                  >
                    {c.delivery_mode === "remote"
                      ? "Iniciar actividad del proyecto"
                      : c.status === "accepted"
                        ? "Voy en camino al lugar"
                        : c.status === "en_route"
                          ? "Marcar llegada al sitio"
                          : "Iniciar actividad"}
                  </RunButton>
                )}

              {!protectedPayment &&
                ["in_progress", "awaiting_confirmation"].includes(c.status) && (
                  <RunButton
                    className="button primary"
                    confirm="¿Confirmar que el servicio se completó y se realizó el pago en persona acordado?"
                    run={() =>
                      api("advance_contract", {
                        contract: id,
                        action: "finish",
                      })
                    }
                    after={refresh}
                  >
                    Confirmar finalización y pago en persona
                  </RunButton>
                )}

              {active && (
                <Action
                  title="Reportar un problema"
                  fields={[reasonField]}
                  action={(v) =>
                    api("dispute_contract", { contract: id, ...v })
                  }
                  after={refresh}
                />
              )}

              {active && !protectedPayment && (
                <Action
                  title="Cancelar servicio"
                  fields={[reasonField]}
                  action={(v) => api("cancel_contract", { contract: id, ...v })}
                  after={refresh}
                />
              )}

              {protectedPayment &&
                ["funded", "disputed"].includes(e?.status) && (
                  <Action
                    title="Solicitar devolución"
                    fields={[reasonField]}
                    action={(v) =>
                      api("request_escrow_refund", { contract: id, ...v })
                    }
                    after={refresh}
                  />
                )}

              {protectedPayment &&
                isProfessional &&
                e?.refund_requested_at &&
                ["funded", "disputed"].includes(e?.status) && (
                  <RunButton
                    confirm="¿Aprobar la devolución de los fondos en custodia a la billetera del cliente?"
                    run={() => api("approve_escrow_refund", { contract: id })}
                    after={refresh}
                  >
                    Aprobar devolución
                  </RunButton>
                )}
            </div>
          </Section>

          {c.delivery_mode === "onsite" && active && (
            <Section title="Ubicación y Trayecto Presencial">
              <ServiceMap contract={id} professional={isProfessional} />
            </Section>
          )}

          {!protectedPayment && (
            <Section title="Registro de Pagos en Persona">
              <ExternalPayments contract={id} />
              <Action
                title="Registrar pago realizado"
                fields={[
                  {
                    name: "amount",
                    label: "Importe pagado",
                    type: "number",
                    min: 1,
                  },
                  {
                    name: "method",
                    label: "Medio de pago (Efectivo, Pago Móvil, etc.)",
                  },
                ]}
                action={(v) =>
                  api("report_external_payment", {
                    contract: id,
                    order: null,
                    amount: Math.round(v.amount * 100),
                    currency: c.currency,
                    method: v.method,
                  })
                }
                after={refresh}
              />
              <p className="muted small">
                El pago se cancela directamente entre ustedes. No hay comisiones
                administrativas.
              </p>
            </Section>
          )}

          <Section title="Historial Oficial del Contrato">
            <div className="timeline">
              {c.events?.map((ev: Data) => (
                <div key={ev.id}>
                  <span className="timeline-dot" />
                  <p>
                    {ev.to_status ? (
                      <Badge value={ev.to_status} />
                    ) : (
                      ev.event_type.replaceAll("_", " ")
                    )}
                  </p>
                  <small>{date(ev.created_at)}</small>
                </div>
              ))}
            </div>
          </Section>
        </aside>
      </div>
      {quote && (
        <Modal
          title="Reservar el pago del servicio"
          onClose={() => setQuote(null)}
        >
          <p>{quote.requirements}</p>
          <div className="quote-row">
            <span>Total a reservar</span>
            <strong>{money(quote.price_minor, quote.currency)}</strong>
          </div>
          <div className="quote-row">
            <span>Comisión de servicio</span>
            <strong>{money(0, quote.currency)}</strong>
          </div>
          <p className="info-note">
            Este importe dejará de estar disponible en tu wallet. Solo se libera
            cuando tú valides la entrega.
          </p>
          <RunButton
            className="button primary"
            run={() => api("fund_contract", { quote: quote.id, key: newKey() })}
            after={() => {
              setQuote(null);
              refresh();
            }}
          >
            Confirmar reserva
          </RunButton>
        </Modal>
      )}
    </>
  );
}
