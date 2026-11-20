# Verificación de backend, web y base de datos

## Nombre de perfil y muestras cerca de Valera: 11 de septiembre de 2026

El nombre `Develope Academy` incumplía la restricción de minúsculas/números/guiones de `app.professionals.slug`. La web y la API ahora lo normalizan a `develope-academy`, explican nombres ocupados junto al campo y conservan la activación idempotente. No se modificó la función SQL que concede los créditos.

- 51/51 pruebas HTTP en 72.53 s, incluidas nueve nuevas para normalización, Unicode, formato, colisión, reintentos y activación simultánea sin duplicar créditos.
- 30 sedes privadas de muestra reubicadas entre 2 500 y 42 500 metros de la dirección “Casa” autorizada en Valera; 30 coberturas ajustadas a 50 000 metros y una dirección de cliente ficticio adaptada. Todas las sedes pertenecen a su parroquia y conservan la jerarquía correcta.
- La consulta real desde “Casa” devuelve 30 profesionales; cada una de las diez categorías presenciales devuelve tres muestras a 50 km. La segunda ejecución de la reubicación y del cargador anterior informa cero modificaciones.
- Huellas de diez tablas históricas/financieras y de todas las direcciones/sedes ajenas a las cuentas demo idénticas antes y después de mover las muestras, antes de añadir actividad de pruebas públicas.
- Tres recorridos HTTPS correctos en 10.2 s: contratación presencial con las nuevas ubicaciones y fotos, búsqueda cercana con 30 resultados/filtro de categoría, y activación móvil con error de nombre ocupado, conversión y tres créditos una sola vez. La cuenta de activación es ficticia y no publica servicios.
- API, web y DB saludables; trabajador activo. TypeScript/Vite, Ruff, importación de la imagen API y Nginx correctos. SHA-256 público coincide con `index-CFmGGFi3.js`, `rolldown-runtime-CbXtAM7H.js` e `index-C9a8_wZG.css`. `/api/health` confirma la base conectada.

Respaldo de base `backups/chamba-20260911T045341Z.dump`, registros de muestras `backups/demo-relocation-20260911T045343Z.json` y código anterior de contenedores `backups/profile-fix-*-before-20260911T045343Z.tar`, todos 0600. La configuración local `.demo-geography-override.json` (0600) preserva el área elegida al repetir la carga. [Implementación y alcance](profile-activation-and-demo-area.md).

## Servicios profesionales y necesidades de clientes: 11 de septiembre de 2026

Publicados los dos formularios en `https://contrata.contupana.pro`: contratación de una oferta con contexto conservado y publicación de una necesidad abierta. Ambos admiten fotos antes de guardar. Las nuevas invitaciones requieren cotización en la moneda elegida y confirmación del cliente. Al asignarse, la solicitud deja de recibir propuestas; las ofertas profesionales siguen disponibles.

| Comprobación | Resultado |
|---|---|
| SQL | 55/55 pruebas, 343.793 s; incluye concurrencia, un ganador por solicitud y varios contratos por profesional |
| HTTP | 42/42 pruebas, 71.41 s; siete casos nuevos para contexto, moneda, cotización, fotografías y cierre |
| Fotos en contrato | Caso adicional de acceso de participantes y rechazo de terceros, 1/1 en 8.86 s tras migración 026 |
| Navegador HTTPS | 3/3 en 26.6 s: presencial móvil/fotos/reintento sin duplicar, necesidad remota con fotos y contratación remota completa con precio confirmado |
| Privacidad | Fotos pendientes privadas, publicación bloqueada sin aprobación, referencias conservadas para participantes después de asignar |
| Historial | Diez tablas históricas y financieras sin cambios entre huellas previas/posteriores a migrar |
| Publicación | API, web y trabajador actualizados; DB y volumen conservados; `/api/health` 200 con base conectada |
| Archivos públicos | SHA-256 coincide con build: `index-DE1z200H.js`, `rolldown-runtime-CbXtAM7H.js`, `index-C9a8_wZG.css` |
| Esquema y contrato | 26 migraciones, 85 tablas, 167 funciones SQL API; 157 operaciones HTTP, 134 de negocio |

