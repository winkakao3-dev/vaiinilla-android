#!/usr/bin/env python3
"""Clase 2 (productor–consumidor): cada clave JSON que la app lee o manda existe en el backend.

La app declara sus claves de red con @Serializable + @SerialName en los DTO. Si el backend renombra o quita
una clave, la app la lee como «vacía» sin error (Z01, P08, T07…). Este script compara contra un listado de
identificadores del backend guardado en scripts/backend_claves.json, así corre en CI sin clonar el backend.

  python3 scripts/contrato_app_backend.py                 # compara (CI)
  python3 scripts/contrato_app_backend.py --regenerar [RUTA_BACKEND]   # refresca el listado desde el backend
  python3 scripts/contrato_app_backend.py --actualizar    # acepta las claves nuevas sin respaldo (ratchet)

Regla: una clave sin respaldo que NO esté en scripts/contrato_baseline.json hace fallar. Quita del baseline
cuando el backend la agregue. Las claves de DTO que solo viven en la app (no van a la red) se marcan con
`// @solo-app` en la línea de la propiedad.
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
KOTLIN = RAIZ / "app" / "src" / "main" / "java"
CLAVES = Path(__file__).with_name("backend_claves.json")
BASELINE = Path(__file__).with_name("contrato_baseline.json")
# DTO que solo guardan datos en el teléfono (DataStore/caché): no van a la red.
SOLO_LOCAL = re.compile(r"(Store|Preferences\w*|ReceivedNoticeLog|StudentCounterJson)\.kt$")
IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")


def claves_de_la_app():
    """{(archivo, clase): [clave_en_el_cable, ...]} de todo @Serializable data class."""
    salida = {}
    for archivo in sorted(KOTLIN.rglob("*.kt")):
        texto = archivo.read_text(encoding="utf-8")
        if "@Serializable" not in texto or SOLO_LOCAL.search(archivo.name):
            continue
        for m in re.finditer(r"@Serializable\s+(?:internal\s+|private\s+)?data class (\w+)\s*\(", texto):
            ini = m.end()
            nivel, i = 1, ini
            while i < len(texto) and nivel:
                nivel += {"(": 1, ")": -1}.get(texto[i], 0)
                i += 1
            cuerpo = texto[ini : i - 1]
            previo = 0
            for p in re.finditer(r"\bval\s+(\w+)\s*:", cuerpo):
                tramo = cuerpo[previo : p.start()]
                previo = p.end()
                linea = cuerpo[p.start() : cuerpo.find("\n", p.start()) if "\n" in cuerpo[p.start():] else len(cuerpo)]
                if "@solo-app" in linea or "@Transient" in tramo:
                    continue
                nombres = re.findall(r'@SerialName\("(\w+)"\)', tramo)
                clave = nombres[-1] if nombres else p.group(1)
                salida.setdefault((str(archivo.relative_to(RAIZ)), m.group(1)), []).append(clave)
    return salida


def regenerar(ruta):
    back = Path(ruta).resolve()
    palabras = set()
    for patron in ("src/**/*.js", "supabase/**/*.sql", "tests/**/*.js"):
        for f in back.glob(patron):
            palabras.update(IDENT.findall(f.read_text(encoding="utf-8", errors="ignore")))
    # Solo identificadores que podrían ser claves de cable (sin las palabras reservadas y de 2 letras o menos).
    palabras = sorted(p for p in palabras if len(p) > 2)
    CLAVES.write_text(json.dumps(palabras, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"backend_claves.json: {len(palabras)} identificadores")


def main():
    args = sys.argv[1:]
    if args and args[0] == "--regenerar":
        regenerar(args[1] if len(args) > 1 else RAIZ.parent / "vaiinilla_back")
        return 0
    backend = set(json.loads(CLAVES.read_text(encoding="utf-8")))
    sin_respaldo = {}
    total = 0
    for (archivo, clase), claves in claves_de_la_app().items():
        for c in claves:
            total += 1
            if c not in backend and len(c) > 2:
                sin_respaldo.setdefault(f"{archivo}::{clase}", []).append(c)
    plano = sorted(f"{k}.{c}" for k, v in sin_respaldo.items() for c in v)
    if args and args[0] == "--actualizar":
        BASELINE.write_text(json.dumps(plano, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"baseline actualizado: {len(plano)} claves sin respaldo de {total}")
        return 0
    permitido = set(json.loads(BASELINE.read_text(encoding="utf-8"))) if BASELINE.exists() else set()
    nuevas = [p for p in plano if p not in permitido]
    resueltas = [p for p in permitido if p not in plano]
    print(f"claves de la app: {total}; sin respaldo en el backend: {len(plano)} (permitidas {len(permitido)})")
    if resueltas:
        print(f"  {len(resueltas)} del baseline ya tienen respaldo: corre --actualizar para bajarlas")
    if nuevas:
        print("FALLA: la app lee/manda claves que el backend no tiene (¿renombrado o no desplegado?):")
        for p in nuevas:
            print("  -", p)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
