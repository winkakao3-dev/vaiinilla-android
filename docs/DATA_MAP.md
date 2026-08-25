# Mapa real de datos de Vaiinilla Android

Fecha de revisión: 2026-08-23

Este documento describe lo que el cliente Android realmente lee, genera,
guarda localmente y envía según `app/src/main/java/`. No sustituye la revisión
de Firebase, del backend ni de los proveedores de infraestructura. Para Play
Data Safety y Apple App Privacy, cada afirmación de “actual” debe confirmarse
con las configuraciones y políticas de esos servicios.

## Resumen de superficies

| Superficie | Estado en Android | Datos observables | Destino o persistencia |
| --- | --- | --- | --- |
| Firebase Auth | Actual | correo, contraseña durante autenticación, UID, nombre de perfil, estado de correo verificado, tokens de Firebase | Firebase Auth; el ID token se presenta al backend |
| Identidad y consentimiento | Actual | nombre, versión de términos, versión de privacidad, identificador contextual cuando el establecimiento lo exige | Backend en `identidad/alta` y `sesiones/contexto-cliente`; versiones legales se consultan desde backend |
| Establecimiento y espacio | Actual | búsqueda, `id`, nombre, slug, etiqueta/requerimiento de identificador, espacio/mesa y token opaco de resolución | Backend público; contexto seleccionado en preferencias locales |
| Catálogo | Actual | productos, categorías, precios, opciones, disponibilidad e imágenes; personal autorizado puede subir imagen | Backend; imágenes se entregan mediante las URLs que devuelve la API |
| Pedidos | Actual | productos/opciones, cantidades, destino, espacio, método de pago disponible, notas a cocina, totales, estados, folio, timestamps y token de recogida | Backend; token de recogida también se guarda localmente cifrado |
| Saldo Vaiinilla | Actual | usuario, establecimiento, saldo visible, movimientos, montos, pedido relacionado, recargas y sesión de caja | Backend autenticado; no hay una cartera persistente local equivalente |
| Cámara y QR | Actual | cámara para escaneo de QR y toma de fotos de producto | El QR interpretado se envía al endpoint correspondiente; la foto seleccionada se sube al backend como multipart |
| Almacenamiento local | Actual | sesión cifrada, token de recogida cifrado, contexto público, carrito mínimo, establecimiento enrolado y preferencias visuales | Android Keystore + SharedPreferences; no se encontró Room/SQLite/Analytics en este cliente |
| Logs | Actual, diagnóstico local | método, path y status HTTP; multipart registra además el tamaño total del payload en bytes | `Log.w` del dispositivo; no se observó envío remoto de logs en el cliente |
| Eliminación de cuenta | Actual | Firebase ID token reciente, confirmación exacta `ELIMINAR` e `Idempotency-Key` UUID v4 | `DELETE identidad/cuenta`; tras HTTP 200 se limpia sesión Firebase, contexto, carrito y tokens locales |
| Stripe/tarjetas | Actual, Test Mode | Stripe Android 23.13.1 + PaymentSheet; `client_secret`, `publishable_key`, `stripe_account_id`, estado de pago e información de pago capturada por Stripe | PaymentSheet envía datos sensibles directamente a Stripe; el backend propio entrega sesión/estado de pago y sigue siendo autoridad del pedido |

## Detalle actual

### Firebase Auth e identidad

`FirebaseStudentAuthRepository` usa correo/contraseña para alta, login,
verificación, recuperación y cierre de sesión. Al crear la cuenta actualiza el
`displayName` del usuario de Firebase. La sesión expuesta al cliente contiene
`uid`, correo, nombre mostrado y `emailVerified`.

Después de verificar el correo, el cliente obtiene un ID token de Firebase y:

- registra o actualiza identidad en `POST identidad/alta` con nombre y las
  versiones vigentes de términos/privacidad;
- intercambia el token por un contexto Vaiinilla en
  `POST sesiones/contexto-cliente`, enviando el slug del establecimiento y,
  cuando aplica, el identificador contextual del alumno;
- guarda el access token corto del backend cifrado con Android Keystore.

La recuperación de acceso solicita al backend el envío de correo mediante
`POST publico/correos/recuperacion`, con el correo normalizado. La entrega de
correo de verificación usa el ID token de Firebase en
`POST publico/correos/verificacion`.

