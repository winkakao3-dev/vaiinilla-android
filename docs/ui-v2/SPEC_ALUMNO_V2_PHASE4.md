# MASTER SPEC — Vaiinilla Alumno UI V2 Phase 4 (checkout + tracking states)

Branch `feature/alumno-ui-v2-demo-parity`. Match demo HTML 1:1 for **checkout variants (14–18)**, **tracking by state (21–24)**, **product customized sheet (08)**, and **assistant chip variants (10/11)**. UI-local demo checkout only — no Stripe/backend.

## Scope

### 5.1 Checkout `14`–`15` — `CartScreen`
- **Entrega**: selectable `Para llevar` vs `En mesa` (Mesa 12 fixture)
- **Pago**: selectable `Efectivo en Caja`, `Saldo Vaiinilla`, `Tarjeta •••• 4242`
- Summary reflects destination + payment
- Saldo: block confirm if balance < total; debit local `WalletUiState` on success
- Tarjeta/saldo: skip caja session blocker (demo instant pay)

### 5.2 Confirmations `16`–`18` — `OrderConfirmationScreen`
- `16` CASH: existing receipt printer copy
- `17` BALANCE: saldo descontado, comanda enviada
- `18` CARD: tarjeta cargada, comanda enviada

### 5.3 Tracking `21`–`24` — `StudentTrackingScreen`
- State-specific hero above timeline when order selected:
  - `21` PAID — cobrado, cocina recibió
  - `22` PREPARING — preparando
  - `23` READY — listo para recoger / mesa
  - `24` DELIVERED — entregado

### 5.4 Product sheet `08` — `ProductDetailSheet`
- When options differ from defaults: badge `Personalizado`, highlighted selection summary, CTA `Agregar personalizado`

### 5.5 Assistant `10`/`11` — `AssistantScreen`
- Chip-specific hero copy/layout (budget / ligero variants)

## Domain (fixture-only path)
- `PaymentMethod.BALANCE`, `PaymentMethod.CARD` for demo orders
- `OrderRepository.createStudentCheckout` — fixture persists; remote returns unsupported
- `ContractRules.validateStudentCheckoutRequest` — permissive; VAI-10 `validateCreateOrderRequest` unchanged

## Acceptance
- [ ] Cart shows 14/15 layouts when switching entrega/pago
- [ ] Balance checkout debits wallet and lands on 17
- [ ] Card checkout lands on 18
- [ ] Tracking hero changes per state 21–24
- [ ] Product sheet shows 08 customized state
- [ ] Assistant chips change hero for 10/11
- [ ] `./gradlew testDebugUnitTest assembleDebug`
