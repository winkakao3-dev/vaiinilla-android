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

Estado: **pendiente — KAK-45**.

El workflow exige una Repository Variable:

```text
VAIINILLA_API_BASE_URL=https://<backend-produccion>/api/v1/
```

No debe usarse el endpoint de development.

### Evidencia sobre `app.vaiinilla.app`

Una prueba manual del 17 de agosto de 2026 en:

```text
https://app.vaiinilla.app/health
```

devuelve la página frontend de Vaiinilla con Error 404 (“Esta ruta no existe”), no el `GET /health` del backend de Saúl. Por ello `app.vaiinilla.app` **no se considera API base confirmada**.

El backend sí define `/health` y negocio bajo `/api/v1/...`; falta el host/ruteo real de producción.

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

Estado: **auditado parcialmente — KAK-50**.

Confirmado desde `saul1217/vaiinilla_back`:

- Railway para backend;
- Supabase/PostgreSQL para datos;
- Supabase Storage para imágenes;
- Firebase Admin para identidad;
- Resend para correo;
- la eliminación de cuenta anonimiza datos identificativos y conserva integridad de registros transaccionales/legales;
- el repo no contiene una matriz/versionado de periodos de retención de logs, backups y snapshots.

Pendiente fuera del repo:

- periodos reales de logs Railway;
- política real de backups/snapshots Supabase;
- periodos aprobados para registros transaccionales/auditoría;
- regiones finales de almacenamiento/procesamiento.

## 7. Signing y Play App Signing

Estado de código: **preparado**.

Estado de material: **pendiente — KAK-51**.

GitHub Actions espera:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

El archivo de keystore nunca debe versionarse.

Google Play App Signing distingue:

- **upload key**: la conserva el desarrollador y firma el AAB que se sube;
- **app signing key**: puede custodiarla Google Play y firma los APK finales para usuarios.

Fuente oficial:
- https://support.google.com/googleplay/android-developer/answer/9842756

Si no existe todavía una upload key oficial de Vaiinilla, este punto requiere terminal/local harness para generarla y guardar una copia de recuperación segura antes de cargar secretos en GitHub.

## 8. Builds y AAB

Estado: **diferido intencionalmente a terminal/local harness**.

El workflow `Android Release Readiness` ejecuta:

- fixtures contractuales;
- auditoría de scope;
- unit tests;
- Android lint;
- ktlint;
- `bundleRelease`;
- carga del AAB como artifact.

Los runs actuales de Release Readiness fallan antes de esas tareas porque falta `VAIINILLA_API_BASE_URL`; la CI normal permanece verde.

No ejecutar builds pesados desde agentes remotos mientras terminal/local harness sea la vía más fiable.

## 9. Qué falta antes de entrar al envío real de Play Console

1. Resolver KAK-45 — API base real de producción.
2. Resolver KAK-47 — recurso web externo de eliminación.
3. Resolver KAK-48 — E2E real de eliminación.
4. Terminar KAK-49 — Firebase Console/Google Cloud.
5. Terminar KAK-50 — matriz de retención/regiones.
6. Terminar KAK-44 — política publicable y URL estable.
7. Resolver KAK-51 — upload key/signing.
8. En terminal: Release Readiness + AAB firmado/verificado.
9. Después, retomar explícitamente formularios Play Console / Data Safety / App Content con datos ya verificados.
