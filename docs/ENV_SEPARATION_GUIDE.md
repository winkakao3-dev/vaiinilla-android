# Guía de Compilación y Distribución de Ambientes (Android)

Este documento describe la arquitectura y los comandos para compilar y distribuir los dos ambientes aislados de Vaiinilla en Android.

---

## 1. Ambientes Configurados

| Parámetro | **Vaiinilla Dev** (`dev`) | **Vaiinilla** (`prod`) |
| :--- | :--- | :--- |
| **Package Name / Application ID** | `com.vaiinilla.app.dev` | `com.vaiinilla.app` |
| **Nombre visible (App Label)** | `Vaiinilla Dev` | `Vaiinilla` |
| **Backend API (Railway)** | `https://vaiinillaback-development.up.railway.app/api/v1/` | `https://vaiinillaback.up.railway.app/api/v1/` |
| **Firebase Project** | `vaiinilla-b3a70` | `vaiinilla-produc` |
| **Firebase Config** | `app/src/dev/google-services.json` | `app/src/prod/google-services.json` |
| **Web URL** | `https://vaiinilla-web-git-develop-saul1217s-projects.vercel.app` | `https://app.vaiinilla.app` |
| **Eliminación de cuenta web** | `https://app.vaiinilla.app/eliminar-cuenta` | `https://app.vaiinilla.app/eliminar-cuenta` |
| **Instalación simultánea** | Sí (IDs y firmas independientes) | Sí (IDs y firmas independientes) |
| **Seed / Pre-config Passwords** | Habilitado en `devDebug` (opcional con `-P`) | Bloqueado (`SEED_AUTH_ENABLED = false`, strings vacíos) |

---

## 2. Configuración Centralizada en Código

Toda la configuración se expone de forma tipada e inmutable mediante la clase `AppEnvironment`:

```kotlin
data class AppEnvironment(
    val environmentName: String,
    val apiBaseUrl: String,
    val webUrl: String,
    val firebaseProjectId: String,
    val isProduction: Boolean,
    val versionName: String,
    val versionCode: Int,
)
```

Inyectable a través de Hilt en cualquier capa o repositorio del proyecto sin acoplamientos a `BuildConfig`.

---

## 3. Comandos de Compilación

### Development (`dev`)

#### Compilar APK de desarrollo:
```bash
./gradlew assembleDevDebug
```
*Salida:* `app/build/outputs/apk/dev/debug/app-dev-debug.apk`

#### Ejecutar pruebas unitarias de desarrollo:
```bash
./gradlew testDevDebugUnitTest
```

---

### Production (`prod`)

#### Compilar APK de pruebas de producción (Debug):
```bash
./gradlew assembleProdDebug
```
*Salida:* `app/build/outputs/apk/prod/debug/app-prod-debug.apk`

#### Compilar Release para Google Play (AAB / APK firmado):
```bash
./gradlew bundleProdRelease -PvaiinillaApiBaseUrl=https://vaiinillaback.up.railway.app/api/v1/
```
o generando APK optimizado:
```bash
./gradlew assembleProdRelease -PvaiinillaApiBaseUrl=https://vaiinillaback.up.railway.app/api/v1/
```
*Salidas:*
- App Bundle: `app/build/outputs/bundle/prodRelease/app-prod-release.aab`
- APK: `app/build/outputs/apk/prod/release/app-prod-release.apk`

> **Regla de Protección de Release:** Gradle validará que la URL de release coincida estrictamente con `https://vaiinillaback.up.railway.app/api/v1/`. Cualquier intento de compilar release apuntando al backend de desarrollo será rechazado automáticamente durante la fase de configuración.

#### Ejecutar pruebas unitarias de producción:
```bash
./gradlew testProdDebugUnitTest
```

---

## 4. Verificación de Contrato y Regresión

Para ejecutar la compuerta de validación de contrato backend con la suite focalizada:
```bash
VAIINILLA_API_BASE_URL=https://vaiinillaback.up.railway.app/api/v1/ ./scripts/check_app_backend_contract.sh
```

---

## 5. Integración Continua (CI/CD)

Los workflows de GitHub Actions en `.github/workflows/` están actualizados para ejecutar las tareas específicas de cada variante:
- `.github/workflows/android-ci.yml`: Valida contratos, ejecuta `testDevDebugUnitTest` y `testProdDebugUnitTest`, ejecuta lint y compila ambos APKs (`app-dev-debug.apk` y `app-prod-debug.apk`).
- `.github/workflows/android-release.yml`: Ejecuta `testProdDebugUnitTest`, `lintProdRelease` y genera el AAB de producción `app-prod-release.aab`.
