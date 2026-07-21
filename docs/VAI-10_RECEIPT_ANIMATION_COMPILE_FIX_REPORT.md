# VAI-10 — Corrección de compilación de la animación del recibo

## Alcance

Corrección puntual sobre la entrega `VAI-10_RECEIPT_ANIMADA`. No se modificaron modelos, contratos, navegación, repositorios, fixtures, Hilt, selección MOCK/REMOTE ni el recorrido de VAI-10.

## Causa del error

`OrderConfirmationScreen.kt` importaba `drawWithContent` desde un paquete incorrecto:

```kotlin
import androidx.compose.foundation.drawWithContent
```

Con las dependencias Compose actuales del proyecto, el modificador pertenece a:

```kotlin
import androidx.compose.ui.draw.drawWithContent
```

Al no resolverse el modificador, el bloque lambda tampoco recibía un `DrawScope` válido y Android Studio reportaba en cascada referencias no resueltas para `clipRect`, `size` y `drawContent`.

## Cambio aplicado

Archivo modificado:

`app/src/main/java/com/vaiinilla/app/ui/screens/OrderConfirmationScreen.kt`

Cambio único de código:

- eliminado el import incorrecto de `androidx.compose.foundation.drawWithContent`;
- añadido el import correcto `androidx.compose.ui.draw.drawWithContent`.

Se conserva sin cambios funcionales:

- animación automática de impresión;
- LED pulsando;
- revelado vertical mediante `clipRect`;
- vibración mínima mediante `translationY`;
- estado final `PASE LISTO`;
- botón funcional `Volver al menú`.

## Validaciones ejecutadas

### Exitosas

```bash
python3 scripts/validate_fixtures.py
# Fixtures VAI-10 válidos.

./scripts/audit_scope.sh
# Alcance VAI-10 limpio.
```

### Gradle solicitado

Se ejecutaron exactamente:

```bash
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug
./gradlew --no-daemon assembleDebug
```

Los tres comandos terminaron con código `1` antes de iniciar las tareas porque Gradle Wrapper no pudo descargar Gradle 8.13 desde `services.gradle.org` (`UnknownHostException`). En este entorno no existe una distribución Gradle 8.13 previamente almacenada.

Por lo tanto, esta entrega **no se declara compilada ni terminada mediante Gradle en este entorno**. La corrección resuelve la causa de los cuatro errores reportados, pero `testDebugUnitTest`, `lintDebug` y `assembleDebug` deben volver a ejecutarse en un entorno con red o con Gradle 8.13 en caché.

## Logs incluidos

- `docs/validation-logs/validate_fixtures.log`
- `docs/validation-logs/validate_fixtures.exit`
- `docs/validation-logs/audit_scope.log`
- `docs/validation-logs/audit_scope.exit`
- `docs/validation-logs/testDebugUnitTest.log`
- `docs/validation-logs/testDebugUnitTest.exit`
- `docs/validation-logs/lintDebug.log`
- `docs/validation-logs/lintDebug.exit`
- `docs/validation-logs/assembleDebug.log`
- `docs/validation-logs/assembleDebug.exit`

## Validación pendiente → RESUELTA

La validación se completó exitosamente el 2026-07-21 en Mac con Android SDK API 36, JDK 21 y Gradle 8.13. Los cinco comandos pasaron con código `0`. Ver `docs/VAI-10_VALIDACION_FINAL_MAC.md`.