La primera ejecución de los dos tests de navegador usaba un token de sesión web anterior a su rotación. Se corrigió el ayudante de pruebas para usar una sesión API separada; la autenticación de la aplicación se conservó. El reintento probado pierde deliberadamente la respuesta después de que el servidor guarda: conserva las fotos y produce una sola solicitud.

Respaldo `backups/chamba-20260911T043607Z.dump` y código previo extraído de contenedores en `backups/customer-requests-*-before-20260911T043740Z.tar` (0600). No se pudo etiquetar o capturar las imágenes anteriores debido a referencias de contenido ausentes en Docker; se conservaron archivos del código en ejecución. Las imágenes nuevas sí pasaron build, importación de API y validación de Nginx antes de activar. [Implementación y comprobantes](customer-requests.md).

## Venezuela, Bs/USD y Mapbox: 11 de septiembre de 2026

Publicado en `https://contrata.contupana.pro`. País activo VE, formato `es-VE` y monedas operativas VES/Bs y USD/US$. El asistente permite nombrar direcciones, seleccionar estado → municipio → parroquia y fijar un marcador en Mapbox. La ubicación del navegador actualiza la búsqueda con permiso del usuario; existe elección manual y recuperación tras fallos de conexión.

| Comprobación | Resultado |
|---|---|
| Migraciones | 24 aplicadas; 85 tablas de negocio y 164 funciones SQL API |
| Catálogo | 25 entidades federales, 336 registros municipales y 1 135 parroquiales; incluye agrupaciones de Dependencias Federales |
| Integridad geográfica | Padres válidos, municipios con hijos, puntos de referencia interiores; rechazo de país, jerarquía o marcador incompatibles |
| SQL | 55/55 pruebas, 331.191 s, base desechable con todas las migraciones |
| HTTP | 35/35 pruebas, 50.98 s; 12 casos geográficos nuevos y pruebas anteriores de registro/pagos |
| Navegador HTTPS | 13/13 recorridos completos, incluidos mapa real, dirección móvil, Bs/USD, cambio de ubicación, 39 servicios e imágenes, registro y contratación simulada |
| Reintento con igual posición | Caso adicional 503 → reintento → parroquia recuperada; probado localmente y en HTTPS junto al cambio de ubicación, 2/2 en 3.6 s |
| Muestras | 39 ofertas, 30 sedes, una dirección y dos productos localizados; dos planes y dos políticas añadidos; segunda ejecución sin cambios |
| Historial | Huellas y recuentos de diez tablas financieras/históricas idénticos antes y después de migrar/localizar, antes de la actividad de navegador sandbox |
| Build y publicación | TypeScript/Vite y Docker correctos; API, DB y web saludables, trabajador activo |
| Archivos públicos finales | SHA-256 coincide con build local: `index-BSjbnFej.js`, `rolldown-runtime-CbXtAM7H.js`, `index-CtE2nh4O.css` |
| Salud pública | `/api/health` HTTP 200, `database: connected`; HTML `Cache-Control: no-cache` |

Las coordenadas de exploración viajan por POST, no se guardan en direcciones ni historial, y los resultados cercanos no exponen sedes privadas. El contrato de API documenta 154 operaciones HTTP, 131 de ellas de negocio. Mapbox sirve teselas reales con atribución; no se almacenan resultados de geocodificación. El dinero histórico conserva su moneda: no se convirtió COP ni se aplicó un tipo de cambio.

Respaldo previo: `backups/chamba-20260911T035102Z.dump` (0600); etiquetas de imágenes anteriores y huellas de historia en `backups/venezuela-*-20260911T035145Z.json`. Las pruebas de navegador crearon únicamente cuentas y actividad ficticias en staging/sandbox. La API de prueba y el preview locales se detuvieron y su base aislada se eliminó después de validar.

Repetir el recorrido completo de navegador sobre el entorno de pruebas: `CHAMBA_GEO_MUTATION=1 CHAMBA_TEST_REGISTRATION=1 PLAYWRIGHT_BASE_URL=https://contrata.contupana.pro npx playwright test` desde `frontend/`. Las banderas habilitan altas ficticias; no usar sobre una base comercial. [Arquitectura, fuente INE/OCHA y límites del catálogo](venezuela-geolocation.md).

