# AGENTS.md

## Cursor Cloud specific instructions

### What this is
Single native Android app (**Vaiinilla**, VAI-10/VAI-11) — Kotlin + Jetpack Compose, built with Gradle (`./gradlew`). Not a monorepo; one module `:app`. There is no local server: the app runs from bundled JSON fixtures in `MOCK` mode (default) or against an external Railway backend in `REMOTE` mode. See `README.md` for the product flow and the standard commands.

### Toolchain (already provisioned in the VM image)
- JDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64` (project targets Java 17; JDK 21 is also present but do not build with it).
- Android SDK at `/opt/android-sdk` (platform `android-36`, `build-tools;36.0.0`, `platform-tools`).
- `~/.bashrc` exports `JAVA_HOME`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, and `PATH`. Interactive shells get these automatically. For non-interactive shells, `local.properties` (`sdk.dir=/opt/android-sdk`) is what Gradle actually reads, so the SDK is found regardless.
- `local.properties` is gitignored; the startup update script recreates it (`sdk.dir` + `vaiinillaDataSource=MOCK`) if missing.

### Build / lint / test / run
Standard commands live in `README.md`, `.github/workflows/android-ci.yml`, and `scripts/verify-on-mac.sh`. The full local verification is `./scripts/verify-on-mac.sh` (fixtures → audit → unit tests → lint → ktlint → assembleDebug). Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### Non-obvious gotchas
- No `/dev/kvm` in the cloud VM, so a hardware-accelerated Android emulator cannot boot. A software (`-no-accel`, TCG) emulator technically starts but does not finish booting in practical time — do not rely on it. To exercise/observe the real UI headlessly, use the repo's Roborazzi screenshot tests: `./gradlew recordRoborazziDebug` renders the actual production Compose screens to `app/src/test/roborazzi/*.png` (catalog, cart, order confirmation, tracking, etc.). Core order-placement logic is covered end-to-end by JVM unit tests (e.g. `OrderContractTest`, `UseCaseTest`, `OrderOperationalTest`) in MOCK mode.
- First `./gradlew` run downloads Gradle 8.13 + dependencies (a few minutes); subsequent runs use the `~/.gradle` cache.
- `MOCK` is the default data source and needs no network. `REMOTE` end-to-end additionally requires the external Railway backend and Firebase seed passwords / JWTs in `local.properties` (JWTs expire ~15 min); neither the backend nor Firebase live in this repo. See `docs/FIREBASE_SEED_AUTH.md` and `docs/VAI-11_DELIVERY_REPORT.md`.
- Demo tools (role selector, gallery, wallet, assistant) are gated to debug + "Solo pruebas" builds via `ALLOW_DEMO_TOOLS` / `SEED_AUTH_ENABLED` in `app/build.gradle.kts`.
