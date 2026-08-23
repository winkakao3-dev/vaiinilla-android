# Google Play Data Safety — formulario de trabajo para Vaiinilla

Fecha de revisión: 2026-08-23

Baseline Android auditado: `b0d73fb2a969ae0bd25ec5b9861254b5b8b7bf70`.

Este documento traduce el comportamiento **actual del cliente Android** a las
categorías del formulario Data Safety de Google Play. No modifica ni presupone
configuración de backend, Railway, Supabase, Firebase Console, Stripe Dashboard
o Play Console. Los campos marcados como **PENDIENTE EXTERNO** no deben cerrarse
por inferencia.

Google Play considera "recogido" cualquier dato transmitido fuera del
dispositivo, incluidos datos enviados por SDKs de terceros. La clasificación
"compartido" tiene excepciones, entre ellas transferencias a proveedores de
servicios que tratan datos por cuenta del desarrollador, pero esa relación debe
confirmarse contractualmente antes de marcar `Shared = No`.

Fuentes oficiales:

- Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Financial features: https://support.google.com/googleplay/android-developer/answer/13849271
- Stripe Mobile SDK privacy: https://support.stripe.com/questions/stripe-mobile-sdk-privacy-details
- Stripe Android SDK: https://github.com/stripe/stripe-android

## 1. Respuestas globales sugeridas

| Pregunta | Respuesta de trabajo | Evidencia / condición |
| --- | --- | --- |
| ¿La app recoge o comparte datos de usuario? | **Sí** | Cuenta, pedidos, saldo, fotos operativas, device ID y Stripe transmiten datos fuera del dispositivo. |
| ¿Todos los datos se cifran en tránsito? | **Sí, sujeto a validación final del release** | Las superficies remotas auditadas usan HTTPS. Confirmar nuevamente en el AAB final antes de enviar el formulario. |
| ¿Existe mecanismo para solicitar eliminación? | **Sí, in-app; publicación web externa aún pendiente** | Android implementa eliminación. KAK-47 sigue abierto para la URL web externa exigida por Play. |
| ¿Revisión de seguridad independiente? | **No** | No existe evidencia de MASA u otra revisión independiente aplicable. |
| ¿Publicidad/marketing basado en datos? | **No demostrado** | No hay SDK de anuncios, Analytics, Crashlytics ni `AD_ID` en el cliente auditado. Stripe declara que sus señales del SDK no se usan para publicidad. |

## 2. Tipos de datos que deben considerarse en el formulario

La columna `Shared` no se cierra donde depende del rol contractual real de
Firebase/Google, Railway/Supabase, Resend, Stripe, redes de tarjeta o bancos
emisores.

| Categoría Google Play | Tipo | Collected | Shared | Requerido / opcional | Finalidad sugerida | Evidencia Android |
| --- | --- | ---: | --- | --- | --- | --- |
| Información personal | Nombre | Sí | PENDIENTE EXTERNO | Opcional a nivel de app; requerido al crear cuenta | Gestión de cuentas; funcionalidad | Firebase Auth + alta de identidad |
| Información personal | Dirección de correo | Sí | PENDIENTE EXTERNO | Opcional a nivel de app; requerido al crear cuenta | Gestión de cuentas; funcionalidad; comunicaciones transaccionales | Firebase Auth, verificación/recuperación |
| Información personal | IDs de usuario | Sí | PENDIENTE EXTERNO | Opcional a nivel de app; requerido en cuenta/operación | Gestión de cuentas; funcionalidad; seguridad | Firebase UID, IDs backend, identificador contextual/matrícula cuando aplica |
| Información financiera | Información para pagos del usuario | **Sí cuando se usa Tarjeta** | **PENDIENTE EXTERNO** | Opcional | Funcionalidad; prevención de fraude/seguridad | Stripe PaymentSheet recoge datos de pago y los envía directamente a Stripe; no pasan por el backend propio |
| Información financiera | Historial de compras | Sí | PENDIENTE EXTERNO | Opcional | Funcionalidad | Pedidos, pagos, folio, importes, estados |
| Información financiera | Otra información financiera | Sí | PENDIENTE EXTERNO | Opcional | Funcionalidad | Saldo Vaiinilla, movimientos, recargas, cashback |
| Fotos y vídeos | Fotos | Sí, solo personal autorizado | PENDIENTE EXTERNO | Opcional | Funcionalidad | Foto de producto subida mediante multipart |
| Actividad de la aplicación | Interacciones con la aplicación | **Sí en Stripe** | PENDIENTE EXTERNO | Opcional | Funcionalidad; análisis; prevención de fraude/seguridad | Stripe Mobile SDK declara telemetría de interacción y características del dispositivo |
| Actividad de la aplicación | Otro contenido generado por usuarios | Sí | PENDIENTE EXTERNO | Opcional | Funcionalidad | Notas libres de pedido para cocina |
| IDs de dispositivo o de otro tipo | IDs de dispositivo o de otro tipo | Sí | PENDIENTE EXTERNO | Dependiente de flujo/rol | Funcionalidad; seguridad; prevención de fraude | `ANDROID_ID` en presencia operativa; Stripe usa señales/características del dispositivo |

