# Activación de perfiles y muestras cercanas

Actualización del 11 de septiembre de 2026 para `contrata.contupana.pro`.

## Error del nombre de perfil

La columna `app.professionals.slug` exige letras minúsculas, números y guiones, mediante `^[a-z0-9]+(-[a-z0-9]+)*$`. El formulario enviaba literalmente “Develope Academy”, con mayúsculas y espacio; la restricción SQL lo rechazaba y la API mostraba un error genérico.

La interfaz convierte el campo al salir de él y al enviar. La API también normaliza antes de validar, por lo que otros clientes reciben el mismo comportamiento: `Develope Academy` → `develope-academy`. La conversión elimina marcas diacríticas y sustituye separadores por un guion; los nombres deben contener al menos una letra o número y no superar 80 caracteres después de normalizar.

Una colisión devuelve `professional_slug_taken` con un mensaje en español junto al campo y conserva los demás valores del formulario. Los valores sin letras/números se rechazan con 422 y detalle de campo. Se mantiene la restricción única de PostgreSQL. La función SQL de activación y su bloqueo no se modificaron: los reintentos y activaciones simultáneas conceden los tres créditos de bienvenida una sola vez.

Código: `backend/app/core/profile.py`, validación específica en `routing.py`, mensajes en `errors.py`, `frontend/src/professionalSlug.ts` y el campo de activación de `Profile.tsx`. La opción `normalize` de `FieldsForm` solo actúa en campos que la declaran; no transforma contraseñas ni otros datos.

## Distribución de las muestras

Se usó la dirección “Casa” autorizada en Valera como referencia. `scripts/relocate_demo_services.py` comprobó propietario, país y vigencia, y reubicó únicamente las 30 sedes privadas reconocidas por slug, correo de demostración y servicio esperado. Los puntos se distribuyen entre 2 500 y 42 500 metros de la referencia y pertenecen a parroquias del catálogo venezolano; se actualizaron estado, municipio y parroquia de cada sede. No se colocaron sedes sobre el punto exacto de la vivienda.

Las 30 coberturas de demostración usan ahora 50 000 metros. El cliente ficticio conserva una dirección compatible con el catálogo reubicado. Se mantienen los 39 servicios y sus precios/imágenes, incluidos nueve remotos. La búsqueda desde la dirección real devuelve 30 profesionales y cada una de las diez categorías presenciales devuelve tres muestras a 50 km.

La operación se ejecuta en una transacción con bloqueo compartido con los demás cargadores de muestras. Por defecto es vista previa con rollback; `--apply` requiere respaldo previo de los registros que va a cambiar. Los parámetros `--address` y `--owner` son UUID de una dirección guardada y su propietario. No se imprimen las coordenadas de la vivienda ni se guardan en el archivo de configuración de la distribución.

`.demo-geography-override.json` (0600, excluido del repositorio) identifica las sedes reubicadas y la dirección de referencia por UUID. El cargador `localize_venezuela_samples.py` respeta esa distribución en este entorno; una instalación nueva conserva la semilla genérica de Caracas/Maracaibo/Valencia. Una segunda vista previa de la reubicación y del cargador anterior produjo cero modificaciones.

## Verificación y respaldos

Se aprobaron nueve pruebas nuevas de activación (conversión, Unicode, valores inválidos, nombre ocupado, repetición y concurrencia) y las 51 pruebas HTTP completas en 72.53 s. TypeScript/Vite, comprobaciones Python, importación de la imagen API y configuración Nginx correctos.

Antes de las pruebas públicas se comprobaron huellas de diez tablas históricas/financieras y de todas las direcciones y sedes de usuarios fuera del dominio de demostración. Coincidieron antes y después de la reubicación; la dirección de referencia del usuario se conservó.

Respaldo de base: `backups/chamba-20260911T045341Z.dump`. Registros de las muestras antes de moverlas: `backups/demo-relocation-20260911T045343Z.json`. Huellas: `backups/demo-relocation-protected-{before,after}-20260911T045343Z.json`. Código previo en ejecución: `backups/profile-fix-{api,worker,web}-before-20260911T045343Z.tar`. Los respaldos son privados (0600).

Las pruebas de navegador están en `frontend/e2e/professional-activation.spec.ts` y `frontend/e2e/demo-nearby.spec.ts`. La primera requiere `CHAMBA_TEST_ACTIVATION=1` fuera de test y crea únicamente una cuenta ficticia sin servicios publicados. La segunda consulta el catálogo desde la dirección del cliente de demostración. `customer-requests.spec.ts` selecciona esa dirección por nombre, para que el recorrido presencial respete la distribución vigente.

Los tres recorridos pasaron en el dominio HTTPS en 10.2 s. Capturas: [activación móvil](screenshots/profile-activation-mobile.png) y [muestras cercanas](screenshots/demo-nearby-valera.png). Los archivos públicos `index-CFmGGFi3.js` e `index-C9a8_wZG.css` coincidieron por SHA-256 con el build local; la API respondió saludable con la base conectada.
