import { useMemo, useState } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowRight,
  Search,
  MapPin,
  Map as MapIcon,
  LayoutGrid,
  ShieldCheck,
  Check,
  Globe,
  Sparkles,
  Wrench,
  Zap,
  Monitor,
  Palette,
  KeyRound,
  Heart,
  Star,
  X,
  Plus,
  CheckCircle2,
} from "lucide-react";
import { api, money, type Data } from "../api";
import { useAuth } from "../context";
import { serviceImages } from "../serviceImages";
import { AddressWizard } from "../AddressWizard";
import { ExploreMap } from "../ExploreMap";
import { LocationStatus, useDeviceLocation, type GeoPoint } from "../location";
import {
  useData,
  PageTitle,
  ProfessionalCard,
  ProfessionalCardSkeleton,
  Loading,
  Empty,
  ErrorBox,
  Section,
  Badge,
  RunButton,
} from "../components";
import {
  vinkoCategoryIconMap,
  VinkoIconPlumbing,
  VinkoIconHandshake,
  VinkoIconShieldSafe,
  VinkoIconUsers,
  VinkoIconSparkle,
  VinkoIconPhoneApp,
  VinkoIconSearchLens,
  VinkoIconChatConversation,
  VinkoIconCheckCircle,
  VinkoIconWallet,
} from "../VinkoIcons";

