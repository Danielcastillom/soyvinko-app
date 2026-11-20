import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { LocateFixed, Maximize, LoaderCircle } from "lucide-react";
import { api, type Data } from "./api";
import { ErrorBox, useData } from "./components";
import { useDeviceLocation, type GeoPoint } from "./location";
import { mapboxTiles } from "./mapbox";

type ServiceMarker = {
  professional_id: string;
  lat: number;
  lon: number;
  approximate: boolean;
  label: string;
};
const validPoint = (point: GeoPoint | null): point is GeoPoint =>
  !!point &&
  Number.isFinite(point.lat) &&
  Number.isFinite(point.lon) &&
  Math.abs(point.lat) <= 90 &&
  Math.abs(point.lon) <= 180;

export function ExploreMap({
  profiles,
  searchPoint,
  category,
  radius,
  loading,
}: {
  profiles: Data[];
  searchPoint: GeoPoint | null;
  category: string;
  radius: number;
  loading: boolean;
}) {
  const geo = useDeviceLocation();
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const servicesRef = useRef<L.LayerGroup | null>(null);
  const locationRef = useRef<L.LayerGroup | null>(null);
  const boundsRef = useRef<L.LatLngBounds | null>(null);
  const fitKeyRef = useRef("");
  const [tileError, setTileError] = useState("");
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState("");
  const token = geo.config?.mapbox_public_token;
  const ids = profiles
    .filter((p) =>
      p.offerings?.some(
        (o: Data) =>
          o.delivery_mode === "onsite" &&
          (!category || o.category_id === category),
      ),
    )
    .map((p) => p.id as string)
    .sort();
  const idsKey = JSON.stringify(ids);
  const requestKey = JSON.stringify([
    idsKey,
    searchPoint?.lat,
    searchPoint?.lon,
    category,
    radius,
    loading,
  ]);
  const markers = useData(async () => {
    const rows: ServiceMarker[] = [];
    if (!loading) {
      for (let i = 0; i < ids.length; i += 100) {
        rows.push(
          ...(await api<ServiceMarker[]>("professional_map_markers", {
            professionals: ids.slice(i, i + 100),
            category: category || undefined,
            lat: searchPoint?.lat,
            lon: searchPoint?.lon,
            radius_m: radius,
          })),
        );
      }
    }
    return { key: requestKey, rows };
  }, [requestKey]);
  const currentMarkers =
    !loading &&
    !markers.loading &&
    !markers.error &&
    markers.data?.key === requestKey
      ? markers.data.rows.filter(
          (m) => validPoint(m) && ids.includes(m.professional_id),
        )
      : [];
  const shownCount = new Set(currentMarkers.map((m) => m.professional_id)).size;

  function fitAll() {
    if (boundsRef.current?.isValid())
      mapRef.current?.fitBounds(boundsRef.current, {
        padding: [40, 40],
        maxZoom: 15,
      });
  }

  useEffect(() => {
    if (!ref.current || !token) return;
    const map = L.map(ref.current, {
      scrollWheelZoom: false,
      zoomControl: false,
      zoomAnimation: false,
    }).setView([8, -66], 6);
    L.control
      .zoom({
        position: "bottomright",
        zoomInTitle: "Acercar",
        zoomOutTitle: "Alejar",
      })
      .addTo(map);
    mapRef.current = map;
    servicesRef.current = L.layerGroup().addTo(map);
    locationRef.current = L.layerGroup().addTo(map);
    fitKeyRef.current = "";
    const tiles = mapboxTiles(token).addTo(map);
    tiles.on("tileerror", () =>
      setTileError("No se pudo cargar parte del mapa. Revisa tu conexión."),
    );
    tiles.on("tileload", () => setTileError(""));
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(ref.current);
    return () => {
      observer.disconnect();
      map.remove();
      mapRef.current = null;
      servicesRef.current = null;
      locationRef.current = null;
    };
  }, [token]);

  useEffect(() => {
    const map = mapRef.current,
      layer = servicesRef.current;
    if (!map || !layer) return;
    layer.clearLayers();
    const groups = new Map<string, ServiceMarker[]>();
    currentMarkers.forEach((marker) => {
      const key = `${marker.lat},${marker.lon}`;
      groups.set(key, [...(groups.get(key) ?? []), marker]);
    });
    groups.forEach((group) => {
      const unique = [
        ...new Map(group.map((m) => [m.professional_id, m])).values(),
      ];
      const content = document.createElement("div");
      content.className = "explore-map-popup";
      unique.forEach((m) => {
        const profile = profiles.find((p) => p.id === m.professional_id)!;
        const item = document.createElement("div");
        const name = document.createElement("strong");
        name.textContent = profile.name;
        const service = document.createElement("span");
        service.textContent =
          profile.offerings?.find(
            (o: Data) =>
              o.delivery_mode === "onsite" &&
              (!category || o.category_id === category),
          )?.title ?? "Servicio presencial";
        const location = document.createElement("small");
        location.textContent = `${m.approximate ? "Zona aproximada" : "Dirección pública"} · ${m.label}`;
        const link = document.createElement("a");
        link.href = `/professionals/${encodeURIComponent(profile.id)}`;
        link.textContent = "Ver perfil";
        item.append(name, service, location, link);
        content.append(item);
      });
      const title =
        unique.length > 1
          ? `${unique.length} profesionales en esta zona`
          : profiles.find((p) => p.id === unique[0].professional_id)!.name;
      const label = document.createElement("span");
      label.textContent = unique.length > 1 ? String(unique.length) : "";
      const marker = L.marker([group[0].lat, group[0].lon], {
        title,
        alt: title,
        keyboard: true,
        icon: L.divIcon({
          className: "explore-service-pin",
          html: label,
          iconSize: [36, 36],
          iconAnchor: [18, 36],
          popupAnchor: [0, -32],
        }),
      })
        .addTo(layer)
        .bindPopup(content, { maxWidth: 280, maxHeight: 260 });
      marker.getElement()?.setAttribute("aria-label", title);
    });
  }, [
    token,
    markers.data,
    markers.loading,
    markers.error,
    requestKey,
    profiles,
  ]);

  useEffect(() => {
    const layer = locationRef.current;
    if (!layer) return;
    layer.clearLayers();
    if (validPoint(searchPoint)) {
      L.circle([searchPoint.lat, searchPoint.lon], {
        radius,
        color: "#6F3DFF",
        weight: 1,
        dashArray: "5 6",
        fillOpacity: 0.04,
        interactive: false,
      }).addTo(layer);
      if (
        !validPoint(geo.point) ||
        L.latLng(searchPoint.lat, searchPoint.lon).distanceTo([
          geo.point.lat,
          geo.point.lon,
        ]) > 1
      ) {
        L.marker([searchPoint.lat, searchPoint.lon], {
          title: "Zona de búsqueda",
          alt: "Zona de búsqueda",
          zIndexOffset: 500,
          icon: L.divIcon({
            className: "explore-search-pin",
            html: "<span></span>",
            iconSize: [24, 24],
            iconAnchor: [12, 12],
          }),
        })
          .addTo(layer)
          .bindPopup("Zona de búsqueda");
      }
    }
    if (validPoint(geo.point)) {
      const position: L.LatLngTuple = [geo.point.lat, geo.point.lon];
      if (Number.isFinite(geo.point.accuracy) && geo.point.accuracy! > 0)
        L.circle(position, {
          radius: geo.point.accuracy,
          color: "#42C9FF",
          weight: 1,
          fillOpacity: 0.12,
          interactive: false,
        }).addTo(layer);
      L.marker(position, {
        title: "Tu ubicación actual",
        alt: "Tu ubicación actual",
        zIndexOffset: 1000,
        icon: L.divIcon({
          className: "explore-location-pin",
          html: "<span></span>",
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        }),
      })
        .addTo(layer)
        .bindPopup("Tu ubicación actual");
    }
  }, [token, geo.point, searchPoint?.lat, searchPoint?.lon, radius]);

  useEffect(() => {
    const bounds = L.latLngBounds([]);
    currentMarkers.forEach((m) => bounds.extend([m.lat, m.lon]));
    if (validPoint(searchPoint))
      bounds.extend([searchPoint.lat, searchPoint.lon]);
    if (validPoint(geo.point)) bounds.extend([geo.point.lat, geo.point.lon]);
    boundsRef.current = bounds;
    const key = JSON.stringify([
      currentMarkers,
      searchPoint?.lat,
      searchPoint?.lon,
      radius,
      !!geo.point,
    ]);
    if (key !== fitKeyRef.current && bounds.isValid()) {
      fitKeyRef.current = key;
      fitAll();
    }
  }, [
    token,
    markers.data,
    markers.loading,
    markers.error,
    requestKey,
    geo.point,
  ]);

  async function locate() {
    setLocating(true);
    setLocationError("");
    try {
      const point = await geo.locate();
      mapRef.current?.setView([point.lat, point.lon], 16);
    } catch (error) {
      setLocationError((error as Error).message);
    } finally {
      setLocating(false);
    }
  }

  return (
    <section
      className="explore-map-section"
      aria-label="Profesionales en el mapa"
    >
      <div className="explore-map-toolbar">
        <div className="explore-map-legend">
          <span>
            <i className="service-dot" /> Servicios
          </span>
          <span>
            <i className="location-dot" /> Tu ubicación
          </span>
          {searchPoint && (
            <span>
              <i className="search-dot" /> Zona de búsqueda
            </span>
          )}
        </div>
        <div className="explore-map-actions">
          <button
            className="icon-button"
            type="button"
            title="Ver todos los marcadores"
            aria-label="Ver todos los marcadores"
            disabled={!token}
            onClick={fitAll}
          >
            <Maximize size={18} />
          </button>
          <button
            className="icon-button"
            type="button"
            title="Centrar en mi ubicación"
            aria-label="Centrar en mi ubicación"
            disabled={locating || !token}
            onClick={() => void locate()}
          >
            {locating ? (
              <LoaderCircle size={18} className="spin" />
            ) : (
              <LocateFixed size={18} />
            )}
          </button>
        </div>
      </div>
      <div
        ref={ref}
        className="explore-map"
        role="region"
        aria-label="Mapa de servicios cercanos"
      />
      <div className="explore-map-status" role="status">
        {loading || markers.loading
          ? "Cargando ubicaciones…"
          : markers.error
            ? "Ubicaciones no disponibles"
            : shownCount
              ? `${shownCount} ${shownCount === 1 ? "profesional en el mapa" : "profesionales en el mapa"}`
              : "No hay servicios con ubicación para estos filtros."}
        {currentMarkers.some((m) => m.approximate) && (
          <span>Las sedes privadas muestran una zona aproximada.</span>
        )}
      </div>
      {(!geo.point || locationError) && geo.status !== "locating" && (
        <p className="explore-map-location-status">
          {locationError || geo.error}
        </p>
      )}
      {!token && (
        <ErrorBox message="El mapa no está disponible en este momento." />
      )}
      {tileError && <ErrorBox message={tileError} />}
      {markers.error && (
        <ErrorBox message={markers.error} retry={markers.reload} />
      )}
    </section>
  );
}
