# Vaiinilla — Handoff técnico para VAI-10

## 1. Identificación

- ID: VAI-10
- Título de Notion: `[AND] Construir catálogo y pedido en efectivo`
- Responsable: David
- Repositorio: `winkakao3-dev/vaiinilla-android`
- URL del repositorio: `https://github.com/winkakao3-dev/vaiinilla-android`
- Tarea de Notion: `https://app.notion.com/p/3a3c4e951cdb8128a627cbe7c8ed78fa`
- Área/repositorio: Kotlin / Android
- Entrega: Entrega 01 — Efectivo E2E
- Prioridad: P0
- Estado registrado en Notion al preparar este handoff: En curso
- Fecha objetivo registrada: 2026-07-22

## 2. Texto completo de la tarea de Notion

### Qué vas a construir

Construir en Android el flujo donde el alumno ve el menú, configura un producto, revisa el carrito, elige efectivo y crea su pedido.

### Ventanas que debes revisar

- 02 — Menú principal: `http://74.208.167.38/v7/#screen=02`
- 07 — Modal de producto: `http://74.208.167.38/v7/#screen=07`
- 08 — Producto personalizado: `http://74.208.167.38/v7/#screen=08`
- 13 — Carrito para llevar y efectivo: `http://74.208.167.38/v7/#screen=13`
- 16 — Pedido confirmado en efectivo: `http://74.208.167.38/v7/#screen=16`

### Para comenzar

Puedes iniciar con datos simulados. Abre las cinco ventanas y entrega a tu IA esta tarea, sus capturas si no puede navegar la demo y el paquete `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/README.md`, `CONTEXT.md` y `CONTRACTS.md`. Pídele primero un mapa `ventana → pantalla Android → acción`.

### Qué debes mostrar

Un video o recorrido en Android: menú → producto → carrito → efectivo → confirmación.

### Cuándo marcar Listo

- El recorrido funciona y se parece al mockup.
- Puede cambiar de datos simulados al Backend sin rehacer las pantallas.
- Las revisiones automáticas están en verde.
- El PR contiene capturas o video y está enlazado en Notion.

El mockup manda visualmente. La bóveda y `CONTRACTS.md` mandan en textos funcionales, precios, estados y reglas.

## 3. Dependencias y estado de base

- Dependencia explícita de Notion: VAI-3 Listo; puede iniciar con fixtures.
- VAI-5 se considera técnicamente terminada por el usuario, aunque Jesús aún no ha autorizado marcarla `Listo` en Notion.
- Rama de trabajo creada: `feature/VAI-10-catalogo-carrito-efectivo`.
- La rama parte de la base Android implementada en VAI-5 porque `origin/main` todavía no contiene esos commits.
- Estado local al crear la rama: limpio.
- Último commit heredado de VAI-5: `2cf228b ci(android): add VAI-5 validation workflow`.
- No trabajar directamente en `main`.

## 4. Archivos de la Bóveda revisados

Se leyeron completos, en este orden:

1. `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/README.md`
2. `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/CONTEXT.md`
3. `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/CONTRACTS.md`

Estado del paquete: aprobado. Contrato: versión 1.0, aprobado por Jesús Leos el 2026-07-20.

## 5. Alcance incluido de VAI-10

- Catálogo del alumno con estado operativo y productos disponibles.
- Categorías, productos, grupos de opciones y opciones.
- Detalle/configuración de producto.
- Cantidad y selección de opciones respetando mínimos y máximos del contrato.
- Carrito con destino `para_llevar`.
- Método de pago `efectivo`.
- Resumen visual previo sin convertir el cliente en autoridad de precios.
- Creación de pedido con fixtures y abstracción de repositorio que pueda cambiar a Backend sin rehacer las pantallas.
- Confirmación del pedido creado.
- Navegación y UI comparables a las ventanas 02, 07, 08, 13 y 16.
- Pruebas unitarias/estáticas necesarias para el alcance.

## 6. Alcance excluido

