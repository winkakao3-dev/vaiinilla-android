#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/.."

export JAVA_HOME="${JAVA_HOME:-/Volumes/CODIGO/Android/jdk-17.0.20.1+1/Contents/Home}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/Volumes/CODIGO/Android/Gradle}"
export ANDROID_HOME="${ANDROID_HOME:-/Volumes/CODIGO/Android/LibraryAndroid/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
ADB_BIN="${ADB:-$ANDROID_HOME/platform-tools/adb}"

skip_build=false
for arg in "$@"; do
    case "$arg" in
        --skip-build) skip_build=true ;;
        *)
            echo "FAIL: unknown argument: $arg" >&2
            exit 1
            ;;
    esac
done

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

if [ "$skip_build" = false ]; then
    ./gradlew --no-daemon assembleDevAlumnoDebug assembleDevCajaDebug assembleDevCocinaDebug
fi

apk_paths=(
    "app/build/outputs/apk/devAlumno/debug/app-dev-alumno-debug.apk"
    "app/build/outputs/apk/devCaja/debug/app-dev-caja-debug.apk"
    "app/build/outputs/apk/devCocina/debug/app-dev-cocina-debug.apk"
)
pkg_ids=(
    "com.vaiinilla.app.dev"
    "com.vaiinilla.app.dev.caja"
    "com.vaiinilla.app.dev.cocina"
)

for apk in "${apk_paths[@]}"; do
    if [ ! -f "$apk" ]; then
        echo "FAIL: missing APK: $apk" >&2
        exit 1
    fi
done

evidence="/Volumes/CODIGO/Android/QA/Vaiinilla/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$evidence"

pkg_field() {
    "$ADB_BIN" -s "$serial" shell dumpsys package "$1" 2>/dev/null \
        | grep "$2" \
        | head -1 \
        | cut -d= -f2- \
        | tr -d ' ' \
        || true
}

for i in "${!apk_paths[@]}"; do
    apk="${apk_paths[$i]}"
    pkg="${pkg_ids[$i]}"

    "$ADB_BIN" -s "$serial" shell dumpsys package "$pkg" \
        | grep -E 'firstInstallTime|lastUpdateTime|versionCode|versionName' \
        > "$evidence/pkg-before-$pkg.txt" || true
    before_first="$(pkg_field "$pkg" firstInstallTime)"

    install_out="$("$ADB_BIN" -s "$serial" install -r "$apk" 2>&1)"
    printf '%s\n' "$install_out" > "$evidence/install-$pkg.txt"
    if ! printf '%s\n' "$install_out" | grep -qx 'Success'; then
        echo "FAIL: install failed for $pkg" >&2
        exit 1
    fi

    "$ADB_BIN" -s "$serial" shell dumpsys package "$pkg" \
        | grep -E 'firstInstallTime|lastUpdateTime|versionCode|versionName' \
        > "$evidence/pkg-after-$pkg.txt"
    after_first="$(pkg_field "$pkg" firstInstallTime)"
    if [ -n "$before_first" ] && [ "$before_first" != "$after_first" ]; then
        echo "FAIL: $pkg firstInstallTime changed ($before_first -> $after_first)" >&2
        exit 1
    fi

    "$ADB_BIN" -s "$serial" logcat -c
    "$ADB_BIN" -s "$serial" shell am force-stop "$pkg"
    start_out="$("$ADB_BIN" -s "$serial" shell am start -W -n "$pkg/com.vaiinilla.app.MainActivity" 2>&1)"
    printf '%s\n' "$start_out" > "$evidence/start-$pkg.txt"
    sleep 8

    "$ADB_BIN" -s "$serial" shell dumpsys activity top > "$evidence/top-$pkg.txt"
    resumed="$("$ADB_BIN" -s "$serial" shell dumpsys activity activities | grep 'topResumedActivity' | head -1)"
    pid="$("$ADB_BIN" -s "$serial" shell pidof "$pkg" | tr -d '[:space:]')"
    "$ADB_BIN" -s "$serial" exec-out screencap -p > "$evidence/screen-$pkg.png"
    "$ADB_BIN" -s "$serial" logcat -d > "$evidence/logcat-$pkg.txt"
    "$ADB_BIN" -s "$serial" shell dumpsys package "$pkg" \
        | awk '/runtime permissions/{f=1} f{print} /^$/{if(f)exit}' \
        > "$evidence/perms-$pkg.txt"

    if ! printf '%s\n' "$start_out" | grep -q 'Status: ok'; then
        echo "FAIL: $pkg launch status not ok" >&2
        exit 1
    fi
    if ! printf '%s\n' "$resumed" | grep -q "$pkg"; then
        echo "FAIL: $pkg not resumed after launch" >&2
        exit 1
    fi
    if [ -z "$pid" ]; then
        echo "FAIL: $pkg process absent after launch" >&2
        exit 1
    fi
    if grep -A 6 'FATAL EXCEPTION' "$evidence/logcat-$pkg.txt" | grep -qF "Process: $pkg,"; then
        echo "FAIL: FATAL EXCEPTION in $pkg" >&2
        exit 1
    fi
    if grep -qF "ANR in $pkg" "$evidence/logcat-$pkg.txt"; then
        echo "FAIL: ANR in $pkg" >&2
        exit 1
    fi
    if grep 'Unable to start activity' "$evidence/logcat-$pkg.txt" | grep -qF "$pkg"; then
        echo "FAIL: unable to start activity in $pkg" >&2
        exit 1
    fi
    if grep 'SecurityException' "$evidence/logcat-$pkg.txt" | grep -qF "$pkg"; then
        echo "FAIL: SecurityException tied to $pkg" >&2
        exit 1
    fi
    echo "OK: $pkg launched, pid $pid"
done

"$ADB_BIN" -s "$serial" shell am start -W -n "com.vaiinilla.app.dev/com.vaiinilla.app.MainActivity" \
    > "$evidence/start-final-alumno.txt" 2>&1

echo "PASS: evidence at $evidence"
