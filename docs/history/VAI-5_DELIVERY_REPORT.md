# Reporte de entrega — VAI-5

## Correcciones aplicadas

- Se mantuvo una base `core/data/domain/ui` enfocada únicamente en VAI-5.
- Se reemplazó `AppContainer` por Hilt, conforme al stack de la bóveda.
- Los fixtures Kotlin duplicados fueron eliminados.
- `catalog.json` y `operational_status.json` son ahora la fuente única para app y pruebas.
- Se añadieron DTO con nombres contractuales `snake_case` y mapeo explícito a dominio.
- `Product.imageKey` se reemplazó por el campo contractual `imageUrl` / `imagen_url`.
- `-PvaiinillaDataSource=MOCK|REMOTE` selecciona implementación sin modificar UI.
- El repositorio remoto se detiene con error explícito hasta recibir OpenAPI aprobado.
- Los importes usan strings decimales; no hay cálculo monetario en UI/ViewModel.
- Se agregó validación independiente de Android SDK para fixtures y alcance.
- CI genera y publica el APK debug como artefacto cuando todas las comprobaciones pasan.
- La descarga temporal de Gradle se verifica con SHA-256.

## Funciones retiradas

- Administración, reportes, promociones e integraciones.
- Wallet, recargas, movimientos, cashback y tarjetas.
- Stickers, tickets e impresora de recibos.
- Carrito, checkout y creación local de pedidos.
- Simulación local de cobro y transiciones de estado.
- Pantallas operativas de roles.

## Validaciones ejecutables sin Android SDK

```bash
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
```

Estas validan forma contractual, dinero decimal, IDs, relaciones, timestamps y ausencia de módulos adelantados.

## Validación pendiente en Mac o GitHub Actions

```bash
./scripts/install-standard-wrapper.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

No marcar VAI-5 como **Listo** hasta obtener build y CI verdes, APK generado y PR aprobado.