### Tipos que no están demostrados por el cliente actual

No marcar como recogidos por la app **solo por precaución** si no aparece nueva
evidencia en el release final:

- ubicación aproximada o precisa;
- teléfono;
- dirección postal;
- contactos;
- calendario;
- SMS/MMS;
- salud/actividad física;
- audio;
- vídeos;
- archivos/documentos;
- historial de navegación web;
- aplicaciones instaladas;
- datos publicitarios / Advertising ID.

Nota: una dirección IP tratada por infraestructura o SDK puede entrar en
categorías de Google Play dependiendo del proveedor y del uso. Confirmar
Firebase/Stripe/proveedores antes de cerrar el CSV de Data Safety.

## 3. Stripe — tratamiento que debe reflejarse

Estado Android actual:

- dependencia `com.stripe:stripe-android:23.13.1`;
- PaymentSheet se presenta desde el checkout;
- `PaymentConfiguration` usa `publishable_key` y `stripe_account_id` recibidos
  para el establecimiento actual;
- el cliente recibe `client_secret` del backend y lo usa para presentar el
  PaymentIntent;
- el cliente **no crea PaymentIntents**;
- el cliente **no contiene Stripe secret key ni webhook secret**;
- el mapper actual exige `pk_test_...`; una `pk_live_...` se rechaza antes de
  PaymentSheet;
- `PaymentSheetResult.Completed` no se considera por sí solo pago confirmado;
  Android vuelve a consultar el estado autoritativo del pedido/pago.

Según Stripe, el SDK móvil puede recoger información de pago, interacción con
el SDK, modelo/características del dispositivo y versión de SO para
funcionalidad, análisis y prevención de fraude. En 3DS2 puede transmitirse
información de dispositivo a la red de tarjeta y al banco emisor como parte del
protocolo.

Por tanto **no** debe enviarse Data Safety con la afirmación histórica de que
"Stripe no existe".

## 4. `Shared` — lo que falta decidir con evidencia externa

Antes de enviar Data Safety, confirmar para cada proveedor si la transferencia
califica bajo la excepción de **service provider** de Google Play o si debe
marcarse como compartida:

- Firebase / Google;
- Railway / Supabase;
- Resend;
- Stripe;
- redes de tarjeta / banco emisor cuando interviene 3DS.

No basta con que el proveedor sea "externo" para marcar `Shared = Yes`, ni basta
con llamarlo "proveedor" para marcar `Shared = No`. La definición de Google
requiere revisar la relación real y el tratamiento por cuenta del desarrollador.

## 5. Financial features declaration

Con el release actual ya no es correcto afirmar que la app solo tiene efectivo
y saldo. La app implementa:

- saldo por establecimiento;
- movimientos;
- recarga en efectivo por Caja;
- pago de pedidos con saldo;
- checkout con tarjeta mediante Stripe PaymentSheet en Test Mode;
- campos/estados de cashback en pedidos y movimientos.

### Selección de trabajo

- **Mobile payments and digital wallets** — **Sí / alta confianza**.
- **Rewards, points, frequent flier miles, and other incentives** — **revisar antes de enviar** porque el cliente expone `cashback` y superficies de recompensa/stickers; confirmar que esas recompensas estarán activas en el release público.
- Loans, BNPL, crypto, investment, insurance, banking, money transfer — **No demostrados** por el cliente actual.

La decisión Test Mode vs Live Mode de Stripe debe resolverse antes de un release
público que anuncie tarjeta como función disponible.

## 6. Prácticas de seguridad visibles desde Android

- token de sesión backend cifrado con AES-GCM / Android Keystore;
- tokens de recogida cifrados con Android Keystore;
- no se encontró almacenamiento local de contraseña;
- logs HTTP propios limitados a método/path/status y tamaño multipart;
- no se encontró logging propio de `Authorization` ni cuerpos de respuesta;
- `android:allowBackup="false"`;
- eliminación local de sesión/contexto/carrito/tokens tras éxito confirmado del
  flujo de eliminación de cuenta.

Estas prácticas no sustituyen la validación de Firebase, Stripe ni
infraestructura externa.

## 7. Gates para poder marcar Data Safety como listo

- [ ] KAK-44: política de privacidad publicable y URL final.
- [ ] KAK-47: recurso web externo de eliminación.
- [ ] KAK-48: E2E de eliminación con cuenta descartable.
- [ ] KAK-49: revisar Firebase/Google Cloud Console.
- [ ] KAK-50: retenciones/backups/regiones externas.
- [ ] Confirmar rol contractual / service-provider de los proveedores para
      responder `Shared` sin inventar.
- [ ] Ejecutar E2E Stripe Test Mode real.
- [ ] Decidir si el primer release público incluye tarjeta y, si sí, resolver
      Test Mode vs Live Mode antes de publicar.
- [ ] Reauditar dependencias, permisos y manifiesto del **AAB exacto** que se
      subirá a Play.
- [ ] Tras recibir los screenshots promocionales, verificar que la metadata no
      prometa tarjeta/live payments si el build publicado no puede completarlos.
