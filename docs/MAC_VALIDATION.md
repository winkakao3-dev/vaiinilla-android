# Validación final en Mac — VAI-5

Ejecuta desde Terminal dentro de la carpeta del proyecto.

## 1. Confirmar Java 17

```bash
java -version
```

Si no muestra Java 17 y tienes Android Studio instalado:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Alternativa cuando ya tienes un JDK 17 instalado:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. Dar permisos

```bash
chmod +x gradlew scripts/*.sh scripts/*.py
```

## 3. Generar el Gradle Wrapper estándar

Requiere internet solo durante la primera descarga:

```bash
./scripts/install-standard-wrapper.sh
```

Confirma que se creó:

```bash
ls -lh gradle/wrapper/gradle-wrapper.jar
./gradlew --version
```

## 4. Configurar Android SDK

Si abriste el proyecto al menos una vez en Android Studio, normalmente se crea `local.properties`. Si no existe, crea uno con la ruta habitual:

```bash
printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties
```

Comprueba que exista API 36:

```bash
ls "$HOME/Library/Android/sdk/platforms/android-36"
```

Si no existe, abre Android Studio → SDK Manager e instala Android 16 / API 36 y sus Build Tools.

## 5. Ejecutar toda la validación

```bash
./scripts/verify-on-mac.sh
```

Equivale a:

```bash
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## 6. Abrir la app

Con un teléfono conectado o emulador iniciado:

```bash
./gradlew installDebug
```

El APK queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 7. Antes del PR

```bash
git status
git add .
git commit -m "VAI-5: base Android Kotlin Compose y CI"
git push -u origin feature/VAI-5-kotlin-compose-base
```

No cambies la tarea de Notion a **Listo** hasta que GitHub Actions quede verde y el PR sea aprobado.
