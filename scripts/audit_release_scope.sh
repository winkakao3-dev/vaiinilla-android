#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/app/src/main/java"

# Las coordenadas de ubicación no son dinero; son el único Double permitido, como propiedad (`val latitude: Double`)
# o como parámetro (`latitude: Double`), opcional o no. Se buscan los tipos como palabra completa, así que un nombre
# como `suggestedFloat` (el fondo de caja, que es un String) no cuenta.
if grep -RnwE 'Double|Float' "$SOURCE/com/vaiinilla/app/domain" |
  grep -vE '(val )?(latitude|longitude): Double\??( = null)?,?$' >/tmp/vaiinilla_money_hits.txt; then
  echo "No se permiten Double/Float en dominio monetario:" >&2
  cat /tmp/vaiinilla_money_hits.txt >&2
  exit 1
fi

echo "Alcance de release limpio."
