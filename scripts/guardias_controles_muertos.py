#!/usr/bin/env python3
"""Clase 5 (controles muertos): un botón que se dibuja pero no hace nada.

Reglas:
  - CERO: `TODO(` en código de producción (la pantalla truena o promete algo que no existe).
  - RATCHET «sin cablear»: un composable de ui/ declara una acción `onX: (...) -> Unit = {}` y NINGUNA llamada de
    producción (fuera de @Preview) la pasa: el botón existe, pero tocarlo no hace nada. Los casos viejos viven en
    scripts/controles_muertos_baseline.json; uno nuevo hace fallar. Si la acción es opcional a propósito, el
    parámetro debe ser nullable (`onX: (() -> Unit)? = null`) y la pantalla debe ocultar el control cuando es null.

  python3 scripts/guardias_controles_muertos.py [--actualizar]
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
MAIN = RAIZ / "app" / "src" / "main" / "java"
BASELINE = Path(__file__).with_name("controles_muertos_baseline.json")

FUN = re.compile(r"@Composable\s+(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:internal|private|public)\s+)?fun\s+(\w+)\s*\(")
PARAM_VACIO = re.compile(r"\b(on[A-Z]\w*)\s*:\s*\(([^()]|\([^()]*\))*\)\s*->\s*Unit\s*=\s*\{\s*(?:_\s*(?:,\s*_\s*)*->\s*)?\}")


def cuerpo_balanceado(texto, desde):
    nivel, i = 1, desde
    while i < len(texto) and nivel:
        nivel += {"(": 1, ")": -1}.get(texto[i], 0)
        i += 1
    return texto[desde : i - 1]


def args_con_nombre(texto, desde):
    """Nombres `x =` de primer nivel dentro de los paréntesis de una llamada."""
    cuerpo = cuerpo_balanceado(texto, desde)
    nivel, nombres, token = 0, set(), ""
    for ch in cuerpo:
        if ch in "({[":
            nivel += 1
        elif ch in ")}]":
            nivel -= 1
        if nivel == 0 and ch == ",":
            token = ""
            continue
        token += ch if nivel == 0 else ""
        m = re.search(r"(\w+)\s*=$", token)
        if nivel == 0 and m and ch == "=":
            nombres.add(m.group(1))
    return nombres


def en_preview(texto, pos):
    ult = list(re.finditer(r"\bfun\s+(\w+)", texto[:pos]))
    previo = texto[max(0, ult[-1].start() - 80) : ult[-1].start()] if ult else ""
    return bool(ult) and ("Preview" in ult[-1].group(1) or "@Preview" in previo)


def analizar():
    archivos = sorted(MAIN.rglob("*.kt"))
    textos = {a: a.read_text(encoding="utf-8") for a in archivos}
    cero, declaradas = [], {}
    for a, t in textos.items():
        for m in re.finditer(r"\bTODO\(", t):
            cero.append(f"{a.relative_to(RAIZ)}:{t.count(chr(10), 0, m.start()) + 1}")
        if "/ui/" not in str(a):
            continue
        for f in FUN.finditer(t):
            cab = cuerpo_balanceado(t, f.end())
            for p in PARAM_VACIO.finditer(cab):
                declaradas.setdefault((str(a.relative_to(RAIZ)), f.group(1)), []).append(p.group(1))
    sin_cablear = []
    for (archivo, nombre), acciones in declaradas.items():
        pasadas, llamadas = set(), 0
        for t in textos.values():
            for c in re.finditer(r"(?<![\w.])" + re.escape(nombre) + r"\s*\(", t):
                if re.search(r"fun\s+$", t[max(0, c.start() - 6) : c.start()]) or en_preview(t, c.start()):
                    continue
                llamadas += 1
                pasadas |= args_con_nombre(t, c.end())
        if llamadas == 0:
            continue  # sin llamadas de producción: no es un control vivo (lo cubre la limpieza de código muerto)
        for accion in acciones:
            if accion not in pasadas:
                sin_cablear.append(f"{archivo}::{nombre}.{accion}")
    return cero, sorted(set(sin_cablear))


def main():
    cero, sin_cablear = analizar()
    if "--actualizar" in sys.argv:
        BASELINE.write_text(json.dumps(sin_cablear, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"baseline: {len(sin_cablear)} acciones sin cablear")
        return 0
    permitido = set(json.loads(BASELINE.read_text(encoding="utf-8"))) if BASELINE.exists() else set()
    nuevas = [s for s in sin_cablear if s not in permitido]
    fallas = [f"[TODO()] {c}" for c in cero] + [f"[accion_sin_cablear] {s}" for s in nuevas]
    if fallas:
        print("GUARDIAS DE CONTROLES MUERTOS: FALLA")
        print("\n".join(fallas))
        print("\nCablea la acción en la llamada, o hazla nullable y oculta el control cuando sea null.")
        return 1
    print(f"GUARDIAS DE CONTROLES MUERTOS: OK ({len(sin_cablear)} acciones viejas sin cablear en el baseline)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
