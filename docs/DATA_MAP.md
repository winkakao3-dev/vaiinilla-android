# Mapa real de datos de Vaiinilla Android

Fecha de revisión: 2026-08-17

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
| Stripe/tarjetas | Futuro | no hay SDK, endpoint ni captura real de tarjeta en este Android | Pendiente; la UI sólo muestra una superficie explicativa y no debe declararse como cobro actual |

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

- método de pago seleccionado por el contrato vigente (`efectivo` o `saldo`);
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

La pantalla “Agregar tarjeta” es una explicación de que el backend aún no
integra una pasarela. `WalletUiState` contiene un modelo de tarjeta para
previews/estado Compose, pero no existe una ruta actual que capture, envíe o
persista una tarjeta real.

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
Firebase Analytics, Crashlytics ni un SDK de Stripe en el código y catálogo de
dependencias inspeccionados. Firebase Auth puede mantener su propio estado
interno del proveedor; debe revisarse por separado.

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

## Futuro: Stripe y métodos digitales

Stripe no está conectado actualmente. Cuando se implemente, el diseño de datos
debe limitar el cliente a tokens/payment-method IDs y estados de pago; no debe
capturar ni persistir PAN, CVC o datos completos de tarjeta en Android ni en el
backend propio. La integración futura requerirá actualizar este documento,
Play Data Safety, Apple App Privacy, permisos, política de privacidad y pruebas
de extremo a extremo antes de declararse disponible.

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
6. Repetir el mapa cuando Stripe y cualquier proveedor de analítica/errores se
   conecten.
