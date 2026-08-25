# Google Play Target Audience — Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **DECISIÓN DE PRODUCTO PREPARADA PARA PLAY CONSOLE**.

Baseline Android auditado: `88ce4d14`.

## Decisión de producto

Vaiinilla está dirigida a usuarios **desde secundaria en adelante** y a personal
autorizado de cafeterías escolares.

En México, la edad típica de secundaria es **12 a 14 años**; media superior,
**15 a 17**; y educación superior, típicamente desde los **18 años**.

Google Play no ofrece un selector específico de “12+”. Sus grupos relevantes
son `9-12`, `13-15`, `16-17` y `18 and over`. Para representar de forma honesta
que Vaiinilla empieza desde primero de secundaria, la propuesta de selección es:

- [x] **Ages 9-12** — seleccionado únicamente porque incluye a usuarios de 12
      años de secundaria.
- [x] **Ages 13-15** — secundaria y transición a media superior.
- [x] **Ages 16-17** — media superior.
- [x] **Ages 18 and over** — educación superior y personal adulto.
- [ ] Ages 6-8.
- [ ] Ages 5 and under.

No se selecciona `9-12` para orientar Vaiinilla a primaria; se selecciona porque
es el bucket más pequeño disponible en Play que contiene la edad de 12 años.
La ficha, screenshots y descripción deben seguir hablando de **cafeterías
escolares / secundaria en adelante**, no de una app infantil.

Fuentes:

- Google Play — Target audience:
  https://support.google.com/googleplay/android-developer/answer/9867159
- Google Play — Families Policy:
  https://support.google.com/googleplay/android-developer/answer/9893335
- México / edades típicas SEP-DOF: secundaria 12-14, media superior 15-17,
  superior 18-22.

## Consecuencia: Families Policy sí aplica

Al seleccionar un grupo que incluye menores, Google Play exige cumplir las
Families Policy Requirements aplicables. Esto **no significa** que Vaiinilla
tenga que presentarse como una app para niños ni entrar automáticamente a la
sección Kids; sí significa que el tratamiento de menores debe ser correcto.

El release actual tiene varias ventajas para este cumplimiento:

- no incluye SDK de anuncios;
- no incluye `AD_ID`;
- no incluye Firebase Analytics, Crashlytics ni Messaging;
- no tiene publicidad personalizada;
- no tiene chat entre usuarios;
- no comparte ubicación GPS;
- no muestra violencia, sexo, drogas, apuestas ni otro contenido adulto
  detectado en la auditoría del cliente;
- los pedidos son de alimentos físicos en cafeterías escolares.

## Datos de menores que requieren atención explícita

El cliente puede tratar, según el flujo:

- nombre;
- correo;
- Firebase UID / IDs de usuario;
- matrícula o identificador contextual cuando el establecimiento lo exige;
- historial de pedidos;
- saldo y movimientos;
- notas de cocina;
- datos/telemetría de dispositivo en ciertas superficies operativas y SDKs;
- información de pago cuando se usa Stripe PaymentSheet.

Google advierte que la recopilación de información personal/sensible de menores,
como nombre o correo, debe declararse y puede requerir consentimiento parental
según la legislación aplicable. Esto queda como requisito legal/operativo para
la política pública y el onboarding; no se inventa desde Android.

## SDKs / APIs con audiencia menor

Al incluir el grupo `9-12`, antes de publicar se debe confirmar que los SDKs y
APIs usados en el build son apropiados para una app que puede dirigirse a
menores y que su tratamiento está reflejado en Data Safety y política de
privacidad.

SDKs relevantes del release actual:

- Firebase Authentication;
- Stripe Android / PaymentSheet;
- CameraX;
- ML Kit Barcode Scanning.

No hay un motivo técnico observado en Android para declarar que Vaiinilla es
18+, pero tampoco debe darse por cumplida la política de menores solo porque no
haya anuncios.

### Stripe

Stripe PaymentSheet está integrado en Test Mode. El SDK recopila telemetría de
interacción y características del dispositivo para funcionalidad, analítica y
prevención de fraude. Eso ya está contemplado en
`docs/play-store/DATA_SAFETY_FORM.md`.

La documentación de Stripe sobre edad para **crear una cuenta Stripe** no debe
confundirse con la edad del cliente que compra comida a un comercio. Vaiinilla
no crea cuentas Stripe para estudiantes desde Android. La relación legal de
pagos para menores, consentimiento y titularidad del medio de pago debe
confirmarse antes de habilitar tarjeta en producción.

## App details / representación de la audiencia

Respuestas de trabajo:

- **¿La app está diseñada principalmente para niños?** → **No**.
- **¿Incluye menores en su audiencia objetivo?** → **Sí**.
- **¿También está diseñada para adultos?** → **Sí**, por educación superior y
  modos de operación de cafetería.
- **¿Tiene anuncios?** → **No** en el cliente auditado.
- **¿La ficha usa personajes, copy o mecánicas para atraer a niños pequeños?** →
  **No**; el branding es escolar/alimentario y funcional.

## Antes de guardar Target Audience en Play Console

- [ ] Tener política de privacidad pública (KAK-44), requisito previo de la
      sección Target audience.
- [x] Ads declaration preparada como `No ads` para el cliente auditado.
- [x] App Access documentado en `APP_ACCESS_REVIEW.md`.
- [ ] Confirmar que Data Safety final refleja tratamiento de menores y SDKs.
- [ ] Confirmar base legal/consentimiento aplicable para nombre, correo y demás
      datos de estudiantes menores.
- [ ] Confirmar condiciones de pago para estudiantes menores si Stripe se
      habilita en Live Mode.
- [ ] Revalidar el AAB exacto enviado a Play por si cambia cualquier SDK.
