#!/usr/bin/env bash
# Installs the pera-design Cursor skill into .cursor/skills/pera-design
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${ROOT}/.cursor/skills/pera-design"
REPO_URL="${1:-https://github.com/winkakao3-dev/pera-design.git}"

if [[ -d "${DEST}/.git" ]]; then
  echo "Updating existing skill at ${DEST}"
  git -C "${DEST}" pull --ff-only
else
  rm -rf "${DEST}"
  git clone --depth 1 "${REPO_URL}" "${DEST}"
fi

if [[ ! -f "${DEST}/SKILL.md" ]]; then
  echo "ERROR: ${DEST}/SKILL.md not found — repo layout may be wrong." >&2
  exit 1
fi

echo "Installed pera-design skill at ${DEST}"
echo "Files:"
ls -la "${DEST}"
