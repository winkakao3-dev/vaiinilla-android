# Firebase seed login (REMOTE · debug only)

Demo-only Email/Password auth for Vaiinilla Android against project `vaiinilla-b3a70`.

## Security rules

- Seed **passwords are never committed** and never embedded in release APKs.
- They live only in **`local.properties`** (gitignored) and are injected into **debug** `BuildConfig`.
- `SEED_AUTH_ENABLED` / `ALLOW_DEMO_TOOLS` are `true` only on the `debug` build type.
- Release builds cannot authenticate with seed accounts even if someone sets REMOTE.

## Flow

1. User selects a role on `RoleSelectorScreen` (staff roles require **Solo pruebas** on debug).
2. In **REMOTE** debug, `RoleAuthViewModel` calls `AuthenticateSeedRoleUseCase`:
   - Firebase `signInWithEmailAndPassword` with the seed account for that role
   - `getIdToken()` → `POST /api/v1/sesiones/contexto` with `Authorization: Bearer <firebase-id-token>` and `{"membresia_id":"..."}`
   - Vaiinilla JWT stored in `SecureSessionStore` and cached per role in `SeedJwtCache`
3. Navigation proceeds into the role screen with a valid session.
4. JWT refresh runs ~3 min before expiry (15 min) and on `401 UNAUTHENTICATED` for business API calls.

**MOCK** skips Firebase and keeps fixture behavior (`BuildConfig` tokens optional).

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

When the alumno submits an order, `StaffPresenceCoordinator.primeStaffPresence(activeRole = CLIENT)`:

1. Signs into cajero and cocina seed accounts sequentially and caches their JWTs
2. Restores the alumno Firebase session + session token
3. Sends `latidos` for Caja and Cocina using the cached JWTs

Requires the same debug seed passwords in `local.properties`.

## Verify REMOTE (debug)

```bash
# local.properties (do not commit)
vaiinillaDataSource=REMOTE
vaiinillaApiBaseUrl=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
vaiinillaSeedPasswordCliente=<rotated>
vaiinillaSeedPasswordCajero=<rotated>
vaiinillaSeedPasswordCocina=<rotated>
vaiinillaSeedPasswordMesero=<rotated>

./gradlew testDebugUnitTest assembleDebug
```

Install on device/emulator with network. Select a role; the app signs in via Firebase and exchanges context before opening the role screen.

## Files

- `domain/auth/SeedAccounts.kt` — emails/membresía; passwords from BuildConfig
- `data/auth/FirebaseSeedAuthRepository.kt` — Firebase + contexto exchange (debug-gated)
- `ui/auth/RoleAuthViewModel.kt` — loading/error on role selector
- `core/security/SeedJwtCache.kt` — per-role JWT cache
- `core/auth/VaiinillaJwtRefreshCoordinator.kt` — refresh timer + 401 hook
- `core/config/DemoFeatures.kt` — demo tool gating
