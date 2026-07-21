# VAI-5 — Validación final

## 1. Fecha y hora

2026-07-20 ~17:40 UTC-5 (CDT)

## 2. Versión de Java

```
openjdk version "17.0.19" 2026-04-21
OpenJDK Runtime Environment Temurin-17.0.19+10 (build 17.0.19+10)
OpenJDK 64-Bit Server VM Temurin-17.0.19+10 (mixed mode, sharing)
```

## 3. Versión de Gradle

```
Gradle 8.13
Revision: 073314332697ba45c16c0a0ce1891fa6794179ff
Kotlin: 2.0.21
Launcher JVM: 17.0.19 (Eclipse Adoptium 17.0.19+10)
```

## 4. Android SDK detectado

```
sdk.dir=/Users/winkakao/Library/Android/sdk
platforms: android-35, android-36
build-tools: 35.0.0
```

## 5. Resultado de fixtures

```
$ python3 scripts/validate_fixtures.py
Fixtures VAI-5 válidos.
Exit code: 0
```

## 6. Resultado de auditoría de alcance

```
$ ./scripts/audit_scope.sh
Alcance VAI-5 limpio.
Exit code: 0
```

## 7. Resultado de tests

```
$ ./gradlew testDebugUnitTest --stacktrace
BUILD SUCCESSFUL in 54s
33 actionable tasks: 33 executed
Exit code: 0
```

Tests ejecutados:
- `ContractRulesTest` — 3 tests (catálogo, estado operativo, dinero decimal)
- `RepositorySelectionTest` — 3 tests (DataSourceMode, fixture repo, remote repo)
- `UseCaseTest` — 2 tests (GetCatalogUseCase, GetOperationalStatusUseCase)

## 8. Resultado de lint

```
$ ./gradlew lintDebug --stacktrace
BUILD SUCCESSFUL in 26s
33 actionable tasks: 10 executed, 23 up-to-date
Exit code: 0
```

## 9. Resultado de assembleDebug

```
$ ./gradlew assembleDebug --stacktrace
BUILD SUCCESSFUL in 28s
43 actionable tasks: 19 executed, 24 up-to-date
Exit code: 0
```

## 10. APK

- **Ruta**: `app/build/outputs/apk/debug/app-debug.apk`
- **Tamaño**: 12 MB
- **SHA-256**: `1f82b011dcd836405fa1e2aeaa4b789841c86ac873a897678183d603570dc37d`
- **Integridad ZIP**: Verificada con `unzip -t` — sin errores.

## 11. Archivos modificados respecto al ZIP original

| Archivo | Cambio |
|---------|--------|
| `README.md` | Sección de limitaciones añadida; checkbox de CI marcado |
| `gradle/wrapper/gradle-wrapper.jar` | Generado por `install-standard-wrapper.sh` |
| `gradle/wrapper/gradle-wrapper.properties` | Actualizado por el wrapper task |
| `gradlew` | Reemplazado por el wrapper estándar de Gradle |
| `gradlew.bat` | Reemplazado por el wrapper estándar de Gradle |
| `VAI-5_VALIDACION_FINAL.md` | Creado (este reporte) |

No se modificó código fuente Kotlin, fixtures JSON, scripts de validación ni configuración de CI.

## 12. Confirmación de ausencia de módulos futuros

No se añadieron módulos de: carrito, checkout, creación de pedidos, seguimiento, Caja, Cocina, Mesero, wallet, saldo, tarjetas, cashback, administración, reportes, promociones, stickers, tickets ni impresora de recibos.

## 13. Limitaciones

- No hubo emulador ni dispositivo físico disponible.
- No se ejecutaron pruebas instrumentadas (`connectedAndroidTest`).
- No se validó visualmente la navegación ni las pantallas.
- Solo se garantiza compilación, pruebas unitarias, lint, auditorías estáticas y generación del APK.

## 14. Errores pendientes

Ninguno. Todos los comandos de validación terminaron con código de salida 0.

Nota: Existe un warning del compilador Kotlin sobre `@ApplicationContext` annotation targeting (`KT-73255`). No afecta la compilación ni el comportamiento.

## 15. Comandos para reproducir

```bash
# 1. Preparar entorno
export JAVA_HOME="/tmp/jdk17/Contents/Home"  # JDK 17 Temurin
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Descomprimir el ZIP original
mkdir -p ~/Desktop/vaiinilla-vai5-work
cd ~/Desktop/vaiinilla-vai5-work
unzip -q ~/Downloads/Vaiinilla_Android_VAI-5_CORREGIDA.zip
cd Vaiinilla-Android-VAI-5

# 3. Configurar SDK
printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties

# 4. Permisos y wrapper
chmod +x gradlew scripts/*.sh scripts/*.py
./scripts/install-standard-wrapper.sh

# 5. Validación completa
./scripts/verify-on-mac.sh

# 6. Verificar APK
ls -lh app/build/outputs/apk/debug/app-debug.apk
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

## Git

```
$ git status --short
(limpio — sin cambios pendientes)

$ git log -1 --oneline
beb3f29 feat(android): complete VAI-5 Compose foundation and CI
```

Rama: `feature/VAI-5-kotlin-compose-base`
