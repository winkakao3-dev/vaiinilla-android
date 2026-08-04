---
tipo: contrato-modulo
proyecto: Vaiinilla
modulo: pedido-efectivo-e2e
version: 1.0
estado: aprobado
autoridad_aprobacion: Jesus Leos
fecha: 2026-07-20
aprobado_por: Jesus Leos
aprobado_en: 2026-07-20
relacionado:
  - "[[README]]"
  - "[[CONTEXT]]"
  - "[[01-Modelo-Datos]]"
  - "[[05-Estandares-API]]"
---

# Contratos — Entrega 01

> [!important]
> Contrato aprobado por Jesús el 20 de julio de 2026. Autoriza la implementación de Entrega 01. Cualquier cambio exige PR autorizado y actualización coordinada de Backend, Kotlin y Swift.

## 1. Invariantes

1. Base obligatoria: `/api/v1`.
2. JSON UTF-8; nombres `snake_case`; timestamps ISO 8601 UTC.
3. UUID como string; cantidades monetarias como string decimal con dos posiciones, por ejemplo `"26.00"`.
4. Todas las respuestas usan el envelope de `[[05-Estandares-API]]`.
5. `establecimiento_id`, `usuario_id`, rol, precios, totales, folio y estado nunca son aceptados como autoridad desde el cliente.
6. Todo `POST` de este flujo exige `Idempotency-Key` UUID, excepto `POST /api/v1/latidos`, que es un `upsert` naturalmente repetible.
7. Las transiciones son atómicas y validan versión/estado actual.
8. El cliente nunca decide si la caja está operando ni si un rol tiene permiso.

## 2. Autenticación y contexto

Intercambio de identidad por contexto:

```http
POST /api/v1/sesiones/contexto
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{ "membresia_id": "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3" }
```

El backend verifica Firebase, resuelve `usuarios.firebase_uid`, valida que la membresía esté activa y pertenezca al usuario, y devuelve un JWT RS256 de Vaiinilla con vigencia de 15 minutos. Los endpoints de negocio reciben `Authorization: Bearer <vaiinilla-access-token>`.

Respuesta `200`:

```json
{
  "data": {
    "access_token": "<jwt>",
    "token_type": "Bearer",
    "expires_in": 900,
    "contexto": {
      "usuario_id": "032819a8-8dbd-4aef-a728-2e1be9ef09ab",
      "membresia_id": "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3",
      "establecimiento_id": "8246ff44-aad0-4e49-9268-b71c997893fe",
      "rol": "cliente"
    }
  },
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
  "error": null
}
```

Errores: `401 UNAUTHENTICATED`, `403 MEMBERSHIP_INACTIVE`, `404 MEMBERSHIP_NOT_FOUND`.

Claims mínimos:

```json
{
  "uid": "firebase-uid",
  "usuario_id": "032819a8-8dbd-4aef-a728-2e1be9ef09ab",
  "membresia_id": "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3",
  "establecimiento_id": "8246ff44-aad0-4e49-9268-b71c997893fe",
  "rol": "cliente"
}
```

Roles permitidos: `cliente`, `cajero`, `cocina`, `admin`, `mesero`.

El access token también incluye `usuario_id`, `iss`, `aud`, `iat` y `exp`. El modo de desarrollo usa tokens locales RS256 con los mismos claims y una llave separada. Debe estar protegido por una bandera explícita, registrar advertencia al iniciar y negarse a arrancar en producción.

## 3. Envelope

Recurso individual:

```json
{
  "data": {},
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
  "error": null
}
```

Error:

```json
{
  "data": null,
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
  "error": {
    "code": "ORDER_INVALID_STATE",
    "message": "El pedido no puede realizar esa transición.",
    "details": []
  }
}
```

## 4. Modelos públicos

### `OperationalStatus`

```json
{
  "recibiendo_pedidos": true,
  "sesion_caja_abierta": true,
  "caja_en_linea": true,
  "cocina_en_linea": true,
  "tiempo_estimado_min": 12,
  "consultado_en": "2026-07-20T15:00:00.000Z"
}
```

### `Category`

```json
{
  "id": 10,
  "nombre": "Bebidas",
  "orden": 1
}
```

### `Product`

```json
{
  "id": 101,
  "categoria_id": 10,
  "estacion_preparacion": "cocina",
  "nombre": "Chocolate frío",
  "descripcion": "Bebida de chocolate",
  "ingredientes": "Leche y cacao",
  "alergenos": "Lácteos",
  "tiempo_estimado_min": 5,
  "precio_mostrador": "20.00",
  "precio_digital": "26.00",
  "disponible": true,
  "imagen_url": "https://storage.example/productos/101.webp",
  "grupos_opcion": []
}
```

