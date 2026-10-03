import { lazy, Suspense, useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  BrowserRouter,
  Routes,
  Route,
  NavLink,
  Link,
  Navigate,
  Outlet,
  useLocation,
} from "react-router-dom";
import {
  LayoutDashboard,
  Compass,
  FileText,
  BriefcaseBusiness,
  Wallet,
  ShoppingBag,
  UserRound,
  LifeBuoy,
  ShieldCheck,
  LogOut,
  Menu,
  X,
  ArrowUpRight,
  Plus,
  Bell,
  Sparkles,
  Mail,
  Clock,
  MapPin,
  Settings,
} from "lucide-react";
import { AuthProvider, ToastProvider, useAuth, useToast } from "./context";
import { Loading, useData } from "./components";
import { request } from "./api";
import "./styles.css";
import { LocationProvider } from "./location";
const Explore = lazy(() =>
  import("./pages/Explore").then((m) => ({ default: m.Explore })),
);
const ProfessionalProfile = lazy(() =>
  import("./pages/Explore").then((m) => ({ default: m.ProfessionalProfile })),
);
const AuthPage = lazy(() =>
  import("./pages/Auth").then((m) => ({ default: m.AuthPage })),
);
const VerifyPage = lazy(() =>
  import("./pages/Auth").then((m) => ({ default: m.VerifyPage })),
);
const MobileKycPage = lazy(() => import("./pages/MobileKycPage"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Requests = lazy(() =>
  import("./pages/Requests").then((m) => ({ default: m.Requests })),
);
const NewRequest = lazy(() =>
  import("./pages/Requests").then((m) => ({ default: m.NewRequest })),
);
const RequestDetail = lazy(() =>
  import("./pages/Requests").then((m) => ({ default: m.RequestDetail })),
);
const Opportunities = lazy(() =>
  import("./pages/Requests").then((m) => ({ default: m.Opportunities })),
);
const Contracts = lazy(() =>
  import("./pages/Contracts").then((m) => ({ default: m.Contracts })),
);
const ContractDetail = lazy(() =>
  import("./pages/Contracts").then((m) => ({ default: m.ContractDetail })),
);
const WalletPage = lazy(() => import("./pages/Wallet"));
const Profile = lazy(() => import("./pages/Profile"));
const Marketplace = lazy(() => import("./pages/Marketplace"));
const Support = lazy(() => import("./pages/Support"));
const Admin = lazy(() => import("./pages/Admin"));
const PrivacyPolicy = lazy(() => import("./pages/PrivacyPolicy"));
const CookiePolicy = lazy(() => import("./pages/CookiePolicy"));
const TermsOfService = lazy(() => import("./pages/TermsOfService"));
const LegalNotice = lazy(() => import("./pages/LegalNotice"));
import CookieConsent, { openCookiePreferences } from "./components/CookieConsent";
import { ErrorBoundary } from "./components/ErrorBoundary";
function Protected({ admin = false }: { admin?: boolean }) {
  const { user, ready } = useAuth();
  if (!ready) return <Loading />;
  if (!user) return <Navigate to="/login" replace />;
  if (
    admin &&
    !user.roles.some((r) => ["admin", "superadmin", "support"].includes(r))
  )
    return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
const links = [
  ["/explore", "Explorar", Compass],
  ["/dashboard", "Mi resumen", LayoutDashboard],
  ["/requests", "Mis solicitudes", FileText],
  ["/contracts", "Mis servicios", BriefcaseBusiness],
  ["/wallet", "Mi wallet", Wallet],
  ["/marketplace", "Marketplace", ShoppingBag],
] as const;
function AppleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M15.97 6.37c.62-.75 1.04-1.8 0.93-2.85-.9.04-2 .6-2.65 1.35-.58.67-.99 1.73-.87 2.76 1 .08 2-.51 2.59-1.26z" />
    </svg>
  );
}

function GooglePlayIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M3.609 1.814L13.792 12 3.61 22.186c-.36-.312-.61-.796-.61-1.39V3.204c0-.594.25-1.078.609-1.39zm11.24 11.24l2.585 2.586-12.01 6.863 9.425-9.449zm0-2.108L5.424 1.497l12.01 6.863-2.585 2.586zm1.485 1.054l3.87-2.212c.796-.455.796-1.196 0-1.651l-3.87-2.212-2.11 2.11 2.11 2.11z" />
    </svg>
  );
}

