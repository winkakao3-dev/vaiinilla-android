# Reporte de entrega — VAI-10

## Alcance

Se implementó exclusivamente el recorrido:

```text
catálogo → detalle/configuración → carrito → efectivo → confirmación
```

La UI se basó en el demo entregado; las reglas, precios, estados y payloads se basaron en `CONTRACTS.md` v1.0.

## Mapa mockup → Android → acción

| Ventana | Implementación Android | Acción | Estado/dato |
|---|---|---|---|
| 02 — Menú principal | `CatalogScreen` | búsqueda, filtro, apertura de producto y carrito | `OperationalStatus`, `Category`, `Product` |
| 07 — Modal de producto | `ProductDetailSheet` | selección inicial y cantidad | `OptionGroup`, mínimos/máximos |
| 08 — Producto personalizado | mismo sheet con selección actualizada | recalcular preview con `BigDecimal` y agregar | `precio_digital`, `precio_extra` |
| 13 — Carrito para llevar y efectivo | `CartScreen` | cantidades, notas y confirmación | `para_llevar`, `espacio_id=null`, `efectivo` |
| 16 — Pedido confirmado | `OrderConfirmationScreen` | mostrar folio, total y volver al menú | `OrderDetail.estado=por_cobrar` |

La ventana 16 contiene elementos de receipt sticker y seguimiento. No se implementaron porque stickers y VAI-11 están fuera de VAI-10; se conservó la jerarquía visual de confirmación mediante un pase de Caja no coleccionable.

## Cambios principales

### Dominio

- `OrderModels.kt`: carrito, request contractual, `OrderSummary`, `OrderDetail`, items y enums limitados a VAI-10.
- `Money.kt`: parseo y operaciones con `BigDecimal` y salida decimal de dos posiciones.
- `ContractRules.kt`: selección de opciones, líneas 1–50, cantidad 1–20, efectivo y `para_llevar`.
- `OrderRepository.kt`: frontera desacoplada para fixtures/backend.
- `BuildCreateOrderRequestUseCase.kt`: construye el body exacto sin precios ni autoridad local.
- `CreateOrderUseCase.kt`: valida y delega creación con idempotencia.

### Datos

- DTO y mapper con nombres `snake_case` para `POST /api/v1/pedidos` y `OrderDetail`.
- `FixtureOrderRepository`: valida catálogo/operación, opciones e idempotencia; calcula como backend fixture y devuelve `por_cobrar`.
- `RemoteOrderRepository`: usa únicamente el endpoint aprobado `pedidos` y falla controladamente sin adaptador OpenAPI.
- `VaiinillaApiClient`: agrega `post` genérico sin inventar rutas ni respuesta.
- DI Hilt selecciona repositorios MOCK/REMOTE para catálogo y pedido.

### UI

- `OrderFlowViewModel`: una sola fuente de estado para catálogo, configuración, carrito, checkout y confirmación.
- `CatalogScreen`: layout en dos columnas, búsqueda, categorías, estado operativo y navbar flotante.
- `ProductDetailSheet`: opciones, cantidad, precio unitario configurado y total de la línea antes de agregar.
- `CartScreen`: items, cantidades, para llevar, efectivo, notas, preview y confirmación.
- `OrderConfirmationScreen`: estado `por_cobrar`, folio, total confirmado y regreso al menú.
- Tabs fuera de alcance se muestran solo como parte visual de la navbar y permanecen inertes.

### Fixtures

- Se añadieron grupos de `Guiso`, `Salsa` y `Extra` al burrito usando únicamente la estructura aprobada.
- Se añadió `created_order.json` compatible con `OrderDetail`.

## Inventario principal de archivos VAI-10

