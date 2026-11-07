#!/usr/bin/env python3
"""Clase 6 (fuga de contexto al cambiar de tienda): ningún campo de OperationalUiState puede sobrevivir al cambio
de negocio sin que alguien lo haya decidido.

`OperationalViewModel.setRole` reinicia el estado cuando cambia el rol o el negocio. Cada campo de
`OperationalUiState` debe (a) reiniciarse en ese `copy(...)`, o (b) estar en scripts/cambio_de_tienda_baseline.json
con el motivo de por qué es seguro que sobreviva (o «POR REVISAR» si nadie lo ha revisado). Un campo nuevo que
no esté en ninguno de los dos hace fallar: así un dato de un negocio no se queda pegado en la pantalla del otro.

  python3 scripts/guardias_cambio_de_tienda.py
  python3 scripts/guardias_cambio_de_tienda.py --actualizar   # solo para quitar entradas ya reiniciadas
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
OPER = RAIZ / "app" / "src" / "main" / "java" / "com" / "vaiinilla" / "app" / "ui" / "operational"
BASELINE = Path(__file__).with_name("cambio_de_tienda_baseline.json")


def campos_del_estado():
    t = (OPER / "OperationalUiState.kt").read_text(encoding="utf-8")
    m = re.search(r"data class OperationalUiState\s*\((.*?)\n\)", t, re.S)
    return re.findall(r"^\s*val\s+(\w+)\s*:", m.group(1), re.M)


def campos_reiniciados():
    v = (OPER / "OperationalViewModel.kt").read_text(encoding="utf-8")
    i = v.index("fun setRole(role: OperationalRole)")
    fin = v.index("if (role == OperationalRole.ADMIN)", i)
    cuerpo = v[i:fin]
    bloque = cuerpo[cuerpo.index("_uiState.value.copy(") :]
    return set(re.findall(r"^\s+(\w+)\s*=", bloque, re.M))


def main():
    sobreviven = sorted(set(campos_del_estado()) - campos_reiniciados())
    permitido = json.loads(BASELINE.read_text(encoding="utf-8")) if BASELINE.exists() else {}
    if "--actualizar" in sys.argv:
        nuevo = {c: permitido.get(c, "POR REVISAR") for c in sobreviven}
        BASELINE.write_text(json.dumps(nuevo, indent=1, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
        print(f"baseline: {len(nuevo)} campos sobreviven al cambio de tienda")
        return 0
    nuevos = [c for c in sobreviven if c not in permitido]
    ya = [c for c in permitido if c not in sobreviven]
    pendientes = [c for c, why in permitido.items() if why == "POR REVISAR" and c in sobreviven]
    print(f"campos que sobreviven al cambio de tienda: {len(sobreviven)} ({len(pendientes)} POR REVISAR)")
    if ya:
        print(f"  ya se reinician, quítalos del baseline: {', '.join(ya)}")
    if nuevos:
        print("FALLA: campos nuevos de OperationalUiState que setRole no reinicia al cambiar de negocio:")
        for c in nuevos:
            print("  -", c)
        print("Reinícialos en setRole o agrégalos a scripts/cambio_de_tienda_baseline.json con el motivo.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
