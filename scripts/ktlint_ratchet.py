#!/usr/bin/env python3
"""Trinquete de ktlint: bloquea avisos nuevos sin exigir arreglar los viejos.

Uso (después de `./gradlew ktlintCheck -PktlintRatchet`):
  python3 scripts/ktlint_ratchet.py            # compara contra config/ktlint/ratchet.json
  python3 scripts/ktlint_ratchet.py --update   # reescribe el archivo (solo para bajar la cuenta)

Se cuenta por (archivo, regla), no por línea, así que mover código no rompe el baseline.
Falla si algún archivo/regla tiene más avisos que el baseline o si aparece uno que no estaba.
"""

from __future__ import annotations

import json
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "app" / "build" / "reports" / "ktlint"
BASELINE = ROOT / "config" / "ktlint" / "ratchet.json"


def collect() -> dict[str, dict[str, int]]:
    reports = sorted(REPORTS.rglob("*.xml"))
    if not reports:
        sys.exit("No hay reportes checkstyle en app/build/reports/ktlint. Corre antes: ./gradlew ktlintCheck -PktlintRatchet")
    seen: set[tuple[str, str, str, str]] = set()
    for report in reports:
        for file_node in ET.parse(report).getroot().iter("file"):
            path = Path(file_node.get("name", "")).as_posix()
            rel = path.split(ROOT.as_posix() + "/", 1)[-1]
            for error in file_node.iter("error"):
                rule = (error.get("source") or "").removeprefix("ktlint:") or "desconocida"
                seen.add((rel, rule, error.get("line", ""), error.get("column", "")))
    counts: dict[str, Counter[str]] = {}
    for rel, rule, _line, _col in seen:
        counts.setdefault(rel, Counter())[rule] += 1
    return {file: dict(sorted(rules.items())) for file, rules in sorted(counts.items())}


def main() -> int:
    current = collect()
    total = sum(sum(rules.values()) for rules in current.values())
    if "--update" in sys.argv:
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        BASELINE.write_text(json.dumps(current, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Baseline reescrito: {total} avisos en {len(current)} archivos.")
        return 0

    baseline = json.loads(BASELINE.read_text(encoding="utf-8")) if BASELINE.exists() else {}
    regressions: list[str] = []
    improved = 0
    for file, rules in current.items():
        for rule, count in rules.items():
            allowed = baseline.get(file, {}).get(rule, 0)
            if count > allowed:
                regressions.append(f"  {file}: {rule} ({allowed} -> {count})")
    for file, rules in baseline.items():
        for rule, allowed in rules.items():
            if current.get(file, {}).get(rule, 0) < allowed:
                improved += 1
    if regressions:
        print(f"ktlint: {len(regressions)} avisos nuevos respecto al baseline:")
        print("\n".join(regressions))
        print("Arréglalos con ./gradlew ktlintFormat (o a mano). No subas el baseline para taparlos.")
        return 1
    print(f"ktlint ratchet OK: {total} avisos viejos, ninguno nuevo.")
    if improved:
        print(f"Hay {improved} avisos menos que el baseline: corre --update para bajarlo.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
