# Plan de implementación · backend Python y portal React

## Objetivo y orden

Entregar primero una API modular sobre PostgreSQL, después un portal web funcional, y verificar los recorridos completos. La futura aplicación React Native reutilizará `/api/v1`, sus modelos OpenAPI y las mismas reglas de negocio. No se construye aún una aplicación móvil nativa.

## 1. Fundamentos y contratos de la API

- Conservar las migraciones existentes y añadir cambios de forma incremental.
- Crear backend con FastAPI, Pydantic y conexiones Psycopg separadas por rol de base de datos.
- Organizar cada dominio en un módulo: autenticación, cuenta, profesionales, solicitudes, contratos/chat, dinero/créditos, tienda, soporte y administración.
- Publicar rutas versionadas, OpenAPI, errores coherentes, identificador de petición, validación e idempotencia.
- Mantener las reglas transaccionales en las funciones SQL ya probadas. Las rutas serán una lista explícita, no ejecución arbitraria de funciones recibidas del navegador.
- Aprovisionar logins SQL limitados; el backend no usa el propietario de la base.

Aceptación: API saludable, conexión real a PostgreSQL, OpenAPI disponible y operaciones privadas sin autenticación rechazadas.

## 2. Identidad y seguridad

- Registro con Argon2id, inicio de sesión, JWT de acceso de corta duración y refresh tokens rotatorios revocables.
- Web: refresh en cookie HttpOnly y protección CSRF; token de acceso en memoria. Cliente nativo: refresh explícito para almacenamiento seguro del dispositivo.
- Autorización por sesión y permisos de aplicación; suspensión y cierre de sesión invalidan acceso.
- Recuperación de contraseña, verificación de correo, cambio de contraseña, sesiones y MFA TOTP.
- Límites persistentes para autenticación, CORS de orígenes explícitos, límites de archivos y secretos fuera del repositorio.

Aceptación: registro/login/refresh/logout, intento de escalamiento, sesión suspendida, CSRF y autenticación inválida probados contra HTTP.

## 3. Módulos funcionales del backend

| Módulo | Operaciones |
|---|---|
| Cuenta | Perfil, contactos, direcciones, preferencias de pago, sesiones y notificaciones. |
| Profesionales | Activación y bono de 3 créditos, servicios presenciales/remotos, sedes, cobertura, portafolio, credenciales, favoritos y reputación. |
| Solicitudes | Crear solicitudes directas/abiertas, adjuntos privados, revisión/publicación, búsqueda por cercanía/remota y propuestas. |
| Contratos | Aceptación, condiciones, chat, contacto autorizado, consentimiento de ubicación, navegación, llegada/inicio, entregas, correcciones, liberación y reseñas. |
| Dinero | Recargas, saldo disponible/reservado, reserva de contrato, devolución, métodos de retiro, cotización de comisión y retiro parcial/total. |
| Créditos | Saldo/libro, catálogo de planes y compra idempotente. |
| Marketplace | Tienda, productos e imágenes, inventario, pedidos, entrega y pagos externos registrados. |
| Soporte | Tickets, conversación del caso, notas internas, estados y asignación. |
| Administración | Indicadores, usuarios/roles, moderación, KYC, planes, tasa de retiro, reservas/disputas, retiros y trazabilidad. |
| Archivos | Carga con tipo/tamaño verificado, eliminación de EXIF en imágenes, almacenamiento privado y acceso autorizado. |

Aceptación: cada operación existente tiene ruta o adaptación específica apropiada; las funciones de banco, autenticación interna y trabajadores no se exponen como acciones ordinarias del usuario.

## 4. Integraciones y trabajo en segundo plano

- Proveedor de pagos intercambiable. Modo local de pruebas identificado; ninguna confirmación del navegador se confunde con confirmación bancaria.
- Contrato de webhook firmado, verificación temporal, eventos únicos e idempotencia. Destinos y retiros validan titularidad y estado.
- Trabajador de notificaciones en aplicación y tareas de expiración, reutilizando la cola durable.
- Transporte de correo configurable; en desarrollo, bandeja local privada para verificar flujos sin enviar mensajes reales.
- La selección e integración comercial del proveedor de pagos/KYC/mapas queda separada de la simulación local y requiere su configuración real.

