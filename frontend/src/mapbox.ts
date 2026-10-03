import L from "leaflet";
export function mapboxTiles(token: string) {
  return L.tileLayer(
    `https://api.mapbox.com/styles/v1/mapbox/streets-v12/tiles/512/{z}/{x}/{y}?access_token=${encodeURIComponent(token)}`,
    {
      tileSize: 512,
      zoomOffset: -1,
      maxZoom: 20,
      attribution:
        '© <a href="https://www.mapbox.com/about/maps/" target="_blank" rel="noopener">Mapbox</a> © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> · <a href="https://apps.mapbox.com/feedback/" target="_blank" rel="noopener">Mejorar el mapa</a>',
    },
  );
}
