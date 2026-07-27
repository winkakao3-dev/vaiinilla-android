# MASTER SPEC — Alumno UI V2 Phase 5 (final HTML parity)

**Branch:** `feature/alumno-ui-v2-demo-parity`  
**Goal:** Close remaining gaps so Alumno Compose matches the demo HTML **1:1** for screens **01–30 + 51–57** (alumno path only).  
**Lead already wrote this SPEC — execute it verbatim.**

## Source of truth (CRITICAL)

Full Mac file `/Users/kakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html` is **not** on this cloud VM. Use recovered fragments:

| Path | Role |
|------|------|
| `docs/ui-v2/demo-screens/screen-XX.html` (and `.txt` for stickers) | **Primary SoT** for copy + structure |
| `docs/ui-v2/SPEC_ALUMNO_V2_PHASE{1,2,3,4}.md` | Background only — **HTML fragments win** on conflicts |
| `/tmp/alumno-ui-gap-report.md` | Gap inventory (already audited) |

Do **not** invent new copy. Diff against `docs/ui-v2/demo-screens/` before editing strings.

Out of scope: Caja/Cocina/Mesero/Admin (`31–50`), Jesús VAI-11 delivery, Firebase auth, real payments.

---

## Work package A — Copy / structure parity (HTML wins)

### A1. Assistant hub `09` / chips `10`/`11` — `AssistantScreen.kt`

Demo HTML (all of 09/10/11) uses the **same hero**:

- Eyebrow: `Pide sin pensarlo tanto` (muted `#9fa19a` / `colors.muted`)
- Mascot triangle
- Title: `¿Qué necesitas hoy?` (centered)
- **No** hero subtitle paragraph
- **No** “Chatear” text link inside the hero (chat opens via topbar icon only)
- Chips unchanged: `Rápido y llenador` | `Menos de $60` | `Algo ligero` | `Combo con bebida`
- Section: `Te recomendamos` / `Según tu elección`

**Remove** chip-specific hero variants (`MODO PRESUPUESTO`, `MODO LIGERO`, `MODO COMBO`, `RECOMENDACIÓN RÁPIDA`). Chips only change the **recommendation list**.

Recommendation lists when catalog has matching names (prefer name-contains mapping; fall back to filter helpers already in `AssistantLocalReplies`):

| Chip | Demo preference order (use available fixture products) |
|------|--------------------------------------------------------|
| Rápido y llenador | Torta de jamón, Burrito norteño, Quesadillas |
| Menos de $60 | Waffle…, Torta…, Quesadillas… (demo prices) — filter ≤60 from catalog |
| Algo ligero | Vaso de fruta / Agua de jamaica / light items from catalog |
| Combo con bebida | Keep existing combo filter logic |

If a demo product is missing from fixtures, skip that row; do not invent fake products.

### A2. Confirmations `16`–`18` — `OrderConfirmationScreen.kt` `confirmationCopy`

Align exactly to demo receipt heads:

| Method | eyebrow | title | subtitle |
|--------|---------|-------|----------|
| CASH | `PEDIDO CREADO` | `Tu pase de Caja acaba de salir.` | `Págalo en efectivo y usa este receipt sticker para identificar la orden.` |
| BALANCE | `PAGO CONFIRMADO` | `Tu compra se volvió un sticker.` | `El saldo fue descontado y Cocina ya recibió la comanda.` |
| CARD | `TARJETA AUTORIZADA` | `Tu comprobante digital está saliendo.` | `La compra fue autorizada y el pedido ya llegó a Cocina.` |

### A3. Tracking timeline + heroes — `OrderTrackingCard.kt` / `CheckoutComponents.kt`

From demo screens `20`–`24` timeline labels/messages (match these strings):

1. `POR COBRAR` — `Caja espera el pago en efectivo.`
2. `COBRADO` — `Cocina recibió la comanda.`
3. `PREPARANDO` — `Tu comida se está preparando.`
4. `LISTO` — `Recógelo en la barra.` **OR** for mesa destination: `El mesero lo llevará a tu mesa.`
5. `ENTREGADO` — `Pedido completado.`

Current step: **Yolk fill + 5dp soft halo** (missing today).

`OrderStateTrackingHero` copy should stay coherent with those states (can keep existing hero cards if already close; prefer demo timeline wording above as source for step rows). Cross-check `screen-20.html`…`screen-24.html`.

### A4. Chat `57` — `AssistantChatScreen.kt`

Welcome / chips from `screen-57.html`:

- Welcome: `¡Hola! Soy tu Asistente Vaiinilla. Pregúntame sobre el menú: dietas, recomendaciones, ingredientes y más.`
- Suggestion chips: `¿Qué es bueno sin gluten?` · `Algo ligero y fresco` · `¿Qué recomiendas?`
- Composer placeholder: `Pregúntame sobre el menú…` (demo also shows `Mensaje para el asistente` as label — keep placeholder matching current if already close; do not add a second conflicting label)

---

## Work package B — Theme token hardening

