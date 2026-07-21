# VAI-5 — Alcance entregado

## Incluido

- proyecto Android nativo con Kotlin y Jetpack Compose;
- navegación ejecutable entre arranque y catálogo de fixtures;
- separación `core`, `data`, `domain` y `ui`;
- inyección de dependencias con Hilt;
- selección `MOCK` / `REMOTE` por propiedad de Gradle;
- cliente remoto deliberadamente vacío hasta recibir OpenAPI aprobado;
- almacenamiento de material de sesión mediante Android Keystore;
- modelos monetarios como strings decimales de dos posiciones;
- fixtures JSON únicos y compatibles con `CONTRACTS.md` v1.0;
- parser estricto y mapeo DTO → dominio;
- pruebas unitarias, auditoría de alcance, Android lint, build y CI.

## Deliberadamente no incluido

Estas funciones pertenecen a tareas posteriores o están fuera de Entrega 01:

- carrito, configuración de producto, checkout y creación de pedido: VAI-10;
- polling, acciones operativas y seguimiento: VAI-11;
- tarjeta/Stripe, wallet, recargas y cashback;
- cancelaciones, reembolsos y modo offline;
- stickers, tickets coleccionables e impresora de recibos;
- administración, analíticas, promociones e integraciones;
- gestión administrativa del catálogo.

## Archivos retirados del prototipo previo

- `AdminScreens.kt`
- `StaffScreens.kt`
- `ReceiptStickerScreens.kt`
- `ReceiptPrinterScreen.kt`
- wallet, tarjetas, movimientos, recargas y navegación asociada
- modelos de dinero con `Double`
- lógica local de avance de pedidos
- documentación histórica de paridad que no forma parte de VAI-5
