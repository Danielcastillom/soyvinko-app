#!/usr/bin/env bash
# Publica una versión de la app Android para la autoactualización obligatoria.
#
#   scripts/publish-release.sh --notes "Novedad 1|Novedad 2" [--optional] [--min-version N] [--out DIR]
#
# 1. Sube antes soyvinko.versionCode (y soyvinko.versionName) en gradle.properties.
# 2. Compila el APK de release firmado con ~/.soyvinko/android/signing.properties (la clave NO cambia nunca).
# 3. Copia el APK y escribe latest.json en frontend/public/downloads/android/ (o --out para una prueba).
# 4. Despliega la web: docker compose build web && docker compose up -d web
#    Las apps instaladas detectan la versión al abrirse (o al volver a ellas) y se actualizan solas.
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=$(pwd)
SIGNING_DIR="${SOYVINKO_SIGNING_DIR:-$HOME/.soyvinko/android}"
OUT="$ROOT/../../frontend/public/downloads"
BASE_URL="https://soyvinko.com/downloads"
NOTES=""; MANDATORY=true; MIN_VERSION=0

while [ $# -gt 0 ]; do
  case "$1" in
    --notes) NOTES="$2"; shift 2 ;;
    --optional) MANDATORY=false; shift ;;
    --min-version) MIN_VERSION="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    *) echo "Opción desconocida: $1" >&2; exit 2 ;;
  esac
done

[ -f "$SIGNING_DIR/signing.properties" ] || { echo "Falta $SIGNING_DIR/signing.properties (clave de firma)." >&2; exit 1; }
CODE=$(grep '^soyvinko.versionCode=' gradle.properties | cut -d= -f2)
NAME=$(grep '^soyvinko.versionName=' gradle.properties | cut -d= -f2)
mkdir -p "$OUT/android"
if [ -f "$OUT/android/latest.json" ]; then
  PUBLISHED=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['versionCode'])" "$OUT/android/latest.json")
  if [ "$CODE" -le "$PUBLISHED" ]; then
    echo "soyvinko.versionCode=$CODE no es mayor que la versión publicada ($PUBLISHED). Súbelo en gradle.properties." >&2
    exit 1
  fi
fi

echo "Compilando SoyVinko $NAME ($CODE) firmado…"
LOCK="${TMPDIR:-/tmp}/soyvinko-android-build.lock"
exec 9>"$LOCK"; flock 9
GRADLE_HOME_DIR="${SOYVINKO_GRADLE_HOME:-$HOME/.soyvinko/gradle-cache}"; mkdir -p "$GRADLE_HOME_DIR"
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp/home -e GRADLE_USER_HOME=/gradle \
  -v "$GRADLE_HOME_DIR":/gradle -v "$ROOT":/project -v "$SIGNING_DIR":/signing:ro -w /project \
  ghcr.io/cirruslabs/android-sdk:35 bash -c '
    set -e; mkdir -p /tmp/home; echo sdk.dir=/opt/android-sdk-linux > local.properties
    rm -rf app/build/outputs/apk/release
    ./gradlew --no-daemon assembleRelease -PsoyvinkoSigning=/signing/signing.properties
    test -f app/build/outputs/apk/release/app-release.apk
    # La firma debe ser la de la clave permanente o las apps instaladas rechazarán la actualización.
    $ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk | grep "SHA-256" | head -1 > app/build/outputs/apk/release/cert.txt
  '
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
CERT=$(sed 's/.*: //' "$ROOT/app/build/outputs/apk/release/cert.txt")
EXPECTED=$(docker run --rm -u "$(id -u):$(id -g)" -v "$SIGNING_DIR":/k:ro ghcr.io/cirruslabs/android-sdk:35 bash -c \
  'keytool -list -v -keystore /k/$(grep storeFile /k/signing.properties | cut -d= -f2) -storepass "$(grep storePassword /k/signing.properties | cut -d= -f2)" 2>/dev/null | grep "SHA256:" | head -1 | sed "s/.*SHA256: //; s/://g" | tr A-F a-f')
[ "$CERT" = "$EXPECTED" ] || { echo "La firma del APK ($CERT) no coincide con la clave permanente ($EXPECTED)." >&2; exit 1; }

FILE="soyvinko-$NAME.apk"
cp "$APK" "$OUT/android/$FILE.tmp" && mv "$OUT/android/$FILE.tmp" "$OUT/android/$FILE"
cp "$APK" "$OUT/soyvinko-android.apk.tmp" && mv "$OUT/soyvinko-android.apk.tmp" "$OUT/soyvinko-android.apk"   # botón de descarga de la web
SHA=$(sha256sum "$OUT/android/$FILE" | cut -d' ' -f1)
SIZE=$(stat -c %s "$OUT/android/$FILE")
python3 - "$OUT/android/latest.json" "$CODE" "$NAME" "$MIN_VERSION" "$MANDATORY" "$BASE_URL/android/$FILE" "$SHA" "$SIZE" "$NOTES" <<'PY'
import json, os, sys, datetime
path, code, name, minv, mandatory, url, sha, size, notes = sys.argv[1:]
data = {
    "versionCode": int(code), "versionName": name, "minVersionCode": int(minv),
    "mandatory": mandatory == "true", "url": url, "sha256": sha, "size": int(size),
    "notes": [n.strip() for n in notes.split("|") if n.strip()],
    "publishedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
}
tmp = path + ".tmp"
with open(tmp, "w") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
os.replace(tmp, path)
print(json.dumps(data, ensure_ascii=False, indent=2))
PY
# Conserva solo las dos versiones más recientes.
ls -1t "$OUT"/android/soyvinko-*.apk | tail -n +3 | xargs -r rm -f
echo
echo "Publicado SoyVinko $NAME ($CODE) en $OUT. Despliega la web para que las apps lo detecten:"
echo "  docker compose build web && docker compose up -d web"
