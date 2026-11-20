# Mi Chamba: descripción funcional y técnica

**Fecha de revisión:** 11 de septiembre de 2026.  
**Finalidad del documento:** describir el propósito, las funciones, los procesos y el alcance técnico de la aplicación como base para la redacción de un contrato.  
**Base de la descripción:** código y documentación existentes en el proyecto Chamba al momento de la revisión.

## 1. Qué es Mi Chamba y para qué sirve

Mi Chamba es una plataforma web que conecta personas que necesitan un servicio con profesionales que pueden realizarlo. Permite buscar proveedores, consultar sus servicios y referencias, solicitar cotizaciones, elegir una propuesta, coordinar la ejecución y registrar la finalización del trabajo.

La plataforma admite servicios **presenciales**, que requieren una ubicación y cobertura territorial, y servicios **remotos**, que se realizan mediante entregas digitales. También incorpora una tienda opcional para profesionales, comunicación entre los participantes, gestión de créditos, una billetera de dinero, verificación de identidad, atención de incidencias y administración.

Su propósito es centralizar el recorrido comercial y operativo: desde encontrar a un profesional hasta gestionar el servicio, su pago cuando corresponda y la valoración de la experiencia. En este documento, el término **contrato de servicio** se refiere al registro de la contratación dentro de la aplicación; no implica una función de firma electrónica certificada.

La versión revisada dispone de portal web adaptable a computadoras y teléfonos. Su configuración operativa contempla Venezuela y operaciones en bolívares (VES) y dólares estadounidenses (USD). Los saldos de ambas monedas se mantienen separados, sin conversión automática.

**Estado financiero actual:** están implementados los registros contables y los recorridos de prueba de recargas, reservas, liberaciones y retiros. El proveedor de pagos funciona en modo de simulación; la integración bancaria y el movimiento de dinero real requieren una etapa adicional.

## 2. Tipos de usuario

| Tipo de usuario | Función dentro de la plataforma |
| --- | --- |
| Visitante | Explorar profesionales, servicios, perfiles públicos y marketplace antes de iniciar sesión. |
| Cliente | Publicar necesidades, solicitar servicios, comparar propuestas, contratar, conversar, revisar entregas, gestionar saldo, comprar productos y solicitar soporte. |
| Profesional | Publicar sus servicios, definir cobertura y horarios, recibir oportunidades, cotizar, ejecutar trabajos, presentar entregas, administrar créditos y gestionar una tienda cuando su plan lo habilite. |
| Soporte | Atender casos, conversar con usuarios y realizar las acciones de seguimiento o moderación permitidas por su rol. |
| Administración | Gestionar usuarios, permisos, categorías, planes, políticas de retiro, verificación de identidad, moderación, operaciones e historial de auditoría. |

Una misma cuenta puede contratar como cliente y activar su perfil profesional. Las acciones administrativas dependen de permisos específicos; tener acceso al panel no habilita todas sus funciones.

## 3. Registro, cuenta y seguridad de acceso

- Registro e inicio de sesión mediante correo y contraseña.
- Gestión de datos personales, contactos y direcciones de servicio.
- Activación del perfil profesional desde la misma cuenta.
- Verificación de correo, recuperación y cambio de contraseña.
- Autenticación en dos pasos mediante códigos de una aplicación autenticadora.
- Consulta de sesiones y dispositivos conectados, con posibilidad de revocar sesiones.
- Configuración de preferencias de pago dentro de la plataforma.
- Consulta de notificaciones y archivos propios.

El envío de correos tiene un transporte SMTP configurable. En el entorno documentado, los mensajes se generan en una bandeja privada de pruebas; su entrega real depende de configurar y validar el proveedor de correo.

## 4. Perfil profesional y publicación de servicios

El profesional dispone de un perfil público y de un área privada para gestionar su oferta.

