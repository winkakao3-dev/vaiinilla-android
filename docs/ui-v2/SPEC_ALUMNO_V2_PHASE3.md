# MASTER SPEC — Vaiinilla Alumno UI V2 Phase 3 (themes + wallet + stickers)

Warm editorial product UI on `feature/alumno-ui-v2-demo-parity`. Match `/Users/kakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html` 1:1 for **themes (03/04)**, **cartera subflows (26–30)**, and **receipt stickers (51–56)**. No Jesús PR #3 delivery docs. No real payments/Stripe.

Phase 1–2 already cover cash journey + assistant + wallet hub + pedidos. Phase 3 unlocks theme switching and the remaining alumno surfaces still stubbed as ComingSoon.

## 1. Stack & global setup
- Kotlin + Jetpack Compose + Material3 + Hilt (existing)
- Theme via `CompositionLocal` + optional `SharedPreferences`/`DataStore` persistence key `vaiinilla_theme` ∈ {`light`,`dark`,`amoled`}
- Wallet subflows: **local UI state only** (fixture amounts/CLABE/card). Never network.
- Stickers: Compose canvases / styled layouts from demo copy — no new backend. Seed from `createdOrder` when present; else demo fixture `#3472` / Burrito norteño / `$101`.
- Demo source: `/Users/kakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html`

## 2. Assets
- Reuse product drawables + `VaiinillaMark`
- Stickers use typography + shapes + existing product image thumbs — no invented photos
- Theme toggle icon: simple sun/moon/circle glyphs (Compose Icons or Path) — no new raster assets required

## 3. File structure
Create:
- `docs/ui-v2/SPEC_ALUMNO_V2_PHASE3.md` (this file)
- `ui/theme/VaiinillaThemeMode.kt` — enum Light/Dark/Amoled + token resolver
- `ui/theme/LocalVaiinillaColors.kt` — `data class VaiinillaColors` + `LocalVaiinillaColors`
- Update `Theme.kt` / `Color.kt` to expose light/dark/amoled palettes
- `ui/theme/ThemePreferences.kt` — read/write theme preference (SharedPreferences is fine)
- `ui/screens/WalletAddMoneyScreen.kt` — demo `26` (card path) + mode for SPEI `27`
- `ui/screens/WalletPaymentMethodsScreen.kt` — `28`
- `ui/screens/WalletAddCardScreen.kt` — `29`
- `ui/screens/WalletAccountScreen.kt` — `30`
- `ui/screens/ReceiptStickerScreen.kt` — pager/chooser for styles `51–56`
- `ui/components/sticker/StickerStyles.kt` — composables for each sticker variant
- `ui/components/ThemeCycleButton.kt` — cycles light→dark→amoled

Edit:
- `MainActivity.kt` / `Theme.kt` — wrap app with theme mode from preference
- `CatalogScreen` topbar avatar or icon-btn — long-press **or** small theme cycle control (demo switches via tool; Android: cycle button near avatar / in role selector footer)
- Prefer: **RoleSelectorScreen** gets a subtle theme cycle control; **Catalog** avatar long-press also cycles (announce via no Toast required — visual change is enough)
- `WalletScreen.kt` — replace ComingSoon stubs with navigation to real sub-screens
- `Routes.kt` + `AppNavHost.kt` — wallet subroutes + sticker route
- `OrderConfirmationScreen.kt` — CTA “Ver sticker” / existing sticker CTA → `Routes.RECEIPT_STICKER`
- Alumno screens that hardcode `Cream`/`Ink` should prefer `LocalVaiinillaColors.current` for backgrounds/text on main chrome (catalog, assistant, cart, wallet, pedidos, stickers). Operational Caja/Cocina/Mesero **may keep** cream/ink constants this phase (out of scope to restyle ops).

## 4. Design tokens

### Light (existing)
```
paper:#f4f1e7; paper2:#e9e6da; ink:#171817; ink2:#2a2b29; muted:#77796f;
line:rgba(23,24,23,.12); accent:#b9d86d; accent2:#d7ef8b; accentInk:#1d250c;
coral:#f15b55; yolk:#ffd15b
```

