# SPEC — Roborazzi UI screenshots (Alumno / cloud Linux)

**Branch:** `feature/alumno-ui-v2-demo-parity`  
**Goal:** Run Compose screenshot tests on JVM (Robolectric + Roborazzi) so the cloud agent can **see** Alumno UI via PNG files without Android Emulator / KVM.  
**Lead already wrote this SPEC — execute it verbatim.**

## Why

Cursor Cloud has a desktop but **no `/dev/kvm`**. Mac local emulator OOMs. Roborazzi unit tests produce PNGs under `app/build/outputs/roborazzi/` that agents can `Read`.

## Stack (pin these)

| Piece | Version / note |
|-------|----------------|
| Roborazzi | **1.70.0** |
| Robolectric | **4.14.1** (or latest 4.14.x if resolve requires) |
| Compose UI Test JUnit4 | from existing Compose BOM (`libs.androidx.compose.bom`) |
| AGP / Kotlin | keep existing (`libs.versions.toml`) |

Do **not** add Paparazzi. Prefer Roborazzi + Robolectric unit tests (`./gradlew :app:recordRoborazziDebug` / `verifyRoborazziDebug`).

## Files to create / touch

### Create

1. `docs/ui-v2/SPEC_ROBORAZZI_SCREENSHOTS.md` — copy of this SPEC (or short pointer + how to run). Prefer writing the runbook into `docs/ui-v2/ROBORAZZI.md` (commands + output paths) and keep this file as the implementation SPEC if already present.
2. `app/src/test/java/com/vaiinilla/app/ui/screenshot/ScreenshotFixtures.kt` — helpers to build `OrderFlowUiState` from fixture catalog JSON (reuse `TestFixtureSource` + `FixtureCatalogRepository` + `ContractFixtureParser`).
3. `app/src/test/java/com/vaiinilla/app/ui/screenshot/AlumnoScreenshotTest.kt` — Roborazzi tests for key screens.
4. Optional: `app/src/test/java/com/vaiinilla/app/ui/screenshot/ScreenshotTheme.kt` — thin wrapper `@Composable fun ScreenshotTheme { VaiinillaTheme(Light) { … } }` if helpful.

### Modify

1. `gradle/libs.versions.toml` — add versions + libraries + plugin for Roborazzi; add `robolectric`, `compose-ui-test-junit4` (compose ui-test from BOM if possible).
2. Root `build.gradle.kts` — `alias(libs.plugins.roborazzi) apply false` (or equivalent).
3. `app/build.gradle.kts`:
   - Apply Roborazzi plugin
   - `testOptions.unitTests.isIncludeAndroidResources = true`
   - Keep existing `vaiinilla.fixtureDir` systemProperty
   - `testImplementation` deps: compose ui-test-junit4, robolectric, roborazzi, roborazzi-compose, roborazzi-junit-rule (or `roborazzi-rule` as published for 1.70.0)
4. `.gitignore` — ignore generated compare diffs if needed; **commit recorded baseline PNGs** under `app/src/test/roborazzi/` or the path Roborazzi 1.70 uses by default for recorded images (document the path in `docs/ui-v2/ROBORAZZI.md`). Prefer committing baselines so CI/agent can verify.

### Do not touch

- Production UI screens (no redesign)
- Firebase / auth branches
- OperationalScreens / Jesús VAI-11 paths (except if shared theme wrappers)
- Emulator / KVM scripts

## Screenshot coverage (minimum)

Each test: phone-ish size **411×891** dp (or 1080×2400 px via Roborazzi size), `VaiinillaTheme(Light)`, capture with `captureRoboImage("name.png")` (or RoborazziRule equivalent).

| Test name / file stem | Composable | State notes |
|-----------------------|------------|-------------|
| `01_splash` | `SplashScreen(onFinished={})` | OK if animation mid-frame; still capture a cream+mark frame |
| `02_role_selector` | `RoleSelectorScreen(testOnlyMode=true, …)` | Theme locals via `VaiinillaTheme` |
| `03_catalog` | `CatalogScreen` | `loading=false`, catalog from fixtures, empty cart, noop lambdas |
| `04_cart` | `CartScreen` | At least 1 cart line from first fixture product (qty 1), payment CASH or BALANCE |
| `05_assistant_hub` | `AssistantScreen` | Catalog loaded, noop lambdas |
| `06_wallet` | `WalletScreen` | `balance=200`, catalog optional if required by state |

Use **noop** lambdas `{}` / `{ _ -> }` everywhere. Do not start coroutines that hit network.

### Fixture loading

Reuse existing unit-test path:

```kotlin
val catalog = FixtureCatalogRepository(TestFixtureSource(), ContractFixtureParser()).getCatalog().getOrThrow()
// or whatever the repository public API is — match existing UseCaseTest / FixtureCatalogRepository
```

Build:

```kotlin
OrderFlowUiState(
  loading = false,
  catalog = catalog,
  operationalStatus = /* from fixture or acceptingOrders=true */,
  testOnlyMode = true,
  cartLines = …,
)
```

If `getCatalog` naming differs, follow `FixtureCatalogRepository` public methods exactly.

`ThemeCycleButton` / `LocalVaiinillaColors` require wrapping in `VaiinillaTheme`.

## Gradle wiring notes

- `dependencyResolutionManagement` already uses `FAIL_ON_PROJECT_REPOS` — put all deps in version catalog + module deps, no ad-hoc `repositories {}` in app.
- Roborazzi Gradle plugin id is typically `io.github.takahirom.roborazzi`.
- Enable Android resources in unit tests or Robolectric will fail on themes/resources.

## Commands (Definition of Done must pass)

From `/workspace`:

```bash
./gradlew :app:recordRoborazziDebug --no-daemon
# or the exact task name Roborazzi 1.70 registers — use that if different
```

Then:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.vaiinilla.app.ui.screenshot.*' --no-daemon
```

Existing unit tests must still pass:

```bash
./gradlew :app:testDebugUnitTest --no-daemon
```

## Output

- Document PNG output directory in `docs/ui-v2/ROBORAZZI.md` (e.g. `app/build/outputs/roborazzi/` and/or `app/src/test/roborazzi/`).
- After record, list generated PNG paths in the worker report.

## Acceptance checklist

- [ ] Skill/docs: `docs/ui-v2/ROBORAZZI.md` with how to record/verify
- [ ] Version catalog + plugins wired; project sync/configure succeeds
- [ ] `AlumnoScreenshotTest` covers the 6 screens above
- [ ] `./gradlew :app:recordRoborazziDebug` (or equivalent) produces PNGs
- [ ] Existing `testDebugUnitTest` still green
- [ ] No emulator / KVM dependency
- [ ] Report: files changed + exact PNG paths for the lead to `Read`

## Closing command

Execute this SPEC verbatim. Do not expand to Paparazzi, dark/AMOLED matrix, or full Phase 4 screen set in this pass. If a Compose API forces a small helper, keep it test-only under `ui/screenshot/`.
