# Política de Privacidad de Vaiinilla

> **Estado:** BORRADOR — NO PUBLICAR HASTA COMPLETAR LOS CAMPOS `[PENDIENTE]` Y VALIDARLOS CON LOS RESPONSABLES CORRESPONDIENTES.
>
> **Base técnica:** este borrador se elaboró a partir de `docs/DATA_MAP.md`, del comportamiento actualmente implementado en el cliente Android y del contrato/configuración vigente del backend. El backend actual identifica Railway, Supabase/PostgreSQL, Firebase/Google y Resend; los datos legales, plazos de retención, regiones de procesamiento y la validación contractual de esos proveedores deben completarse antes de publicación.

**Última actualización del borrador:** 17 de agosto de 2026

Vaiinilla ("**Vaiinilla**", "**la Aplicación**", "**nosotros**" o "**nuestro servicio**") reconoce la importancia de la privacidad y la protección de los datos de sus usuarios.

Esta Política de Privacidad explica qué información puede ser recopilada, utilizada, almacenada o transmitida cuando una persona utiliza la aplicación Vaiinilla, así como las finalidades de dicho tratamiento, las medidas generales de seguridad utilizadas y las opciones disponibles para gestionar o eliminar su información.

## 1. Responsable del tratamiento

- **Aplicación:** Vaiinilla
- **Responsable legal / Razón social:** `[PENDIENTE — DEFINIR PROPIETARIO LEGAL DE VAIINILLA]`
- **Nombre comercial, si aplica:** `[PENDIENTE]`
- **Domicilio:** `[PENDIENTE — SI CORRESPONDE LEGALMENTE]`
- **País / jurisdicción:** `[PENDIENTE]`
- **Correo para asuntos de privacidad:** `[PENDIENTE — CORREO DE PRIVACIDAD/SOPORTE]`
- **Sitio web oficial:** `[PENDIENTE — URL OFICIAL]`

Estos datos deberán completarse y validarse antes de publicar esta Política de Privacidad.

## 2. Alcance

Esta Política aplica al cliente móvil Android de Vaiinilla y a los servicios de backend necesarios para proporcionar las funciones de la aplicación.

Vaiinilla permite, entre otras funciones, crear y administrar una cuenta, acceder a establecimientos, consultar catálogos, realizar pedidos, consultar y utilizar Saldo Vaiinilla, utilizar códigos QR y acceder a determinadas funciones operativas autorizadas.

Algunas funciones pueden depender del establecimiento, rol, membresía o modalidad de acceso de cada usuario.

## 3. Información de cuenta e identidad

Para crear y utilizar una cuenta podemos tratar:

- nombre mostrado o nombre asociado a la cuenta;
- dirección de correo electrónico;
- identificador único de usuario generado por Firebase;
- estado de verificación del correo electrónico;
- información necesaria para autenticar y mantener una sesión;
- versiones de Términos y Condiciones y Política de Privacidad aceptadas;
- identificadores contextuales requeridos por determinados establecimientos, cuando corresponda.

La contraseña se utiliza durante el proceso de autenticación mediante Firebase Authentication. La aplicación Android no almacena la contraseña del usuario en su almacenamiento propio.

## 4. Autenticación

Vaiinilla utiliza actualmente **Firebase Authentication** para autenticar usuarios mediante correo electrónico y contraseña.

Firebase proporciona identificadores y tokens de autenticación que permiten verificar la identidad del usuario y establecer una sesión válida con los servicios de Vaiinilla.

La aplicación puede solicitar una nueva autenticación para realizar determinadas operaciones sensibles, como la eliminación definitiva de una cuenta.

El tratamiento realizado directamente por Firebase también está sujeto a las prácticas y condiciones del proveedor.

## 5. Información de establecimientos y contexto de uso

La aplicación puede tratar información relacionada con los establecimientos utilizados por el usuario, incluyendo:

- identificador del establecimiento;
- nombre;
- slug o código interno;
- espacio, mesa u otra ubicación operativa dentro del establecimiento;
- identificadores necesarios para asociar al usuario con un establecimiento;
- membresías o permisos asociados;
- modalidad o rol autorizado.

Parte de esta información puede conservarse localmente para restaurar el contexto de uso de la aplicación.

## 6. Información de pedidos

Cuando un usuario realiza o participa en un pedido, Vaiinilla puede tratar:

- productos seleccionados;
- opciones o modificaciones de producto;
- cantidades;
- notas destinadas a cocina;
- establecimiento;
- espacio o destino del pedido;
- modalidad para llevar o en espacio;
- método de pago seleccionado;
- precios, descuentos y totales;
- saldo o movimientos relacionados;
- estado del pedido;
- folio;
- fechas y horas;
- token o código de recogida;
- información necesaria para que Caja, Cocina, Mesero u otros roles autorizados procesen el pedido.

