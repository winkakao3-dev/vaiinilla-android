# AGENTS.md

## Cursor Cloud specific instructions

### What this repo is
Single native Android app (`:app`, Kotlin + Jetpack Compose, Gradle KTS) — the Vaiinilla
"alumno" ordering flow (catalog → cart → cash → confirmation). Build/test/run commands are
documented in `README.md` and `scripts/verify-on-mac.sh`; the CI pipeline is
`.github/workflows/android-ci.yml`.

### Branches
`main` is an empty init commit — all real code lives on `feature/*` branches. The most complete
is `feature/alumno-ui-v2-demo-parity` (builds on VAI-11 → VAI-10 → VAI-5). Base environment/setup
work on the relevant feature branch, not `main`.

### Toolchain (already provisioned in the VM snapshot)
- JDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64` (system default `java` is JDK 21).
- Android SDK at `$HOME/android-sdk` (`platform-tools`, `platforms;android-36`, `build-tools;36.0.0`).
- Gradle 8.13 via the wrapper (`./gradlew`), AGP 8.13.2, Kotlin 2.2.21, compileSdk/targetSdk 36, minSdk 26.

### Non-obvious gotchas
- The Gradle JVM is pinned in `~/.gradle/gradle.properties` (`org.gradle.java.home=<JDK17>`) because
  the system default `java` is JDK 21. Non-interactive shells do NOT source `~/.bashrc`, so do not
  rely on `JAVA_HOME`/`ANDROID_HOME` being exported — `./gradlew` still works because of that pin
  plus `sdk.dir` in `local.properties` (gitignored; `sdk.dir=$HOME/android-sdk`).
- No `/dev/kvm` in this VM: an Android emulator will not run, so the GUI app and `androidTest`
  instrumentation cannot be executed here. Verify changes with JVM unit tests + lint + `assembleDebug`.
  The core order-flow logic is fully covered by `app/src/test` unit tests.
- Data source defaults to `MOCK` (offline fixtures) and works with no secrets. `REMOTE` mode needs
  RS256 JWT tokens (see `local.properties.example`) that are not present in this environment.
- Scope audits: this branch's CI uses `scripts/audit_scope_vai11.sh` (passes). The older
  `scripts/audit_scope.sh` is VAI-10-only and intentionally fails on VAI-11 code — do not use it here.

### Verify pipeline (matches CI)
```
python3 scripts/validate_fixtures.py
./scripts/audit_scope_vai11.sh
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug
./gradlew --no-daemon assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
```
