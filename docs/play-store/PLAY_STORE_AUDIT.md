# Google Play Audit — Vaiinilla

Fecha original: 2026-08-18.

Revisión de estado vigente: 2026-08-23 (`main` `b0d73fb2`).

Repositorio auditado: `winkakao3-dev/vaiinilla-android`.

Valores consolidados para Play Console: `docs/play-store/PLAY_CONSOLE_FORM_VALUES.md`.

Rama auditada: `main`.

Commit base auditado: `847fa1ea556179574474b3357b7466e505bd9e61`.

Actualización posterior: el icono se convirtió a PNG RGBA y se volvió a validar.

## Resumen

Resultado general: **NOT READY**.

> Los conteos PASS/FAIL/BLOCKED siguientes corresponden a la auditoría base del
> 18 de agosto. La revisión del 23 de agosto corrige Stripe, signing, screenshots
> y Data Safety en las secciones específicas sin reetiquetar retroactivamente
> todos los conteos históricos.

PASS: 16.

FAIL: 3.

BLOCKED: 10.

NOT APPLICABLE: 3.

No se publicó la aplicación.
No se subió el AAB a Google Play.
No se modificó Linear.
No se modificó producción.
No se modificó signing.
No se modificó Firebase.
No se modificaron variables remotas.

## 1. Android release

Estado: **PASS** para identidad y compilación.

| Campo | Resultado | Evidencia |
| --- | --- | --- |
| `applicationId` | PASS | `app/build.gradle.kts`: `com.vaiinilla.app` |
| `namespace` | PASS | `app/build.gradle.kts`: `com.vaiinilla.app` |
| `minSdk` | PASS | 26 |
| `compileSdk` | PASS | 36 |
| `targetSdk` | PASS | 36 |
| `versionCode` | PASS | 16 en el AAB local |
| `versionName` | PASS | 0.5.0 en el AAB local |
| R8 | PASS con observación | `isMinifyEnabled = true`; `isShrinkResources = true` |
| ProGuard | PASS | No hay reglas globales amplias en `app/proguard-rules.pro` |

El AAB local contiene:

- paquete: `com.vaiinilla.app`;
- `versionCode`: 16;
- `versionName`: 0.5.0;
- `minSdkVersion`: 26;
- `targetSdkVersion`: 36;
- `compileSdkVersion`: 36.

La política oficial actual exige API 35 o superior para nuevas apps y updates
desde el 31 de agosto de 2025. La misma página indica API 36 o superior para
nuevas apps y updates desde el 31 de agosto de 2026. El proyecto usa API 36.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/11926878?hl=es>

### Validaciones ejecutadas

- `python3 scripts/validate_fixtures.py` — PASS.
- `./scripts/audit_release_scope.sh` — PASS.
- `./gradlew --no-daemon testDebugUnitTest` — PASS con SDK temporal del proceso.
- `./gradlew --no-daemon lintDebug` — PASS con SDK temporal del proceso.
- `./gradlew --no-daemon ktlintCheck` — PASS con SDK temporal del proceso.
- `./gradlew --no-daemon bundleRelease` — PASS como generación de bundle.

La primera ejecución de tests terminó FAIL porque no había `ANDROID_HOME`.
El SDK local existe en `/Users/kakao/Library/Android/sdk`.
La segunda ejecución terminó PASS.
No se creó `local.properties`.

## 2. Cuenta Play

Estado: **BLOCKED**.

La decisión de trabajo indica:

- cuenta personal/individual;
- titular: César Alejandro Loya Domínguez;
- nombre público preferido: César Loya.

No se verificó desde Play Console:

- fecha de creación de la cuenta;
- acceso actual a producción;
- estado de “Solicitar acceso a producción”;
- datos públicos del desarrollador;
- tipo final de cuenta aceptado por Google.

La aplicación contiene saldo y recargas en efectivo. Google Play exige la
declaración de funciones financieras para todas las apps publicadas, incluso
si no ofrecen funciones financieras. La clasificación probable incluye
“Mobile payments and digital wallets”, pero la decisión legal y de producto no
se debe inventar.

