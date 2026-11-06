---
name: vaiinilla-pruebas
description: Auditoría de punta a punta de Vaiinilla Android (versión 2.0) antes de una prueba en vivo en teléfonos. Úsala antes de cualquier prueba en vivo, al preparar o auditar un bloque de recorridos (por ejemplo «bloque G» o «G01, G02, Z07»), al preguntar qué va a fallar en vivo, al revisar pantallas, textos cortados o letra grande en la JVM con Roborazzi, al probar timers o salidas rápidas, y al probar QR o NFC sin cámara. Rastrea cada dato desde la tabla o el endpoint hasta el @Composable, simula los recorridos en seco, y entrega hallazgos verificados con Archivo:Línea y parche.
argument-hint: "[bloque-o-recorridos]"
---

# vaiinilla-pruebas

Encuentra lo que va a fallar en la prueba en vivo antes de gastar teléfonos y tiempo en ella. Lee código y corre pruebas en la JVM con `vbuild test`. No cambia el código de la app ni toca los teléfonos, salvo que el usuario lo pida.

**Regla inquebrantable. PROHIBIDO hacer revisión ligera:** un recorrido no recibe menos revisión porque ya haya pasado en los teléfonos. Los 182 recorridos se auditan con la misma profundidad.

Alcance pedido: `$ARGUMENTS`

- Vacío: corre la Fase 1 sobre todo el flujo que el usuario esté por probar. Si no se sabe cuál es, pregúntalo.
- Un bloque o una lista de recorridos: corre la Fase 1 limitada a esas pantallas y después la Fase 2.

## Preparación

1. Fija las dos versiones que vas a leer y escríbelas al inicio del reporte con su commit.
   - **Android:** la rama que indique el usuario. Si no indica ninguna, el código de la APK combinada de la prueba (`.worktrees/apk-combinada`, rama `integracion/pruebas-android`).
   - **Backend:** `origin/develop` de `saul1217/vaiinilla_back`. Si la prueba corre contra otro servidor, como el de pruebas de VPS2, confirma qué commit corre allí, porque ese es el que vale.
2. Lee con `git grep <ref>` y `git show <ref>:<ruta>`, sin cambiar de rama en ningún checkout. El checkout principal de Android puede estar en uso.
3. El clon del backend es `/Volumes/KINGSTON1TB/Dev/proyectos/vaiinilla_back`. Puede estar en una rama vieja y con archivos sin commit, así que corre solo `git fetch origin develop` y lee desde `origin/develop`. Nunca hagas checkout ahí. El backend se cambia con una rama y un PR hacia `develop` que revisa el agente de Saúl.
4. Si hay recorridos, toma su texto de la hoja `pruebas-telefonos/hoja-resultados.xlsx` (hoja «Recorridos») o de `sitio/datos-*.js` en `ssh vps:/root/qa-testers`. Un id que no aparezca en ninguna de las dos se reporta como no encontrado. No inventes sus pasos.

## Dónde vive cada capa

| Capa | Ruta |
|---|---|
| Base de datos | `supabase/migrations/` (la tabla o columna vigente es la de la última migración que la toca) |
| Endpoint | `src/routes/*.routes.js`, `src/controllers/`, `src/services/`, `src/repositories/` |
| DTO y JSON | `app/src/main/java/com/vaiinilla/app/data/<área>/` (`*Dtos.kt`, `*Json.kt`, `*Mapper.kt`) |
| Repository | `domain/repository/` (la interfaz) y `data/<área>/Remote*Repository*.kt` |
| ViewModel | `ui/**/*ViewModel.kt` (`_uiState`) |
| Pantalla | `@Composable` en `ui/**` |

La traza siempre sigue este camino: `Tabla/Endpoint Backend -> DTO Kotlin -> Repository -> ViewModel (_uiState) -> @Composable`.

## Fase 1: los 7 vectores de auditoría estática

Prohibido revisar estilo de código o archivos aislados. Cada hallazgo tiene que salir de la traza cruzada completa.

Cada vector es un pase independiente, con su propia salida. Los 7 son obligatorios. Lánzalos como subagentes en paralelo, uno por vector, con la versión de cada repo y el alcance en el encargo. Solo leen. En las máquinas donde un hook niega los subagentes, no lo apagues: córrelos tú mismo, en serie, salvo que el dueño autorice una excepción en el chat.

### 1. Productor -> Consumidor (escritura contra lectura)

- Rastrea dónde se guardan configuraciones, pagos anticipados, reservas y fondos de caja.
- Verifica que la pantalla que consume ese dato consulte la misma tabla y el mismo campo, y que no lo excluya un `WHERE` o un `.filter {}` mal puesto. Ejemplos: filtrar solo `UNPAID` e ignorar rentas prepagadas, o ignorar el fondo sugerido configurado en la tienda.

