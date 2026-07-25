# MASTER SPEC — Vaiinilla Expo (warm editorial · Jesús method)

Replicate Vaiinilla for **Expo (React Native)** with the same internal architecture and delivery method Jesús used on Android (bóveda contracts → domain/data/ui → MOCK|REMOTE → phased VAI), **HTML 1:1 UI**, and **Firebase seed auth → sesiones/contexto → JWT**. Direction: warm editorial (cream paper, lime accent, coral, yolk) matching `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html`. Quality bar: demo screenshots and motion must feel identical to the HTML on phone width 390.

**Lead already wrote this SPEC — execute Phase 0 verbatim. Later phases are roadmap only.**

## 0. Skills in force

- **orquestador** — lead specs; Composer implements
- **pera-design** — craft/finish; chrome neutral, content color; finished controls
- **expo-*** under `.agents/skills/` — Expo Router structure, native UI guidance

## 1. Stack & global setup (Phase 0)

| Piece | Choice |
|-------|--------|
| App location | `/workspace/expo/` (sibling to Android `app/`; monorepo, do not delete Kotlin) |
| Framework | **Expo SDK 52** (stable Firebase JS path) or latest stable if 52 unavailable — prefer **SDK 52** |
| Router | **expo-router** with `src/app/` |
| Language | TypeScript strict |
| Styling | StyleSheet + theme tokens file (mirror Android Color.kt). Optional NativeWind only if expo-tailwind skill used without delaying Phase 0 |
| State | React context + hooks for order flow (mirror ViewModel responsibilities); no Redux |
| Money | Decimal strings + `decimal.js` (no float money) |
| HTTP | `fetch` wrapper with Bearer JWT + Idempotency-Key |
| Auth | Firebase JS SDK Auth (email/password seed) + AsyncStorage persistence; then `POST …/sesiones/contexto` |
| Data modes | `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=MOCK\|REMOTE` (default MOCK) |
| Package manager | bun or npm — pick one and stick |

Create with `npx create-expo-app@latest` targeting SDK 52 template with router if possible.

## 2. Assets

- Copy product images from Android `app/src/main/res/drawable-nodpi/` into `expo/assets/products/` (or reference shared later)
- HTML SoT already at `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html`
- Fixtures: copy `app/src/main/assets/fixtures/*.json` → `expo/assets/fixtures/`

## 3. File structure (Phase 0)

```
expo/
  package.json
  app.json
  tsconfig.json
  src/
    app/                    # routes only
      _layout.tsx
      index.tsx             # splash → redirect
      roles.tsx
      (student)/
        _layout.tsx         # tabs shell
        menu.tsx
        cart.tsx
        confirmation.tsx
      login.tsx             # Firebase seed login (optional entry)
    components/
      bottom-nav.tsx        # Uber frosted nav (from docs/ui-v2/uber_navbar_replica.html)
      product-card.tsx
      physical-press.tsx
    screens/
      splash-screen.tsx
      role-selector-screen.tsx
      catalog-screen.tsx
      cart-screen.tsx
      confirmation-screen.tsx
      login-screen.tsx
    core/
      config.ts             # DATA_SOURCE, API_BASE_URL
      http-client.ts
      session-store.ts
    domain/
      models.ts             # Catalog, Product, OrderDetail, Money helpers
      contract-rules.ts
      money.ts              # decimal.js wrappers
    data/
      fixtures/
      catalog-repository.ts # MOCK + REMOTE switch
      order-repository.ts
      auth/
        firebase.ts
        seed-accounts.ts    # same emails/membresia as Android SeedAccounts
        seed-auth-repository.ts
    theme/
      colors.ts
      typography.ts
      spacing.ts
  README.md
```

Mirror Android naming intent: UI never talks HTTP; repositories are the boundary.

## 4. Design tokens (from HTML / Android Color.kt)

```
paper: #f4f1e7
paper2: #e9e6da
ink: #171817
ink2: #2a2b29
muted: #77796f
accent/lime: #b9d86d
accentSoft: #d7ef8b
accentInk: #1d250c
coral: #f15b55
yolk: #ffd15b
navGlass: rgba(17,17,17,0.97)
navPill: #292929
navTextActive: #f2f2f2
navTextIdle: #b7b7b7
```

Radius: cards ~22–28, nav stadium 44, buttons 20.  
Press scale: **0.97** on nav items; **0.965** default controls (pera/Uber).

