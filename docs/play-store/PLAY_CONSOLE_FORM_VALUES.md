# Google Play Console — valores de trabajo para Vaiinilla

Fecha de revisión: 2026-08-23

Estado: **CHECKLIST MAESTRA PREPARADA; CARGA EN PLAY CONSOLE PENDIENTE**.

Este archivo concentra los valores que ya pueden trasladarse a Play Console y
separa lo que todavía depende de URLs, credenciales, validación jurídica o
configuración externa. No contiene secretos.

## Store Listing

- App name: **Vaiinilla**
- App or game: **App**
- Category: **Food & Drink / Comida y bebida**
- Default language: **Spanish** — seleccionar locale exacto en Play Console.
- Short description:
  `Consulta el menú y haz pedidos anticipados en cafeterías escolares.`
- Full description: ver `STORE_LISTING.md`.
- Tags: elegir hasta 5 de las opciones reales de Play Console.
- Screenshots: DESCARTADOS los screenshots de prueba anteriores. Pendiente subir los nuevos screenshots finales.

## App content — Privacy policy

- Privacy policy URL: `https://app.vaiinilla.app/legal/privacidad/2026-07` (**CUBIERTO Y VERIFICADO EN VIVO**).
- Devuelta por `/api/v1/publico/legal/vigente` y activa en web SPA.

## App content — Ads

Respuesta:

**No, my app does not contain ads / No contiene anuncios.**

Evidencia Android:

- sin SDK de anuncios;
- sin `AD_ID`;
- sin Firebase Analytics;
- sin banners/interstitials/native ads;
- sin autopromoción publicitaria integrada detectada.

Las compras de alimentos y el checkout no convierten por sí mismos a la app en
una app "con anuncios".

## App content — App access / Sign-in details

Respuesta:

**All or some functionality is restricted / Toda o parte de la funcionalidad
está restringida.**

Usar los access sets preparados en `APP_ACCESS_REVIEW.md`:

1. customer/student review access;
2. staff review access, si los modos operativos están presentes en el release.

Credenciales: **PENDIENTES EXTERNAS; nunca guardar en Git/Linear**.

## App content — Target audience and content

Selección de trabajo:

- [ ] Ages 5 and under
- [ ] Ages 6-8
- [x] Ages 9-12
- [x] Ages 13-15
- [x] Ages 16-17
- [x] Ages 18 and over

Motivo: producto desde secundaria; en México secundaria comienza típicamente a
los 12 años. Ver `TARGET_AUDIENCE.md`.

- Primarily designed for children under 13: **No**.
- Includes children/minors in target audience: **Yes**.
- Designed for adults too: **Yes**.

Consecuencia: Families Policy aplicable. Ver `FAMILIES_COMPLIANCE.md`.

## App content — Content rating / IARC

Completar como app no-juego de comida/pedidos.

Respuestas técnicas preparadas:

- violence/gore: **No**;
- fear/horror: **No**;
- sexual content/nudity: **No**;
- profanity/offensive language: **No**;
- illegal drugs: **No**;
- tobacco/vaping: **No**;
- alcohol: **No** en el release escolar auditado;
- gambling/casino: **No**;
- weapons: **No**;
- hate/discrimination: **No**;
- user-to-user communication: **No**;
- public UGC: **No**;
- precise location sharing: **No**.

Compras: alimentos/bienes físicos. No inventar la clasificación final; guardar
la emitida por IARC después del cuestionario. Ver `IARC_CONTENT_RATING.md`.

## App content — Financial features

Declaración conservadora basada en el alcance implementado:

### Seleccionar

- [x] **Mobile payments and digital wallets**
- [x] **Rewards, points, frequent flier miles, and other incentives**

Motivo de la segunda selección: el contrato Android incluye
`cashback_otorgado`, Wallet reconoce movimientos `cashback` y el producto tiene
superficies de recompensas/stickers. Declararlo evita subdeclarar una función
que ya forma parte del contrato/producto, aunque el cashback pueda ser cero en
ciertos pedidos.

### No seleccionar según la evidencia actual

