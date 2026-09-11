#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
START=$(date +%s)
# All current project dependencies are cached on the VPS. Offline avoids remote metadata checks.
TARGET="${1:-alumno}"
case "$TARGET" in
  alumno) TASKS=":app:installDevAlumnoDebug" ;;
  caja) TASKS=":app:installDevCajaDebug" ;;
  cocina) TASKS=":app:installDevCocinaDebug" ;;
  all) TASKS=":app:installDevAlumnoDebug :app:installDevCajaDebug :app:installDevCocinaDebug" ;;
  *)
    echo "Uso: $0 [alumno|caja|cocina|all]" >&2
    exit 1
    ;;
esac
if ! ./gradlew --offline $TASKS; then
  echo "Offline build missed a dependency; retrying with normal dependency resolution..." >&2
  ./gradlew $TASKS
fi
END=$(date +%s)
printf 'Fast debug install finished in %ss\n' "$((END-START))"
