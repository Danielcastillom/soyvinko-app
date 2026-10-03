# SoyVinko · App Android nativa

App Android en **Kotlin + Jetpack Compose (Material 3)** con el diseño del portal: morado `#6F3DFF`, Manrope en títulos y DM Sans en textos.

## Qué incluye (v1.2.0)
Todas las secciones son nativas; la app no abre la web. Solo abre apps del teléfono para llamar, WhatsApp, correo, navegación en Maps y la app autenticadora.

**Cuenta y acceso**
- Bienvenida.
- Inicio de sesión con código de 6 dígitos por correo.
- Registro de persona o empresa.
- Recuperar contraseña.
- Términos y privacidad nativos.
- Sesión persistente cifrada (Android Keystore) y renovación automática.

**Inicio ("Mi resumen", igual que la web)**
- Progreso "Completa tu cuenta" e indicadores de solicitudes, servicios, saldo y Vinkos.
- "Requiere tu atención", servicios con filtros y avance, novedades y solicitudes recientes.

**Explorar** (`ui/feature/explore`)
- Búsqueda por categoría, texto, cercanía (GPS o zona elegida en el mapa), filtros y mapa de profesionales.
- Perfil público y favoritos.
- Crear solicitudes (directa a un servicio, o abierta presencial/remota) con fotos y dirección.
- Mis solicitudes, propuestas recibidas y aceptación.

**Servicios** (`ui/feature/contracts`)
- Lista y detalle con el próximo paso según el rol.
- Condiciones, pago protegido, entregas, pagos directos, calificaciones, disputas y reembolsos.
- Chat con fotos.
- **Ubicación en vivo:**
  - El cliente comparte, de 1 a 24 h, su GPS en vivo o la dirección del servicio, y ve la última posición del profesional.
  - El profesional ve la ruta y transmite su posición mientras va en camino; puede abrir Maps.
  - Estado visible para ambos: activa o no, origen y vencimiento.
- **Seguimiento en segundo plano:** con la app cerrada sigue compartiendo la posición, con una notificación permanente y el botón "Detener".

**Wallet** (`ui/feature/wallet`)
- Vinkos y saldos en Bs y US$.
- Compra de Vinkos con saldo, recargas y paquetes.
- Historial de movimientos.
- Retiros y cuentas bancarias o de Pago Móvil.

**Perfil** (`ui/feature/profile`)
- Cuenta y contactos.
- Direcciones con estado, municipio y parroquia, GPS y mapa.
- Verificación de identidad con cámara y lectura automática de la cédula.
- Seguridad: contraseña, sesiones y verificación en dos pasos.
- Novedades, soporte y legales.

**Panel profesional y marketplace** (`ui/feature/pro`)
- Activación del perfil profesional.
- Servicios, sedes y cobertura, portafolio, credenciales y contactos.
- Eliminar cualquiera de ellos: los servicios con historial se archivan.
- Oportunidades y propuestas.
- Marketplace con carrito y pedidos.
- Mi tienda, con fotos de productos (aviso "En revisión" y opción de quitarlas).

**Administración** (`ui/feature/admin`, solo personal: roles admin, superadmin o soporte)
- Panel general con indicadores y las secciones que permite tu rol.
- Usuarios, verificación de identidad, retiros, soporte, moderación, planes, comisiones, tasas de Vinkos, tiendas, pedidos y auditoría.

**Actualizaciones**
- Obligatorias y automáticas: ver la sección más abajo.

## Conexión con los datos
La app **no se conecta a PostgreSQL**. Poner credenciales de base de datos dentro de un APK permitiría a cualquiera extraerlas. Usa la **API v1** (`/api/v1/auth/*` y `/api/v1/account/dashboard`) con `client: "native"`, y la API aplica las mismas reglas, permisos y límites que la web. Solo se permite HTTPS (`network_security_config.xml`).

URL por defecto: `https://soyvinko.com/`. Para otra:
```bash
./gradlew assembleDebug -PapiBaseUrl=https://contrata.contupana.pro/
```

