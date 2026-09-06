#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
STAGING_DIR="/srv/chatgpt-workspace/apk_staging"
mkdir -p "$STAGING_DIR"

echo "==> Limpiando árbol de trabajo git..."
git checkout -- .

echo "==> Registrando variantes en google-services.json..."
python3 - << 'PY'
import json, copy
path = "app/src/dev/google-services.json"
data = json.load(open(path))
dev_client = next(c for c in data["client"] if c["client_info"]["android_client_info"]["package_name"] == "com.vaiinilla.app.dev")
existing = {c["client_info"]["android_client_info"]["package_name"] for c in data["client"]}
for pkg in ["com.vaiinilla.app.dev.caja", "com.vaiinilla.app.dev.cocina"]:
    if pkg not in existing:
        c = copy.deepcopy(dev_client)
        c["client_info"]["android_client_info"]["package_name"] = pkg
        data["client"].append(c)
json.dump(data, open(path, "w"), indent=2)
PY

echo "==> 1/3 Compilando Vaiinilla (Alumno)..."
sed -i 's/name="app_name">Vaiinilla Dev</name="app_name">Vaiinilla (Alumno)</' app/src/dev/res/values/strings.xml
./gradlew assembleDevDebug
cp app/build/outputs/apk/dev/debug/app-dev-debug.apk "$STAGING_DIR/vaiinilla-dev-alumno.apk"

echo "==> 2/3 Compilando Vaiinilla (Caja)..."
sed -i 's/applicationIdSuffix = ".dev"/applicationIdSuffix = ".dev.caja"/' app/build.gradle.kts
sed -i 's/name="app_name">Vaiinilla (Alumno)</name="app_name">Vaiinilla (Caja)</' app/src/dev/res/values/strings.xml
./gradlew assembleDevDebug
cp app/build/outputs/apk/dev/debug/app-dev-debug.apk "$STAGING_DIR/vaiinilla-dev-caja.apk"

echo "==> 3/3 Compilando Vaiinilla (Cocina)..."
sed -i 's/applicationIdSuffix = ".dev.caja"/applicationIdSuffix = ".dev.cocina"/' app/build.gradle.kts
sed -i 's/name="app_name">Vaiinilla (Caja)</name="app_name">Vaiinilla (Cocina)</' app/src/dev/res/values/strings.xml
./gradlew assembleDevDebug
cp app/build/outputs/apk/dev/debug/app-dev-debug.apk "$STAGING_DIR/vaiinilla-dev-cocina.apk"

echo "==> Restaurando repositorio a estado limpio..."
git checkout -- .

echo "==> ¡Las 3 APKs se generaron exitosamente en $STAGING_DIR!"
ls -lh "$STAGING_DIR"
