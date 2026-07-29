#!/usr/bin/env bash
# Installs pera-design into this workspace:
#   .cursor/skills/pera-design/  — Cursor skill (SKILL.md, reference/, code/, …)
#   references/                  — asset library at workspace root (paths used in SKILL.md)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST_SKILL="${ROOT}/.cursor/skills/pera-design"
DEST_REFS="${ROOT}/references"
REPO_URL="${1:-https://github.com/winkakao3-dev/pera-design.git}"
TMP="$(mktemp -d)"

cleanup() { rm -rf "${TMP}"; }
trap cleanup EXIT

echo "Cloning ${REPO_URL} …"
git clone --depth 1 "${REPO_URL}" "${TMP}/pera-design"

SKILL_SRC="${TMP}/pera-design/.cursor/skills/pera-design"
REFS_SRC="${TMP}/pera-design/references"

if [[ ! -f "${SKILL_SRC}/SKILL.md" ]]; then
  echo "ERROR: expected ${SKILL_SRC}/SKILL.md — repo layout may have changed." >&2
  exit 1
fi

if [[ ! -d "${REFS_SRC}" ]]; then
  echo "ERROR: expected ${REFS_SRC} — references folder missing." >&2
  exit 1
fi

echo "Installing skill → ${DEST_SKILL}"
rm -rf "${DEST_SKILL}"
mkdir -p "${ROOT}/.cursor/skills"
cp -R "${SKILL_SRC}" "${DEST_SKILL}"

echo "Installing references → ${DEST_REFS}"
rm -rf "${DEST_REFS}"
cp -R "${REFS_SRC}" "${DEST_REFS}"

echo "Done."
echo "  Skill:       ${DEST_SKILL}/SKILL.md"
echo "  References:  ${DEST_REFS} ($(du -sh "${DEST_REFS}" | cut -f1))"
echo "  Example:     ${DEST_REFS}/examples/uber-navbar-replica.html"
