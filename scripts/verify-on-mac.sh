#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

python3 scripts/validate_fixtures.py
./scripts/audit_release_scope.sh
./gradlew --no-daemon testDevAlumnoDebugUnitTest testProdAlumnoDebugUnitTest
./gradlew --no-daemon lintDevAlumnoDebug
./gradlew --no-daemon ktlintCheck
./gradlew --no-daemon assembleDevAlumnoDebug assembleProdAlumnoDebug

echo
echo "Validación Android terminada."
echo "APK: app/build/outputs/apk/devAlumno/debug/app-dev-alumno-debug.apk"
