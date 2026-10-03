# Integración del portal y las aplicaciones

La API HTTP y el portal ya están implementados. Ver [backend y web](backend-web.md) para instalación, rutas y autenticación. Este documento describe la frontera SQL utilizada por el servidor.

## Conexión y frontera de confianza

`app` contiene tablas privadas; `internal`, funciones auxiliares; `api`, operaciones permitidas al backend. `meta` registra migraciones. Todas las tablas de `app` tienen RLS activada sin políticas públicas, y los roles de ejecución no reciben permisos sobre tablas ni secuencias. Acceden mediante funciones `SECURITY DEFINER` con nombres calificados, `search_path` fijado y comprobaciones explícitas de actor/participación.

Los navegadores y móviles **no se conectan directamente a PostgreSQL**. El backend comprueba la sesión y fija el actor al comenzar cada transacción:

```sql
BEGIN;
-- El UUID procede de la sesión validada por el servidor, nunca del cuerpo HTTP.
SELECT set_config('app.user_id', $1, true);
SELECT set_config('app.correlation_id', $2, true);
SELECT api.my_account();
COMMIT;
```

Usar consultas parametrizadas. `set_config(..., true)` y la llamada de negocio deben ejecutarse en la misma conexión y transacción. Con pooling, no usar una configuración de sesión persistente. El contexto no es una firma ni una autenticación criptográfica: quien controla las credenciales SQL del backend puede fijarlo. Las claves SQL permanecen exclusivamente en servicios de confianza.

El usuario `chamba_owner` de `.env` sirve para migrar y administrar la instancia local, no para atender tráfico de la app. Crear identidades de conexión diferentes, con claves aleatorias en el gestor de secretos y sin `SUPERUSER`, `BYPASSRLS`, `CREATEROLE` ni propiedad de objetos:

| Rol de grupo NOLOGIN | Uso |
|---|---|
| `chamba_api` | Backend de cliente y profesional. |
| `chamba_auth` | Servicio que verifica contraseñas y emite/resuelve sesiones. |
| `chamba_billing` | Confirmación de pagos de planes/recargas, verificación de destinos de retiro, envío/conciliación de retiros e incidencias monetarias. |
| `chamba_admin` | Rutas de administración; las funciones exigen además permisos del usuario de aplicación. |
| `chamba_worker` | Ingesta de archivos, expiración y cola de notificaciones. |

Ejemplo de provisión por el operador, usando `psql` y `\password` para evitar claves en historial:

```sql
CREATE ROLE chamba_backend LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
GRANT chamba_api TO chamba_backend;
\password chamba_backend
```

Repetir con identidades separadas para los otros servicios. No otorgar todos los grupos al mismo login. Para el primer superadministrador, registrar la cuenta normal y, como propietario, insertar explícitamente su UUID en `app.user_roles` con `role_code='superadmin'`; no existe cuenta administrativa predeterminada. Los siguientes cambios de rol se realizan con `api.assign_role` y quedan auditados.

## Registro y login

1. El servidor valida correo, contraseña y consentimiento a términos/privacidad.
2. Calcula **Argon2id** con sal aleatoria. `api.register_user` recibe el hash; no recibe ni almacena la contraseña en claro. La restricción SQL solo comprueba el prefijo, no la fortaleza ni la autenticidad del hash.
3. La identidad puede vincularse a proveedores externos mediante `auth_identities`. Los tokens de recuperación/verificación se guardan como hashes de secretos de alta entropía; nunca el código original.
4. `api.auth_account` solo está disponible para `chamba_auth`; permite obtener el hash para validarlo en ese servicio. Aplicar límites de intentos, manejo uniforme de errores, verificación de correo y MFA desde el servidor.
5. Después de validar credenciales/MFA, generar un token criptográficamente aleatorio, guardar su SHA-256 con `api.create_session` y devolver el secreto al dispositivo de forma segura. `api.resolve_session` rechaza revocadas, expiradas y usuarios suspendidos.
6. Rotación de refresh, recuperación de contraseña, verificación de correo y MFA TOTP están implementados en `backend/app/modules/auth`. La verificación de teléfono por SMS/WhatsApp requiere seleccionar e integrar su proveedor.

## Recorrido de contratación

### Directa