### 2. Ciclo de vida en Compose y modales (pérdida de estado)

- Inspecciona todos los `ModalBottomSheet`, `Dialog`, `LaunchedEffect` y `DisposableEffect` del alcance.
- Reporta cualquier estado en `remember` dentro de un `if (showSheet)` que se destruya al cerrar el modal, y los `onDismiss` que limpien listas capturadas antes. Ejemplo: borrar los jugadores al cerrar la hoja de pago.

### 3. Controles muertos y bloqueos en checkout

- Verifica en las pantallas y hojas de cobro que cada parámetro editable del pedido (duración, cantidades, propinas) tenga un control activo.
- Reporta los que sean un `Text` estático o una lambda vacía `{}`.

### 4. Paridad multi-rol (simetría de reglas de negocio)

- Compara los flujos que crean o modifican la misma entidad desde distintos roles: Caja/POS, teléfono del cliente, Mesero y Cocina.
- Reporta si un rol exige un dato que otro omite o que el backend deja pasar en blanco. Ejemplo: pedir jugadores al rentar una pista.

### 5. Contratos de API y defaults silenciosos en DTO

- Cruza cada respuesta JSON del backend con su `data class`.
- Detecta propiedades con default (`= 0.0`, `= ""`, `= emptyList()`, `= null`) cuya llave no coincida exactamente con la del backend, porque ocultan el fallo.
- Lee el `Json { }` que parsea cada DTO, porque cambia por archivo. `ignoreUnknownKeys = true` calla una llave mal escrita. `coerceInputValues` cambia un valor inválido por el default. `explicitNulls` no vale igual en `core/network/ApiResponseParser.kt` que en `data/admin/AdminBusinessJson.kt`.

### 6. Semántica exhaustiva de textos y enums

- Revisa todos los `when` e `if/else` que convierten enums o estados en textos de UI, como la modalidad de entrega «Pasa por él» contra «A domicilio».
- Reporta cualquier condicional invertido o rama `else` que agrupe estados incorrectos.

### 7. Timers, debounces y salidas rápidas

- Busca `delay()`, `debounce()`, `LaunchedEffect` con esperas y guardados diferidos.
- Por cada uno responde: si el usuario sale de la pantalla durante la espera, ¿la acción se manda, se pierde o se manda dos veces? Mira `onDispose` y `ViewModel.onCleared`, y dónde vive el ViewModel: una mutación en `viewModelScope` muere si el ViewModel se destruye junto con la pantalla.
- Compruébalo con un test. En Compose usa `composeTestRule.mainClock.advanceTimeBy`, y en un ViewModel `advanceTimeBy` de `runTest`. Simula la salida quitando la pantalla de la composición, como en `KitchenReadyDelayTest` (caso B04).
- Para el cambio de día (caso Y02), si el camino lee `System.currentTimeMillis()` o `Instant.now()`, agrega un parámetro `now` con el reloj real por defecto, como `OfflineCounterQueue` (`now: () -> Instant = Instant::now`), y adelanta 24 horas en el test sin esperar. Si no lee la hora, como `StoreWelcomeMemory`, no inyectes nada y prueba que una instancia nueva conserve el dato.
## Fase 2: simulacro en seco de recorridos

Corre solo si el usuario indicó un bloque o una lista de recorridos.

1. Simula cada recorrido leyendo el código desde el `onClick` del `@Composable` hasta el `INSERT` o `UPDATE` del backend, y de regreso al `_uiState`.
2. Clasifica cada uno:
   - `[LISTO PARA VIVO]`: el camino completo existe y coincide con lo que pide el recorrido.
   - `[FALLARÁ EN VIVO]`: indica el archivo y la línea exacta del bloqueo.
   - `[NO SE PUEDE SABER LEYENDO]`: depende de datos o de hardware, y di qué hay que mirar en vivo.
3. Si leer no alcanza, puedes correr el recorrido en la JVM con jvm-roles (abajo), siempre que no haya una prueba en vivo o una compilación en curso.

## Fase Visual en JVM

Corre esta fase en todo recorrido que involucre diseño, etiquetas críticas (P08 «Apartado» contra «Encargo», A04 filtros en «Dónde pedir»), refresco de pantalla o letra grande (L01). Ya hay Roborazzi en el proyecto. La clase base es `app/src/test/java/com/vaiinilla/app/ui/screenshot/VisualAuditBase.kt`:

