# Google Play — preparación técnica de Vaiinilla Android

Fecha de corte: 2026-08-17

Este documento concentra prerrequisitos comprobables para publicar `com.vaiinilla.app` en Google Play. No sustituye los formularios de Play Console ni reabre automáticamente App Content/Data Safety; sirve para llegar a esa etapa con infraestructura, privacidad y release correctamente preparados.

## 1. Compatibilidad de plataforma

Estado: **cubierto**.

- `applicationId`: `com.vaiinilla.app`.
- `compileSdk`: 36.
- `targetSdk`: 36.
- `minSdk`: 26.

Google Play exige Android 16 / API 36 para nuevas apps y actualizaciones móviles enviadas a partir del 31 de agosto de 2026. El proyecto ya está en API 36.

Fuente oficial:
- https://support.google.com/googleplay/android-developer/answer/11926878

## 2. Política de privacidad

Estado: **borrador técnico avanzado; no publicable todavía**.

Google Play exige una política de privacidad completa y accesible desde Play Console y desde la app. La URL debe ser pública, activa, accesible globalmente y no un PDF.

Repo:
- `docs/PRIVACY_POLICY_DRAFT.md`
- `docs/DATA_MAP.md`

Faltan principalmente datos legales, retenciones, regiones/configuración de proveedores, edad mínima y URL pública final.

Fuente oficial:
- https://support.google.com/googleplay/android-developer/answer/10144311

## 3. Eliminación de cuenta

Estado in-app: **implementado**.

Estado web externo: **pendiente — KAK-47**.

Google Play exige que una app que permite crear cuentas ofrezca:

1. una vía detectable dentro de la app para solicitar eliminación;
2. un recurso web externo desde el que también pueda iniciarse la solicitud;
3. eliminación de datos asociados, salvo retenciones legítimas que deben declararse claramente.

Fuentes oficiales:
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://support.google.com/googleplay/android-developer/answer/10144311

La URL externa todavía no existe/está confirmada. `saul1217/vaiinilla-web` no es accesible con la conexión GitHub actual, así que la implementación web requiere acceso del responsable o trabajo desde su repo.

## 4. Endpoint Android de producción

Estado: **resuelto — KAK-45**.

Railway CLI confirmó que el environment `production` del proyecto `vainiilla-pruebas` ejecuta el servicio `vaiinilla_back` desde `saul1217/vaiinilla_back` / `main`, deployment `5d16aa171cfb8a489f7eb73e73f7f45fe2480fef`.

Dominio production demostrado:

```text
https://vaiinillaback-development-3f6c.up.railway.app
```

Aunque el hostname contiene `development`, Railway lo asigna inequívocamente al environment `production`. Se verificó:

```text
GET /health   -> 200
GET /api/v1/  -> 200, api: vaiinilla, version: v1
```

Repository Variable configurada y re-leída con coincidencia exacta:

```text
VAIINILLA_API_BASE_URL=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

`app.vaiinilla.app` sigue siendo la superficie frontend y no la API base.

## 5. Firebase

Estado repo: **auditado parcialmente — KAK-49**.

Confirmado:

- Firebase project id: `vaiinilla-b3a70`.
- Android package: `com.vaiinilla.app`.
- Gradle incluye Firebase Authentication.
- No aparecen dependencias Firebase Analytics, Crashlytics, Messaging ni Storage en el cliente actual.

Pendiente en consola:

- confirmar que ése es el proyecto definitivo de producción;
- revisar restricciones de la API key cliente;
- comprobar servicios habilitados realmente;
- confirmar retención/configuración de Firebase Authentication;
- obtener las regiones/ubicación de procesamiento que deban declararse.

## 6. Retención backend

Estado: **parcialmente resuelto — KAK-50**.

Confirmado desde Railway production:

- backend `vaiinilla_back`, 1 réplica, región `us-east4-eqdc4a`;
- sin volúmenes Railway;
- logs/métricas disponibles, sin periodo de retención expuesto por CLI;
- Railway no expuso backups/snapshots.

Confirmado desde código:

- Firebase se elimina primero;
- datos identificativos, datos visibles de pedidos, invitaciones y snapshots auditables se anonimizan;
- pedidos, pagos, movimientos, wallet y aceptaciones legales se conservan por integridad;
- `limites_tasa` mayores a 48 horas se limpian vía `pg_cron`.

Pendiente externo:

- retención real de logs Railway;
- backups/snapshots/regiones de Supabase;
- retención Firebase/Resend;
- confirmación externa de migraciones Supabase aplicadas.

## 7. Signing y Play App Signing

Estado de código: **preparado**.

Estado de material: **bloqueado — KAK-51**.

La búsqueda local no encontró una upload key/keystore oficial. Sólo aparecieron keystores de debug, que no sirven para el release oficial.

GitHub Actions espera:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

El archivo de keystore nunca debe versionarse. `bundleRelease` ya pudo generar un AAB técnico, pero `jarsigner` confirmó que está unsigned. Antes de publicar hay que obtener/generar la upload key oficial, custodiarla, configurar los cuatro secrets y volver a verificar el AAB firmado.

## 8. Builds y AAB

Estado: **verificación técnica ejecutada en terminal**.

Checkout limpio de `main` en `5e5a9aaffcc2508be52510597c4ed338b4ef6000`:

- `python3 scripts/validate_fixtures.py` — PASS;
- `./scripts/audit_release_scope.sh` — PASS;
- `./gradlew --no-daemon testDebugUnitTest` — PASS;
- `./gradlew --no-daemon lintDebug` — PASS;
- `./gradlew --no-daemon ktlintCheck` — PASS;
- `./gradlew --no-daemon bundleRelease` — PASS.

El AAB se generó correctamente pero quedó **unsigned**, así que aún no es publicable. BUILD SUCCESS no equivale a AAB listo para Play hasta completar KAK-51 y verificar la firma.

## 9. Qué falta antes de entrar al envío real de Play Console

1. Resolver KAK-47 — recurso web externo de eliminación.
2. Resolver KAK-48 — crear/obtener una cuenta Firebase de producción explícitamente descartable y ejecutar el E2E.
3. Terminar KAK-49 — Firebase Console/Google Cloud.
4. Terminar KAK-50 — retenciones/backups/regiones externas.
5. Terminar KAK-44 — política publicable y URL estable.
6. Resolver KAK-51 — upload key/signing; volver a generar e inspeccionar AAB firmado.
7. Después, retomar explícitamente formularios Play Console / Data Safety / App Content con datos ya verificados.