const DEFAULT_TITLE = "Vinko | Profesionales y Servicios de Confianza en Venezuela";
const DEFAULT_DESCRIPTION =
  "Contrata plomeros, electricistas, técnicos de refrigeración, sistemas y creativos verificados en Venezuela. Pagos en custodia protegidos, trato directo y presupuestos transparentes.";
const SEO: Record<string, [string, string]> = {
  "/explore": [DEFAULT_TITLE, DEFAULT_DESCRIPTION],
  "/professionals/": [
    "Perfil profesional · SoyVinko",
    "Conoce servicios, reseñas y zona de cobertura de este profesional verificado en SoyVinko.",
  ],
  "/marketplace": [
    "Marketplace de productos · SoyVinko",
    "Compra productos de comercios y profesionales de confianza en Venezuela, con entrega coordinada.",
  ],
  "/terminos": ["Términos de servicio · SoyVinko", "Condiciones de uso de la plataforma SoyVinko."],
  "/privacidad": ["Política de privacidad · SoyVinko", "Cómo SoyVinko trata y protege tus datos personales."],
  "/cookies": ["Política de cookies · SoyVinko", "Qué cookies usa SoyVinko y cómo configurarlas."],
  "/aviso-legal": ["Aviso legal · SoyVinko", "Información legal del titular de SoyVinko."],
};

type AndroidRelease = { version: string; path: string; size: string };

/**
 * Última versión publicada de la app Android (la misma que usa la autoactualización).
 * El enlace apunta al archivo de esa versión: nunca se sirve un APK viejo desde caché.
 */
