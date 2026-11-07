#!/usr/bin/env bash
# Compara (verify) o regraba (record) las capturas de Roborazzi de los tests que las generan.
#   scripts/screenshots.sh verify            # corre solo las clases con captureRoboImage y aplica el trinquete
#   scripts/screenshots.sh record [Clase...] # regraba referencias (de todas las clases o solo de las dadas)
#   scripts/screenshots.sh baseline          # reescribe config/screenshots/stale.json con el estado actual
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

mode="${1:-verify}"
shift || true

test_classes() {
  grep -rl "captureRoboImage" app/src/test/java \
    | sed -e 's#^app/src/test/java/##' -e 's#\.kt$##' -e 's#/#.#g' \
    | sort
}

filters=()
if [[ $# -gt 0 ]]; then
  for c in "$@"; do filters+=(--tests "*$c"); done
else
  while read -r c; do filters+=(--tests "$c"); done < <(test_classes)
fi

case "$mode" in
  verify|baseline)
    ./gradlew verifyRoborazziDevAlumnoDebug "${filters[@]}" --continue || true
    if [[ "$mode" == baseline ]]; then python3 scripts/screenshots_ratchet.py --update; else python3 scripts/screenshots_ratchet.py; fi
    ;;
  record)
    ./gradlew recordRoborazziDevAlumnoDebug "${filters[@]}"
    ;;
  *)
    echo "Uso: $0 verify|record|baseline" >&2
    exit 2
    ;;
esac
