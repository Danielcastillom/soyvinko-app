import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { MapPin, LocateFixed } from "lucide-react";
import { api, request, type Data } from "./api";

export type GeoPoint = {
  lat: number;
  lon: number;
  accuracy?: number;
  timestamp?: number;
};
type LocationState = {
  point: GeoPoint | null;
  area: Data | null;
  config: Data | null;
  status: "locating" | "ready" | "denied" | "unavailable" | "outside";
  error: string;
  locate: () => Promise<GeoPoint>;
};
const LocationContext = createContext<LocationState | null>(null);
const options = { enableHighAccuracy: true, timeout: 15000, maximumAge: 30000 };
const positionPoint = (p: GeolocationPosition): GeoPoint => ({
  lat: p.coords.latitude,
  lon: p.coords.longitude,
  accuracy: p.coords.accuracy,
  timestamp: p.timestamp,
});

export function LocationProvider({ children }: { children: ReactNode }) {
  const [point, setPoint] = useState<GeoPoint | null>(null),
    [area, setArea] = useState<Data | null>(null),
    [config, setConfig] = useState<Data | null>(null),
    [status, setStatus] = useState<LocationState["status"]>("locating"),
    [error, setError] = useState("");
  const publish = useCallback((p: GeolocationPosition, force = false) => {
    const next = positionPoint(p);
    setPoint((previous) => {
      if (
        !force &&
        previous &&
        Math.hypot(
          (previous.lat - next.lat) * 111000,
          (previous.lon - next.lon) * 109000,
        ) < 100 &&
        (previous.accuracy ?? Infinity) <= (next.accuracy ?? Infinity) &&
        Date.now() - (previous.timestamp ?? 0) < 60000
      )
        return previous;
      return next;
    });
  }, []);
  const failed = useCallback((e: GeolocationPositionError) => {
    setPoint(null);
    setArea(null);
    setStatus(e.code === 1 ? "denied" : "unavailable");
    setError(
      e.code === 1
        ? "Puedes permitir la ubicación en tu navegador o elegir una zona en el mapa."
        : "No pudimos obtener tu ubicación. Puedes reintentar o elegir una zona.",
    );
  }, []);
  const locate = useCallback(
    () =>
      new Promise<GeoPoint>((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error("Este navegador no ofrece geolocalización."));
          return;
        }
        setStatus("locating");
        navigator.geolocation.getCurrentPosition(
          (p) => {
            publish(p, true);
            resolve(positionPoint(p));
          },
          (e) => {
            failed(e);
            reject(
              new Error(
                e.code === 1
                  ? "Permite la ubicación en tu navegador o selecciona el punto en el mapa."
                  : "No pudimos obtener tu ubicación. Selecciona el punto en el mapa.",
              ),
            );
          },
          options,
        );
      }),
    [publish, failed],
  );
  useEffect(() => {
    let active = true;
    request("/api/v1/config")
      .then((data) => {
        if (active) setConfig(data);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    if (!navigator.geolocation) {
      setStatus("unavailable");
      return;
    }
    let watch: number | null = null;
    const start = () => {
      if (!document.hidden && watch === null)
        watch = navigator.geolocation.watchPosition(publish, failed, options);
    };
    const stop = () => {
      if (watch !== null) navigator.geolocation.clearWatch(watch);
      watch = null;
    };
    const visibility = () => {
      if (document.hidden) stop();
      else start();
    };
    start();
    document.addEventListener("visibilitychange", visibility);
    return () => {
      stop();
      document.removeEventListener("visibilitychange", visibility);
    };
  }, [publish, failed]);
  useEffect(() => {
    if (!point) return;
    let active = true;
    setArea(null);
    api<Data | null>("locate_parish", { lat: point.lat, lon: point.lon })
      .then((result) => {
        if (!active) return;
        setArea(result);
        setStatus(result ? "ready" : "outside");
        setError(
          result
            ? ""
            : "Los servicios presenciales están disponibles actualmente en Venezuela.",
        );
      })
      .catch(() => {
        if (active) {
          setStatus("unavailable");
          setError(
            "No pudimos identificar tu zona. Puedes elegirla manualmente.",
          );
        }
      });
    return () => {
      active = false;
    };
  }, [point]);
  return (
    <LocationContext.Provider
      value={{ point, area, config, status, error, locate }}
    >
      {children}
    </LocationContext.Provider>
  );
}
export function useDeviceLocation() {
  const value = useContext(LocationContext);
  if (!value) throw new Error("LocationProvider requerido");
  return value;
}
export function LocationStatus() {
  const geo = useDeviceLocation();
  return (
    <div className="location-status" aria-live="polite">
      <MapPin size={16} />
      <span>
        {geo.area
          ? `${geo.area.parish_name}, ${geo.area.state_name}`
          : geo.status === "locating"
            ? "Buscando tu ubicación…"
            : "Venezuela"}
      </span>
      {!geo.area && geo.status !== "locating" && (
        <button
          type="button"
          className="link-button"
          onClick={() => {
            void geo.locate().catch(() => {});
          }}
        >
          <LocateFixed size={15} /> Usar mi ubicación
        </button>
      )}
    </div>
  );
}
