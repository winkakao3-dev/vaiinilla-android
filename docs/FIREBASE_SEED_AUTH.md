# Firebase seed login (debug only)

Convenience Email/Password auth for local debugging against project `vaiinilla-b3a70`. It still uses Firebase and Railway; it is not a data-source replacement.

## Security rules

- Seed **passwords are never committed** and never embedded in release APKs.
- They live only in **`local.properties`** (gitignored) and are injected into **debug** `BuildConfig`.
- `SEED_AUTH_ENABLED` is `true` only on debug/preview builds.
- Release builds cannot authenticate with seed accounts.

## Flow

1. The local debug flow calls `AuthenticateSeedRoleUseCase` when a role needs to be exercised:
   - Firebase `signInWithEmailAndPassword` with the seed account for that role
   - `getIdToken()` → `POST /api/v1/sesiones/contexto` with `Authorization: Bearer <firebase-id-token>` and `{"membresia_id":"..."}`
   - Vaiinilla JWT stored in `SecureSessionStore` and cached per role in `SeedJwtCache`
3. Navigation proceeds into the role screen with a valid session.
4. JWT refresh runs ~3 min before expiry (15 min) and on `401 UNAUTHENTICATED` for business API calls.

## Seed identities (emails / membresía · no passwords)

| Role | Email | membresia_id |
|------|-------|--------------|
| CLIENT | cliente@vaiinilla.test | 9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3 |
| CASHIER | cajero@vaiinilla.test | a1111111-0000-4000-8000-0000000000a1 |
| KITCHEN | cocina@vaiinilla.test | a1111111-0000-4000-8000-0000000000a2 |
| WAITER | mesero@vaiinilla.test | a1111111-0000-4000-8000-0000000000a3 |

Passwords: set in `local.properties` as `vaiinillaSeedPasswordCliente|Cajero|Cocina|Mesero`.  
Rotate them in Firebase Auth whenever they leak; never paste them into git, docs, or PR comments.

## Staff presence (single device)

When the alumno submits an order in **debug REMOTE**, `StaffPresenceCoordinator.primeStaffPresence(activeRole = CLIENT)` may:

1. Signs into cajero and cocina seed accounts sequentially and caches their JWTs
2. Restores the alumno Firebase session + session token
3. Sends `latidos` for Caja and Cocina using the cached JWTs

If the debug seed passwords are present in `local.properties`, the single-device convenience path signs in the seed Caja/Cocina accounts and sends their heartbeats. If they are absent, the coordinator does not invent permissions or credentials: the backend remains the authority and the normal multi-device staff presence flow must provide the required heartbeats. This helper is never enabled as a release dependency.

## Verify the debug helper

```bash
# local.properties (do not commit)
vaiinillaApiBaseUrl=https://vaiinillaback.up.railway.app/api/v1/
vaiinillaSeedPasswordCliente=<rotated>
vaiinillaSeedPasswordCajero=<rotated>
vaiinillaSeedPasswordCocina=<rotated>
vaiinillaSeedPasswordMesero=<rotated>

./gradlew testDebugUnitTest assembleDebug
```

Install on device/emulator with network. The app signs in via Firebase and exchanges context before opening the role screen.

## Files

- `domain/auth/SeedAccounts.kt` — emails/membresía; passwords from BuildConfig
- `data/auth/FirebaseSeedAuthRepository.kt` — Firebase + contexto exchange (debug-gated)
- `ui/mode/AuthorizedAccessViewModel.kt` — authorized mode state
- `core/security/SeedJwtCache.kt` — per-role JWT cache
- `core/auth/VaiinillaJwtRefreshCoordinator.kt` — refresh timer + 401 hook
