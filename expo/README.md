# Vaiinilla Expo — Phase 1

Expo (React Native) port of Vaiinilla with Jesús-style architecture: domain → repositories → UI, **MOCK fixtures by default**, optional Firebase seed auth + REMOTE API.

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
npm run export      # static bundle check
npm run android
npm run web
```

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

Set `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=REMOTE` and configure Firebase + API.

Login flow: Firebase email/password → `POST /sesiones/contexto` → store Vaiinilla JWT on API calls (Bearer).

**Note:** Student checkout with saldo, tarjeta, or mesa is blocked in REMOTE (MOCK-only); cash + para llevar uses the API.

## Firebase env (optional)

If any value is missing, the login screen shows **“Firebase no configurado”** and MOCK still works.

```env
EXPO_PUBLIC_FIREBASE_API_KEY=
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=
EXPO_PUBLIC_FIREBASE_PROJECT_ID=
EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET=
EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
EXPO_PUBLIC_FIREBASE_APP_ID=
```

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
2. **Entrar como alumno** → Catalog (search, chips, product sheet)
3. Bottom nav: **Menú · Asistente · Pedidos · Cartera · Carrito** (real screens)
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

## Routes (Phase 1)

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
  src/app/          # routes only
  src/components/
  src/screens/
  src/core/
  src/domain/
  src/data/
  src/hooks/
  src/theme/
  assets/fixtures/
```

Specs: `docs/expo/SPEC_EXPO_PHASE0.md`, `docs/expo/SPEC_EXPO_PHASE1.md`