### Dark (demo `.dark` / screen 03)
```
paper:#1d1e1c; paper2:#292a27; ink:#f5f1e5; ink2:#dedbd0; muted:#aaa99f;
line:rgba(255,255,255,.12); accent:#b9d86d (keep brand lime; optional lift #c7e87a only if needed for contrast);
accentInk:#182008
```
Splash dark: `#08090B` if splash reads theme.

### AMOLED (demo `.amoled` / screen 04)
```
paper:#000000; paper2:#111111; ink:#f8f5ec; ink2:#dedbd0; muted:#a4a39c;
line:rgba(255,255,255,.15); accentInk:#172008
```
Optional radial lime wash at top-right `rgba(199,232,122,.12)` on root bg for amoled catalog only.

Nav glass may stay dark in all themes (demo nav is dark). Product cards / chips use `paper2`/`ink` from Local colors.

## 5. Sections

### 5.1 Theme system
- `VaiinillaTheme(mode)` provides `LocalVaiinillaColors` + Material colorScheme mapped reasonably (background=paper, onBackground=ink, primary=accent, onPrimary=accentInk, surface=paper2)
- Cycle order: Light → Dark → Amoled → Light
- Persist across process death
- Catalog/Assistant/Cart/Wallet/Pedidos/Confirmation/Stickers backgrounds use Local colors
- Acceptance: switching theme updates paper/ink immediately without restart

### 5.2 Wallet `26` — Add money (card)
Topbar back `Añadir dinero`
- Section `Monto a agregar` with big amount (default `$100`)
- Chip amounts: `$50 $100 $200 $500` (select updates amount)
- Section `¿Cómo quieres agregarlo?`
  - VISA Tarjeta •••• 4242 — “Acreditación inmediata en la demo” (selected by default for 26)
  - SPEI Transferencia — “Usa tu CLABE y referencia personal” → navigates/switches to `27` layout
- Preview card strip: `VISA DANI ÁLVAREZ •••• 4242 · vence 08/29`
- Primary CTA `Agregar al saldo` → updates **local** displayed balance on Wallet hub (hold balance in a simple `WalletUiState` remembered at NavHost level or ViewModel without Hilt if easier: `var walletBalance by rememberSaveable { mutableStateOf(200) }` passed down). Then pop back.
- Disclaimer not required beyond demo feel; no network.

### 5.3 Wallet `27` — Add money SPEI
Same shell; SPEI selected:
- Banco receptor `STP`
- CLABE `646180157034852019` + Copiar (Clipboard)
- Referencia `UTCH241087` + Copiar
- Note exact: `La transferencia no paga el producto directamente. Primero se acredita al saldo y después eliges Saldo al confirmar.`
- CTA `Simular transferencia recibida` → credit selected amount to local balance + pop

### 5.4 Wallet `28` — Payment methods
- Intro copy: `La tarjeta puede pagar un pedido directamente o añadir dinero. La transferencia sólo recarga el saldo.`
- Section Tarjetas + Agregar → `29`
- Card row VISA •••• 4242 / DANI ÁLVAREZ · vence 08/29 / check
- `Agregar método de pago` → `29`
- Section Transferencia SPEI with CLABE/referencia summary
- Footer decision line from demo

### 5.5 Wallet `29` — Add card
- Decorative card preview `VAIINILLA · VISA` with masked number / name / expiry bound to fields
- Fields: Nombre del titular, Número, Vencimiento, CVV (local state only)
- Disclaimer exact: `Interfaz demostrativa. Los datos no se procesan ni se envían a una pasarela bancaria.`
- CTA `Guardar tarjeta` → pop to `28` (may append a second fake card in local list)

### 5.6 Wallet `30` — Mi cuenta
- Avatar DA, `Dani`, `Cuenta de estudiante activa`
- Datos: Matrícula `UTCH-241087`, Correo `dani.alvarez@utch.mx`, Tel `614 555 0187`, Plantel `Campus Chihuahua`
- Código para Caja block with UTCH-241087
- Actividad reciente: SPEI +$100 / Pedido #3411 −$42 (fixture rows)

