# Vaiinilla Android

Cliente Android nativo de **Vaiinilla** para la experiencia de alumno y la operación diaria del establecimiento. Este repositorio contiene la aplicación Android actual; los prototipos, entregas históricas y material de referencia que también viven en el repo no sustituyen al código de `app/` como fuente de verdad del producto.

## Estado actual

La app ya cubre el flujo principal de punta a punta para alumno y operación.

### Alumno

- descubrimiento y selección de establecimiento;
- deep links de establecimiento e invitaciones;
- registro, inicio de sesión, verificación y recuperación con Firebase Auth;
- catálogo, búsqueda y categorías;
- detalle de producto con opciones, personalización, alérgenos y tiempo estimado;
- carrito, destino del pedido y notas;
- pago en **efectivo** o con **Saldo Vaiinilla**;
- confirmación visual tipo receipt/sticker;
- seguimiento del pedido;
- cartera con saldo y movimientos por establecimiento;
- navegación persistente entre **Menú**, **Pedidos**, **Cartera** y **Carrito**.

### Operación

La misma app contiene superficies autorizadas para:

- **Caja** — sesión operativa, cobro de pedidos y recargas de saldo;
- **Cocina** — preparación y avance de pedidos;
- **Mesero** — flujo de entrega cuando corresponde.

Los permisos y estados siguen viniendo del backend; la UI no inventa autorización local ni mantiene una segunda fuente de verdad.

## Pagos

| Método | Estado |
| --- | --- |
| Efectivo | Implementado |
| Saldo Vaiinilla | Implementado |
| Tarjeta / Stripe | Integración pendiente |

Existen superficies y modelos para tarjeta dentro de la UI, pero el backend vigente todavía no expone el flujo digital completo. No se debe presentar tarjeta como método funcional hasta que la integración Stripe/backend esté conectada y verificada de punta a punta.

## Fuente de datos

En runtime la aplicación usa servicios reales:

- **Firebase Auth** para identidad;
- **backend remoto** para contexto del establecimiento, catálogo, pedidos, operación y wallet.

No existe un modo MOCK alternativo dentro del APK de producción. Fixtures, previews y baselines visuales existen únicamente para desarrollo y pruebas.

Los totales y estados confirmados por el servidor son autoritativos. El dominio monetario evita `Double`/`Float` para cálculos de dinero y usa representación decimal/`BigDecimal` cuando corresponde.

## Deep links

La entrada Android reconoce enlaces de `vaiinilla.app`, incluyendo:

```text
https://vaiinilla.app/e/{establecimiento}
https://vaiinilla.app/invitaciones/aceptar?token={token}
```

Los tokens de invitación se consumen y se eliminan del `Intent` después de capturarse.

## Arquitectura

```text
app/src/main/java/com/vaiinilla/app/
├── core/       # configuración y utilidades de infraestructura
├── data/       # auth, catálogo, contratos, discovery, operación, pedidos y wallet remotos
├── domain/     # modelos, repositorios y casos de uso
└── ui/         # navegación, estado, pantallas, componentes, temas y módulos por feature
```

La app usa una separación `data → domain → ui`, inyección con Hilt y navegación Compose. Las integraciones remotas permanecen detrás de repositorios para evitar que las pantallas dependan directamente de HTTP/Firebase.

## Stack

- Kotlin 2.x
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt / KSP
- Kotlinx Serialization
- Firebase Auth
- CameraX + ML Kit Barcode Scanning + ZXing
- Robolectric + Roborazzi para pruebas visuales JVM
- ktlint
- Gradle / Android Gradle Plugin

Configuración Android actual:

```text
compileSdk 36
targetSdk 36
minSdk 26
JDK 17
```

## UI y regresión visual

La interfaz mantiene el lenguaje visual propio de Vaiinilla: navegación flotante, sheets de producto, receipts/stickers, estados operativos, haptics y soporte de temas.

Las referencias visuales versionadas viven en:

```text
app/src/test/roborazzi/
docs/ui-v2/
```

Para comparar la UI contra las baselines comprometidas:

```bash
./gradlew :app:verifyRoborazziDebug --no-daemon
```

Para volver a grabarlas de forma deliberada:

```bash
./gradlew :app:recordRoborazziDebug --no-daemon
```

No se deben actualizar baselines únicamente para hacer pasar una regresión: primero se valida que el cambio visual sea intencional.

## Configuración local

Requisitos:

- Android Studio / Android SDK 36;
- JDK 17;
- acceso al backend de desarrollo que corresponda.

Copia el ejemplo local:

```bash
cp local.properties.example local.properties
```

Configura al menos `sdk.dir` y la URL del backend:

```properties
sdk.dir=/path/to/Android/sdk
vaiinillaApiBaseUrl=https://.../api/v1/
```

Las contraseñas de cuentas seed son opcionales, sólo se usan en builds de depuración/preview y deben permanecer en `local.properties` o propiedades Gradle. **Nunca se deben subir credenciales al repositorio.**

La configuración Firebase del proyecto Android está en `app/google-services.json`; no contiene las contraseñas seed.

## Build

```bash
./gradlew --no-daemon assembleDebug
```

También se puede pasar el backend explícitamente:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PvaiinillaApiBaseUrl=https://.../api/v1/
```

APK de debug:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Verificación

Antes de considerar un cambio terminado:

```bash
python3 scripts/validate_fixtures.py
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug
./gradlew --no-daemon ktlintCheck
./gradlew --no-daemon assembleDebug
```

La CI de GitHub ejecuta estas verificaciones en pull requests y pushes a `main`.

Para cambios de UI también se espera verificación visual con Roborazzi y, cuando el comportamiento dependa de gestos, cámara, navegación o integración real, prueba en dispositivo Android.

## Estructura del repositorio

```text
app/          # aplicación Android nativa actual
scripts/      # validación y utilidades de desarrollo
docs/         # contratos, especificaciones, evidencia e historial
artifacts/    # evidencia y artefactos auxiliares
expo/         # prototipo/referencia; no es el runtime Android actual
gallery/      # referencias visuales y material auxiliar
tools/        # herramientas de apoyo
```

### Sobre `docs/`

El repositorio conserva documentación de entregas anteriores (`VAI-10`, `VAI-11`, `VAI-26`, `VAI-27`) porque sigue siendo útil como historial, evidencia y referencia contractual. Sus secciones de “fuera de alcance”, fechas, responsables o estado de una entrega **no representan automáticamente el estado actual del producto**.

Para trabajo nuevo:

- usa el código de `app/` y el backend vigente como realidad de ejecución;
- usa `docs/source-of-truth/` para contratos e invariantes que sigan vigentes;
- usa `docs/ui-v2/` para especificaciones y regresión visual;
- usa `docs/history/` y los delivery reports como contexto histórico, no como roadmap actual.

## Regla de cambio

No cambiar en silencio contratos de API, estados de pedido, permisos, modelo de datos o arquitectura. Si una tarea requiere modificar alguno de esos límites, debe tratarse como una decisión explícita y verificarse en ambos lados del contrato.

---

**Vaiinilla Android es la implementación móvil nativa activa.** El README describe el producto actual; los tickets y reportes históricos quedan como evidencia dentro de `docs/`.