Fuentes:

- <https://support.google.com/googleplay/android-developer/answer/13849271?hl=en-GB>
- <https://support.google.com/googleplay/android-developer/answer/13634885?hl=en>

## 3. Store Listing

Estado: **IN PROGRESS**.

Copy y categoría documentados en `docs/play-store/STORE_LISTING.md`.

### Verificado desde el repositorio

- Nombre de aplicación: `Vaiinilla` — PASS.
- Aplicación Android, no juego — PASS por el tipo de proyecto.
- `applicationId`: `com.vaiinilla.app` — PASS.

### Cerrado en el borrador de metadata

- Categoría: `Comida y bebida` — PASS de producto; falta cargarla en Play Console.
- Descripción breve — PASS de copy, 67/80 caracteres.
- Descripción completa — PASS de copy, 1,308/4,000 caracteres.
- Nombre `Vaiinilla` — PASS.

### Pendiente externo / Play Console

- Locale exacto del idioma predeterminado — BLOCKED.
- Tags exactos ofrecidos por Play Console — BLOCKED.
- Correo del desarrollador/soporte — BLOCKED.
- Teléfono — BLOCKED cuando corresponda.
- Sitio web — BLOCKED.
- URL de política de privacidad pública — BLOCKED.
- URL web de eliminación de cuenta — FAIL.
- Seis screenshots finales — generados externamente, pendientes de entrega/validación.

Metadata de trabajo: `docs/play-store/STORE_LISTING.md`.

## 4. Recursos gráficos

Estado: **FAIL**.

### Icono Play

Ruta:
`docs/play-store/assets/final/vaiinilla-play-icon-512.png`

- Resolución: 512 × 512 px — PASS.
- Archivo real: PNG — PASS.
- Peso: 25,523 bytes — PASS.
- Tamaño máximo de 1024 KB — PASS.
- Símbolo centrado — PASS visual.
- Símbolo sin recorte — PASS visual.
- Pixelación visible — PASS visual.
- Alpha — PASS.

El archivo es PNG RGBA de 8 bits por canal, con canal alpha.
La documentación oficial exige PNG de 32 bits con alpha para el icono de Play.
La validación técnica actual cumple este requisito.

### Feature Graphic

Ruta:
`docs/play-store/assets/final/vaiinilla-feature-graphic-1024x500.jpg`

- Resolución: 1024 × 500 px — PASS.
- Archivo real: JPEG — PASS.
- Sin alpha — PASS.
- Peso: 36,150 bytes — PASS.
- Logo completo visible — PASS visual.
- Wordmark completo visible — PASS visual.
- Pixelación visible — PASS visual.
- Texto promocional adicional — PASS; no existe.
- Mockup de teléfono — PASS; no existe.

### Screenshots

Estado: **FAIL**.

No existen screenshots finales en `docs/play-store/assets/`.
Google Play exige al menos dos screenshots para publicar el store listing.
La checklist interna conserva seis screenshots pendientes.

Los drafts actuales se abrieron y se confirmaron inválidos:

- `drafts/vaiinilla-play-icon-512-draft.jpg` — FAIL visual.
- `drafts/vaiinilla-feature-graphic-1024x500-draft.jpg` — FAIL visual.

Los drafts se conservaron.
Los drafts no se usaron como fuente.

Fuente oficial de assets:
<https://support.google.com/googleplay/android-developer/answer/9866151?hl=en>

## 5. App Access

Estado: **IN PROGRESS**.

Instrucciones no secretas preparadas en `docs/play-store/APP_ACCESS_REVIEW.md`.

El cliente contiene:

- registro con correo y contraseña;
- login con correo y contraseña;
- verificación de correo;
- roles operativos;
- rutas protegidas para cuenta, pedidos, saldo y operación.

No existe evidencia local de credenciales de reviewer configuradas en Play
Console.
No se guardaron credenciales en el repositorio.
No se guardaron contraseñas en este reporte.

Avance:

