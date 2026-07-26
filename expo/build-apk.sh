#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

test -f .env || cp .env.example .env
echo "sdk.dir=$ANDROID_HOME" > android/local.properties

# heap
if grep -q '^org.gradle.jvmargs=' android/gradle.properties; then
  sed -i '' 's/^org.gradle.jvmargs=.*/org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8/' android/gradle.properties
else
  echo 'org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8' >> android/gradle.properties
fi

echo "==> Building release APK..."
cd android
chmod +x gradlew
./gradlew assembleRelease --no-daemon

APK="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK" ]; then
  echo "Release APK missing, trying debug..."
  ./gradlew assembleDebug --no-daemon
  APK="app/build/outputs/apk/debug/app-debug.apk"
fi

OUT="../vaiinilla-expo-latest.apk"
cp "$APK" "$OUT"
ls -lh "$APK" "$OUT"
echo "DONE:$OUT"
echo "$(cd .. && pwd)/vaiinilla-expo-latest.apk" > /tmp/vaiinilla-apk-path.txt
