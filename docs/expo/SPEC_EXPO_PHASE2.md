# MASTER SPEC — Vaiinilla Expo Phase 2 (images + polish + Firebase DX + EAS)

**Branch:** `cursor/expo-vaiinilla-5c6b`  
**Base already shipped:** Phase 0 + Phase 1 (gallery, Alumno flows, ops fixtures, Firebase scaffold).  
**Goal:** Make the Expo demo feel **real** — product photos (not letter swatches), tighter UI craft on hero surfaces, Firebase/REMOTE developer experience ready without secrets in repo, and EAS preview config so an installable build can be produced later.  
**SoT UI:** `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html` + Android `ProductImage.kt` / Alumno screens.  
**Lead already wrote this SPEC — execute it verbatim.**

Skills: orquestador (this), pera-design (finished controls), existing Expo tokens (warm editorial cream/lime — **keep**; do not invent a new palette).

## Out of Phase 2

- Live Stripe charges / admin commission reports
- Store submit / production EAS credentials in CI
- Pixel-perfect every HTML sticker kerning (improve stickers, don’t chase 1:1)
- Committing real Firebase API keys or `.env` with secrets
- Touching Android Kotlin under `app/`

## Architecture (keep)

Extend `expo/src/{domain,data,core,screens,components,hooks,app}` — no parallel stack.

- MOCK / Solo pruebas remain default and work offline
- REMOTE still optional; degrade gracefully when Firebase env missing
- Money: decimal strings + decimal.js
- Routes stay thin

---

## A. Product images (must)

Android already has real photos in `app/src/main/res/drawable-nodpi/` (webp/jpg). Expo `assets/products/` is empty.

1. **Copy** all product/logo images from Android drawable-nodpi into `expo/assets/products/` (same filenames: `burrito_norteno.webp`, `jamaica.jpg`, `waffle.jpg`, `logo.webp`, etc.).
2. Create `expo/src/components/product-image.tsx` mirroring Android `ProductImage.kt`:
   - Input: `imageUrl` (`fixture://key` or bare key), `style`, optional `contentFit`
   - Map keys → `require('../../assets/products/<file>')` for:
     - jamaica, burrito_norteno, waffle, burrito_barbacoa, burrito_frijol_queso, burrito_machaca, fruta, montado_asada, montado_chorizo, montado_machaca, montado_norteno, quesa, quesadilla_harina, sincronizada_nortena, torta
   - Unknown key → waffle fallback
3. Wire `ProductImage` into:
   - `product-card.tsx` (replace letter swatch)
   - Catalog product detail sheet in `catalog-screen.tsx`
   - Assistant recommendation cards if they show product art (`assistant-screen.tsx`)
   - Cart line items if they show a thumb (`cart-screen.tsx`) — add small 48–56px thumb if missing
4. Optional: splash/brand — if splash or roles can use `logo.webp` without fighting Expo splash config, use it as a mark; do not break splash.

---

## B. Visual polish pass (hero surfaces)

Keep tokens in `theme/`. Pera: finished press states, no unfinished TextInputs, intentional motion already via `PhysicalPress` — extend lightly.

Focus polish (copy/spacing/hierarchy closer to HTML, not full pixel chase):

1. **Catalog** — clearer header (“Menú” / brand mark), search field finished, chips spacing, product grid denser if needed, detail sheet with full-bleed product image on top
2. **Cart / checkout** — payment method cards (efectivo / saldo / tarjeta) clearer badges; mesa 1–6 chips; sticky confirm CTA finished
3. **Confirmation** — stronger success head + sticker CTA hierarchy
4. **Roles** — brand-forward first screen (logo + Vaiinilla hero signal); keep Solo pruebas + Ver todas las fases
5. **Stickers** — ensure all 6 styles render without layout collapse; improve typography hierarchy where thin
6. **Bottom nav** — leave Uber frosted behavior; only fix if a tab label/icon is off

Do **not** add cards for decoration. Do **not** add purple themes or new color systems.

---

## C. Firebase / REMOTE developer experience

Auth already works in MOCK and calls Firebase when `DATA_SOURCE=REMOTE` + env set. Harden DX:

1. Add `expo/.env.example` with all `EXPO_PUBLIC_*` keys (empty values) + short comments. Do **not** add a real `.env` with secrets.
2. On **login** and **roles** screens, show a small **mode chip**: `MOCK` | `REMOTE` and Firebase configured yes/no (meta text, not a dashboard widget cluster).
3. Improve REMOTE error copy when:
   - Firebase not configured but user tries REMOTE login
   - `sesiones/contexto` fails (network / 401) — surface readable Spanish message
4. Ensure `http-client` already attaches Bearer for REMOTE; if not, fix.
5. Update `expo/README.md` Phase 2 section: copy `.env.example` → `.env`, set REMOTE, restart Expo, seed login path.
6. Add `expo/.gitignore` entry for `.env` / `.env.local` if missing (keep example tracked).

---

## D. EAS preview config (installable later)

Cannot run a cloud EAS build without Expo account secrets in this environment — ship **config only**:

1. Add `expo/eas.json` with profiles:
   - `development` (dev client optional / or internal)
   - `preview` (internal distribution APK/AAB-friendly)
   - `production` (store-shaped, unused for now)
2. Update `expo/app.json`:
   - `android.package`: `com.vaiinilla.app` (or `com.vaiinilla.expo` if collision risk — prefer `com.vaiinilla.expo`)
   - `ios.bundleIdentifier`: `com.vaiinilla.expo`
   - keep existing icon/splash
3. README: document `npx eas-cli build --profile preview --platform android` (user runs on their machine / Expo account).

Do **not** commit Expo tokens or run interactive `eas login`.

---

## E. Docs

- Write/update `docs/expo/SPEC_EXPO_PHASE2.md` is this file (already written by lead).
- Update `expo/README.md` with Phase 2 checklist (images, env example, EAS).

---

## Acceptance

- [ ] `expo/assets/products/` contains the Android product/logo images (non-empty)
- [ ] Catalog grid and product sheet show real photos for known `fixture://` keys
- [ ] Cart shows product thumbs when lines exist
- [ ] Roles + login show MOCK/REMOTE + Firebase status
- [ ] `expo/.env.example` exists; `.env` gitignored
- [ ] `expo/eas.json` + android/ios ids in `app.json`
- [ ] `npm run typecheck` green in `expo/`
- [ ] `npx expo export --platform web` succeeds (or document failure reason)
- [ ] Android Kotlin under `app/` untouched
- [ ] README updated for Phase 2

## Footguns

1. Do NOT rewrite Phase 0/1 architecture  
2. Do NOT require Firebase for MOCK demos  
3. Do NOT commit secrets / real `.env`  
4. Do NOT touch `app/**` Kotlin  
5. Do NOT commit or push (lead will)  
6. Prefer `require()` static maps for assets (Metro); no dynamic `require(variable)`  

## Closing

Execute Phase 2 verbatim. Prefer porting `ProductImage.kt` mapping and copying drawable-nodpi bytes as-is.