- [x] documentar flujo de cliente y staff sin guardar passwords en Git;
- [x] definir que Play debe marcar toda o parte de la funcionalidad como restringida;
- [x] preparar instrucciones en inglés para Play Console;
- [ ] crear o confirmar una cuenta reviewer cliente, reutilizable y verificada;
- [ ] crear/confirmar cuenta reviewer staff si los modos operativos entran al release público;
- [ ] probar las cuentas sobre el mismo build candidato;
- [ ] cargar credenciales exclusivamente en Play Console.

## 6. Ads

Estado: **PASS**.

No se encontró un SDK de anuncios.
No se encontró Firebase Analytics.
No se encontró Crashlytics.
No se encontró Firebase Messaging.
No se encontró Firebase Storage como dependencia del cliente.
Stripe Android `23.13.1` está presente, pero no es un SDK de anuncios.

El permiso `com.google.android.gms.permission.AD_ID` no aparece en el
manifiesto final del AAB.

No se encontró uso de Advertising ID para publicidad.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/6048248?hl=en>

## 7. Data Safety

Estado: **BLOCKED**.

Google Play exige una declaración completa y exacta.
La declaración debe incluir datos transmitidos por SDKs y bibliotecas.
El formulario también debe indicar si existe un mecanismo de eliminación.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>

La tabla operativa actualizada, incluyendo Stripe, está en
`docs/play-store/DATA_SAFETY_FORM.md`. La tabla histórica siguiente se conserva
como evidencia de la auditoría inicial, pero **no debe usarse para enviar el
formulario actual sin aplicar la actualización Stripe**.

### Tabla histórica de datos

| Tipo de dato | Collected | Shared | Required/Optional | Finalidad | Proveedor | Retención | Eliminación | Evidencia |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Nombre | Sí | Backend; service provider por confirmar | Requerido para registro operativo | Gestión de cuenta y operación | Firebase / backend | Pendiente | Eliminación de cuenta y anonimización; E2E pendiente | `FirebaseStudentAuthRepository.kt`, `RemoteStudentEnrollmentApi.kt` |
| Correo | Sí | Firebase y backend; clasificación service provider pendiente | Requerido para cuenta | Gestión de cuenta, verificación y acceso | Firebase / backend / Resend | Pendiente | Eliminación de cuenta y anonimización | `FirebaseStudentAuthRepository.kt`, `RemoteAuthorizedAccessRepository.kt` |
| User ID | Sí | Backend | Requerido para identidad | Gestión de cuenta y autorización | Firebase / Railway / Supabase | Pendiente | Anonimización y limpieza | `StudentAuthSession.kt`, `RemoteAccountDeletionRepository.kt` |
| Historial de compras | Sí | Backend y roles operativos autorizados | Requerido para pedidos | Funcionalidad de pedidos | Railway / Supabase | Plazo legal pendiente | Conservación o anonimización según contrato | `RemoteOrderRepository.kt` |
| Saldo y movimientos | Sí | Backend y operación de Caja | Requerido para wallet | Funcionalidad de saldo | Railway / Supabase | Plazo legal pendiente | Conservación contable y anonimización | `RemoteWalletRepository.kt` |
| Notas de pedido | Sí | Backend y roles operativos autorizados | Opcional en checkout | Procesar pedido | Railway / Supabase | Pendiente | Pendiente de contrato | `OrderContractDtos.kt`, `RemoteOrderRepository.kt` |
| Fotos de producto | Sí | Backend y almacenamiento de catálogo | Opcional para el rol operativo | Catálogo | Railway / Supabase Storage | Pendiente | Pendiente de contrato | `CashierCatalogPanel.kt`, `RemoteCatalogRepository.kt` |
| Device or other IDs | Sí | Backend durante presencia operativa | Opcional según rol | Presencia operativa y seguridad | Railway / backend | Pendiente | Pendiente de contrato | `AndroidDeviceIdentity.kt`, `OperationalHeartbeatCoordinator.kt` |
| Ubicación | No demostrada | No demostrada | Not applicable | No hay permiso ni API de ubicación | — | — | — | Manifiesto final sin permisos de ubicación |
| Salud | No demostrada | No demostrada | Not applicable | No hay SDK ni funcionalidad de salud | — | — | — | Dependencias y código inspeccionados |
| Datos publicitarios | No demostrados | No demostrados | Not applicable | No hay SDK de anuncios ni `AD_ID` | — | — | — | Manifiesto final y dependencias |