- Nombre público, identificador del perfil y presentación profesional.
- Servicios por categoría, con título, descripción, precio de referencia, moneda y modalidad presencial o remota.
- Edición y gestión de la oferta de servicios.
- Sedes de atención y decisión sobre la visibilidad pública de su dirección.
- Horarios asociados a las sedes.
- Zonas y radios de cobertura por categoría.
- Contactos profesionales compartibles según los permisos del flujo de contratación.
- Portafolio de trabajos y registro de credenciales.
- Consulta de reputación, reseñas publicadas y estado de verificación.

Un profesional puede ofrecer distintos servicios y mantener varios contratos. La asignación de un trabajo cierra la solicitud correspondiente y conserva las demás ofertas y contrataciones.

Los horarios publicados describen la atención del profesional. No se incluye en esta descripción una agenda automática de citas o una sincronización con calendarios externos.

## 5. Búsqueda, ubicación y mapas

La sección de exploración permite encontrar profesionales según el servicio que necesita el cliente.

- Búsqueda por categoría y modalidad, con filtros de precio, moneda, reputación y verificación según la consulta.
- Búsqueda presencial por cercanía y cobertura del profesional.
- Búsqueda remota sin exigir una dirección física.
- Orden por distancia y opción de recomendaciones que considera verificación, posicionamiento vigente, calificación y distancia.
- Consulta de perfiles, servicios, portafolio, reseñas y favoritos.
- Vistas independientes de listado y mapa, conservando los filtros al alternarlas.
- Marcadores de profesionales presenciales, agrupación de ubicaciones coincidentes y acceso al perfil desde el mapa.
- Uso de la ubicación del navegador con permiso del usuario o selección manual de una zona.

Las direcciones se organizan por estado, municipio y parroquia de Venezuela. El usuario indica un punto en el mapa y completa la dirección. El servidor valida la relación entre los niveles geográficos y la ubicación indicada.

Las sedes públicas pueden mostrar su ubicación exacta. Las privadas muestran una referencia aproximada de la parroquia cuando existe información suficiente. La búsqueda de servicios remotos utiliza el listado, sin mapa de cercanía.

## 6. Solicitudes, cotizaciones y contratación

### 6.1. Solicitar un servicio a un profesional concreto

1. El cliente selecciona una oferta en el perfil del profesional.
2. Describe la necesidad, elige la moneda e indica una dirección si el servicio es presencial.
3. Puede adjuntar hasta seis fotografías de referencia.
4. La solicitud se publica y se entrega al profesional elegido.
5. El profesional presenta una cotización en la moneda seleccionada.
6. El cliente revisa y acepta la propuesta.
7. El sistema registra el contrato, descuenta un crédito al profesional y habilita el chat.

El precio publicado como referencia no sustituye la cotización final. Seleccionar otra moneda no convierte automáticamente el precio anunciado.

### 6.2. Publicar una necesidad abierta

1. El cliente indica categoría, modalidad, título, descripción, moneda y presupuesto opcional.
2. Para trabajos presenciales, define dirección y radio de búsqueda.
3. Puede incorporar fotografías de referencia.
4. La solicitud queda disponible para los profesionales elegibles.
5. Los profesionales interesados presentan propuestas.
6. El cliente elige manualmente una propuesta y se crea el contrato correspondiente.

Las nuevas solicitudes se publican al enviarlas, después de las validaciones del sistema. Administración puede retirarlas posteriormente con un motivo y registro de moderación.

La selección de una propuesta asigna un único profesional, rechaza las demás propuestas pendientes y evita una segunda contratación de la misma solicitud. Si el profesional elegido no dispone del crédito necesario, la contratación no se completa.

Las fotos válidas permanecen accesibles para los profesionales elegibles mientras la solicitud está publicada y para los participantes autorizados dentro del contrato después de la asignación.

## 7. Ejecución, comunicación y finalización del servicio

### 7.1. Funciones compartidas

- Consulta de las condiciones y del estado de la contratación.
- Chat entre cliente y profesional después de la aceptación.
- Acceso a fotografías, documentos y evidencias autorizadas del trabajo.
- Registro de eventos y cambios de estado.
- Gestión de condiciones que requieren aceptación de ambas partes.
- Seguimiento desde los paneles de solicitudes y servicios.
- Registro y confirmación de pagos externos cuando corresponda al tipo de operación.