1. Cliente registra dirección con `api.add_address`.
2. Busca con `api.search_professionals`: dirección propia, categoría, radio, precio/moneda, calificación mínima y orden.
3. Consulta `api.professional_profile` y `api.professional_reviews`.
4. Crea `api.create_request(..., 'direct', profesional, ...)`. Ese destino queda privado.
5. Adjunta imágenes, se revisan y se aprueba la solicitud. `api.publish_request` representa el consentimiento del cliente a contactar al profesional seleccionado.
6. El profesional ve `api.available_requests` o el nuevo `api.service_feed` y ejecuta `api.accept_direct_request`.
7. La transacción crea un contrato, descuenta exactamente un crédito, crea la conversación y encola avisos. El presupuesto indicado por el cliente no se interpreta como precio acordado.

### Publicación abierta

1. Se crea/publica la solicitud con modo `broadcast` y destino profesional nulo.
2. Se generan eventos para los profesionales de categoría y cobertura compatibles, con el UUID de la solicitud; el feed vuelve a comprobar autorización y vigencia.
3. Cada profesional puede hacer una propuesta con `api.submit_proposal`; no paga por proponer.
4. El cliente consulta `api.request_proposals` y elige usando `api.accept_proposal`.
5. Se bloquea la solicitud, se verifica saldo/cobertura/vigencia, se cobra al elegido y se rechazan las demás propuestas. Si falta saldo, la solicitud y sus propuestas siguen sin adjudicarse.

### Ejecución

Este apartado describe principalmente servicios presenciales con pago externo. Para servicios remotos y presenciales protegidos, utilizar el [recorrido de dinero, entregas y liberación](payments.md). El remoto se crea con `api.create_remote_request` y no requiere dirección; la búsqueda remota usa `api.search_remote_professionals`.

- `api.contract_contacts` entrega contactos compartibles únicamente a las partes del contrato.
- `api.send_message` y `api.chat_messages` funcionan sobre el contrato; no existe chat de servicio previo a la aceptación mutua.
- `api.propose_terms` y `api.accept_terms` permiten acordar precio y forma de pago con versión y aceptación de ambos. Una nueva propuesta de condiciones reemplaza a las pendientes anteriores.
- El botón **Enviar ubicación** llama a `api.share_location`. `api.navigate` entrega el destino, y `api.report_position` permite al profesional compartir su última posición durante el trayecto/trabajo. No se calcula ruta ni ETA en SQL.
- `api.advance_contract` admite `depart`, `arrive`, `start`, `finish` para presenciales con pago externo. La app puede ejecutar `arrive` y `start` en una sola transacción al marcar llegada e inicio. En pago protegido, el profesional envía una entrega con `api.submit_delivery`; el cliente valida y libera con `api.release_contract_funds`. En remoto se inicia directamente con `start`, solo si existe reserva financiada.
- `api.rate_contract` crea reseña pendiente de moderación. `api.dispute_contract` abre ticket y detiene el acceso a la ubicación.
- La cancelación temprana tiene una operación específica. La devolución de crédito exige un contrato cancelado/disputado, un ticket asociado, permiso administrativo y motivo.

## Privacidad de contenido y ubicaciones

El feed profesional no incluye distancia hasta el cliente, para reducir la posibilidad de deducir sus coordenadas mediante consultas repetidas. La pertenencia a una zona aún revela que la solicitud es atendible desde esa cobertura: limitar frecuencia de cambios de sedes/cobertura y consultas en el backend para reducir sondeos.

Un cliente puede escribir su teléfono o fotografiar una dirección. Por ello, los textos deben pasar revisión y los archivos nacen en estado `pending`. El trabajador procesa archivos en almacenamiento **privado**, analiza contenido, elimina EXIF/GPS, genera derivados y registra el resultado con `api.register_upload`. La base almacena metadatos y claves de objetos, no imágenes ni URLs públicas. Solo un revisor autorizado puede aprobarlos. `exif_removed` es una declaración del trabajador, no un procesamiento que haga PostgreSQL.

`api.file_object_key` autoriza cada lectura. El backend debe usar esa clave para entregar una URL firmada de vida corta o transmitir el archivo autorizado. Para archivos propios pendientes, permitir solo descarga/preview seguro de cuarentena; nunca servir HTML/SVG activo en el origen de la app. Los archivos KYC se consultan por el dueño o mediante la ruta administrativa `api.kyc_case`, con motivo auditado.

