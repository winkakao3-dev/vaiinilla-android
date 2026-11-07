#!/usr/bin/env python3
"""Trinquete de capturas de pantalla (Roborazzi).

Después de `scripts/screenshots.sh verify` lee app/build/test-results/roborazzi/*/results-summary.json y
compara contra config/screenshots/stale.json, la lista de capturas que ya estaban distintas o sin referencia
cuando se armó la red de seguridad. Bloquea cuando:
  - una captura que estaba limpia ahora cambió (más de la tolerancia) o perdió su referencia;
  - aparece una captura nueva sin referencia (hay que grabarla: scripts/screenshots.sh record).
Avisa, sin bloquear, de las capturas viejas que ya quedaron limpias (bórralas de stale.json con --update).

  python3 scripts/screenshots_ratchet.py [--update] [--tolerance 0.001]
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "app" / "build" / "test-results" / "roborazzi"
GOLDEN_DIR = (ROOT / "app" / "src" / "test" / "roborazzi").as_posix() + "/"
STALE = ROOT / "config" / "screenshots" / "stale.json"
DEFAULT_TOLERANCE = 0.001  # 0.1 % de píxeles distintos; absorbe el ruido de antialias


def load_results() -> dict[str, tuple[str, float]]:
    summaries = sorted(RESULTS.glob("*/results-summary.json"))
    if not summaries:
        sys.exit("No hay results-summary.json. Corre antes: scripts/screenshots.sh verify")
    current: dict[str, tuple[str, float]] = {}
    for summary in summaries:
        for item in json.loads(summary.read_text(encoding="utf-8")).get("results", []):
            golden = item.get("golden_file_path", "")
            if not golden.startswith(GOLDEN_DIR):
                continue  # capturas de auditoría que se graban en build/, no son referencias
            current[golden.removeprefix(GOLDEN_DIR)] = (item.get("type", ""), float(item.get("diff_percentage") or 0))
    return current


def main() -> int:
    tolerance = DEFAULT_TOLERANCE
    if "--tolerance" in sys.argv:
        tolerance = float(sys.argv[sys.argv.index("--tolerance") + 1])
    current = load_results()
    bad = {name for name, (kind, diff) in current.items() if kind == "added" or (kind == "changed" and diff > tolerance)}

    if "--update" in sys.argv:
        previous = set(json.loads(STALE.read_text(encoding="utf-8"))) if STALE.exists() else set()
        # Una corrida parcial no puede declarar limpia una captura que no corrió.
        kept = {name for name in previous if name not in current} | bad
        STALE.parent.mkdir(parents=True, exist_ok=True)
        STALE.write_text(json.dumps(sorted(kept), indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"stale.json: {len(kept)} capturas pendientes de regrabar ({len(current)} comparadas en esta corrida).")
        return 0

    stale = set(json.loads(STALE.read_text(encoding="utf-8"))) if STALE.exists() else set()
    new_bad = sorted(bad - stale)
    now_clean = sorted(name for name in stale if name in current and name not in bad)
    clean = sum(1 for name in current if name not in bad)
    print(f"Capturas comparadas: {len(current)}. Limpias: {clean}. Viejas pendientes: {len(bad & stale)}.")
    if now_clean:
        print(f"{len(now_clean)} capturas viejas ya quedaron limpias; corre --update para sacarlas de stale.json.")
    if new_bad:
        print(f"{len(new_bad)} capturas rotas que antes estaban bien:")
        for name in new_bad:
            kind, diff = current[name]
            print(f"  {name}: {kind} {diff:.4%}" if kind == "changed" else f"  {name}: sin referencia")
        print("Si el cambio es intencional, regraba: scripts/screenshots.sh record (y sube los PNG).")
        return 1
    print("Capturas OK: ninguna regresión nueva.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