### 7.2. Servicios presenciales

El recorrido permite registrar la salida del profesional, su llegada, el inicio del trabajo, la finalización informada y la confirmación del cliente. La interfaz puede registrar llegada e inicio conjuntamente.

El cliente puede compartir la ubicación exacta del servicio dentro del contrato mediante autorización expresa, temporal y revocable. El profesional puede compartir su posición durante el trayecto. El mapa permite consultar las posiciones autorizadas y abrir la ruta en un servicio de mapas externo.

El permiso de ubicación del contrato tiene una duración de 1 a 24 horas y se revoca también al completar, cancelar o disputar el servicio. La autorización del navegador para buscar profesionales es independiente de este permiso.

### 7.3. Servicios remotos y entregas

El profesional inicia el trabajo y presenta entregables con descripción, archivos y versiones. El cliente puede solicitar correcciones, revisar una nueva versión o aceptar la entrega vigente.

En los servicios con pago protegido, el inicio exige fondos reservados. La aceptación de la entrega y la liberación del pago corresponden al cliente. El profesional no puede liberar su propio pago; tampoco se libera automáticamente por el paso del tiempo.

La finalización exige las actuaciones de ambas partes previstas en el flujo: entrega o fin informado por el profesional y aceptación del cliente. La versión actual contempla una reserva y una liberación total por contrato, sin pagos parciales por hitos.

## 8. Billetera, pagos protegidos y retiros

La billetera, denominada wallet en la interfaz, permite administrar saldo de dinero tanto a clientes como a profesionales.

- Crear solicitudes de recarga y consultar su estado.
- Consultar saldo disponible, importes reservados e historial de movimientos por moneda.
- Reservar el importe acordado para un servicio protegido.
- Liberar el importe completo a la billetera del profesional tras la validación del cliente.
- Conservar y reutilizar saldo disponible para otras contrataciones.
- Registrar métodos de retiro y consultar su estado de verificación.
- Cotizar y solicitar retiros parciales o totales del saldo disponible.
- Consultar importe solicitado, porcentaje de comisión, comisión calculada y neto a recibir.
- Seguir el estado de los retiros y gestionar devoluciones según el flujo autorizado.

El pago protegido es obligatorio en servicios remotos. En presenciales se aplica si lo exige la solicitud o la preferencia de cualquiera de las partes. Los trabajos presenciales sin protección pueden registrar el pago acordado fuera de la plataforma.

**Regla de comisión:** la comisión de la plataforma se aplica únicamente al retiro de dinero, con una política configurable por moneda. Recargar, conservar, reservar, pagar y liberar saldo no generan esta comisión. Los planes y créditos profesionales se gestionan por separado.

Los fondos reservados no se pueden gastar o retirar como saldo disponible. Un retiro cancelado antes de su envío o confirmado como fallido devuelve el importe bruto reservado. Un resultado externo desconocido conserva la reserva hasta su conciliación.

Las devoluciones de dinero de un contrato protegido siguen un proceso de solicitud, aceptación del profesional o resolución administrativa vinculada a una incidencia. La reserva contable y estos recorridos están implementados; su operación con dinero real depende del proveedor financiero pendiente de integrar.

## 9. Créditos y planes profesionales

Los créditos son unidades comerciales para la contratación del profesional, distintas del dinero de la billetera.

- Asignación de tres créditos de bienvenida una sola vez al activar el perfil profesional.
- Consumo de un crédito al quedar contratado el profesional.
- Consulta del saldo y del historial de créditos.
- Catálogo y compra de planes con precio, moneda, cantidad de créditos y beneficios.
- Beneficios configurables, como acceso al marketplace o posicionamiento temporal.
- Conservación de las condiciones de cada compra aunque cambie posteriormente el plan.
- Devolución controlada de un crédito desde soporte, vinculada a un caso y a un motivo.

Cancelar un servicio no devuelve automáticamente el crédito. Los créditos comprados no vencen en la versión descrita. Los beneficios temporales comienzan cuando se confirma el pago de la compra y no acumulan su duración automáticamente entre compras.

