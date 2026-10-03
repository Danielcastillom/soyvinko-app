# Publicación en contrata.contupana.pro

El portal de pruebas se publica en **https://contrata.contupana.pro** desde el 11 de septiembre de 2026. Conserva la marca Chamba, las cuentas ficticias y las reglas del proyecto recuperado de «Diseñar BD marketplace de servicios».

## Direcciones

| Recurso | URL |
|---|---|
| Portal y búsqueda | https://contrata.contupana.pro/explore |
| Inicio de sesión | https://contrata.contupana.pro/login |
| Registro | https://contrata.contupana.pro/register |
| Administración, con rol autorizado | https://contrata.contupana.pro/admin |
| Salud de API y PostgreSQL | https://contrata.contupana.pro/api/health |
| Documentación de API | https://contrata.contupana.pro/api/docs |

Las credenciales ficticias aleatorias están en `../.local-access.md`, privado (0600). No se publican en la web.

El catálogo se amplió a 39 profesionales y servicios de ejemplo, tres por cada una de las 13 categorías, con imágenes por categoría. Los 36 accesos adicionales están en `.demo-services-access.json` (0600). [Muestras y procedimiento de carga](service-samples.md).

Venezuela es el único país activo, con monedas VES/Bs y USD. Las migraciones 022–024 incorporan estados, municipios, parroquias y validación de direcciones. [Geolocalización, Mapbox y actualización del catálogo](venezuela-geolocation.md).

## Configuración y operación

DNS A: `contrata.contupana.pro → 82.29.62.188`. Nginx del servidor termina TLS y envía la web a `127.0.0.1:8180` y `/api/` directamente a `127.0.0.1:8181`. PostgreSQL continúa en loopback, puerto 55432. Los cuatro contenedores conservan `restart: unless-stopped` y el volumen de datos existente.

El virtual host instalado es `/etc/nginx/sites-available/contrata.contupana.pro`, enlazado desde `sites-enabled`. Su copia reproducible está en `deploy/contrata.nginx.conf`. `deploy/contrata.http.conf` sirve únicamente para emitir el primer certificado antes de activar TLS.

`backend/.env` configura:

```dotenv
APP_ENV=staging
PAYMENT_MODE=sandbox
PUBLIC_WEB_URL=https://contrata.contupana.pro
WEB_ORIGINS=https://contrata.contupana.pro
FORWARDED_ALLOW_IPS=127.0.0.1,172.22.0.1
```

`MAPBOX_PUBLIC_TOKEN` contiene el token público `pk.` usado por los mapas; `/api/v1/config` lo entrega al navegador junto con país y monedas activos. No poner un token secreto `sk.` en este campo. Las imágenes permiten `https://api.mapbox.com` en CSP y la cabecera `Permissions-Policy` permite geolocalización del propio origen. El mapa requiere conectividad con Mapbox; los selectores administrativos consultan el catálogo local.

`172.22.0.1` es la puerta de enlace verificada de `chamba_default` al publicar. Si se recrea esa red, consultar `docker network inspect chamba_default` y actualizar el valor. El proxy reemplaza las cabeceras de IP del visitante; Uvicorn solo las acepta del proxy de confianza. No configurar `FORWARDED_ALLOW_IPS=*`.

Las cookies de sesión y CSRF usan `Secure` cuando la URL pública es HTTPS; el refresh conserva HttpOnly, SameSite Strict, rotación y revocación. Los ambientes `staging` y `production` rechazan URLs públicas HTTP. Producción continúa rechazando pagos sandbox. Para iniciar sesión en este despliegue, utilizar la URL HTTPS, también desde el propio servidor.

Los registros de acceso del virtual host omiten parámetros de URL, referer, tokens y cookies. Las rutas API desconocidas devuelven JSON 404; no el HTML de React. Los archivos ocultos como `.env` devuelven 404. Se mantienen los controles de archivos privados y permisos de cada operación.

Tras modificar la aplicación:

```bash
cd /home/davebas/chamba
docker compose up -d --build --wait --no-deps api worker web
docker compose ps
curl --fail https://contrata.contupana.pro/api/health
```

Las migraciones nuevas se aplican antes de actualizar la aplicación con `python3 scripts/db.py migrate`. No modificar migraciones aplicadas ni eliminar volúmenes. Los respaldos previos a esta publicación están en `backups/`: un dump PostgreSQL y un archivo privado con el código/configuración anteriores.

## Certificado y renovación

El certificado de Let's Encrypt está en `/home/davebas/.config/letsencrypt-contrata/live/contrata.contupana.pro/`. La primera emisión vence el 10 de diciembre de 2026. El temporizador de usuario `contrata-cert-renew.timer` comprueba renovación dos veces al día con demora aleatoria y persistencia. El usuario tiene `Linger=yes`, por lo que no requiere una sesión interactiva abierta.

Los archivos de servicio/temporizador y el hook están en `deploy/`. Solo si se renueva el certificado, el hook valida Nginx y recarga su configuración. El temporizador general de Certbot no sustituye este servicio, porque la configuración de este dominio reside en el directorio del usuario.

```bash
systemctl --user status contrata-cert-renew.timer
systemctl --user list-timers contrata-cert-renew.timer
journalctl --user -u contrata-cert-renew.service --no-pager
certbot renew --dry-run --cert-name contrata.contupana.pro \
  --config-dir /home/davebas/.config/letsencrypt-contrata \
  --work-dir /home/davebas/.cache/letsencrypt-contrata \
  --logs-dir /home/davebas/.local/state/letsencrypt-contrata
```

## Verificación y límites

```bash
cd /home/davebas/chamba/backend
APP_ENV=test PUBLIC_WEB_URL=http://testserver WEB_ORIGINS=http://testserver \
  PAYMENT_MODE=sandbox .venv/bin/pytest -q
cd ../frontend
npm run build
PLAYWRIGHT_BASE_URL=https://contrata.contupana.pro npx playwright test
```

Las pruebas HTTP usan una base desechable. Las pruebas de navegador utilizan las cuentas ficticias existentes y crean actividad de prueba; no ejecutarlas sobre una base comercial. Se comprueban HTTPS, sesión después de recargar, cookies, logout, orígenes ajenos, rutas privadas, móvil, administración y contratación remota con reserva y liberación simuladas.

Esta publicación es un **entorno de pruebas accesible por internet**. El aviso de pagos simulados continúa visible. No hay pasarela bancaria, movimientos de dinero real ni proveedor automático de KYC. La verificación documental es manual. El correo de recuperación/verificación aún se guarda en la bandeja privada del servidor: falta configurar un transporte SMTP para recibir esos mensajes en buzones externos. No se han enviado mensajes reales durante la publicación.

La aplicación React Native y la integración de proveedores comerciales siguen como siguiente etapa del proyecto original.
