# Vaiinilla Android — VAI-11

App Android nativa del flujo alumno **VAI-10** (catálogo → carrito efectivo/saldo → confirmación) más **VAI-11** (seguimiento + Caja/Cocina/Mesero) con cliente remoto Railway.

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
6. Revisa el carrito para `para_llevar`, añade notas y elige `efectivo` o `saldo`.
7. La app envía un request contractual sin precios, total, folio, tenant, usuario ni estado.
8. Railway valida y devuelve `OrderDetail`: `por_cobrar` para efectivo o `cobrado` para saldo.
9. La confirmación muestra folio, total confirmado y el siguiente paso: Caja para efectivo o Cocina para saldo.
10. Caja cobra, Cocina prepara/lista, entrega y el alumno ve el seguimiento por polling.

## Límites respetados

Fuera de esta rama/entrega quedan:

- tarjeta, Stripe o recargas digitales;
- stickers, receipts coleccionables o reimpresión;
- administración de cashback, cancelaciones y ajustes administrativos;

El campo contractual `cashback_otorgado` se conserva en `OrderSummary`, pero no hay lógica de cashback.

## Arquitectura

```text
app/src/main/java/com/vaiinilla/app/
├── core/       # ambiente, cliente HTTP vacío y seguridad
├── data/       # DTO, JSON contractual, repositorios remotos y Hilt
├── domain/     # modelos, dinero BigDecimal, reglas, repositorios y casos de uso
└── ui/         # estado compartido, navegación, componentes y pantallas Compose
```

La UI depende de `CatalogRepository`, `OrderRepository` y `WalletRepository`; la implementación de producción usa Firebase + Railway.

## Entrega 03 — Wallet por establecimiento

- El alumno consulta el saldo visible y los movimientos de su establecimiento mediante `wallets/me`.
- Caja busca clientes con el identificador contextual del establecimiento y registra recargas en efectivo con Caja abierta.
- Las recargas envían `Idempotency-Key`; los buckets internos, el consumo y la prorrata permanecen bajo control del servidor.
- Stripe, recargas digitales y cualquier UI de buckets quedan fuera de esta entrega.

## Dinero

- Los modelos mantienen importes como `String` decimal con dos posiciones.
- Los cálculos visuales y la validación contractual usan `BigDecimal`.
- No se usa `Double` ni `Float` en el dominio monetario.
- Los totales del carrito son una previsualización; el `OrderDetail` devuelto por repositorio es la autoridad para la confirmación.

## Fuente de datos

La aplicación usa una única fuente de datos en runtime: Firebase para identidad y Railway para catálogo, contexto, accesos y operación.
MOCK fue retirado del runtime y del APK; los fixtures que permanecen en tests o previews no son una fuente de datos de producción.

## Estado REMOTE (VAI-27)

- API de desarrollo: `https://vaiinillaback-development-3f6c.up.railway.app/api/v1/`
- Swagger: `https://vaiinillaback-development-3f6c.up.railway.app/api/docs/`
- Build validada: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `c84aa5d28c6c3dce068bceace1bae483a0f90b65c5d5eaaba7ac6365a258aed0`

Build reproducible:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PvaiinillaApiBaseUrl=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

La evidencia de prueba en dispositivo Android real sigue pendiente; no se reporta como ejecutada mientras `adb devices -l` no muestre un dispositivo y se registre la matriz REMOTE. Ver `docs/VAI-27_HARDENING.md`.

Paths remotos: `catalogo`, `estado-operativo`, `pedidos`, `wallets/me`, `wallets/clientes`, `wallets/{usuarioId}/recargas-efectivo`, cobros, transiciones, `latidos`, `sesiones-caja`.
El contexto operativo se obtiene después de Firebase; no se aceptan JWT manuales ni una fuente local alternativa. Ver `local.properties.example` y `docs/VAI-11_DELIVERY_REPORT.md`.

## Validación

```bash
chmod +x gradlew scripts/*.sh scripts/*.py
python3 scripts/validate_fixtures.py
./scripts/audit_scope_vai11.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew ktlintCheck
./gradlew assembleDebug
```

O todo junto:

```bash
./scripts/verify-on-mac.sh
```

La autenticación auxiliar de cuentas seed existe sólo para depuración local y siempre pasa por Firebase + Railway; nunca cambia la fuente de datos de la app. Ver `docs/FIREBASE_SEED_AUTH.md`.

APK esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Consulta `docs/VAI-11_DELIVERY_REPORT.md` (y `docs/VAI-10_DELIVERY_REPORT.md` para el tramo alumno cash).
Para el hardening de acceso REMOTE de VAI-27, el APK, SHA-256 y la matriz de pruebas, consulta `docs/VAI-27_HARDENING.md`.
