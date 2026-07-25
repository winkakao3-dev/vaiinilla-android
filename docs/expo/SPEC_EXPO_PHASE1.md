# MASTER SPEC — Vaiinilla Expo Phase 1 (Alumno completo + Firebase)

**Branch:** `cursor/expo-vaiinilla-5c6b`  
**Goal:** Close the “human missing list”: Alumno flows matching HTML SoT, demo gallery, Solo pruebas, Firebase seed + REMOTE when configured. Ops Caja/Cocina/Mesero: **fixture-level workable screens** (list + cash collect for Caja).  
**SoT UI:** `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html` + Android screens on `feature/alumno-ui-v2-demo-parity` as behavior reference.  
**Lead already wrote this SPEC — execute it verbatim.**

Skills: orquestador (this), pera-design (finished controls), expo-router / expo-native-ui as needed.

## Out of Phase 1
- Pixel-perfect every sticker kerning vs HTML (ship 6 styles that are recognizably correct)
- EAS store submit
- Stripe live charges
- Admin reportes

## Architecture (keep Jesús shape)

Extend existing `expo/src/{domain,data,core,screens,components,hooks,app}` — do not invent a parallel stack.

- `DATA_SOURCE=MOCK` / Solo pruebas → fixtures only  
- `REMOTE` + Firebase env → seed login → `sesiones/contexto` → JWT on API calls  
- Money: decimal strings + decimal.js  
- Routes stay thin; screens hold UI

## Routes to add (`src/app`)

```
(student)/
  assistant.tsx
  assistant-chat.tsx
  orders.tsx          # tracking
  wallet.tsx
  wallet-add-money.tsx
  wallet-methods.tsx
  wallet-add-card.tsx
  wallet-account.tsx
  sticker.tsx
demo/gallery.tsx
(ops)/
  caja.tsx
  cocina.tsx
  mesero.tsx
```

Wire bottom nav to real routes (remove “Próximamente” alerts for those tabs).

## Screens / features

### A. Demo gallery
- Role selector **Ver todas las fases** → `/demo/gallery`
- Forces Solo pruebas / MOCK
- Sectioned list jumping to: catalog, empty search, assistant, chat, empty cart, cart cash, confirmation, tracking empty/active, wallet (+ sub), sticker styles 0–5, caja/cocina/mesero
- Seeder helper seeds cart/order/wallet state like Android `DemoGallerySeeder` (port concepts)

### B. Assistant hub + chat (HTML 09, 57)
- Hero: eyebrow `Pide sin pensarlo tanto`, title `¿Qué necesitas hoy?`, chips, recommendations from catalog/filter helpers
- Chat: welcome + 3 suggestion chips + composer; local reply stubs OK (port `AssistantLocalReplies` ideas)

### C. Tracking (HTML 19–24)
- Empty + list/detail with timeline steps (POR COBRAR / PAGO CONFIRMADO / …) matching Android Phase 6 strings
- Seed orders from fixtures / seeder

### D. Wallet (HTML 25–30)
- Hub balance, add money (card/SPEI UI), methods, add card, account
- Local wallet state (remember/async); MOCK debit on balance checkout if you enable student checkout for BALANCE in MOCK

### E. Stickers (51–56)
- Pager or style chips: Editorial, Core, Limited, Breakfast, QR Live, Thermal — port simplified layouts from Android `StickerStyles.kt` / HTML text
- Entry from confirmation + tracking + gallery

### F. Checkout beyond cash (MOCK)
- Entrega: para llevar / en mesa (Mesa 1–6)
- Pago: efectivo / saldo / tarjeta with Uber-ish cards (CASH/SALDO/VISA badges)
- Confirmation copy for BALANCE/CARD matching HTML heads
- REMOTE: keep blocking unsupported student checkout with clear message (like Android)

### G. Ops fixture screens
- **Caja:** list `por_cobrar`, collect cash (amount ≥ total), advance to cobrado in MOCK repo
- **Cocina:** list paid/preparing; mark preparing/ready if easy
- **Mesero:** list ready; mark delivered if easy  
Extend `order-repository` MOCK mutations as needed.

### H. Firebase + REMOTE
- Ensure login → contexto → session store works when `EXPO_PUBLIC_FIREBASE_*` + REMOTE set
- Catalog/orders use HTTP client with Bearer when REMOTE
- Document env in `expo/README.md`
- If Firebase missing: MOCK + login degrade (already); keep

### I. UI parity pass (catalog, roles, cart, confirmation, nav)
- Align copy, spacing, cream/lime/coral tokens, Uber nav already present
- Product images: if still placeholders, try copy any webp/png from Android res or HTML-extracted assets if present under docs; else keep tinted placeholders keyed by product name
- Pera: press scale, finished buttons, no default system chrome look

## Acceptance

- [ ] Bottom nav opens Menu / Asistente / Pedidos / Cartera / Carrito (real screens)
- [ ] Gallery opens from roles and jumps work for ≥12 destinations
- [ ] Assistant + chat usable in MOCK
- [ ] Tracking empty + at least one active seeded state
- [ ] Wallet hub + one subflow
- [ ] Sticker screen with ≥3 styles switchable
- [ ] Cart supports mesa + saldo/tarjeta in MOCK
- [ ] Caja can collect a MOCK cash order
- [ ] `npm run typecheck` green in `expo/`
- [ ] `npx expo export --platform web` succeeds (or document failure)
- [ ] README updated with Phase 1 routes + how to try gallery
- [ ] Android Kotlin still untouched

## Footguns

1. Do NOT rewrite Phase 0 architecture  
2. Do NOT require Firebase to use MOCK  
3. Do NOT claim pixel-perfect stickers if time-boxed — ship recognizable  
4. Do NOT commit/push (lead will)

## Closing

Execute Phase 1 verbatim. Prefer porting behavior from Android `feature/alumno-ui-v2-demo-parity` sources under `/workspace/app` and HTML fragments under `docs/ui-v2/`.
