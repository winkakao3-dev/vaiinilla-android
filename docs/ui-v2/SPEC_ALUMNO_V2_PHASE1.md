# MASTER SPEC — Vaiinilla Alumno UI V2 (demo parity)

Warm editorial product UI for the student (Alumno) path of Vaiinilla Android, matched 1:1 to `~/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html`. Separate from Jesús VAI-11 PR work. Quality bar: demo screenshots and motion must feel identical on Compose.

## 1. Stack & global setup
- Kotlin + Jetpack Compose + Material3 + Hilt (existing project)
- Branch: `feature/alumno-ui-v2-demo-parity`
- Data: keep MOCK/REMOTE repositories; **do not invent backend endpoints** for wallet/assistant — UI can use local fixtures/stubs
- Demo source of truth: `/Users/winkakao/Downloads/Vaiinilla_Demo_Web_IA_CHAT.html`

## 2. Assets manifest
- Extract product images + logos from demo CSS `.asset-*` base64 into `app/src/main/res/drawable-nodpi/`
- Export `vainilla-mark` SVG → vector drawable + adaptive launcher (`mipmap-anydpi-v26`)
- No invented placeholder rectangles; reuse demo assets only

## 3. File structure (Phase 1)
- `tools/extract_demo_assets.py` — one-time extractor
- `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — token parity
- `ui/components/PhysicalPress.kt`, `VaiinillaMark.kt`, `QuickActionCards.kt`, `ActiveOrderBanner.kt`
- `ui/components/VaiinillaBottomNav.kt` — 5 tabs + pill/bounce
- `ui/screens/SplashScreen.kt`
- Polish: `RoleSelectorScreen`, `CatalogScreen`, `CartScreen`, `OrderConfirmationScreen`
- `AndroidManifest.xml` + splash theme + launcher icons

## 4. Design tokens (verbatim from demo)
```
--ink:#171817; --ink-2:#2a2b29; --paper:#f4f1e7; --paper-2:#e9e6da; --muted:#77796f;
--accent:#b9d86d; --accent-2:#d7ef8b; --accent-ink:#1d250c; --coral:#f15b55; --yolk:#ffd15b;
--line:rgba(23,24,23,.12); --shadow:0 18px 46px rgba(19,22,18,.18);
--r-xl:34px; --r-lg:28px; --r-md:20px; --r-sm:14px;
```
Fix Android `Coral` to `#F15B55`. Add `Yolk`, `AccentInk`, `Ink2`, `Line`.

## 5. Phase 1 screens (cash alumno journey only)
Canonical path: `01 → 02 → 07 → 13 → 16 → 20`
1. Splash — bootIconIn 0.45s, bootIconExpand 0.7s scale→2.75, bootSplashOut 0.4s
2. Role selector `01` — hero, watermark mark, role cards, “Entrar como alumno”
3. Menu `02` — greeting, search, chips, quick cards, product grid, bottom nav
4. Product sheet `07` — visual parity with existing sheet
5. Cart `13` — empty + cash/takeaway path (payment picker UI may show disabled non-cash)
6. Confirmation `16` — keep 2.65s receipt printer; add sticker CTA UI (can navigate later)
7. Bottom nav — Menú/Asistente/Pedidos/Cartera/Carrito; Assistent/Wallet can open stub “Próximamente” sheets for now IF not yet built — prefer wiring Pedidos→tracking and Carrito→cart

## 6. Motion budget (≤3 families)
1. Splash expand
2. Nav pill slide + icon bounce (190–430ms)
3. Physical press scale (.965 / .93) 90ms down / 240ms release
Respect `prefers-reduced-motion` / Compose reduced motion.

## 7. Footguns
1. DO NOT modify or push to PR #3 / merge into Jesús delivery docs as “done”
2. DO NOT invent Stripe/wallet/backend APIs — stub UI only
3. DO NOT break MOCK fixtures / unit tests for order create cash
4. DO NOT use Inter/Roboto as brand identity; keep existing Compose font setup unless demo requires specific face already in project
5. DO NOT commit secrets or `local.properties`
6. DO NOT implement Caja/Cocina/Mesero redesign in Phase 1

## 8. Acceptance checklist
- [ ] App icon + splash with vainilla mark
- [ ] Colors match demo hexes (esp. coral/yolk)
- [ ] Role selector looks like screen 01
- [ ] Catalog has quick cards + chips + search parity
- [ ] Bottom nav 5 tabs with active lime pill
- [ ] Cash cart → confirmation printer animation still works
- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew testDebugUnitTest` succeeds

## Closing
Build Phase 1 as a faithful Compose reproduction of the demo alumno cash path. Do not “improve” tokens or skip splash/icon.
