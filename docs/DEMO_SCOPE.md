# Demo / Solo pruebas scope

Surfaces that are **demonstrative only** and must not ship as product behavior.

## Technical gates

| Gate | Meaning |
|------|---------|
| `BuildConfig.ALLOW_DEMO_TOOLS` | `true` only on **debug** builds |
| Solo pruebas (`testOnlyMode`) | Runtime toggle on the role selector (debug only) |
| `DemoFeatures.isUnlocked(testOnlyMode)` | Both must be true |

Release APKs never set `ALLOW_DEMO_TOOLS`, so these UIs cannot unlock.

## Restricted surfaces

Unlocked only when `DemoFeatures.isUnlocked(testOnlyMode)`:

1. **Selector interno de roles** (Caja / Cocina / Mesero / Administración)
2. **Galería demo** (“Ver todas las fases”)
3. **Wallet** + tarjetas / recargas / métodos de pago
4. **Asistente** (home + chat)

Without Solo pruebas (even on debug), the app exposes the alumno entry path only.

## Product paths (not demo-gated)

- Catálogo → carrito → checkout efectivo → confirmación / sticker
- Seguimiento de pedido alumno
- Pantallas operativas alcanzables vía selector interno **solo** con Solo pruebas en debug

## Seed REMOTE auth

Also debug-only (`SEED_AUTH_ENABLED`). See `docs/FIREBASE_SEED_AUTH.md`.
