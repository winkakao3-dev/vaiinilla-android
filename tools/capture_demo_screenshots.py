#!/usr/bin/env python3
"""Capture demo HTML phone frames as PNGs for visual comparison."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html"
OUT = ROOT / "docs/ui-v2/screenshot-compare/demo"
# Demo screens mapped to Kotlin alumno path (Entrega 01 + VAI-25 discovery)
SCREEN_IDS = [
    "01",
    "02",
    "07",
    "13",
    "16",
    "19",
    "20",
    "21",
    "22",
    "23",
    "24",
    "09",
    "25",
]

CAPTURE_JS = """
const ids = %s;
const outDir = %s;
for (const id of ids) {
  const el = document.querySelector(`.demo-screen[data-id="${id}"] .phone-frame`);
  if (!el) {
    console.warn('missing frame', id);
    continue;
  }
  el.scrollIntoView({ block: 'center' });
  await new Promise(r => setTimeout(r, 120));
}
return ids.filter(id => document.querySelector(`.demo-screen[data-id="${id}"] .phone-frame`));
""" % (repr(SCREEN_IDS), repr(str(OUT)))


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    for screen_id in SCREEN_IDS:
        url = f"file://{HTML}#screen={screen_id}"
        dest = OUT / f"demo_{screen_id}.png"
        cmd = [
            "google-chrome",
            "--headless=new",
            "--disable-gpu",
            "--hide-scrollbars",
            "--window-size=500,950",
            f"--screenshot={dest}",
            url,
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(result.stderr or result.stdout, file=sys.stderr)
            return result.returncode
        print(f"saved {dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
