#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

api_url="${VAIINILLA_API_BASE_URL:-}"
if [[ -z "$api_url" && -f local.properties ]]; then
  api_url="$(awk -F= '$1 == "vaiinillaApiBaseUrl" { sub(/^[^=]*=/, ""); print; exit }' local.properties)"
fi

if [[ -z "$api_url" ]]; then
  echo "FAIL: falta VAIINILLA_API_BASE_URL/vaiinillaApiBaseUrl." >&2
  exit 1
fi

case "$api_url" in
  https://localhost.invalid/*|*localhost*|*127.0.0.1*)
    echo "FAIL: la APK usaría un backend local/inválido: $api_url" >&2
    exit 1
    ;;
  https://*/api/v1/|https://*/api/v1)
    ;;
  *)
    echo "FAIL: la API debe ser HTTPS y terminar en /api/v1/: $api_url" >&2
    exit 1
    ;;
esac

echo "PASS: API base apta para dispositivo real: $api_url"
echo "Running focused app/backend regression tests..."
./gradlew --no-daemon testDebugUnitTest \
  --tests 'com.vaiinilla.app.StudentAuthViewModelTest' \
  --tests 'com.vaiinilla.app.OrderUserDtoNullabilityTest' \
  --tests 'com.vaiinilla.app.OrderRepositorySelectionTest' \
  --tests 'com.vaiinilla.app.StripeOrderContractTest' \
  --tests 'com.vaiinilla.app.StripeCheckoutPolicyTest' \
  --tests 'com.vaiinilla.app.StripeIdempotencyPersistenceTest' \
  --tests 'com.vaiinilla.app.ui.order.StripeCheckoutUiTest' \
  --tests 'com.vaiinilla.app.ConfirmationTicketCopyTest' \
  --tests 'com.vaiinilla.app.RemoteWalletRepositoryTest' \
  --tests 'com.vaiinilla.app.RemoteAuthorizedAccessRepositoryTest'

echo "PASS: app/backend regression gate"