## Corrección del registro: 11 de septiembre de 2026

Publicado en `https://contrata.contupana.pro/register`.

Se reprodujo un HTTP 422 con una contraseña de menos de 12 caracteres. La API indicaba el campo en `error.details`, pero el cliente descartaba esos detalles y mostraba únicamente un mensaje genérico. Los registros no guardan los valores enviados, por lo que no permiten identificar el campo exacto de la solicitud original reportada.

- El formulario valida nombre de 2 a 120 caracteres y contraseña de 12 a 128, muestra errores junto al campo en español y conserva lo escrito para corregirlo. Los errores del servidor enfocan el primer campo rechazado.
- Se eliminan espacios exteriores del nombre y correo; la contraseña se conserva exactamente. El servidor también rechaza nombres formados solo por espacios.
- Se mantienen las restricciones del servidor y el rechazo de cuentas duplicadas; no se incluyen contraseñas ni valores de entrada en las respuestas de validación.
- La página HTML usa `Cache-Control: no-cache`; las imágenes y los archivos versionados conservan su caché. El bundle reportado `app-DKKA8w9L.js` no existía en este dominio. La aplicación usa React Router 7.18.3 y no produjo los avisos de v6 durante el recorrido público.
- Backend: las 15 pruebas existentes y las 8 nuevas de registro aprobaron. Las nuevas usan una base desechable, verifican ausencia de altas al rechazar datos y comprueban que los espacios de la contraseña son significativos.
- TypeScript/Vite, Ruff y configuración Nginx correctos. API, trabajador y web actualizados; base de datos existente conservada.
- Cinco pruebas de navegador público aprobadas en 7.7 s: validación móvil, detalle de errores de API, cookies/sesión HTTPS, protección del proxy y registro real de una cuenta ficticia. Se comprobó 422, corrección, 201, panel, recarga y cierre de sesión.
- Archivos públicos verificados por SHA-256: `index-Dbi8B2sq.js` y `index-BeNrZRkG.css`. `/api/health` devuelve 200 con base conectada.

Pruebas de formulario sin crear cuentas: `PLAYWRIGHT_BASE_URL=https://contrata.contupana.pro npx playwright test e2e/registration.spec.ts` desde `frontend/`. La prueba `registration-live.spec.ts` requiere `CHAMBA_TEST_REGISTRATION=1` y HTTPS staging/sandbox; crea una cuenta ficticia identificada como «Prueba de registro HTTPS», sin enviar correos.

Imágenes Docker anteriores conservadas mediante etiquetas, registradas en `backups/registration-images-20260911T030840Z.json` para reversión del código.

## Tres muestras por categoría: 11 de septiembre de 2026

Publicado en `https://contrata.contupana.pro/explore`: **39 profesionales y 39 servicios de ejemplo**, tres en cada una de las 13 categorías. Se añadieron 36 perfiles; los tres originales se conservaron.

- Las 13 consultas públicas por categoría devuelven tres perfiles distintos, con su servicio, precio, descripción y modalidad.
- La segunda ejecución de la semilla creó cero perfiles y conservó los 39; los 36 profesionales nuevos tienen exactamente los tres créditos de bienvenida.
- No se añadieron reseñas ni expedientes KYC a los perfiles nuevos.
- Se crearon diez imágenes adicionales con Imagegen. Las 13 portadas WebP se verificaron por HTTPS, MIME y SHA-256 contra el archivo del proyecto. La mayor pesa 95 448 bytes.
- TypeScript/Vite y comprobaciones Python correctos. Se reconstruyó y publicó solo el contenedor web; API saludable.
- Dos recorridos públicos de Chromium aprobados en 22.7 s: exploración escritorio/móvil y revisión de los 39 servicios, sus imágenes y un perfil de cada categoría.
- Respaldo previo: `backups/chamba-20260911T024805Z.dump`. Credenciales adicionales en `.demo-services-access.json` (0600).

