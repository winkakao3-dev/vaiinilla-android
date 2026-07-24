# MASTER SPEC — Vaiinilla Alumno UI V2 Phase 2 (demo parity)

Warm editorial product UI for the student (Alumno) path. Match 1:1 to `/Users/kakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html`. Branch `feature/alumno-ui-v2-demo-parity` only — never touch Jesús PR #3 delivery docs as “done”. Quality bar: bottom-nav tabs that still open “Próximamente” must become real demo screens; empty + tracking states must match demo copy/layout.

Phase 1 already shipped splash/01/02/07/13/16 + nav chrome. Phase 2 completes the remaining **alumno tab destinations** and empty/active states: **09, 57, 12, 19, 20, 25, 05 banner, 06 empty search**.

## 1. Stack & global setup
- Kotlin + Jetpack Compose + Material3 + Hilt (existing)
- Reuse tokens in `ui/theme/Color.kt` (Ink/Cream/Lime/Coral/Yolk/…)
- Reuse `VaiinillaBottomNav`, `PhysicalPress`, `VaiinillaMark`, `ActiveOrderBanner`, `ProductImage`
- Data: **no new backend endpoints**. Assistant replies = local canned logic from catalog fixtures. Wallet = hard-coded demo fixture UI (`$200`, `UTCH-241087`, VISA •••• 4242). Orders/tracking keep existing `OperationalViewModel` / order models.
- Demo path on this machine: `/Users/kakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html`

## 2. Assets manifest
- Reuse existing `drawable-nodpi` product assets + `VaiinillaMark`
- Triangle mascot for assistant hero (09): Compose `Canvas` / `Path` lime triangle with eyes+smile matching demo SVG (no inventing photos)
- Wallet watermark `$` is text, not an image
- No invented placeholder rectangles for product thumbs — use `ProductImage` / existing drawables (`torta`, `burrito_norteno`, `quesa`, etc.)

## 3. File structure (create / edit)
Create:
- `docs/ui-v2/SPEC_ALUMNO_V2_PHASE2.md` (this file — already written by lead)
- `app/src/main/java/com/vaiinilla/app/ui/screens/AssistantScreen.kt` — demo `09`
- `app/src/main/java/com/vaiinilla/app/ui/screens/AssistantChatScreen.kt` — demo `57`
- `app/src/main/java/com/vaiinilla/app/ui/screens/WalletScreen.kt` — demo `25`
- `app/src/main/java/com/vaiinilla/app/ui/components/DemoEmptyState.kt` — shared empty block matching `.empty`
- `app/src/main/java/com/vaiinilla/app/ui/components/OrderTrackingCard.kt` — dark task-card + timeline parity for `20`
- `app/src/main/java/com/vaiinilla/app/ui/assistant/AssistantLocalReplies.kt` — canned reply map (no network)

Edit:
- `Routes.kt` — add `ASSISTANT`, `ASSISTANT_CHAT`, `WALLET` (keep `STUDENT_TRACKING` as Pedidos)
- `AppNavHost.kt` — wire routes; remove ComingSoon for assistant/wallet from catalog/cart
- `CatalogScreen.kt` — empty search `06`; show `ActiveOrderBanner` when `createdOrder` or selected tracking order exists; “No sé qué pedir” → assistant
- `CartScreen.kt` — empty state `12` when cart empty (title “Tu pedido”, CTA “Ver menú”); bottom nav assistant/wallet → real routes
- `StudentTrackingScreen.kt` — redesign to `19`/`20` with topbar “Mis pedidos”, demo empty, dark task card, timeline, summary, **bottom nav** (active Pedidos)
- `OrderConfirmationScreen.kt` / nav — sticker CTA may open chat or tracking; keep receipt printer
- Drop / stop using `ComingSoonSheet` for Asistente & Cartera on alumno path (file may remain for other stubs)

## 4. Design tokens (verbatim — already in Color.kt)
```
--ink:#171817; --ink-2:#2a2b29; --paper:#f4f1e7; --paper-2:#e9e6da; --muted:#77796f;
--accent:#b9d86d; --accent-2:#d7ef8b; --accent-ink:#1d250c; --coral:#f15b55; --yolk:#ffd15b;
--line:rgba(23,24,23,.12); --r-xl:34px; --r-lg:28px; --r-md:20px; --r-sm:14px;
```
Additional surfaces from demo CSS:
- Assistant hero card: bg `#171817`, text `#f6f1e5`, radius `32dp`, pad `24dp`
- Task card dark: bg `#1c1d1b`, text `#f5f2e8`, radius ~`24–28dp`
- Status badge: pad `8×10`, radius `12dp`, bg `rgba(255,255,255,.45)`, 10sp black letter-spacing `.08em`
- Timeline current dot: Yolk + 5dp halo; done dot: Lime; idle: CreamDeep
- Balance card: Lime bg, AccentInk text, radius `32dp`, balance `50sp` weight 950
- Empty: CreamDeep bg, radius `28dp`, icon box `72dp` Lime radius `25dp`
- AI chat bg: radial lime glow at top + Cream; composer CreamDeep radius `22dp`; send Lime `40×40` radius `14dp`
- Chat suggestion chips: minHeight `42dp`, pill, border `1.5dp` lime-mix, weight 800