- [ ] Personal loan direct lender
- [ ] Loan facilitator
- [ ] Payday loans
- [ ] Banking
- [ ] Line of credit
- [ ] Earned wage advances
- [ ] Microfinance banking
- [ ] Money transfer and wire services
- [ ] Buy now, pay later
- [ ] Cryptocurrency wallet
- [ ] Cryptocurrency exchange
- [ ] NFT sales/trading/awards
- [ ] Stock trading and portfolio management
- [ ] Crowdfunding/chit funds
- [ ] Credit monitoring/reporting
- [ ] Financial advice
- [ ] Insurance
- [ ] Other financial feature, salvo que cambie el producto

No seleccionar `My app doesn't provide any financial features`.

## Google Play Billing / Payments policy

Vaiinilla cobra por **alimentos físicos**. Google Play Billing está diseñado y
es obligatorio, con las excepciones de política aplicables, para bienes y
servicios digitales; Google indica expresamente que no debe usarse para compras
de productos físicos como alimentación ni servicios físicos como reparto de
comida.

Por tanto:

- Stripe/efectivo/saldo para la compra de comida física **no requieren Google
  Play Billing** por esa compra física;
- esto no exime las declaraciones Financial features, Data Safety, Stripe ni
  las obligaciones de menores.

Fuente:
https://support.google.com/googleplay/android-developer/answer/9858738

## App content — Account deletion

- Account creation in app: **Yes**.
- In-app deletion path: **Yes / implemented**.
- External deletion URL: **PENDIENTE KAK-47**.
- E2E deletion against disposable production account: **PENDIENTE KAK-48**.

No cerrar la declaración hasta tener la URL web pública y validar E2E.

## App content — Data Safety

Estado: **IN PROGRESS**.

Usar `DATA_SAFETY_FORM.md`.

Puntos ya preparados:

- data collected/shared globally: **Yes**;
- encryption in transit: **Yes, revalidar en AAB final**;
- account deletion: existe in-app, URL externa pendiente;
- independent security review/MASA: **No evidence / No**;
- ads/marketing use: **No** según cliente auditado.

Blockers:

- clasificación contractual `Shared` de proveedores;
- retenciones/regiones;
- menores/consentimiento;
- Firebase/Stripe/provider review;
- decisión Stripe Live Mode.

## Government apps

Respuesta: **No / Not a government app**.

No hay autoridad gubernamental, servicio oficial ni representación de una
agencia pública en el cliente auditado.

## Health apps / Health features

Respuesta: **No / Not a health app**.

No hay funcionalidad clínica, médica, de fitness, Health Connect o datos de
salud.

## Advertising ID

Respuesta de trabajo: **No use of Advertising ID**.

- no `AD_ID` en manifest;
- no SDK de anuncios;
- no acceso a AAID detectado.

## Families preflight

Estado: **IN PROGRESS**.

PASS técnico:

- sin ads/AAID;
- sin identificadores expresamente prohibidos detectados;
- sin GPS;
- sin teléfono/TelephonyManager;
- sin chat entre usuarios;
- contenido apropiado según preflight IARC.

Pendiente antes de producción:

- confirmar Firebase Auth con usuarios menores;
- confirmar Stripe/PaymentSheet con usuarios menores antes de Live Mode;
- confirmar base legal/consentimiento;
- política pública final.

## Orden sugerido al entrar a Play Console

1. Cargar política de privacidad pública cuando KAK-44 esté resuelto.
2. Ads → **No**.
3. App Access → restringida; cargar credenciales demo.
4. Target Audience → `9-12`, `13-15`, `16-17`, `18+`.
5. IARC → trasladar `IARC_CONTENT_RATING.md` y guardar clasificación emitida.
6. Financial features → Mobile payments/digital wallets + Rewards/incentives.
7. Account deletion → añadir URL web cuando KAK-47 esté lista.
8. Data Safety → completar matriz final tras cerrar proveedores/retención.
9. Store Listing → copiar metadata y cargar los 6 screenshots validados.
10. Crear track Internal/Closed y revisar pre-launch report antes de Production.
