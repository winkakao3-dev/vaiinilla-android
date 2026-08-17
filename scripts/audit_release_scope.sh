#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/app/src/main/java"

if grep -RniE 'Double|Float' "$SOURCE/com/vaiinilla/app/domain" >/tmp/vaiinilla_money_hits.txt; then
  echo "No se permiten Double/Float en dominio monetario:" >&2
  cat /tmp/vaiinilla_money_hits.txt >&2
  exit 1
fi

echo "Alcance de release limpio."
