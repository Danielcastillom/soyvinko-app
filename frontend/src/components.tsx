import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import {
  LoaderCircle,
  Plus,
  X,
  Check,
  ArrowUpRight,
  ShieldCheck,
  MapPin,
  Star,
  Globe,
  BriefcaseBusiness,
} from "lucide-react";
import { Link } from "react-router-dom";
import { api, ApiError, money, type Data, type Operation } from "./api";
import { useToast } from "./context";
import { type ServiceImage } from "./serviceImages";
export function useData<T = any>(
  load: () => Promise<T>,
  deps: unknown[] = [],
  poll = 0,
) {
  const [data, setData] = useState<T | null>(null),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    const run = () =>
      load()
        .then((v) => {
          if (active) {
            setData(v);
            setError("");
          }
        })
        .catch((e) => {
          if (active) setError(e.message);
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    void run();
    const timer = poll
      ? setInterval(() => {
          if (!document.hidden) void run();
        }, poll)
      : undefined;
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [...deps, version]);
  return { data, error, loading, reload: () => setVersion((x) => x + 1) };
}
export function Loading() {
  return (
    <div className="loading">
      <LoaderCircle size={24} className="spin" /> Cargando…
    </div>
  );
}
export function Empty({
  title = "Todavía no hay elementos",
  text = "Cuando tengas actividad, la encontrarás aquí.",
  children,
}: {
  title?: string;
  text?: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      <BriefcaseBusiness size={32} />
      <h3>{title}</h3>
      <p>{text}</p>
      {children}
    </div>
  );
}
export function ErrorBox({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div className="error-box" role="alert">
      {message}
      {retry && (
        <button className="link-button" onClick={retry}>
          Reintentar
        </button>
      )}
    </div>
  );
}
export function PageTitle({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  children?: ReactNode;
}) {
  return (
    <div className="page-title">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      <div className="page-actions">{children}</div>
    </div>
  );
}
const statuses: Record<string, string> = {
  active: "Activo",
  inactive: "Inactivo",
  paused: "En pausa",
  draft: "Borrador",
  pending: "Pendiente",
  approved: "Aprobado",
  rejected: "Rechazado",
  in_review: "En revisión",
  review: "En revisión",
  failed: "Fallido",
  open: "Publicada",
  matched: "Contratado",
  accepted: "Aceptado",
  en_route: "En camino",
  arrived: "En el lugar",
  in_progress: "En curso",
  awaiting_confirmation: "Por confirmar",
  completed: "Finalizado",
  cancelled: "Cancelado",
  disputed: "En disputa",
  expired: "Vencido",
  withdrawn: "Retirada",
  published: "Publicada",
  hidden: "Oculta",
  funded: "Fondos reservados",
  unfunded: "Sin financiar",
  released: "Liberado",
  settled: "Confirmada",
  paid: "Pagado",
  processing: "Procesando",
  verified: "Verificado",
  disabled: "Desactivado",
  closed: "Cerrado",
  resolved: "Resuelto",
  delivered: "Entregado",
  dispatched: "Enviado",
  onsite: "Presencial",
  remote: "Remoto",
  guaranteed: "Pago protegido",
  external: "Pago directo",
  frozen: "Congelado",
};
export const label = (value: string) => statuses[value] ?? value;
export function Badge({ value }: { value: string }) {
  return <span className={`badge status-${value}`}>{label(value)}</span>;
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
    const el = ref.current;
    return () => el?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="modal-head">
        <h2>{title}</h2>
        <button className="icon-button" aria-label="Cerrar" onClick={onClose}>
          <X />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export type FieldSpec = {
  name: string;
  label: string;
  type?: string;
  required?: boolean;
  options?: { value: string; label: string }[];
  multiple?: boolean;
  value?: any;
  hint?: string;
  min?: number;
  max?: number;
  step?: string;
  minLength?: number;
  maxLength?: number;
  autoComplete?: string;
  normalize?: (value: string) => string;
};
export function FieldsForm({
  fields,
  submit,
  onSubmit,
  children,
}: {
  fields: FieldSpec[];
  submit: string;
  onSubmit: (values: Data) => Promise<unknown>;
  children?: ReactNode;
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const formRef = useRef<HTMLFormElement>(null);
  const formId = useId();
  async function handle(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setFieldErrors({});
    const fd = new FormData(e.currentTarget),
      result: Data = {};
    for (const field of fields) {
      const raw = fd.get(field.name);
      const v =
        typeof raw === "string" && field.normalize ? field.normalize(raw) : raw;
      if (field.multiple) {
        result[field.name] = fd.getAll(field.name).filter(Boolean);
        continue;
      }
      result[field.name] =
        field.type === "checkbox"
          ? v === "on"
          : field.type === "number"
            ? v === ""
              ? null
              : Number(v)
            : v === ""
              ? null
              : v;
    }
    try {
      await onSubmit(result);
    } catch (err) {
      if (err instanceof ApiError) {
        const next: Record<string, string> = {};
        for (const detail of err.details) {
          const name = detail.field.replace(/^body\./, "");
          if (fields.some((field) => field.name === name))
            next[name] = detail.message;
        }
        setFieldErrors(next);
        const first = fields.find((field) => next[field.name]);
        const input = first && formRef.current?.elements.namedItem(first.name);
        if (input instanceof HTMLElement) input.focus();
      }
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo completar la operación.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <form
      ref={formRef}
      onSubmit={handle}
      className="fields-form"
      onInvalid={(event) => {
        const input = event.target as HTMLInputElement;
        const field = fields.find((f) => f.name === input.name);
        if (!field) return;
        const message = input.validity.valueMissing
          ? "Completa este campo."
          : input.validity.tooShort
            ? `Escribe al menos ${field.minLength} caracteres.`
            : input.validity.tooLong
              ? `Usa como máximo ${input.maxLength} caracteres.`
              : input.validity.typeMismatch && field.type === "email"
                ? "Escribe un correo electrónico válido."
                : input.validationMessage;
        setFieldErrors((current) => ({ ...current, [field.name]: message }));
      }}
      onInput={(event) => {
        const name = (event.target as HTMLInputElement).name;
        setFieldErrors((current) => {
          const next = { ...current };
          delete next[name];
          return next;
        });
        setError("");
      }}
    >
      {fields.map((f) => {
        const description =
          [
            f.hint && `${formId}-${f.name}-hint`,
            fieldErrors[f.name] && `${formId}-${f.name}-error`,
          ]
            .filter(Boolean)
            .join(" ") || undefined;
        const accessibility = {
          "aria-invalid": fieldErrors[f.name] ? true : undefined,
          "aria-describedby": description,
        };
        return (
          <label
            key={f.name}
            className={f.type === "checkbox" ? "check-field" : ""}
          >
            {f.type !== "checkbox" && (
              <span>
                {f.label}
                {f.required !== false && <small> *</small>}
              </span>
            )}
            {f.options ? (
              <select
                {...accessibility}
                multiple={f.multiple}
                name={f.name}
                defaultValue={f.value ?? (f.multiple ? [] : "")}
                required={f.required !== false}
              >
                <option value="">Selecciona una opción</option>
                {f.options.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            ) : f.type === "textarea" ? (
              <textarea
                {...accessibility}
                name={f.name}
                defaultValue={f.value ?? ""}
                required={f.required !== false}
                rows={4}
                minLength={f.minLength}
                maxLength={f.maxLength ?? 10000}
              />
            ) : f.type === "checkbox" ? (
              <>
                <input
                  {...accessibility}
                  type="checkbox"
                  name={f.name}
                  defaultChecked={f.value ?? false}
                />
                <span>{f.label}</span>
              </>
            ) : (
              <input
                {...accessibility}
                name={f.name}
                type={f.type ?? "text"}
                defaultValue={f.value ?? ""}
                required={f.required !== false}
                min={f.min}
                max={f.max}
                step={
                  f.step ??
                  (f.type === "number"
                    ? f.name === "amount"
                      ? "0.01"
                      : "1"
                    : undefined)
                }
                minLength={f.minLength}
                maxLength={f.maxLength ?? (f.type === "password" ? 128 : 1000)}
                autoComplete={f.autoComplete}
                onBlur={(event) => {
                  if (f.normalize)
                    event.currentTarget.value = f.normalize(
                      event.currentTarget.value,
                    );
                }}
              />
            )}
            {f.hint && (
              <small id={`${formId}-${f.name}-hint`} className="field-hint">
                {f.hint}
              </small>
            )}
            {fieldErrors[f.name] && (
              <small
                id={`${formId}-${f.name}-error`}
                className="field-error"
                role="alert"
              >
                {fieldErrors[f.name]}
              </small>
            )}
          </label>
        );
      })}
      {children}
      {error && <ErrorBox message={error} />}
      <button className="button primary" disabled={busy}>
        {busy ? (
          <LoaderCircle size={18} className="spin" />
        ) : (
          <Check size={18} />
        )}{" "}
        {busy ? "Procesando…" : submit}
      </button>
    </form>
  );
}
export function Action({
  title,
  fields = [],
  action,
  after,
  className = "button secondary",
  children,
  description,
}: {
  title: string;
  fields?: FieldSpec[];
  action: (values: Data) => Promise<unknown>;
  after?: () => void;
  className?: string;
  children?: ReactNode;
  description?: string;
}) {
  const [open, setOpen] = useState(false);
  const toast = useToast();
  return (
    <>
      <button className={className} onClick={() => setOpen(true)}>
        {children ?? (
          <>
            <Plus size={16} />
            {title}
          </>
        )}
      </button>
      {open && (
        <Modal title={title} onClose={() => setOpen(false)}>
          {description && <p className="muted">{description}</p>}
          <FieldsForm
            fields={fields}
            submit={title}
            onSubmit={async (values) => {
              await action(values);
              toast("Cambios guardados.");
              setOpen(false);
              after?.();
            }}
          />
        </Modal>
      )}
    </>
  );
}
export function RunButton({
  children,
  run,
  after,
  className = "button secondary",
  confirm,
}: {
  children: ReactNode;
  run: () => Promise<unknown>;
  after?: () => void;
  className?: string;
  confirm?: string;
}) {
  const [busy, setBusy] = useState(false),
    [open, setOpen] = useState(false);
  const toast = useToast();
  const execute = async () => {
    setBusy(true);
    try {
      await run();
      toast("Operación completada.");
      setOpen(false);
      after?.();
    } catch (e) {
      toast((e as Error).message, true);
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <button
        disabled={busy}
        className={className}
        onClick={() => (confirm ? setOpen(true) : void execute())}
      >
        {busy ? <LoaderCircle size={16} className="spin" /> : children}
      </button>
      {open && (
        <Modal title="Confirmar operación" onClose={() => setOpen(false)}>
          <p>{confirm}</p>
          <button
            disabled={busy}
            className="button primary"
            onClick={() => void execute()}
          >
            Confirmar
          </button>
        </Modal>
      )}
    </>
  );
}
export function ProfessionalCardSkeleton() {
  return (
    <article className="professional-card skeleton-card" aria-hidden="true">
      <div className="card-cover skeleton-shimmer">
        <div className="avatar skeleton-shimmer" />
      </div>
      <div className="card-content">
        <div className="skeleton-line skeleton-title skeleton-shimmer" />
        <div className="skeleton-line skeleton-subtitle skeleton-shimmer" />
        <div className="skeleton-line skeleton-rating skeleton-shimmer" />
        <div className="skeleton-line skeleton-text skeleton-shimmer" />
        <div className="skeleton-line skeleton-meta skeleton-shimmer" />
        <div className="card-bottom">
          <div className="skeleton-line skeleton-price skeleton-shimmer" />
          <div className="round-link skeleton-shimmer" />
        </div>
      </div>
    </article>
  );
}

export function ProfessionalCard({
  profile,
  index = 0,
  image,
}: {
  profile: Data;
  index?: number;
  image?: ServiceImage;
}) {
  const [failedImage, setFailedImage] = useState<string | null>(null);
  const showImage = image && failedImage !== image.src;
  const offerings = profile.offerings ?? [];
  const initial =
    profile.name
      ?.split(" ")
      .slice(0, 2)
      .map((x: string) => x[0])
      .join("") ?? "P";
  return (
    <article className="professional-card">
      <div
        className={`card-cover cover-${index % 4}${showImage ? " has-service-image" : ""}`}
      >
        {showImage ? (
          <>
            <img
              className="service-cover-image"
              src={image.src}
              alt={image.alt}
              width={1280}
              height={720}
              loading="lazy"
              decoding="async"
              onError={() => setFailedImage(image.src)}
            />
            <span className="service-cover-shade" aria-hidden="true" />
            <span className="service-category-label">{image.label}</span>
          </>
        ) : (
          <span className="cover-shape" />
        )}
        <div className="avatar">{initial}</div>
        <span className="cover-specialty">
          {offerings[0]?.delivery_mode === "remote" ? (
            <Globe size={15} />
          ) : (
            <BriefcaseBusiness size={15} />
          )}{" "}
          {showImage
            ? offerings[0]?.delivery_mode === "remote"
              ? "Remoto"
              : "Presencial"
            : offerings[0]?.delivery_mode === "remote"
              ? "Talento sin fronteras"
              : "Cerca de ti"}
        </span>
      </div>
      <div className="card-content">
        <div className="row between card-header-row">
          <h3 title={profile.name}>{profile.name}</h3>
          {profile.verified && (
            <span className="verified-badge-chip" title="Profesional con documento de identidad verificado">
              <ShieldCheck size={16} /> Verificado
            </span>
          )}
        </div>
        <p className="specialty">
          {offerings[0]?.title ?? "Servicios profesionales"}
        </p>
        <div className="rating">
          <Star size={14} fill="currentColor" />
          <strong className="rating-score">{profile.rating ? Number(profile.rating).toFixed(1) : "Nuevo"}</strong>
          <span>
            {" "}
            {profile.review_count
              ? `(${profile.review_count} ${profile.review_count === 1 ? 'opinión' : 'opiniones'})`
              : "En SoyVinko"}
          </span>
        </div>
        <p className="bio clamp-2">{profile.bio}</p>
        <div className="row muted small card-location-row">
          <MapPin size={14} />
          <span>{profile.city ?? "Disponible en remoto"}</span>
          {profile.distance_m != null && (
            <span className="dist-pill">· {(profile.distance_m / 1000).toFixed(1)} km</span>
          )}
        </div>
        <div className="card-bottom">
          <div>
            <small>Presupuesto desde</small>
            <strong>
              {money(offerings[0]?.price_min_minor, offerings[0]?.currency)}
            </strong>
          </div>
          <Link
            className="round-link"
            aria-label={`Ver perfil de ${profile.name}`}
            to={`/professionals/${profile.id}`}
          >
            <ArrowUpRight size={20} />
          </Link>
        </div>
      </div>
    </article>
  );
}
export function Section({
  title,
  children,
  action,
}: {
  title: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <section className="panel">
      <div className="section-head">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
export const currencyField: FieldSpec = {
  name: "currency",
  label: "Moneda",
  value: "VES",
  options: [
    { value: "VES", label: "Bs · Bolívar" },
    { value: "USD", label: "USD · Dólar" },
  ],
};
export const reasonField: FieldSpec = {
  name: "reason",
  label: "Motivo",
  type: "textarea",
  hint: "Describe el motivo con al menos 10 caracteres.",
};

export function ExternalPayments({
  contract,
  order,
}: {
  contract?: string;
  order?: string;
}) {
  const { user } = useAuthForPayments();
  const data = useData(
    () => api<Data[]>("external_payment_reports", { contract, order }),
    [contract, order],
    5000,
  );
  if (data.error) return <ErrorBox message={data.error} />;
  return (
    <div>
      {data.data?.map((r) => (
        <div className="list-row" key={r.id}>
          <div>
            <strong>{money(r.amount_minor, r.currency)}</strong>
            <p>{r.method}</p>
            <Badge value={r.confirmed_at ? "settled" : "pending"} />
          </div>
          {!r.confirmed_at && r.reported_by !== user?.id && (
            <RunButton
              confirm="¿Confirmas que recibiste este pago por el importe y medio indicados?"
              run={() => api("confirm_external_payment", { report: r.id })}
              after={data.reload}
            >
              Confirmar recepción del pago
            </RunButton>
          )}
        </div>
      ))}
    </div>
  );
}
import { useAuth as useAuthForPayments } from "./context";
