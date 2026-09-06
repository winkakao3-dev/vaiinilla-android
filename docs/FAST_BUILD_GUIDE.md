# Guía de Compilación Rápida (5 Segundos) - Vaiinilla Android

Esta guía documenta cómo generar APKs y compilar el proyecto en segundos aprovechando la arquitectura del servidor VPS (4 vCPUs, 8 GB RAM).

---

## 🚫 Regla Crítica: NUNCA USAR `--no-daemon`

> **Por qué tardaba 15 minutos**:
> Al pasar `--no-daemon`, Gradle mata la JVM tras cada comando, descartando:
> 1. Toda la caché en memoria y optimizaciones JIT de Java.
> 2. El daemon persistente del compilador incremental de Kotlin y KSP.
> 3. Las clases y plugins precargados.

Al **no** incluir `--no-daemon`, el daemon se mantiene caliente y las pasadas incrementales toman **entre 4 y 6 segundos**.

---

## ⚡ Comandos Rápidos de Compilación

### 1. Compilar APK de Desarrollo (`devDebug`)
```bash
# En el VPS (4 a 6 segundos):
cd /srv/chatgpt-workspace/vaiinilla-android
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDevDebug
```

### 2. Descargar el APK a tu máquina local:
```bash
# Desde tu Mac:
scp chatgpt-vps:/srv/chatgpt-workspace/vaiinilla-android/app/build/outputs/apk/dev/debug/app-dev-debug.apk ~/Downloads/vaiinilla-debug.apk
```

### 3. Compilar APK de Producción (`prodDebug`):
```bash
cd /srv/chatgpt-workspace/vaiinilla-android
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleProdDebug

# Descargar desde tu Mac:
scp chatgpt-vps:/srv/chatgpt-workspace/vaiinilla-android/app/build/outputs/apk/prod/debug/app-prod-debug.apk ~/Downloads/vaiinilla-prod-debug.apk
```

### 4. Pruebas Unitarias Rápidas:
```bash
./gradlew testDevDebugUnitTest
```

### 5. Formato de Código:
```bash
./gradlew ktlintFormat
./gradlew ktlintCheck
```

---

## 🛠 Configuración de Memoria en `gradle.properties`
- JVM Heap: `-Xmx4096m -XX:+UseG1GC -XX:+ParallelRefProcEnabled`
- Kotlin Daemon: `kotlin.daemon.jvmoptions=-Xmx2048m -XX:+UseG1GC`
- Caché & Paralelismo: `org.gradle.daemon=true`, `org.gradle.parallel=true`, `org.gradle.caching=true`, `kotlin.incremental=true`, `ksp.incremental=true`.
