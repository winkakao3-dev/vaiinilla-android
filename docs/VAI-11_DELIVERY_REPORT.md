# Reporte de entrega — VAI-11

## Resumen

Implementación Android de seguimiento del alumno y pantallas operativas (Caja, Cocina, Mesero) con fixtures y cliente remoto contra Railway development.

## Evidencia E2E

- Video: `docs/evidence/VAI-11-e2e-remote.mp4`
- Flujo grabado: Alumno crea pedido efectivo → Caja cobra → Cocina prepara → entrega → seguimiento refleja `entregado`
- Backend: `https://vaiinillaback-development-3f6c.up.railway.app/api/v1/`
- PR: https://github.com/winkakao3-dev/vaiinilla-android/pull/3

## Alcance entregado

- Selector de rol y pantallas operativas comparables al mockup
- Seguimiento del alumno con polling incremental
- Cliente HTTP remoto (`Authorization: Bearer`), repositorios REMOTE/MOCK intercambiables
- Latidos de Caja/Cocina, sesión de caja, transiciones y cobro en efectivo
- Soporte de `qr_token` para entrega `para_llevar`
- Tokens por rol vía `local.properties` (sin Firebase Auth en la app)
- **Actualización:** REMOTE usa Firebase Email/Password seed login + `sesiones/contexto` (ver `docs/FIREBASE_SEED_AUTH.md`)
- `StaffPresenceCoordinator` para demo en un solo dispositivo
- `collectCash` envía `version_esperada` desde la UI (sin GET previo)
- Red en `Dispatchers.IO`; sincronización del bootstrap token al reinstalar

## Validación local

```bash
python3 scripts/validate_fixtures.py
./scripts/audit_scope_vai11.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Notas

- Los JWT expiran en ~15 min; en REMOTE la app los renueva con seed login automáticamente
- No commitear `local.properties`, `secrets/` ni passwords (excepto seed demo en código)
