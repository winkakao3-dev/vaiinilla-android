# MASTER SPEC — Alumno UI V2 Phase 6 (full HTML parity pass)

**Branch:** `feature/alumno-ui-v2-demo-parity`  
**Goal:** Close remaining Alumno gaps against the **full** demo HTML now in-repo.  
**Lead already wrote this SPEC — execute it verbatim.**

## Source of truth

| Path | Role |
|------|------|
| `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html` | **Primary SoT** (full file, ~1.2MB) |
| `/tmp/phase6-gap-vs-full-html.md` | Gap audit (do these 12 fixes) |
| Phase 5 SPEC | Already done — do **not** re-litigate |

Alumno only: **01–30, 51–57**. Ignore ops **31–50**. Deprioritize **02A** apple-menu.

---

## Fixes (do all 12)

### 1. Payment-aware tracking timeline step 1 — `OrderTrackingCard.kt`
- Pending/cash: `POR COBRAR` / `Caja espera el pago en efectivo.`
- Paid via saldo/tarjeta (order already past pending): step 1 = `PAGO CONFIRMADO` / `Saldo descontado y pedido enviado.`
- Keep steps 2–5 as Phase 5 HTML strings; mesa LISTO message when `IN_SPACE`.

### 2. Tracking layout — `StudentTrackingScreen.kt`
HTML structure for 20–24: **task-card → Seguimiento → timeline → Resumen**.  
**Remove** (or gate off) `OrderStateTrackingHero` so the selected-order view matches HTML (no extra hero card above).

### 3. LISTO task-card yolk — `OrderTrackingCard.kt`
When `OrderState.READY`, task-card background = **Yolk** (HTML `task-card yellow`), not always dark `#1C1D1B`. Adjust text contrast (ink on yolk).

### 4–6. Confirmation chrome — `OrderConfirmationScreen.kt`
- Printer status: `IMPRIMIENDO STICKER…` / `STICKER LISTO` (all payment methods 16–18).
- Primary CTA: **`Seguir pedido`** (replace `Ver seguimiento`).
- After print: collection unlock strip with:
  - `Creando tu comprobante coleccionable`
  - A style unlocked line (pick one matching payment/style: e.g. `Receipt editorial desbloqueado` / `Vaiinilla Core añadido…` / `Live Receipt añadido…` — match HTML screens 16–18)
  - `Se guardó automáticamente en tu colección.`

### 7. Checkout payment chrome — `CheckoutComponents.kt` (`CheckoutPaymentPicker`)
Match HTML 13–15:
- Brand badges: `CASH` / `SALDO` / `VISA`
- Titles like `Efectivo en Caja`, `Saldo Vaiinilla · $200` (use wallet balance), `Tarjeta •••• 4242`
- Subtitles per HTML (cashback / available / cargo inmediato)
- Footer help: transferencia only for adding money to balance (exact copy from HTML 13–15)

### 8. Mesa picker — `CartScreen.kt` + fixtures/domain as needed
When destination `IN_SPACE`, allow choosing **Mesa 1–6** (HTML). Stop hardcoding only `Mesa 12`. Persist selected space id into checkout (`DemoCheckoutFixtures` may expand to list 1–6). Default can be Mesa 4 or 1 to match a common HTML screen — prefer **Mesa 4** if HTML 23 uses it, else first option.

### 9. Assistant hero contrast — `AssistantScreen.kt`
Hero title color: use `colors.paper` (hero surface is `colors.ink`). Remove hardcoded `Color(0xFFF6F1E5)`.

### 10. Product sheet CTA — `ProductDetailSheet.kt`
Always CTA `Agregar · $…` (HTML 07/08). Do **not** rename to `Agregar personalizado` when customized. Keep Personalizado badge / highlight if present. Optional: estimated time as range `8–10 min` when product has that meta.

### 11. Active order banner — `ActiveOrderBanner.kt`
- Eyebrow: `Pedido activo` (not `PEDIDO ACTIVO`)
- PREPARANDO badge: yolk background like HTML `status-badge preparing`

### 12. Cart warning banners — `CartScreen.kt`
Replace fixed `#FFF1CC` / `#FFDED9` with theme-aware washes (yolk/coral on `LocalVaiinillaColors`).

---

## Roborazzi

After UI fixes:
1. Re-record existing stems that change: at least `05_assistant_hub`, `11_cart_mesa_saldo`, `12_cart_tarjeta`, `13_confirm_cash`, `15_tracking_por_cobrar`, `16_tracking_preparando`.
2. Add if missing: `21_tracking_pagado` (PAID / balance timeline with `PAGO CONFIRMADO` step 1), `22_confirm_saldo` (BALANCE confirmation with sticker strings + unlock strip).

Update `docs/ui-v2/ROBORAZZI.md`.

```bash
./gradlew :app:recordRoborazziDebug --no-daemon
./gradlew :app:verifyRoborazziDebug --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

---

## Do not

- Touch ops 31–50 / OperationalScreens redesign
- Revert Phase 5 assistant hero / confirmation **heads** (those already match)
- Implement 02A apple-menu theme
- Commit or push (lead will)
- Invent copy not in the HTML

---

## Acceptance

- [ ] All 12 fixes landed and match HTML wording
- [ ] No `OrderStateTrackingHero` on student tracking selected view
- [ ] Mesa 1–6 selectable for En mesa
- [ ] Roborazzi verify + unit tests + assembleDebug green
- [ ] Report files changed + absolute PNG paths (new/updated)

## Closing

Execute verbatim. Prefer HTML over older SPECs on any conflict.
