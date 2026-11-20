# Marcadores en Explorar

Explorar abre en la pestana `Listado`. La pestana independiente `Ver mapa` muestra los profesionales presenciales de los resultados filtrados, la ubicacion del navegador en azul y, cuando es diferente, la zona de busqueda en amarillo. Solo se presenta una vista a la vez y los filtros se conservan al cambiar de pestana. Los controles permiten centrar el mapa en la ubicacion actual y encuadrar todos los puntos. La modalidad remota selecciona el listado y deshabilita la pestana de mapa.

Cada marcador abre una ficha con el nombre, el servicio, la ubicacion publica o aproximada y el enlace al perfil. Los profesionales que comparten coordenadas se agrupan en un marcador con contador; la ficha permite acceder a todos ellos. La zona manual o la direccion guardada siguen siendo el origen de busqueda aunque se centre el mapa en el dispositivo.

## Consulta y privacidad

La migracion `027_explore_map_markers.sql` agrega `api.professional_map_markers`. El registro HTTP expone `POST /api/v1/public/professional-map-markers`. Recibe hasta 100 identificadores en `professionals`, categoria opcional, coordenadas opcionales de busqueda y radio. Solo devuelve perfiles activos con ofertas presenciales activas, moneda operativa, categoria activa y cobertura compatible. La web divide en lotes los resultados que superan ese limite.

Las sedes publicas devuelven su punto exacto y direccion. Las privadas devuelven unicamente el punto de referencia del catalogo de su parroquia y `approximate: true`; no se deriva un punto de su coordenada privada. Una sede privada antigua sin parroquia se omite. No se exponen nombres privados de sedes ni se modifican sus permisos. Las respuestas anteriores de busqueda y sus distancias permanecen intactas.

La ubicacion del dispositivo utiliza el proveedor existente, con permiso del navegador y actualizaciones mientras la pagina esta visible. Los marcadores de resultados anteriores se retiran al cambiar filtros o durante un error. La consulta admite reintento; sin permiso de ubicacion se conserva la seleccion manual y no se inventa una posicion.

## Verificacion

- `.venv/bin/pytest -q tests/test_explore_map.py tests/test_venezuela.py` desde `backend`: 18 pruebas aprobadas en base desechable.
- `npm run build` desde `frontend`: aprobado.
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:5180 npx playwright test e2e/explore-map.spec.ts`: tres recorridos de escritorio y movil. Los listados y marcadores son controlados por las pruebas; Mapbox y el catalogo geografico se consultan realmente.
- Capturas: `output/explore-map-desktop.png` y `output/explore-map-mobile.png`.

## Activacion

Publicado el 11 de septiembre de 2026 en `https://contrata.contupana.pro/explore`, con autorizacion del usuario. Se aplico la migracion 027 y se actualizaron API y web. Ambos contenedores quedaron saludables, y las comprobaciones publicas de salud, web y nueva consulta devolvieron HTTP 200.

Los dos recorridos con datos publicados pasaron: direccion guardada con 30 profesionales y tres al filtrar plomeria; ubicacion del navegador simulada en Juan Ignacio Montilla con 27 profesionales dentro de cobertura y tres al filtrar plomeria. Los marcadores coincidieron con los resultados. Se verificaron tambien los enlaces de perfil y el ancho movil. No se simularon respuestas de API en estos recorridos.

Respaldo previo: `backups/chamba-20260911T131848Z.dump`. Imagenes anteriores: `chamba-api:before-explore-map-20260911` y `chamba-web:before-explore-map-20260911`. La base, el volumen y el trabajador conservaron sus contenedores. Las capturas publicadas estan en `output/explore-map-live-desktop.png` y `output/explore-map-live-mobile.png`.
