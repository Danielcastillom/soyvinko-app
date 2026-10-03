import { useRef, useState } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowRight,
  Plus,
  MapPin,
  Globe,
  ShieldCheck,
  Paperclip,
} from "lucide-react";
import { api, newKey, expiry, money, date, request, type Data } from "../api";
import { useAuth, useToast } from "../context";
import { AddAddressButton } from "../AddressWizard";
import { RequestPhotoInput, RequestImages } from "../RequestPhotos";
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
  currencyField,
  reasonField,
  ErrorBox,
  ProfessionalCard,
  type FieldSpec,
} from "../components";
export function Requests() {
  const rows = useData(() => api<Data[]>("my_requests"), [], 10000);
  return (
    <>
      <PageTitle
        eyebrow="PANEL DE CLIENTE"
        title="Tus solicitudes"
        description="Tus necesidades y propuestas recibidas. Cada solicitud se cierra al asignar un profesional."
      >
        <Link className="button primary" to="/requests/new">
          <Plus size={17} /> Nueva solicitud
        </Link>
      </PageTitle>
      {rows.loading ? (
        <Loading />
      ) : rows.error ? (
        <ErrorBox message={rows.error} retry={rows.reload} />
      ) : rows.data?.length ? (
        <div className="request-grid">
          {rows.data.map((r) => (
            <Link
              key={r.id}
              to={`/requests/${r.id}`}
              className="panel request-card"
            >
              <div className="row between">
                <Badge
                  value={
                    r.moderation_status === "rejected" ? "rejected" : r.status
                  }
                />
                <span className="small muted">
                  {r.delivery_mode === "remote" ? (
                    <Globe size={18} />
                  ) : (
                    <MapPin size={18} />
                  )}
                </span>
              </div>
              <h2>{r.title}</h2>
              <p className="clamp-2">{r.description}</p>
              <div className="row between">
                <strong>
                  {r.budget_max_minor == null
                    ? `Por cotizar en ${r.currency === "VES" ? "Bs" : "USD"}`
                    : money(r.budget_max_minor, r.currency)}
                </strong>
                <ArrowRight size={20} />
              </div>
              <small className="muted">
                {date(r.created_at)} ·{" "}
                {r.mode === "direct"
                  ? "Invitación directa"
                  : "Convocatoria abierta"}
              </small>
            </Link>
          ))}
        </div>
      ) : (
        <Empty
          title="De una necesidad a una solución"
          text="Publica tu primera solicitud y encuentra a la persona indicada."
        >
          <Link className="button primary" to="/requests/new">
            Crear solicitud
          </Link>
        </Empty>
      )}
    </>
  );
}
export function NewRequest() {
  const [params] = useSearchParams(),
    navigate = useNavigate();
  const target = params.get("professional"),
    offeringId = params.get("offering");
  const direct = Boolean(target || offeringId);
  const [openMode, setOpenMode] = useState(
      params.get("mode") === "remote" ? "remote" : "onsite",
    ),
    [protect, setProtect] = useState(false),
    [photos, setPhotos] = useState<File[]>([]),
    [saving, setSaving] = useState(false),
    [submission] = useState(() => ({ key: newKey(), expires_at: expiry() }));
  const uploaded = useRef(new Map<File, string>());
  const cats = useData(() => api<Data[]>("catalog"), []),
    account = useData(() => api("my_account"), []),
    profile = useData(
      () =>
        target
          ? api("professional_profile", { professional: target })
          : Promise.resolve(null),
      [target],
    );
  const choices = (profile.data?.offerings ?? []).filter((o: Data) =>
    offeringId
      ? o.id === offeringId
      : (!params.get("category") || o.category_id === params.get("category")) &&
        (!params.get("mode") || o.delivery_mode === params.get("mode")),
  );
  const service = choices.length === 1 ? choices[0] : null;
  const mode = direct ? service?.delivery_mode : openMode;
  if (cats.loading || (direct && profile.loading)) return <Loading />;
  if (cats.error) return <ErrorBox message={cats.error} retry={cats.reload} />;
  if (direct && profile.error)
    return <ErrorBox message={profile.error} retry={profile.reload} />;
  if (direct && !service)
    return (
      <Empty
        title="Selecciona el servicio que deseas contratar"
        text="Abre el perfil y elige un servicio disponible para conservar sus datos."
      >
        <Link
          className="button primary"
          to={target ? `/professionals/${target}` : "/explore"}
        >
          Ver servicios
        </Link>
      </Empty>
    );
  const fields: FieldSpec[] = [
    ...(!direct
      ? [
          {
            name: "category",
            label: "Categoría",
            value: params.get("category"),
            options: (cats.data ?? []).map((c) => ({
              value: c.id,
              label: c.name,
            })),
          },
          {
            name: "title",
            label: "Título de tu solicitud",
            minLength: 5,
            maxLength: 160,
            hint: "Ejemplo: necesito reparar una fuga en la cocina.",
          },
        ]
      : []),
    {
      name: "description",
      label: direct
        ? "Explica la falla o lo que necesitas"
        : mode === "remote"
          ? "Requisitos y resultado esperado"
          : "Describe lo que necesitas",
      type: "textarea",
      minLength: 10,
      maxLength: 10000,
      hint: "Describe el problema y el resultado que esperas. Añade fotos debajo si te ayudan a explicarlo.",
    },
    {
      ...currencyField,
      label: "Moneda de pago",
      value: direct
        ? service.currency
        : ["VES", "USD"].includes(params.get("currency") ?? "")
          ? params.get("currency")
          : "VES",
      hint: "El profesional cotizará en la moneda elegida. Confirmarás el importe antes de asignar el trabajo.",
    },
    ...(!direct
      ? [
          {
            name: "amount",
            label:
              mode === "remote" ? "Presupuesto ofrecido" : "Presupuesto máximo",
            type: "number",
            min: 0.01,
            required: false,
            value: params.get("amount") ?? "",
            hint: "Opcional. Déjalo vacío si prefieres recibir cotizaciones.",
          },
        ]
      : []),
    ...(mode === "onsite"
      ? [
          {
            name: "address",
            label: "Dirección del servicio",
            options: (account.data?.addresses ?? [])
              .filter((a: Data) => a.country_code === "VE")
              .map((a: Data) => ({
                value: a.id,
                label: `${a.label} · ${a.parish_name ?? a.city}`,
              })),
          },
          ...(!direct
            ? [
                {
                  name: "radius_m",
                  label: "Radio de búsqueda",
                  value: "25000",
                  options: [
                    { value: "5000", label: "5 km" },
                    { value: "10000", label: "10 km" },
                    { value: "25000", label: "25 km" },
                    { value: "50000", label: "50 km" },
                  ],
                },
              ]
            : []),
        ]
      : []),
  ];
  return (
    <>
      <Link
        to={direct ? `/professionals/${target}` : "/requests"}
        className="back-link"
      >
        ← {direct ? "Volver al servicio" : "Mis solicitudes"}
      </Link>
      <PageTitle
        eyebrow={direct ? "SOLICITAR UN SERVICIO" : "PUBLICAR UNA NECESIDAD"}
        title={direct ? "Cuéntale qué necesitas" : "¿Qué necesitas resolver?"}
        description={
          direct
            ? "Los datos del servicio ya están seleccionados."
            : "Recibe propuestas de varios profesionales y elige quién te ayuda."
        }
      />
      <div className="two-col">
        <Section
          title={
            direct ? "Tu solicitud al profesional" : "Detalles de tu necesidad"
          }
        >
          {direct ? (
            <div className="selected-service-summary">
              <div className="row wrap">
                <Badge value={mode} />
                <span className="small">
                  {cats.data?.find((c) => c.id === service.category_id)?.name}
                </span>
              </div>
              <h3>{service.title}</h3>
              <p>{profile.data?.name}</p>
              <span className="small">
                Precio anunciado: desde{" "}
                <strong>
                  {money(service.price_min_minor, service.currency)}
                </strong>
              </span>
              <p className="field-hint">
                El importe final depende de lo que necesitas. No se convierte
                automáticamente entre Bs y USD.
              </p>
            </div>
          ) : (
            <div className="mode-cards">
              <button
                type="button"
                disabled={saving}
                className={mode === "onsite" ? "selected" : ""}
                onClick={() => setOpenMode("onsite")}
              >
                <MapPin />
                <strong>Presencial</strong>
                <span>Un profesional va al lugar</span>
              </button>
              <button
                type="button"
                disabled={saving}
                className={mode === "remote" ? "selected" : ""}
                onClick={() => setOpenMode("remote")}
              >
                <Globe />
                <strong>Remoto</strong>
                <span>Trabajan desde cualquier lugar</span>
              </button>
            </div>
          )}
          {mode === "onsite" && (
            <div className="row between" style={{ marginBottom: 20 }}>
              <p className="muted small">
                Elige o guarda el lugar donde necesitas el servicio.
              </p>
              <AddAddressButton after={account.reload} />
            </div>
          )}
          {account.error && mode === "onsite" && (
            <ErrorBox message={account.error} retry={account.reload} />
          )}
          <FieldsForm
            key={`${mode}-${service?.id ?? "open"}`}
            fields={fields}
            submit={direct ? "Enviar solicitud" : "Publicar solicitud"}
            onSubmit={async (values) => {
              setSaving(true);
              try {
                const files: string[] = [];
                for (const file of photos) {
                  let id = uploaded.current.get(file);
                  if (!id) {
                    const form = new FormData();
                    form.set("file", file);
                    form.set("purpose", "request");
                    id = (await request("/api/v1/files/upload", "POST", form))
                      .id as string;
                    uploaded.current.set(file, id);
                  }
                  files.push(id);
                }
                const common = {
                  ...submission,
                  description: values.description,
                  currency: values.currency,
                  address: mode === "onsite" ? values.address : null,
                  files,
                  protect: mode === "remote" || protect,
                };
                const id = direct
                  ? await api("request_offering", {
                      ...common,
                      offering: service.id,
                    })
                  : await api("create_customer_request", {
                      ...common,
                      category: values.category,
                      delivery_mode: mode,
                      title: values.title,
                      budget:
                        values.amount == null
                          ? null
                          : Math.round(values.amount * 100),
                      radius_m:
                        mode === "onsite" ? Number(values.radius_m) : null,
                    });
                navigate(`/requests/${id}`);
              } finally {
                setSaving(false);
              }
            }}
          >
            <RequestPhotoInput
              files={photos}
              onChange={setPhotos}
              disabled={saving}
            />
            {!direct && mode === "onsite" && (
              <label className="check-field">
                <input
                  type="checkbox"
                  checked={protect}
                  onChange={(e) => setProtect(e.target.checked)}
                />
                <span>Quiero trabajar con pago protegido</span>
              </label>
            )}
          </FieldsForm>
        </Section>
        <aside>
          <Section title="Tú eliges a quién contratar">
            <p className="info-note">
              <ShieldCheck size={20} />
              {mode === "onsite"
                ? "Tu dirección exacta solo se comparte cuando lo autorizas dentro del contrato."
                : "Este servicio es remoto: no necesitas dirección ni radio de búsqueda."}
            </p>
            <p>
              {direct
                ? "El profesional recibe tu solicitud y te envía una cotización en Bs o USD, según tu elección."
                : "Varios profesionales pueden enviarte propuestas para esta necesidad."}
            </p>
            <p>
              Cuando eliges una propuesta, la solicitud queda asignada y deja de
              recibir ofertas. El profesional puede seguir atendiendo a otros
              clientes.
            </p>
            {mode === "remote" && (
              <p>
                El pago remoto queda protegido hasta que revises y liberes la
                entrega.
              </p>
            )}
          </Section>
        </aside>
      </div>
    </>
  );
}
export function RequestDetail() {
  const { id } = useParams(),
    navigate = useNavigate(),
    toast = useToast();
  const data = useData(
      () =>
        api<Data[]>("my_requests").then((rows) =>
          rows.find((x) => x.id === id),
        ),
      [id],
      10000,
    ),
    proposals = useData(
      () => api<Data[]>("request_proposals", { request: id }),
      [id],
      10000,
    );
  const r = data.data;
  if (data.loading) return <Loading />;
  if (data.error) return <ErrorBox message={data.error} />;
  if (!r) return <Empty title="Solicitud no disponible" />;
  return (
    <>
      <Link className="back-link" to="/requests">
        ← Mis solicitudes
      </Link>
      <PageTitle
        title={r.title}
        description={`${r.delivery_mode === "remote" ? "Proyecto remoto" : "Servicio presencial"} · ${r.budget_max_minor == null ? `Cotización en ${r.currency === "VES" ? "Bs" : "USD"}` : money(r.budget_max_minor, r.currency)}`}
      >
        <Badge value={r.status} />
        <Badge value={r.moderation_status} />
      </PageTitle>
      {r.moderation_status === "rejected" && (
        <div className="error-box" role="status">
          La administración desautorizó esta solicitud. No está disponible para
          los profesionales.
        </div>
      )}
      {r.status === "open" && r.moderation_status === "approved" && (
        <div className="success-box" role="status">
          {r.mode === "direct"
            ? "Solicitud enviada al profesional. Esperando su cotización."
            : "Solicitud publicada. Esperando propuestas de los profesionales."}
        </div>
      )}
      {r.contract_id && (
        <div className="success-box row between wrap">
          <span>
            Profesional asignado. Esta solicitud ya no recibe propuestas.
          </span>
          <Link className="button primary" to={`/contracts/${r.contract_id}`}>
            Ver servicio contratado <ArrowRight size={16} />
          </Link>
        </div>
      )}
      <div className="two-col">
        <div>
          <Section title="Lo que necesitas">
            <p className="preserve">{r.description}</p>
            <RequestImages
              photos={r.attachments}
              requestId={r.status === "draft" ? id : undefined}
              after={data.reload}
            />
            <div className="row wrap">
              {r.status === "draft" && r.moderation_status !== "rejected" && (
                <RunButton
                  className="button primary"
                  run={() => api("publish_request", { request: id })}
                  after={data.reload}
                >
                  {r.mode === "direct"
                    ? "Enviar al profesional"
                    : "Publicar solicitud"}
                </RunButton>
              )}
              {["draft", "open"].includes(r.status) && (
                <Action
                  title="Cancelar solicitud"
                  fields={[reasonField]}
                  action={(v) => api("cancel_request", { request: id, ...v })}
                  after={data.reload}
                />
              )}
            </div>
            {r.status === "draft" && (
              <label className="upload-zone">
                <Paperclip size={20} />
                <span>Añadir una foto de referencia</span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={async (e) => {
                    const f = e.target.files?.[0];
                    if (!f) return;
                    try {
                      const form = new FormData();
                      form.set("file", f);
                      form.set("purpose", "request");
                      const uploaded = await request(
                        "/api/v1/files/upload",
                        "POST",
                        form,
                      );
                      await api("attach_request_file", {
                        request: id,
                        file: uploaded.id,
                      });
                      data.reload();
                      e.target.value = "";
                      toast("Foto adjunta.");
                    } catch (err) {
                      toast((err as Error).message, true);
                    }
                  }}
                />
              </label>
            )}
          </Section>
          <Section
            title={
              r.mode === "direct"
                ? "Cotización del profesional"
                : "Propuestas recibidas"
            }
          >
            {proposals.error ? (
              <ErrorBox message={proposals.error} />
            ) : proposals.data?.length ? (
              proposals.data.map((p) => (
                <div className="proposal-row" key={p.id}>
                  <div>
                    <Link to={`/professionals/${p.professional.id}`}>
                      <h3>{p.professional.name}</h3>
                    </Link>
                    <p>{p.message}</p>
                    <strong>{money(p.price_minor, p.currency)}</strong>{" "}
                    <Badge value={p.status} />
                  </div>
                  {p.status === "pending" &&
                    r.status === "open" &&
                    r.moderation_status === "approved" &&
                    Date.parse(p.expires_at) > Date.now() && (
                      <RunButton
                        className="button primary"
                        confirm={`¿Contratar a ${p.professional.name} por ${money(p.price_minor, p.currency)}? Se abrirá el chat entre ambos.`}
                        run={async () => {
                          const contract = await api("accept_proposal", {
                            proposal: p.id,
                          });
                          navigate(`/contracts/${contract}`);
                        }}
                      >
                        Elegir profesional
                      </RunButton>
                    )}
                </div>
              ))
            ) : (
              <Empty
                title={
                  r.contract_id
                    ? "La solicitud ya está asignada"
                    : "Aquí llegarán las propuestas"
                }
                text={
                  r.contract_id
                    ? "Puedes continuar en el servicio contratado."
                    : r.mode === "direct"
                      ? "El profesional enviará el precio en la moneda que elegiste. Tú decides si lo aceptas."
                      : "Podrás recibir varias propuestas y elegir una. Al asignar el trabajo, se cierran las demás ofertas."
                }
              />
            )}
          </Section>
        </div>
        <aside>
          <Section title="Detalles">
            <p>
              <Badge value={r.delivery_mode} />
            </p>
            <p>
              {r.mode === "direct"
                ? "Invitación a un profesional específico."
                : "Convocatoria para profesionales de la categoría."}
            </p>
            {!r.contract_id && ["draft", "open"].includes(r.status) && (
              <p>Disponible hasta {date(r.expires_at)}</p>
            )}
            {r.requires_platform_payment && (
              <p className="info-note">
                Pago protegido: el saldo se reserva después de contratar.
              </p>
            )}
          </Section>
        </aside>
      </div>
    </>
  );
}
export function Opportunities() {
  const [mode, setMode] = useState("onsite"),
    navigate = useNavigate();
  const { user } = useAuth();
  const data = useData(
      () => api<Data[]>("service_feed", { delivery_mode: mode }),
      [mode],
      10000,
    ),
    mine = useData(() => api<Data[]>("my_proposals"), [], 10000);
  if (!user?.roles.includes("professional"))
    return (
      <Empty
        title="Activa tu perfil profesional"
        text="Recibe 3 créditos de bienvenida y encuentra oportunidades."
      >
        <Link className="button primary" to="/profile?tab=professional">
          Activar perfil
        </Link>
      </Empty>
    );
  return (
    <>
      <PageTitle
        eyebrow="PANEL PROFESIONAL"
        title="Solicitudes de clientes"
        description="Puedes cotizar y atender varios trabajos. Tus servicios siguen disponibles para otros clientes."
      />
      <div className="segmented">
        {[
          ["onsite", "En tu zona"],
          ["remote", "Proyectos remotos"],
        ].map(([v, l]) => (
          <button
            key={v}
            className={v === mode ? "active" : ""}
            onClick={() => setMode(v)}
          >
            {l}
          </button>
        ))}
      </div>
      <div className="request-grid spaced">
        {data.loading ? (
          <Loading />
        ) : data.error ? (
          <ErrorBox message={data.error} />
        ) : data.data?.length ? (
          data.data.map((r) => (
            <Section title={r.title} key={r.request_id ?? r.id}>
              <p>{r.description}</p>
              <RequestImages photos={r.attachments} />
              <p>
                <strong>
                  {r.budget_max_minor == null && r.offered_price_minor == null
                    ? `Cotización en ${r.currency === "VES" ? "Bs" : "USD"}`
                    : money(
                        r.budget_max_minor ?? r.offered_price_minor,
                        r.currency,
                      )}
                </strong>{" "}
                {r.mode === "direct" ? (
                  <span className="badge">Invitación para ti</span>
                ) : (
                  <Badge value="open" />
                )}
              </p>
              {mine.data?.some(
                (p: Data) => p.request_id === (r.request_id ?? r.id),
              ) ? (
                <p className="info-note">
                  Ya enviaste una propuesta. Puedes consultar su estado en Mis
                  propuestas.
                </p>
              ) : r.mode === "direct" && !r.requires_quote ? (
                <RunButton
                  className="button primary"
                  confirm="Al aceptar se consume 1 crédito y se habilitan el chat y los datos de contacto."
                  run={async () => {
                    const id = await api("accept_direct_request", {
                      request: r.request_id ?? r.id,
                    });
                    navigate(`/contracts/${id}`);
                  }}
                >
                  Aceptar invitación · 1 crédito
                </RunButton>
              ) : (
                <Action
                  title={
                    r.mode === "direct"
                      ? "Enviar cotización"
                      : "Enviar propuesta"
                  }
                  fields={[
                    {
                      name: "message",
                      label: "Cuéntale cómo puedes ayudar",
                      type: "textarea",
                    },
                    {
                      name: "amount",
                      label: `Precio ofrecido en ${r.currency === "VES" ? "Bs" : "USD"}`,
                      type: "number",
                      min: 0.01,
                    },
                  ]}
                  action={(v) =>
                    api("submit_proposal", {
                      request: r.request_id ?? r.id,
                      message: v.message,
                      price_minor: Math.round(v.amount * 100),
                      expires_at: new Date(
                        Math.min(
                          Date.parse(expiry()),
                          Date.parse(r.expires_at),
                        ),
                      ).toISOString(),
                    })
                  }
                  after={() => {
                    data.reload();
                    mine.reload();
                  }}
                  description="El cliente confirmará tu precio antes de asignarte el trabajo. Enviar la propuesta no consume créditos; contratar consume 1 crédito."
                />
              )}
            </Section>
          ))
        ) : (
          <Empty
            title="No hay solicitudes disponibles por ahora"
            text="Revisa tus servicios y la cobertura de tus sedes para recibir oportunidades."
          />
        )}
      </div>
      <Section title="Mis propuestas">
        {mine.data?.length ? (
          mine.data.map((p) => (
            <div className="list-row" key={p.id}>
              <div>
                <h3>{p.title}</h3>
                <p>{p.message}</p>
                <Badge value={p.status} />
              </div>
              {p.status === "pending" && (
                <RunButton
                  run={() => api("withdraw_proposal", { proposal: p.id })}
                  after={mine.reload}
                >
                  Retirar propuesta
                </RunButton>
              )}
            </div>
          ))
        ) : (
          <Empty title="Todavía no has enviado propuestas" />
        )}
      </Section>
    </>
  );
}
