# SoyVinko para iPhone

La app de iPhone es la misma que la de Android: todas las pantallas, la API, la sesión, los mapas y
el GPS viven en el módulo Kotlin Multiplatform `../shared` (Compose Multiplatform). Esta carpeta solo
contiene el contenedor de iOS:

- `project.yml`: proyecto de Xcode descrito con XcodeGen (el `.xcodeproj` se genera, no se guarda en git).
- `SoyVinko/`: arranque SwiftUI que muestra `MainViewController()` del módulo compartido, iconos y colores.

Antes de enlazar la app, Xcode ejecuta `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, que
compila el framework `Shared`.

## Compilar en la nube con ios-builder

La compilación usa [ios-builder](https://github.com/MobAI-App/ios-builder): un Mac de GitHub Actions
compila la app y devuelve el `.ipa`. La configuración está en `builder.json` (raíz del repositorio) y en
`.github/workflows/ios-build.yml`.

1. Crear un repositorio en GitHub (puede ser privado) y subir este proyecto (la rama principal debe
   incluir `.github/workflows/`).
2. Completar `github.owner` y `github.repo` en `builder.json`.
3. Iniciar sesión una vez: `builder auth github`.
4. Compilar: `builder ios build` (sin firma: sirve para el simulador y para revisar que compila).

## Instalar en un iPhone real

iOS solo instala apps firmadas. Hace falta una cuenta del Apple Developer Program (99 USD al año) y
cargar el certificado y el perfil de aprovisionamiento como secretos del repositorio
(`builder ios signing` guía el proceso). Con eso:

- `builder ios build --profile development`: instala en los iPhone registrados en la cuenta.
- `builder ios release`: sube la versión a TestFlight / App Store.

Las actualizaciones obligatorias del APK de Android no aplican en iOS: allí las versiones nuevas
llegan por TestFlight o el App Store.

## Versión

`MARKETING_VERSION` y `CURRENT_PROJECT_VERSION` en `project.yml` siguen a `soyvinko.versionName` y
`soyvinko.versionCode` de `../gradle.properties`.
