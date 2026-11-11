#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/app/src/main/java"

# Lo que no es dinero sí puede ser Double: las coordenadas de ubicación y las cantidades de inventario (kilos,
# piezas, existencia, mínimo), como propiedad (`val latitude: Double`) o como parámetro (`quantity: Double`),
# opcional o no. Un costo o un precio nunca. Se buscan los tipos como palabra completa, así que un nombre como
# `suggestedFloat` (el fondo de caja, que es un String) no cuenta.
if grep -RnwE 'Double|Float' "$SOURCE/com/vaiinilla/app/domain" |
  grep -vE '(val )?(latitude|longitude|initialStock|minimum|quantity): Double\??( = null)?(,|\): String =)?$' \
    >/tmp/vaiinilla_money_hits.txt; then
  echo "No se permiten Double/Float en dominio monetario:" >&2
  cat /tmp/vaiinilla_money_hits.txt >&2
  exit 1
fi

echo "Alcance de release limpio."
