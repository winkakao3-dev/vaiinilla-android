#!/usr/bin/env bash
set -euo pipefail

# Smoke autenticado con Maestro: recorre la UI con sesión real sin limpiar datos.
# Alumno navega sus 4 tabs; Caja/Cocina entran al modo operativo disponible.
# Nunca ejecuta uninstall, pm clear ni mutaciones de pedidos/pagos.

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/.."

export JAVA_HOME="${JAVA_HOME:-/Volumes/CODIGO/Android/jdk-17.0.20.1+1/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/Volumes/CODIGO/Android/LibraryAndroid/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export MAESTRO_CLI_NO_ANALYTICS=true
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

ADB_BIN="${ADB:-$ANDROID_HOME/platform-tools/adb}"
MAESTRO_BIN="/Volumes/CODIGO/Android/bin/maestro"

for arg in "$@"; do
    case "$arg" in
        *)
            echo "FAIL: unknown argument: $arg" >&2
            exit 1
            ;;
    esac
done

if [ ! -x "$MAESTRO_BIN" ]; then
    echo "FAIL: maestro wrapper missing: $MAESTRO_BIN" >&2
    exit 1
fi

if [ -n "${ANDROID_SERIAL:-}" ]; then
    serial="$ANDROID_SERIAL"
else
    count=0
    serial=""
    while IFS="$(printf '\t')" read -r dev_id dev_state; do
        [ -z "${dev_id:-}" ] && continue
        case "$dev_state" in
            device)
                count=$((count + 1))
                serial="$dev_id"
                ;;
            *)
                echo "FAIL: device $dev_id is in state '$dev_state'" >&2
                exit 1
                ;;
        esac
    done <<EOF
$("$ADB_BIN" devices | awk 'NR > 1 && NF > 0 {print $1 "\t" $2}')
EOF
    if [ "$count" -ne 1 ]; then
        echo "FAIL: expected exactly one connected device, found $count" >&2
        exit 1
    fi
fi

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
evidence_dir="/Volumes/CODIGO/Android/QA/Vaiinilla/${stamp}-maestro-auth"
mkdir -p "$evidence_dir"
echo "evidence: $evidence_dir"

run_pkg() {
    local pkg="$1"
    local flow="$2"
    local slug="${pkg##*.dev}"
    slug="${slug#.}"
    slug="${slug:-alumno}"

    echo "== $pkg ($flow)"
    "$ADB_BIN" -s "$serial" logcat -c || true

    local status=0
    "$MAESTRO_BIN" --udid "$serial" test -e APP_ID="$pkg" "$flow" || status=$?

    "$ADB_BIN" -s "$serial" logcat -d \
        | grep -E "DeviceTokenRegistrar|VaiinillaMessaging|FcmService|FirebaseMessaging" \
        >"$evidence_dir/${slug}-notifications.log" || true
    "$ADB_BIN" -s "$serial" shell dumpsys package "$pkg" \
        | grep -E "android.permission.(POST_NOTIFICATIONS|CAMERA)|firstInstallTime|versionName" \
        >"$evidence_dir/${slug}-permissions.txt" || true
    "$ADB_BIN" -s "$serial" exec-out screencap -p >"$evidence_dir/${slug}-final.png" || true

    if [ "$status" -ne 0 ]; then
        echo "FAIL: $pkg (maestro exit $status)"
        return "$status"
    fi
    echo "PASS: $pkg"
    return 0
}

failed=0
run_pkg "com.vaiinilla.app.dev" ".maestro/alumno-home.yaml" || failed=1
run_pkg "com.vaiinilla.app.dev.caja" ".maestro/staff-enter.yaml" || failed=1
run_pkg "com.vaiinilla.app.dev.cocina" ".maestro/staff-enter.yaml" || failed=1

"$ADB_BIN" -s "$serial" shell am start -W -n "com.vaiinilla.app.dev/com.vaiinilla.app.MainActivity" >/dev/null || true

if [ "$failed" -ne 0 ]; then
    echo "FAIL: authenticated smoke had failures, evidence in $evidence_dir" >&2
    exit 1
fi
echo "PASS: authenticated smoke, alumno foreground"