Los precios y las políticas del entorno de demostración no constituyen tarifas comerciales definitivas.

## 10. Marketplace y venta de productos

El profesional puede activar una tienda si dispone del beneficio correspondiente en su plan.

- Administración de la tienda y de su catálogo.
- Publicación de productos con descripción, imágenes, precio y moneda.
- Gestión de inventario y disponibilidad.
- Exploración del catálogo por parte de los compradores.
- Pedidos con varios productos de una misma tienda.
- Reserva de inventario al crear un pedido.
- Seguimiento de los estados del pedido.
- Restitución de inventario al cancelar, evitando devoluciones duplicadas.
- Comunicación autorizada de los datos de entrega por el comprador.
- Registro del pago externo y confirmación por la contraparte.

El inventario se controla para evitar que dos compradores consuman simultáneamente la última unidad. El flujo de productos utiliza pagos pactados entre las partes, sin comisión de venta de la plataforma. No se incluyen una pasarela de cobro de productos ni una integración automática con empresas de transporte.

## 11. Identidad, reputación y privacidad

### Verificación de identidad

Los usuarios pueden abrir un expediente de verificación, aportar documentos y consultar el resultado. El personal autorizado revisa el expediente y registra una decisión con vigencia cuando se aprueba.

El distintivo de verificación depende de un expediente aprobado y vigente. Comprar posicionamiento no otorga ese distintivo. Los retiros requieren verificación de identidad vigente y un método de retiro validado según el flujo financiero.

La revisión documental es manual. No se incluye una verificación automática de identidad mediante un proveedor externo.

### Reputación

Cada participante puede emitir una reseña de 1 a 5 sobre un contrato terminado, con una reseña por participante y contrato. Los promedios consideran las reseñas publicadas. El personal autorizado puede moderar su visibilidad, sin cambiar la puntuación otorgada.

### Privacidad y archivos

El listado de oportunidades del profesional no expone la identidad, contactos, dirección exacta ni coordenadas del cliente. El acceso a contactos compartibles se habilita según la participación en la contratación; la ubicación exacta exige además consentimiento expreso.

Los archivos tienen controles de propiedad, finalidad y acceso. Las imágenes admitidas se validan y recodifican para eliminar metadatos EXIF. Las fotografías de solicitudes admiten JPG, PNG o WEBP, con un máximo de 10 MB por archivo. También existen documentos PDF autorizados para fines como identidad, chat o evidencia.

## 12. Soporte, administración y notificaciones

### Atención al usuario

- Creación y consulta de casos de soporte.
- Conversación entre el usuario y el personal de atención.
- Estados de seguimiento, asignación y gestión del caso.
- Notas internas separadas de los mensajes visibles al usuario.
- Atención de incidencias de contratos, pagos y créditos mediante acciones autorizadas.

### Administración

- Panel de indicadores y colas de trabajo.
- Gestión de usuarios, estados de cuenta y roles según permisos.
- Gestión de categorías y planes.
- Configuración de comisiones de retiro por moneda.
- Moderación de solicitudes, archivos y reseñas.
- Revisión de expedientes de identidad.
- Consulta y gestión de retiros en el entorno financiero habilitado.
- Resolución de devoluciones de saldo con caso y motivo registrados.
- Consulta de eventos e historial de auditoría.

Las operaciones de soporte financiero tienen reglas específicas. El sistema no ofrece un editor libre de saldos o puntuaciones.

### Notificaciones y tareas automáticas

La plataforma genera avisos dentro de la aplicación y mantiene una cola persistente de entrega con reintentos y control de duplicados. Un proceso interno atiende esa cola, caduca solicitudes vencidas y ejecuta tareas de limpieza.

El chat y las notificaciones se actualizan mediante consultas periódicas mientras la pantalla está visible. Las notificaciones push móviles, SMS y WhatsApp requieren integraciones adicionales.

## 13. Procesos principales resumidos