No incluir coordenadas en notificaciones, logs, textos de chat automáticos ni objetos públicos. `location_shared` es solo un evento; las coordenadas se recuperan con `api.navigate`. Revocar la ubicación impide futuras lecturas autorizadas, pero no elimina copias ya vistas o guardadas por un receptor. El backend debe respetar revocación y caducidad en caché. La posición del profesional caduca a los 15 minutos; la tarea de mantenimiento la elimina.

Los campos `*_ciphertext` esperan cifrado autenticado de la aplicación con claves fuera de PostgreSQL. Tener una columna `bytea` no cifra datos automáticamente. `order_delivery` solo se comparte después de aceptar el pedido y por acción explícita del comprador. Las direcciones ordinarias se protegen por permisos; cifrado de discos, copias y conexiones es configuración de despliegue.

## Compra de créditos y ventas

`api.start_purchase` fija una copia del plan activo. `api.confirm_purchase` se ejecuta **solo en el servicio de pagos** después de verificar firma, comercio, importe, moneda, compra y estado real con la pasarela. El identificador del evento y el del pago externo son únicos; un reintento no vuelve a acreditar. La función no valida firmas HTTP ni consulta pasarelas por sí misma.

El marketplace requiere un beneficio `marketplace` vigente. `api.activate_store`, `api.add_product`, `api.create_order` y `api.advance_order` cubren apertura, inventario y pedido. El carrito recibe IDs y cantidades; los precios y totales se calculan en el servidor. Los pagos de productos y de presenciales sin protección siguen siendo externos. `api.report_external_payment` y `api.confirm_external_payment` conservan declaraciones de las partes; no son prueba bancaria ni liquidación de fondos. La base rechaza registrar un servicio protegido por esta vía.

Los servicios protegidos utilizan wallet y reserva por contrato. Las recargas y el uso del saldo no tienen comisión; la única comisión sobre dinero de la wallet se aplica al retiro confirmado. El [detalle financiero](payments.md) sustituye cualquier supuesto anterior de pagos externos para servicios remotos.

Una compra de plan pagada no se deshace con SQL manual. Antes de automatizar reembolsos/contracargos, definir cómo tratar créditos gastados, beneficios ya usados e importes parciales; usar nuevos eventos de reversión y conciliación. `purchase_reversal` y estados relacionados están previstos, pero la automatización queda pendiente de esa política.

## Soporte, auditoría y notificaciones

Los permisos de aplicación distinguen soporte, administración y superadministración. La cuenta de soporte consulta/gestiona tickets; no recibe automáticamente permisos para KYC, asignar roles o devolver créditos. Las consultas administrativas de expedientes requieren motivo y se auditan. Las reseñas solo cambian visibilidad por moderación.

Los eventos y libros históricos impiden `UPDATE`/`DELETE` mediante triggers y no son accesibles como tablas desde la app. Esto no los hace invulnerables al propietario/superusuario de PostgreSQL: respaldar y exportar auditoría a un destino independiente con retención controlada. La auditoría de filas guarda operación e identificador, no copias de secretos ni datos personales.

`api.claim_notifications` usa `FOR UPDATE SKIP LOCKED` y un arrendamiento de dos minutos. `api.finish_notification` exige el número de intento vigente y reintenta hasta ocho veces con espera creciente. Un evento representa una entrega por canal seleccionado por el trabajador; si se necesita entrega obligatoria por varios canales, crear trabajos separados por canal antes de usar la cola con ese requisito. La entrega es **al menos una vez**: usar idempotencia del proveedor, porque un fallo después del envío y antes del acuse puede repetir una notificación. No hay envíos externos configurados.

Programar `api.expire_requests` periódicamente; las consultas y aceptaciones ya comprueban vigencia sin depender del trabajador. Planificar retención/purgado de sesiones, posiciones, documentos y datos personales según el producto y sus obligaciones aplicables. La implementación no pretende acreditar cumplimiento normativo.

## Referencias técnicas

- [PostgreSQL: políticas de filas y excepciones del propietario](https://www.postgresql.org/docs/current/ddl-rowsecurity.html).
- [PostgreSQL: privilegios predeterminados globales y por esquema](https://www.postgresql.org/docs/current/sql-alterdefaultprivileges.html).
- [PostgreSQL: funciones SECURITY DEFINER y search_path](https://www.postgresql.org/docs/current/sql-createfunction.html).
- [PostGIS: ST_DWithin con geography y distancias en metros](https://postgis.net/docs/ST_DWithin.html).
- [Imagen PostGIS: etiquetas y ruta del volumen de PostgreSQL 18](https://github.com/postgis/docker-postgis).