```kotlin
class CarritoVisualTest : VisualAuditBase() {
    @Test
    fun carrito() = audit("carrito") { CartScreen(/* estado de prueba */) }

    @Test
    fun saldoSeRefresca() {
        val state = MutableStateFlow(/* antes */)
        auditRefresh("saldo", mutate = { state.value = /* después */ }) { Pantalla(state) }
    }
}
```

- `audit` pinta en escala `1.0` y `1.3`. Para abrir una `ModalBottomSheet` o llegar a un estado, usa `afterPaint`: corre después de cada pintada.
- La prueba falla sola si la pantalla sale negra o de un solo color, o si un texto sale con elipsis, cortado, partido a media palabra o encimado.
- `auditRefresh` falla si mutar el estado no cambia ni un píxel.

Corre `vbuild test NombreDelTest`. Los PNG quedan en `app/build/outputs/roborazzi/auditoria/`, en el checkout donde corriste. **Abre cada `.png` con la herramienta `Read`** y confirma tú mismo:

a) No hay pantallas negras, como la que pasó en el carrito.
b) Ningún texto se corta con elipsis ni se encima en Carrito, Caja o listas con `fontScale = 1.3f`.
c) La pantalla se refresca tras mutar el `_uiState` (compara `-antes.png` y `-despues.png`).

Una prueba en verde no sustituye esa revisión: el detector no ve colores ni jerarquía, ni si la etiqueta dice lo que debe. En tu reporte cita el PNG que abriste. Los baselines de `src/test/roborazzi` no se tocan desde aquí.

## QR y NFC sin cámara

`ScanBridge` (`ui/discovery/ScanBridge.kt`) entrega el dato al mismo callback que usa el lector real. Hace falta que el lector esté abierto, igual que en el teléfono.

| Recorrido | Lector | Quién lo consume |
|---|---|---|
| A05 | QR de mesa | `AppNavHost.kt`, `QrScannerDialog` «Apunta al QR de tu mesa» |
| B04, B05, E01, G05 | QR del pedido | `AppNavHost.kt`, `pendingPickupDelivery` (Caja, Entregar) |
| I03 | QR del pedido | `WaiterOperationalScreen.kt`, `onDeliver` |
| G04, W02 | QR de cuenta | `OperationalViewModel.resolveWalletUserQr` |
| F09 | QR y NFC de credencial | `StudentCounterSheet.kt`, `actions.onCredentialRead` |

En un test de JVM, con el lector en pantalla:

```kotlin
ScanBridge.injectQr(payload) // false si no hay lector de QR abierto
ScanBridge.injectNfc("04A1B2C3") // false si no hay lector de NFC armado
```

En jvm-roles, con el lector abierto en el rol:

```bash
python3 /Volumes/KINGSTON1TB/Dev/proyectos/vaiinilla-roles/android/jvm-ui.py qr "<payload>"
python3 /Volumes/KINGSTON1TB/Dev/proyectos/vaiinilla-roles/android/jvm-ui.py nfc 04A1B2C3
```

En la corrida combinada (API + JVM), saca el dato real de la tienda de pruebas de VPS2 y pásaselo al lector. No inventes un payload.

- **Pedido:** `GET /api/v1/pedidos/{id}/qr` como el cliente dueño. Devuelve `qr_token`.
- **Mesa:** `https://vaiinilla.app/<slug>/m/<token>`, con el slug y el token reales del espacio (`QrPayloadParser.encodeSpace`).
- **Cuenta:** `https://vaiinilla.app/u/<id>`, con el id real del usuario (`QrPayloadParser.encodeUser`).
- **Credencial:** el prefijo es `StudentCredentialPayload.PREFIX`, con el dato real del alumno.

Esto prueba la lógica detrás de la cámara. No prueba que la cámara enfoque ni que el teléfono detecte la etiqueta: repórtalo así y no marques esa parte como vista en un teléfono.

## Cobertura con VPS2

En la corrida de API y JVM cubre estos flujos con las cuentas de prueba de VPS2. Antes de cada corrida comprueba lo que hace falta, porque cambia. Revisa solo nombres de variables y el prefijo de la llave, nunca su valor.

| Recorridos | Hace falta | Estado al 5 nov 2026 |
|---|---|---|
| Q01, J10, F03, S01, S07 (Stripe) | Llaves de prueba y una tienda conectada | Hay llaves `sk_test_` y `pk_test_`, pero `establecimiento_stripe` tiene 0 filas: ninguna tienda puede cobrar con tarjeta todavía |
| J07 (correo) | `ACCESS_EMAIL_FROM` y un proveedor de correo | No existen esas variables: invitar da 500 |
| P06 (asientos numerados) | Una tienda con asientos | El backend lo soporta; ninguna tienda de pruebas los tiene |
| M01 a M05 (2FA) | Una cuenta con TOTP enrolado en Firebase y su semilla | `plataforma.service.js` exige TOTP reciente; no hay una semilla de prueba documentada |