Esta información se utiliza para crear, procesar, entregar, cobrar y mantener el historial operativo correspondiente.

## 7. Saldo Vaiinilla y operaciones financieras

Cuando se utiliza Saldo Vaiinilla podemos tratar:

- identidad de la cuenta;
- establecimiento;
- saldo disponible;
- movimientos;
- montos;
- recargas;
- descripción del movimiento;
- saldo posterior a cada operación;
- pedido relacionado;
- fechas;
- información relativa a la sesión de Caja que haya procesado una operación.

Estos datos son administrados principalmente por los sistemas del backend de Vaiinilla.

### Tarjetas y Stripe

A la fecha de este borrador, la aplicación Android **no captura ni almacena directamente números completos de tarjeta, códigos CVC ni otros datos completos de tarjetas bancarias mediante Stripe**.

La integración de métodos digitales mediante Stripe se encuentra prevista como una funcionalidad futura. Cuando esa integración sea habilitada, esta Política de Privacidad deberá actualizarse antes de declararla disponible en producción.

- **Proveedor de pagos futuro:** Stripe
- **Estado actual:** no habilitado para captura real de tarjeta en el cliente Android.

## 8. Cámara, fotografías y códigos QR

Vaiinilla puede solicitar acceso a la cámara para funciones específicas.

### Códigos QR

La cámara puede utilizarse para leer códigos QR relacionados con:

- establecimientos;
- espacios o mesas;
- cuentas o identificadores utilizados en operaciones autorizadas.

El valor interpretado puede enviarse al backend correspondiente para resolver el recurso asociado. Vaiinilla no utiliza esta función para grabar vídeo de forma continua.

### Imágenes de productos

Los usuarios con permisos operativos adecuados pueden tomar una fotografía o seleccionar una imagen del dispositivo para actualizar imágenes de productos. La imagen seleccionada puede enviarse al backend de Vaiinilla.

El acceso a la cámara ocurre cuando el usuario utiliza voluntariamente una función que lo requiere y está sujeto a los permisos proporcionados por Android.

## 9. Información almacenada en el dispositivo

Vaiinilla puede almacenar localmente información necesaria para el funcionamiento de la aplicación, incluyendo:

- credenciales o tokens de sesión;
- contexto de establecimiento;
- información mínima del carrito;
- tokens de recogida;
- identificadores necesarios para restaurar el contexto del usuario;
- preferencias visuales, como el tema de la aplicación.

Los tokens de sesión sensibles gestionados directamente por Vaiinilla utilizan mecanismos de cifrado apoyados por **Android Keystore**.

La aplicación no utiliza actualmente una base de datos local Room/SQLite para mantener una copia completa de la información de la cuenta.

## 10. Información técnica y diagnóstico

Los sistemas de Vaiinilla pueden procesar información técnica necesaria para:

- establecer comunicaciones con el backend;
- detectar errores;
- proteger las sesiones;
- solucionar problemas operativos;
- prevenir solicitudes incorrectas o no autorizadas.

El cliente Android registra localmente información limitada sobre determinadas solicitudes HTTP, como método, ruta y código de respuesta. El cliente HTTP actual no registra contraseñas, tokens de autenticación, encabezados `Authorization` ni cuerpos completos de respuestas HTTP sensibles.

**Retención y tratamiento de logs del backend y proveedores:** `[PENDIENTE — CONFIRMAR CON BACKEND/INFRAESTRUCTURA]`

## 11. Finalidades del tratamiento

La información tratada por Vaiinilla puede utilizarse para:

1. crear y administrar cuentas;
2. autenticar usuarios;
3. verificar direcciones de correo;
4. recuperar el acceso a una cuenta;
5. identificar establecimientos y espacios;
6. ofrecer catálogos y productos;
7. crear y gestionar pedidos;
8. procesar operaciones realizadas por Caja, Cocina y Mesero;
9. administrar Saldo Vaiinilla y sus movimientos;
10. mostrar información e historial asociado a las operaciones;
11. mantener la seguridad de las cuentas y sesiones;
12. diagnosticar errores y mejorar la estabilidad;
13. cumplir obligaciones legales, contables, fiscales o de seguridad que resulten aplicables;
14. atender solicitudes de soporte, privacidad o eliminación de cuenta.

## 12. Servicios y terceros

Para proporcionar las funciones de Vaiinilla podemos utilizar proveedores tecnológicos que procesan información necesaria para operar el servicio.

### Firebase / Google

