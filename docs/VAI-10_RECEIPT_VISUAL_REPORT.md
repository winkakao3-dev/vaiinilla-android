# VAI-10 — Corrección visual de confirmación / Receipt Printer

## Objetivo

Adaptar la pantalla de confirmación existente de VAI-10 al lenguaje visual de la impresora y el receipt editorial de la demo anterior, sin reemplazar la arquitectura, los modelos ni las reglas contractuales del proyecto actual.

## Auditoría realizada

### Proyecto base VAI-10

- Se mantuvo como única base funcional y arquitectónica.
- `OrderConfirmationScreen.kt` mostraba una confirmación correcta pero simplificada: encabezado informativo, tarjeta negra de pase de Caja y botón de retorno.
- `OrderDetail` ya expone todos los datos necesarios: `summary`, `items` y `kitchenNotes`.
- Se conservaron Hilt, repositorios MOCK/REMOTE, fixtures, casos de uso, navegación y pruebas existentes.

### Proyecto anterior de referencia

Se revisaron visualmente y a nivel de composición:

- `ReceiptPrinterScreen.kt`
- `ReceiptStickerScreens.kt`
- `ReceiptPrinterMachine`
- `ReceiptPaperOutput`
- `TicketBarcode`

Solo se recuperaron patrones visuales: carcasa oscura, ranura, encabezado editorial, papel oscuro, barcode decorativo, tabla de metadatos y jerarquía tipográfica. No se copiaron modelos, navegación, `DemoOrder`, animaciones de impresión, colección, reimpresión ni seguimiento.

## Archivo modificado

- `app/src/main/java/com/vaiinilla/app/ui/screens/OrderConfirmationScreen.kt`

No se modificaron modelos, DTO, repositorios, endpoints, fixtures ni reglas de dominio.

## Mapeo de OrderDetail

| Diseño | Fuente VAI-10 |
|---|---|
| Número de orden | `order.summary.folio` |
| Identificador bajo barcode | `order.summary.id` |
| Fecha operativa | `order.summary.operationalDate` |
| Total | `order.summary.total` |
| Líneas del pedido | `order.items` |
| Cantidad y producto | `item.quantity`, `item.productName` |
| Subtotal de línea | `item.subtotal` |
| Opciones | `item.options[].name` |
| Notas de cocina | `order.kitchenNotes` cuando no está vacío |
| Pago | valor contractual fijo de VAI-10: Efectivo |
| Destino | valor contractual fijo de VAI-10: Para llevar |
| Estado | valor contractual fijo de VAI-10: Por cobrar |

Los importes continúan llegando como `String` y se formatean con `moneyLabel`; no se introdujeron cálculos monetarios con `Double` o `Float`.

## Cambios visuales

- Encabezado de impresora con `VAIINILLA / RECEIPT LAB`, `PASE DE CAJA`, folio, indicador de pase listo y ranura.
- Papel oscuro editorial conectado visualmente debajo de la impresora.
- Título `ANTOJO`, escalas decorativas, sello VNNL y barcode determinista basado en el folio.
- Tabla de metadatos para fecha, pedido, total, pago, destino y estado.
- Listado real y adaptable de todos los artículos, subtotales y opciones.
- Bloque de instrucciones para cocina solo cuando existen notas.
- Total final y texto contractual de presentación en Caja.
- Único botón funcional: `Volver al menú`.
- Layout con `BoxWithConstraints`, paddings compactos y scroll vertical para distintos tamaños.

## Exclusiones respetadas

No se implementaron:

- `DemoOrder` ni modelos del proyecto viejo.
- stickers coleccionables funcionales.
- reimpresión.
- cobro de Caja.
- seguimiento o transiciones VAI-11.
- endpoints o campos nuevos.
- métodos de pago o destinos adicionales.

## Validaciones ejecutadas

### Exitosas

```bash
python3 scripts/validate_fixtures.py
# Fixtures VAI-10 válidos.

./scripts/audit_scope.sh
# Alcance VAI-10 limpio.
```

### Intentadas, pero no ejecutadas por el entorno

```bash
./gradlew --no-daemon testDebugUnitTest --stacktrace
./gradlew --no-daemon lintDebug --stacktrace
./gradlew --no-daemon assembleDebug --stacktrace
```

Los tres comandos terminaron antes de iniciar las tareas porque Gradle Wrapper no pudo resolver `services.gradle.org` (`UnknownHostException`). Por ello no se afirma que tests, lint o build Android hayan pasado.

Logs:

- `docs/validation-logs/testDebugUnitTest.log`
- `docs/validation-logs/lintDebug.log`
- `docs/validation-logs/assembleDebug.log`
- archivos `.exit` correspondientes

## Validación pendiente en una Mac con Android SDK y red

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

## Evidencia visual

No se generó captura real de la pantalla porque el entorno no pudo descargar Gradle y, por tanto, no fue posible compilar ni lanzar Compose. La implementación queda trazable en `OrderConfirmationScreen.kt`; la captura debe tomarse después de ejecutar `assembleDebug` o abrir el proyecto en Android Studio.
