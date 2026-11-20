import operations from "./operations.json";
export type Data = Record<string, any>;
export type User = {
  id: string;
  email: string;
  name: string;
  roles: string[];
  permissions: string[];
  email_verified: boolean;
  account_type?: "natural" | "juridico";
  business_name?: string | null;
  rif?: string | null;
  contact_name?: string | null;
};
export type FieldError = { field: string; message: string };
export class ApiError extends Error {
  constructor(
    message: string,
    public code: string,
    public status: number,
    public requestId?: string,
    public details: FieldError[] = [],
  ) {
    super(message);
  }
}
// nginx devuelve HTML en 502/504 y algunas respuestas no traen cuerpo: nunca romper por JSON inválido.
async function readJson(r: Response): Promise<Data> {
  if (!(r.headers.get("content-type") ?? "").includes("json")) return {};
  return r.json().catch(() => ({}));
}
async function send(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(path, init);
  } catch {
    throw new ApiError(
      "No hay conexión con el servidor. Revisa tu internet e intenta de nuevo.",
      "network",
      0,
    );
  }
}
const UNAVAILABLE = "El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.";
let accessToken: string | null = null;
let refreshPromise: Promise<Data> | null = null;
let onSession: ((data: Data | null) => void) | null = null;
export function observeSession(callback: (data: Data | null) => void) {
  onSession = callback;
}
export function setSession(data: Data | null) {
  accessToken = data?.access_token ?? null;
  onSession?.(data);
}
function csrf() {
  return (
    document.cookie
      .split("; ")
      .find((x) => x.startsWith("chamba_csrf="))
      ?.split("=")[1] ?? ""
  );
}
export async function refreshSession(): Promise<Data> {
  if (!refreshPromise)
    refreshPromise = (async () => {
      const r = await send("/api/v1/auth/refresh", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf() },
        body: JSON.stringify({ client: "web" }),
      });
      const body = await readJson(r);
      if (!r.ok) {
        // Solo cerramos sesión si el servidor la rechaza, no ante una caída temporal.
        if (r.status < 500) setSession(null);
        throw new ApiError(
          body.error?.message ?? "Inicia sesión",
          "session_expired",
          r.status,
        );
      }
      setSession(body.data);
      return body.data;
    })().finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}
export async function request<T = any>(
  path: string,
  method = "GET",
  body?: unknown,
  retry = true,
): Promise<T> {
  const headers: Record<string, string> = {};
  const requestToken = accessToken;
  if (requestToken) headers.Authorization = `Bearer ${requestToken}`;
  if (body !== undefined && !(body instanceof FormData))
    headers["Content-Type"] = "application/json";
  if (path.startsWith("/api/v1/auth/")) headers["X-CSRF-Token"] = csrf();
  const r = await send(path, {
    method,
    headers,
    credentials: "include",
    body:
      body === undefined
        ? undefined
        : body instanceof FormData
          ? body
          : JSON.stringify(body),
  });
  if (r.status === 401 && retry && accessToken && !path.includes("/auth/")) {
    if (accessToken === requestToken) await refreshSession();
    return request(path, method, body, false);
  }
  const content = await readJson(r);
  if (!r.ok)
    throw new ApiError(
      content.error?.message ??
        (r.status >= 500 ? UNAVAILABLE : "No se pudo completar la operación."),
      content.error?.code ?? "error",
      r.status,
      content.error?.request_id,
      Array.isArray(content.error?.details) ? content.error.details : [],
    );
  return content.data as T;
}
export function api<T = any>(
  name: keyof typeof operations,
  params: Data = {},
): Promise<T> {
  const op = operations[name];
  let path = op.path;
  if (op.method === "GET") {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== null && v !== undefined) query.set(k, String(v));
    });
    if (query.size) path += "?" + query;
    return request<T>(path);
  }
  return request<T>(path, "POST", params);
}
export async function privateFileBlob(id: string, retry = true): Promise<Blob> {
  const token = accessToken;
  const r = await fetch(`/api/v1/files/${id}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (r.status === 401 && retry && token) {
    if (accessToken === token) await refreshSession();
    return privateFileBlob(id, false);
  }
  if (!r.ok)
    throw new Error(
      "No tienes acceso a este archivo o todavía está en revisión.",
    );
  return r.blob();
}
export async function downloadFile(id: string) {
  const blob = await privateFileBlob(id),
    url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = id;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export const money = (minor: number | null | undefined, currency = "VES") =>
  minor == null
    ? "A convenir"
    : `${currency === "VES" ? "Bs" : currency === "USD" ? "US$" : currency} ${new Intl.NumberFormat(
        "es-VE",
        {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        },
      ).format(minor / 100)}`;
export const date = (value: string) =>
  new Date(value).toLocaleDateString("es-VE", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "America/Caracas",
  });
export const newKey = () => crypto.randomUUID();
export const expiry = () => new Date(Date.now() + 7 * 86400000).toISOString();
export type Operation = keyof typeof operations;
