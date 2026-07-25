# Vaiinilla Expo — Phase 0

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
- Cash cart path: `para_llevar` + `efectivo` → order state `por_cobrar` with folio from fixture repository.

### REMOTE

Set `EXPO_PUBLIC_VAIINILLA_DATA_SOURCE=REMOTE` and configure Firebase + API. Login flow: Firebase email/password → `POST /sesiones/contexto` → store Vaiinilla JWT.

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

## Phase 0 flow

1. Splash → Roles
2. **Entrar como alumno** → Catalog (search, chips, product sheet)
3. Bottom nav: Menu + Cart work; other tabs show “Próximamente”
4. Cart → confirm cash / para llevar → Confirmation with folio
5. Optional: Login screen from roles

## Project layout

```
expo/
  src/app/          # routes only
  src/components/
  src/screens/
  src/core/
  src/domain/
  src/data/
  src/theme/
  assets/fixtures/
```

Master spec: `docs/expo/SPEC_EXPO_PHASE0.md`