## Compilar
Requiere JDK 17 y Android SDK 35 (o Android Studio Ladybug o superior).
```bash
./gradlew testDebugUnitTest      # 134 pruebas: cliente HTTP, actualizador y pantallas de cada sección
./gradlew recordRoborazziDebug   # capturas de las pantallas en app/build/outputs/roborazzi
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
```
Sin Android instalado, con Docker:
```bash
docker run --rm -u $(id -u):$(id -g) -e HOME=/tmp -v "$PWD":/project -w /project \
  ghcr.io/cirruslabs/android-sdk:35 ./gradlew assembleDebug
```

## Actualizaciones obligatorias y automáticas
La app consulta `https://soyvinko.com/downloads/android/latest.json` al abrirse y al volver a ella (cada 30 min como máximo).

**Con versión nueva obligatoria** (por defecto, o si la instalada es menor que `minVersionCode`):
1. Bloquea la app con la pantalla "Actualización obligatoria".
2. Descarga el APK sola, con barra de progreso.
3. Verifica el tamaño, el SHA-256 y que provenga del mismo dominio que el manifiesto.
4. Lanza la instalación con `PackageInstaller`.

Lo que hace Android depende de la situación:
- **Primera autoactualización:** Android pide una sola vez el permiso "Instalar apps de esta fuente" y luego una confirmación ("Actualizar").
- **Android 12 o superior, desde la segunda:** la app ya es su propio instalador, así que puede instalar sin preguntar.
- **Opcionales** (`--optional`): muestran un aviso con "Actualizar / Más tarde".

### Publicar una versión
1. Sube `soyvinko.versionCode` (+1) y `soyvinko.versionName` en `gradle.properties`.
2. Ejecuta:
   ```bash
   scripts/publish-release.sh --notes "Novedad 1|Novedad 2"      # obligatoria
   scripts/publish-release.sh --notes "..." --optional             # opcional
   scripts/publish-release.sh --notes "..." --optional --min-version 5
   ```
   El script:
   - compila el APK de release firmado y comprueba que la firma sea la clave permanente;
   - lo copia a `frontend/public/downloads/android/` y al enlace del botón de la web (`/downloads/soyvinko-android.apk`);
   - escribe `latest.json`.
3. Despliega la web: `docker compose build web && docker compose up -d web`.

### Clave de firma (crítica)
Está en `~/.soyvinko/android/` (`soyvinko-release.jks` y `signing.properties`, permisos 600, fuera del repositorio). Huella SHA-256: `71:57:C6:27:D4:5E:83:C8:65:AE:DD:0F:F1:02:D3:EE:B5:A1:B3:79:EB:FE:88:34:14:B5:59:A4:BE:EA:28:60`.

**Haz una copia de seguridad cifrada fuera del servidor.** Si se pierde, ninguna app instalada podrá actualizarse: habría que desinstalar e instalar de nuevo en todos los teléfonos.

## Publicar en Google Play
Google Play no permite que una app se actualice a sí misma (permiso `REQUEST_INSTALL_PACKAGES`). Para la tienda habría que quitar el módulo `update/` y usar las actualizaciones de Play (In-App Updates en modo inmediato).

1. Crear una clave de firma: `keytool -genkey -v -keystore soyvinko-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias soyvinko`.
2. Guárdala fuera del repositorio; si se pierde, no se puede actualizar la app.
3. Añadir `signingConfigs` en `app/build.gradle.kts` (leyendo la contraseña de variables de entorno).
4. Generar `./gradlew bundleRelease` y subir el `.aab` a Play Console.

## Estructura
```
app/src/main/java/com/soyvinko/app/
  data/        Modelos, ApiClient (OkHttp, renovación de sesión), AuthRepository, SessionStore cifrado
  ui/          ViewModels (sesión, login, registro, recuperación, inicio)
  ui/theme/    Colores y tipografías de la marca
  ui/components/  Campos, botones, avisos, código de 6 dígitos, requisitos de contraseña
  ui/screens/  Bienvenida, Login, Registro, Recuperar, Inicio
```
