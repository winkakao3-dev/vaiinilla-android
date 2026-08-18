# Android release readiness — estado actual

Fecha de revisión: 2026-08-17

Este documento resume únicamente los pendientes vigentes para preparar una publicación Android. No usa IDs de tareas históricas, “entregas” anteriores ni estados viejos de Notion como fuente de verdad para el release actual.

## Regla de trabajo actual

- La fuente técnica del cliente es el código de `app/`, `docs/source-of-truth/` y los contratos vigentes del backend.
- Los trabajos pesados se ejecutan en terminal/local harness. El 17 de agosto de 2026 se ejecutaron fixtures, auditoría de scope, unit tests, lint, ktlint y `bundleRelease` con resultado PASS; el AAB generado fue técnicamente válido pero **unsigned**, por lo que aún no es publicable.
- Linear usa **KAK-46** como tracker maestro de publicación. Los bloqueos independientes viven en KAK-44, KAK-45 y KAK-47 a KAK-51.
- Las antiguas Entregas/VAI pueden conservarse como historia, pero no vuelven a ser backlog actual.

## Mapeo Linear vigente

- **KAK-46** — tracker maestro de pendientes de publicación Android.
- **KAK-44** — completar datos legales y publicar Política de Privacidad.
- **KAK-45** — **resuelto**: `VAIINILLA_API_BASE_URL` de producción configurada y verificada en GitHub.
- **KAK-47** — publicar recurso web externo para eliminar cuenta.
- **KAK-48** — validar E2E real de eliminación de cuenta.
- **KAK-49** — confirmar configuración, retención y restricciones de Firebase.
- **KAK-50** — definir retención de logs, auditoría y backups del backend.
- **KAK-51** — configurar material y secretos de signing Android de producción.

Los gates Gradle/lint/ktlint/bundle ya fueron ejecutados en terminal/local harness. Lo pendiente de release técnico es principalmente signing oficial y la posterior verificación de un AAB firmado/publicable.

## Requisitos Play ya cubiertos técnicamente

- `applicationId`: `com.vaiinilla.app`.
- `targetSdk`: 36 y `compileSdk`: 36. Google Play exigirá API 36 para nuevas apps y actualizaciones móviles a partir del 31 de agosto de 2026, por lo que el proyecto ya está configurado al nivel requerido.
- El cliente Android usa Firebase Authentication. La auditoría de dependencias actual no encontró Firebase Analytics, Crashlytics ni Messaging.
- El workflow `Android Release Readiness` ya impide construir release sin `VAIINILLA_API_BASE_URL` explícita.
- El mismo workflow soporta signing opcional mediante `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`.

Referencia operativa adicional: `docs/PLAY_STORE_RELEASE_PREP.md`.

## Avanzado y documentado

- Eliminación de cuenta implementada en Android: reautenticación por contraseña, Firebase ID token reciente, `DELETE identidad/cuenta`, idempotencia y limpieza local después de éxito confirmado.
- Backend actual elimina la identidad Firebase, revoca accesos, anonimiza datos personales y conserva registros contables/auditables bajo identidad anónima cuando corresponde.
- La migración backend `20260817010000_26_eliminacion_cuenta.sql` anonimiza snapshots identificativos, correo destinatario y actor de auditoría; no borra los registros transaccionales/legales que deben conservar integridad.
- Proveedores técnicos identificados: Railway, Supabase/PostgreSQL, Supabase Storage, Firebase/Google y Resend.
- Logging Android revisado: método, path y status HTTP; multipart añade tamaño en bytes. No se registran cuerpos de respuesta ni `Authorization` en el cliente HTTP actual.
- Firebase Android confirmado contra `google-services.json`: proyecto `vaiinilla-b3a70`, paquete `com.vaiinilla.app`. La presencia del bucket en la configuración no implica por sí sola uso de Firebase Storage; no existe dependencia de Firebase Storage en `app/build.gradle.kts`.

## KAK-45 — endpoint de producción confirmado y configurado

