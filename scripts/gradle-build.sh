#!/bin/bash
# Una sola Gradle a la vez en esta Mac (8 núcleos, 8 GB). Sin nice. Config en ~/.gradle/gradle.properties.
# Uso: scripts/gradle-build.sh <args de gradlew>   (corre en el checkout/worktree actual)
set -u
LOCK=/tmp/vaiinilla-gradle.lock
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-/Volumes/KINGSTON1TB/Dev/gradle}

while ! mkdir "$LOCK" 2>/dev/null; do
  holder=$(cat "$LOCK/pid" 2>/dev/null)
  if [ -n "$holder" ] && ! kill -0 "$holder" 2>/dev/null; then rm -rf "$LOCK"; continue; fi
  echo "Esperando a Gradle de $(cat "$LOCK/dir" 2>/dev/null) (pid $holder)..." >&2
  sleep 5
done
echo $$ > "$LOCK/pid"; pwd > "$LOCK/dir"
trap 'rm -rf "$LOCK"' EXIT INT TERM

# Un solo daemon vivo: los de otros worktrees ociosos se apagan antes (compatibles se reutilizan solos).
pgrep -f 'GradleDaemon' | wc -l | xargs echo "daemons vivos:" >&2
exec_dir=$(pwd)
(cd "$exec_dir" && ./gradlew "$@")