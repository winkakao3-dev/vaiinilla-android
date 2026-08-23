# Google Play Families — preflight de Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **IN PROGRESS — preflight técnico completado; validación jurídica/SDK pendiente**.

Baseline Android auditado: `88ce4d14`.

Vaiinilla es una app de audiencia mixta: está diseñada desde secundaria en
adelante y también incluye educación superior/personal adulto. Para representar
usuarios de 12 años en Play se usará el grupo `9-12`, por lo que aplican las
Families Policy Requirements pertinentes.

Fuentes oficiales:

- Families Policy Requirements:
  https://support.google.com/googleplay/android-developer/answer/9893335
- Data practices in Families apps:
  https://support.google.com/googleplay/android-developer/answer/11043825
- Target audience:
  https://support.google.com/googleplay/android-developer/answer/9867159

## Resultado del preflight

| Requisito / riesgo | Estado | Evidencia / acción |
| --- | --- | --- |
| Audiencia declarada con precisión | PASS de producto | Secundaria en adelante; ver `TARGET_AUDIENCE.md`. |
| Contenido apropiado para menores | PASS técnico | Sin violencia, sexo, drogas, alcohol, apuestas, horror o lenguaje ofensivo detectados; ver IARC. |
| Ads | PASS | Sin SDK de anuncios, sin `AD_ID`, sin publicidad personalizada. |
| AAID y otros IDs expresamente prohibidos para menores | PASS en cliente auditado | No se encontró AAID, SIM serial, Build Serial, BSSID, MAC, SSID, IMEI ni IMSI. |
| Número de teléfono vía TelephonyManager | PASS | No se encontró `TelephonyManager` ni permisos de teléfono. |
| Ubicación precisa | PASS | Sin permisos de ubicación ni API GPS. |
| Cámara | PASS funcional / declarar cuando corresponda | Cámara para QR; frames se procesan localmente. Staff puede subir foto de producto. |
| Android ID | DISCLOSED / REVIEW | `ANDROID_ID` se usa en identidad/presencia operativa. Google lo considera dato sensible de menores que debe declararse; ya está en Data Safety. |
| Comunicación entre usuarios | PASS / N/A | No existe chat usuario-a-usuario. |
| Contenido generado públicamente por usuarios | PASS / N/A | No hay feed/perfil/social UGC. Notas de cocina son privadas y funcionales. |
| API/SDK aptos para audiencia con menores | **PENDIENTE EXTERNO** | Confirmar Firebase Auth, Stripe y cualquier SDK que trate datos. |
| Consentimiento/base legal para datos de menores | **PENDIENTE JURÍDICO** | Nombre, correo, IDs/matrícula y datos de pedido pueden corresponder a menores. |
| Política de privacidad pública | **PENDIENTE KAK-44** | Debe explicar tratamiento de menores y SDKs. |

## Identificadores prohibidos por Families

Google indica que una app orientada a menores y adultos no debe transmitir de
menores o usuarios de edad desconocida:

- Android Advertising ID (AAID);
- SIM Serial;
- Build Serial;
- BSSID;
- MAC;
- SSID;
- IMEI;
- IMSI.

El código Android auditado no usa esos identificadores. Tampoco declara
`AD_ID`.

`ANDROID_ID` es distinto de AAID. Google lo enumera como dato sensible de
menores que debe declararse. Vaiinilla lo usa en `AndroidDeviceIdentity` para
presencia/identidad de dispositivo en modos operativos; está documentado en
Data Safety y debe mantenerse en la revisión de menores.

## Cámara

El permiso `CAMERA` existe por dos motivos:

1. escaneo QR de establecimiento/espacio mediante CameraX + ML Kit;
2. foto de producto en superficie operativa autorizada.

Para QR, el análisis ocurre en el dispositivo y la app trabaja con el valor
interpretado; no se observó subida de frames de cámara. En el flujo de staff,
la foto elegida/tomada sí puede enviarse al catálogo como multipart.

No existe permiso de micrófono.

## APIs y SDKs — blocker real antes de producción

Google exige que las APIs/SDKs usados por apps que incluyen menores sean
apropiados para servicios dirigidos a menores. En una app mixta, si un SDK no
está aprobado para menores, Google contempla usar una pantalla neutral de edad
o implementarlo de modo que no recoja datos de menores.

Vaiinilla **no tiene hoy una pantalla neutral de edad**.

Revisión necesaria:

### Firebase Authentication

- se usa para cuenta, login, verificación y tokens;
- procesa correo, UID, nombre de perfil y datos de autenticación;
- confirmar términos/configuración aplicables al uso con estudiantes menores;
- reflejar el resultado en política y Data Safety.

### Stripe Android / PaymentSheet

- actualmente Test Mode;
- se invoca solo al seleccionar `Tarjeta`;
- Stripe documenta telemetría de interacción/características del dispositivo y
  señales antifraude;
- antes de Live Mode, confirmar el tratamiento permitido para menores y el
  modelo de consentimiento/titularidad del método de pago.

Si esa validación no permite usar PaymentSheet con menores, las opciones de
producto posteriores serían, sin decidirlas aquí:

1. mantener tarjeta deshabilitada para menores;
2. colocar el flujo detrás de una pantalla neutral/adult gate adecuada;
3. no incluir tarjeta en el primer release público;
4. adoptar otra solución compatible.

**Este preflight no modifica la app ni el backend para elegir una de esas
opciones.**

### CameraX / ML Kit

- CameraX controla la cámara;
- ML Kit Barcode Scanning interpreta QR localmente;
- no se encontró envío de frames a un servicio remoto desde este flujo;
- mantener verificación de versiones/términos antes del AAB final.

## Social features

No se activa el bloque de aplicación social:

- `AssistantChatScreen` usa `AssistantLocalReplies` dentro del dispositivo;
- un estudiante no conversa con otro estudiante;
- las notas del pedido se envían al establecimiento para preparar alimentos;
- compartir un receipt usa `ACTION_SEND` del sistema y la app receptora es
  elegida por el usuario.

Por tanto no existe una función principal de chat anónimo, chat con extraños ni
intercambio social dentro de Vaiinilla.

## Checklist de cierre

- [x] Audiencia de producto definida desde secundaria.
- [x] Sin ads / `AD_ID`.
- [x] Sin identificadores Families expresamente prohibidos encontrados.
- [x] Sin teléfono, SMS/call log o GPS.
- [x] IARC técnico preparado.
- [x] Data Safety incluye datos de cuenta, device ID y Stripe.
- [ ] Confirmar términos/uso de Firebase Authentication con usuarios menores.
- [ ] Confirmar Stripe/PaymentSheet para usuarios menores antes de Live Mode.
- [ ] Confirmar base legal/consentimiento aplicable por jurisdicción/escuela.
- [ ] Completar política pública con sección de menores.
- [ ] Reauditar el AAB final por SDKs, permisos e identificadores.
- [ ] Si cambia cualquier SDK o se añaden anuncios/social features, repetir este
      preflight antes de producción.
