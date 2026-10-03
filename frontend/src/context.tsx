import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import {
  observeSession,
  refreshSession,
  setSession,
  request,
  type User,
  type Data,
} from "./api";
const AuthContext = createContext<{
  user: User | null;
  ready: boolean;
  login: (data: Data) => Promise<Data | void>;
  logout: () => Promise<void>;
  reload: () => Promise<void>;
}>({
  user: null,
  ready: false,
  login: async () => {},
  logout: async () => {},
  reload: async () => {},
});
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null),
    [ready, setReady] = useState(false);
  // Token de traspaso KYC al móvil: se lee durante el render, antes de que la página limpie la URL.
  const [kycAuth] = useState(() => {
    const searchParams = new URLSearchParams(window.location.search);
    return (
      new URLSearchParams(window.location.hash.slice(1)).get("token") ||
      searchParams.get("token") ||
      searchParams.get("kyc_auth")
    );
  });
  useEffect(() => {
    observeSession((data) => setUser(data?.user ?? null));
    if (kycAuth) {
      setSession({ access_token: kycAuth });
      request<User>("/api/v1/auth/me")
        .then((me) => setUser(me))
        .catch(() => {})
        .finally(() => setReady(true));
      return () => observeSession(() => {});
    }
    if (document.cookie.includes("chamba_csrf="))
      refreshSession()
        .catch(() => {})
        .finally(() => setReady(true));
    else setReady(true);
    return () => observeSession(() => {});
  }, []);
  const login = async (data: Data) => {
    const result = await request<Data>("/api/v1/auth/login", "POST", {
      ...data,
      client: "web",
    });
    if (result && result.access_token) {
      setSession(result);
    }
    return result;
  };
  const logout = async () => {
    try {
      await request("/api/v1/auth/logout", "POST", { client: "web" });
    } finally {
      setSession(null);
    }
  };
  const reload = async () => {
    setUser(await request("/api/v1/auth/me"));
  };
  return (
    <AuthContext.Provider value={{ user, ready, login, logout, reload }}>
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => useContext(AuthContext);
const ToastContext = createContext<(message: string, error?: boolean) => void>(
  () => {},
);
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<{
    message: string;
    error: boolean;
  } | null>(null);
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 6000);
      return () => clearTimeout(timer);
    }
  }, [toast]);
  return (
    <ToastContext.Provider
      value={(message, error = false) => setToast({ message, error })}
    >
      {children}
      {toast && (
        <div
          className={`toast ${toast.error ? "error" : ""}`}
          role={toast.error ? "alert" : "status"}
        >
          <span>{toast.message}</span>
          <button aria-label="Cerrar aviso" onClick={() => setToast(null)}>
            ×
          </button>
        </div>
      )}
    </ToastContext.Provider>
  );
}
export const useToast = () => useContext(ToastContext);