[Catálogo, datos fuente, imágenes y reproducción](service-samples.md). [Captura de diseño gráfico con tres muestras](screenshots/three-design-services-desktop.png).

## Imágenes de servicios: 11 de septiembre de 2026

Se publicaron tres escenas de ejemplo generadas con Imagegen: plomería, mantenimiento de computadores y diseño gráfico. Las imágenes se asignan por categoría del catálogo y conservan los perfiles, precios, filtros y enlaces existentes. Las tarjetas incluyen portada 16:9, etiqueta de categoría, iniciales e indicador presencial/remoto; si falla una imagen se recupera la portada anterior.

- WebP 1280 × 720: 90 738 bytes (plomería), 95 448 bytes (tecnología), 87 442 bytes (diseño).
- Carga diferida, texto alternativo y caché HTTP de 30 días con nombres versionados.
- TypeScript/Vite y reconstrucción del contenedor web correctos.
- Las tres imágenes públicas devuelven HTTPS 200, MIME `image/webp` y SHA-256 idéntico al archivo del proyecto.
- Revisión de escritorio/móvil, filtro remoto, navegación al perfil y recuperación ante imágenes fallidas correctos, sin errores JavaScript ni desbordamiento horizontal.
- Recorrido existente de exploración pública aprobado en el dominio.

Originales y prompts: [output/imagegen/services/prompts.md](../output/imagegen/services/prompts.md). Archivos web: `frontend/public/images/services/`. Capturas: [escritorio](screenshots/services-images-desktop.png) y [móvil](screenshots/services-images-mobile.png).

## Publicación HTTPS: 11 de septiembre de 2026

Continuación verificada en **https://contrata.contupana.pro**, en modo `staging` con pagos sandbox.

| Comprobación | Resultado |
|---|---|
| DNS | A apunta a 82.29.62.188, el servidor de esta aplicación |
| HTTPS | Certificado Let's Encrypt válido para el subdominio; vence 2026-12-10 |
| Redirección | HTTP devuelve 301 hacia HTTPS |
| API pública | `/api/health`: HTTP 200, `status=ok`, `database=connected` |
| Configuración activa | `staging`, `sandbox`, origen HTTPS y cookies seguras |
| Servicios | API/web/DB saludables; trabajador activo; reinicio automático configurado |
| Backend | 15 pruebas correctas: 10 recorridos existentes y 5 casos de publicación |
| Frontend | TypeScript y Vite compilados; imagen web reconstruida |
| Navegador HTTPS | 6/6 escenarios correctos en Chromium, 33.9 s |
| Sesión | Login, refresh al recargar, cookies Secure/HttpOnly/SameSite, logout y acceso protegido |
| Proxy | API inexistente: JSON 404; privados: 401; `.env`: 404; origen ajeno: 403 |
| IP del visitante | Límite HTTP usa la IP real; X-Forwarded-For y X-Real-IP falsificados no se aceptan |
| Recorrido completo | Solicitud remota, aceptación, crédito, reserva, chat, entrega y liberación simulada |
| Renovación | `certbot renew --dry-run`: correcto; temporizador de usuario activo; servicio ejecutado con código 0 |
| Respaldo previo | Dump PostgreSQL y archivo privado de código/configuración en `backups/` |

Los seis escenarios incluyen vista móvil, cliente, profesional y administración. Las capturas de `docs/screenshots/` se actualizaron desde el dominio publicado. Ruff y compilación Python correctos. El test global de Nginx pasa; existen advertencias previas en otros virtual hosts del servidor.

No se integró pasarela bancaria ni se enviaron correos reales: el correo continúa en bandeja privada local. Operación y renovación reproducibles en [deployment.md](deployment.md).

## Evidencia de la entrega local anterior

Validación local del 11 de septiembre de 2026.