| Proceso | Secuencia funcional | Resultado |
| --- | --- | --- |
| Alta profesional | Registro, activación, presentación, servicios y cobertura | Perfil profesional y créditos iniciales, concedidos una sola vez. |
| Contratación directa | Elección del servicio, solicitud, cotización y aceptación | Contrato con un profesional concreto y chat habilitado. |
| Necesidad abierta | Publicación, recepción de propuestas y selección manual | Un único contrato por solicitud y cierre de las propuestas restantes. |
| Servicio presencial | Coordinación, ubicación consentida, trayecto, llegada, ejecución y confirmación | Historial del trabajo y estado final según las confirmaciones requeridas. |
| Servicio remoto protegido | Reserva, inicio, entrega, revisión, posibles correcciones y aceptación | Entrega aprobada y liberación del importe a la billetera profesional. |
| Retiro de saldo | Método validado, cotización, solicitud, reserva y confirmación del resultado | Neto y comisión registrados, o devolución de la reserva cuando corresponde. |
| Compra de productos | Selección, pedido, reserva de existencias, aceptación, pago externo y entrega | Pedido trazable e inventario actualizado. |
| Atención de incidencia | Apertura del caso, conversación, revisión y resolución autorizada | Decisión registrada y acciones de soporte auditables. |

En la versión actual, los pasos que requieren confirmación financiera externa se ejecutan mediante el proveedor de simulación.

## 14. Arquitectura y tecnologías

| Componente | Tecnología | Responsabilidad |
| --- | --- | --- |
| Portal web | React, TypeScript y React Router | Pantallas, navegación y experiencia de clientes, profesionales y operadores. |
| Herramientas web | Vite y Node.js | Desarrollo, compilación y preparación de los recursos del portal. |
| API | Python, FastAPI y Pydantic | Operaciones HTTP, validación de entradas, autenticación y coordinación de módulos. |
| Acceso a datos | Psycopg con conexiones por rol | Comunicación parametrizada y transaccional con la base de datos. |
| Persistencia | PostgreSQL y PostGIS | Usuarios, ofertas, contratos, movimientos, inventario, auditoría y consultas geográficas. |
| Reglas de negocio | Funciones SQL transaccionales | Consistencia de asignaciones, créditos, reservas monetarias e inventario. |
| Mapas | Leaflet y mapas de Mapbox | Visualización geográfica, selección de puntos y marcadores. |
| Autenticación | Argon2, JWT y TOTP | Protección de contraseñas, acceso por sesión y autenticación en dos pasos. |
| Procesamiento interno | Trabajador Python y cola persistente | Notificaciones, vencimientos y mantenimiento. |
| Archivos | Almacenamiento privado y procesamiento con Pillow | Validación de imágenes y entrega de archivos con autorización. |
| Servicio web | Nginx, HTTPS y Docker Compose | Publicación del portal y organización de base de datos, API, web y trabajador. |
| Documentación de integración | OpenAPI y Swagger | Especificación de las operaciones y modelos de la API versionada. |
| Verificación del software | Pruebas SQL, pytest y Playwright | Comprobación de reglas de datos, API y recorridos de navegador. |

Los módulos del backend abarcan autenticación, cuenta, profesionales, solicitudes, contratos, billetera, marketplace, soporte, administración, archivos e integraciones. La API puede servir como base de una futura aplicación móvil, pero esa aplicación nativa no está incluida en la versión revisada.

### Controles técnicos relevantes

- Sesiones revocables, tokens de acceso de corta duración y renovación con rotación de tokens.
- Cookies de renovación protegidas y comprobaciones contra solicitudes de origen no autorizado.
- Contraseñas almacenadas mediante hash Argon2 y límites de intentos de autenticación.
- Autorización por usuario, rol, permiso y participación en cada operación.
- Cuentas de base de datos con permisos separados para API, autenticación, administración, facturación y tareas internas.
- Operaciones parametrizadas y funciones permitidas expresamente; el navegador no selecciona SQL arbitrario.
- Claves de reintento que evitan duplicar contrataciones, compras, mensajes y movimientos en los flujos que las utilizan.
- Bloqueos transaccionales para impedir doble adjudicación, doble consumo de saldo o venta de inventario inexistente.
- Libros separados para créditos y dinero; movimientos monetarios equilibrados por moneda y reservas diferenciadas.
- Webhooks de prueba firmados y controles de referencia, importe, moneda y duplicidad.
- Registros de auditoría e identificadores de petición para relacionar acciones con operaciones del sistema.
- Migraciones incrementales y herramientas de respaldo de la base de datos.

