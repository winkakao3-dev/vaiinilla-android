#!/bin/zsh
# Compila el arnés jvm-roles con heaps pequeños (la Mac tiene 8 GB: con los de gradle.properties ya se reinició)
# y deja su classpath para `vbuild jvm-roles reiniciar`. Sin argumentos: ktlint, VigiaTest y el arnés.
#   tools/vigia/compilar.sh && vbuild jvm-roles reiniciar && python3 tools/vigia/vigia.py explorar
cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
export JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
export ANDROID_HOME=${ANDROID_HOME:-/Volumes/KINGSTON1TB/Dev/android-sdk}
export JVM_ROLES_CLASSPATH_FILE=$HOME/.cache/vaiinilla/jvm-roles/classpath
mkdir -p $HOME/.cache/vaiinilla/jvm-roles
(( $# )) || set -- :app:ktlintCheck :app:testDevAlumnoDebugUnitTest --tests '*.VigiaTest' --tests '*.JvmRolesHarness' --rerun
# Guardia: si la memoria libre baja de 8 %, se apaga Gradle antes de que la Mac se congele.
( while sleep 5; do
    f=$(memory_pressure | awk -F': ' '/free percentage/{gsub("%","",$2);print $2}')
    if [ "$f" -lt 8 ]; then echo "GUARDIA: libre $f%"; pkill -f GradleWrapperMain; pkill -f GradleDaemon; exit; fi
  done ) & G=$!
s=$(date +%s)
nice -n 10 ./gradlew "$@" \
  --no-daemon --no-parallel --max-workers=1 --console=plain --quiet \
  -Dorg.gradle.jvmargs="-Xmx1792m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC -Dfile.encoding=UTF-8" \
  -Pkotlin.compiler.execution.strategy=in-process 2>&1 | grep -vE '^w: |^warning:|Configuration cache entry|SDK XML versions' | tail -40
rc=${pipestatus[1]}
kill $G 2>/dev/null
echo "rc=$rc $(( $(date +%s)-s ))s"
exit $rc
