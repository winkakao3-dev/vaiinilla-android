#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
STAGING_DIR="/srv/chatgpt-workspace/apk_staging"
mkdir -p "$STAGING_DIR"

TARGET="${1:-all}"
case "$TARGET" in
  alumno|caja|cocina|all) ;;
  *)
    echo "Uso: $0 [alumno|caja|cocina|all]" >&2
    exit 1
    ;;
esac

if [[ "$TARGET" == "all" ]]; then
  ROLES=(alumno caja cocina)
else
  ROLES=("$TARGET")
fi

capitalize() {
  echo "${1^}"
}

TASKS=()
for role in "${ROLES[@]}"; do
  TASKS+=(":app:assembleDev$(capitalize "$role")Debug")
done

echo "==> Compilando en una sola invocación de Gradle: ${ROLES[*]}"
./gradlew "${TASKS[@]}"

for role in "${ROLES[@]}"; do
  flavor="dev$(capitalize "$role")"
  cp "app/build/outputs/apk/${flavor}/debug/app-dev-${role}-debug.apk" "$STAGING_DIR/vaiinilla-dev-${role}.apk"
  echo "  -> $STAGING_DIR/vaiinilla-dev-${role}.apk"
done

echo "==> Listo. No se tocó git ni ningún archivo del árbol de trabajo."
ls -lh "$STAGING_DIR"/vaiinilla-dev-*.apk
