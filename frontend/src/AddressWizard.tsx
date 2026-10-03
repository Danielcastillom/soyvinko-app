import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Home,
  LocateFixed,
  MapPin,
  Plus,
} from "lucide-react";
import { api, type Data } from "./api";
import { ErrorBox, Loading, Modal, useData } from "./components";
import { useDeviceLocation, type GeoPoint } from "./location";
import { mapboxTiles } from "./mapbox";

function AddressMap({
  area,
  point,
  onPick,
}: {
  area: Data;
  point: GeoPoint | null;
  onPick: (point: GeoPoint) => void;
}) {
  const geo = useDeviceLocation(),
    ref = useRef<HTMLDivElement>(null),
    mapRef = useRef<L.Map | null>(null),
    markerRef = useRef<L.Marker | null>(null),
    pickRef = useRef(onPick),
    [error, setError] = useState("");
  pickRef.current = onPick;
  useEffect(() => {
    if (!ref.current || !geo.config?.mapbox_public_token) return;
    setError("");
    const map = L.map(ref.current, {
      scrollWheelZoom: false,
      maxBounds: [
        [-1, -75],
        [17, -58],
      ],
      maxBoundsViscosity: 0.8,
    });
    mapRef.current = map;
    map.fitBounds(
      [
        [area.bbox[1], area.bbox[0]],
        [area.bbox[3], area.bbox[2]],
      ],
      { padding: [22, 22], maxZoom: 15 },
    );
    map.panTo([area.latitude, area.longitude], { animate: false });
    const tiles = mapboxTiles(geo.config.mapbox_public_token).addTo(map);
    tiles.on("tileerror", () =>
      setError(
        "No se pudo cargar parte del mapa. Comprueba tu conexión y vuelve a abrir este paso.",
      ),
    );
    tiles.on("tileload", () => setError(""));
    L.geoJSON(area.geometry, {
      style: { color: "#6F3DFF", weight: 2, fillOpacity: 0.08 },
      interactive: false,
    }).addTo(map);
    map.on("click", (e: L.LeafletMouseEvent) =>
      pickRef.current({ lat: e.latlng.lat, lon: e.latlng.lng }),
    );
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(ref.current);
    return () => {
      observer.disconnect();
      markerRef.current = null;
      mapRef.current = null;
      map.remove();
    };
  }, [area.code, geo.config?.mapbox_public_token]);
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !point) return;
    if (!markerRef.current) {
      markerRef.current = L.marker([point.lat, point.lon], {
        draggable: true,
        keyboard: true,
        title: "Dirección seleccionada",
        icon: L.divIcon({
          className: "address-map-pin",
          html: "<span></span>",
          iconSize: [32, 40],
          iconAnchor: [16, 40],
        }),
      }).addTo(map);
      markerRef.current.on("dragend", () => {
        const p = markerRef.current!.getLatLng();
        pickRef.current({ lat: p.lat, lon: p.lng });
      });
    } else markerRef.current.setLatLng([point.lat, point.lon]);
    if (map.getZoom() < 16 || !map.getBounds().contains([point.lat, point.lon]))
      map.setView([point.lat, point.lon], 16);
  }, [point?.lat, point?.lon, area.code, geo.config?.mapbox_public_token]);
  return (
    <div className="address-map-section">
      <div
        ref={ref}
        className="address-map"
        role="region"
        aria-label="Mapa para elegir la dirección"
      />
      {!geo.config?.mapbox_public_token && (
        <ErrorBox message="El mapa no está disponible en este momento. Vuelve a intentarlo." />
      )}
      {error && <ErrorBox message={error} />}
      <button
        className="button secondary small"
        type="button"
        onClick={() => {
          const center = mapRef.current?.getCenter();
          if (center) onPick({ lat: center.lat, lon: center.lng });
        }}
      >
        <MapPin size={16} /> Fijar en el centro del mapa
      </button>
      <small className="field-hint">
        Toca el lugar exacto o arrastra el marcador. La zona delimitada
        corresponde a {area.name}.
      </small>
    </div>
  );
}

