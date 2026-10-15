# Vigía

Prueba la app de Android completa en una JVM (sin emulador) y anota lo que una persona vería mal: cargas
que no terminan, errores que nadie avisa, pantallas que dicen "no hay nada" cuando en realidad no cargaron,
textos cortados, botones que no hacen nada. Corre contra el backend de dev con las personas del sandbox de
agentes.

## Antes de subir un AAB

```sh
tools/vigia/vigilar.sh                 # compila, explora con 3 redes y corre los flujos
tools/vigia/vigilar.sh --sin-flujos    # sin escribir nada en el sandbox de dev
```

Sale con 0 solo si no hubo errores. Cada corrida deja `informe.md`, `hallazgos.json` y capturas en
`~/.cache/vaiinilla/vigia/<fecha>/`.

## Las piezas

| Pieza | Qué hace |
|---|---|
| `compilar.sh` | Compila el arnés con heaps pequeños (la Mac tiene 8 GB) y corre `VigiaTest`. |
| `vigia.py explorar` | Entra a cada rol y toca todo lo que no escribe, con la red `normal`, `"fallas 30"`, `"lento 6000"` o `caido`. Solo lectura: si algo escribe, vuelve como error y no llega a dev. |
| `flujos.py` | Recorridos escritos a mano entre roles (alumno pide → caja cobra → cocina prepara). Sí escriben en el sandbox, comparan cada paso con la API y limpian lo que crean. |
| `backend.py` | La API de dev vista como cada rol, para comparar la pantalla con la verdad. Nunca imprime tokens. |

## Lo que detecta

| Hallazgo | Cuándo |
|---|---|
| `carga-eterna` | Algo sigue cargando sin ninguna petición en curso. |
| `carga-larga` | Una carga con red tarda más del triple del límite. |
| `error-mudo` | Falló una escritura y la pantalla no cambió ni avisó. |
| `falso-exito` | La pantalla dijo "listo" o "cobrado" y el backend sigue igual (flujos). |
| `vacio-falso` | Con la red caída la pantalla dice "Al día", "Nada por cobrar" o "Sin pedidos". |
| `en-vivo-viejo` | Dice "En vivo" mientras sus actualizaciones fallan. |
| `pantalla-en-blanco` | Nada en pantalla por más de 4 s. |
| `cierre` | La app truena. |
| `texto-cortado`, `fuera-de-pantalla`, `encimados` | Revisión de cada pantalla nueva sin tocarla. |
| `toque-sin-efecto`, `boton-sin-nombre`, `atras-no-regresa` | Al explorar. |
| `pantalla-vs-backend`, `flujo-roto`, `interferencia` | En los flujos. |

## A mano

```sh
vbuild jvm-roles reiniciar                      # levanta el arnés con la última compilación
V() { python3 tools/vigia/vigia.py "$@"; }
V explorar --roles cocina,caja --red caido      # un rol, una red
V red "fallas 100 --ruta transiciones"          # sabotear solo una ruta
V role caja && V list && V shot /tmp/caja.png   # cualquier comando del arnés
python3 tools/vigia/flujos.py --lista
vbuild jvm-roles parar                          # siempre al terminar
```

## Cosas que muerden

- El sandbox de agentes es compartido. Si otro agente cobra o prepara el pedido de un flujo antes que él,
  el paso queda como `interferencia` y no se puede juzgar.
- Explorar no toca lo que escribe (cobrar, pagar, cerrar caja, rechazar, cambiar disponibilidad…). Eso solo
  se prueba en los flujos, y los errores de escritura solo aparecen ahí.
- `toque-sin-efecto` en las opciones de tema es ruido: el cambio es solo de color.
