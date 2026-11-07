#!/bin/bash
# Una sola Gradle a la vez en esta Mac (8 núcleos, 8 GB), en el orden de la cola. Sin nice ni flags de memoria.
# Uso: scripts/gradle-build.sh <args de gradlew>   (corre en el worktree actual)
# GRADLE_QUEUE_BYPASS=1 se salta la cola (el candado sigue valiendo).
set -u
LOCK=${GRADLE_LOCK_DIR:-/tmp/vaiinilla-gradle.lock}
COLA=${GRADLE_QUEUE_FILE:-$HOME/.cache/gradle-queue/cola.txt}
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-/Volumes/KINGSTON1TB/Dev/gradle}
AQUI=$(git rev-parse --show-toplevel 2>/dev/null || pwd)

# Imprime "ruta|nombre" del primer pendiente con ruta conocida; nada si la cola está vacía.
primero_pendiente() {
  [ -f "$COLA" ] || return 0
  awk -F' \\| ' '/^#/||/^$/{next} /\| HECHA *$/{next} $3!="?"{print $3"|"$2; exit}' "$COLA"
}
esta_en_cola() { [ -f "$COLA" ] && awk -F' \\| ' -v p="$AQUI" '$3==p && !/\| HECHA *$/{f=1} END{exit !f}' "$COLA"; }

if [ "${GRADLE_QUEUE_BYPASS:-0}" != 1 ] && esta_en_cola; then
  while :; do
    sig=$(primero_pendiente)
    { [ -z "$sig" ] || [ "${sig%%|*}" = "$AQUI" ]; } && break
    echo "No es tu turno: le toca a '${sig#*|}' (${sig%%|*}). Espero..." >&2
    sleep 15
  done
fi

while ! mkdir "$LOCK" 2>/dev/null; do
  holder=$(cat "$LOCK/pid" 2>/dev/null)
  if [ -n "$holder" ] && ! kill -0 "$holder" 2>/dev/null; then rm -rf "$LOCK"; continue; fi
  echo "Esperando a Gradle de $(cat "$LOCK/dir" 2>/dev/null) (pid $holder)..." >&2
  sleep 5
done
echo $$ > "$LOCK/pid"; echo "$AQUI" > "$LOCK/dir"
trap 'rm -rf "$LOCK"' EXIT INT TERM

./gradlew "$@"
rc=$?
if [ $rc -eq 0 ] && [ "${GRADLE_QUEUE_BYPASS:-0}" != 1 ] && esta_en_cola; then
  awk -F' \\| ' -v OFS=' | ' -v p="$AQUI" '$3==p && !/\| HECHA *$/{print $0" | HECHA"; next} {print}' "$COLA" > "$COLA.tmp" && mv "$COLA.tmp" "$COLA"
fi
exit $rc