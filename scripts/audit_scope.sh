#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/app/src/main/java"

FORBIDDEN=$(find "$SOURCE" -type f | grep -Ei '/(Admin|Wallet|Receipt|Sticker|Cashier|Kitchen|Waiter)[^/]*\.(kt|java)$' || true)
if [ -n "$FORBIDDEN" ]; then
  echo "Se encontraron módulos fuera de VAI-10:" >&2
  echo "$FORBIDDEN" >&2
  exit 1
fi

if grep -RniE 'cobros-efectivo|/transiciones|latidos|stripe|recarga|reembolso|cancelaci[oó]n|ticket coleccionable|reimprimir' "$SOURCE" >/tmp/vai10_scope_hits.txt; then
  echo "Se encontraron endpoints o funciones fuera del alcance VAI-10:" >&2
  cat /tmp/vai10_scope_hits.txt >&2
  exit 1
fi

if grep -RniE 'Double|Float' "$SOURCE/com/vaiinilla/app/domain" >/tmp/vai10_money_hits.txt; then
  echo "No se permiten Double/Float en dominio monetario:" >&2
  cat /tmp/vai10_money_hits.txt >&2
  exit 1
fi

if grep -RnE 'PaymentMethod\.[A-Z][A-Z_]*' "$SOURCE" | grep -v 'PaymentMethod.CASH' >/tmp/vai10_payment_hits.txt; then
  echo "VAI-10 solo puede usar PaymentMethod.CASH:" >&2
  cat /tmp/vai10_payment_hits.txt >&2
  exit 1
fi

if grep -RnE 'OrderDestination\.[A-Z][A-Z_]*' "$SOURCE" | grep -v 'OrderDestination.TAKE_AWAY' >/tmp/vai10_destination_hits.txt; then
  echo "VAI-10 solo puede usar OrderDestination.TAKE_AWAY:" >&2
  cat /tmp/vai10_destination_hits.txt >&2
  exit 1
fi

echo "Alcance VAI-10 limpio."