- Seguimiento completo y pantallas operativas de VAI-11/VAI-13.
- Integración real de catálogo/pedidos con Backend si el contrato o el repositorio actual no la soportan todavía; conservar la frontera para el cambio posterior.
- Tarjeta/Stripe, wallet, cashback, descuentos por producto, cancelaciones, reembolsos, stickers, analíticas, administración de catálogo, offline de Caja y publicación en tiendas.
- Cambios a tablas, endpoints, estados, permisos, tenant, idempotencia o reglas monetarias del contrato.

## 7. Contratos que deben respetarse

### Invariantes

- Base de API: `/api/v1`.
- JSON UTF-8, campos `snake_case`, timestamps ISO 8601 UTC.
- UUID como string.
- Dinero como string decimal con dos posiciones, por ejemplo `"26.00"`.
- Todas las respuestas usan envelope `{data, meta, error}`.
- El cliente no es autoridad para tenant, usuario, rol, precios, totales, folio o estado.
- Todo `POST` exige `Idempotency-Key` UUID.
- El servidor es la fuente de verdad.

### Modelos relevantes

`OperationalStatus`, `Category`, `Product`, `OptionGroup`, `OrderSummary` y `OrderDetail` deben mantenerse compatibles con los ejemplos de `CONTRACTS.md`. Los campos principales del producto incluyen `id`, `categoria_id`, `estacion_preparacion`, `nombre`, `descripcion`, `ingredientes`, `alergenos`, `tiempo_estimado_min`, `precio_mostrador`, `precio_digital`, `disponible`, `imagen_url` y `grupos_opcion`.

La solicitud de creación debe usar esta forma contractual:

```json
{
  "metodo_pago": "efectivo",
  "destino": "para_llevar",
  "espacio_id": null,
  "notas_cocina": "Sin azúcar",
  "items": [
    { "producto_id": 101, "cantidad": 1, "opcion_ids": [301] }
  ]
}
```

### Estados permitidos

El pedido nace en `por_cobrar`. El flujo posterior es `cobrado → preparando → listo → entregado`; VAI-10 solo debe llegar hasta la creación/confirmación del pedido y no implementar el seguimiento operativo de VAI-11.

### Reglas funcionales

- Sin sesión de caja abierta y disponibilidad operativa válida no se puede crear pedido.
- `para_llevar` exige `espacio_id: null`.
- El único método de pago de esta entrega es `efectivo`.
- El backend calculará precios, totales, folio, tenant, usuario y estado cuando exista integración real.
- No usar `Double` o `Float` para dinero.
- No inventar endpoints, campos, estados ni reglas.

## 8. Reglas visuales

El mockup define composición, jerarquía, navegación y estilo visual. La bóveda y `CONTRACTS.md` mandan sobre textos funcionales, precios, estados y reglas. Si la demo no es navegable, usar capturas de las cinco ventanas asignadas y dejar evidencia comparable en el reporte/PR.

Antes de escribir código, entregar un mapa:

`ventana → pantalla Android → acción → estado/dato usado`

## 9. Validación esperada

No hay emulador disponible. La validación será local mediante scripts, tests unitarios, lint, build y análisis estático. Ejecutar como mínimo:

```bash
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Si existen comandos adicionales definidos por la implementación, reportarlos. No ocultar fallos, no desactivar tests/lint y no afirmar que un comando pasó si no se ejecutó.

## 10. Reglas de entrega para ChatGPT

- Leer primero este archivo y los tres archivos de la Bóveda incluidos en el ZIP.
- Auditar el proyecto actual antes de editar.
- Implementar exclusivamente VAI-10.
- Mantener la separación `core/data/domain/ui` y Hilt existente.
- Mantener compatibilidad con fixtures y contratos.
- Añadir/actualizar pruebas para el alcance.
- Conservar las validaciones de VAI-5.
- Actualizar README y reporte de entrega.
- Devolver un ZIP limpio, reporte detallado y comandos de validación.
- No incluir `.git`, cachés, builds, `local.properties` ni secretos.
- No afirmar build/lint/tests verdes sin evidencia real.

