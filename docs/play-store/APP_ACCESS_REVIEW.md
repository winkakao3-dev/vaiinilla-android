# Google Play App Access — Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **INSTRUCCIONES PREPARADAS; CREDENCIALES DEMO PENDIENTES**.

Este documento no contiene passwords, tokens privados ni credenciales reales.
Las credenciales de reviewer deben mantenerse fuera de Git/Linear y cargarse
solo en Play Console.

Fuentes oficiales:

- App content / App access:
  https://support.google.com/googleplay/android-developer/answer/9859455
- Requisitos de sign-in details:
  https://support.google.com/googleplay/android-developer/answer/15748846

## Declaración que corresponde en Play Console

Seleccionar:

**All or some functionality is restricted / Toda o parte de la funcionalidad
está restringida.**

Motivo: sin sesión el lanzamiento normal lleva a login. Menú/establecimiento,
pedidos, saldo, cuenta y los modos operativos dependen de autenticación o
contexto autorizado.

Google Play admite hasta cinco conjuntos de instrucciones de acceso. Para
Vaiinilla se recomienda usar **dos**, sin hacer que el reviewer dependa de OTP,
correo de verificación, invitaciones de un solo uso, ubicación física ni QR
temporal.

## Conjunto 1 — Cliente / estudiante

### Preparación externa requerida

Crear o confirmar una cuenta demo que:

- sea exclusivamente para Google Play Review;
- tenga correo verificado;
- esté activa de forma permanente durante el proceso de review;
- use contraseña reutilizable que no expire durante la revisión;
- esté vinculada al establecimiento de prueba que se indique en Play Console;
- tenga solo el rol cliente, para que el lanzamiento vaya al flujo normal del
  usuario y no al selector de modos de staff;
- permita consultar menú, carrito, pedidos, cuenta y saldo sin depender de una
  acción manual posterior del equipo.

No crear ni modificar esa cuenta desde este repositorio.

### Texto sugerido para Play Console — ENGLISH

Title:

`Vaiinilla customer review access`

Instructions:

```text
1. Launch Vaiinilla. The app opens on the student sign-in screen.
2. Sign in with the reusable review email and password provided in this Play Console access set.
3. If the app asks for an establishment-specific student/client identifier, enter the reusable review identifier provided below.
4. After sign-in, select the review establishment shown below if it is not already selected.
5. You can now review the menu, product details, cart/checkout, order history/tracking, account and Vaiinilla balance surfaces.
6. No one-time password, physical location, external hardware or paid subscription is required for this review account.

Review establishment: [REVIEW_ESTABLISHMENT_NAME]
Context/client ID, only if requested: [REVIEW_CONTEXT_ID]
```

Play Console fields to fill outside Git:

- Username/email: `[REVIEW_CUSTOMER_EMAIL]`
- Password: `[REVIEW_CUSTOMER_PASSWORD]`
- Other instructions: use the text above, replacing the non-secret placeholders.

## Conjunto 2 — Staff / operación

Este conjunto solo es necesario si Caja/Cocina/Mesero estarán habilitados y
forman parte del AAB público que se envíe a revisión. Como esos modos existen en
el APK actual, la opción más segura es proporcionar acceso en vez de esperar a
que Google lo solicite después.

### Preparación externa requerida

Crear o confirmar una cuenta demo que:

- sea exclusivamente para review;
- tenga correo verificado;
- tenga preasignados los modos operativos que Google deba revisar;
- no requiera aceptar una invitación temporal durante el review;
- no requiera un QR físico ni acceso desde una red/ubicación específica;
- mantenga credenciales reutilizables y válidas durante toda la revisión.

### Texto sugerido para Play Console — ENGLISH

Title:

`Vaiinilla staff review access`

Instructions:

```text
1. Launch Vaiinilla and sign in with the reusable staff review credentials provided in this access set.
2. The app opens the authorized mode selector when staff modes are available.
3. Select Cashier, Kitchen or Waiter to review the corresponding operational surface.
4. Use the mode selector to switch between the staff roles enabled for this demo account.
5. The review account does not require a one-time invitation, temporary QR code, physical location, external hardware or paid subscription.
```

Play Console fields to fill outside Git:

- Username/email: `[REVIEW_STAFF_EMAIL]`
- Password: `[REVIEW_STAFF_PASSWORD]`

## Reglas para las credenciales

Antes de subirlas a Play Console comprobar:

- [ ] no pertenecen a una persona real;
- [ ] funcionan desde una instalación limpia;
- [ ] funcionan fuera de la red escolar;
- [ ] no requieren OTP/2FA no reproducible;
- [ ] no caducan durante el review;
- [ ] no obligan a revisar un correo para verificar la cuenta;
- [ ] no dependen de un código/QR temporal;
- [ ] permiten llegar a toda la funcionalidad descrita;
- [ ] las instrucciones de Play Console están en inglés;
- [ ] se probaron usando el mismo AAB/build candidato que se enviará a Google.

## Estado de implementación Android auditado

- Sin sesión y sin deep link pendiente, `resolveLaunchDestination()` envía a
  `Login`.
- Tras login de cliente, el flujo continúa hacia discovery/establecimiento.
- Una cuenta con staff modes puede iniciar en `StaffModes`.
- La app soporta cliente, Caja, Cocina y Mesero.
- El repositorio no debe almacenar credenciales de reviewer.

## Para cerrar App Access

- [ ] Obtener/crear cuenta demo cliente sin tocar datos reales.
- [ ] Obtener/crear cuenta demo staff si esos modos entran al release público.
- [ ] Probar ambas desde instalación limpia del build candidato.
- [ ] Pegar credenciales e instrucciones en Play Console > App content > App access.
- [ ] Guardar en el repo solo evidencia no secreta de que el acceso fue validado.
