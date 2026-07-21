# Vaiinilla Android — VAI-5

Base nativa Android para **Entrega 01 — Pedido en efectivo E2E**. Este repositorio corresponde únicamente a **VAI-5: Kotlin/Compose, arquitectura y CI**.

La fuente de verdad es la bóveda:

- `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/README.md`
- `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/CONTEXT.md`
- `Vaiinilla/Modulos/Entrega-01-Pedido-Efectivo/CONTRACTS.md` v1.0

## Qué demuestra

- Aplicación Android con Jetpack Compose y navegación ejecutable.
- Separación `core / data / domain / ui`.
- Inyección de dependencias con Hilt.
- Cambio entre repositorio `MOCK` y `REMOTE` sin modificar pantallas.
- Cliente remoto deliberadamente vacío hasta recibir OpenAPI aprobado.
- Fixtures JSON canónicos con nombres `snake_case` del contrato.
- Mapeo explícito DTO → dominio; `imagen_url` se conserva como campo contractual.
- Tokens futuros protegidos con Android Keystore.
- Validación automática de fixtures, alcance, tests, lint y build en CI.

No implementa carrito, creación de pedido, cobro ni seguimiento. Esas funciones corresponden a VAI-10 y VAI-11.

## Limitaciones de validación

- No se dispone de emulador ni dispositivo físico en CI local.
- No se ejecutan pruebas instrumentadas (`connectedAndroidTest`).
- No se validó visualmente la navegación ni las pantallas.
- Solo se garantiza compilación, pruebas unitarias, lint, auditorías estáticas y generación del APK.

## Estructura

```text
app/src/main/java/com/vaiinilla/app/
├── core/       # configuración, cliente remoto vacío y seguridad
├── data/       # DTO, parser JSON, fixtures, repositorios y módulos Hilt
├── domain/     # modelos, reglas, interfaces y casos de uso
└── ui/         # ViewModel, navegación, pantallas y tema
```

Los fixtures viven únicamente en:

```text
app/src/main/assets/fixtures/
├── catalog.json
└── operational_status.json
```

La app y las pruebas leen esos mismos archivos. No existe una segunda copia hardcodeada en Kotlin.

## Requisitos

- macOS, Linux o Windows.
- JDK 17.
- Android SDK 36.
- Conexión a internet durante la primera sincronización de Gradle.

## Primera verificación

```bash
chmod +x gradlew scripts/*.sh scripts/*.py
python3 scripts/validate_fixtures.py
./scripts/audit_scope.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

APK esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Gradle Wrapper

El paquete incluye un bootstrap temporal verificado por SHA-256 para poder ejecutar Gradle 8.13 aun antes de generar el wrapper estándar. En la primera Mac con acceso a internet, reemplázalo por el wrapper oficial:

```bash
./scripts/install-standard-wrapper.sh
```

Después confirma:

```bash
ls -lh gradle/wrapper/gradle-wrapper.jar
./gradlew --version
```

Los archivos `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` y `gradle-wrapper.properties` generados deben subirse al repositorio.

## Fuente de datos

Por defecto se usan fixtures:

```bash
./gradlew assembleDebug -PvaiinillaDataSource=MOCK
```

Para comprobar que UI y dominio no dependen del mock:

```bash
./gradlew assembleDebug \
  -PvaiinillaDataSource=REMOTE \
  -PvaiinillaApiBaseUrl=https://api.dev.example/api/v1/
```

El modo `REMOTE` no inventa endpoints ni interpreta respuestas todavía. Devuelve un error controlado hasta recibir OpenAPI aprobado.

## Criterios de VAI-5

- [x] Compose y navegación.
- [x] Separación UI/domain/data.
- [x] Hilt como inyección de dependencias.
- [x] Repositorio simulado/remoto configurable.
- [x] Cliente remoto vacío.
- [x] Almacenamiento seguro preparado.
- [x] Fixtures JSON compatibles con el contrato y usados por app/tests.
- [x] Tests y CI definidos.
- [x] README con comandos y estructura.
- [x] Sin lógica monetaria en Composables o ViewModels.
- [x] Ejecutar Gradle con Android SDK 36 y obtener CI verde antes de marcar **Listo**.

Consulta `docs/VAI-5_SCOPE.md` y `docs/DELIVERY_REPORT.md` para el alcance y la evidencia técnica.

Consulta también `docs/MAC_VALIDATION.md` para los comandos finales en macOS.