### `OptionGroup`

```json
{
  "id": 201,
  "nombre": "Tipo de leche",
  "min_selecciones": 1,
  "max_selecciones": 1,
  "opciones": [
    { "id": 301, "nombre": "Entera", "precio_extra": "0.00" }
  ]
}
```

### `OrderSummary`

```json
{
  "id": "9f023852-b234-4350-813d-7af67e9192ea",
  "folio": 42,
  "fecha_operativa": "2026-07-20",
  "estado": "por_cobrar",
  "metodo_pago": "efectivo",
  "destino": "para_llevar",
  "espacio": null,
  "subtotal": "26.00",
  "ahorro_combinado": "0.00",
  "cashback_otorgado": "0.00",
  "total": "26.00",
  "version": 1,
  "creado_en": "2026-07-20T15:05:00.000Z",
  "actualizado_en": "2026-07-20T15:05:00.000Z"
}
```

### `CashSession`

```json
{
  "id": "73f1759c-7524-4e21-bf93-72d989f1c70e",
  "fecha_operativa": "2026-07-20",
  "monto_inicial": "500.00",
  "abierta_en": "2026-07-20T14:55:00.000Z",
  "cerrada_en": null,
  "cierre_automatico": false
}
```

### `OrderDetail`

Extiende `OrderSummary` con:

```json
{
  "usuario": { "nombre": "Ana Pérez", "matricula": "A01234" },
  "notas_cocina": "Sin azúcar",
  "items": [
    {
      "id": 501,
      "producto_id": 101,
      "nombre_producto": "Chocolate frío",
      "estacion_preparacion": "cocina",
      "cantidad": 1,
      "precio_digital_unitario": "26.00",
      "subtotal": "26.00",
      "opciones": [
        { "opcion_id": 301, "nombre": "Entera", "precio_extra": "0.00" }
      ]
    }
  ]
}
```

Los datos de `usuario` solo se incluyen para roles operativos autorizados. Para otros clientes nunca se exponen.

## 5. Estados y transiciones

```text
por_cobrar --cajero/cobro efectivo--> cobrado
cobrado --cocina--> preparando
preparando --cocina--> listo
listo + para_llevar --cajero--> entregado
listo + en_espacio --mesero--> entregado
```

No se permite saltar pasos, retroceder ni repetir un efecto. Excepción controlada: si todos los items tienen `estacion_preparacion = caja`, el cobro registra `cobrado` y luego `listo` automáticamente dentro de la misma transacción, con dos eventos y versiones consecutivas. Cocina nunca recibe ese pedido.

En pedidos mixtos, Cocina recibe el pedido pero solo los items cuyo snapshot indique `cocina`; los items `caja` se ensamblan al entregar. Los terminales `cancelado`, `no_recogido` y `expirado` existen en el modelo global, pero sus acciones quedan fuera de esta entrega.

## 6. Endpoints

### `GET /api/v1/sesiones-caja/activa`

Roles: `cajero`, `cocina`, `mesero`, `admin`. Devuelve la sesión activa del tenant o `data: null` si no existe.

### `POST /api/v1/sesiones-caja`

Roles: `cajero`, `admin`. Requiere `Idempotency-Key`.

Request:

```json
{ "monto_inicial": "500.00" }
```

Efecto atómico: valida que no exista otra sesión abierta, asigna `fecha_operativa`, crea la sesión y registra el movimiento de apertura según el esquema físico aprobado. Respuesta `201` con la sesión creada. La operación de cierre queda fuera de esta entrega, pero la seed E2E debe partir sin sesión abierta.

### `GET /api/v1/estado-operativo`

Roles: todos. Devuelve `OperationalStatus` para el tenant activo.

Errores: `401 UNAUTHENTICATED`, `403 MEMBERSHIP_INACTIVE`.

### `POST /api/v1/latidos`

Roles: `cajero`, `cocina`, `mesero`. No recibe `Idempotency-Key`.

Request:

```json
{ "dispositivo": "tablet-cocina-01", "rol": "cocina" }
```

El backend valida que `rol` coincida con el rol autenticado. Respuesta `204`.

### `GET /api/v1/catalogo`

Roles: todos. Query opcional: `actualizado_desde=<ISO-8601>` cuando el esquema soporte actualización incremental. En la primera respuesta completa:

```json
{
  "data": { "categorias": [], "productos": [] },
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": "2026-07-20T15:00:00.000Z|101" },
  "error": null
}
```