Si falta algo, márcalo `pendiente` con el motivo exacto. No lo des por probado ni le quites la verificación al backend para que pase.
## Fase 3: juez anti-alucinación y formato de salida

Antes de reportar un bug, abre los archivos de las 4 capas involucradas (backend, DTO, ViewModel, pantalla) y confirma cada línea que vas a citar. Si no pudiste leer una capa, el hallazgo no entra a la tabla: va aparte como sospecha, con lo que faltó leer. Si un subagente trajo el hallazgo, repite tú la lectura.

Entrega una tabla o lista accionable. Cada hallazgo lleva:

1. `[ID / Severidad]` y el recorrido afectado. Severidad: `Bloquea`, `Alta`, `Media` o `Baja`.
2. La traza exacta: `Archivo:Línea` donde nace el error -> `Archivo:Línea` donde explota en UI.
3. La causa raíz en una oración y el parche de código exacto para corregirlo sin romper flujos existentes. El parche se propone y no se aplica, salvo que el usuario lo pida. Si el arreglo es del backend, dilo: va en una rama con PR hacia `develop`.
4. Si se solicita, el test de integración en JVM (abajo).

Cierra el reporte con las dos versiones leídas, los recorridos por clasificación y lo que no se revisó.

## Pruebas en la JVM, sin emulador

Son dos herramientas distintas:

**Test de integración (ViewModel + FakeRepository en memoria).** Es JUnit puro, sin red ni dispositivo, y es lo que se entrega con un hallazgo. Copia el patrón de `app/src/test/java/com/vaiinilla/app/ui/operational/PendingRefundsViewModelTest.kt`: un `FakeRepository` que implementa la interfaz de `domain`, `UnconfinedTestDispatcher` y `TestScope`. Si la interfaz es grande, mira `ui/parentmode/ParentModeFakes.kt`. El test debe fallar sin el parche y pasar con él. Se corre con:

```bash
vbuild test NombreDelTest
```

**jvm-roles (la app completa en Robolectric contra el backend de dev).** Sirve para recorrer un flujo por roles con unos 400 MB, incluidos QR y NFC con el puente de arriba. Sus comandos están en la skill `vaiinilla-roles`, sección «Android: por defecto jvm-roles». Resumen:

```bash
vbuild jvm-roles
python3 /Volumes/KINGSTON1TB/Dev/proyectos/vaiinilla-roles/android/jvm-ui.py role caja
vbuild jvm-roles parar
```

Corre `vbuild jvm-roles` desde el checkout que quieres probar y apágalo en cuanto termines. No cubre la hoja de Stripe, Google Sign-In, la lectura física de NFC o cámara ni push: eso se mira en los teléfonos. Según las reglas del proyecto, jvm-roles cuenta como prueba y no como dispositivo.

Para los dos: si `vbuild` no está en el PATH, antes corre `export PATH="/Volumes/KINGSTON1TB/Dev/proyectos/vaiinilla-workspace/mac:$PATH"`. No uses `./gradlew` directo. Un test escrito y sin correr se reporta así, sin decir que pasó.

## Reglas de las pruebas en vivo

- Sin emuladores. Esta regla manda sobre lo que diga `vaiinilla-roles`: lo que pide hardware se prueba en un teléfono real.
- En los teléfonos solo se usan dos MCP: Mobile Next (`mobile-mcp`) y ADB MCP (`adb-mcp`).
- Una sola build combinada para toda la corrida. Esta auditoría no arma otra.
- Durante una prueba en vivo no se compila ni se corre Gradle en esa Mac. Deja los tests escritos y córrelos al terminar.
- Los resultados van a dos lugares:
  - **La hoja** `pruebas-telefonos/hoja-resultados.xlsx`, hoja «Recorridos». Columnas: Bloque, ID, Área, Recorrido, Teléfono · app (cuenta), Tienda, Resultado, Paso, Qué pasó, Comentario visual, Severidad, Capturas, Probado (hora), Para arreglar (repo / pantalla).
  - **La página** `qa.kakaowin.garden/robot-tel`, con `POST /robot-tel/api/resultados` como el tester `robot-tel`. Campos: `plataforma` (`android`), `recorrido` (una letra y dos dígitos, como `G01`), `estado` (`funciona`, `no_funciona`, `pendiente` o `comentario`), `motivo` (mínimo 5 caracteres en `no_funciona` y `pendiente`), `paso`, `quien` y `capturas` (hasta 3). Las credenciales no se guardan en este repo.
- El simulacro de la Fase 2 no marca nada como probado. Solo cuenta lo que se vio en el teléfono.