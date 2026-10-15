#!/bin/zsh
# La revisión completa antes de subir un AAB, en un solo comando:
#   1. compila con heaps pequeños y corre VigiaTest (compilar.sh);
#   2. explora los 4 roles con red normal, con fallas, lenta y caída, en solo lectura;
#   3. corre los flujos: los de escrituras bloqueadas (no cambian el sandbox) y los de punta a punta (crean
#      pedidos en el sandbox y los cierran al final).
# Sale con 0 solo si todo quedó sin errores; los informes quedan en ~/.cache/vaiinilla/vigia/.
#   tools/vigia/vigilar.sh               todo
#   tools/vigia/vigilar.sh --sin-flujos  sin escribir nada en el sandbox de dev
#   tools/vigia/vigilar.sh --sin-compilar  reusa el classpath de la última compilación
cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
flujos=1 compilar=1
for a in "$@"; do
  case $a in
    --sin-flujos) flujos=0 ;;
    --sin-compilar) compilar=0 ;;
    *) echo "No conozco $a"; exit 64 ;;
  esac
done
V() { python3 tools/vigia/vigia.py "$@"; }

if (( compilar )); then
  tools/vigia/compilar.sh || { echo "❌ no compila o VigiaTest falla"; exit 1; }
fi
vbuild jvm-roles reiniciar || exit 1
trap 'vbuild jvm-roles parar >/dev/null 2>&1' EXIT

rc=0 resumen=()
for red in normal "fallas 30" "lento 6000" caido; do
  echo "\n=== explorar · red $red ==="
  if V explorar --red "$red"; then resumen+=("✅ explorar ($red)"); else rc=1; resumen+=("❌ explorar ($red)"); fi
done
if (( flujos )); then
  echo "\n=== flujos ==="
  if python3 tools/vigia/flujos.py; then resumen+=("✅ flujos"); else rc=1; resumen+=("❌ flujos"); fi
fi

echo "\n=== vigía ==="
printf '%s\n' "${resumen[@]}"
(( rc == 0 )) && echo "Listo para subir." || echo "Hay errores: revisa los informe.md de arriba antes de subir."
exit $rc