## 5. Sections

### 5.1 Assistant hub `09` — `AssistantScreen`
**One job:** quick intent chips + recommendations; entry to chat.

Layers (top→bottom):
1. Topbar title `Asistente Vaiinilla` + optional bell icon-btn (no-op or toast-less)
2. Hero card dark: eyebrow `Pide sin pensarlo tanto` (Muted/opacity), lime triangle mascot (~92×82), headline centered `¿Qué necesitas hoy?` ~28–32sp black
3. Chips row (scroll): `Rápido y llenador` (default active), `Menos de $60`, `Algo ligero`, `Combo con bebida` — selecting filters local recommendation list only
4. Section head: `Te recomendamos` / `Según tu elección`
5. Recommendation rows (paper-2, radius 22, pad 9): thumb 68, title, meta `N–M min · Comida`, price bold — tap opens product sheet via callback **or** adds intent to navigate catalog with product id if sheet host is catalog-only; prefer callback `onOpenProduct(productId)` / navigate catalog
6. Primary text button / link under hero area: open full chat `57` — also a visible control “Chatear” or tapping headline area; demo uses separate screen via tools — wire explicit “Abrir chat” chip or icon in topbar
7. Bottom nav active **Asistente**

Named motion:
- `assistantHeroEnter`: opacity 0→1, translateY 12→0, 280ms easeOut, once
- Chip press via existing `physicalPress`
Reduced motion: skip enter translation.

Recommendations fixture (match demo names/prices when catalog has them; else closest catalog products):
- Torta de jamón $45 / Burrito norteño $64 / Quesadillas $40 — map by name contains from `CatalogRepository` / order state products; if missing, still show row with existing drawable.

### 5.2 Assistant chat `57` — `AssistantChatScreen`
**One job:** conversational stub over local canned replies.

Layers:
1. Header: mark ✦ in lime-tint square 34/radius 13, title `Asistente Vaiinilla`, clear + close icon buttons
2. Welcome (when transcript empty): `VaiinillaMark` ~132×110, greeting copy exact, suggestion chips:
   - `¿Qué es bueno sin gluten?`
   - `Algo ligero y fresco`
   - `¿Qué recomiendas?`
3. Transcript (when messages exist): user bubbles right ink-on-lime-soft; assistant left paper-2; 14sp; radius 18; pad 12; gap 10
4. Composer absolute bottom: input placeholder `Pregúntame sobre el menú…`, mic icon (no-op), send Lime

Local reply rules (`AssistantLocalReplies.kt`):
- gluten → mention items without wheat if detectable else “Prueba frutas o jamaica; confirma alérgenos en cocina.”
- ligero/fresco → suggest jamaica / fruta / lighter items from catalog names
- recomienda / default → suggest burrito norteño or first available comida + price string from catalog
- Always 1–3 short Spanish sentences; never invent Stripe/wallet APIs
Clear resets transcript + shows welcome. Close → pop back (assistant hub or catalog).

Motion:
- `chatSendPop`: new bubble scale .94→1 + fade 180ms
- Respect reduced motion

Bottom nav: active Asistente (composer above nav — leave ~72dp+composer clearance; demo has nav under chat — replicate: screen-body flex column, nav outside ai-chat like other screens)

### 5.3 Wallet `25` — `WalletScreen`
**One job:** show cartera chrome with fixture numbers; secondary actions stub sheets OK.

Layers:
1. Topbar `Cartera`
2. Balance card Lime: watermark `$` huge low-opacity absolute, eyebrow `Saldo disponible`, balance `$200`, small `UTCH-241087`, button `Añadir dinero` (light/on-lime) → ComingSoonSheet “Añadir dinero”
3. Row of 3 wallet-actions (paper-2 radius 19): Añadir dinero / Métodos de pago / Mi cuenta — Métodos & cuenta → ComingSoonSheet with demo subtitles
4. Mini grid: `$29` Cashback acumulado · `1` Pedidos realizados
5. Section `Métodos de pago` + Administrar (stub): VISA •••• 4242 selected check; optional SPEI row
6. Section `Movimientos` (if present in demo remainder): 2–3 fake rows with icon boxes — keep minimal if time; at least one “Pedido #3472 −$101” and “Recarga +$200”
7. Bottom nav active **Cartera**

