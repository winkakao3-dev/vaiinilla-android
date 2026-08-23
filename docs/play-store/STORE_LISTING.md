# Google Play Store Listing — Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **BORRADOR LISTO PARA CARGA**, con campos externos aún pendientes.

Este archivo concentra el copy y las decisiones de metadata que sí pueden
cerrarse desde el repositorio Android. No contiene credenciales ni modifica
Play Console.

Fuentes oficiales:

- Ficha de Play Store y límites de texto:
  https://support.google.com/googleplay/android-developer/answer/9859152
- Categorías y etiquetas:
  https://support.google.com/googleplay/android-developer/answer/9859673

## Metadata principal

| Campo | Valor / estado |
| --- | --- |
| Nombre | `Vaiinilla` — FINAL |
| Tipo | Aplicación — FINAL |
| Categoría | `Comida y bebida` — FINAL propuesta |
| Idioma predeterminado | Español — seleccionar locale exacto en Play Console |
| Descripción breve | FINAL propuesta |
| Descripción completa | FINAL propuesta |
| Tags | Pendientes de elegir desde las opciones reales de Play Console |
| Email de soporte/desarrollador | PENDIENTE EXTERNO |
| Teléfono | PENDIENTE EXTERNO / opcional según cuenta y ficha |
| Sitio web | PENDIENTE EXTERNO |
| Política de privacidad | PENDIENTE KAK-44 |
| URL web de eliminación | PENDIENTE KAK-47 |

Google Play permite hasta 30 caracteres para el nombre, 80 para la descripción
breve y 4,000 para la descripción completa.

## Descripción breve

> Consulta el menú y haz pedidos anticipados en cafeterías escolares.

Longitud verificada: **67 caracteres**.

## Descripción completa

Vaiinilla facilita la forma de pedir alimentos dentro de cafeterías escolares.

Consulta el menú de la cafetería a la que acudes, descubre los productos
disponibles y prepara tu pedido desde tu teléfono antes de llegar a caja.

Con Vaiinilla puedes realizar pedidos anticipados y reducir el tiempo que pasas
haciendo fila. Elige tus productos, personaliza tu pedido cuando haya opciones
disponibles y consulta su estado mientras la cafetería lo prepara.

También puedes acceder a tu historial de pedidos para revisar compras
anteriores y consultar tu saldo Vaiinilla y sus movimientos dentro de tu
cafetería.

Vaiinilla reúne en un mismo lugar las principales funciones que necesitas para
comprar en tu cafetería:

- Consulta el menú y los productos disponibles.
- Explora categorías, precios y opciones de cada producto.
- Realiza pedidos desde tu teléfono.
- Consulta el estado de tus pedidos.
- Revisa tu historial de compras.
- Consulta tu saldo y movimientos de Vaiinilla.
- Accede a la cafetería y espacio correspondiente mediante los métodos
  disponibles en el establecimiento.

Cada cafetería puede ofrecer productos, precios, métodos de pago y funciones
diferentes según su configuración.

Vaiinilla busca hacer más simple el momento de pedir: menos tiempo esperando y
más claridad sobre tu pedido.

## Decisiones de copy

No promocionar todavía en la ficha pública:

- `paga con tarjeta`;
- `Stripe`;
- `pagos con tarjeta en producción`;
- cualquier afirmación de pago live.

Motivo: el cliente actual integra Stripe PaymentSheet, pero está limitado a
`pk_test_...` y el E2E real/decisión Test Mode vs Live Mode sigue pendiente.

Sí se puede describir:

- menú y catálogo;
- pedidos;
- personalización de productos cuando existe;
- seguimiento/estado de pedido;
- historial;
- saldo Vaiinilla y movimientos;
- selección de establecimiento/espacio.

## Categoría y tags

Categoría recomendada y cerrada para el borrador: **Comida y bebida**. Google
incluye esta categoría para experiencias relacionadas con restaurantes y
alimentación, y representa mejor el uso principal que `Compras`, `Educación` o
`Finanzas`.

Google Play permite hasta cinco tags. Los tags concretos deben elegirse desde
las opciones que Play Console muestre para la app; no se versionan nombres de
tags inventados. Priorizar, si aparecen, conceptos equivalentes a:

1. pedidos de comida;
2. restaurantes/cafeterías;
3. comida y bebida;
4. pedidos anticipados;
5. menú de alimentos.

Elegir solo tags cuyo significado sea evidente al comparar la ficha con la app
real.

## Recursos gráficos

- Icono Play: FINAL.
- Feature Graphic: FINAL.
- 6 screenshots promocionales: **generados externamente, todavía no recibidos**.
- Tras recibirlos: validar 1080 × 1920, formato, peso, legibilidad, fidelidad a
  la UI y ausencia de funciones no disponibles.

Orden narrativo previsto de screenshots:

1. establecimiento / espacio;
2. catálogo;
3. detalle de producto;
4. carrito / checkout;
5. seguimiento del pedido;
6. saldo Vaiinilla.

## Campos que impiden marcar Store Listing como DONE

- [ ] Seleccionar locale exacto del idioma predeterminado en Play Console.
- [ ] Elegir hasta cinco tags reales ofrecidos por Play Console.
- [ ] Definir email de soporte/desarrollador público.
- [ ] Definir teléfono si corresponde.
- [ ] Confirmar sitio web público.
- [ ] Publicar política de privacidad y pegar URL final.
- [ ] Publicar URL web de eliminación de cuenta.
- [ ] Recibir, validar e integrar los seis screenshots finales.
