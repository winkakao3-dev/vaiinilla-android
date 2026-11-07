# Lanzamiento escalonado en Google Play

Plan para publicar la app de cliente (`vaiinilla.innovapro.app`) sin exponer a todos los usuarios de golpe.
No cambia código: es el procedimiento. Aplica a cada versión nueva de Producción.

## Escalones

| Escalón | % de usuarios | Mínimo de espera | Se pasa al siguiente si… |
|---|---|---|---|
| 1 | 5 % | 24 h con tráfico real (un servicio completo de un negocio) | Sin cierres nuevos, sin ANR nuevos, pedidos y pagos normales |
| 2 | 20 % | 24 h | Igual, y sin quejas de soporte sobre la versión |
| 3 | 100 % | Fin | Se mantiene vigilancia 48 h |

Con un solo negocio activo (como el boliche del lanzamiento) el 5 % puede ser un puñado de teléfonos:
en ese caso el escalón 1 se sostiene con los teléfonos del equipo y la espera cuenta por servicios, no solo por horas.

## Antes de subir el porcentaje (checklist)

1. **Cierres (Crashlytics)**: ningún cierre nuevo de la versión con más de 1 usuario afectado. Si Crashlytics está apagado
   (`vaiinilla.crashReporting=off`), este punto no se puede verificar: no subir de escalón sin otra señal.
2. **Play Console > Calidad > Android vitals**: tasa de cierres y de ANR de la versión no peor que la anterior.
3. **Pedidos y pagos**: el último pedido real con tarjeta, con saldo y en efectivo terminó bien (folio, cobro, entrega).
4. **Backend**: sin errores 5xx nuevos en la API desde que la versión recibe tráfico.
5. **Soporte**: ninguna queja repetida sobre la versión.

## Cuándo detener

Detener el lanzamiento (Play Console > Versión > Detener lanzamiento) si:
- aparece un cierre que afecta pedidos, pagos o el QR de recogida;
- la tasa de cierres de la versión supera 2 veces la anterior;
- se cobra algo que no debía (cualquier caso de dinero).

Detener no desinstala a quien ya la recibió. Para ese grupo: publicar una versión corregida (código de versión nuevo)
y llevarla otra vez por los escalones; si es crítico, subir directo a 100 % con la corrección ya verificada.

## Cómo se hace en Play Console

1. Producción > Crear versión nueva > subir el AAB firmado > notas de la versión.
2. En "Porcentaje de lanzamiento" poner 5 y enviar a revisión. Dejar la **publicación administrada apagada** solo si se
   quiere que salga sola al aprobarse; con escalones es mejor **encendida** para controlar la hora.
3. Para subir: Producción > Versión > "Aumentar el lanzamiento" > 20 % y luego 100 %.

## Quién decide

Naev decide cada ajuste de porcentaje. Esta guía solo fija el criterio; no se sube de escalón sin su visto bueno.

## Pendiente de confirmar

- Si la cuenta de Play permite lanzamiento por porcentaje en la app actual (debería: es la función estándar de Producción).
- Qué negocio sirve de "cobaya" real en el 5 %, porque Play elige usuarios al azar y no por negocio.