## 15. Alcance actual y dependencias adicionales

### Disponible en la versión revisada

Portal web adaptable, API, persistencia, cuentas, perfiles, búsqueda geográfica, mapa, solicitudes, propuestas, contratos, chat, ejecución presencial y remota, entregas, reseñas, créditos, planes, marketplace, KYC manual, soporte, administración, auditoría y notificaciones internas. También están implementados los recorridos contables y de interfaz de la billetera y el pago protegido, con confirmaciones financieras simuladas.

### Requiere una etapa o integración adicional

- Pasarela financiera real, recargas bancarias, custodia de fondos, transferencias, retiros y conciliación con el proveedor seleccionado.
- Verificación automática de identidad mediante una entidad externa.
- Aplicación móvil nativa para Android o iOS.
- Notificaciones push, mensajes SMS o WhatsApp y autenticación mediante proveedores sociales.
- Configuración y validación de entrega real de correo electrónico.
- Navegación giro a giro y cálculo de tiempo estimado de llegada dentro de Mi Chamba.
- Cobros parciales por hitos, firma electrónica certificada, facturación fiscal e integraciones contables externas.
- Automatización logística para pedidos del marketplace.
- Antivirus u OCR para documentos.
- Validación de capacidad a escala comercial y acuerdos de disponibilidad, mantenimiento y respuesta de soporte.

Los precios de planes, beneficios, porcentajes de retiro, proveedores externos, políticas de cancelación y devolución, conservación de datos, alojamiento y responsabilidades de mantenimiento deben concretarse en el alcance comercial que acuerden las partes. Los datos y tarifas de demostración no fijan esas condiciones.

## 16. Descripción breve para incorporar al contrato

Mi Chamba es una plataforma web de intermediación y gestión de servicios presenciales y remotos entre clientes y profesionales. Su alcance funcional comprende cuentas de usuario, perfiles profesionales, publicación y búsqueda de servicios, geolocalización, solicitudes de trabajo, cotizaciones, selección de propuestas, registro de contrataciones, mensajería, seguimiento de ejecución, entregables, calificaciones, créditos y planes profesionales, billetera y registro de pagos protegidos, tienda de productos, control de inventario, verificación manual de identidad, soporte y administración. La solución se compone de un portal React y TypeScript, una API Python y FastAPI, una base PostgreSQL con PostGIS y procesos internos de notificación y auditoría. La versión descrita utiliza pagos simulados; la integración financiera real, la aplicación móvil nativa y los demás servicios externos se delimitarán expresamente en el alcance contratado.

## 17. Referencias del proyecto

Esta descripción se elaboró mediante revisión del código y de la documentación local. No constituye una nueva ejecución completa de pruebas funcionales ni una certificación de operación comercial.

- `README.md`: visión general y reglas del producto.
- `frontend/src/main.tsx` y `frontend/src/pages/`: navegación y pantallas funcionales.
- `backend/app/main.py`, `backend/app/core/` y `backend/app/modules/`: arquitectura, autenticación y módulos HTTP.
- `backend/app/worker.py`: tareas internas y notificaciones.
- `db/migrations/`: modelo persistente y reglas transaccionales, incluida la publicación automática de solicitudes de la migración 028.
- `docs/customer-requests.md`: solicitudes, cotizaciones, fotografías, asignación y moderación.
- `docs/explore-map.md` y `docs/venezuela-geolocation.md`: búsqueda, mapas y privacidad geográfica.
- `docs/payments.md`: billetera, reservas, liberaciones, retiros y límites financieros.
- `docs/backend-web.md`: componentes, seguridad, operación e integraciones pendientes.
- `docs/verification.md`: evidencia de verificaciones previas documentadas.

Ante diferencias entre documentación histórica y cambios posteriores, se ha priorizado el comportamiento descrito en el código y en la documentación específica más reciente.
