# Vaiinilla Android — agent notes

## Entorno

- JDK: `/Volumes/CODIGO/Android/jdk-17.0.20.1+1/Contents/Home` (`JAVA_HOME`)
- Gradle home: `/Volumes/CODIGO/Android/Gradle` (`GRADLE_USER_HOME`)
- adb: `/Volumes/CODIGO/Android/LibraryAndroid/sdk/platform-tools/adb`

## Comandos

```bash
./gradlew :app:compileDevAlumnoDebugKotlin   # compilar flavor alumno dev
./gradlew :app:compileDevCajaDebugKotlin     # flavor caja dev
./gradlew :app:assembleDevCajaDebug          # APK caja dev
./gradlew :app:testDevCajaDebugUnitTest      # tests unitarios
./gradlew :app:ktlintCheck                   # lint
```

## Performance en esta Mac (8 GB RAM — crítico)

`gradle.properties` pide `-Xmx3072m` para el daemon + `-Xmx2048m` para el Kotlin
daemon: en esta Mac de 8 GB eso provoca memory pressure y los compiles se van a
20+ min. Pasar heaps menores por línea de comando hace el build ~40x más rápido
(32 s por flavor):

```bash
./gradlew <tasks> \
  -Dorg.gradle.jvmargs="-Xmx2048m -XX:+UseG1GC" \
  -Dkotlin.daemon.jvmargs="-Xmx1408m" \
  --console=plain
```

Notas:

- No pipear Gradle a `tail` en builds largos: oculta el progreso hasta el final.
- Cache remoto Gradle disponible vía env `GRADLE_REMOTE_CACHE_URL/USER/PASSWORD`
  (mismos valores que los secrets del repo en CI; solo lectura si se omiten).
- Existe Firebase App Distribution automático en `android-ci.yml`: push a `main`
  compila los 3 dev APKs y los distribuye a `APP_DISTRIBUTION_TESTERS`.

Flavors: `devAlumno` (`com.vaiinilla.app.dev`), `devCaja` (`com.vaiinilla.app.dev.caja`), `devCocina` (`com.vaiinilla.app.dev.cocina`), y sus pares `prod*`.

## Limpieza post-sesión (importante)

Los daemons de Gradle/Kotlin quedan vivos ~3h y consumen CPU alta en segundo plano. Al terminar de compilar/testear:

```bash
./gradlew --stop            # mata GradleDaemon + KotlinCompileDaemon
adb kill-server             # si ya no se usa el dispositivo
```

No dejar watchers (`adb wait-for-device`, loops de logcat, servidores http) corriendo en background al terminar.

## Dispositivo de pruebas

Samsung SM-S926B con root (Magisk): `su -c` para `kill -9`, leer `/data/data/<pkg>/`, `tc netem` para latencia, `settings put` para font_scale/animators. Restaurar siempre: `tc qdisc del dev wlan0 root`, `font_scale 1.0`, `animator_duration_scale 1.0`.