### Observaciones

- Firebase Authentication es un proveedor externo visible en el AAB.
- CameraX y ML Kit están presentes para cámara y lectura de QR.
- La subida de imágenes de producto es iniciada por una acción operativa.
- El selector de compartir puede enviar un receipt a otra aplicación.
- `ANDROID_ID` se usa en presencia operativa.
- La retención exacta no está definida.
- Las regiones y subprocesadores no están confirmados.

No se puede marcar `Shared = No` sólo porque el proveedor sea externo.
La excepción de service provider requiere confirmación contractual.

## 8. Account deletion

Estado: **FAIL** para la superficie externa. **PASS** para la superficie in-app.

### In-app

La eliminación in-app está implementada:

- reautenticación por contraseña;
- ID token Firebase renovado;
- `DELETE identidad/cuenta`;
- `Idempotency-Key`;
- limpieza local después de HTTP 200.

Evidencia:

- `AccountDeletionViewModel.kt`;
- `RemoteAccountDeletionRepository.kt`;
- `FirebaseStudentAuthRepository.kt`.

### Web externa

No existe una URL web externa confirmada.
La cuenta GitHub actual no tiene acceso confirmado a `saul1217/vaiinilla-web`.
La política oficial exige un enlace web funcional y descubrible.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/13327111?hl=en>

### E2E

Estado: **BLOCKED**.

Falta una cuenta Firebase de producción creada sólo para pruebas.
No se ejecutó un `DELETE` contra una cuenta personal.

## 9. IARC

Estado: **IN PROGRESS**.

La auditoría de contenido y las respuestas técnicas están preparadas en
`docs/play-store/IARC_CONTENT_RATING.md`.

No se detectan violencia, sexo, drogas, alcohol, apuestas, lenguaje ofensivo,
chat usuario-a-usuario ni geolocalización. Las notas de cocina son privadas y el
asistente es local, no social. La clasificación exacta no se inventa: quedará
pendiente hasta que IARC la emita desde Play Console.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/9898843?hl=en>

## 10. Target audience

Estado: **IN PROGRESS — decisión de producto cerrada**.

Vaiinilla se dirige **desde secundaria en adelante**. En México la edad típica
de secundaria es 12-14 años. Como Play no ofrece un bucket `12+`, la selección
de trabajo será `9-12`, `13-15`, `16-17` y `18 and over`. No se seleccionan
`5 and under` ni `6-8`.

Seleccionar `9-12` activa las obligaciones de Families para una audiencia que
incluye menores. La app no tiene anuncios/AD_ID, pero quedan por cerrar la base
legal/consentimiento de datos de menores y la validación de proveedores/SDKs.

Detalle: `docs/play-store/TARGET_AUDIENCE.md`. Preflight Families: `docs/play-store/FAMILIES_COMPLIANCE.md`.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/9867159?hl=en>

## 11. Financial features

Estado: **BLOCKED**.

La aplicación implementa:

- saldo por establecimiento;
- movimientos;
- recarga en efectivo por Caja;
- pago de pedidos con saldo.

Implementa además checkout con `Tarjeta` mediante Stripe Android 23.13.1 +
PaymentSheet, actualmente cerrado a `pk_test_...` (Test Mode).

No implementa actualmente:

- préstamos;
- BNPL;
- criptomonedas;
- inversión;
- seguros.

Google Play incluye “Mobile payments and digital wallets” en la declaración de
funciones financieras y esa selección es aplicable al alcance actual. Además,
el cliente expone cashback/recompensas, por lo que debe revisarse si corresponde
marcar “Rewards, points, frequent flier miles, and other incentives” en el
release público.

La cuenta personal también requiere revisión porque Google indica que las
cuentas de organización son las recomendadas para servicios financieros.