| Comprobación | Resultado |
|---|---|
| Motor | PostgreSQL 18.6 / PostGIS 3.6.4 |
| Migraciones | 21 aplicadas; migración repetible con verificación SHA-256 |
| Esquema | 80 tablas de negocio, 156 funciones SQL API |
| Servidor HTTP | FastAPI, 148 operaciones OpenAPI |
| Runtime | DB, API y web saludables; trabajador de notificaciones activo |
| Escuchas | Web 127.0.0.1:8180, API 127.0.0.1:8181, DB 127.0.0.1:55432 |
| Secretos | .env, backend/.env y .local-access.md con permisos 0600 |
| Datos locales | 5 cuentas ficticias, catálogo/plan DEMO, tasa DEMO 2 % COP y actividad de pruebas |
| Dinero real | Ningún cobro, transferencia ni retiro bancario real |
| SQL | 55/55 casos correctos, 327.233 s, migraciones hasta 019 |
| HTTP | 10/10 pruebas correctas, 18.49 s, incluida la migración 021 |
| Frontend | TypeScript y Vite compilan correctamente; pantallas con carga diferida |
| Dependencias web | `npm audit --omit=dev`: 0 vulnerabilidades conocidas tras actualizar React Router a 7.18.3 |
| Python | Compilación y comprobaciones Ruff E9/F63/F7/F82 correctas |
| Navegador | 4/4 escenarios correctos en Chromium, 35.8 s, incluyendo móvil y contratación completa |

## HTTP: qué se comprobó

- Salud, catálogo público, validación de límites, rutas protegidas y cobertura de operaciones SQL expuestas/internas.
- Los cinco logins de ejecución no son superusuarios y no pueden consultar credenciales directamente.
- Registro con Argon2id; cookie HttpOnly, CSRF obligatorio, rotación, revocación por reutilización de refresh y logout efectivo.
- Intento de asignarse roles administrativos rechazado; cuenta suspendida pierde acceso.
- Contrato remoto directo: bono único, crédito consumido una vez, feed sin identidad del cliente, rechazo de terceros y de inicio sin financiación.
- Recarga pendiente sin saldo; confirmación sandbox solo del propietario, repetición sin doble saldo, reserva, chat idempotente, entregas/correcciones y liberación íntegra solo por cliente.
- Verificación, destino de retiro, cotización con tasa, retiro administrativo de prueba y contabilidad correcta.
- Archivos con EXIF eliminado, rechazo de contenido falso, propiedad y revisión administrativa autorizada; notas internas de soporte ocultas al cliente.
- Firma HMAC y tiempo del webhook, evento repetido sin doble recarga y rechazo de simulaciones en producción.
- MFA TOTP, recuperación con correo privado, enlace de un solo uso y revocación tras cambiar contraseña.
- Contrato presencial por propuesta: cercanía, selección manual, consentimiento/revocación de ubicación, llegada/inicio/finalización y confirmación de pago externo.
- Marketplace: compra de plan, stock, actualización auditada, reserva/cancelación de pedido, cifrado de dirección y rechazo de acceso ajeno.

Las bases HTTP de prueba se crean por ejecución y se eliminan al finalizar. Dos avisos de deprecación de Starlette/HTTPX en el cliente de pruebas no afectan los resultados.

## Navegador: recorridos y capturas

1. Exploración pública, filtros presenciales/remotos, vista de 1440 px y móvil de 390 px, menú y ausencia de desbordamiento horizontal.
2. Login, paneles de cliente/perfil, wallet con recarga de prueba persistida tras recargar la página, soporte y visualización móvil.
3. Login administrativo, todas las colas y vistas profesionales de servicios, oportunidades y tienda.
4. Dos sesiones de cliente/profesional: solicitud remota, aprobación, publicación, aceptación, saldo reservado, chat, inicio, entrega y liberación validada desde la web.

Capturas reales: [escritorio](screenshots/explore-desktop.png), [móvil](screenshots/explore-mobile.png), [wallet](screenshots/wallet.png), [administración](screenshots/admin.png), [contrato completado](screenshots/contract-completed.png).

Una prueba adicional verifica que el historial personal y la cola administrativa conservan la moneda USD y el neto correcto del retiro.

Los casos de navegador usan exclusivamente la base demo y dejan actividad ficticia visible. Los fallos iniciales de selectores ambiguos se corrigieron especificando la cabecera o el menú correspondiente; después pasaron los escenarios afectados.