Verificado desde Railway CLI y HTTP el 17 de agosto de 2026:

- proyecto Railway: `vainiilla-pruebas`;
- environment: `production`;
- servicio: `vaiinilla_back`;
- source: `saul1217/vaiinilla_back` / `main`;
- deployment SHA: `5d16aa171cfb8a489f7eb73e73f7f45fe2480fef`;
- región: `us-east4-eqdc4a` / US East;
- réplicas: `1`;
- dominio público: `https://vaiinillaback-development-3f6c.up.railway.app`;
- custom domains: ninguno;
- healthcheck: `/health`, timeout 30 s;
- start command: `npm start`;
- restart: `ON_FAILURE`, máximo 3 reintentos.

Aunque el hostname contiene `development`, Railway lo asigna inequívocamente al environment **production** del servicio vigente. La comprobación HTTP dio:

- `GET /health` → `200`;
- `GET /api/v1/` → `200`, `api: vaiinilla`, `version: v1`.

La Repository Variable quedó configurada y re-leída con coincidencia exacta:

```text
VAIINILLA_API_BASE_URL=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

Por tanto KAK-45 ya no es un bloqueo.

## KAK-47 — recurso web externo de eliminación

Google Play exige que una app que permite crear cuentas ofrezca una vía detectable de eliminación dentro de la app y también un recurso web externo.

Estado actual:

- el flujo dentro de Android existe;
- todavía no existe una URL web pública confirmada para iniciar la solicitud fuera de la app;
- el repositorio esperado `saul1217/vaiinilla-web` devuelve 404 con la conexión GitHub disponible, por lo que no puede editarse desde este agente actualmente;
- cuando tengamos acceso al repo web, la página debe reutilizar el flujo de identidad/backend y no exponer Firebase Admin, service accounts ni secretos en navegador.

## KAK-48 — E2E de eliminación bloqueado sólo por cuenta descartable

El endpoint de producción ya está demostrado y el commit backend desplegado contiene `DELETE /api/v1/identidad/cuenta`. No se ejecutó ningún `DELETE` porque no existe todavía una cuenta Firebase de producción inequívocamente descartable con credenciales y una forma segura de comprobar la anonimización.

El siguiente E2E debe usar exclusivamente una cuenta creada para prueba; no usar cuentas personales o reales.

## KAK-49 — auditoría Firebase parcialmente avanzada

Confirmado desde el código Android:

- proyecto Firebase: `vaiinilla-b3a70`;
- paquete Android: `com.vaiinilla.app`;
- dependencia Firebase usada por la app: Authentication;
- no se encontraron dependencias de Analytics, Crashlytics, Messaging ni Firebase Storage en la configuración Gradle actual;
- existe una API key cliente en `google-services.json`, como es normal en Firebase Android.

Sigue requiriendo acceso a Firebase/Google Cloud Console para verificar:

- proyecto exacto que se considera producción;
- restricciones reales de la API key;
- servicios habilitados en consola;
- configuración/retención aplicable de Authentication;
- regiones/ubicación de procesamiento que deban declararse.

No rotar ni restringir servicios a ciegas sin revisar consola y dependencias reales.

## KAK-50 — retención backend parcialmente resuelta

Confirmado desde Railway production:

- backend único `vaiinilla_back`;
- sin volúmenes Railway;
- logs y métricas son consultables, pero Railway CLI no expone el periodo de retención;
- no se expusieron backups/snapshots desde Railway.

Confirmado desde código backend:

- Firebase se elimina primero en el flujo de eliminación;
- perfil, identificadores, datos visibles de pedidos, invitaciones y snapshots auditables se anonimizan;
- pedidos, pagos, movimientos, wallet y aceptaciones legales se conservan por integridad;
- `limites_tasa` mayores a 48 horas se limpian mediante `pg_cron`.

Sigue sin estar confirmado:

- retención real de logs Railway;
- backups, snapshots y regiones de Supabase;
- retención Firebase/Resend;
- confirmación externa de migraciones aplicadas en Supabase.

No inventar plazos legales ni técnicos.

## KAK-51 — production signing completado

KAK-51 quedó resuelto en terminal/local harness. No existía una upload key oficial previa ni evidencia de publicación anterior de `com.vaiinilla.app`; el `debug.keystore` histórico bajo `expo/` no se reutilizó.

Se creó una upload key oficial fuera del repositorio con alias `vaiinilla-upload`, RSA 4096 / SHA256withRSA, fingerprint público SHA-256 `4F:93:81:4F:0B:67:68:12:22:23:08:70:9B:53:05:4E:70:CA:86:DD:0C:A1:E0:D0:DE:39:8C:86:9E:96:D8:7A`, custodia primaria y recovery copy con permisos restrictivos. Las contraseñas no se versionaron ni se expusieron.

GitHub Secrets configurados:

- `ANDROID_KEYSTORE_BASE64`;
- `ANDROID_KEYSTORE_PASSWORD`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEY_PASSWORD`.

