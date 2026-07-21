#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/app/src/main/java"

FORBIDDEN=$(find "$SOURCE" -type f | grep -Ei '/(Admin|Wallet|Cart|Checkout|Receipt|Sticker|Cashier|Kitchen|Waiter)[^/]*\.(kt|java)$' || true)
if [ -n "$FORBIDDEN" ]; then
  echo "Se encontraron módulos fuera de VAI-5:" >&2
  echo "$FORBIDDEN" >&2
  exit 1
fi

if grep -RniE 'stripe|cashback|recarga|impresora de recibos|ticket coleccionable' "$SOURCE" >/tmp/vai5_scope_hits.txt; then
  echo "Se encontraron conceptos fuera del alcance en código de producción:" >&2
  cat /tmp/vai5_scope_hits.txt >&2
  exit 1
fi

if grep -RniE 'Double|Float' "$SOURCE/com/vaiinilla/app/domain" >/tmp/vai5_money_hits.txt; then
  echo "No se permiten Double/Float en modelos de dominio monetarios:" >&2
  cat /tmp/vai5_money_hits.txt >&2
  exit 1
fi

echo "Alcance VAI-5 limpio."
