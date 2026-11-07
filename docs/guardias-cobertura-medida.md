# Guardias: cobertura medida por clase (segunda tanda)

Continúa `docs/guardias-clases-de-error.md` (PR #108). Esta tanda **solo agrega archivos de pruebas, scripts y CI**; no toca
`OperationalViewModel`, `Venue*` ni `AppNavHost`. Está construida sobre la rama de los rediseños
(`claude/project-thread-venue-context`: red único, bandeja única y `VenueContext`), así que las guardias corren sobre
el código ya rediseñado.

«Medido» = lo que el script o la prueba cuenta hoy; «estimado» = juicio sobre qué fracción de los casos futuros de la clase
se atrapa antes de un teléfono (ver advertencia al final).

| # | Clase | Qué se agregó | Medido hoy | Estimado antes → ahora |
|---|-------|---------------|------------|------------------------|
| 1 | Estado efímero en modales/timers | Nada nuevo: depende de migrar los 37 archivos del baseline a ViewModel (rediseño «estado por pantalla», aún en curso) | 37 archivos con `remember` en modales, 1 timer largo | ~55 % → ~55 % |
| 2 | Productor–consumidor | `scripts/contrato_app_backend.py`: cada clave `@SerialName` de un DTO de red existe en el backend (`scripts/backend_claves.json`, regenerable) | 1 152 claves revisadas, 0 sin respaldo | ~45 % → ~80 % (revisa nombres, no tipos) |
| 3 | Asimetría multi-rol | Backend: `paridad-roles-rls-ampliada.mjs` (llamadas al mesero, sesiones de caja, tutelas) | 15/15 pasan en Postgres 16 real (con la anterior) | ~60 % → ~80 % |
| 4 | Estado → texto | (sin cambios en esta tanda) | — | ~65 % |
| 5 | Controles muertos | `scripts/guardias_controles_muertos.py`: `TODO(` = CERO y acción `onX = {}` que ninguna llamada de producción pasa | 208 acciones opcionales revisadas, 0 sin cablear, 0 `TODO(` | ~50 % → ~80 % |
| 6 | Fuga de contexto | `scripts/guardias_cambio_de_tienda.py`: todo campo de `OperationalUiState` o se reinicia en `setRole` o está justificado | 34 campos: 18 se reinician, **16 sobreviven (14 POR REVISAR)** | ~70 % → ~75 % (sube a ~90 % al revisar los 14) |
| 7 | Filtros silenciosos | (sin cambios) | — | ~90 % |
| 8 | Letra grande | `FontScaleAuditTest`: Carrito, Caja, Cocina y Mesero, demo y peor caso, con letra 1.3 y 2.0 | 8 pantallas × 2 escalas; baseline pendiente de grabar en la Mac | ~50 % → ~85 % cuando se grabe el baseline |

## Hallazgo de la clase 6

Al cambiar de negocio, `setRole` no reinicia estos campos de `OperationalUiState` (la primera lectura del servidor los corrige,
pero mientras tanto se ven los del negocio anterior): `suggestedCashFloat`, `expectedCash`, `cashOpeningAmount`,
`cashOpenedAt`, `collectedToday`, `closeCash`, `kitchenPreparesUnpaid`, `unsyncedOfflineCash`, `storeImages`,
`storeImageBusy`, `storeImageNotice`, `catalogFailed`, `actionNotice`, `lastSyncedAt`. `keepKnownStaffState` además
conserva con `?:` los de caja. Arreglarlo es tocar `OperationalViewModel`, así que queda para cuando terminen los rediseños;
la guardia impide que se agreguen campos nuevos sin decidirlo.

## Cómo correr

```
python3 scripts/contrato_app_backend.py            # clase 2 (CI)
python3 scripts/guardias_controles_muertos.py      # clase 5 (CI)
python3 scripts/guardias_cambio_de_tienda.py       # clase 6 (CI)
python3 scripts/contrato_app_backend.py --regenerar ../vaiinilla_back   # tras cambiar el backend
FONT_SCALE_RECORD=1 ./gradlew :app:testDevAlumnoDebugUnitTest --tests '*FontScaleAuditTest*'   # Mac, graba el baseline
```

## Advertencia

Los porcentajes «estimado» son un juicio, no una medición de cobertura de código. Las pruebas Kotlin (`FontScaleAuditTest`)
no se compilaron en la nube (no hay Android SDK): las valida el CI o la Mac.