function useAndroidRelease(): AndroidRelease | null {
  const [release, setRelease] = useState<AndroidRelease | null>(null);
  useEffect(() => {
    let active = true;
    fetch("/downloads/android/latest.json", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (!active || !data?.url || !data?.versionName) return;
        const path = new URL(data.url, window.location.origin).pathname;
        // Solo archivos propios: el manifiesto no puede redirigir a otro dominio.
        if (!path.startsWith("/downloads/")) return;
        setRelease({ version: data.versionName, path, size: `${(Number(data.size) / 1048576).toFixed(1)} MB` });
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);
  return release;
}

function Layout() {
  const androidRelease = useAndroidRelease();
  const { user, logout } = useAuth(),
    toast = useToast(),
    [menu, setMenu] = useState(false),
    location = useLocation();
  const current =
    links.find(([path]) => location.pathname.startsWith(path))?.[1] ??
    (location.pathname.startsWith("/profile")
      ? "Mi perfil"
      : location.pathname.startsWith("/admin")
        ? "Administración"
        : location.pathname.startsWith("/opportunities")
          ? "Oportunidades"
          : "SoyVinko");
  useEffect(() => {
    const seo = Object.entries(SEO).find(([path]) => location.pathname.startsWith(path))?.[1];
    document.title = seo?.[0] ?? (current === "SoyVinko" ? DEFAULT_TITLE : `${current} · SoyVinko`);
    document
      .querySelector('meta[name="description"]')
      ?.setAttribute("content", seo?.[1] ?? DEFAULT_DESCRIPTION);
    // Canónica por ruta y en el dominio que sirve la página (antes todas apuntaban a la portada).
    const url = window.location.origin + location.pathname;
    document.querySelector('link[rel="canonical"]')?.setAttribute("href", url);
    document.querySelector('meta[property="og:url"]')?.setAttribute("content", url);
  }, [location.pathname, current]);
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Ir al contenido
      </a>
      {menu && (
        <button
          className="sidebar-backdrop"
          aria-label="Cerrar menú"
          onClick={() => setMenu(false)}
        />
      )}
      <aside className={`sidebar ${menu ? "mobile-open" : ""}`}>
        <div className="sidebar-brand">
          <Link className="brand" to="/explore" aria-label="SoyVinko">
            <img src="/logo.svg" alt="Vinko" className="brand-logo" />
          </Link>
          <button
            className="icon-button mobile-only"
            aria-label="Cerrar menú"
            onClick={() => setMenu(false)}
          >
            <X />
          </button>
        </div>
        <div className="workspace-label">TU PRÓXIMA BUENA CONEXIÓN</div>
        <nav aria-label="Navegación principal">
          {links
            .filter(
              ([path]) => user || ["/explore", "/marketplace"].includes(path),
            )
            .map(([path, title, Icon]) => (
              <NavLink
                key={path}
                to={path}
                onClick={() => setMenu(false)}
                className={({ isActive }) =>
                  isActive ? "nav-item active" : "nav-item"
                }
              >
                <Icon size={20} />
                <span>{title}</span>
                {path === "/explore" && <span className="nav-new">Nuevo</span>}
              </NavLink>
            ))}
          {user?.roles.includes("professional") && (
            <>
              <NavLink
                to="/profile?tab=professional"
                className={() =>
                  `nav-item ${location.pathname === "/profile" && location.search.includes("tab=professional") ? "active" : ""}`
                }
                onClick={() => setMenu(false)}
              >
                <BriefcaseBusiness size={20} />
                <span>Panel Profesional</span>
              </NavLink>
              <NavLink
                to="/opportunities"
                className={({ isActive }) =>
                  `nav-item ${isActive ? "active" : ""}`
                }
                onClick={() => setMenu(false)}
              >
                <Sparkles size={20} />
                <span>Oportunidades</span>
              </NavLink>
            </>
          )}
        </nav>
        <div className="sidebar-promo">
          <div className="promo-star">✳</div>
          <strong>
            Lo que sabes hacer
            <br />
            merece SoyVinko.
          </strong>
          <p>Conecta con nuevos clientes y haz crecer tu negocio.</p>
          <Link
            to={user ? "/profile?tab=professional" : "/register"}
            onClick={() => setMenu(false)}
          >
            Mi perfil profesional <ArrowUpRight size={17} />
          </Link>
        </div>
        <div className="sidebar-apps">
          <div className="sidebar-apps-label">Descarga la App</div>
          <button
            type="button"
            className="sidebar-app-btn"
            title="Descargar en App Store"
            onClick={() =>
              toast(
                "La app de SoyVinko para iPhone estará disponible próximamente",
              )
            }
          >
            <span className="sidebar-app-icon">
              <AppleIcon />
            </span>
            <div className="sidebar-app-text">
              <small>Descargar en</small>
              <strong>App Store</strong>
            </div>
            <span className="sidebar-app-badge">iPhone</span>
          </button>
          <a
            className="sidebar-app-btn android"
            href={androidRelease?.path ?? "/downloads/soyvinko-android.apk"}
            download={androidRelease ? `SoyVinko-${androidRelease.version}.apk` : "SoyVinko.apk"}
            type="application/vnd.android.package-archive"
            title={
              androidRelease
                ? `Descargar SoyVinko ${androidRelease.version} para Android (${androidRelease.size})`
                : "Descargar la app de SoyVinko para Android (APK)"
            }
            onClick={() =>
              toast(
                `Descargando SoyVinko${androidRelease ? ` ${androidRelease.version}` : ""} para Android. Al abrir el archivo, permite instalar apps de esta fuente si Android lo pide.`,
              )
            }
          >
            <span className="sidebar-app-icon">
              <GooglePlayIcon />
            </span>
            <div className="sidebar-app-text">
              <small>{androidRelease ? `Versión ${androidRelease.version}` : "Descargar para"}</small>
              <strong>Android (APK)</strong>
            </div>
            <span className="sidebar-app-badge">{androidRelease ? "Nueva" : "Android"}</span>
          </a>
        </div>
        <div className="sidebar-bottom">
          {user && (
            <>
              <NavLink
                to="/profile"
                className="nav-item"
                onClick={() => setMenu(false)}
              >
                <UserRound size={19} />
                Mi perfil
              </NavLink>
              <NavLink
                to="/support"
                className="nav-item"
                onClick={() => setMenu(false)}
              >
                <LifeBuoy size={19} />
                Ayuda y soporte
              </NavLink>
              {user.roles.some((r) =>
                ["admin", "superadmin", "support"].includes(r),
              ) && (
                <NavLink
                  to="/admin"
                  className="nav-item"
                  onClick={() => setMenu(false)}
                >
                  <ShieldCheck size={19} />
                  Administración
                </NavLink>
              )}
              <button className="nav-item logout" onClick={() => void logout()}>
                <LogOut size={18} />
                Cerrar sesión
              </button>
            </>
          )}
          <div className="sidebar-footer">
            Hecho para conectar.<span>SOYVINKO © 2026</span>
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="row">
            <button
              className="icon-button mobile-only"
              aria-label="Abrir menú"
              onClick={() => setMenu(true)}
            >
              <Menu />
            </button>
            <span className="breadcrumb">
              SoyVinko <span>/</span> <strong>{current}</strong>
            </span>
          </div>

          {!user && (
            <nav className="topbar-nav-links desktop-only" aria-label="Navegación del sitio">
              <Link to="/explore" className="topbar-nav-link">
                Explorar
              </Link>
              <a href="/explore#como-funciona" className="topbar-nav-link">
                ¿Cómo funciona?
              </a>
              <a href="/explore#garantia" className="topbar-nav-link">
                Garantía Vinko
              </a>
              <a href="/explore#faq" className="topbar-nav-link">
                Preguntas
              </a>
              <Link to="/marketplace" className="topbar-nav-link">
                Marketplace
              </Link>
            </nav>
          )}

          <div className="topbar-actions">
            {user ? (
              <>
                <Link
                  to="/dashboard"
                  className="icon-button notification-bell"
                  aria-label="Ver novedades"
                >
                  <Bell size={21} />
                </Link>
                <span className="topbar-divider" />
                <Link className="account-chip" to="/profile">
                  <span className="small-avatar">{user.name.charAt(0)}</span>
                  <div>
                    <strong>{user.account_type === "juridico" ? user.name : user.name.split(" ")[0]}</strong>
                    <small>
                      {user.roles.includes("professional")
                        ? "Cliente y profesional"
                        : user.account_type === "juridico" || user.roles.includes("company")
                          ? `Empresa ${user.rif ? `· ${user.rif}` : ""}`
                          : "Mi cuenta personal"}
                    </small>
                  </div>
                </Link>
              </>
            ) : (
              <>
                <Link className="link-button" to="/login">
                  Iniciar sesión
                </Link>
                <Link className="button primary small" to="/register">
                  Crear cuenta <ArrowUpRight size={16} />
                </Link>
              </>
            )}
          </div>
        </header>
        {user && !user.email_verified && (
          <div className="email-unverified-bar" role="alert">
            <div className="email-unverified-content">
              <Mail size={16} />
              <span>
                Tu correo electrónico (<strong>{user.email}</strong>) aún no ha sido verificado.
              </span>
            </div>
            <button
              type="button"
              className="email-unverified-btn"
              onClick={async () => {
                try {
                  await request("/api/v1/auth/verification-email", "POST");
                  toast("¡Correo de activación enviado! Revisa tu bandeja de entrada.");
                } catch (e: any) {
                  toast(e?.message || "No se pudo enviar el correo de activación.");
                }
              }}
            >
              Reenviar enlace de activación
            </button>
          </div>
        )}
        <main id="main" className="main-content">
          <ErrorBoundary key={location.pathname}>
            <Suspense fallback={<Loading />}>
              <Outlet />
            </Suspense>
          </ErrorBoundary>
        </main>
        <footer className="rich-footer">
          <div className="footer-inner">
            <div className="footer-columns-grid">
              <div className="footer-col brand-col">
                <Link to="/explore" className="footer-logo-link" aria-label="Volver al inicio">
                  <img src="/logo.svg" alt="SoyVinko" className="footer-logo-img" loading="lazy" decoding="async" />
                </Link>
                <p className="footer-tagline">
                  Conectamos el talento de personas y oficios de confianza con hogares y empresas en toda Venezuela.
                </p>
                <ul className="footer-contact">
                  <li>
                    <Mail size={15} />
                    <a href="mailto:contacto@soyvinko.com">contacto@soyvinko.com</a>
                  </li>
                  <li>
                    <Clock size={15} /> Lun a Sáb · 8:00 am – 7:00 pm
                  </li>
                  <li>
                    <MapPin size={15} /> Venezuela · Bolívares y divisas
                  </li>
                </ul>
                <div className="footer-badges">
                  <span className="footer-trust-chip">
                    <ShieldCheck size={14} /> Pagos en custodia
                  </span>
                  <span className="footer-trust-chip">
                    <Sparkles size={14} /> KYC verificado
                  </span>
                </div>
              </div>

              <nav className="footer-col" aria-label="Especialidades">
                <h4>Especialidades</h4>
                <ul className="footer-links-list">
                  <li><Link to="/explore?category=plomeria">Plomería y tuberías</Link></li>
                  <li><Link to="/explore?category=electricidad">Electricidad e instalaciones</Link></li>
                  <li><Link to="/explore?category=refrigeracion">Refrigeración y aires</Link></li>
                  <li><Link to="/explore?category=informatica">Soporte técnico y redes</Link></li>
                  <li><Link to="/explore?category=cerrajeria">Cerrajería residencial y autos</Link></li>
                  <li><Link to="/explore?category=diseno-grafico">Diseño y desarrollo web</Link></li>
                </ul>
              </nav>

              <nav className="footer-col" aria-label="Plataforma">
                <h4>Plataforma</h4>
                <ul className="footer-links-list">
                  <li><a href="/explore#como-funciona">¿Cómo funciona?</a></li>
                  <li><a href="/explore#garantia">Garantía y seguridad</a></li>
                  <li><a href="/explore#faq">Preguntas frecuentes</a></li>
                  <li><Link to="/marketplace">Marketplace de productos</Link></li>
                  <li><Link to={user ? "/requests/new" : "/register"}>Publicar requerimiento</Link></li>
                  <li><Link to={user ? "/profile?tab=professional" : "/register"}>Unirme como profesional</Link></li>
                </ul>
              </nav>

              <nav className="footer-col" aria-label="Soporte y confianza">
                <h4>Soporte y confianza</h4>
                <ul className="footer-links-list">
                  <li><Link to="/support">Centro de ayuda y disputas</Link></li>
                  <li><Link to="/privacidad">Protección de datos (RGPD)</Link></li>
                  <li><Link to="/terminos">Garantía y custodia (Escrow)</Link></li>
                  <li><Link to="/aviso-legal">Aviso legal</Link></li>
                </ul>
              </nav>
            </div>

            <div className="footer-bottom-row">
              <span>© 2026 SoyVinko. Todos los derechos reservados.</span>
              <nav className="footer-legal-links" aria-label="Enlaces legales">
                <Link to="/terminos">Términos de servicio</Link>
                <Link to="/privacidad">Política de privacidad</Link>
                <Link to="/cookies">Política de cookies</Link>
                <button
                  type="button"
                  className="footer-btn-link"
                  onClick={openCookiePreferences}
                  aria-label="Configurar preferencias de cookies"
                >
                  <Settings size={13} /> Configurar cookies
                </button>
              </nav>
            </div>
          </div>
        </footer>

        {/* Banner interactivo de Consentimiento de Cookies (CMP) */}
        <CookieConsent />

        {/* Barra de navegación inferior móvil para UX ágil */}
        <nav className="mobile-bottom-nav mobile-only" aria-label="Navegación móvil rápida">
          <NavLink to="/explore" className={({ isActive }) => `mobile-nav-item ${isActive ? "active" : ""}`}>
            <Compass size={20} />
            <span>Explorar</span>
          </NavLink>
          <NavLink to="/marketplace" className={({ isActive }) => `mobile-nav-item ${isActive ? "active" : ""}`}>
            <ShoppingBag size={20} />
            <span>Tienda</span>
          </NavLink>
          {user ? (
            <>
              <NavLink to="/requests" className={({ isActive }) => `mobile-nav-item ${isActive ? "active" : ""}`}>
                <FileText size={20} />
                <span>Solicitudes</span>
              </NavLink>
              <NavLink to="/profile" className={({ isActive }) => `mobile-nav-item ${isActive ? "active" : ""}`}>
                <UserRound size={20} />
                <span>Mi cuenta</span>
              </NavLink>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => `mobile-nav-item ${isActive ? "active" : ""}`}>
                <UserRound size={20} />
                <span>Ingresar</span>
              </NavLink>
              <NavLink to="/register" className={({ isActive }) => `mobile-nav-item mobile-nav-highlight ${isActive ? "active" : ""}`}>
                <Plus size={20} />
                <span>Registrarme</span>
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </div>
  );
}
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <LocationProvider>
            <ErrorBoundary>
            <Suspense fallback={<Loading />}>
              <Routes>
                <Route path="/login" element={<AuthPage />} />
                <Route path="/register" element={<AuthPage register />} />
                <Route path="/verify" element={<VerifyPage />} />
                <Route path="/verificacion-movil" element={<MobileKycPage />} />
                <Route path="/kyc-mobile" element={<MobileKycPage />} />
                <Route element={<Layout />}>
                  <Route index element={<Navigate to="/explore" replace />} />
                  <Route path="/explore" element={<Explore />} />
                  <Route
                    path="/professionals/:id"
                    element={<ProfessionalProfile />}
                  />
                  <Route path="/marketplace" element={<Marketplace />} />
                  <Route path="/privacidad" element={<PrivacyPolicy />} />
                  <Route path="/privacy" element={<PrivacyPolicy />} />
                  <Route path="/cookies" element={<CookiePolicy />} />
                  <Route path="/terminos" element={<TermsOfService />} />
                  <Route path="/terms" element={<TermsOfService />} />
                  <Route path="/aviso-legal" element={<LegalNotice />} />
                  <Route element={<Protected />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/requests" element={<Requests />} />
                    <Route path="/requests/new" element={<NewRequest />} />
                    <Route path="/requests/:id" element={<RequestDetail />} />
                    <Route path="/opportunities" element={<Opportunities />} />
                    <Route path="/contracts" element={<Contracts />} />
                    <Route path="/contracts/:id" element={<ContractDetail />} />
                    <Route path="/wallet" element={<WalletPage />} />
                    <Route path="/profile" element={<Profile />} />
                    <Route path="/professional" element={<Navigate to="/profile?tab=professional" replace />} />
                    <Route path="/support" element={<Support />} />
                  </Route>
                  <Route element={<Protected admin />}>
                    <Route path="/admin" element={<Admin />} />
                  </Route>
                  <Route
                    path="*"
                    element={
                      <div className="empty">
                        <h1>Esta página no está disponible</h1>
                        <Link className="button primary" to="/explore">
                          Volver a explorar
                        </Link>
                      </div>
                    }
                  />
                </Route>
              </Routes>
            </Suspense>
            </ErrorBoundary>
          </LocationProvider>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
