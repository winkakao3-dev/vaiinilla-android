# SPEC — Demo phase jumper (Solo pruebas gallery)

**Branch:** `feature/alumno-ui-v2-demo-parity`  
**Goal:** From the role selector, jump to any Alumno demo phase/screen without walking the full flow — and keep using **Solo pruebas** (fixtures only, no backend).  
**Lead already wrote this SPEC — execute it verbatim.**

## Context (already exists)

- `TestOnlyModeCard` on `RoleSelectorScreen`: switch **Solo pruebas** → `DataSourceResolver.isTestOnlyMode` + fixtures, no API/Firebase/network.
- User wants a **small button** to open a gallery of all demo phases/screens.
- Stickers 51–56 and screens 01–30 + 57 already exist as Compose routes/composables.

## Product behavior

### Entry
On `RoleSelectorScreen`, **below** the Solo pruebas card (or as a secondary row when Solo pruebas is ON):

- Button / text button: **`Ver todas las fases`** (or `Galería demo`)
- Prefer showing it **always**, but tapping it:
  1. Forces `testOnlyMode = true` if not already
  2. Navigates to new route `Routes.DEMO_GALLERY` (`"demo/gallery"`)

Optional tiny caption under the button: `Salta a cualquier pantalla con fixtures locales.`

### Gallery screen — `DemoGalleryScreen`
Simple scrollable list (no cards-in-hero clutter; one section purpose):

**Title:** `Galería demo`  
**Subtitle:** `Solo pruebas · sin backend`  
**Back:** returns to role selector.

Grouped sections with tappable rows (id + title). On tap: navigate to that destination with fixture state prepared.

#### Groups & destinations

**Arranque**
| ID | Label | Destination |
|----|-------|-------------|
| Splash | Splash | `Routes.SPLASH` (or skip — optional) |
| 01 | Selector de roles | `Routes.ROLE_SELECTOR` |

**Menú**
| ID | Label | Notes |
|----|-------|-------|
| 02 | Catálogo | `CATALOG`, clear search |
| 05 | Banner pedido activo | `CATALOG` + seed `activeOrder` PREPARING if AppNavHost/catalog supports it; else open tracking |
| 06 | Búsqueda vacía | `CATALOG` + set search to something with no hits (e.g. `zzzz`) via ViewModel if possible; else document manual |
| 07/08 | Producto (sheet) | `CATALOG` + select first product (open sheet) if ViewModel API allows |

**Asistente**
| 09 | Hub asistente | `ASSISTANT` |
| 57 | Chat | `ASSISTANT_CHAT` |

**Carrito / checkout**
| 12 | Carrito vacío | `CART` with empty cart |
| 13 | Llevar + efectivo | seed 1 cart line, TAKE_AWAY + CASH |
| 14 | Mesa + saldo | seed cart, IN_SPACE + BALANCE, mesa 4 |
| 15 | Llevar + tarjeta | seed cart, TAKE_AWAY + CARD |

**Confirmación**
| 16 | Confirm efectivo | `CONFIRMATION` with CASH printed/demo order |
| 17 | Confirm saldo | BALANCE |
| 18 | Confirm tarjeta | CARD |

**Pedidos**
| 19 | Sin pedidos | `STUDENT_TRACKING` empty |
| 20 | Por cobrar | seed PENDING_PAYMENT order |
| 21–24 | Cobrado / Preparando / Listo / Entregado | seed order in that state |

**Cartera**
| 25 | Hub | `WALLET` |
| 26 | Añadir dinero | `WALLET_ADD_MONEY` card |
| 27 | SPEI | add-money SPEI method |
| 28 | Métodos | `WALLET_METHODS` |
| 29 | Agregar tarjeta | `WALLET_ADD_CARD` |
| 30 | Mi cuenta | `WALLET_ACCOUNT` |

**Stickers**
| 51–56 | Editorial / Core / Limited / Breakfast / QR Live / Thermal | `RECEIPT_STICKER` — if pager can accept initial style index, open that page; else open sticker screen and note style chips |

**Ops (optional short section)**
| Caja / Cocina / Mesero | existing operational routes |

### State seeding
Add a small helper (e.g. `DemoGallerySeeder` or methods on `OrderFlowViewModel` / test-only API) used **only** from gallery:

- Load catalog from fixtures if needed
- `seedCartWithFirstProduct()`
- `seedCheckout(destination, payment, spaceId?)`
- `seedOrder(state: OrderState, payment: PaymentMethod)` into whatever powers tracking/confirmation (createdOrder / active order list)
- Prefer reusing existing fixture repositories and screenshot fixture patterns from `ScreenshotFixtures.kt`

If a seed is hard without refactor, navigate to the closest screen and leave a one-line comment in gallery — **do not** invent a fake parallel UI. Prefer real screens.

Confirmation may need `createdOrder` non-null in ViewModel — mirror how `AppNavHost` already navigates to confirmation after submit, or set demo order directly for gallery.

### Solo pruebas
- Opening gallery **must** enable test-only mode.
- Keep existing badge when enabled.
- Do not call remote API from gallery paths.

### Out of scope
- Redesigning ops 31–50
- Changing HTML SoT
- Replacing Solo pruebas card
- Production “debug menu” gated by BuildConfig — **OK to show always on this demo branch**; if easy, wrap gallery entry with `BuildConfig.DEBUG || testOnlyMode` but SPEC prefers visible when Solo pruebas is on OR always on this feature branch. **Decision: show button always on RoleSelector; tapping enables Solo pruebas.**

## Files

Create:
- `app/src/main/java/com/vaiinilla/app/ui/screens/DemoGalleryScreen.kt`
- `app/src/main/java/com/vaiinilla/app/ui/demo/DemoGallerySeeder.kt` (or similar)

Modify:
- `Routes.kt` — `DEMO_GALLERY`
- `AppNavHost.kt` — route + wiring
- `RoleSelectorScreen.kt` — entry button
- `OrderFlowViewModel` / ui state as needed for seeding
- Optional: one Roborazzi `23_demo_gallery.png` of the gallery list

## Acceptance

- [ ] Button on roles screen opens gallery and forces Solo pruebas
- [ ] Gallery lists Alumno phases with working jumps for: catalog, assistant, chat, empty cart, seeded cart variants, confirmation (at least one), tracking empty + at least one active state, wallet hub + one subflow, sticker screen
- [ ] No network/backend required when using gallery
- [ ] `./gradlew :app:assembleDebug :app:testDebugUnitTest` green (record Roborazzi if added)
- [ ] Report how to try: Solo pruebas → Ver todas las fases → tap a row

## Closing

Execute verbatim. Keep UI simple (list + sections). Do not commit/push (lead will).