Fuentes:

- <https://support.google.com/googleplay/android-developer/answer/13849271?hl=en-GB>
- <https://support.google.com/googleplay/android-developer/answer/13634885?hl=en>

## 12. Government apps

Estado: **NOT APPLICABLE** según la evidencia del repositorio.

No se encontró una función gubernamental ni una dependencia de una agencia.

## 13. Health

Estado: **NOT APPLICABLE** según la evidencia del repositorio.

No se encontró una función médica, clínica o de investigación en humanos.

## 14. Advertising ID

Estado: **PASS**.

- No existe `AD_ID` en el manifiesto final.
- No existe SDK de anuncios.
- No existe Analytics o Crashlytics en las dependencias directas.
- No se encontró uso de identificadores para publicidad.

No se debe completar la declaración de Advertising ID como “sí” sin evidencia
de un SDK o una ruta de publicidad.

## 15. Testing y acceso a producción

Estado: **BLOCKED**.

La documentación oficial actual indica que las cuentas personales creadas
después del 13 de noviembre de 2023 necesitan un closed test con al menos 12
testers activos durante 14 días continuos antes de solicitar acceso a
producción.

No se verificó:

- fecha de creación de la cuenta;
- cantidad actual de testers;
- continuidad de 14 días;
- acceso a producción;
- formulario de solicitud de producción.

La skill comunitaria `play-store-submission` menciona 20 testers. Esa regla no
coincide con la documentación oficial actual. Se usa el valor oficial de 12.

Fuente:
<https://support.google.com/googleplay/android-developer/answer/14151465?hl=en>

## 16. Diferencias contra Linear

La Activity maestra es `KAK-46` dentro de `Vaiinilla — Manual técnico y
operación`.

| Punto | Linear | Auditoría | Estado | Acción |
| --- | --- | --- | --- | --- |
| Target API | `targetSdk` 36 documentado | AAB final con target 36 | PASS | Sin acción técnica |
| Icono Play | PASS | 512 × 512, 28,220 bytes, PNG RGBA con alpha | PASS | Sin acción adicional en esta auditoría |
| Feature Graphic | PASS | 1024 × 500 JPEG sin alpha | PASS | Sin acción técnica |
| Screenshots | 6 generados externamente, aún no entregados | Pendientes de recepción/validación e integración en repo | IN PROGRESS | Recibir, validar e integrar los 6 finales |
| Signing | KAK-51 Done | AAB del `main` actual (`b0d73fb2`) verificado desde workflow `32505457217`; `jarsigner` válido | PASS | Conservar evidencia y usar el AAB final decidido para Play |
| Account deletion | KAK-47 pendiente | In-app PASS; URL externa inexistente | FAIL | Publicar recurso web externo |
| Privacy | KAK-44 pendiente | Política sólo en borrador con campos pendientes | BLOCKED | Completar datos legales y URL pública |
| Data Safety | Retomado | Matriz actualizada con Stripe en `DATA_SAFETY_FORM.md`; `Shared`/retenciones externos pendientes | IN PROGRESS | Cerrar evidencia externa y trasladar respuestas a Play Console |
| Firebase | KAK-49 pendiente | Firebase Auth visible; consola no verificada | BLOCKED | Revisar consola, restricciones y retención |
| Financial features | Pendiente Play Console | Wallet + saldo + Stripe hacen aplicable `Mobile payments and digital wallets`; cashback requiere revisar Rewards/Incentives | IN PROGRESS | Completar declaración con alcance final del release |
| R8 | No documentado en Linear | Minify y shrink PASS; flag de optimized resource shrinking no confirmado | BLOCKED | Revisar la configuración antes de cerrar release |

Linear no se modificó.

## 17. Bloqueos externos

