# Android release readiness — estado actual

Fecha de revisión: 2026-08-17

Este documento resume únicamente los pendientes vigentes para preparar una publicación Android. No usa IDs de tareas históricas, "entregas" anteriores ni estados de Notion como fuente de verdad para el release actual. Esos materiales pueden conservar valor histórico, pero no definen el alcance presente.

## Regla de trabajo actual

- La fuente técnica del cliente es el código de `app/`, `docs/source-of-truth/` y los contratos vigentes del backend.
- Los trabajos de build/Gradle, lint, ktlint, bundle y pruebas pesadas se ejecutan en terminal/local harness; no se consideran pendientes ejecutables desde esta sesión.
- Linear usa **KAK-46** como tracker único de publicación. Los bloqueos independientes viven en KAK-44, KAK-45 y KAK-47 a KAK-51; las antiguas Entregas/VAI no vuelven a ser backlog actual.

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

## Avanzado y documentado

- Eliminación de cuenta implementada en Android: reautenticación por contraseña, Firebase ID token reciente, `DELETE identidad/cuenta`, idempotencia y limpieza local después de HTTP 200.
- Backend actual documenta la eliminación de identidad Firebase, revocación de accesos, anonimización y conservación de registros contables/auditables bajo un identificador anónimo.
- Proveedores técnicos actuales identificados para privacidad/data map: Railway, Supabase/PostgreSQL, Supabase Storage, Firebase/Google y Resend.
- Logging Android revisado: método, path y status HTTP; multipart añade tamaño en bytes. No se registran cuerpos de respuesta ni `Authorization` en el cliente HTTP actual.
- `docs/DATA_MAP.md` y `docs/PRIVACY_POLICY_DRAFT.md` reflejan este estado.

## Bloqueados por información externa

### URL pública del backend de producción

`VAIINILLA_API_BASE_URL` sigue sin poder configurarse hasta recibir/verificar el dominio público del servicio Railway de producción. Seguimiento: **KAK-45**. No se debe reutilizar ni asumir la URL de development.

### Política de privacidad publicable

Todavía faltan datos que no pueden inferirse del código:

- responsable legal / razón social y nombre comercial si aplica;
- correo oficial de privacidad/soporte y sitio web legal;
- jurisdicción y domicilio si corresponde;
- plazos de retención para identidad, pedidos, pagos/saldo, aceptaciones legales, imágenes, logs y backups;
- regiones exactas de procesamiento y validación contractual de proveedores;
- público objetivo / edad mínima;
- confirmación formal sobre venta o compartición de datos con anunciantes.

### Recurso web externo de eliminación

El flujo dentro de Android existe, pero aún falta una URL web pública desde la que una persona pueda iniciar la eliminación de su cuenta fuera de la app. Seguimiento: **KAK-47**. La implementación debe reutilizar el backend y no exponer Firebase Admin ni secretos en navegador.

## Pendientes técnicos que no requieren rediseño

- **KAK-48:** ejecutar una eliminación E2E con cuenta descartable contra el backend real: reautenticación → DELETE → eliminación Firebase → anonimización/revocación → cleanup Android.
- **KAK-49:** confirmar en Firebase Console la retención/configuración propia del proveedor y revisar restricciones de la API key cliente.
- **KAK-50:** confirmar retención de logs, auditoría y backups del backend.
- **KAK-51:** confirmar material/configuración definitiva de signing cuando se prepare el release firmado.

## Diferidos a terminal/local harness

Cuando estén disponibles el endpoint de producción y la configuración necesaria:

- ejecutar las verificaciones Gradle/lint/ktlint correspondientes;
- ejecutar `Android Release Readiness`;
- generar/inspeccionar el AAB release;
- verificar signing si se proporciona la llave definitiva.

Estos pasos se dejan deliberadamente fuera de esta sesión para no bloquear el trabajo documental y de configuración.

## Fuera de este estado de release

- Las antiguas "entregas" e IDs de tareas no se usan para decidir qué falta hoy.
- Stripe/tarjetas no están habilitados actualmente en Android. Si son requisito para la primera publicación será una decisión explícita de producto; no se infiere de documentación histórica.
- La preparación de formularios de Google Play/App Content permanece aparcada hasta que se decida retomarla explícitamente.
