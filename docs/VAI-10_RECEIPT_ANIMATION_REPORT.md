# VAI-10 — Integración de animación Receipt Printer

## Alcance

Se usó `VAI-10_RECEIPT_CORREGIDA` como única base funcional. El proyecto `PARIDAD_DEMO_RECEIPT_PRINTER` se consultó solamente para recuperar el patrón de animación visual de impresión.

No se modificaron modelos, contratos, DTO, repositorios, endpoints, fixtures, casos de uso, navegación, Hilt ni selección MOCK/REMOTE.

## Archivo modificado

- `app/src/main/java/com/vaiinilla/app/ui/screens/OrderConfirmationScreen.kt`

## Animación integrada

La pantalla ejecuta una sola secuencia automática al recibir un `OrderDetail` válido:

1. El estado visual inicia en `IMPRIMIENDO PASE…`.
2. El LED amarillo pulsa mientras la impresión está activa.
3. El pase oscuro se revela verticalmente desde la ranura mediante clipping progresivo.
4. Durante la salida se aplica una vibración horizontal/vertical mínima tomada del patrón del demo.
5. Al completar 100 %, el estado cambia a `PASE LISTO`.
6. El botón `Volver al menú` aparece con una transición breve y conserva su callback original.

La duración de impresión es de 2.65 segundos, precedida por una pausa de 180 ms. No existe control de reimpresión ni un ciclo manual.

## Datos reales conservados

El contenido final del pase continúa usando exclusivamente `OrderDetail`:

- `order.summary.folio`
- `order.summary.id`
- `order.summary.operationalDate`
- `order.summary.total`
- `order.items`
- `order.kitchenNotes`

Los valores contractuales permanecen:

- Pago: `Efectivo`
- Destino: `Para llevar`
- Estado: `Por cobrar`

No se introdujeron `DemoOrder`, campos nuevos ni cálculos monetarios con `Double` o `Float`.

## Exclusiones respetadas

No se añadieron:

- stickers coleccionables funcionales;
- reimpresión;
- colección;
- seguimiento o transiciones VAI-11;
- cobro de Caja;
- módulos operativos;
- navegación antigua;
- endpoints nuevos.

## Validaciones ejecutadas

### Exitosas

```bash
python3 scripts/validate_fixtures.py
# Fixtures VAI-10 válidos.

./scripts/audit_scope.sh
# Alcance VAI-10 limpio.
```

### Intentadas

```bash
./gradlew --no-daemon testDebugUnitTest --stacktrace
./gradlew --no-daemon lintDebug --stacktrace
./gradlew --no-daemon assembleDebug --stacktrace
```

Los tres comandos finalizaron con código `1` antes de ejecutar las tareas porque Gradle Wrapper no pudo resolver `services.gradle.org` (`UnknownHostException`). Por tanto, no se afirma que tests, lint o build hayan pasado.

Logs incluidos:

- `docs/validation-logs/testDebugUnitTest.log`
- `docs/validation-logs/lintDebug.log`
- `docs/validation-logs/assembleDebug.log`
- archivos `.exit` correspondientes
- `docs/validation-logs/validate_fixtures.log`
- `docs/validation-logs/audit_scope.log`

## Validación pendiente

En una Mac con Android SDK y acceso de red:

```bash
printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties
chmod +x gradlew scripts/*.sh scripts/*.py
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
./gradlew --no-daemon testDebugUnitTest --stacktrace
./gradlew --no-daemon lintDebug --stacktrace
./gradlew --no-daemon assembleDebug --stacktrace
```

APK esperada:

`app/build/outputs/apk/debug/app-debug.apk`