export function AddressWizard({
  kind = "address",
  onClose,
  onSaved,
}: {
  kind?: "address" | "branch" | "search";
  onClose: () => void;
  onSaved: (point?: GeoPoint, area?: Data) => void;
}) {
  const geo = useDeviceLocation(),
    [step, setStep] = useState(0),
    [name, setName] = useState(""),
    [state, setState] = useState(""),
    [municipality, setMunicipality] = useState(""),
    [parish, setParish] = useState(""),
    [point, setPoint] = useState<GeoPoint | null>(null),
    [line, setLine] = useState(""),
    [instructions, setInstructions] = useState(""),
    [publicAddress, setPublicAddress] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const states = useData(() => api<Data[]>("geo_states"), []),
    municipalities = useData(
      () =>
        state
          ? api<Data[]>("geo_municipalities", { state })
          : Promise.resolve([]),
      [state],
    ),
    parishes = useData(
      () =>
        municipality
          ? api<Data[]>("geo_parishes", { municipality })
          : Promise.resolve([]),
      [municipality],
    ),
    detail = useData(
      () =>
        parish ? api<Data>("geo_parish", { parish }) : Promise.resolve(null),
      [parish],
    );
  const steps = [
    kind === "search" ? "País" : "Nombre y país",
    "Estado",
    "Municipio",
    "Parroquia",
    "Ubicación exacta",
  ];
  const area = detail.data?.code === parish ? detail.data : null;
  const ready =
    step === 0
      ? kind === "search" || !!name.trim()
      : step === 1
        ? !!state
        : step === 2
          ? !!municipality
          : step === 3
            ? !!parish
            : !!point &&
              !!area &&
              (kind === "search" || line.trim().length >= 3);
  const pick = (p: GeoPoint) => {
    setPoint(p);
    setError("");
  };
  async function currentLocation() {
    setBusy(true);
    setError("");
    try {
      const p = await geo.locate();
      const resolved = await api<Data | null>("locate_parish", {
        lat: p.lat,
        lon: p.lon,
      });
      if (!resolved)
        throw new Error(
          "Tu ubicación está fuera de Venezuela. Selecciona la zona manualmente.",
        );
      setState(resolved.state_code);
      setMunicipality(resolved.municipality_code);
      setParish(resolved.parish_code);
      setPoint(p);
      setStep(kind === "search" || name.trim() ? 4 : 0);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!ready || busy) return;
    setError("");
    if (step < 4) {
      setStep(step + 1);
      return;
    }
    setBusy(true);
    try {
      if (kind === "search") {
        const resolved = await api<Data | null>("locate_parish", {
          lat: point!.lat,
          lon: point!.lon,
        });
        if (!resolved || resolved.parish_code !== parish)
          throw new Error(
            "Elige un punto dentro de la parroquia seleccionada.",
          );
        onSaved(point!, resolved);
      } else {
        const values = {
          country: "VE",
          state,
          municipality,
          parish,
          line: line.trim(),
          lon: point!.lon,
          lat: point!.lat,
        };
        if (kind === "branch")
          await api("add_branch_ve", {
            ...values,
            name: name.trim(),
            public_address: publicAddress,
          });
        else
          await api("add_address_ve", {
            ...values,
            label: name.trim(),
            instructions: instructions.trim() || undefined,
          });
        onSaved();
      }
      onClose();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={
        kind === "branch"
          ? "Añadir sede"
          : kind === "search"
            ? "Elegir zona de búsqueda"
            : "Añadir dirección"
      }
      onClose={onClose}
    >
      <form className="address-wizard" onSubmit={submit}>
        <ol className="address-steps" aria-label="Pasos para elegir dirección">
          {steps.map((label, i) => (
            <li
              key={label}
              className={i === step ? "current" : i < step ? "complete" : ""}
              aria-current={i === step ? "step" : undefined}
            >
              <span>{i < step ? <Check size={12} /> : i + 1}</span>
              <small>{label}</small>
            </li>
          ))}
        </ol>
        <div className="address-step-heading">
          <span>Paso {step + 1} de 5</span>
          <h3>{steps[step]}</h3>
        </div>
        {step === 0 && (
          <div className="fields-form">
            {kind !== "search" && (
              <>
                <label>
                  Nombre para esta {kind === "branch" ? "sede" : "dirección"} *
                  <input
                    autoFocus
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Ej. Casa, Trabajo, Negocio"
                    maxLength={80}
                    required
                    autoComplete="off"
                  />
                </label>
                <div className="address-presets">
                  {["Casa", "Trabajo", "Negocio"].map((label) => (
                    <button
                      key={label}
                      className={name === label ? "selected" : ""}
                      type="button"
                      onClick={() => setName(label)}
                    >
                      <Home size={14} />
                      {label}
                    </button>
                  ))}
                </div>
              </>
            )}
            <label>
              País
              <select aria-label="País" value="VE" disabled>
                <option value="VE">Venezuela</option>
              </select>
              <small className="field-hint">
                Actualmente disponible únicamente en Venezuela.
              </small>
            </label>
          </div>
        )}
        {step === 1 && (
          <div className="fields-form">
            <label>
              Estado o entidad federal *
              <select
                autoFocus
                value={state}
                required
                disabled={states.loading}
                onChange={(e) => {
                  setState(e.target.value);
                  setMunicipality("");
                  setParish("");
                  setPoint(null);
                }}
              >
                <option value="">Selecciona tu estado</option>
                {states.data?.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            {states.error && (
              <ErrorBox message={states.error} retry={states.reload} />
            )}
          </div>
        )}
        {step === 2 && (
          <div className="fields-form">
            <label>
              Municipio *
              <select
                autoFocus
                value={municipality}
                required
                disabled={municipalities.loading}
                onChange={(e) => {
                  setMunicipality(e.target.value);
                  setParish("");
                  setPoint(null);
                }}
              >
                <option value="">Selecciona tu municipio</option>
                {municipalities.data
                  ?.filter((m) => m.state_code === state)
                  .map((m) => (
                    <option key={m.code} value={m.code}>
                      {m.name}
                    </option>
                  ))}
              </select>
            </label>
            {municipalities.error && (
              <ErrorBox
                message={municipalities.error}
                retry={municipalities.reload}
              />
            )}
          </div>
        )}
        {step === 3 && (
          <div className="fields-form">
            <label>
              Parroquia *
              <select
                autoFocus
                value={parish}
                required
                disabled={parishes.loading}
                onChange={(e) => {
                  setParish(e.target.value);
                  setPoint(null);
                }}
              >
                <option value="">Selecciona tu parroquia</option>
                {parishes.data
                  ?.filter((p) => p.municipality_code === municipality)
                  .map((p) => (
                    <option key={p.code} value={p.code}>
                      {p.name}
                    </option>
                  ))}
              </select>
            </label>
            {parishes.error && (
              <ErrorBox message={parishes.error} retry={parishes.reload} />
            )}
          </div>
        )}
        {step === 4 && (
          <>
            {detail.loading ? (
              <Loading />
            ) : detail.error ? (
              <ErrorBox message={detail.error} retry={detail.reload} />
            ) : (
              area && (
                <>
                  <p className="address-summary">
                    {name && <strong>{name} · </strong>}
                    {area.name}, {area.municipality_name}, {area.state_name},
                    Venezuela
                  </p>
                  <AddressMap area={area} point={point} onPick={pick} />
                  <p
                    className={
                      point
                        ? "address-pin-status selected"
                        : "address-pin-status"
                    }
                  >
                    {point ? (
                      <>
                        <Check size={16} /> Punto seleccionado. Puedes ajustarlo
                        arrastrando el marcador.
                      </>
                    ) : (
                      <>
                        <MapPin size={16} /> Selecciona el lugar exacto en el
                        mapa para continuar.
                      </>
                    )}
                  </p>
                  {kind !== "search" && (
                    <div className="fields-form">
                      <label>
                        Dirección y número *
                        <input
                          value={line}
                          onChange={(e) => setLine(e.target.value)}
                          placeholder="Av. principal, edificio, piso o número de casa"
                          minLength={3}
                          maxLength={300}
                          required
                          autoComplete="street-address"
                        />
                      </label>
                      {kind === "address" ? (
                        <label>
                          Indicaciones para llegar{" "}
                          <span className="muted">(opcional)</span>
                          <textarea
                            value={instructions}
                            onChange={(e) => setInstructions(e.target.value)}
                            placeholder="Punto de referencia, entrada, local…"
                            rows={2}
                            maxLength={1000}
                          />
                        </label>
                      ) : (
                        <label className="check-field">
                          <input
                            type="checkbox"
                            checked={publicAddress}
                            onChange={(e) => setPublicAddress(e.target.checked)}
                          />
                          Mostrar esta dirección en mi perfil público
                        </label>
                      )}
                    </div>
                  )}
                </>
              )
            )}
          </>
        )}
        <button
          className="link-button address-geolocate"
          type="button"
          disabled={busy}
          onClick={() => {
            void currentLocation();
          }}
        >
          <LocateFixed size={17} /> Usar mi ubicación actual
        </button>
        {error && <ErrorBox message={error} />}
        <div className="address-wizard-actions">
          <button
            type="button"
            className="button secondary"
            disabled={busy}
            onClick={() =>
              step ? (setStep(step - 1), setError("")) : onClose()
            }
          >
            <ArrowLeft size={16} />
            {step ? "Anterior" : "Cancelar"}
          </button>
          <button className="button primary" disabled={!ready || busy}>
            {busy ? (
              "Procesando…"
            ) : step < 4 ? (
              <>
                Continuar <ArrowRight size={16} />
              </>
            ) : (
              <>
                <Check size={16} />
                {kind === "search"
                  ? "Buscar aquí"
                  : kind === "branch"
                    ? "Guardar sede"
                    : "Guardar dirección"}
              </>
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export function AddAddressButton({
  kind = "address",
  after,
}: {
  kind?: "address" | "branch";
  after: () => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button className="button secondary" onClick={() => setOpen(true)}>
        <Plus size={16} />
        {kind === "branch" ? "Añadir sede" : "Añadir dirección"}
      </button>
      {open && (
        <AddressWizard
          kind={kind}
          onClose={() => setOpen(false)}
          onSaved={after}
        />
      )}
    </>
  );
}