La verificación local produjo un AAB release firmado y `jarsigner` confirmó la firma y coincidencia del certificado. El workflow `Android Release Readiness` también terminó correctamente en el run `32089291183` y su artifact verificó `jar verified` con el mismo fingerprint.

El commit `7bb1061af36c778eddf7cde1fc79e8b49be2397f` añadió defaults seguros de versión (`0.5.0` / `16`) cuando las variables opcionales llegan vacías y reforzó `.gitignore` para `*.jks` / `*.keystore`.

Desde la perspectiva de signing, el AAB ya está listo para ser usado en el proceso de Play Console. No se publicó nada en Google Play durante esta verificación.

## Política de privacidad publicable

Todavía faltan datos que no pueden inferirse del código:

- responsable legal / razón social y nombre comercial si aplica;
- correo oficial de privacidad/soporte y sitio web legal;
- jurisdicción y domicilio si corresponde;
- plazos de retención para identidad, pedidos, pagos/saldo, aceptaciones legales, imágenes, logs y backups;
- regiones exactas de procesamiento y validación contractual de proveedores;
- público objetivo / edad mínima;
- confirmación formal sobre venta o compartición de datos con anunciantes.

La política pública deberá estar en una URL activa, accesible globalmente y no ser un PDF.

## Verificación realizada en terminal/local harness

Verificación técnica previa contra checkout limpio `5e5a9aaffcc2508be52510597c4ed338b4ef6000`:

- `python3 scripts/validate_fixtures.py` — PASS;
- `./scripts/audit_release_scope.sh` — PASS;
- `./gradlew --no-daemon testDebugUnitTest` — PASS;
- `./gradlew --no-daemon lintDebug` — PASS;
- `./gradlew --no-daemon ktlintCheck` — PASS;
- `./gradlew --no-daemon bundleRelease` — PASS.

Después de completar KAK-51, se volvió a generar `bundleRelease` con production signing y la firma se verificó con `jarsigner`. El certificado del AAB coincide con la upload key oficial.

El workflow `Android Release Readiness` se ejecutó con production URL + signing y terminó exitosamente:

- run: `32089291183`;
- fixtures — PASS;
- scope audit — PASS;
- unit tests — PASS;
- Android lint — PASS;
- ktlint — PASS;
- release bundle — PASS;
- upload release AAB — PASS;
- artifact CI — `jar verified`.

Commit de soporte: `7bb1061af36c778eddf7cde1fc79e8b49be2397f`.

Pendiente de terminal posterior: E2E de eliminación con una cuenta Firebase de producción inequívocamente descartable.

## Fuera de este estado por ahora

- Las antiguas “entregas” e IDs de tareas no se usan para decidir qué falta hoy.
- Stripe/tarjetas no están habilitados actualmente en Android. Si fueran requisito para la primera publicación, debe ser una decisión explícita de producto.
- La preparación detallada de formularios Google Play/App Content/Data Safety sigue aparcada hasta que se retome explícitamente; este documento sólo mantiene los prerrequisitos técnicos y legales necesarios para que esa etapa no empiece con información falsa.
