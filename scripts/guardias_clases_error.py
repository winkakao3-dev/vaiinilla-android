#!/usr/bin/env python3
"""Guardias estáticas para las clases de error que ya vimos (ver docs/guardias-clases-de-error.md).

Corre en segundos, sin Gradle ni Android SDK. Dos tipos de regla:
  - CERO: no se permite ningún caso (la clase ya se arregló en todo el código).
  - RATCHET: el código viejo tiene casos conocidos guardados en scripts/guardias_baseline.json; falla si
    aparece UNO NUEVO (o más de los guardados en un archivo). Al arreglar uno, baja el número con --actualizar.

Uso: python3 scripts/guardias_clases_error.py [--actualizar]
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
MAIN = RAIZ / "app" / "src"
BASELINE = Path(__file__).with_name("guardias_baseline.json")


def fuentes():
    for carpeta in sorted(MAIN.glob("*/java")):
        if carpeta.parent.name in ("test", "androidTest"):
            continue
        yield from sorted(carpeta.rglob("*.kt"))


def rel(p):
    return str(p.relative_to(RAIZ))


# ── Clase 7: filtros silenciosos (acentos) ─────────────────────────────────────────────────────────
BUSQUEDA = re.compile(
    r"\.(contains|startsWith)\(\s*(?:[^()]|\(\))*?(query|needle|search|busca|filtro|texto|term)(?:[^()]|\(\))*?ignoreCase\s*=\s*true",
    re.I,
)

# ── Clase 5: controles muertos ──────────────────────────────────────────────────────────────────────
BOTON_MUERTO = re.compile(
    r"\b(Button|IconButton|TextButton|OutlinedButton|FilledTonalButton|ElevatedButton|FilterChip|AssistChip)"
    r"\s*\(\s*onClick\s*=\s*\{\s*\}",
)

# ── Clase 1: estado efímero ─────────────────────────────────────────────────────────────────────────
RETARDO_LARGO = re.compile(r"\bdelay\(\s*(\d[\d_]*)\s*(?:L|\.toLong\(\))?\s*\)")
TIENE_MODAL = re.compile(r"\b(\w*BottomSheet|\w*Dialog)\s*\(")
ESTADO_TEMPORAL = re.compile(r"(?:=|\bby)\s*remember\s*\{\s*mutableStateOf")


def contar():
    cero, ratchet = {"busqueda_sin_normalizar": [], "boton_sin_accion": []}, {
        "timer_largo_en_pantalla": {},
        "estado_remember_en_modal": {},
    }
    for archivo in fuentes():
        texto = archivo.read_text(encoding="utf-8")
        nombre = rel(archivo)
        for n, linea in enumerate(texto.splitlines(), 1):
            if BUSQUEDA.search(linea) and "normalizeForSearch" not in linea:
                cero["busqueda_sin_normalizar"].append(f"{nombre}:{n}")
        for m in BOTON_MUERTO.finditer(texto):
            cero["boton_sin_accion"].append(f"{nombre}:{texto.count(chr(10), 0, m.start()) + 1}")
        largos = [m for m in RETARDO_LARGO.finditer(texto) if int(m.group(1).replace("_", "")) >= 3000]
        if largos and "@Composable" in texto:
            ratchet["timer_largo_en_pantalla"][nombre] = len(largos)
        if TIENE_MODAL.search(texto):
            n_estado = len(ESTADO_TEMPORAL.findall(texto))
            if n_estado:
                ratchet["estado_remember_en_modal"][nombre] = n_estado
    return cero, ratchet


def main():
    cero, ratchet = contar()
    if "--actualizar" in sys.argv:
        BASELINE.write_text(json.dumps(ratchet, indent=2, sort_keys=True, ensure_ascii=False) + "\n")
        print(f"Baseline actualizado: {rel(BASELINE)}")
        return 0
    base = json.loads(BASELINE.read_text()) if BASELINE.exists() else {}
    fallas = []
    for regla, casos in cero.items():
        for c in casos:
            fallas.append(f"[{regla}] {c}")
    for regla, por_archivo in ratchet.items():
        permitido = base.get(regla, {})
        for archivo, n in por_archivo.items():
            if n > permitido.get(archivo, 0):
                fallas.append(f"[{regla}] {archivo}: {n} casos (permitidos {permitido.get(archivo, 0)})")
    if fallas:
        print("GUARDIAS DE CLASES DE ERROR: FALLA")
        print("\n".join(fallas))
        print("\nQué hacer: ver docs/guardias-clases-de-error.md (cada regla explica el arreglo correcto).")
        return 1
    total = sum(len(c) for c in cero.values())
    print(f"GUARDIAS DE CLASES DE ERROR: OK ({total} casos en reglas CERO; ratchet sin casos nuevos)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