## Cobertura SQL

La suite se ejecutó sobre una base temporal separada, eliminada al terminar. Incluyó:

- Bono de bienvenida único y activaciones repetidas.
- Ambos caminos de contratación, selección manual y apertura de chat.
- Débito único, falta de saldo sin adjudicación parcial y devolución auditada.
- Ocultación de identidad/dirección en el feed y rechazo de accesos ajenos.
- Consentimiento explícito de ubicación, revocación y cierre del acceso al finalizar.
- Confirmación de fin por ambas partes, reseña única y efecto de moderación sobre el promedio.
- Cercanía, verificación, posicionamiento y propiedad de direcciones/sedes.
- Rechazo de publicaciones sin moderación y acceso autorizado a imágenes.
- Pago de plan con importe incorrecto, llamada desde rol indebido, evento repetido y reutilización del pago externo.
- KYC aprobado/revocado, sesiones revocadas, cuenta suspendida y permisos de funciones.
- Pedidos, inventario, cancelación repetida, comisión cero y activación del marketplace.
- Condiciones de pago con aceptación mutua y rechazo de versiones superadas.
- Declaraciones de pago con confirmación de contraparte y entrega privada de pedidos.
- Dos conexiones aceptando la misma solicitud simultáneamente.
- Dos solicitudes compitiendo por el último crédito.
- Dos propuestas compitiendo por la misma adjudicación.
- Dos compradores compitiendo por la última unidad.
- Dos confirmaciones del mismo pago en paralelo.
- Trabajadores de notificaciones sin reclamar el mismo trabajo simultáneamente y rechazo de un intento desactualizado.
- Servicios remotos directos y abiertos sin dirección ni necesidad de cobertura física.
- Preferencia de pago protegido en el perfil, aplicada también a contratos presenciales y conservada aunque cambie después el perfil.
- Recarga sin saldo utilizable antes de liquidación, sin comisión y sin duplicar un mismo evento.
- Imposibilidad de iniciar trabajo protegido sin fondos, retirar una reserva o cancelar un contrato financiado sin resolver el dinero.
- Liberación exclusiva por el cliente con importe íntegro para el profesional, idempotencia y comisión cero.
- Entregas sucesivas y solicitud de correcciones; una versión antigua no permite liberar.
- Rechazo de archivos ajenos en entregas, incluso si el profesional tiene otros archivos propios aprobados.
- Condiciones financiadas inmutables y cotización invalidada si cambian las condiciones aunque se mantenga el precio.
- Disputa que bloquea la liberación y devolución mediante acuerdo del profesional o resolución de soporte.
- Cotización de retiro parcial/total, porcentaje configurable, tasa cero y conservación de la tasa cotizada ante cambios administrativos.
- Comisión contable únicamente al confirmar un retiro; devolución íntegra de retiros cancelados/fallidos.
- KYC, titularidad del destino, moneda y verificación del método antes del retiro.
- Resultado de retiro desconocido sin liberar la reserva; clave estable para reconciliar el mismo envío.
- Prohibición de usar el mismo pago externo para acreditar créditos comerciales y dinero de wallet.
- Contabilidad de suma cero, saldo materializado consistente con entradas y bloqueo de modificación de movimientos.
- Financiación de un servicio y retiro simultáneos compitiendo por el mismo saldo.
- Liberación y recarga simultáneas sin doble crédito.
- Incidencia que congela salidas; el indicador `funds_guaranteed` deja de afirmar garantía y `funding_on_hold` informa la retención.


## Límites de esta verificación

No se han probado pasarelas/KYC externos, custodia bancaria real, push, SMS, correo entregado externamente, React Native, cálculo de ETA ni carga a escala de producción. La web muestra un mapa de coordenadas consentidas; el cálculo de ruta se abre en un servicio externo elegido por el usuario. Los cuatro escenarios del navegador no equivalen a pruebas exhaustivas de cada combinación de permisos y estado.

Para repetir:

```bash
python3 tests/run.py
(cd backend && .venv/bin/pytest -q)
(cd frontend && npm run build)
(cd frontend && npx playwright test)
```