Se utiliza Firebase Authentication para autenticación y administración de identidad.

### Infraestructura del backend

La implementación actual de Vaiinilla utiliza los siguientes proveedores técnicos:

- **Railway** para alojamiento y despliegue del backend;
- **Supabase / PostgreSQL** para persistencia transaccional;
- **Supabase Storage** para imágenes de catálogo;
- **Firebase / Google** para identidad y autenticación;
- **Resend** para correo transaccional.

La identificación técnica de estos proveedores está confirmada por el código y la configuración vigentes. Antes de publicar esta política todavía deben confirmarse la entidad jurídica aplicable cuando corresponda, las regiones exactas de procesamiento, los plazos de retención y cualquier subprocesador o transferencia que deba declararse legalmente.

**Venta de datos personales a anunciantes:** `[PENDIENTE — CONFIRMAR FORMALMENTE CON RESPONSABLE LEGAL/PRODUCTO ANTES DE PUBLICAR]`

## 13. Seguridad de la información

Vaiinilla utiliza medidas técnicas destinadas a reducir el acceso no autorizado o uso indebido de la información.

Entre las medidas implementadas actualmente en el cliente Android se encuentran:

- Firebase Authentication;
- comunicaciones autenticadas mediante tokens;
- cifrado de credenciales propias almacenadas localmente mediante Android Keystore;
- separación entre tokens Firebase y tokens de contexto Vaiinilla;
- renovación controlada de sesiones;
- limpieza de datos locales al cerrar sesión o después de una eliminación de cuenta confirmada;
- ausencia de contraseñas almacenadas por el cliente Vaiinilla;
- restricciones para evitar registrar credenciales y tokens sensibles en logs.

Ningún sistema puede garantizar seguridad absoluta. Vaiinilla deberá revisar y actualizar razonablemente sus controles a medida que evolucione el servicio.

## 14. Conservación de datos

Vaiinilla conservará información mientras sea necesaria para proporcionar el servicio y durante los periodos adicionales que resulten necesarios por obligaciones legítimas.

Los **periodos definitivos todavía deben ser definidos por el responsable legal y el backend**.

| Tipo de información | Periodo previsto |
| --- | --- |
| Cuenta e identidad | `[PENDIENTE]` |
| Datos Firebase Authentication | `[PENDIENTE — CONFIRMAR CONFIGURACIÓN/RETENCIÓN FIREBASE]` |
| Pedidos | `[PENDIENTE — PLAZO LEGAL/CONTABLE]` |
| Registros de pagos/recargas/saldo | `[PENDIENTE — PLAZO LEGAL/CONTABLE]` |
| Aceptación de términos y privacidad | `[PENDIENTE]` |
| Imágenes de productos | `[PENDIENTE]` |
| Logs de seguridad/auditoría backend | `[PENDIENTE]` |
| Backups | `[PENDIENTE — CONFIRMAR EXISTENCIA Y RETENCIÓN]` |

Cuando sea necesario conservar determinados registros después de una eliminación de cuenta por motivos legítimos —por ejemplo, obligaciones legales, contables, de seguridad o prevención de fraude— dichos registros deberán conservarse sólo durante el periodo necesario y, cuando sea posible, de forma anonimizada o disociada de la identidad directa del usuario.

## 15. Eliminación definitiva de la cuenta

Los usuarios pueden iniciar la eliminación definitiva de su cuenta directamente desde Vaiinilla.

**Ruta actual en Android:** Cuenta → Eliminar cuenta

Antes de efectuar una solicitud de eliminación, Vaiinilla solicita una nueva autenticación del usuario para proteger la operación.

La aplicación obtiene un Firebase ID token reciente y envía una solicitud autenticada al servicio de Vaiinilla encargado de realizar la eliminación. La aplicación móvil **no elimina por sí sola la identidad mediante `FirebaseAuth.currentUser.delete()`**; la eliminación real pertenece al backend.

El backend actual elimina la identidad del proyecto Firebase, desactiva membresías y autoridad de plataforma, anonimiza perfil, identificadores e información visible en pedidos e invitaciones, y conserva pedidos, pagos, movimientos, wallet y aceptaciones legales vinculados a un UUID anónimo para no romper contabilidad ni auditoría. Una tarea restringida completa automáticamente la anonimización si una caída temporal interrumpe el proceso entre Firebase y PostgreSQL.

Una vez que el backend confirma correctamente la eliminación mediante HTTP 200:

- la sesión Firebase se cierra;
- se eliminan credenciales y tokens Vaiinilla almacenados localmente;
- se elimina el contexto de establecimientos y membresías almacenado en la sesión;
- se eliminan datos locales relacionados con carrito y recogida;
- se detienen tareas asociadas a la sesión;
- se elimina el historial de navegación autenticado;
- la aplicación regresa al acceso público.

