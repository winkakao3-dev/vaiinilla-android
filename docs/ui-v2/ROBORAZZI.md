# Roborazzi — Alumno UI screenshots (JVM)

Compose screenshot tests run on the JVM with **Robolectric + Roborazzi** (no emulator/KVM). Implementation spec: [`SPEC_ROBORAZZI_SCREENSHOTS.md`](SPEC_ROBORAZZI_SCREENSHOTS.md).

## Record baselines

From `/workspace`:

```bash
./gradlew :app:recordRoborazziDebug --no-daemon
```

## Verify against baselines

```bash
./gradlew :app:verifyRoborazziDebug --no-daemon
```

## Run screenshot tests only

```bash
./gradlew :app:testDebugUnitTest --tests 'com.vaiinilla.app.ui.screenshot.*' --no-daemon
```

## Run all unit tests

```bash
./gradlew :app:testDebugUnitTest --no-daemon
```

## Paths

| Purpose | Path |
|---------|------|
| **Committed reference PNGs** | `app/src/test/roborazzi/` |
| Test run / compare output | `app/build/outputs/roborazzi/` |
| HTML report (after verify) | `app/build/reports/roborazzi/index.html` |

Recorded files (one per test):

- `app/src/test/roborazzi/01_splash.png`
- `app/src/test/roborazzi/02_role_selector.png`
- `app/src/test/roborazzi/03_catalog.png`
- `app/src/test/roborazzi/04_cart.png`
- `app/src/test/roborazzi/05_assistant_hub.png`
- `app/src/test/roborazzi/06_wallet.png`
- `app/src/test/roborazzi/07_catalog_empty_search.png`
- `app/src/test/roborazzi/08_assistant_default.png`
- `app/src/test/roborazzi/09_assistant_budget_chip.png`
- `app/src/test/roborazzi/10_cart_empty.png`
- `app/src/test/roborazzi/11_cart_mesa_saldo.png`
- `app/src/test/roborazzi/12_cart_tarjeta.png`
- `app/src/test/roborazzi/13_confirm_cash.png`
- `app/src/test/roborazzi/14_tracking_empty.png`
- `app/src/test/roborazzi/15_tracking_por_cobrar.png`
- `app/src/test/roborazzi/16_tracking_preparando.png`
- `app/src/test/roborazzi/17_wallet_add_money.png`
- `app/src/test/roborazzi/18_chat_welcome.png`
- `app/src/test/roborazzi/19_catalog_dark.png`
- `app/src/test/roborazzi/20_sticker_receipt.png`

## Test sources

- `app/src/test/java/com/vaiinilla/app/ui/screenshot/AlumnoScreenshotTest.kt`
- `app/src/test/java/com/vaiinilla/app/ui/screenshot/ScreenshotFixtures.kt`
- `app/src/test/java/com/vaiinilla/app/ui/screenshot/ScreenshotTheme.kt`

Screen size: **411×891 dp** (`@Config(qualifiers = "w411dp-h891dp-…")`). Theme: `VaiinillaTheme(Light)` via `ScreenshotTheme` (dark catalog uses `VaiinillaThemeMode.Dark`).