El cliente no demuestra por sí solo qué metadatos adicionales conserva
Firebase (por ejemplo, logs internos del proveedor); eso debe verificarse en
Firebase Console y en la política de Firebase del proyecto.

### Eliminación de cuenta

La eliminación definitiva de cuenta está implementada en el cliente Android.
`FirebaseStudentAuthRepository` reautentica la sesión de correo/contraseña y
obtiene un ID token nuevo. `RemoteAccountDeletionRepository` envía ese token al
backend en `DELETE identidad/cuenta`, junto con un `Idempotency-Key` UUID v4 y
el body exacto `{"confirmacion":"ELIMINAR"}`.

El contrato actual del backend elimina la identidad de Firebase, desactiva
membresías y autoridad de plataforma, anonimiza perfil e identificadores y
conserva pedidos, pagos, movimientos, wallet y aceptaciones legales vinculados
a un UUID anónimo para mantener contabilidad y auditoría. Una tarea restringida
puede completar la anonimización si ocurre una caída entre Firebase y
PostgreSQL.

El cliente sólo ejecuta `StudentSessionCleanup.clear()` después de una respuesta
HTTP 200: limpia el token de contexto, cierra la sesión Firebase, borra el
contexto invitado/carrito y elimina tokens de recogida locales. La prueba E2E
real con una cuenta descartable sigue pendiente.

### Establecimientos, espacios y QR

La búsqueda y consulta de establecimientos es pública. El modelo contiene el
identificador, nombre, slug, la etiqueta del identificador contextual y si ese
identificador es obligatorio. Un QR `https://vaiinilla.app/e/{slug}` selecciona
establecimiento; un QR `https://vaiinilla.app/u/{id}` representa una cuenta de
usuario para la operación de Caja; los QR de espacio son valores opacos que se
envían en `POST publico/espacios/resolver`.

El establecimiento y el espacio seleccionados se guardan en
`GuestSessionStore` para restaurar la experiencia invitada. El carrito local
sólo guarda una instantánea mínima de IDs de producto, cantidades e IDs de
opciones; vuelve a resolver los productos contra el catálogo actual.

### Pedidos y operación

La creación de pedido envía al backend:

- método de pago seleccionado por el contrato vigente (`efectivo`, `saldo` o `stripe`);
- destino (`para_llevar` o `en_espacio`) y espacio cuando corresponde;
- notas libres para cocina;
- IDs de producto, cantidades e IDs de opciones.

La respuesta puede contener nombre y matrícula/identificador del usuario,
partidas, precios, descuentos, cashback, total, estado, folio, fechas,
versiones y token de recogida. Caja puede registrar cobro en efectivo; los
roles operativos pueden ejecutar transiciones de pedido. El servidor es la
autoridad para permisos, precios, saldo y estado.

### Saldo

La pantalla de cartera lee `GET wallets/me`, que devuelve identidad del
cliente, saldo por establecimiento y movimientos con tipo, descripción,
monto, saldo posterior, pedido relacionado y fecha. Caja puede buscar clientes
por nombre/identificador y registrar recargas en efectivo mediante
`POST wallets/{userId}/recargas-efectivo`; la respuesta incluye saldos anterior y
nuevo, movimiento y sesión de caja.

Las pantallas históricas de “Agregar tarjeta” dentro de Wallet siguen siendo
superficies no funcionales, pero el **checkout sí integra Stripe** mediante
PaymentSheet. La selección `Tarjeta` usa la sesión Stripe devuelta para el
pedido actual. Android no almacena PAN/CVC ni crea PaymentIntents; la captura
de información sensible ocurre dentro del SDK de Stripe y se transmite
directamente a Stripe.

### Cámara, imágenes y QR

El manifiesto declara `CAMERA`. CameraX + ML Kit analiza en el dispositivo un
único QR y entrega el valor al flujo de resolución; no se encontró grabación ni
subida automática de video. En la superficie operativa de catálogo, una foto
tomada con cámara o elegida de la galería se prepara localmente y se sube como
imagen del producto a `PUT catalogo/productos/{id}/imagen` mediante multipart.

### Persistencia local y seguridad