Fonts: use Expo Google fonts — **DM Sans** or **Plus Jakarta Sans** for body; a display face for headlines (e.g. **Fraunces** or **Libre Baskerville**) — warm editorial. Do not ship Inter/Roboto/system-only.

## 5. Phase 0 screens (ship these)

### 5.1 Splash
Cream paper, centered mark (simple lime triangle / VA mark), expand motion ~450ms in + 700ms expand (match Android timings if feasible; reduced-motion → static).

### 5.2 Roles
Same copy as HTML 01 / Android RoleSelector: Solo pruebas toggle, **Ver todas las fases** can be stub navigate alert for Phase 0, Entrar como alumno, role cards. Job: pick Alumno → menu.

### 5.3 Catalog (HTML 02)
Hola Dani / ¿Qué se te antoja? / search / chips / quick cards / product grid / Uber bottom nav (5 tabs; non-menu tabs can stub “próximo” for Phase 0 except Cart).

### 5.4 Product sheet (minimal)
Modal/sheet: name, options min/max, qty, CTA `Agregar · $…`.

### 5.5 Cart cash path (VAI-10)
Para llevar + efectivo only for Phase 0 createOrder. Confirm → folio from repository.

### 5.6 Confirmation
PEDIDO CREADO copy + folio; CTA to menu.

### 5.7 Login (Firebase seed)
Emails from Android seed docs; password `saul1234`; on success call contexto if REMOTE else store mock session. Env vars for Firebase config via `EXPO_PUBLIC_FIREBASE_*` — if missing, login screen shows “configure env” and MOCK path still works without Firebase.

## 6. Domain rules (Jesús / VAI-10)

Port from Android `ContractRules` / fixtures:

- Create order request: no client prices/totals; CASH + TAKE_AWAY only for production create
- Options min/max selections
- Qty 1–20; consolidate identical cart lines
- MOCK fixture validates and returns OrderDetail `por_cobrar`
- Money as decimal strings

## 7. Firebase + session (same method as PR #5)

Seed accounts (document in `expo/README.md`):

| email | membresia_id |
|-------|----------------|
| cliente@vaiinilla.test | 9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3 |
| cajero@… | a1111111-0000-4000-8000-0000000000a1 |
| cocina@… | …a2 |
| mesero@… | …a3 |

Password: `saul1234`  
Flow: Firebase signIn → POST `sesiones/contexto` with membresia → store access JWT → API calls.  
Refresh: best-effort stub in Phase 0; document follow-up.

Default API base: `https://vaiinillaback-development-3f6c.up.railway.app/api/v1/` (same as Android remote).

## 8. Cross rules

- Phone-first 390 width; Android Kotlin code **untouched** this pass
- Solo pruebas / MOCK: no network
- Bottom nav: Uber replica tokens (dense dark dock)
- Pera: finished press states; no default gray buttons
- Footguns: Do NOT replace Android app; Do NOT invent Stripe; Do NOT use float for money; Do NOT put non-routes in `src/app/`

## 9. Roadmap (not Phase 0)

1. Assistant, wallet, tracking, stickers (HTML 09–30, 51–57)  
2. Demo gallery jumper  
3. Ops Caja/Cocina/Mesero REMOTE  
4. Full Firebase refresh + EAS builds  
5. Visual regression screenshots

## 10. Acceptance checklist (Phase 0)

- [ ] `expo/` boots with `npx expo start` (or `bun expo start`) without missing-module errors
- [ ] Folder structure matches §3 (routes only under `src/app`)
- [ ] Tokens match §4
- [ ] MOCK: catalog loads from fixtures; cash cart creates `por_cobrar` order; confirmation shows folio
- [ ] Solo pruebas / MOCK documented in `expo/README.md`
- [ ] Firebase module present; login works **or** gracefully degrades if env missing
- [ ] Uber-style bottom nav on catalog
- [ ] Android `app/` Kotlin unchanged
- [ ] Skills committed: `.cursor/skills/pera-design`, `.agents/skills/*`, lockfile
- [ ] Report: how to run + files created

## Closing

Execute **Phase 0 only** verbatim. Do not attempt full HTML 1:1 of all 57 screens in this pass. Do not commit/push (lead will). Prefer matching Android domain semantics over inventing new API shapes.
