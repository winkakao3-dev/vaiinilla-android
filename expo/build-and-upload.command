#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
LOG=/tmp/vaiinilla-build-upload.log
exec > >(tee "$LOG") 2>&1
echo "==> $(date) build+upload start"

export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH:/opt/homebrew/bin"

test -f .env || cp .env.example .env
mkdir -p android
echo "sdk.dir=$ANDROID_HOME" > android/local.properties

if [ ! -f android/gradlew ]; then
  echo "android/ missing — run prebuild first"
  npx expo prebuild --platform android --no-install
fi

if grep -q '^org.gradle.jvmargs=' android/gradle.properties 2>/dev/null; then
  sed -i '' 's/^org.gradle.jvmargs=.*/org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8/' android/gradle.properties
else
  echo 'org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8' >> android/gradle.properties
fi

cd android
chmod +x gradlew
./gradlew clean assembleRelease --no-daemon
APK="app/build/outputs/apk/release/app-release.apk"
test -f "$APK"
cd ..
cp "$APK" ./vaiinilla-expo-latest.apk
cp "$APK" ./vaiinilla-expo-splashfix.apk
ls -lh ./vaiinilla-expo-latest.apk

echo "==> upload release"
gh release upload expo-v0.3.0-ui ./vaiinilla-expo-latest.apk --clobber || true

TAG=expo-v0.3.1-splash
cd /Users/kakao/Projects/vaiinilla-android
git tag -f "$TAG" HEAD -m "Expo splash fix APK"
git push -f origin "$TAG"
gh release delete "$TAG" -y 2>/dev/null || true
gh release create "$TAG" expo/vaiinilla-expo-latest.apk \
  --title "Vaiinilla Expo v0.3.1 — splash fix APK" \
  --notes "APK con fix: ya no se queda en el logo. Desinstala la app anterior e instala este APK."

gh release view "$TAG" --json url,assets
date > /tmp/vaiinilla-build-upload-done
echo DONE