- `AndroidKeyStoreSessionStore`: cifra el access token del backend con AES-GCM;
  IV y ciphertext viven en `SharedPreferences`.
- `SharedPreferencesPickupTokenStore`: cifra los tokens de recogida con
  Android Keystore. También lee una clave legacy plaintext una sola vez para
  migrarla; ese camino debe mantenerse como punto de revisión de seguridad.
- `GuestSessionStore`: guarda contexto público y carrito mínimo, no una sesión
  de autorización.
- `StudentAuthPreferences`: guarda solamente el `establishmentId` enrolado
  como ayuda de UX.
- `ThemePreferences`: guarda la preferencia de tema.

No se encontró almacenamiento local de contraseña, una base Room/SQLite,
Firebase Analytics ni Crashlytics. **Sí existe Stripe Android 23.13.1** en el
catálogo de dependencias y PaymentSheet está cableado al checkout. Firebase Auth
y Stripe pueden mantener o transmitir datos propios del proveedor; deben
reflejarse en Data Safety según su comportamiento y relación contractual.

### Logs

`HttpVaiinillaApiClient` escribe `Log.w` con información reducida: método, path
y código HTTP. Las cargas multipart registran además el tamaño total del payload
en bytes. El cliente HTTP actual no registra `Authorization`, `Location` ni
cuerpos de respuesta en esos logs. No se observó un colector remoto de logs en
el cliente Android inspeccionado; la retención de logs del backend y de los
proveedores debe revisarse por separado.

### Infraestructura backend observada

La implementación y configuración actuales del backend identifican estos
proveedores técnicos:

- **Railway** para alojamiento/despliegue del backend;
- **Supabase / PostgreSQL** para persistencia transaccional;
- **Supabase Storage** para imágenes de catálogo;
- **Firebase / Google** para identidad y autenticación;
- **Resend** para correo transaccional.

Los nombres de proveedor ya no son un dato desconocido para este mapa. Siguen
pendientes la región exacta de procesamiento, los plazos de retención y la
validación legal/contractual de cada proveedor antes de una declaración pública.

## Stripe y métodos digitales — estado actual

Stripe está integrado en Android en **Test Mode** mediante
`com.stripe:stripe-android:23.13.1` y PaymentSheet. El cliente recibe desde el
contrato del pedido `client_secret`, `publishable_key` y `stripe_account_id`;
no genera PaymentIntents ni contiene claves secretas de Stripe.

`OrderContractMapper` exige actualmente una publishable key `pk_test_...`; una
`pk_live_...` falla antes de presentar PaymentSheet. Por tanto, la integración
es funcional a nivel de cliente pero todavía necesita E2E real y una decisión
explícita Test Mode vs Live Mode antes de anunciar tarjeta como disponible en
producción pública.

La información sensible de tarjeta se introduce en componentes de Stripe y se
envía directamente a Stripe. Stripe documenta además recopilación de interacción
con el SDK, características/modelo del dispositivo y versión de sistema para
funcionalidad, análisis y prevención de fraude; 3DS2 puede transmitir datos de
dispositivo a la red de tarjeta/banco emisor. Todo ello debe reflejarse en Play
Data Safety.

La guía operativa para rellenar el formulario vive en
`docs/play-store/DATA_SAFETY_FORM.md`.

## Pendientes para las declaraciones de tienda

1. Confirmar en Firebase Console los datos y retención propios del proveedor.
2. Confirmar en backend los plazos de retención y acceso aplicables a identidad,
   pedidos, saldo, imágenes, auditoría y backups.
3. Revisar el alert de Secret Scanning existente sobre la configuración Firebase
   y confirmar las restricciones de la API key; no confundir esa key pública de
   configuración con una credencial privada.
4. Ejecutar la eliminación de cuenta E2E con una cuenta descartable contra el
   backend real y publicar el recurso web externo de eliminación requerido para
   la distribución en tienda.
5. Confirmar regiones de procesamiento y la relación legal/contractual con
   Railway, Supabase, Firebase/Google y Resend.
6. Confirmar la clasificación contractual de Stripe y demás proveedores para
   cerrar `Shared` en Data Safety; no inferirla solo desde el código.
7. Ejecutar E2E Stripe Test Mode y decidir Test Mode vs Live Mode antes de un
   release público con tarjeta.
