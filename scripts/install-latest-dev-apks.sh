#!/usr/bin/env bash
# Downloads the latest green Android CI artifacts and installs the three dev
# APKs on the OnePlus over Tailscale ADB — no local compilation required.
# Usage: ./scripts/install-latest-dev-apks.sh [run-id]
set -euo pipefail

REPO="winkakao3-dev/vaiinilla-android"
DEVICE="${ONEPLUS_ADB:-100.103.30.42:5555}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cd "$(dirname "$0")/.."

if [[ "${1:-}" == "" ]]; then
  RUN_ID="$(gh run list --repo "$REPO" --workflow android-ci.yml --branch main \
    --status success --limit 1 --json databaseId --jq '.[0].databaseId')"
else
  RUN_ID="$1"
fi
[[ -n "$RUN_ID" ]] || { echo "No successful main CI run found yet."; exit 1; }
echo "Downloading artifacts from run $RUN_ID"
gh run download "$RUN_ID" --repo "$REPO" -n vaiinilla-dev-debug-apks -D "$WORK"

adb connect "$DEVICE" >/dev/null
declare -A PKGS=(
  [app-dev-alumno-debug.apk]=com.vaiinilla.app.dev
  [app-dev-caja-debug.apk]=com.vaiinilla.app.dev.caja
  [app-dev-cocina-debug.apk]=com.vaiinilla.app.dev.cocina
)
for apk in "${!PKGS[@]}"; do
  echo "Installing $apk -> ${PKGS[$apk]}"
  adb -s "$DEVICE" install -r -d "$WORK/$apk" | tail -1
done

echo "Installed versions on device:"
for pkg in com.vaiinilla.app.dev com.vaiinilla.app.dev.caja com.vaiinilla.app.dev.cocina; do
  printf '  %s: ' "$pkg"
  adb -s "$DEVICE" shell "dumpsys package $pkg 2>/dev/null | grep -m1 versionName" | tr -d '\r' || true
done
