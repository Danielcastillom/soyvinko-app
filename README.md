# Chamba · Backend Python y portal React

Portal para conectar clientes con profesionales, con API modular FastAPI, web React/TypeScript y PostgreSQL + PostGIS. Incluye contratación presencial/remota, chat, pagos protegidos contables, wallet, créditos, marketplace, KYC manual, administración y soporte.

**Portal de pruebas publicado:** [contrata.contupana.pro](https://contrata.contupana.pro) · [API y Swagger](https://contrata.contupana.pro/api/docs).

[Configuración HTTPS, renovación y operación](docs/deployment.md). Las conexiones internas continúan en `127.0.0.1:8180` (web) y `127.0.0.1:8181` (API).

- [Plan detallado y seguimiento](docs/IMPLEMENTATION_PLAN.md)
- [Instalación, arquitectura, recorridos y API para móvil](docs/backend-web.md)
- [Resultados de pruebas](docs/verification.md)
- [Contratar servicios, publicar necesidades y adjuntar fotos](docs/customer-requests.md)
- [Activación de perfiles y muestras en el área de Valera](docs/profile-activation-and-demo-area.md)
- [Contrato OpenAPI](docs/openapi.json)
- Accesos de prueba: `.local-access.md`, archivo privado con contraseñas aleatorias.

El catálogo base incluye 41 cuentas ficticias: administración, cliente y **39 profesionales con tres muestras de servicio por cada una de las 13 categorías**. Incluye imágenes ilustrativas, planes y precios de demostración en Bs y USD, una tarifa de retiro DEMO de 2 % y actividad de pruebas. **Los pagos son simulados; no se mueve dinero real.** La única comisión del producto se aplica al retirar saldo. React Native reutilizará la API v1 en una etapa posterior.

Venezuela es el único país activo. Las direcciones se eligen por estado, municipio y parroquia, con marcador Mapbox y nombre libre. La búsqueda usa la ubicación del navegador cuando el usuario concede permiso, con selección manual como alternativa. [Catálogo geográfico, monedas y operación](docs/venezuela-geolocation.md).

[Catálogo de muestras y carga repetible](docs/service-samples.md). Las cuentas adicionales se guardan en `.demo-services-access.json` (privado); los accesos originales se conservan en `.local-access.md`.

## Puesta en marcha

Requisitos: Docker Compose y Python 3.12 con venv. Node 22.20+ para desarrollar/probar el frontend fuera de Docker.

```bash
cd /home/davebas/chamba
python3 scripts/dev.py setup
python3 scripts/dev.py status
```

Para una instalación ya preparada:

```bash
python3 scripts/dev.py up
python3 scripts/dev.py stop
python3 scripts/db.py backup
```

La base `chamba` escucha en `127.0.0.1:55432`; utiliza el volumen `chamba_chamba_data`. `backend/.env` contiene conexiones restringidas y claves de aplicación; `.env` contiene la cuenta de migración. Se generan con permisos 0600 y están excluidos del repositorio. El backend no usa el propietario SQL.

Las migraciones se aplican en orden y con verificación SHA-256. No modificar migraciones aplicadas; añadir una nueva. `stop` conserva los datos. Los respaldos de PostgreSQL se guardan en `backups/`; respaldar también archivos privados y claves de cifrado por separado. No usar `docker compose down -v` para detener: elimina el volumen.

## Reglas implementadas

| Área | Comportamiento |
|---|---|
| Cuenta | Un usuario puede ser cliente y activar su perfil profesional; no necesita dos cuentas. |
| Bienvenida | **3 créditos una sola vez al activar el perfil profesional**, según la regla confirmada. |
| Modalidad | Cada servicio ofrecido se marca como presencial (`onsite`) o remoto (`remote`). Los remotos no requieren dirección ni cobertura geográfica. |
| Contratación directa | El cliente elige un servicio y describe su necesidad. El profesional cotiza en la moneda elegida y el cliente confirma la propuesta; se crea el contrato, se descuenta 1 crédito y se abre el chat en la misma transacción. Las invitaciones anteriores conservan su flujo. |
| Solicitud abierta | Los profesionales con categoría y cobertura compatibles reciben un evento; pueden proponer precio. El cliente elige manualmente una propuesta. Solo se cobra al profesional elegido. |
| Saldo insuficiente | La contratación no se completa; no se abre chat ni se adjudica la solicitud parcialmente. |
| Un ganador | Una solicitud solo puede originar un contrato, incluso si se aceptan dos propuestas simultáneamente. Un usuario puede tener muchos contratos. |
| Reintentos | Activación, contratación, compras, mensajes y pedidos tienen protección contra duplicados. Una clave reutilizada con contenido incompatible produce error donde corresponde. |
| Datos del cliente | El feed profesional no devuelve identidad del cliente, contactos, direcciones, coordenadas ni distancia al cliente. |
| Contacto | Los participantes del contrato pueden consultar sus contactos compartibles después de la aceptación mutua. |
| Ubicación | Solo el cliente la comparte expresamente dentro del contrato; el permiso dura de 1 a 24 horas y puede revocarse. También se revoca al completar, cancelar o disputar el servicio. |
| Navegación | Se entrega el destino consentido y la última posición reciente del profesional; el mapa y el cálculo de ruta pertenecen a la aplicación. |
| Trabajo presencial | Salida hacia el servicio, llegada, inicio, fin informado por el profesional y fin confirmado por el cliente. La interfaz puede registrar llegada e inicio en una sola transacción. |
| Trabajo remoto | Inicio sin navegación, entregables con versiones, revisión del cliente, solicitud de correcciones y liberación expresa del pago. |
| Finalización | Solo queda completado cuando **ambas partes** confirman. No se cierra automáticamente si falta una confirmación. |
| Calificaciones | Una por participante y contrato terminado, de 1 a 5. Promedio y recomendaciones usan reseñas publicadas. Soporte puede moderar su visibilidad, sin cambiar la puntuación. |
| Verificación | El distintivo se obtiene de un expediente KYC aprobado y vigente; no se compra con posicionamiento. |
| Cercanía | Orden predeterminado por distancia ascendente, dentro de la cobertura de una sede para la categoría buscada. |
| Posicionamiento | Orden alternativo explícito `recommended`: verificación, beneficio de posicionamiento vigente, calificación y distancia. El orden `distance` siempre mantiene la cercanía primero. |
| Planes | Créditos, importe, moneda y beneficios se copian a la compra para preservar las condiciones adquiridas aunque cambie el plan. |
| Pago protegido | Obligatorio en servicios remotos; opcional en presenciales. Si el cliente o el profesional marca que requiere pagos dentro de la plataforma, el contrato presencial también usa protección. |
| Reserva | El importe acordado sale del saldo disponible del cliente y queda reservado por contrato. El profesional no inicia hasta que esté financiado. |
| Liberación | Solo el cliente puede validar la entrega vigente y liberar el importe completo a la wallet del profesional. No existe liberación automática por tiempo ni por decisión unilateral del profesional. |
| Wallet de dinero | Clientes y profesionales pueden recargar, conservar y reutilizar saldo, y solicitar retiros parciales o totales del disponible. Cada moneda mantiene saldo independiente. |
| Comisiones | **Solo se cobra al retirar dinero**, con porcentaje configurable por administración y cotización previa del importe neto. Recargar, reservar, pagar y liberar un servicio no tiene comisión. Un retiro cancelado o fallido devuelve todo lo reservado. |
| Cobros externos | Los presenciales sin protección y los productos conservan su pago pactado entre las partes, sin comisión de contratación o venta. Los créditos y los planes mantienen su modelo separado. |
| Marketplace | Tienda opcional del profesional, productos, inventario, pedidos de varios productos de una misma tienda, estados y entrega compartida por el comprador. |
| Inventario | Reserva al crear pedido y restitución una sola vez al cancelar. Dos compradores no pueden consumir la última unidad simultáneamente. |
| Soporte | Roles separados, tickets, notas internas, moderación, suspensiones, revisión KYC y devolución de un crédito con ticket y motivo. |
| Trazabilidad | Libros de créditos y movimientos, eventos de contratación y pedidos, webhooks, historial de soporte, auditoría y cola durable de notificaciones. |

## Documentación

- [Modelo y diagrama](docs/model.md).
- [Integración y límites de seguridad](docs/integration.md).
- [Servicios remotos, wallet y retiros](docs/payments.md).
- [Diccionario de tablas generado desde PostgreSQL](docs/data-dictionary.md).
- [Firmas y permisos de las funciones](docs/api-reference.md).
- [Migraciones SQL](db/migrations/).
- [Pruebas ejecutables](tests/run.py).
- [Resultados de verificación](docs/verification.md).

## Decisiones comerciales pendientes

- Definir precios comerciales de los planes para Venezuela. Las monedas operativas son VES y USD; el dinero se guarda en unidades menores enteras, con dos decimales. No se aplica conversión automática entre monedas.
- Configurar qué planes activan marketplace y cuánto dura su posicionamiento. Los beneficios empiezan al confirmarse el pago; compras con beneficios temporales se solapan, no se acumulan automáticamente. Los créditos comprados no vencen en esta versión.
- Definir el porcentaje de retiro por moneda. La tarifa DEMO no define una tasa comercial: los retiros requieren una política configurada, que puede ser 0 %. La comisión se descuenta del importe solicitado y solo se contabiliza al confirmarse el retiro.
- Definir reglas adicionales de cancelación, reembolso de compras de planes, contracargos y disputas. Cancelar un servicio **no devuelve automáticamente** el crédito; soporte dispone de una devolución controlada. El dinero reservado puede devolverse a la wallet mediante aceptación del profesional o resolución de soporte. Contracargos y reembolsos bancarios requieren conciliación con el proveedor.
- Definir proveedores de pagos, KYC, almacenamiento y notificaciones, así como retención de datos. Mapbox ya proporciona los mapas. No se han procesado verificaciones externas ni pagos reales.


## Alcance de esta entrega

Implementados el backend HTTP, autenticación, portal React, paneles, trabajador interno y simulación firmada de pagos, sobre 85 tablas, 26 migraciones y 167 funciones SQL. La API expone 157 operaciones HTTP. Los perfiles incluyen servicios, sedes, cobertura, horarios, portafolio y credenciales; los paneles consumen datos persistidos.

La pasarela y custodia bancaria reales, automatización KYC externa, push móvil, cálculo de ETA, operación a escala y aplicación React Native requieren la siguiente etapa. Consulta los límites concretos y recorridos en [backend y web](docs/backend-web.md).