Solo entrega productos disponibles al rol `cliente`. Roles administrativos quedan fuera de esta entrega.

### `GET /api/v1/espacios`

Roles: todos. Devuelve espacios activos del tenant:

```json
{
  "data": [
    { "id": 701, "nombre": "Mesa 4", "tipo": "mesa" }
  ],
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
  "error": null
}
```

No expone el `qr_token` interno del espacio. La selección del alumno usa `id`; el backend vuelve a validar tenant y actividad al crear el pedido.

### `POST /api/v1/pedidos`

Rol: `cliente`. Requiere `Idempotency-Key`.

Request:

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

Validaciones:

- caja abierta y dispositivos operativos según la regla aprobada;
- 1 a 50 líneas; `cantidad` entre 1 y 20;
- producto disponible y perteneciente al tenant;
- opciones pertenecen al producto y cumplen mínimos/máximos;
- `en_espacio` exige `espacio_id` activo del tenant;
- `para_llevar` exige `espacio_id: null`;
- el único método aceptado en esta entrega es `efectivo`;
- precios, total, folio, usuario, tenant y estado se calculan en servidor.

Efecto atómico: crea pedido, items, opciones y primer evento `por_cobrar`. Respuesta `201 OrderDetail`.

### `GET /api/v1/pedidos/{pedido_id}`

Roles:

- `cliente`: únicamente pedidos propios;
- `cajero`: pedidos `por_cobrar` y pedidos `listo` con destino `para_llevar`;
- `cocina`: pedidos `cobrado`, `preparando` o `listo` que contengan al menos un item `cocina`; la respuesta omite items de estación `caja`;
- `mesero`: pedidos `listo` con destino `en_espacio`;
- `admin`: cualquier pedido del tenant en modo lectura.

Respuesta `200 OrderDetail`.

### `GET /api/v1/pedidos`

Roles: todos, con filtrado server-side por rol. Query:

```text
?estado=por_cobrar,cobrado&actualizado_desde=2026-07-20T15:00:00.000Z&limit=50&cursor=<opaco>
```

- `cliente`: solo sus pedidos;
- `cajero`: `por_cobrar` y pedidos `listo` con destino `para_llevar`;
- `cocina`: `cobrado`, `preparando` y `listo` que requieren cocina; cada detalle contiene únicamente items `cocina`;
- `mesero`: `listo` con destino `en_espacio`;
- `admin`: cualquier pedido del tenant en modo lectura;
- `limit`: 1–100, default 50;
- orden estable por `actualizado_en`, luego `id`;
- respuesta incluye cursor opaco para evitar perder empates de timestamp.

### `POST /api/v1/pedidos/{pedido_id}/cobros-efectivo`

Rol: `cajero`. Requiere `Idempotency-Key`.

Request:

```json
{ "monto_recibido": "50.00", "version_esperada": 1 }
```

Validaciones: pedido del tenant, `por_cobrar`, método `efectivo`, caja abierta, monto recibido mayor o igual al total. El cambio se calcula en servidor.

Efecto atómico:

- inserta el pago en efectivo `confirmado`, sin actualizaciones posteriores;
- inserta movimiento `cobro` por `monto_recibido` y, cuando corresponda, movimiento `cambio`; comparten `idempotency_key` y usan secuencias distintas;
- calcula `base_calculo = SUM(precio_mostrador_unitario × cantidad)` y registra `comision = ROUND(base_calculo × 0.03, 2)` como devengada;
- agrega evento `por_cobrar → cobrado`;
- actualiza la proyección del pedido y su versión;
- si no hay items de Cocina, agrega además el evento automático `cobrado → listo` y devuelve la versión final.

Respuesta `201`:

```json
{
  "data": {
    "pedido": {},
    "monto_recibido": "50.00",
    "cambio": "24.00"
  },
  "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
  "error": null
}
```

### `POST /api/v1/pedidos/{pedido_id}/transiciones`

Roles: `cocina`, `cajero`, `mesero`. Requiere `Idempotency-Key`.

Request:

```json
{ "estado_objetivo": "preparando", "version_esperada": 2 }
```

El backend aplica exactamente la matriz de la sección 5. Respuesta `201 OrderDetail` con versión incrementada.

## 7. Idempotencia y concurrencia

- Primera recepción: procesa y persiste status/body.
- Repetición de misma llave, actor, operación y hash: reproduce el primer resultado.
- Misma llave con hash distinto: `409 IDEMPOTENCY_KEY_REUSED`.
- `version_esperada` distinta a la actual: `409 ORDER_VERSION_CONFLICT` con la versión actual en `details`.
- Dos cajeros no pueden cobrar el mismo pedido dos veces.
- Dos dispositivos de Cocina no pueden registrar la misma transición dos veces.

