# Guardias contra las 8 clases de error

Las ocho clases ya vistas (U04, B04, U05, N05, I09, U03, P08, Z01, C05, L01, T07, W02/G04…). Cada una tiene una guardia
automática que corre en CI. «Cobertura» es una estimación honesta de qué fracción de los casos futuros de esa clase se
detecta antes de llegar a un teléfono, contando los rediseños en curso.

| # | Clase | Rediseños en curso | Guardia de este PR | Qué falta para ≥ 90 % | Cobertura |
|---|-------|--------------------|--------------------|-----------------------|-----------|
| 1 | Estado efímero en modales/timers | Estado único por pantalla (planeado) | Ratchet: no se agregan `remember { mutableStateOf }` en archivos con hojas/diálogos ni `delay` ≥ 3 s en pantallas (`guardias_clases_error.py`) | Pruebas de ciclo de vida (cerrar hoja, rotar, salir antes del timer) por pantalla; migrar los 37 archivos del baseline a ViewModel | ~55 % hoy, ~85 % al migrar |
| 2 | Productor–consumidor | Ninguno lo cubre | Ya existen fixtures contractuales; paridad de lectura por rol en backend | Prueba de contrato DTO Android ↔ OpenAPI del backend en CI (cada campo que la app lee existe y es del mismo tipo) | ~45 % |
| 3 | Asimetría multi-rol | VenueContext ayuda en cliente | Backend: `paridad-roles-rls.mjs` (admin/cajero/cocina/mesero ven lo mismo y nada ajeno) | Extender la matriz a más tablas (llamadas, cortes, tutelas) y a las reglas de pantalla por rol en jvmroles | ~60 % |
| 4 | Inversión semántica estado→texto | Ninguno | `StateToTextGuardTest`: todos los estados × formas de pago tienen letrero, los finales no comparten texto, entregado ≠ activo | Misma matriz para destino (mesa/llevar/«Pasa por él»), renta y tutela | ~65 % |
| 5 | Controles muertos | Ninguno | CERO casos de `Button(onClick = {})` | Detección de parámetros que se dibujan como texto fijo (no automatizable bien): checklist en revisión de PR + pruebas de recorrido | ~50 % |
| 6 | Fuga de contexto / UI estancada | VenueContext + red único + bandeja | Ninguna nueva (no se toca Venue/OperationalViewModel mientras sigan los hilos) | Prueba «cambiar de tienda limpia todo» y «el estado cambia → la UI redibuja» sobre VenueContext cuando se mezcle | ~70 % al mezclar VenueContext |
| 7 | Filtros silenciosos (RLS/acentos) | Ninguno | Android: CERO búsquedas con `ignoreCase` sin `normalizeForSearch` (arreglé Ayuda y Modo autorizado). Backend: guardia estática de `LIKE`/`ILIKE` (arreglé búsqueda de negocios del Super Admin) + `busqueda-establecimientos-sin-acentos.mjs` + paridad RLS | Matriz RLS contra el backend de pruebas (VPS2) por rol | ~90 % |
| 8 | Desbordamiento / letra grande | Screenshot tests/CI existentes (L01) | Pruebas L01 y hoja de cobro ya cubren Caja; sin guardia nueva | Capturas Roborazzi con `fontScale` 1.3 y 2.0 de Carrito, Caja, Cocina y Mesero (necesita compilar en la Mac) | ~50 % |

**Resumen:** con este PR y los rediseños el promedio es ~60 %. Se llega a ≥ 90 % en las clases 1, 3, 4, 6 y 7 con
los pasos de «Qué falta»; las clases 2, 5 y 8 piden trabajo extra (contrato OpenAPI, capturas con letra grande, revisión).

## Reglas de `scripts/guardias_clases_error.py`

- **Búsqueda sin normalizar (CERO):** usa `texto.normalizeForSearch().contains(consulta.normalizeForSearch())`.
- **Botón sin acción (CERO):** un `Button`/`IconButton` con `onClick = {}` es un control muerto; deshabilítalo o conéctalo.
  (`clickable(onClick = {})` para tragarse toques en hojas sí está permitido.)
- **Timer largo en pantalla (RATCHET):** un `delay` ≥ 3 s dentro de un composable pierde su efecto si el usuario sale
  (B04). Mueve el temporizador al ViewModel o guarda la marca al instante.
- **`remember` en archivos con hojas/diálogos (RATCHET):** el estado que debe sobrevivir al cierre de la hoja vive en el
  ViewModel o en `rememberSaveable` (U04). Al migrar un archivo corre `python3 scripts/guardias_clases_error.py --actualizar`.
