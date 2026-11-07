# Red de seguridad: CI, capturas, contrato y cierres

Qué cubre cada capa, cómo correrla en la Mac y qué falta. Fecha: 6 nov 2026, sobre `main` 5243c56b.

## 1. CI `verify` confiable

`verify` ya no depende de `gh pr merge --admin`. Pasa cuando:

| Paso | Qué hace | Estado de partida |
|---|---|---|
| Fixtures, alcance, gate app/backend | Igual que antes | Verde |
| Pruebas unitarias Dev y Prod | Excluye las de `app/quarantine-tests.txt` | 14 pruebas rojas viejas, aisladas |
| Android lint (Prod) | Igual que antes | Ver resultado en el PR |
| ktlint | `ktlintCheck -PktlintRatchet` + `scripts/ktlint_ratchet.py` | 2 828 avisos viejos en 131 archivos, congelados |

- **Cuarentena**: el job `quarantine` corre esas 14 pruebas sin bloquear. Al arreglar una, se borra de `app/quarantine-tests.txt`.
  Local: `./gradlew testDevAlumnoDebugUnitTest -PquarantineOnly`.
- **ktlint con trinquete**: cuenta avisos por (archivo, regla), no por línea; mover código no rompe nada. Un aviso nuevo falla el CI.
  Para bajar el baseline tras limpiar: `./gradlew ktlintCheck -PktlintRatchet && python3 scripts/ktlint_ratchet.py --update`.
  Nunca subir el baseline para tapar un aviso nuevo.
- No se tocó código de producción para esto.

## 2. Capturas de pantalla

Antes, las 240 capturas de Roborazzi solo se grababan: nada las comparaba. Ahora:

```bash
scripts/screenshots.sh verify     # compara y aplica el trinquete (≈7 min en la Mac)
scripts/screenshots.sh record     # regraba referencias (después, subir los PNG)
scripts/screenshots.sh baseline   # reescribe config/screenshots/stale.json
```

Resultado al armarlo: **244 capturas comparadas, 64 limpias, 180 desactualizadas** (la pantalla cambió desde que se grabó el PNG,
o nunca hubo referencia). Esas 180 están en `config/screenshots/stale.json`: no bloquean, pero cualquiera de las 64 limpias que cambie sí
bloquea. Dos corridas seguidas dieron exactamente el mismo resultado (sin falsos positivos).
Tolerancia: 0.1 % de píxeles distintos.

**Dejado fuera por riesgo**: no regrabé las 180. Serían ~80 MB más de PNG en el historial y, al ser pantallas que otros hilos
están rediseñando, aprobar hoy lo que se ve sería congelar trabajo en curso. Cada hilo regraba lo suyo
(`scripts/screenshots.sh record NombreDeLaClase`) y corre `--update` para sacarlo de `stale.json`.

**CI**: las referencias son de Mac y el runner es Linux. El job `screenshots` corre pero **no bloquea** (`continue-on-error`) hasta
que una corrida salga verde en el runner; entonces se quita esa línea.

## 3. Pruebas de contrato app-backend

Hecho: `ApiErrorContractTest` fija el sobre de error del backend (`code`, `message`, `details`, `Retry-After`, cuerpos no JSON,
campos nuevos). Los fixtures de éxito ya los cubren `scripts/validate_fixtures.py` y `OrderContractTest`.

**Pendiente (depende de `claude/project-thread-red-unico`, PR #106, sin juntar)**: una prueba a nivel HTTP que levante un servidor
local (`com.sun.net.httpserver`, sin dependencias nuevas) y compruebe contra el cliente único: ruta y método, cabecera
`Authorization`, `Idempotency-Key` en mutaciones, reintento solo de GET, refresco de sesión una vez, y decodificación de cada fixture.
Hoy `HttpVaiinillaApiClient` no se puede construir en una prueba sin sus cinco dependencias, y el PR #106 lo rehace. Escribirla ahora
sería hacerla dos veces.

## 4. Reporte de cierres (Crashlytics)

Crashlytics ya estaba en la app y encendido en producción. Ahora:

- Interruptor de compilación: `-Pvaiinilla.crashReporting=on|off|auto` o `VAIINILLA_CRASH_REPORTING`. `auto` (por defecto) = igual
  que antes: solo producción en release. `off` apaga el envío sin tocar código.
- El manifiesto lo deja **apagado de origen** (`firebase_crashlytics_collection_enabled=false`); la app lo enciende al arrancar según la política.
- `CrashDataScrubber` quita correos, teléfonos, tokens y contraseñas del mensaje (y de las causas) antes de que salga hacia Crashlytics, tanto
  el reporte como el cierre fatal. La pila no cambia. El log local sigue viendo el original.
- No se agregan claves personalizadas ni identificador de usuario.

**Límite**: el limpiador es por patrones; un dato personal con formato raro en un mensaje puede pasar. Regla de equipo: no meter datos
de personas en mensajes de excepciones. Cierres nativos (NDK) no llevan mensaje.

## 5. Lanzamiento escalonado

Ver `docs/LANZAMIENTO_ESCALONADO.md` (5 % → 20 % → 100 %, criterios para subir y para detener).

## Cómo probarlo en teléfono

Solo hace falta para Crashlytics: instalar una compilación Prod con `-Pvaiinilla.crashReporting=on`, provocar un cierre y ver en la consola
de Firebase que llega sin correos ni teléfonos en el mensaje. No se hizo en dispositivo.