- Acceso a Play Console.
- Fecha de creación de la cuenta personal.
- Estado de acceso a producción.
- Cuenta y credentials de reviewer.
- Política de privacidad pública: RESUELTO (`https://app.vaiinilla.app/legal/privacidad/2026-07`).
- Datos legales y retenciones.
- Regiones y subprocesadores.
- Cuenta Firebase descartable para E2E.
- URL web externa de eliminación: RESUELTO (`https://app.vaiinilla.app/eliminar-cuenta`).
- Clasificación financiera del saldo y recargas.
- Screenshots para la ficha de la tienda: pendientes (screenshots automáticos anteriores descartados; a la espera de nuevos screenshots finales).
- Cerrar `Shared`/retenciones/proveedores en Data Safety.
- E2E Stripe Test Mode y decisión Test Mode vs Live Mode.

## 18. Orden recomendado

1. Confirmar el tipo de cuenta Play y el alcance financiero del saldo.
2. Confirmar acceso a Play Console y la fecha de creación de la cuenta.
3. Conservar el icono PNG RGBA validado.
4. Recibir, validar e integrar los seis screenshots finales ya generados.
5. Cerrar la matriz Data Safety/Stripe con evidencia externa y completar Financial features.
6. Completar la política de privacidad pública.
7. Publicar el recurso web externo de eliminación.
8. Crear una cuenta Firebase descartable y ejecutar el E2E.
9. Ejecutar E2E Stripe Test Mode y decidir Test Mode vs Live Mode.
10. Trasladar a Play Console IARC, Target Audience y App Access ya documentados; obtener la clasificación IARC emitida.
11. Preparar credentials de reviewer sin guardar passwords en Git o Linear.
12. Revisar R8/compatibilidad final del AAB exacto que se subirá.
13. Ejecutar el flujo de testing requerido por Play Console.

## Skills usadas

Skills cargadas desde los repositorios indicados:

- Android `play-policy-insights` — auditoría estática de permisos, cuenta y Data Safety.
- Android `r8-analyzer` — revisión de R8 y reglas ProGuard.
- Android `testing-setup` — revisión de pruebas unitarias, UI y screenshot tests.
- `play-store-submission` — checklist comunitaria de Play Store.
- `gplay-preflight` — revisión del alcance de preflight offline.
- `gplay-submission-checks` — revisión del alcance de validaciones previas.
- `gplay-metadata-sync` — revisión de estructura de metadata.
- `gplay-screenshot-automation` — revisión de estructura y límites de imágenes.
- `gplay-release-flow` — revisión del alcance de release y rollout.

No se instaló un skill de forma permanente.
No se instaló `gplay` CLI.
No se autenticó Play Console.
No se ejecutó ningún comando de escritura de Play Console.

## Reglas de skills desactualizadas

- `play-store-submission` indica 20 testers. La documentación oficial actual indica 12 testers durante 14 días continuos para el caso aplicable.
- `play-store-submission` contiene ejemplos de target API inferiores. La documentación oficial actual exige API 35 para el periodo vigente y API 36 desde el 31 de agosto de 2026 para nuevas apps y updates.
- Las skills comunitarias no sustituyen la clasificación oficial de funciones financieras, Data Safety, testing o cuenta de desarrollador.
- La skill oficial `play-policy-insights` no resolvió `PACKAGE_NAME` ni `TARGET_SDK` durante su inicialización. Se usó la evidencia del AAB y la configuración real como fuente principal.

## Fuentes oficiales

- Target API: <https://support.google.com/googleplay/android-developer/answer/11926878?hl=es>
- Testing de cuentas personales: <https://support.google.com/googleplay/android-developer/answer/14151465?hl=en>
- Assets de preview: <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en>
- Account deletion: <https://support.google.com/googleplay/android-developer/answer/13327111?hl=en>
- Data Safety: <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>
- IARC: <https://support.google.com/googleplay/android-developer/answer/9898843?hl=en>
- Target audience: <https://support.google.com/googleplay/android-developer/answer/9859655?hl=en>
- Financial features: <https://support.google.com/googleplay/android-developer/answer/13849271?hl=en-GB>
- Developer account type: <https://support.google.com/googleplay/android-developer/answer/13634885?hl=en>
- Advertising ID: <https://support.google.com/googleplay/android-developer/answer/6048248?hl=en>
