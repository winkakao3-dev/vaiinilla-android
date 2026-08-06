# VAI-27 — hardening del runtime REMOTE

## Estado y alcance

Esta revisión endurece el runtime REMOTE de VAI-27 sobre Firebase + Railway. MOCK fue eliminado como fuente de datos del runtime y no se empaqueta como una alternativa de ejecución en el APK. Los fixtures que siguen existiendo bajo `app/src/test` o en previews de Compose sólo sirven para pruebas/renderizados locales y no autorizan accesos ni pedidos.

El backend configurado para esta build es:

```text
https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

Las cuentas seed (`cliente`, `cajero`, `cocina`, `mesero`) son una ayuda exclusiva para builds `debug`/`preview`. Sus contraseñas sólo se leen desde `local.properties`; no están en este repositorio, en el APK release ni en esta documentación.

## Cambios de hardening

- Las llamadas bloqueantes del cliente `HttpURLConnection` para accesos, modos, invitaciones, alta de identidad, contexto y correo se ejecutan dentro de `Dispatchers.IO`.
- `POST /sesiones/contexto` envía `Idempotency-Key` en cada activación de modo y genera una llave UUID nueva en cada renovación.
- `POST /sesiones/contexto-cliente` también genera una llave nueva en cada alta o renovación del contexto del alumno; ya no usa una llave derivada determinísticamente del establecimiento y matrícula.
- La aceptación de invitación conserva una llave estable por token de invitación para que los reintentos sigan siendo idempotentes.
- El escáner QR ignora callbacks de CameraX que terminan después de cerrar el diálogo, libera el analyzer/proveedor y executor, informa errores de inicialización y muestra una ruta visible cuando no existe cámara trasera.

## Validación local

Ejecutada sobre el árbol local:

```bash
python3 scripts/validate_fixtures.py
./scripts/audit_scope_vai11.sh
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:ktlintCheck
./gradlew --no-daemon :app:lintDebug
./gradlew --no-daemon :app:assembleDebug \
  -PvaiinillaApiBaseUrl=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

Todos terminaron correctamente. El APK generado es:

```text
app/build/outputs/apk/debug/app-debug.apk
SHA-256: c84aa5d28c6c3dce068bceace1bae483a0f90b65c5d5eaaba7ac6365a258aed0
```

La integridad ZIP del APK también fue comprobada. Comando reproducible de build:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PvaiinillaApiBaseUrl=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

## Prueba física pendiente

En esta ejecución `adb devices -l` no reportó ningún dispositivo ni había un AVD disponible. Por eso todavía no se afirma como evidencia ejecutada la matriz que requiere Jesús:

- registro, verificación de correo y login;
- lectura QR y cierre del diálogo durante la inicialización de CameraX;
- cambio Cliente/Cocina;
- aceptación de invitación;
- creación y seguimiento de un pedido REMOTE;
- renovación del JWT después de 15 minutos.

La build está lista para instalarse en un dispositivo con backend, Firebase y credenciales de prueba configurados; esa matriz debe ejecutarse y registrarse desde un teléfono real antes de cerrar el PR.
