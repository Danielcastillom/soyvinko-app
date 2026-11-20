# Venezuela, direcciones y búsqueda por ubicación

Publicado el 11 de septiembre de 2026 en [contrata.contupana.pro](https://contrata.contupana.pro/explore). El país activo es Venezuela (`VE`), la configuración regional es `es-VE` y la zona horaria predeterminada es `America/Caracas`. Las operaciones nuevas admiten bolívares (`VES`, símbolo `Bs`) y dólares estadounidenses (`USD`, símbolo `US$`), con importes enteros en unidades menores y dos decimales en pantalla.

## Direcciones y mapa

El asistente de direcciones y sedes sigue cinco pasos: nombre libre y país, estado o entidad federal, municipio, parroquia y ubicación exacta. Los nombres sugeridos Casa, Trabajo y Negocio son editables. Venezuela es la única opción de país habilitada. Cada selector carga exclusivamente los hijos de la selección anterior; cambiar un padre borra sus descendientes y el marcador anterior.

Al elegir parroquia se presenta su límite en un mapa Mapbox. El usuario marca un punto con un clic, arrastra el marcador o usa el botón accesible para fijarlo en el centro del mapa. Debe indicar dirección y número antes de guardar; las indicaciones para llegar son opcionales. El backend comprueba la jerarquía completa y que el punto pertenezca a la parroquia seleccionada, con una tolerancia de 100 metros en límites cartográficos. Las sedes nuevas son privadas salvo elección expresa de dirección pública.

`frontend/src/AddressWizard.tsx` sirve para direcciones personales, sedes profesionales y búsqueda manual anónima. Las direcciones se guardan solamente al confirmar. Las consultas geográficas usan funciones SQL con permisos explícitos; las tablas de catálogo tienen RLS y no admiten acceso directo del rol de la aplicación.

## Catálogo y procedencia

Fuente: **Instituto Nacional de Estadística / OCHA Venezuela, COD-AB v01**, distribuido por [Humanitarian Data Exchange](https://data.humdata.org/dataset/cod-ab-ven), licencia **CC BY-IGO 3.0**. Se conserva la jerarquía, los códigos y los nombres de esa edición. OCHA indica límites de origen del 2 de mayo de 2018, aptos para uso humanitario desde el 23 de febrero de 2021 y revisados el 17 de enero de 2025; la edición descargada se publicó en agosto de 2026. No se presenta la fecha de descarga como fecha de actualización de todos los límites.

El [archivo GeoJSON original](https://data.humdata.org/dataset/5b141d29-534f-4f01-a0bc-41e2f375d925/resource/93b05eca-adbd-4ce3-bae3-b4d6d64fac8b/download/ven_admin_boundaries.geojson.zip) se descargó el 11 de septiembre de 2026. Su SHA-256 está fijado en la cabecera de `db/migrations/023_venezuela_catalog.sql`.

| Tabla | Registros | Composición |
|---|---:|---|
| `app.geo_countries` | 1 | Venezuela |
| `app.geo_states` | 25 | 23 estados, Distrito Capital y Dependencias Federales |
| `app.geo_municipalities` | 336 | 335 municipios y la agrupación de Dependencias Federales |
| `app.geo_parishes` | 1 135 | 1 134 parroquias y la agrupación de Dependencias Federales |

Las agrupaciones de Dependencias Federales están identificadas mediante `kind`; no se cuentan como un estado, municipio o parroquia ordinarios. No existen hijos huérfanos ni municipios sin registros subordinados en este catálogo. Los polígonos se reparan y simplifican preservando topología, con tolerancia de 0,00015 grados y coordenadas a seis decimales. El punto de referencia de cada parroquia queda dentro de su polígono.

El generador `scripts/import_venezuela_geography.py` requiere Shapely 2.1.2 solamente durante desarrollo. Recibe el ZIP original y se niega a sobrescribir una migración existente. Para una futura actualización se debe preparar una migración nueva, revisar cambios de códigos y relaciones y conservar las direcciones históricas; no modificar la migración 023 ya aplicada.

## Ubicación del navegador y privacidad

`frontend/src/location.tsx` solicita la ubicación mediante la API del navegador y la actualiza mientras la página está visible. El permiso depende del navegador y del usuario. Al ocultar o cerrar la página se detiene el seguimiento. Se reducen las actualizaciones por movimientos menores a 100 metros durante 60 segundos si la precisión no mejora. Un reintento manual vuelve a consultar la zona incluso con coordenadas idénticas después de un fallo temporal.

La posición de exploración se mantiene en memoria: no se guarda en localStorage ni en una dirección o historial del servidor. Se envía en el cuerpo de consultas POST de localización y búsqueda, sin coordenadas en la URL. El proxy registra rutas sin parámetros. Compartir la ubicación dentro de un contrato sigue requiriendo el consentimiento explícito y temporal del cliente, independiente del permiso del navegador.

Sin permiso, ante un fallo o fuera de Venezuela, se puede elegir manualmente una zona venezolana en el mapa. No se inventa una ubicación precisa. La búsqueda cercana considera categoría, distancia, cobertura, disponibilidad y verificación del profesional. Devuelve distancias redondeadas a 100 metros y no expone las coordenadas exactas de sedes privadas. Los servicios remotos siguen disponibles sin dirección presencial.

## Mapbox y API

`MAPBOX_PUBLIC_TOKEN` se configura en `backend/.env`. Debe ser un token público `pk.` y el backend lo entrega en `/api/v1/config`; nunca se utiliza un token secreto `sk.` en el navegador. El valor concreto no se incluye en esta documentación. La política de imágenes permite `https://api.mapbox.com`, y la página publicada permite geolocalización de su propio origen sobre HTTPS.

El cliente Leaflet solicita mapas Streets v12 mediante [Mapbox Static Tiles API](https://docs.mapbox.com/api/maps/static-tiles/), con teselas de 512 píxeles y la atribución Mapbox/OpenStreetMap visible. No se usa un geocodificador Mapbox ni se almacenan resultados de geocodificación; los límites administrativos proceden del catálogo INE/OCHA. La selección precisa requiere interacción con el mapa.

| Operación | Método y ruta |
|---|---|
| Estados | `GET /api/v1/public/geo-states` |
| Municipios del estado | `GET /api/v1/public/geo-municipalities` |
| Parroquias del municipio | `GET /api/v1/public/geo-parishes` |
| Límite y referencia de la parroquia | `GET /api/v1/public/geo-parish` |
| Identificar parroquia del punto | `POST /api/v1/public/locate-parish` |
| Profesionales cercanos | `POST /api/v1/public/nearby-professionals` |
| Guardar dirección propia | `POST /api/v1/account/add-address` |
| Guardar sede profesional | `POST /api/v1/professionals/add-branch` |

Las dos rutas de alta existentes usan ahora `api.add_address_ve` y `api.add_branch_ve`. El contrato de campos completo está en [OpenAPI](openapi.json). Las funciones SQL anteriores se conservan para compatibilidad interna, sin ruta HTTP.

## Monedas e instalación

Las migraciones 022–024 incorporan la geografía, el catálogo fijado y las monedas operativas. No convierten dinero ni cambian la moneda de contratos, compras, movimientos o wallets históricos. Los saldos previos se pueden consultar conservando su denominación original. Los nuevos servicios, solicitudes, productos, planes y políticas admiten únicamente VES o USD. No se ha configurado un tipo de cambio ni una conversión automática.

`scripts/dev.py setup` prepara el catálogo de demostración venezolano. Para una instalación sandbox ya existente, después de aplicar migraciones:

```bash
python3 scripts/localize_venezuela_samples.py          # Vista previa, rollback
python3 scripts/localize_venezuela_samples.py --apply  # Solo muestras reconocidas
```

Este script verifica slugs, correos y servicios ficticios exactos; no modifica perfiles reales. En la publicación actualizó 39 ofertas, 30 sedes presenciales, una dirección de ejemplo y dos productos, y añadió dos planes y dos políticas de retiro de ejemplo. La segunda ejecución no cambió registros. Los precios en Bs y USD son valores independientes de demostración, no una conversión de los antiguos precios COP. Los presenciales de ejemplo se distribuyen entre Caracas, Maracaibo y Valencia; permanecen las tres muestras por categoría y sus imágenes.

## Verificación y respaldo

Se aprobaron 55 pruebas SQL, 35 HTTP y los 13 recorridos completos de navegador sobre HTTPS, incluidos registro, cuentas, imágenes, mapa, direcciones móviles, cambio de ubicación y contratación con pagos simulados en Bs. Se añadió y verificó además el caso de recuperación después de una respuesta 503 con las mismas coordenadas. El build TypeScript/Vite y las imágenes Docker se verificaron antes de actualizar los contenedores. La captura móvil está en [screenshots/venezuela-address-mobile.png](screenshots/venezuela-address-mobile.png).

El respaldo previo es `backups/chamba-20260911T035102Z.dump` (0600). Las etiquetas de imágenes anteriores están en `backups/venezuela-images-20260911T035145Z.json`. Las huellas de diez tablas de historia financiera fueron idénticas antes y después de migrar y localizar muestras, antes de ejecutar los recorridos públicos que añaden actividad sandbox. La base y su volumen existentes se conservaron. Los comprobantes están en `backups/venezuela-history-before-20260911T035145Z.json` y `backups/venezuela-history-after-20260911T035145Z.json`.