### 5.7 Stickers `51–56` — `ReceiptStickerScreen`
Entry: Confirmation sticker CTA + optional from Pedidos detail overflow (“Ver sticker”).
UI:
- Topbar `← Tu receipt sticker` + share icon no-op
- Horizontal pager or chip switcher: `XS S M XL` is size for 51; also style chips: `Editorial | Core | Limited | Breakfast | QR Live | Térmico` mapping 51–56
- Default style based on order: Editorial `51` for cash; allow swipe to others (demo gallery)
- Each style is a full-bleed card matching demo hierarchy/copy; pull folio/total/product from `createdOrder` when available

**51 Editorial product sticker:** size chips XS–XXL; fields FECHA/PEDIDO/TOTAL/PAGO/DESTINO; product name; “School edition 01”; instructions footer.
**52 Core drop:** “VAIINILLA RECEIPT STICKER DROP 024 YA ES TUYO.”; PEDIDO PAGADO; product + variants; ORDEN/DESTINO/HORA; Total pagado Saldo/Efectivo; code `VNL-{folio}-{total}MX`; COMMON 01/24.
**53 Limited:** HOT LUNCH limited edition; Montado norteño / serie fuego; RARE; 013/150; reward copy.
**54 Breakfast Club:** AM BREAKFAST CLUB 2026; progress 3/5; line items; code MORNING-…
**55 Live QR:** monospace terminal aesthetic `VNL://ORDER LIVE RECEIPT`; READY TO PICK; fields ORDER_ID/PRODUCT/PAYMENT/DESTINATION/TOTAL; VERIFIED HASH.
**56 Thermal:** cafeteria header; order number big; line items; paid method; thanks Dani; barcode-like blocks (Canvas rectangles — not a real barcode lib required).

Motion: sticker enter scale .96→1 + fade 220ms; pager spring. Reduced motion: skip.

## 6. Cross-section rules
- Wallet balance state shared across hub + add-money (rememberSaveable at NavHost or tiny non-Hilt holder)
- Routes examples:
  - `wallet/add-money?method=card|spei`
  - `wallet/methods`
  - `wallet/add-card`
  - `wallet/account`
  - `receipt-sticker`
- Bottom nav hidden on wallet subflows and sticker (back stack to hub/confirmation) — match demo `no-nav` feel (`padding-bottom` smaller)
- Do not break cash MOCK tests
- Run `./gradlew testDebugUnitTest assembleDebug` after changes (SDK at `/Users/kakao/Library/Android/sdk`, `local.properties` already set, gitignored)

## 7. Footguns
1. DO NOT process real card data or call Stripe
2. DO NOT invent backend wallet endpoints
3. DO NOT merge into Jesús VAI-11 docs as delivery
4. DO NOT restyle Caja/Cocina/Mesero this phase (optional leave Cream constants)
5. DO NOT commit `local.properties` or secrets
6. DO NOT remove Phase 2 assistant/wallet hub
7. DO NOT use Inter/Roboto as brand face
8. Clipboard copy must use Android clipboard — no crash if unavailable

## 8. Acceptance checklist
- [ ] Theme cycles Light/Dark/AMOLED and persists; catalog paper/ink update (demo 03/04 parity)
- [ ] Wallet → Añadir dinero opens 26; switch SPEI shows 27; simulate credits balance
- [ ] Métodos de pago 28 / Agregar tarjeta 29 / Mi cuenta 30 match demo copy
- [ ] Confirmation (or pedidos) opens sticker gallery 51–56 with order folio/total when available
- [ ] Sticker screens are no-nav; back returns
- [ ] `./gradlew testDebugUnitTest` SUCCESS
- [ ] `./gradlew assembleDebug` SUCCESS

Build it as a faithful reproduction of this spec. Do not improve timings, change tokens, or refactor mid-build.
