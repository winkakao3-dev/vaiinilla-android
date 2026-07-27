#!/usr/bin/env python3
"""Extract embedded demo assets from Vaiinilla_Demo_Web_IA_CHAT.html into res/drawable-nodpi."""

from __future__ import annotations

import argparse
import base64
import re
from pathlib import Path

ASSET_PATTERN = re.compile(
    r'\.asset-([a-z0-9_]+)\{[^}]*url\("data:image/([^;]+);base64,([^"]+)"\)',
)


def extract_assets(html_path: Path, output_dir: Path) -> int:
    html = html_path.read_text(encoding="utf-8")
    output_dir.mkdir(parents=True, exist_ok=True)
    written = 0
    for name, fmt, payload in ASSET_PATTERN.findall(html):
        ext = "webp" if fmt == "webp" else "jpg" if fmt in {"jpeg", "jpg"} else fmt
        target = output_dir / f"{name}.{ext}"
        if target.exists():
            continue
        target.write_bytes(base64.b64decode(payload))
        written += 1
        print(f"wrote {target}")
    return written


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--html",
        type=Path,
        default=Path.home() / "Downloads" / "Vaiinilla_Demo_Web_IA_CHAT.html",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("app/src/main/res/drawable-nodpi"),
    )
    args = parser.parse_args()
    count = extract_assets(args.html, args.out)
    print(f"done ({count} new files)")


if __name__ == "__main__":
    main()
