# VAI-10 — Corrección `clipRect` de la animación del recibo

## Estado de la entrega

La corrección puntual solicitada está aplicada, pero **no se declara el build Android validado** porque el entorno no pudo descargar Gradle 8.13. Los comandos Gradle fueron ejecutados y terminaron antes de iniciar sus tareas por `UnknownHostException: services.gradle.org`.

## Base utilizada

- `Vaiinilla_Android_VAI-10_RECEIPT_ANIMADA_COMPILE_FIX.zip`
- No se sustituyó la arquitectura ni se incorporaron modelos o navegación del proyecto demo anterior.

## Archivo funcional modificado

`app/src/main/java/com/vaiinilla/app/ui/screens/OrderConfirmationScreen.kt`

## Corrección aplicada

El bloque `Modifier.drawWithContent` ya tenía el import correcto. El error restante ocurría porque `clipRect` es una extensión de `DrawScope` y no estaba importada.

Se añadió:

```kotlin
import androidx.compose.ui.graphics.drawscope.clipRect
```

El revelado vertical continúa usando:

```kotlin
.drawWithContent {
    val current = progress().coerceIn(0f, 1f)
    clipRect(
        left = 0f,
        top = 0f,
        right = size.width,
        bottom = size.height * current,
    ) {
        drawContent()
    }
}
```

No se cambiaron:

- modelos o contratos;
- navegación;
- Hilt;
- repositorios MOCK/REMOTE;
- fixtures;
- flujo menú → producto → carrito → efectivo → confirmación;
- estado `efectivo`, destino `para_llevar` y estado `por_cobrar`;
- datos provenientes de `OrderDetail`.

Se conservaron la animación de impresión, LED pulsando, revelado vertical, vibración mínima, estado final `PASE LISTO` y botón `Volver al menú`.

## Entorno

```text
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10+7-Debian-1deb13u1)
OpenJDK 64-Bit Server VM (build 21.0.10+7-Debian-1deb13u1, mixed mode, sharing)
```

## Validaciones ejecutadas

### Fixtures

```bash
python3 scripts/validate_fixtures.py
```

Resultado real:

```text
Fixtures VAI-10 válidos.
```

Código de salida: `0`.

### Auditoría de alcance

```bash
./scripts/audit_scope.sh
```

Resultado real:

```text
Alcance VAI-10 limpio.
```

Código de salida: `0`.

### Unit tests

```bash
./gradlew --no-daemon testDebugUnitTest
```

Código de salida: `1`.

No se inició la tarea. Gradle Wrapper intentó descargar `gradle-8.13-bin.zip` y falló con:

```text
java.net.UnknownHostException: services.gradle.org
```

### Lint

```bash
./gradlew --no-daemon lintDebug
```

Código de salida: `1` por la misma imposibilidad de descargar Gradle 8.13. La tarea no comenzó.

### Assemble debug

```bash
./gradlew --no-daemon assembleDebug
```

Código de salida: `1` por la misma imposibilidad de descargar Gradle 8.13. La tarea no comenzó y no se generó APK.

## Logs incluidos

En `docs/validation-logs/`:

- `validate_fixtures.log`
- `validate_fixtures.exit`
- `audit_scope.log`
- `audit_scope.exit`
- `testDebugUnitTest.log`
- `testDebugUnitTest.exit`
- `lintDebug.log`
- `lintDebug.exit`
- `assembleDebug.log`
- `assembleDebug.exit`

## Validación pendiente → RESUELTA

La validación se completó exitosamente el 2026-07-21 en Mac con Android SDK API 36, JDK 21 y Gradle 8.13. Los cinco comandos pasaron con código `0`. Ver `docs/VAI-10_VALIDACION_FINAL_MAC.md`.
