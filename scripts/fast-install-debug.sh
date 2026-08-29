#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
START=$(date +%s)
# All current project dependencies are cached on the VPS. Offline avoids remote metadata checks.
if ! ./gradlew --offline :app:installDebug; then
  echo "Offline build missed a dependency; retrying with normal dependency resolution..." >&2
  ./gradlew :app:installDebug
fi
END=$(date +%s)
printf 'Fast debug install finished in %ss\n' "$((END-START))"
