import { useMemo, useState, type ComponentType } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  Bell,
  BriefcaseBusiness,
  CheckCircle2,
  CheckCheck,
  ChevronRight,
  Circle,
  Coins,
  Compass,
  FileText,
  Hand,
  MapPin,
  MessageSquare,
  PackageCheck,
  Search,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  Star,
  UserRoundCheck,
  Wallet,
  type LucideProps,
} from "lucide-react";
import { api, money, date, type Data } from "../api";
import { useAuth } from "../context";
import { useData, Loading, Empty, Badge, ErrorBox, label } from "../components";

type Icon = ComponentType<LucideProps>;

const greeting = () => {
  const hour = Number(
    new Date().toLocaleString("en-US", {
      hour: "numeric",
      hour12: false,
      timeZone: "America/Caracas",
    }),
  );
  return hour < 12
    ? "Buenos días"
    : hour < 19
      ? "Buenas tardes"
      : "Buenas noches";
};

const relative = (value: string) => {
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60000);
  if (minutes < 1) return "Ahora";
  if (minutes < 60) return `Hace ${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `Hace ${hours} h`;
  const days = Math.round(hours / 24);
  return days < 7 ? `Hace ${days} ${days === 1 ? "día" : "días"}` : date(value);
};

// Cada tema de notificación con un texto claro, un icono y el lugar al que lleva.
const NOTIFICATION: Record<
  string,
  { title: string; icon: Icon; tone: string; to: (id: string) => string }
> = {
  "delivery.submitted": {
    title: "Tienes una entrega por revisar",
    icon: PackageCheck,
    tone: "violet",
    to: (id) => `/contracts/${id}`,
  },
  "review.requested": {
    title: "Ya puedes calificar tu servicio",
    icon: Star,
    tone: "amber",
    to: (id) => `/contracts/${id}`,
  },
  "proposal.received": {
    title: "Recibiste una nueva propuesta",
    icon: Hand,
    tone: "mint",
    to: (id) => `/requests/${id}`,
  },
  "contract.accepted": {
    title: "Tu servicio fue confirmado",
    icon: CheckCircle2,
    tone: "mint",
    to: (id) => `/contracts/${id}`,
  },
  "contract.funded": {
    title: "El pago quedó protegido en custodia",
    icon: ShieldCheck,
    tone: "violet",
    to: (id) => `/contracts/${id}`,
  },
  "request.available": {
    title: "Hay una solicitud nueva en tu zona",
    icon: Search,
    tone: "cyan",
    to: () => "/opportunities",
  },
  "chat.message": {
    title: "Tienes un mensaje nuevo",
    icon: MessageSquare,
    tone: "cyan",
    to: () => "/contracts",
  },
};
const notificationInfo = (topic: string) =>
  NOTIFICATION[topic] ?? {
    title: "Hay novedades en tu cuenta",
    icon: Bell,
    tone: "violet",
    to: () => "/dashboard",
  };

// Progreso visible de un servicio según su estado.
const STEPS = ["accepted", "in_progress", "awaiting_confirmation", "completed"];
const stepOf = (status: string) =>
  ["en_route", "arrived"].includes(status)
    ? 1
    : Math.max(STEPS.indexOf(status), 0);

type Task = {
  key: string;
  icon: Icon;
  tone: string;
  title: string;
  text: string;
  to: string;
  cta: string;
};

function tasksFrom(contracts: Data[], notifications: Data[]): Task[] {
  const tasks: Task[] = [];
  for (const c of contracts) {
    const other = c.is_customer ? c.professional_name : c.customer_name;
    if (c.status === "disputed")
      tasks.push({
        key: `d-${c.id}`,
        icon: ShieldAlert,
        tone: "coral",
        title: "Hay una disputa abierta",
        text: `${c.title} · con ${other}`,
        to: `/contracts/${c.id}`,
        cta: "Revisar caso",
      });
    else if (
      c.status === "awaiting_confirmation" &&
      ((c.is_customer && !c.customer_finished_at) ||
        (c.is_professional && !c.professional_finished_at))
    )
      tasks.push({
        key: `f-${c.id}`,
        icon: CheckCheck,
        tone: "violet",
        title: "Confirma que el servicio terminó",
        text: `${c.title} · ${other} ya lo marcó como finalizado`,
        to: `/contracts/${c.id}`,
        cta: "Confirmar",
      });
    else if (c.status === "accepted" && c.is_professional)
      tasks.push({
        key: `s-${c.id}`,
        icon: BriefcaseBusiness,
        tone: "mint",
        title: "Tienes un servicio por iniciar",
        text: `${c.title} · para ${other}`,
        to: `/contracts/${c.id}`,
        cta: "Ver detalles",
      });
    else if (c.status === "completed" && !c.reviews_count)
      tasks.push({
        key: `r-${c.id}`,
        icon: Star,
        tone: "amber",
        title: "Califica tu experiencia",
        text: `${c.title} · tu opinión ayuda a la comunidad`,
        to: `/contracts/${c.id}`,
        cta: "Calificar",
      });
  }
  for (const n of notifications)
    if (
      !n.read_at &&
      n.topic === "delivery.submitted" &&
      !tasks.some((t) => t.to === `/contracts/${n.aggregate_id}`)
    )
      tasks.push({
        key: `n-${n.id}`,
        icon: PackageCheck,
        tone: "violet",
        title: "Revisa la entrega recibida",
        text: "Aprueba el trabajo o solicita cambios",
        to: `/contracts/${n.aggregate_id}`,
        cta: "Revisar",
      });
  return tasks.slice(0, 5);
}

export default function Dashboard() {
  const { user } = useAuth();
  const isPro = !!user?.roles.includes("professional");
  const stats = useData(() => api("dashboard"), []),
    contracts = useData(() => api<Data[]>("my_contracts"), []),
    credits = useData(() => api("my_wallet"), []),
    requests = useData(() => api<Data[]>("my_requests"), []),
    ves = useData(() => api("money_wallet", { currency: "VES" }), []),
    usd = useData(() => api("money_wallet", { currency: "USD" }), []),
    kyc = useData(() => api<Data[]>("my_kyc"), []),
    account = useData(() => api("my_account"), []),
    notifications = useData(() => api<Data[]>("my_notifications"), [], 15000);
  const [filter, setFilter] = useState<"active" | "completed" | "all">(
    "active",
  );

  const allContracts = contracts.data ?? [];
  const shownContracts = allContracts.filter((c) =>
    filter === "all"
      ? true
      : filter === "completed"
        ? c.status === "completed"
        : !["completed", "cancelled"].includes(c.status),
  );
  const unread = (notifications.data ?? []).filter((n) => !n.read_at);
  const tasks = useMemo(
    () => tasksFrom(allContracts, notifications.data ?? []),
    [contracts.data, notifications.data],
  );

  const kycApproved = (kyc.data ?? []).some((k) => k.status === "approved");
  const kycPending = (kyc.data ?? []).some((k) =>
    ["pending", "in_review"].includes(k.status),
  );
  const setup = [
    {
      done: !!user?.email_verified,
      title: "Confirma tu correo",
      text: "Protege tu cuenta y recibe avisos.",
      to: "/profile?tab=security",
      icon: UserRoundCheck,
    },
    {
      done: kycApproved,
      title: kycPending ? "Identidad en revisión" : "Verifica tu identidad",
      text: kycPending
        ? "Te avisaremos al aprobarla."
        : "Necesaria para cobrar y retirar.",
      to: "/profile?tab=verification",
      icon: ShieldCheck,
    },
    {
      done: !!account.data?.addresses?.length,
      title: "Guarda una dirección",
      text: "Encuentra profesionales cerca de ti.",
      to: "/profile",
      icon: MapPin,
    },
    {
      done: isPro,
      title: "Activa tu perfil profesional",
      text: "Ofrece tus servicios y recibe 3 Vinkos.",
      to: "/profile?tab=professional",
      icon: Sparkles,
    },
  ];
  const setupDone = setup.filter((s) => s.done).length;
  const setupReady = !kyc.loading && !account.loading;

  const markAll = async () => {
    await Promise.all(
      unread.map((n) => api("read_notification", { notification: n.id })),
    );
    notifications.reload();
    stats.reload();
  };

  const firstName = user?.name.split(" ")[0] ?? "";
  const kpis: {
    icon: Icon;
    title: string;
    value: string;
    sub: string;
    to: string;
    tone: string;
    loading: boolean;
  }[] = [
    {
      icon: FileText,
      title: "Solicitudes abiertas",
      value: String(stats.data?.requests ?? 0),
      sub: requests.data?.length
        ? `${requests.data.length} en total`
        : "Publica lo que necesitas",
      to: "/requests",
      tone: "violet",
      loading: stats.loading,
    },
    {
      icon: BriefcaseBusiness,
      title: "Servicios en curso",
      value: String(stats.data?.active_contracts ?? 0),
      sub: `${stats.data?.completed_contracts ?? 0} finalizados`,
      to: "/contracts",
      tone: "mint",
      loading: stats.loading,
    },
    {
      icon: Wallet,
      title: "Saldo disponible",
      value: money(ves.data?.available_minor ?? 0, "VES"),
      sub: `${money(usd.data?.available_minor ?? 0, "USD")} · ${money(ves.data?.reserved_for_services_minor ?? 0, "VES")} en custodia`,
      to: "/wallet",
      tone: "cyan",
      loading: ves.loading,
    },
    {
      icon: Coins,
      title: "Vinkos",
      value: String(credits.data?.balance ?? credits.data?.credits ?? 0),
      sub: isPro ? "1 Vinko por contratación" : "Para recibir clientes",
      to: "/wallet",
      tone: "amber",
      loading: credits.loading,
    },
  ];

  return (
    <div className="dash">
      <header className="dash-hero">
        <div>
          <div className="eyebrow">TU ESPACIO SOYVINKO</div>
          <h1>
            {greeting()}, {firstName}.
          </h1>
          <p>
            {tasks.length
              ? `Tienes ${tasks.length} ${tasks.length === 1 ? "tarea pendiente" : "tareas pendientes"} y ${stats.data?.active_contracts ?? 0} servicios en curso.`
              : "Todo está al día. ¿Qué quieres resolver hoy?"}
          </p>
        </div>
        <div className="dash-hero-actions">
          <Link to="/explore" className="button secondary">
            <Compass size={17} /> Explorar
          </Link>
          <Link to="/requests/new" className="button primary">
            Nueva solicitud <ArrowRight size={17} />
          </Link>
        </div>
      </header>

      {setupReady && setupDone < setup.length && (
        <section className="dash-setup" aria-label="Completa tu cuenta">
          <div className="dash-setup-head">
            <div>
              <h2>Completa tu cuenta</h2>
              <p>
                {setupDone} de {setup.length} pasos listos
              </p>
            </div>
            <div
              className="dash-progress"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={setup.length}
              aria-valuenow={setupDone}
            >
              <span style={{ width: `${(setupDone / setup.length) * 100}%` }} />
            </div>
          </div>
          <ol className="dash-setup-steps">
            {setup.map((s) => (
              <li key={s.title} className={s.done ? "done" : ""}>
                {s.done ? (
                  <div className="dash-step">
                    <CheckCircle2 size={20} className="dash-step-check" />
                    <div>
                      <strong>{s.title}</strong>
                      <span>Listo</span>
                    </div>
                  </div>
                ) : (
                  <Link to={s.to} className="dash-step">
                    <s.icon size={20} />
                    <div>
                      <strong>{s.title}</strong>
                      <span>{s.text}</span>
                    </div>
                    <ChevronRight size={16} className="dash-step-go" />
                  </Link>
                )}
              </li>
            ))}
          </ol>
        </section>
      )}

      <div className="dash-kpis">
        {kpis.map((k) => (
          <Link to={k.to} className={`dash-kpi tone-${k.tone}`} key={k.title}>
            <span className="dash-kpi-icon">
              <k.icon size={20} />
            </span>
            <small>{k.title}</small>
            {k.loading ? (
              <span className="dash-skeleton" />
            ) : (
              <strong>{k.value}</strong>
            )}
            <span className="dash-kpi-sub">{k.sub}</span>
            <ArrowUpRight size={16} className="dash-kpi-go" />
          </Link>
        ))}
      </div>
      {stats.error && <ErrorBox message={stats.error} />}

      {tasks.length > 0 && (
        <section className="dash-panel dash-tasks">
          <div className="dash-panel-head">
            <h2>
              Requiere tu atención{" "}
              <span className="dash-count">{tasks.length}</span>
            </h2>
          </div>
          <ul>
            {tasks.map((t) => (
              <li key={t.key}>
                <Link to={t.to} className="dash-task">
                  <span className={`dash-task-icon tone-${t.tone}`}>
                    <t.icon size={18} />
                  </span>
                  <div>
                    <strong>{t.title}</strong>
                    <span>{t.text}</span>
                  </div>
                  <span className="dash-task-cta">
                    {t.cta} <ArrowRight size={14} />
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="dash-grid">
        <div className="dash-side">
          <section className="dash-panel">
            <div className="dash-panel-head">
              <h2>Tus servicios</h2>
              <div
                className="dash-tabs"
                role="tablist"
                aria-label="Filtrar servicios"
              >
                {(
                  [
                    ["active", "Activos"],
                    ["completed", "Finalizados"],
                    ["all", "Todos"],
                  ] as const
                ).map(([value, text]) => (
                  <button
                    key={value}
                    role="tab"
                    aria-selected={filter === value}
                    className={filter === value ? "active" : ""}
                    onClick={() => setFilter(value)}
                  >
                    {text}
                  </button>
                ))}
              </div>
            </div>
            {contracts.loading ? (
              <Loading />
            ) : contracts.error ? (
              <ErrorBox message={contracts.error} />
            ) : shownContracts.length ? (
              <ul className="dash-services">
                {shownContracts.slice(0, 6).map((c) => {
                  const step = stepOf(c.status);
                  const other = c.is_customer
                    ? c.professional_name
                    : c.customer_name;
                  return (
                    <li key={c.id}>
                      <Link to={`/contracts/${c.id}`} className="dash-service">
                        <span className="dash-avatar" aria-hidden="true">
                          {(other ?? "?").trim().charAt(0).toUpperCase()}
                        </span>
                        <div className="dash-service-main">
                          <div className="dash-service-top">
                            <strong>{c.title ?? "Servicio contratado"}</strong>
                            <Badge value={c.status} />
                          </div>
                          <span className="dash-service-meta">
                            {c.is_customer ? "Profesional" : "Cliente"}:{" "}
                            {other ?? "—"} · {label(c.delivery_mode)}
                            {c.payment_protection === "guaranteed" && (
                              <>
                                {" "}
                                · <ShieldCheck size={13} /> Protegido
                              </>
                            )}
                          </span>
                          {!["cancelled", "disputed"].includes(c.status) && (
                            <div
                              className="dash-steps"
                              aria-label={`Progreso: ${label(c.status)}`}
                            >
                              {STEPS.map((s, i) => (
                                <span
                                  key={s}
                                  className={i <= step ? "on" : ""}
                                />
                              ))}
                            </div>
                          )}
                        </div>
                        <div className="dash-service-side">
                          <strong>
                            {money(c.agreed_price_minor, c.currency)}
                          </strong>
                          <span>{date(c.accepted_at)}</span>
                        </div>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <Empty
                title={
                  filter === "completed"
                    ? "Aún no tienes servicios finalizados"
                    : "Tu próximo proyecto empieza aquí"
                }
                text="Explora profesionales o publica lo que necesitas resolver."
              >
                <Link className="button secondary" to="/explore">
                  Encontrar un profesional
                </Link>
              </Empty>
            )}
            {allContracts.length > 6 && (
              <Link className="dash-more" to="/contracts">
                Ver todos tus servicios <ArrowRight size={15} />
              </Link>
            )}
          </section>
          <section className="dash-panel">
            <div className="dash-panel-head">
              <h2>Tus solicitudes</h2>
              <Link className="link-button" to="/requests">
                Ver todas <ArrowRight size={15} />
              </Link>
            </div>
            {requests.data?.length ? (
              <ul className="dash-requests">
                {requests.data.slice(0, 3).map((r) => (
                  <li key={r.id}>
                    <Link to={`/requests/${r.id}`}>
                      <div>
                        <strong>{r.title}</strong>
                        <span>
                          {r.status === "open" && r.expires_at
                            ? `Vence ${date(r.expires_at)}`
                            : date(r.created_at)}
                        </span>
                      </div>
                      <Badge value={r.status} />
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="dash-muted">
                Aún no publicas solicitudes.{" "}
                <Link to="/requests/new">Publica la primera</Link> y recibe
                propuestas.
              </p>
            )}
          </section>
        </div>

        <div className="dash-side">
          <section className="dash-panel">
            <div className="dash-panel-head">
              <h2>
                Novedades{" "}
                {unread.length > 0 && (
                  <span className="dash-count">{unread.length}</span>
                )}
              </h2>
              {unread.length > 0 && (
                <button className="link-button" onClick={markAll}>
                  Marcar todo como leído
                </button>
              )}
            </div>
            {notifications.loading && !notifications.data ? (
              <Loading />
            ) : notifications.data?.length ? (
              <ul className="dash-feed">
                {notifications.data.slice(0, 6).map((n) => {
                  const info = notificationInfo(n.topic);
                  return (
                    <li key={n.id} className={n.read_at ? "" : "unread"}>
                      <Link
                        to={info.to(n.aggregate_id)}
                        className="dash-feed-item"
                        onClick={() => {
                          if (!n.read_at)
                            void api("read_notification", {
                              notification: n.id,
                            }).then(notifications.reload);
                        }}
                      >
                        <span className={`dash-task-icon tone-${info.tone}`}>
                          <info.icon size={16} />
                        </span>
                        <div>
                          <strong>{info.title}</strong>
                          <span>{relative(n.created_at)}</span>
                        </div>
                        {!n.read_at && (
                          <Circle
                            size={8}
                            className="dash-unread-dot"
                            aria-label="Sin leer"
                          />
                        )}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <Empty
                title="Estás al día"
                text="Las novedades de tus servicios aparecerán aquí."
              />
            )}
          </section>
        </div>
      </div>

      <div className="dash-banner">
        <span className="dash-banner-icon">
          <Sparkles size={22} />
        </span>
        <div>
          <h2>
            {isPro
              ? "Tu próximo cliente te está buscando."
              : "Lo que sabes hacer también vale."}
          </h2>
          <p>
            {isPro
              ? `Tienes ${credits.data?.balance ?? stats.data?.credit_balance ?? 0} Vinkos para nuevas contrataciones. Revisa las solicitudes de tu zona.`
              : "Activa tu perfil profesional, ofrece tus servicios y recibe 3 Vinkos de bienvenida."}
          </p>
        </div>
        <Link
          to={isPro ? "/opportunities" : "/profile?tab=professional"}
          className="button dark"
        >
          {isPro ? "Ver oportunidades" : "Activar mi perfil"}{" "}
          <ArrowRight size={16} />
        </Link>
      </div>
    </div>
  );
}
