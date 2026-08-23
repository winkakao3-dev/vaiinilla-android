# Google Play IARC Content Rating — Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **RESPUESTAS TÉCNICAS PREPARADAS; CUESTIONARIO PLAY PENDIENTE**.

Todas las apps publicadas en Google Play deben completar el cuestionario IARC.
La clasificación final la asigna IARC según las respuestas y la región, por lo
que este documento no inventa una etiqueta final.

Fuente oficial:

- https://support.google.com/googleplay/android-developer/answer/9898843
- https://support.google.com/googleplay/android-developer/answer/9859655

## Naturaleza de la app

Vaiinilla es una **aplicación de comida y bebida / pedidos de alimentos
físicos**, no un juego.

Funciones auditadas:

- selección de cafetería/establecimiento;
- catálogo de alimentos;
- personalización de pedido;
- carrito y checkout;
- seguimiento/historial;
- saldo Vaiinilla;
- cuenta de usuario;
- modos operativos autorizados para Caja, Cocina y Mesero;
- asistente local para recomendaciones del catálogo.

## Respuestas de trabajo para contenido

| Área | Respuesta propuesta | Evidencia |
| --- | --- | --- |
| Violencia | **No** | Sin contenido o mecánicas violentas detectadas. |
| Sangre / gore | **No** | No existe. |
| Miedo / horror | **No** | No existe. |
| Contenido sexual / desnudez | **No** | No existe. |
| Lenguaje ofensivo / profanidad | **No** | No hay contenido de este tipo en el cliente. |
| Drogas ilegales | **No** | No existe. |
| Tabaco / vapeo | **No** | No existe. |
| Alcohol | **No** | No existe en el cliente/catálogo fixture auditado; el release escolar no debe promocionarlo. |
| Apuestas / casino | **No** | No existe. |
| Simulación de apuestas | **No** | No existe. |
| Armas | **No** | No existe. |
| Contenido discriminatorio / odio | **No** | No existe. |
| Ubicación precisa compartida | **No** | Sin permisos/API GPS. “Mesa/espacio” es contexto del establecimiento, no geolocalización. |
| Comunicación usuario-a-usuario | **No** | No existe chat entre usuarios. |
| UGC público | **No** | Usuarios no publican contenido visible a otros usuarios. |
| Notas de usuario | **Sí, privadas/funcionales** | Notas de cocina se transmiten al establecimiento para procesar el pedido; no son contenido público/social. |
| Fotos subidas | **Solo staff autorizado** | Foto de producto puede subirse al catálogo por rol operativo; no es una red social ni feed público de usuarios. |
| Compartir fuera de la app | **Sí, iniciado por usuario** | Receipt sticker usa el share sheet del sistema; no crea comunicación interna entre usuarios. |
| Asistente/chat | **Local, no social** | `AssistantLocalReplies` responde localmente según catálogo; no conecta usuarios entre sí. |

## Compras y dinero

Vaiinilla permite comprar **bienes físicos (alimentos)** y maneja saldo por
establecimiento. También integra Stripe PaymentSheet en Test Mode.

Si el cuestionario IARC pregunta por compras:

- responder que existen transacciones/pedidos de bienes físicos cuando la
  pregunta abarque compras reales;
- no declarar compras de bienes digitales si la pregunta se refiere a contenido
  digital o compras dentro de juegos;
- no declarar apuestas, premios monetarios o loot boxes;
- cashback/recompensas del producto no equivalen por sí solos a apuestas.

La declaración de **Financial features** de Google Play es un formulario
separado y ya está cubierta en `DATA_SAFETY_FORM.md`.

## Contenido dinámico de cafeterías

Los productos, nombres, imágenes y disponibilidad pueden provenir del catálogo
del establecimiento. Por diseño de producto, el release para cafeterías
escolares debe mantener ese catálogo apropiado para la audiencia declarada.

Antes del release público:

- [ ] confirmar que los establecimientos de producción no publican alcohol,
      tabaco, productos restringidos ni imágenes inapropiadas;
- [ ] confirmar que los assets promocionales y screenshots cumplen la misma
      expectativa;
- [ ] si el producto futuro permite contenido diferente, volver a completar el
      cuestionario IARC cuando cambie la naturaleza del contenido.

## Resultado esperado, no declaración final

Con el contenido Android actualmente auditado, no se observan factores de
violencia, sexo, drogas, lenguaje, apuestas u horror que eleven la madurez del
contenido. Es razonable esperar una **clasificación baja / apta para audiencia
amplia**, pero la etiqueta exacta (por ejemplo ESRB/PEGI u otra regional) debe
ser la emitida por IARC después de completar Play Console.

## Checklist para completar IARC

- [x] App identificada como no-juego.
- [x] Auditoría de contenido sensible en cliente realizada.
- [x] Comunicación usuario-a-usuario descartada.
- [x] UGC público descartado.
- [x] Compras identificadas como bienes físicos, no bienes digitales de juego.
- [ ] Completar cuestionario en Play Console con estas respuestas.
- [ ] Guardar certificado/clasificación IARC emitida.
- [ ] Comparar la clasificación emitida con Target Audience antes de producción.