Replace hardcoded light `Cream` / `Ink` / `CreamDeep` / `MutedInk` / `AccentInk` with `LocalVaiinillaColors.current` (`paper`, `ink`, `paper2`, `muted`, `accentInk`, etc.) on **alumno** surfaces:

- `AssistantChatScreen.kt`
- `SplashScreen.kt` — use `colors.paper` (optional dark splash `#08090B` only if theme is Dark/Amoled; Light stays cream paper)
- `OrderTrackingCard.kt`
- `ActiveOrderBanner.kt`
- `QuickActionCards.kt`
- `DemoEmptyState.kt`
- `RoleSelectorScreen.kt` leftover hardcoded Cream/Ink where `LocalVaiinillaColors` should drive
- `VaiinillaBottomNav.kt` if still hardcoding Ink for inactive states incorrectly

**Do not** rewrite OperationalScreens theme in this pass.

Lime / Coral / Yolk brand accents may stay as named colors when they are brand, not paper/ink.

---

## Work package C — Roborazzi matrix expansion

Extend `AlumnoScreenshotTest` (+ fixtures helpers) and **record** baselines under `app/src/test/roborazzi/`. Keep existing 01–06 names; add:

| Stem | Screen / state |
|------|----------------|
| `07_catalog_empty_search` | Catalog search with no hits (`06`) |
| `08_assistant_default` | Assistant chip rápido (hero fixed) |
| `09_assistant_budget_chip` | Chip Menos de $60 (same hero, different list) |
| `10_cart_empty` | Empty cart (`12`) |
| `11_cart_mesa_saldo` | En mesa + saldo (`14`) |
| `12_cart_tarjeta` | Tarjeta (`15`) |
| `13_confirm_cash` | Confirmation CASH (`16`) — static printed if possible |
| `14_tracking_empty` | Pedidos empty (`19`) |
| `15_tracking_por_cobrar` | Tracking PENDING (`20`) |
| `16_tracking_preparando` | PREPARING (`22`) |
| `17_wallet_add_money` | Wallet add money (`26`) |
| `18_chat_welcome` | Assistant chat welcome (`57`) |
| `19_catalog_dark` | Catalog under `VaiinillaTheme(Dark)` (`03`) |
| `20_sticker_receipt` | One sticker style (`51` or default) |

Update `docs/ui-v2/ROBORAZZI.md` with the new stems.

Commands must pass:

```bash
./gradlew :app:recordRoborazziDebug --no-daemon
./gradlew :app:verifyRoborazziDebug --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

---

## Work package D — Light cleanup

- Leave `ComingSoonSheet.kt` unless unused and easy to delete without nav breakage; optional delete if zero references.
- Do not touch ops roles.

---

## Files likely touched

- `app/src/main/java/com/vaiinilla/app/ui/screens/AssistantScreen.kt`
- `app/src/main/java/com/vaiinilla/app/ui/screens/AssistantChatScreen.kt`
- `app/src/main/java/com/vaiinilla/app/ui/screens/OrderConfirmationScreen.kt`
- `app/src/main/java/com/vaiinilla/app/ui/screens/SplashScreen.kt`
- `app/src/main/java/com/vaiinilla/app/ui/screens/RoleSelectorScreen.kt` (only token leftovers)
- `app/src/main/java/com/vaiinilla/app/ui/components/OrderTrackingCard.kt`
- `app/src/main/java/com/vaiinilla/app/ui/components/CheckoutComponents.kt` (timeline/hero strings if needed)
- `app/src/main/java/com/vaiinilla/app/ui/components/ActiveOrderBanner.kt`
- `app/src/main/java/com/vaiinilla/app/ui/components/QuickActionCards.kt`
- `app/src/main/java/com/vaiinilla/app/ui/components/DemoEmptyState.kt`
- `app/src/main/java/com/vaiinilla/app/ui/assistant/AssistantLocalReplies.kt` (if filter lists need tweaks)
- `app/src/test/java/com/vaiinilla/app/ui/screenshot/*`
- `app/src/test/roborazzi/*.png`
- `docs/ui-v2/ROBORAZZI.md`
- `docs/ui-v2/SPEC_ALUMNO_V2_PHASE5.md` — keep this SPEC in repo (already this file)

Avoid drive-by refactors outside the list.

---

## Acceptance checklist

- [ ] Assistant 09/10/11 hero matches HTML (same eyebrow+title; chips only change recommendations; no Chatear in hero)
- [ ] Confirmation 16/17/18 copy matches table above
- [ ] Tracking timeline strings + yolk 5dp halo
- [ ] Chat 57 welcome + suggestion chips match HTML
- [ ] Alumno chrome uses `LocalVaiinillaColors` (no light-only hardcode on listed files)
- [ ] Roborazzi baselines expanded + `verifyRoborazziDebug` green
- [ ] `testDebugUnitTest` + `assembleDebug` green
- [ ] Report: files changed + absolute PNG paths for new baselines

## Closing command

Execute verbatim. Prefer HTML fragment copy over older Phase 2/4 SPEC wording when they conflict. Do not expand to ops 31–50. Do not commit or push (lead will).
