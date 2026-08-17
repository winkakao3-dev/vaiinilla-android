#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

python3 scripts/validate_fixtures.py
./scripts/audit_release_scope.sh
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug
./gradlew --no-daemon ktlintCheck
./gradlew --no-daemon assembleDebug

echo
echo "Validación Android terminada."
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
