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

production_api_url="https://vaiinillaback.up.railway.app/api/v1/"
dev_api_url="https://vaiinillaback-development.up.railway.app/api/v1/"
normalized_api_url="${api_url%/}/"

if [[ "$normalized_api_url" != "$production_api_url" && "$normalized_api_url" != "$dev_api_url" ]]; then
  echo "FAIL: endpoint no reconocido para Vaiinilla: $api_url" >&2
  echo "Esperado: $production_api_url o $dev_api_url" >&2
  exit 1
fi

echo "PASS: API base apta para dispositivo real: $api_url"
echo "Running focused app/backend regression tests..."
./gradlew --no-daemon testDevDebugUnitTest testProdDebugUnitTest \
  --tests 'com.vaiinilla.app.StudentAuthViewModelTest' \
  --tests 'com.vaiinilla.app.OrderUserDtoNullabilityTest' \
  --tests 'com.vaiinilla.app.OrderRepositorySelectionTest' \
  --tests 'com.vaiinilla.app.StripeOrderContractTest' \
  --tests 'com.vaiinilla.app.StripeCheckoutPolicyTest' \
  --tests 'com.vaiinilla.app.StripeIdempotencyPersistenceTest' \
  --tests 'com.vaiinilla.app.ui.order.StripeCheckoutUiTest' \
  --tests 'com.vaiinilla.app.ConfirmationTicketCopyTest' \
  --tests 'com.vaiinilla.app.RemoteWalletRepositoryTest' \
  --tests 'com.vaiinilla.app.RemoteAuthorizedAccessRepositoryTest' \
  --tests 'com.vaiinilla.app.EnvironmentSeparationTest'

echo "PASS: app/backend regression gate"