La eliminación no consiste simplemente en suspender o bloquear la cuenta.

### Información que puede conservarse

Después de la eliminación podrían conservarse determinados registros cuando exista una obligación legítima, incluyendo potencialmente:

- registros de pedidos;
- registros de pagos, saldo o recargas;
- evidencia de aceptación de términos;
- registros necesarios para cumplimiento legal, contable o auditoría;
- registros necesarios para seguridad o prevención de fraude.

Cuando corresponda, estos datos deberán conservarse anonimizados o con los datos identificativos reducidos al mínimo necesario.

**Periodos específicos de conservación:** `[PENDIENTE — DEFINIR CON RESPONSABLE LEGAL/BACKEND]`

**Estado de validación técnica:** `ANDROID IMPLEMENTED + BACKEND IMPLEMENTED/DEPLOYED + CI GREEN + REAL E2E PENDING`

Antes de publicación debe ejecutarse una prueba real con cuenta descartable que valide:

Firebase reauthentication → DELETE real backend → eliminación Firebase → anonimización/revocación backend → cleanup/navegación Android.

### Solicitud externa de eliminación

Además del flujo dentro de la aplicación, deberá existir un recurso web externo para iniciar la solicitud de eliminación.

**URL para eliminación de cuenta:** `[PENDIENTE — CREAR/PUBLICAR PÁGINA WEB EXTERNA DE ELIMINACIÓN]`

## 16. Derechos y solicitudes de privacidad

El usuario podrá comunicarse con Vaiinilla para realizar consultas o solicitudes relacionadas con sus datos cuando la legislación aplicable lo permita, incluyendo acceso, corrección, eliminación y demás derechos que correspondan.

Las solicitudes deberán enviarse a:

**Correo de privacidad:** `[PENDIENTE]`

Podremos solicitar información razonablemente necesaria para verificar la identidad de la persona antes de procesar una solicitud relacionada con una cuenta.

**Procedimiento y plazo legal de respuesta:** `[PENDIENTE — DEFINIR SEGÚN JURISDICCIÓN]`

## 17. Menores de edad

**Público objetivo / edad mínima:** `[PENDIENTE — DEFINIR ANTES DE PLAY STORE]`

Una vez que el responsable de producto y legal determine el público objetivo oficial de Vaiinilla, este apartado deberá actualizarse para reflejar los requisitos correspondientes.

## 18. Transferencias y ubicación del procesamiento

Algunos proveedores tecnológicos pueden procesar información en infraestructura ubicada fuera del país del usuario.

**País(es), regiones y mecanismo aplicable:** `[PENDIENTE — CONFIRMAR PROVEEDORES Y JURISDICCIÓN]`

## 19. Cambios a esta Política

Vaiinilla podrá modificar esta Política de Privacidad cuando cambien las funciones de la aplicación, sus prácticas de datos, los proveedores utilizados o las obligaciones legales aplicables.

La fecha de la versión vigente aparecerá al inicio de la Política. Cuando corresponda, se solicitará una nueva aceptación o se notificará al usuario sobre cambios relevantes.

## 20. Contacto

- **Responsable:** `[PENDIENTE]`
- **Correo:** `[PENDIENTE]`
- **Sitio web:** `[PENDIENTE]`
- **Domicilio, si aplica:** `[PENDIENTE]`

---

## Checklist interno antes de publicar

Este bloque es interno y deberá eliminarse de la versión pública final.

- [ ] Definir razón social / propietario legal.
- [ ] Definir correo oficial de privacidad o soporte.
- [ ] Confirmar dominio/sitio web oficial.
- [ ] Confirmar jurisdicción y domicilio si corresponde.
- [ ] Definir plazos de retención para identidad, pedidos, pagos/recargas/saldo, términos, imágenes, logs y backups.
- [x] Identificar proveedores técnicos actuales: Railway, Supabase/PostgreSQL, Supabase Storage, Firebase/Google y Resend.
- [ ] Confirmar regiones, entidad jurídica aplicable, subprocesadores y condiciones contractuales que deban declararse.
- [ ] Confirmar formalmente si existe o no venta/compartición de datos con anunciantes.
- [ ] Definir público objetivo / edad mínima.
- [ ] Crear y publicar la URL externa de eliminación de cuenta.
- [ ] Confirmar configuración/retención de Firebase.
- [ ] Ejecutar la prueba E2E real de eliminación de cuenta.
- [ ] Volver a revisar este documento cuando Stripe u otros SDK de analítica/errores se habiliten.
