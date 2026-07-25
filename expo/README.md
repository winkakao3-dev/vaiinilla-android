# Vaiinilla Expo — Phase 2

Expo (React Native) port of Vaiinilla with Jesús-style architecture: domain → repositories → UI, **MOCK fixtures by default**, optional Firebase seed auth + REMOTE API.

**Phase 2 adds:** real product photos (from Android drawable-nodpi), UI polish on hero surfaces, Firebase/REMOTE developer DX (`.env.example`, mode chips), and EAS preview config for future installable builds.

## Stack

- **Expo SDK 52** · **expo-router** · `src/app/` routes only
- TypeScript strict · `decimal.js` money · warm editorial tokens from HTML SoT

## Run

```bash
cd expo
npm install
npm run start
```

Other commands:

```bash
npm run typecheck   # tsc --noEmit
npm run export      # static bundle check (web)
npm run android
npm run web
```

## Phase 2 checklist

- [x] Product images in `assets/products/` (copied from Android `drawable-nodpi`)
- [x] `ProductImage` component wired into catalog, cart, assistant
- [x] Roles + login show MOCK/REMOTE + Firebase configured status
- [x] `expo/.env.example` (copy → `.env`, gitignored)
- [x] `eas.json` + `com.vaiinilla.expo` bundle/package ids in `app.json`
- [x] `npm run typecheck` · `npx expo export --platform web`

## Data modes

| Variable | Values | Default |
|----------|--------|---------|
| `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE` | `MOCK` \| `REMOTE` | `MOCK` |
| `EXPO_PUBLIC_API_BASE_URL` | Railway API base | `https://vaiinillaback-development-3f6c.up.railway.app/api/v1/` |

### Solo pruebas / MOCK

- Toggle **Solo pruebas** on the roles screen (persisted in AsyncStorage).
- With `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=MOCK` (default): catalog and orders use `assets/fixtures/*.json` — **no network**.
- Student checkout supports **para llevar / en mesa (Mesa 1–6)** and **efectivo / saldo / tarjeta** in MOCK.
- Cash orders → `por_cobrar`; saldo/tarjeta → `cobrado` instantly (demo).
- Wallet balance is local (AsyncStorage); saldo checkout debits on confirm.

### REMOTE

1. Copy `expo/.env.example` → `expo/.env`
2. Set `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=REMOTE`
3. Fill `EXPO_PUBLIC_FIREBASE_*` and optionally `EXPO_PUBLIC_API_BASE_URL`
4. Restart Expo (`npm run start`)
5. Roles or Login screen shows **MOCK/REMOTE** chip + Firebase status
6. Login: Firebase email/password → `POST /sesiones/contexto` → Bearer on API calls

**Note:** Student checkout with saldo, tarjeta, or mesa is blocked in REMOTE (MOCK-only); cash + para llevar uses the API.

## Firebase env (optional)

If any value is missing, screens show **Firebase no configurado** and MOCK still works.

```bash
cp .env.example .env
# edit .env — do not commit .env
```

```env
EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=REMOTE
EXPO_PUBLIC_API_BASE_URL=
EXPO_PUBLIC_FIREBASE_API_KEY=
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=
EXPO_PUBLIC_FIREBASE_PROJECT_ID=
EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET=
EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
EXPO_PUBLIC_FIREBASE_APP_ID=
```

## EAS preview build (config only)

Requires your Expo account locally — not run in CI here.

```bash
cd expo
npx eas-cli build --profile preview --platform android
```

Profiles in `eas.json`: `development`, `preview` (internal APK/AAB-friendly), `production`.

Bundle IDs: `com.vaiinilla.expo` (iOS + Android).

## Seed accounts

Password for all: **`saul1234`**

| Email | membresia_id | Rol |
|-------|----------------|-----|
| `cliente@vaiinilla.test` | `9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3` | cliente |
| `cajero@vaiinilla.test` | `a1111111-0000-4000-8000-0000000000a1` | cajero |
| `cocina@vaiinilla.test` | `a1111111-0000-4000-8000-0000000000a2` | cocina |
| `mesero@vaiinilla.test` | `a1111111-0000-4000-8000-0000000000a3` | mesero |

In MOCK mode, login stores a mock session without calling Firebase.

## Phase 1 flow

1. Splash → Roles
2. **Entrar como alumno** → Catalog (search, chips, product sheet with photos)
3. Bottom nav: **Menú · Asistente · Pedidos · Cartera · Carrito**
4. Cart → entrega (para llevar / mesa) + pago (efectivo / saldo / tarjeta) → Confirmation
5. Tracking timeline on **Pedidos**; wallet hub + subflows; sticker styles 0–5
6. **Ver todas las fases** → `/demo/gallery` (forces Solo pruebas, ≥12 jump targets)
7. Ops roles: **Caja** (collect cash), **Cocina**, **Mesero** fixture screens
8. Optional: Login screen from roles (Firebase + REMOTE)

## Demo gallery

From **Roles → Ver todas las fases**:

- Opens `/demo/gallery` and enables Solo pruebas
- Sectioned jumps: catalog, empty search, assistant, chat, cart states, checkout variants, confirmation, tracking, wallet, stickers, caja/cocina/mesero

Quick try:

```bash
cd expo && npm run web
# Roles → Ver todas las fases → pick any destination
```

## Caja cash collect (MOCK)

1. Roles → **Ver todas las fases** → Operación → **Caja** (seeds a `por_cobrar` order), or create a cash cart and confirm.
2. Roles → **Cajero** (or gallery → Caja).
3. Enter amount ≥ total → **Cobrar efectivo** → order advances to `cobrado`.

## Routes

```
(student)/
  menu, cart, confirmation
  assistant, assistant-chat
  orders
  wallet, wallet-add-money, wallet-methods, wallet-add-card, wallet-account
  sticker
demo/gallery
(ops)/
  caja, cocina, mesero
roles, login
```

## Project layout

```
expo/
  assets/products/   # product + logo images (Phase 2)
  assets/fixtures/
  src/app/           # routes only
  src/components/    # ProductImage, ProductCard, …
  src/screens/
  src/core/
  src/domain/
  src/data/
  src/hooks/
  src/theme/
  .env.example
  eas.json
```

Specs: `docs/expo/SPEC_EXPO_PHASE0.md`, `docs/expo/SPEC_EXPO_PHASE1.md`, `docs/expo/SPEC_EXPO_PHASE2.md`