export function Explore() {
  const { user } = useAuth();
  const geo = useDeviceLocation();
  const [manualPoint, setManualPoint] = useState<GeoPoint | null>(null),
    [manualArea, setManualArea] = useState<Data | null>(null),
    [chooseZone, setChooseZone] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const [showAllCats, setShowAllCats] = useState(false);
  const [openFaq, setOpenFaq] = useState<number | null>(null);
  const [category, setCategory] = useState(""),
    [mode, setMode] = useState(""),
    [search, setSearch] = useState(""),
    [address, setAddress] = useState(""),
    [sort, setSort] = useState("distance"),
    [radius, setRadius] = useState(10000),
    [verified, setVerified] = useState(false);
  const cats = useData(() => api<Data[]>("catalog"), []),
    account = useData(
      () => (user ? api("my_account") : Promise.resolve(null)),
      [user?.id],
    );
  const savedAddress = (account.data?.addresses ?? []).find(
    (a: Data) => a.id === address && a.country_code === "VE",
  );
  const searchPoint = savedAddress
    ? { lat: savedAddress.latitude, lon: savedAddress.longitude }
    : (manualPoint ?? (geo.area ? geo.point : null));
  const data = useData(async () => {
    if (user && category && mode === "onsite" && address) {
      const rows = await api<Data[]>("search_professionals", {
        category,
        address,
        radius_m: radius,
        sort,
        verified_only: verified,
      });
      return rows.map((r) => ({
        ...r.profile,
        distance_m: r.distance_m,
        sponsored: r.sponsored,
      }));
    }
    if (user && category && mode === "remote")
      return api<Data[]>("search_remote_professionals", {
        category,
        verified_only: verified,
      });
    if (mode !== "remote" && searchPoint) {
      const nearby = await api<Data[]>("nearby_professionals", {
        lat: searchPoint.lat,
        lon: searchPoint.lon,
        category: category || undefined,
        radius_m: radius,
        verified_only: verified,
        limit: 100,
      });
      if (sort === "recommended")
        nearby.sort(
          (a, b) =>
            Number(b.verified) - Number(a.verified) ||
            (b.rating ?? 0) - (a.rating ?? 0) ||
            a.distance_m - b.distance_m,
        );
      if (mode === "onsite") return nearby;
      const remote = await api<Data[]>("discover", {
        limit: 100,
        category: category || undefined,
        mode: "remote",
      });
      return [
        ...nearby,
        ...remote.filter((p) => !nearby.some((n) => n.id === p.id)),
      ];
    }
    return api<Data[]>("discover", {
      limit: 100,
      category: category || undefined,
      mode: mode || undefined,
    });
  }, [
    category,
    mode,
    address,
    sort,
    radius,
    verified,
    user?.id,
    searchPoint?.lat,
    searchPoint?.lon,
  ]);
  const profiles = useMemo(
    () =>
      (data.data ?? []).filter(
        (p) =>
          (!verified || p.verified) &&
          `${p.name} ${p.bio} ${p.offerings?.map((o: Data) => o.title).join(" ")}`
            .toLowerCase()
            .includes(search.toLowerCase()),
      ),
    [data.data, verified, search],
  );
  return (
    <>
      <div className="explore-intro explore-hero-human">
        <div className="hero-left-content">
          <LocationStatus />
          <div className="eyebrow hero-eyebrow">
            <span className="live-dot" /> <VinkoIconSparkle size={13} /> TALENTO REAL · PERSONAS QUE CONECTAN
          </div>
          <h1>
            Personas de confianza para <em>hacerlo realidad.</em>
          </h1>
          <p className="hero-subtitle">
            Para las reparaciones de tu hogar, tu tecnología y tus proyectos clave.
            Conecta directamente con profesionales verificados con trato humano y pagos protegidos.
          </p>

          <div className="hero-search">
            <Search size={22} />
            <input
              aria-label="Buscar profesionales"
              placeholder="¿Qué servicio o profesional necesitas hoy?"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  document.getElementById("results")?.scrollIntoView({ behavior: "smooth" });
                }
              }}
            />
            {search && (
              <button
                type="button"
                className="search-clear-btn"
                aria-label="Borrar búsqueda"
                onClick={() => setSearch("")}
              >
                <X size={16} />
              </button>
            )}
            <button
              className="button primary"
              onClick={() =>
                document
                  .getElementById("results")
                  ?.scrollIntoView({ behavior: "smooth" })
              }
            >
              Buscar experto <ArrowRight size={16} />
            </button>
          </div>

          <div className="hero-quick-tags">
            <span className="quick-label">Populares:</span>
            {[
              ["plomeria", "Plomería"],
              ["electricidad", "Electricidad"],
              ["informatica", "Tecnología"],
              ["cerrajeria", "Cerrajería"],
              ["diseno-grafico", "Diseño"],
            ].map(([slug, label]) => {
              const catObj = (cats.data ?? []).find((c: any) => c.slug === slug);
              const isSel = catObj && category === catObj.id;
              return (
                <button
                  key={slug}
                  type="button"
                  className={`quick-pill ${isSel ? "active" : ""}`}
                  onClick={() => {
                    if (catObj) {
                      setCategory(category === catObj.id ? "" : catObj.id);
                      document.getElementById("results")?.scrollIntoView({ behavior: "smooth" });
                    }
                  }}
                >
                  {label}
                </button>
              );
            })}
          </div>

        </div>

        <div className="hero-right-visual" aria-hidden="true">
          <div className="hero-mosaic">
            <figure className="mosaic-main">
              <img
                src="/images/lifestyle/hero-handshake.webp"
                alt=""
                width={1200}
                height={896}
                fetchPriority="high"
              />
            </figure>
            <figure className="mosaic-tile">
              <img
                src="/images/services/electricidad-v1.webp"
                alt=""
                width={1280}
                height={720}
              />
              <figcaption>
                <Wrench size={13} /> Hogar y reparaciones
              </figcaption>
            </figure>
            <figure className="mosaic-tile">
              <img
                src="/images/services/diseno-grafico-v1.webp"
                alt=""
                width={1280}
                height={720}
              />
              <figcaption>
                <Monitor size={13} /> Talento digital
              </figcaption>
            </figure>

            <div className="hero-floating-card badge-rating">
              <div className="badge-avatar-group">
                <span className="mini-avatar">V</span>
                <span className="mini-avatar professional">P</span>
              </div>
              <div className="badge-info">
                <div className="stars">★★★★★ <span className="score">4.9 / 5</span></div>
                <p>"Excelente atención y trabajo impecable"</p>
              </div>
            </div>

            <div className="hero-floating-card badge-guarantee">
              <div className="guarantee-icon-box">
                <VinkoIconShieldSafe size={20} />
              </div>
              <div className="badge-info">
                <strong>Garantía SoyVinko</strong>
                <span>Pago liberado tras tu conformidad</span>
              </div>
            </div>

            <div className="hero-active-chip">
              <span className="live-dot" />
              <span>+120 profesionales activos hoy</span>
            </div>
          </div>
        </div>

        <ul className="hero-trust-bar">
          <li>
            <span className="trust-icon"><VinkoIconUsers size={20} /></span>
            <div>
              <strong>Perfiles verificados</strong>
              <span>Identidad revisada (KYC)</span>
            </div>
          </li>
          <li>
            <span className="trust-icon"><VinkoIconShieldSafe size={20} /></span>
            <div>
              <strong>Pagos protegidos</strong>
              <span>Se liberan con tu conformidad</span>
            </div>
          </li>
          <li>
            <span className="trust-icon"><VinkoIconHandshake size={20} /></span>
            <div>
              <strong>Trato directo</strong>
              <span>Presupuesto claro antes de empezar</span>
            </div>
          </li>
          <li>
            <span className="trust-icon"><VinkoIconChatConversation size={20} /></span>
            <div>
              <strong>Soporte humano</strong>
              <span>Mediación si algo sale mal</span>
            </div>
          </li>
        </ul>
      </div>

      <section className="categories-strip">
        <div className="section-head">
          <div>
            <div className="eyebrow">EXPLORA POR ESPECIALIDAD</div>
            <h2>¿Por dónde empezamos hoy?</h2>
          </div>
          <button
            className="button secondary small"
            onClick={() => setShowAllCats(!showAllCats)}
          >
            {showAllCats ? "Mostrar destacadas" : "Ver todas las categorías"} <ArrowRight size={15} />
          </button>
        </div>
        <div className="category-grid">
          {(cats.data ?? [])
            .filter((c) =>
              showAllCats
                ? true
                : [
                    "plomeria",
                    "electricidad",
                    "informatica",
                    "cerrajeria",
                    "diseno-grafico",
                    "programacion",
                  ].includes(c.slug)
            )
            .map((c, i) => {
              const Icon = vinkoCategoryIconMap[c.slug] ?? VinkoIconPlumbing;
              return (
                <button
                  key={c.id}
                  className={`category ${category === c.id ? "selected" : ""}`}
                  onClick={() => {
                    setCategory(category === c.id ? "" : c.id);
                    document.getElementById("results")?.scrollIntoView({ behavior: "smooth" });
                  }}
                >
                  <span className={`category-icon tint-${i % 4}`}>
                    <Icon size={24} strokeWidth={2.2} />
                  </span>
                  <span className="category-title">
                    {c.name === "Reparación y soporte de computadores"
                      ? "Tecnología"
                      : c.name === "Refrigeración y aire acondicionado"
                        ? "Refrigeración"
                        : c.name === "Reparación de electrodomésticos"
                          ? "Electrodomésticos"
                          : c.name}
                  </span>
                </button>
              );
            })}
        </div>
      </section>

      {/* Sección Humanizada: Historias de Conexión Real */}
      <section className="human-stories-section">
        <div className="stories-head">
          <div>
            <div className="eyebrow">CONEXIONES QUE TRANSFORMAN</div>
            <h2>Personas reales resolviendo en comunidad</h2>
          </div>
          <p>
            Una plataforma diseñada para brindarte tranquilidad, trato humano y resultados garantizados.
          </p>
        </div>

        <div className="human-stories-grid">
          <article className="story-card story-featured">
            <img
              src="/images/lifestyle/service-inspection.webp"
              alt="Profesional y cliente revisando el servicio con claridad"
              className="story-img"
              width={1200}
              height={896}
              loading="lazy"
              decoding="async"
            />
            <div className="story-card-content">
              <span className="story-tag">
                <VinkoIconHandshake size={14} /> Trato directo
              </span>
              <h3>Transparencia total</h3>
              <p>
                Inspección honesta, diálogo sin rodeos y presupuesto acordado antes de comenzar. Tú mantienes el control de principio a fin.
              </p>
              <div className="story-meta">
                <VinkoIconShieldSafe size={14} /> Pago seguro con custodia
              </div>
            </div>
          </article>

          <article className="story-card">
            <div className="story-image-wrap">
              <img
                src="/images/lifestyle/client-booking.webp"
                alt="Cliente solicitando servicio cómodamente desde el móvil"
                className="story-img"
                width={1200}
                height={896}
                loading="lazy"
                decoding="async"
              />
            </div>
            <div className="story-card-content">
              <span className="story-tag">
                <VinkoIconPhoneApp size={14} /> Solicitud en 2 min
              </span>
              <h3>Pide a tu medida</h3>
              <p>
                Describe lo que necesitas desde tu teléfono o computadora. Recibe propuestas claras y perfiles verificados sin perder tiempo.
              </p>
              <div className="story-meta">
                <Check size={14} /> Perfiles 100% verificados
              </div>
            </div>
          </article>

          <article className="story-card">
            <div className="story-image-wrap">
              <img
                src="/images/lifestyle/digital-creative.webp"
                alt="Especialista creativa y de desarrollo trabajando"
                className="story-img"
                width={1200}
                height={896}
                loading="lazy"
                decoding="async"
              />
            </div>
            <div className="story-card-content">
              <span className="story-tag">
                <VinkoIconSparkle size={14} /> Talento especializado
              </span>
              <h3>De lo técnico a lo creativo</h3>
              <p>
                Electricistas, plomeros, cerrajeros, diseñadores y programadores listos para dar lo mejor de sí en cada proyecto.
              </p>
              <div className="story-meta">
                <VinkoIconUsers size={14} /> Comunidad que se apoya
              </div>
            </div>
          </article>
        </div>
      </section>

      {/* Sección Rediseñada: Cómo funciona SoyVinko */}
      <section id="como-funciona" className="how-it-works-v2">
        <div className="how-header-v2">
          <div className="how-eyebrow-pill">
            <VinkoIconSparkle size={13} /> PROCESO TRANSPARENTE
          </div>
          <h2>Contratar con tranquilidad nunca fue tan fácil</h2>
          <p>
            Eliminamos la incertidumbre de contratar servicios informales. Tres pasos claros diseñados para darte seguridad total de principio a fin.
          </p>
        </div>

        <div className="how-steps-grid-v2">
          {/* Paso 1 */}
          <div className="how-card-v2">
            <div className="how-card-top">
              <span className="step-pill">PASO 01</span>
              <span className="step-tag-free">Sin costo para ti</span>
            </div>
            
            <div className="how-mini-mockup mockup-search">
              <div className="mockup-search-input">
                <Search size={14} />
                <span>Reparación de aire acondicionado...</span>
              </div>
              <div className="mockup-tags-row">
                <span className="m-tag active">Refrigeración</span>
                <span className="m-tag">Electricidad</span>
                <span className="m-tag">Plomería</span>
              </div>
              <div className="mockup-location-chip">
                <MapPin size={12} /> Cerca de tu ubicación · Respuesta rápida
              </div>
            </div>

            <div className="how-card-body">
              <div className="how-icon-title">
                <div className="how-step-icon">
                  <VinkoIconSearchLens size={22} />
                </div>
                <h3>1. Explora o publica</h3>
              </div>
              <p>
                Dinos qué necesitas. Explora perfiles en tu zona o publica una solicitud en 2 minutos indicando fecha y presupuesto.
              </p>
            </div>
            
            <div className="how-card-footer">
              <CheckCircle2 size={16} className="green-check" />
              <span>Publicación rápida y sin compromiso</span>
            </div>
          </div>

          {/* Paso 2 */}
          <div className="how-card-v2">
            <div className="how-card-top">
              <span className="step-pill">PASO 02</span>
              <span className="step-tag-verified">Identidad Validada</span>
            </div>
            
            <div className="how-mini-mockup mockup-chat">
              <div className="mockup-pro-header">
                <span className="mockup-avatar">CR</span>
                <div className="mockup-pro-meta">
                  <strong>Carlos Ramírez</strong>
                  <span>★★★★★ 4.9 · Verificado</span>
                </div>
                <ShieldCheck size={18} className="shield-green" />
              </div>
              <div className="mockup-chat-bubble">
                <p>"Hola, revisé tu requerimiento. Puedo ir hoy con repuestos originales."</p>
                <span className="quote-chip">Presupuesto acordado: $35</span>
              </div>
            </div>

            <div className="how-card-body">
              <div className="how-icon-title">
                <div className="how-step-icon">
                  <VinkoIconChatConversation size={22} />
                </div>
                <h3>2. Compara y acuerda</h3>
              </div>
              <p>
                Revisa opiniones reales, fotos de trabajos anteriores y credenciales verificadas. Chatea directo y fija el alcance.
              </p>
            </div>
            
            <div className="how-card-footer">
              <CheckCircle2 size={16} className="green-check" />
              <span>Trato directo con el especialista</span>
            </div>
          </div>

          {/* Paso 3 */}
          <div className="how-card-v2 highlight-v2">
            <div className="how-card-top">
              <span className="step-pill highlight">PASO 03</span>
              <span className="step-tag-escrow">Garantía Escrow</span>
            </div>
            
            <div className="how-mini-mockup mockup-escrow">
              <div className="escrow-status-bar">
                <div className="escrow-lock-icon">
                  <VinkoIconShieldSafe size={20} />
                </div>
                <div>
                  <strong>Pago en Custodia Segura</strong>
                  <span>Fondos protegidos en SoyVinko</span>
                </div>
              </div>
              <div className="escrow-progress">
                <div className="escrow-progress-fill" />
              </div>
              <div className="escrow-release-cta">
                <span>✓ Liberar fondos tras tu conformidad</span>
              </div>
            </div>

            <div className="how-card-body">
              <div className="how-icon-title">
                <div className="how-step-icon highlight">
                  <VinkoIconShieldSafe size={22} />
                </div>
                <h3>3. Paga con protección</h3>
              </div>
              <p>
                Tu dinero queda resguardado. El profesional realiza el trabajo y los fondos solo se liberan cuando confirmes tu satisfacción.
              </p>
            </div>
            
            <div className="how-card-footer highlight">
              <CheckCircle2 size={16} className="green-check" />
              <span>Garantía de satisfacción o mediación</span>
            </div>
          </div>
        </div>
      </section>

      <div id="results" className="section-head results-head">
        <div>
          <div className="eyebrow">ENCUENTRA TU MATCH</div>
          <h2>Talento para lo que necesitas</h2>
          <p className="muted">
            Personas con experiencia, cerca de ti o a un clic de distancia.
            {!data.loading && (
              <span className="results-count-badge">
                {profiles.length} {profiles.length === 1 ? "profesional disponible" : "profesionales disponibles"}
                {search ? ` para "${search}"` : ""}
              </span>
            )}
          </p>
        </div>
        <Link
          to={user ? "/requests/new" : "/register"}
          className="button primary"
        >
          <Plus size={16} /> Publicar una solicitud
        </Link>
      </div>
      <div
        className="tabs explore-view-tabs"
        role="tablist"
        aria-label="Vista de resultados"
      >
        {[
          { id: "list", label: "Listado", icon: LayoutGrid, map: false },
          { id: "map", label: "Ver mapa", icon: MapIcon, map: true },
        ].map(({ id, label, icon: Icon, map }) => (
          <button
            key={id}
            id={`explore-${id}-tab`}
            type="button"
            role="tab"
            className={showMap === map ? "active" : ""}
            aria-selected={showMap === map}
            aria-controls="explore-results-panel"
            tabIndex={showMap === map ? 0 : -1}
            disabled={map && mode === "remote"}
            onClick={() => setShowMap(map)}
            onKeyDown={(event) => {
              if (
                !["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)
              )
                return;
              event.preventDefault();
              const next =
                mode !== "remote" &&
                (event.key === "End" || (event.key !== "Home" && !map));
              setShowMap(next);
              document
                .getElementById(`explore-${next ? "map" : "list"}-tab`)
                ?.focus();
            }}
          >
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>
      <div className="filters">
        <div className="segmented">
          {[
            ["", "Todos"],
            ["onsite", "Presenciales"],
            ["remote", "Remotos"],
          ].map(([v, l]) => (
            <button
              className={mode === v ? "active" : ""}
              key={v}
              onClick={() => {
                setMode(v);
                if (v === "remote") setShowMap(false);
              }}
            >
              {l}
            </button>
          ))}
        </div>
        <select
          aria-label="Categoría"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          <option value="">Todas las categorías</option>
          {cats.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <label className="inline-check">
          <input
            type="checkbox"
            checked={verified}
            onChange={(e) => setVerified(e.target.checked)}
          />{" "}
          Solo verificados
        </label>
      </div>
      {mode !== "remote" && (
        <div className="nearby-filter">
          <MapPin size={18} />
          <div className="nearby-location-copy">
            <strong>
              {savedAddress
                ? savedAddress.label
                : manualArea
                  ? `${manualArea.parish_name}, ${manualArea.state_name}`
                  : geo.area
                    ? `Cerca de ti · ${geo.area.parish_name}`
                    : "Encuentra servicios cerca de ti"}
            </strong>
            <small>
              {searchPoint
                ? "Ordenamos los servicios presenciales por distancia."
                : geo.error ||
                  "Permite tu ubicación o elige una zona en el mapa."}
            </small>
          </div>
          <select
            aria-label="Dirección para buscar"
            value={address || (manualPoint ? "manual" : "current")}
            onChange={(e) => {
              if (e.target.value === "current") {
                setAddress("");
                setManualPoint(null);
                setManualArea(null);
                void geo.locate().catch(() => {});
              } else if (e.target.value === "manual") setAddress("");
              else setAddress(e.target.value);
            }}
          >
            <option value="current">Mi ubicación actual</option>
            {manualPoint && (
              <option value="manual">Zona elegida en el mapa</option>
            )}
            {account.data?.addresses
              ?.filter((a: Data) => a.country_code === "VE")
              .map((a: Data) => (
                <option key={a.id} value={a.id}>
                  {a.label} · {a.city}
                </option>
              ))}
          </select>
          <button
            className="button secondary small"
            type="button"
            onClick={() => setChooseZone(true)}
          >
            Elegir zona en el mapa
          </button>
          <select
            aria-label="Radio de búsqueda"
            value={radius}
            onChange={(e) => setRadius(+e.target.value)}
          >
            {[5000, 10000, 25000, 50000].map((x) => (
              <option key={x} value={x}>
                {x / 1000} km
              </option>
            ))}
          </select>
          <select
            aria-label="Ordenar resultados"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            <option value="distance">Más cercanos primero</option>
            <option value="recommended">Verificados y destacados</option>
          </select>
          {user && (
            <Link className="link-button" to="/profile">
              Mis direcciones
            </Link>
          )}
        </div>
      )}
      {chooseZone && (
        <AddressWizard
          kind="search"
          onClose={() => setChooseZone(false)}
          onSaved={(point, area) => {
            setAddress("");
            setManualPoint(point!);
            setManualArea(area!);
          }}
        />
      )}
      <div
        id="explore-results-panel"
        role="tabpanel"
        aria-labelledby={`explore-${showMap ? "map" : "list"}-tab`}
        tabIndex={0}
      >
        {showMap ? (
          <>
            <ExploreMap
              profiles={data.loading || data.error ? [] : profiles}
              searchPoint={searchPoint}
              category={category}
              radius={radius}
              loading={data.loading}
            />
            {data.error && (
              <ErrorBox message={data.error} retry={data.reload} />
            )}
          </>
        ) : data.loading ? (
          <div className="professional-grid" aria-busy="true">
            {[...Array(6)].map((_, i) => (
              <ProfessionalCardSkeleton key={i} />
            ))}
          </div>
        ) : data.error ? (
          <ErrorBox message={data.error} retry={data.reload} />
        ) : profiles.length ? (
          <div className="professional-grid">
            {profiles.map((p, i) => (
              <ProfessionalCard
                key={p.id}
                profile={p}
                index={i}
                image={
                  serviceImages[
                    cats.data?.find(
                      (c) => c.id === p.offerings?.[0]?.category_id,
                    )?.slug
                  ]
                }
              />
            ))}
          </div>
        ) : (
          <Empty
            title="Aún no encontramos un profesional"
            text="Prueba otra categoría o amplía la zona de búsqueda."
          />
        )}
      </div>

      {/* Sección de Seguridad y Garantía Vinko */}
      <section id="garantia" className="vinko-trust-section">
        <div className="trust-inner-banner">
          <div className="trust-visual-col">
            <div className="trust-badge-circle">
              <VinkoIconShieldSafe size={48} />
            </div>
            <div className="trust-stat">
              <strong>100%</strong>
              <span>Pagos con custodia</span>
            </div>
          </div>
          <div className="trust-content-col">
            <div className="eyebrow light">SEGURIDAD GARANTIZADA</div>
            <h2>Tu tranquilidad es nuestra prioridad absoluta</h2>
            <p>
              A diferencia de contratar por redes sociales o clasificados informales, en SoyVinko cuentas con un ecosistema seguro diseñado para proteger tanto a clientes como a profesionales independientes.
            </p>
            <div className="trust-pillars-grid">
              <div className="trust-pillar-item">
                <div className="pillar-icon"><VinkoIconWallet size={20} /></div>
                <div>
                  <strong>Pago en Custodia (Escrow)</strong>
                  <p>El profesional inicia con la certeza de que el dinero existe, y tú tienes la garantía de que no se paga hasta finalizar.</p>
                </div>
              </div>
              <div className="trust-pillar-item">
                <div className="pillar-icon"><VinkoIconCheckCircle size={20} /></div>
                <div>
                  <strong>Validación de Identidad (KYC)</strong>
                  <p>Verificamos documentos oficiales de identidad y datos de contacto de cada prestador registrado.</p>
                </div>
              </div>
              <div className="trust-pillar-item">
                <div className="pillar-icon"><VinkoIconHandshake size={20} /></div>
                <div>
                  <strong>Mediación y Soporte Humano</strong>
                  <p>Si surge algún desacuerdo, nuestro equipo de soporte interviene de inmediato para asegurar una solución justa.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Sección de FAQ Interactivo */}
      <section id="faq" className="vinko-faq-section">
        <div className="section-head text-center">
          <div>
            <div className="eyebrow">PREGUNTAS FRECUENTES</div>
            <h2>¿Tienes dudas? Te las respondemos</h2>
            <p className="muted">Todo lo que necesitas saber antes de contratar o unirte como profesional.</p>
          </div>
        </div>

        <div className="faq-accordion-list">
          {[
            {
              q: "¿Cómo funciona la protección de pagos en custodia (escrow)?",
              a: "Cuando contratas un servicio y realizas el abono, el dinero no va inmediatamente al profesional. Se mantiene en custodia segura dentro de SoyVinko. Una vez que el profesional finaliza la tarea y tú confirmas tu conformidad, el sistema libera los fondos.",
            },
            {
              q: "¿Cómo se verifica la identidad de los profesionales?",
              a: "Cada profesional registrado en SoyVinko pasa por un proceso de verificación de identidad donde valida su documento de identidad (cédula o RIF), número de teléfono y experiencia profesional, otorgando una insignia visible de 'Identidad Verificada'.",
            },
            {
              q: "¿Tiene algún costo publicar una solicitud de servicio?",
              a: "No, para los clientes la publicación de requerimientos y la búsqueda en el catálogo es 100% gratuita. Solo pagas por el servicio acordado cuando decides contratar a un profesional.",
            },
            {
              q: "¿Qué métodos de pago puedo utilizar en Venezuela?",
              a: "SoyVinko soporta múltiples métodos adaptados a la realidad venezolana, incluyendo Pago Móvil, transferencias bancarias nacionales, pagos en divisas y billetera digital integrada para recargas inmediatas.",
            },
            {
              q: "¿Cómo puedo registrarme para ofrecer mis servicios?",
              a: "Solo debes hacer clic en 'Ofrecer mis servicios', completar tu registro con tu número de teléfono o correo, activar tu perfil profesional con tus oficios y experiencia, y comenzarás a recibir solicitudes de clientes en tu zona.",
            },
          ].map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (
              <div key={idx} className={`faq-item ${isOpen ? "open" : ""}`}>
                <button
                  type="button"
                  className="faq-question-btn"
                  aria-expanded={isOpen}
                  onClick={() => setOpenFaq(isOpen ? null : idx)}
                >
                  <span>{faq.q}</span>
                  <span className="faq-toggle-icon">
                    {isOpen ? "−" : "+"}
                  </span>
                </button>
                {isOpen && (
                  <div className="faq-answer-content">
                    <p>{faq.a}</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* Banner de Doble Propósito: Clientes y Profesionales */}
      <section className="dual-cta-section">
        <div className="dual-cta-card client-cta">
          <div className="cta-icon"><VinkoIconSparkle size={26} /></div>
          <div className="eyebrow light">PARA CLIENTES Y HOGARES</div>
          <h2>¿Tienes un proyecto o reparación pendiente?</h2>
          <p>Publica lo que necesitas hoy mismo y recibe presupuestos de profesionales calificados en tu ciudad.</p>
          <Link to={user ? "/requests/new" : "/register"} className="button primary">
            Publicar solicitud gratis <ArrowRight size={16} />
          </Link>
        </div>

        <div className="dual-cta-card pro-cta">
          <div className="cta-icon"><VinkoIconUsers size={26} /></div>
          <div className="eyebrow light">PARA PROFESIONALES Y OFICIOS</div>
          <h2>¿Quieres hacer crecer tu clientela e ingresos?</h2>
          <p>Únete a la comunidad de talento de SoyVinko. Conecta con nuevos clientes y cobra con total seguridad.</p>
          <Link to={user ? "/profile?tab=professional" : "/register"} className="button dark">
            Ofrecer mis servicios <ArrowRight size={16} />
          </Link>
        </div>
      </section>
    </>
  );
}
export function ProfessionalProfile() {
  const { id } = useParams(),
    { user } = useAuth();
  const data = useData(
    () =>
      user
        ? api("professional_profile", { professional: id })
        : api<Data[]>("discover", { limit: 100 }).then((rows) =>
            rows.find((p) => p.id === id),
          ),
    [id, user?.id],
  );
  const reviews = useData(
    () =>
      user
        ? api<Data[]>("professional_reviews", { professional: id })
        : Promise.resolve([]),
    [id, user?.id],
  );
  if (data.loading) return <Loading />;
  if (data.error) return <ErrorBox message={data.error} />;
  if (!data.data) return <Empty title="Perfil no disponible" />;
  const p = data.data;
  return (
    <>
      <Link className="back-link" to="/explore">
        ← Explorar profesionales
      </Link>
      <div className="profile-hero">
        <div className="avatar large">
          {p.name
            ?.split(" ")
            .map((s: string) => s[0])
            .slice(0, 2)
            .join("")}
        </div>
        <div>
          <div className="eyebrow">PROFESIONAL SOYVINKO</div>
          <h1>{p.name}</h1>
          <p>
            {p.years_experience
              ? `${p.years_experience} años de experiencia`
              : "Disponible para nuevos proyectos"}
          </p>
          <div className="row">
            {p.verified && (
              <span className="badge status-verified">
                <ShieldCheck size={14} /> Identidad verificada
              </span>
            )}
            <span>
              <Star size={14} /> {p.rating ?? "Sin calificaciones aún"}
            </span>
          </div>
        </div>
        {user && user.id !== id && (
          <RunButton
            run={() => api("set_favorite", { professional: id, enabled: true })}
          >
            <Heart size={18} /> Guardar
          </RunButton>
        )}
      </div>
      <div className="two-col">
        <div>
          <Section title="Conoce mi trabajo">
            <p>{p.bio}</p>
            <div className="profile-facts">
              <span>
                <ShieldCheck size={19} />{" "}
                {p.requires_platform_payment
                  ? "Trabaja con pago protegido"
                  : "Acuerda la forma de pago"}
              </span>
              <span>
                <Check size={19} /> Condiciones acordadas antes de empezar
              </span>
            </div>
          </Section>
          <Section title="Servicios">
            {p.offerings?.map((o: Data) => (
              <div className="list-row" key={o.id}>
                <div>
                  <Badge value={o.delivery_mode ?? "onsite"} />
                  <h3>{o.title}</h3>
                  <p>{o.description}</p>
                  <strong>{money(o.price_min_minor, o.currency)}</strong>
                </div>
                <Link
                  className="button primary"
                  to={
                    user
                      ? `/requests/new?professional=${id}&offering=${o.id}`
                      : "/login"
                  }
                >
                  Solicitar servicio <ArrowRight size={16} />
                </Link>
              </div>
            ))}
          </Section>
          <Section title="Opiniones y recomendaciones">
            {reviews.data?.length ? (
              reviews.data.map((r: Data) => (
                <div key={r.id} className="review">
                  <strong>{"★".repeat(r.rating)}</strong>
                  <p>{r.comment}</p>
                  {r.response && <blockquote>{r.response}</blockquote>}
                </div>
              ))
            ) : (
              <Empty
                title="Una nueva historia por escribir"
                text="Las opiniones aparecen después de un servicio terminado y revisado."
              />
            )}
          </Section>
        </div>
        <aside>
          <Section title="Así funciona">
            <ol className="steps">
              <li>Describe lo que necesitas.</li>
              <li>Recibe su cotización y confirma el precio.</li>
              <li>Asigna el trabajo y conversa con el profesional.</li>
              <li>Valida el trabajo y califica tu experiencia.</li>
            </ol>
            <p className="info-note">
              Tus datos de contacto se comparten después de aceptar. Tu
              ubicación exacta se envía solo cuando tú lo decides.
            </p>
          </Section>
          {p.branches?.length > 0 && (
            <Section title="Sedes">
              {p.branches.map((b: Data) => (
                <p key={b.id}>
                  <MapPin size={15} /> {b.name} · {b.city}
                  {b.address && <small>{b.address}</small>}
                </p>
              ))}
            </Section>
          )}
        </aside>
      </div>
    </>
  );
}
