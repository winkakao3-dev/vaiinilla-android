# SPEC — Uber frosted bottom nav (Vaiinilla student)

**Branch:** `feature/alumno-ui-v2-demo-parity`  
**SoT:** `docs/ui-v2/uber_navbar_replica.html`  
**Goal:** Restyle `VaiinillaBottomNav` to match the Uber replica chrome (dense frosted dark bar, sliding pill, press scale `.97`). Keep Vaiinilla’s **5 tabs** and destinations.  
**Lead already wrote this SPEC — execute it verbatim.**

## Critical tokens from HTML (`:root` + mobile `@media`)

| Token | Value |
|-------|--------|
| nav-bg | `rgba(17, 17, 17, 0.97)` → Compose ≈ `Color(0xF7111111)` |
| nav-border | `rgba(255,255,255,0.10)` |
| active-bg (pill) | `#292929` |
| text-active | `#f2f2f2` |
| text-idle | `#b7b7b7` |
| Mobile height | **88dp** |
| Mobile radius | **44dp** (stadium) |
| Mobile padding | **9dp** |
| Pill | inset top/bottom/left = padding; width = `(W - 2*pad) / tabCount` |
| Shadow | `0 18dp 50dp` black 50% + inset top hairline white ~2.5% |
| Backdrop | blur **22px** + saturate ~135% if using Haze; if not, keep **0.97 alpha** so it never looks “almost transparent” |
| Press | `scale(0.97)` on item `:active` |
| Pill motion | **340ms** `cubic-bezier(.22, .8, .25, 1)` |
| Icon (mobile) | **27dp** |
| Label (mobile) | **14sp**, weight 500 idle / **700** active, letter-spacing slightly tight |
| Item gap icon→label | ~3dp |

## Keep Vaiinilla behavior

- Tabs still: Menú / Asistente / Pedidos / Cartera / Carrito (5 columns, not Uber’s 4).
- Cart badge stays (coral).
- Existing bounce-on-activate can stay subtle; press scale must be **0.97**.
- Do **not** switch to lime active pill for this pass — Uber replica uses **gray `#292929` pill** + light text. (Brand lime is out for this chrome.)

## Theme note

This Uber chrome is intentionally **dark glass** on all themes (like Uber). Optionally in Light theme you may keep the same dark dock (Uber look floating over cream content) — preferred. Do not go back to near-clear `NavGlass 0xA6…`.

## Files

- Update `VaiinillaBottomNav.kt` to match geometry/colors/press
- Update `Color.kt` nav tokens (`NavGlass`, `NavBorder`, `NavPill`, `NavTextIdle`, `NavTextActive`) to Uber values
- `PhysicalPress.kt`: add `Nav` scale = **0.97** (or set Small to 0.97 for nav only)
- Optional Haze for real blur — nice-to-have; **0.97 fill is mandatory** so opacity is fixed even without blur
- Re-record Roborazzi with nav visible: `03_catalog`, `05_assistant_hub`, `04_cart`, `19_catalog_dark` (and any others that show nav)

## Acceptance

- [ ] Bar looks dense dark frosted (~97% opaque), not washed/transparent
- [ ] Sliding `#292929` pill; idle/active text colors match HTML
- [ ] Press scale ~0.97 on tabs
- [ ] 5 Vaiinilla tabs + cart badge still work
- [ ] `./gradlew :app:assembleDebug :app:verifyRoborazziDebug :app:testDebugUnitTest` green
- [ ] Report files + updated PNG paths

## Closing

Execute verbatim from `uber_navbar_replica.html`. Do not commit/push.
