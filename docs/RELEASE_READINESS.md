# Android release readiness — estado actual

Fecha de revisión: 2026-08-17

Este documento resume únicamente los pendientes vigentes para preparar una publicación Android. No usa IDs de tareas históricas, “entregas” anteriores ni estados viejos de Notion como fuente de verdad para el release actual.

## Regla de trabajo actual

- La fuente técnica del cliente es el código de `app/`, `docs/source-of-truth/` y los contratos vigentes del backend.
- Los trabajos de build/Gradle, lint, ktlint, bundle y pruebas pesadas se ejecutan en terminal/local harness; no se ejecutan desde esta sesión.
- Linear usa **KAK-46** como tracker maestro de publicación. Los bloqueos independientes viven en KAK-44, KAK-45 y KAK-47 a KAK-51.
- Las antiguas Entregas/VAI pueden conservarse como historia, pero no vuelven a ser backlog actual.

## Mapeo Linear vigente

- **KAK-46** — tracker maestro de pendientes de publicación Android.
- **KAK-44** — completar datos legales y publicar Política de Privacidad.
- **KAK-45** — configurar `VAIINILLA_API_BASE_URL` de producción en GitHub.
- **KAK-47** — publicar recurso web externo para eliminar cuenta.
- **KAK-48** — validar E2E real de eliminación de cuenta.
- **KAK-49** — confirmar configuración, retención y restricciones de Firebase.
- **KAK-50** — definir retención de logs, auditoría y backups del backend.
- **KAK-51** — configurar material y secretos de signing Android de producción.

Los pasos de Gradle/lint/ktlint/AAB siguen diferidos a terminal/local harness y no tienen un issue de ejecución separado en este corte.

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

## KAK-45 — endpoint de producción todavía NO confirmado

Notion registraba `app.vaiinilla.app` como despliegue, pero una comprobación manual en navegador el 17 de agosto de 2026 mostró que `https://app.vaiinilla.app/health` devuelve la página web branded de Vaiinilla con **Error 404 / “Esta ruta no existe”**, no el health check del backend.

El backend de Saúl define `GET /health` y negocio bajo `/api/v1/...`, así que **no se debe configurar** `VAIINILLA_API_BASE_URL=https://app.vaiinilla.app/api/v1/` hasta demostrar que ese host realmente enruta la API o hasta obtener el dominio Railway de producción correcto.

El browser automation conectado estuvo temporalmente deshabilitado, por lo que no se pudo ampliar la comprobación automática desde esta sesión. La evidencia manual es suficiente para rechazar `app.vaiinilla.app` como URL confirmada por ahora.

## KAK-47 — recurso web externo de eliminación

Google Play exige que una app que permite crear cuentas ofrezca una vía detectable de eliminación dentro de la app y también un recurso web externo.

Estado actual:

- el flujo dentro de Android existe;
- todavía no existe una URL web pública confirmada para iniciar la solicitud fuera de la app;
- el repositorio esperado `saul1217/vaiinilla-web` devuelve 404 con la conexión GitHub disponible, por lo que no puede editarse desde este agente actualmente;
- cuando tengamos acceso al repo web, la página debe reutilizar el flujo de identidad/backend y no exponer Firebase Admin, service accounts ni secretos en navegador.

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

## KAK-50 — retención backend parcialmente avanzada

La auditoría del repositorio backend confirmó:

- no hay una política de retención de logs/backups expresada como configuración versionada en `.env.example`;
- el servicio usa Railway, Supabase/PostgreSQL, Supabase Storage, Firebase Admin y Resend;
- la migración de eliminación conserva registros transaccionales/legales y los anonimiza cuando corresponde;
- existen tablas y campos de auditoría, pero el código no define por sí solo cuántos días/meses/años deben conservarse;
- los periodos de logs de infraestructura, snapshots/backups y regiones siguen siendo datos operativos/de proveedor que deben confirmarse fuera del repo.

No inventar plazos legales ni técnicos.

## KAK-51 — signing preparado en código, material aún pendiente

El workflow actual está listo para recibir una clave de subida/keystore sin versionarla. Antes de cerrar KAK-51 hay que decidir si ya existe una clave de subida válida o generar una nueva en terminal, custodiarla y configurar los cuatro GitHub Secrets esperados.

Para Google Play se recomienda usar Play App Signing y conservar de forma segura la **upload key** del desarrollador; la clave de firma de aplicación puede gestionarla Google Play.

No se ha generado ni subido ningún keystore desde esta sesión.

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

## Diferidos a terminal/local harness

Cuando estén disponibles el endpoint de producción y la configuración necesaria:

- ejecutar KAK-48 con una cuenta descartable contra servicios reales;
- preparar/generar la upload key si no existe y configurar signing de KAK-51;
- ejecutar las verificaciones Gradle/lint/ktlint correspondientes;
- ejecutar `Android Release Readiness`;
- generar/inspeccionar el AAB release;
- verificar signing del artefacto.

## Fuera de este estado por ahora

- Las antiguas “entregas” e IDs de tareas no se usan para decidir qué falta hoy.
- Stripe/tarjetas no están habilitados actualmente en Android. Si fueran requisito para la primera publicación, debe ser una decisión explícita de producto.
- La preparación detallada de formularios Google Play/App Content/Data Safety sigue aparcada hasta que se retome explícitamente; este documento sólo mantiene los prerrequisitos técnicos y legales necesarios para que esa etapa no empiece con información falsa.