```text
app/src/main/java/com/vaiinilla/app/
├── core/network/VaiinillaApiClient.kt
├── data/di/VaiinillaModule.kt
├── data/order/
│   ├── FixtureOrderRepository.kt
│   ├── OrderContractDtos.kt
│   ├── OrderContractJson.kt
│   ├── OrderContractMapper.kt
│   └── RemoteOrderRepository.kt
├── domain/model/
│   ├── ContractRules.kt
│   ├── Money.kt
│   └── OrderModels.kt
├── domain/repository/OrderRepository.kt
├── domain/usecase/
│   ├── BuildCreateOrderRequestUseCase.kt
│   └── CreateOrderUseCase.kt
└── ui/
    ├── components/
    ├── navigation/
    ├── order/
    └── screens/

app/src/main/assets/fixtures/created_order.json
app/src/test/java/com/vaiinilla/app/OrderContractTest.kt
app/src/test/java/com/vaiinilla/app/OrderRepositorySelectionTest.kt
app/src/test/java/com/vaiinilla/app/Vai10RulesTest.kt
scripts/validate_fixtures.py
scripts/audit_scope.sh
```

## Pruebas añadidas

- `OrderContractTest`
  - parseo de `created_order.json`;
  - serialización exacta del request sin precio/total/estado/folio;
  - creación en efectivo para llevar y `por_cobrar`;
  - replay idempotente;
  - rechazo de llave reutilizada con payload distinto.
- `Vai10RulesTest`
  - mínimos y máximos de opciones;
  - rechazo de opción ajena;
  - cálculo BigDecimal;
  - límites 1–20 y 1–50;
  - `para_llevar` exige espacio nulo.
- `OrderRepositorySelectionTest`
  - el modo remoto se detiene sin inventar adaptación;
  - verifica path `pedidos`, body contractual e `Idempotency-Key`.
- Se conservaron las pruebas de VAI-5 para catálogo, estado operativo y selección de repositorio.

## Comandos ejecutados en este entorno

### Pasaron

```bash
python3 scripts/validate_fixtures.py
# Fixtures VAI-10 válidos.

./scripts/audit_scope.sh
# Alcance VAI-10 limpio.
```

También se ejecutaron comprobaciones parciales de sintaxis con el compilador Kotlin instalado:

```bash
kotlinc <modelos de dominio> -d /tmp/vai10-kotlinc/domain.jar
kotlinc <stubs mínimos + dominio + casos de uso + OrderFlowViewModel> -d /tmp/vai10-vmcheck/vm.jar
```

Ambas terminaron con código 0. Estas comprobaciones no sustituyen el build Android.

Los logs completos de los intentos Gradle se incluyen en `docs/validation-logs/`, junto con un archivo `.exit-code` por comando.

### No pudieron ejecutarse completamente

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Los tres comandos fueron intentados de forma individual y terminaron con código `1` antes de iniciar la tarea. El contenedor no tiene Android SDK configurado ni una distribución Gradle 8.13 en caché, y el wrapper no pudo descargarla por falta de resolución DNS (`UnknownHostException: services.gradle.org`). Por ello no se afirma que tests Gradle, lint o build hayan pasado.

## Validación requerida en Mac

```bash
cd <raíz-del-proyecto>
printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties
chmod +x gradlew scripts/*.sh scripts/*.py
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
./gradlew --no-daemon testDebugUnitTest --stacktrace
./gradlew --no-daemon lintDebug --stacktrace
./gradlew --no-daemon assembleDebug --stacktrace
```

Verificación opcional del APK:

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
```

## No validado

- Renderizado en emulador o dispositivo físico.
- Comparación visual pixel a pixel contra las ventanas 02, 07, 08, 13 y 16.
- Pruebas instrumentadas.
- Integración con backend real/OpenAPI.
- Resultado final de Android lint, unit tests y APK en este contenedor.

## Riesgos pendientes

- La API remota seguirá mostrando error controlado hasta recibir el adaptador generado/validado contra OpenAPI.
- La fidelidad visual debe verificarse en 390×844 y 412×892 en un dispositivo o emulador.
- Los tabs de Asistente, Pedidos y Cartera son deliberadamente inertes para no adelantar otras tareas.
