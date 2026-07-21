#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

GRADLE_VERSION=8.13
DIST_SHA256=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
WRAPPER_SHA256=81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f

./gradlew wrapper \
  --gradle-version "$GRADLE_VERSION" \
  --distribution-type bin \
  --gradle-distribution-sha256-sum "$DIST_SHA256"

# La segunda ejecución actualiza también scripts y JAR con la versión seleccionada.
./gradlew wrapper \
  --gradle-version "$GRADLE_VERSION" \
  --distribution-type bin \
  --gradle-distribution-sha256-sum "$DIST_SHA256"

JAR=gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$JAR" ]; then
  echo "No se generó $JAR" >&2
  exit 1
fi

if command -v shasum >/dev/null 2>&1; then
  ACTUAL=$(shasum -a 256 "$JAR" | awk '{print $1}')
elif command -v sha256sum >/dev/null 2>&1; then
  ACTUAL=$(sha256sum "$JAR" | awk '{print $1}')
else
  echo "No se encontró shasum/sha256sum para verificar el wrapper." >&2
  exit 1
fi

if [ "$ACTUAL" != "$WRAPPER_SHA256" ]; then
  echo "Checksum inesperado para gradle-wrapper.jar." >&2
  echo "Esperado: $WRAPPER_SHA256" >&2
  echo "Obtenido: $ACTUAL" >&2
  exit 1
fi

echo "Wrapper estándar Gradle $GRADLE_VERSION generado y verificado."
