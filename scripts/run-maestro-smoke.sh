#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/.."

export JAVA_HOME="${JAVA_HOME:-/Volumes/CODIGO/Android/jdk-17.0.20.1+1/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/Volumes/CODIGO/Android/LibraryAndroid/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export MAESTRO_CLI_NO_ANALYTICS=true
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

ADB_BIN="${ADB:-$ANDROID_HOME/platform-tools/adb}"
MAESTRO_BIN="/Volumes/CODIGO/Android/bin/maestro"

flow=".maestro/launch-smoke.yaml"
for arg in "$@"; do
    case "$arg" in
        --fresh) flow=".maestro/fresh-install-smoke.yaml" ;;
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

if [ ! -f "$flow" ]; then
    echo "FAIL: missing flow: $flow" >&2
    exit 1
fi

pkg_ids=(
    "com.vaiinilla.app.dev"
    "com.vaiinilla.app.dev.caja"
    "com.vaiinilla.app.dev.cocina"
)

for pkg in "${pkg_ids[@]}"; do
    echo "maestro smoke: $pkg"
    "$MAESTRO_BIN" --udid "$serial" test -e APP_ID="$pkg" "$flow"
    echo "PASS: $pkg"
done

"$ADB_BIN" -s "$serial" shell am start -W -n "com.vaiinilla.app.dev/com.vaiinilla.app.MainActivity" >/dev/null
echo "PASS: all flows, alumno foreground"