Aceptación: simular recarga, financiación, entrega, liberación y retiro sin afectar dinero real; un callback sin firma o un usuario que intente confirmar dinero ajeno no puede hacerlo.

## 5. Portal React + TypeScript

- Sistema visual de Chamba: navegación clara, adaptación a móvil, estados vacíos, carga, errores y confirmación de acciones sensibles.
- Explorar profesionales con filtros, perfiles, categorías y contratación.
- Registro/login y panel de cliente: solicitudes, servicios contratados, chat, wallet, pedidos, perfil y soporte.
- Panel profesional: servicios, sedes, cobertura, oportunidades, propuestas, contratos, entregas, créditos y tienda opcional.
- Panel administrativo con permisos, colas de moderación/KYC, gestión de usuarios, soporte, planes, comisión y operaciones financieras.
- Formularios conectados a la API real; no saldos ni contratos simulados solo en el navegador.
- Sesión compartida con API, renovación controlada, navegación por rutas y cliente TypeScript reutilizable conceptualmente por React Native.

Aceptación: recorridos reales desde navegador, persistencia tras recarga, rutas protegidas, estados de operación claros y sin desbordamiento en móvil/escritorio.

## 6. Instalación, validación y entrega

- Instalar dependencias con versiones registradas; ofrecer comandos de desarrollo y Docker Compose para API, trabajador y web.
- Inicializar acceso administrativo local y perfiles ficticios identificados para explorar el portal; contraseñas aleatorias en archivo privado, sin credenciales predeterminadas en código.
- Ejecutar pruebas de la base, pruebas HTTP, compilación TypeScript/Vite y comprobaciones de navegador.
- Actualizar documentación, cobertura de rutas y límites de integración externa.
- Dejar funcionando la web y el backend localmente, sin publicar un dominio ni activar cobros reales.

## Reglas comerciales que no se cambian

1. Tres créditos una sola vez al activar profesional; uno al quedar contratado.
2. Privacidad del cliente antes de aceptación mutua; ubicación exacta únicamente por consentimiento explícito.
3. Remotos siempre con pago protegido; presencial según solicitud/preferencias.
4. El cliente valida la entrega y libera el importe íntegro al profesional.
5. Recarga, conservación, uso y liberación de saldo sin comisión; la tasa configurable se cobra solo al confirmar un retiro.
6. Dinero y créditos son libros separados. No se gastan ni retiran fondos reservados.

## Seguimiento

- [x] Fundamentos y migración de integración.
- [x] Autenticación y seguridad HTTP.
- [x] Módulos y rutas funcionales.
- [x] Integraciones locales y trabajador.
- [x] Portal web de cliente/profesional.
- [x] Administración y soporte.
- [x] Pruebas automatizadas y navegador.
- [x] Entorno local y documentación final.

## Entrega y evidencia

Implementados los seis bloques del plan. Entorno local con 80 tablas, 21 migraciones, 156 funciones SQL y 148 operaciones HTTP; web en 8180, API en 8181 y trabajador de notificaciones activo.

Se verificaron 55 casos SQL, 10 recorridos HTTP y los cuatro escenarios de navegador descritos en [verification.md](verification.md). La instalación, estructura de módulos, autenticación para web/móvil y recorridos manuales están en [backend-web.md](backend-web.md).

El modo financiero entregado es sandbox, con datos ficticios y sin cobros reales. Proveedores comerciales, custodia bancaria, push, OTP por teléfono, carga de producción y React Native pertenecen a la siguiente etapa externa descrita en ese documento.

## Continuación autorizada: publicación web

El 11 de septiembre se recuperó la conversación original y se solicitó publicar `contrata.contupana.pro`. Esta autorización amplía la entrega local del bloque 6.

- [x] Recuperar requisitos y estado de implementación.
- [x] Respaldar base y configuración anterior.
- [x] Añadir modo de pruebas público con HTTPS y cookies seguras.
- [x] Configurar dominio, proxy web/API y certificado.
- [x] Verificar recorridos en el dominio y renovación del certificado.

Configuración reproducible y límites en [deployment.md](deployment.md).
