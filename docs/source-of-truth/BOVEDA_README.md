---
tipo: paquete-modulo
proyecto: Vaiinilla
modulo: pedido-efectivo-e2e
estado: aprobado
fecha: 2026-07-20
aprobado_por: Jesus Leos
aprobado_en: 2026-07-20
relacionado:
  - "[[CONTEXT]]"
  - "[[CONTRACTS]]"
  - "[[03-Decisiones]]"
---

# Entrega 01 — Pedido en efectivo de punta a punta

Tablero del equipo: [Vaiinilla — Ejecución MVP](https://app.notion.com/p/3a3c4e951cdb81dda4acfbcf4113c51f).

## Resultado que debe existir el viernes

Un alumno abre el catálogo, configura productos, elige efectivo y crea un pedido. Caja lo cobra, Cocina lo prepara y marca listo, Caja o Mesero lo entrega según el destino, y el alumno ve el estado final. Todo ocurre contra el mismo backend y el mismo contrato en Android e iOS.

## Este paquete contiene exactamente tres archivos

1. `README.md` — qué se entrega, límites y definición de terminado.
2. `CONTEXT.md` — por qué existe, reglas, flujo del equipo y trabajo de cada persona.
3. `CONTRACTS.md` — modelos, endpoints, estados, errores, concurrencia y pruebas de contrato.

La IA debe leerlos **en ese orden y completos** antes de escribir código. Si un repositorio contiene `AGENTS.md`, `CLAUDE.md` o reglas propias, también se leen y se obedecen.

## Alcance incluido

- disponibilidad operativa según sesión de caja;
- catálogo, categorías, productos, grupos y opciones;
- carrito y cálculo visual previo;
- creación server-side de pedido en efectivo;
- vista de pedidos por rol;
- cobro en efectivo;
- transición `cobrado → preparando → listo → entregado`;
- destinos `para_llevar` y `en_espacio`;
- entrega por Caja o Mesero según destino;
- polling incremental y latido;
- trazabilidad, idempotencia y pruebas E2E.

## Fuera de esta entrega

- tarjeta/Stripe y webhooks;
- wallet, recargas y cashback;
- cancelaciones y reembolsos;
- stickers;
- analíticas y administración del catálogo;
- modo offline de Caja;
- publicación en tiendas;
- descuentos por producto.

Fuera de alcance no significa fuera del MVP: significa que se implementará en otra entrega.

## Fuentes de verdad

En caso de contradicción:

1. `[[00-Estado-Actual]]`;
2. `[[01-Modelo-Datos]]` y `[[05-Estandares-API]]`;
3. este `CONTRACTS.md`, una vez aprobado;
4. `[[04-Especificacion-Funcional]]` para comportamiento;
5. mockup para diseño visual;
6. este `CONTEXT.md` y la tarea diaria.

Nada dentro de `Archivo/` se usa para implementar.

## Definición de terminado

- [ ] Contrato aprobado por Jesús.
- [ ] Migración y seeds ejecutan desde base vacía.
- [ ] OpenAPI coincide con `CONTRACTS.md`.
- [ ] Pruebas unitarias y de integración pasan en backend.
- [ ] Pruebas de contrato pasan con los mismos fixtures en backend, Kotlin y Swift.
- [ ] Android e iOS completan el flujo con el backend real del ambiente de desarrollo.
- [ ] Un reintento no duplica pedido, cobro ni transición.
- [ ] Un rol o tenant incorrecto recibe error y no filtra datos.
- [ ] El estado sobrevive cierre/reapertura de las apps.
- [ ] El PR tiene revisión requerida y CI verde.
- [ ] La demo E2E se realiza y la bóveda se actualiza después del PR autorizado.

## Regla de parada

La IA se detiene y reporta el punto exacto si necesita cambiar una tabla, columna, endpoint, estado, permiso o regla de este paquete. Nunca corrige el contrato en silencio.
