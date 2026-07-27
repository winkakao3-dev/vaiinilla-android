# Vaiinilla Android — VAI-11

App Android nativa del flujo alumno **VAI-10** (catálogo → carrito efectivo → confirmación) más **VAI-11** (seguimiento + Caja/Cocina/Mesero) con cliente remoto Railway.

Fuentes de verdad:

- `docs/source-of-truth/BOVEDA_README.md`
- `docs/source-of-truth/BOVEDA_CONTEXT.md`
- `docs/source-of-truth/BOVEDA_CONTRACTS.md` v1.0
- `docs/source-of-truth/VAIINILLA_TASK_HANDOFF.md`
- `docs/VAI-11_DELIVERY_REPORT.md`

## Flujo implementado

1. El alumno abre el catálogo y ve disponibilidad operativa.
2. Puede buscar y filtrar productos por categoría.
3. Abre un producto en un sheet visual comparable al mockup.
4. Selecciona opciones respetando `min_selecciones` y `max_selecciones`, y una cantidad entre 1 y 20.
5. Agrega configuraciones al carrito; una línea idéntica se consolida sin superar 20 unidades.
6. Revisa el carrito para `para_llevar`, añade notas y usa exclusivamente `efectivo`.
7. La app envía un request contractual sin precios, total, folio, tenant, usuario ni estado.
8. En MOCK el fixture valida y devuelve `OrderDetail` en `por_cobrar`; en REMOTE lo hace Railway.
9. La confirmación muestra folio, total confirmado y siguiente paso en Caja.
10. Caja cobra, Cocina prepara/lista, entrega y el alumno ve el seguimiento por polling.

## Límites respetados

No se implementan:

- destino `en_espacio`;
- tarjeta, saldo, wallet, recargas o cashback funcional;
- stickers, receipts coleccionables o reimpresión;
- cancelaciones, reembolsos, administración o analíticas;
- Firebase Auth dentro de la app (JWT por rol vía `local.properties`).

El campo contractual `cashback_otorgado` se conserva en `OrderSummary`, pero no hay lógica de cashback.

## Arquitectura

```text
app/src/main/java/com/vaiinilla/app/
├── core/       # ambiente, cliente HTTP vacío y seguridad
├── data/       # DTO, JSON contractual, fixtures, repositorios y Hilt
├── domain/     # modelos, dinero BigDecimal, reglas, repositorios y casos de uso
└── ui/         # estado compartido, navegación, componentes y pantallas Compose
```

La UI depende de `CatalogRepository` y `OrderRepository`; cambiar `MOCK` por `REMOTE` no requiere rehacer pantallas.

## Dinero

- Los modelos mantienen importes como `String` decimal con dos posiciones.
- Los cálculos visuales y del backend fixture usan `BigDecimal`.
- No se usa `Double` ni `Float` en el dominio monetario.
- Los totales del carrito son una previsualización; el `OrderDetail` devuelto por repositorio es la autoridad para la confirmación.

## Fixtures

```text
app/src/main/assets/fixtures/
├── catalog.json
├── operational_status.json
└── created_order.json
```

`catalog.json` incluye los grupos contractuales necesarios para demostrar configuración del burrito. `created_order.json` valida la forma de `OrderDetail` que nace en `por_cobrar`.

## Fuente de datos

Por defecto:

```bash
./gradlew assembleDebug -PvaiinillaDataSource=MOCK
```

Frontera remota (Railway development):

```bash
./gradlew assembleDebug \
  -PvaiinillaDataSource=REMOTE \
  -PvaiinillaApiBaseUrl=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

Paths remotos: `catalogo`, `estado-operativo`, `pedidos`, cobros, transiciones, `latidos`, `sesiones-caja`.  
Requiere JWT frescos por rol en `local.properties` (caducan ~15 min). Ver `local.properties.example` y `docs/VAI-11_DELIVERY_REPORT.md`.

## Validación

```bash
chmod +x gradlew scripts/*.sh scripts/*.py
python3 scripts/validate_fixtures.py
./scripts/audit_scope_vai11.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

O todo junto:

```bash
./scripts/verify-on-mac.sh
```

APK esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Consulta `docs/VAI-11_DELIVERY_REPORT.md` (y `docs/VAI-10_DELIVERY_REPORT.md` para el tramo alumno cash).