## 8. Códigos de error del módulo

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Forma o tipos inválidos. |
| 401 | `UNAUTHENTICATED` | Token ausente o inválido. |
| 403 | `FORBIDDEN_ROLE` | Rol sin permiso. |
| 403 | `MEMBERSHIP_INACTIVE` | Membresía inactiva. |
| 404 | `ORDER_NOT_FOUND` | Pedido inexistente o invisible para el actor. |
| 409 | `IDEMPOTENCY_KEY_REUSED` | Misma llave con request distinto. |
| 409 | `ORDER_INVALID_STATE` | Transición no permitida. |
| 409 | `ORDER_VERSION_CONFLICT` | Versión desactualizada. |
| 422 | `ESTABLISHMENT_NOT_RECEIVING` | Caja/operación no disponible. |
| 422 | `PRODUCT_UNAVAILABLE` | Producto no disponible. |
| 422 | `INVALID_PRODUCT_OPTION` | Opción ajena o cardinalidad inválida. |
| 422 | `INVALID_SPACE` | Espacio requerido, inactivo o de otro tenant. |
| 422 | `INSUFFICIENT_CASH_RECEIVED` | Efectivo menor al total. |
| 429 | `RATE_LIMITED` | Exceso de requests. |

Para evitar enumeración entre tenants, un recurso de otro establecimiento se responde como `404`, no `403`.

## 9. Polling y latido

- Caja y Cocina consultan como máximo cada cinco segundos mientras la pantalla operativa esté visible.
- El servidor usa cursor compuesto/opaco; el timestamp por sí solo no basta.
- Timeout, app en segundo plano y errores 5xx aplican backoff con jitter.
- Al volver al primer plano se solicita un delta inmediato.
- La UI conserva el último estado, indica desactualización y nunca inventa una transición exitosa.
- Después de una mutación exitosa, el cliente aplica la respuesta del servidor y luego reconcilia en el siguiente poll.

## 10. Pruebas de contrato obligatorias

### Caso feliz

- crear pedido efectivo para llevar;
- replay idempotente devuelve mismo `pedido.id` y folio;
- cobrar y comprobar cambio;
- avanzar por Cocina;
- entregar por Caja;
- repetir con espacio y entregar por Mesero.

### Autorización

- cliente no lista pedidos ajenos;
- cajero no avanza Cocina;
- cocina no cobra;
- cajero no entrega `en_espacio`;
- mesero no entrega `para_llevar`;
- token de tenant B no detecta la existencia de pedido de tenant A.

### Validación y consistencia

- no crear sin caja abierta;
- no aceptar precio ni total del cliente;
- rechazar producto/opción/espacio de otro tenant;
- rechazar cardinalidad de opciones;
- rechazar efectivo insuficiente;
- rechazar transición saltada;
- pedido compuesto solo por items de Caja avanza automáticamente a `listo` y no aparece en Cocina;
- pedido mixto muestra a Cocina únicamente sus items y espera su transición a `listo`;
- conflicto por versión concurrente;
- mismo idempotency key con payload diferente produce 409;
- ningún fallo intermedio deja pedido sin items o transición sin evento.

## 11. Cambios que requieren detenerse

- agregar o renombrar endpoint, tabla, columna o estado;
- cambiar tipos de dinero o formato de timestamp;
- cambiar el origen del tenant o rol;
- relajar idempotencia, transacción, RLS o autorización;
- alterar el orden del ciclo del pedido;
- incluir un método de pago o módulo fuera de alcance;
- interpretar un dato del mockup contra `00` a `06`.

La IA reporta la necesidad, explica impacto en Backend/Kotlin/Swift y espera decisión de Jesús.

## 12. Decisiones aplicadas

- Comisión de efectivo y saldo: 3% de la suma de precios de mostrador capturados en items; centavos y un solo redondeo por pedido.
- Caja: ledger `movimientos_caja` INSERT-only; `cobro` registra lo recibido y `cambio` lo devuelto.
- Pago en efectivo: se inserta `confirmado` y no cambia; los intentos Stripe se definirán en el contrato de Tarjeta.
- Preparación: `productos.estacion_preparacion` usa `cocina` o `caja`.
- Concurrencia: `pedidos.version` y `pedido_eventos` se escriben atómicamente.
- El esquema físico obligatorio está en `[[01-Modelo-Datos]]` §11.