No real payments.

### 5.4 Empty cart `12` — inside `CartScreen`
When `cartLines` empty:
- Topbar `Tu pedido` (not “Carrito” if demo says Tu pedido)
- Empty block: cart icon in lime 72 box, `Tu pedido está vacío`, `Agrega algo del menú para empezar.`, primary `Ver menú` → catalog
- Bottom nav active Carrito
When non-empty keep Phase 1 cash checkout UI.

### 5.5 Pedidos `19`/`20` — rebuild `StudentTrackingScreen`
List mode (no selection / no orders): match `19`
- Topbar `Mis pedidos`
- Empty: receipt icon, `Sin pedidos activos`, `Cuando confirmes uno aparecerá aquí.`, primary `Pedir algo` → catalog
- If orders exist: list dark task-cards (folio, status badge, meta $ / destino / pago) — tap selects

Detail mode (selected): match `20`
- Same topbar
- Dark task-card: eyebrow `Pedido actual`, `#FOLIO`, status badge uppercase, meta `$total`, destino, método
- Section `Seguimiento` / `Actualización en vivo`
- Timeline 5 steps: POR COBRAR → COBRADO → PREPARANDO → LISTO → ENTREGADO with demo microcopy; mark done/current from `OrderStatus`
- Section `Resumen` summary rows + total
- Bottom nav active **Pedidos**
Remove “Ventanas 20–24…” debug copy and “Roles” escape — use avatar/back only if needed via long-press or keep subtle back to role selector in avatar if catalog has it; prefer consistency with menu avatar pattern.

### 5.6 Catalog polish `05`/`06`
- If `createdOrder != null` (or operational selected client order): show `ActiveOrderBanner` under topbar (status label from order state; destination Para llevar / En espacio)
- Search with no matches: empty in grid `No encontramos eso` / `Prueba otra palabra o categoría.` / `Limpiar búsqueda`
- “No sé qué pedir” → navigate Assistant
- Wire bottom nav Asistente → `Routes.ASSISTANT`, Cartera → `Routes.WALLET` (not ComingSoon)

### 5.7 Signature / craft (Pera, constrained to product UI)
Fingerprint: keep existing selection-less Compose focus rings; physical press on chips/rows/CTA; empty icon boxes use Lime not gray. Motion budget ≤3 families: (1) nav pill existing (2) physical press (3) chat/assistant enter micro. No new scroll-scrub signatures (this is app chrome, not marketing landing).

## 6. Cross-section rules
- All new alumno screens include `VaiinillaBottomNav` with correct `selected` tab and same callbacks: Menu→catalog, Assistant→assistant, Orders→tracking, Wallet→wallet, Cart→cart
- Navigation: single-top; from tabs use `navigate(route) { launchSingleTop=true }` without clearing back stack excessively; from empty CTAs pop to catalog
- Breakpoints: phone only; horizontal pad ~20dp like Phase 1
- Reduced motion: `LocalAccessibilityManager` / `remember` check — skip enter translations and chat pop scale

## 7. Footguns
1. DO NOT modify/merge into Jesús VAI-11 PR narrative or change `docs/VAI-11_*` as delivery
2. DO NOT invent backend/Stripe/Firebase for wallet or assistant
3. DO NOT break cash create-order MOCK tests / fixtures
4. DO NOT redesign Caja/Cocina/Mesero in this phase
5. DO NOT replace Phase 1 receipt printer timing (2.65s)
6. DO NOT use Inter/Roboto as brand; keep existing Type.kt
7. DO NOT commit `local.properties` or secrets
8. DO NOT leave Asistente/Cartera on ComingSoon from Catalog/Cart nav
9. DO NOT show English debug strings (“Ventanas 20–24”, “Polling local…”) on Pedidos

## 8. Acceptance checklist
- [ ] Tab Asistente opens hub `09` (hero + chips + recommendations), not ComingSoon
- [ ] From hub (or control) can open chat `57` with welcome + 3 suggestion chips + composer
- [ ] Sending a chip/message appends user+assistant bubbles via local replies; Clear restores welcome
- [ ] Tab Cartera opens `25` with `$200` balance card and action row
- [ ] Empty cart shows `12` copy + Ver menú
- [ ] Pedidos empty shows `19` copy; with order shows dark card + timeline like `20`
- [ ] Catalog shows ActiveOrderBanner when there is a created/active order
- [ ] Catalog empty search matches `06` copy
- [ ] Bottom nav works from Assistant/Wallet/Orders/Cart/Catalog
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew assembleDebug` passes (if SDK available; otherwise compile Kotlin sources clean)

Build it as a faithful reproduction of this spec. Do not improve timings, change tokens, or refactor mid-build.
