# Contratar servicios y publicar necesidades

Actualización publicada el 11 de septiembre de 2026 en [contrata.contupana.pro](https://contrata.contupana.pro/explore).

| Flujo | Formulario | Asignación |
|---|---|---|
| Solicitar un servicio profesional | Descripción del problema, Bs/USD, dirección solo presencial y fotos opcionales; título, categoría y modalidad proceden de la oferta | El profesional cotiza y el cliente confirma su propuesta |
| Publicar una necesidad | Modalidad, categoría, título, descripción, moneda, presupuesto opcional, dirección/radio presencial y fotos opcionales | Varios profesionales pueden proponer; el cliente elige uno |

Las ofertas profesionales permanecen activas después de contratar. Un profesional puede atender simultáneamente a distintos clientes. El cierre afecta únicamente a la solicitud asignada, no al profesional ni a las demás necesidades del cliente.

## Contexto, precio y cierre

Los enlaces usan `professional` y `offering`. La pantalla consulta la oferta guardada y presenta sus datos como información. Los enlaces antiguos con categoría/modalidad se resuelven si identifican una única oferta; si son ambiguos, se pide elegir el servicio del perfil.

`POST /api/v1/requests/request-offering` recibe la oferta y los datos de la necesidad. PostgreSQL deriva profesional, categoría, título y modalidad; no admite que el cliente HTTP los sobrescriba. Se guarda una referencia y una instantánea del servicio. Se rechazan ofertas inactivas, contratación propia, profesionales suspendidos, direcciones ajenas y ubicaciones fuera de cobertura. Una invitación presencial usa la cobertura real del profesional, sin pedir un radio al cliente.

El precio «desde» conserva su moneda original. Elegir Bs para un anuncio en USD no convierte ni reetiqueta el importe. El profesional cotiza un precio positivo en la moneda elegida y el cliente lo confirma antes de asignar. Las nuevas invitaciones usan `requires_quote` y no pueden saltarse esa confirmación mediante la aceptación directa antigua. Las invitaciones previas conservan su comportamiento.

`POST /api/v1/requests/create-customer-request` permite publicar necesidades con presupuesto opcional, incluso remotas. Las propuestas se expresan en la moneda de la solicitud y no pueden vencer después de ella.

La aceptación bloquea la solicitud y crea un único contrato; cambia la solicitud a `matched`, acepta la propuesta elegida, rechaza las demás pendientes y descuenta un crédito al profesional en una transacción. Reintentar la misma aceptación devuelve el contrato existente. El feed solo devuelve solicitudes abiertas, aprobadas y vigentes; una solicitud asignada rechaza nuevas propuestas. La pantalla se actualiza periódicamente mientras está visible y el cliente dispone de un enlace al contrato.

## Fotos y reintentos

Ambos formularios admiten hasta seis imágenes JPG, PNG o WEBP de hasta 10 MB cada una, con vista previa y eliminación antes de guardar. La subida verifica y recodifica la imagen, eliminando EXIF. La creación de la solicitud y sus vínculos a imágenes es atómica: archivos ajenos, repetidos, rechazados, de otro propósito o inexistentes impiden crearla.

Las fotos recién subidas siguen privadas hasta adjuntarse a una solicitud. Al enviarla se aprueban automáticamente solo sus imágenes técnicamente válidas: del mismo cliente, de propósito `request`, con EXIF eliminado, formato/tamaño admitidos, sin borrado ni rechazo administrativo. La solicitud no puede publicarse con adjuntos inseguros. Los profesionales elegibles pueden ver las fotos de la solicitud publicada; después de asignar, ambos participantes las conservan en el contrato. Los demás usuarios no reciben acceso. La interfaz usa descargas autorizadas, URLs Blob temporales y respuestas `private, no-store`. Esta política no aprueba archivos de identidad, portafolios ni subidas sin adjuntar.

`POST /api/v1/requests/remove-request-image` permite quitar fotos únicamente de un borrador propio. No altera referencias de solicitudes publicadas o asignadas. El formulario reutiliza las fotos subidas al reintentar y conserva una clave estable para no duplicar la solicitud si se pierde la respuesta. Los archivos subidos que no se adjuntan permanecen privados en los archivos de su propietario.

## Publicación automática y moderación posterior

La migración 028 hace que `request_offering` y `create_customer_request` publiquen en la misma transacción que crea la solicitud y adjunta las fotos. «Enviar solicitud» entrega la invitación al profesional elegido; «Publicar solicitud» la muestra a los profesionales elegibles. No hay aprobación administrativa previa. Se conserva el evento duradero `request.available`, deduplicado por solicitud y destinatario. El panel profesional se actualiza cada diez segundos mientras está visible.

El administrador conserva la acción «Desautorizar» con motivo y registro de moderación. Una solicitud retirada desaparece del feed, deja de permitir el acceso del profesional a las fotos de la solicitud y bloquea nuevas cotizaciones y asignaciones. «Autorizar publicación» permite restaurarla. Los reintentos del cliente no reviven solicitudes rechazadas, canceladas ni asignadas; cambiar las fotos de un borrador rechazado tampoco elimina el rechazo.

Los editores antiguos `create_request` y `create_remote_request` conservan sus borradores para compatibilidad; su publicación explícita ya no requiere aprobación previa. El cambio no modifica contratos, cobros, saldos ni solicitudes históricas canceladas.

## Implementación y evidencia

La migración 025 añade contexto de oferta, cotización, creación atómica, controles de imágenes y consultas de solicitudes. La 026 añade las fotos al detalle del contrato después de validar participación. Interfaz en `Requests.tsx`, `Explore.tsx`, `Contracts.tsx` y `RequestPhotos.tsx`.

Pasaron 55 pruebas SQL (343.793 s) y 42 HTTP (71.41 s), más la comprobación específica de las fotos dentro del contrato al incorporar la migración 026. Las tres pruebas de navegador sobre HTTPS pasaron en 26.6 s: contratación presencial móvil con fotos y respuesta perdida/reintento sin duplicado; necesidad remota sin presupuesto obligatorio; cotización → asignación → reserva → entrega → liberación con fotos accesibles por ambos participantes. [Captura móvil](screenshots/request-offering-mobile.png).

Las pruebas nuevas están en `backend/tests/test_customer_requests.py` y `frontend/e2e/customer-requests.spec.ts`; el recorrido remoto actualizado está en `frontend/e2e/portal.spec.ts`. Este último usa datos y pagos ficticios del entorno sandbox.

Respaldo previo: `backups/chamba-20260911T043607Z.dump` (0600). Código anterior extraído de los tres contenedores: `backups/customer-requests-*-before-20260911T043740Z.tar`, con índice privado `customer-requests-code-20260911T043740Z.json`. Se respaldó el código porque Docker reportó referencias de imágenes/contenido anteriores ausentes al intentar conservarlas. Las imágenes nuevas se construyeron y verificaron antes de activar.

Las huellas y cantidades de diez tablas históricas/financieras coincidieron antes y después de migrar, antes de la actividad pública de pruebas. Los comprobantes son `backups/customer-requests-history-{before,after}-20260911T043740Z.json`. No se cambiaron monedas ni importes de contratos o movimientos anteriores.

### Activación de la publicación automática (028)

Aplicada el 11 de septiembre de 2026 a las 14:26 UTC. Web pública verificada con `index-CVKXecAP.js`; API y base de datos saludables, sin reiniciarlas. Respaldo previo `backups/chamba-20260911T142037Z.dump` (0600). Se conservó el contenido web anterior en `backups/auto-publication-web-before-20260911T142100Z/` y su configuración Nginx; Docker no pudo etiquetar la referencia de imagen del contenedor anterior porque no estaba disponible.

Pasaron 59 pruebas HTTP, 55 SQL y tres recorridos de navegador sobre HTTPS: envío presencial móvil con respuesta perdida y reintento sin duplicación, aparición automática en el panel profesional ya abierto, publicación remota con foto y desautorización administrativa, y contratación remota completa. No hubo desbordamiento horizontal en las vistas móviles verificadas. Las huellas de las diez tablas históricas/financieras coincidieron durante la migración y antes de las pruebas sandbox; comprobante en `backups/auto-publication-history-20260911.json`.

Se publicaron las dos solicitudes vigentes dirigidas a Mateo que estaban en borrador pendiente, reutilizando `api.publish_request` y sus validaciones bajo la identidad de cada propietario. La recuperación quedó identificada como mantenimiento en auditoría, con correlación `automatic-publication-policy-028-20260911`. No se crearon solicitudes duplicadas: ambas tuvieron un único evento `request.available`, entregado por el canal interno. Se verificó desde la cuenta profesional la solicitud original «No llega Agua» y el acceso a su foto. No se modificaron las solicitudes históricas canceladas, rechazadas ni asignadas.
