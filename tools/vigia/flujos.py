#!/usr/bin/env python3
"""Flujos escritos a mano entre roles, con el vigía mirando y el backend como juez.

  flujos.py                      corre todos los flujos con la red normal
  flujos.py pedido-mostrador     corre solo esos
  flujos.py --red "fallas 30"    los mismos flujos con la red saboteada (lo que falle debe avisarse)
  flujos.py --lista              qué flujos hay

Cada paso espera a que aparezca lo que va a tocar (si no aparece: flujo-roto, con captura), junta lo que vio
el vigía durante el paso y, donde importa, compara la pantalla con la API de dev (pantalla-vs-backend).
A diferencia del explorador, los flujos sí escriben en el sandbox de dev: crean sus propios pedidos y al
final los cierran o cancelan, aunque el flujo se rompa a medias.

Informe en ~/.cache/vaiinilla/vigia/<fecha>-flujos/. Antes: tools/vigia/compilar.sh y vbuild jvm-roles reiniciar.
"""
import argparse
import datetime
import json
import re
import subprocess
import sys
import time
import traceback

from backend import Backend, BackendError
from vigia import OUT_ROOT, send, send_json

FLUJOS = {}
LISTA = re.compile(r"^'(.*)' \[\d+,\d+\]\[\d+,\d+\]", re.MULTILINE)


def flujo(nombre, descripcion):
    def registrar(fn):
        FLUJOS[nombre] = (descripcion, fn)
        return fn
    return registrar


class FlujoRoto(Exception):
    pass


class Flujo:
    def __init__(self, nombre, out, backend, red):
        self.nombre = nombre
        self.red = red
        self.out = out
        self.api = backend
        self.rol = None
        self.pasos = []
        self.hallazgos = []
        self.limpiezas = []
        self.revisadas = set()

    # --- lo que ve y anota ------------------------------------------------------------------------------

    def _anotar(self, tipo, gravedad, detalle, pantalla=(), red=()):
        n = len(self.hallazgos) + 1
        captura = f'{self.nombre}-{n:02d}-{tipo}.png'
        try:
            send('shot', [str(self.out / captura)])
        except RuntimeError:
            captura = None
        self.hallazgos.append({
            'n': n, 'flujo': self.nombre, 'rol': self.rol, 'tipo': tipo, 'gravedad': gravedad, 'detalle': detalle,
            'pantalla': list(pantalla), 'red': list(red), 'pasos': list(self.pasos), 'captura': captura,
        })
        print(f'    ⚠️  [{gravedad}] {tipo}: {detalle}')

    def _recoger(self):
        datos = send_json('vigia')
        for h in datos['hallazgos']:
            self._anotar(h['tipo'], h['gravedad'], h['detalle'], h['pantalla'], h['red'])

    def _paso(self, texto):
        self.pasos.append(f'{self.rol or "-"}: {texto}')
        print(f'  · {self.pasos[-1]}')

    def _pantalla(self):
        return send_json('nodos')

    def _textos(self):
        return [m.group(1) for m in LISTA.finditer(send('list'))]

    def _revisar_si_nueva(self, nodos):
        if nodos['pantalla'] in self.revisadas:
            return
        self.revisadas.add(nodos['pantalla'])
        for h in send_json('revisar')['hallazgos']:
            self._anotar(h['tipo'], h['gravedad'], h['detalle'], h['pantalla'], h['red'])

    # --- acciones ---------------------------------------------------------------------------------------

    def entrar(self, rol):
        self.rol = rol
        self._paso(f'entrar como {rol}')
        send('role', [rol])
        self._recoger()

    def deeplink(self, url):
        self._paso(f'abrir {url}')
        send('deeplink', [url])
        self._recoger()

    def ver(self, patron, segundos=20, opcional=False):
        """Espera a que algún texto en pantalla cumpla [patron] (regex, sin importar mayúsculas).

        Con [opcional] devuelve None si no aparece en lugar de romper el flujo (sirve para avisos que duran poco).
        """
        regex = re.compile(patron, re.IGNORECASE)
        limite = time.time() + segundos
        while True:
            for texto in self._textos():
                encontrado = regex.search(texto)
                if encontrado:
                    return encontrado
            if time.time() > limite:
                if opcional:
                    return None
                self._roto(f'nunca apareció un texto que cumpla /{patron}/ en {segundos} s')
            send('wait', [500 if opcional else 1000])

    def no_ver(self, patron, segundos=20):
        regex = re.compile(patron, re.IGNORECASE)
        limite = time.time() + segundos
        while any(regex.search(t) for t in self._textos()):
            if time.time() > limite:
                self._roto(f'el texto /{patron}/ siguió en pantalla después de {segundos} s')
            send('wait', [1000])

    def tocar(self, patron, segundos=20, opcional=False, campo=False):
        """Toca lo primero tocable cuyo texto cumpla [patron]; espera a que exista y esté habilitado.

        Los campos de texto solo cuentan con [campo]: un buscador con "Chocolate" escrito no es el producto.
        """
        regex = re.compile(patron, re.IGNORECASE)
        limite = time.time() + segundos
        while True:
            nodos = self._pantalla()
            candidatos = [n for n in nodos['nodos'] if regex.search(n['texto']) and bool(n.get('campo')) == campo]
            if candidatos:
                break
            if time.time() > limite:
                if opcional:
                    return None
                self._roto(f'no apareció nada tocable que cumpla /{patron}/ en {segundos} s')
            send('wait', [1000])
        self._revisar_si_nueva(nodos)
        nodo = candidatos[0]
        self._paso(f'tocar "{nodo["texto"][:70]}"')
        r = send_json('tocar', [nodo['id']])
        self._recoger()
        if r['efecto'] == 'nada' and not nodo.get('marcado'):  # tocar una opción ya elegida no tiene por qué cambiar nada
            self._anotar('toque-sin-efecto', 'sospecha',
                         f'Tocar "{nodo["texto"][:70]}" no cambió nada en pantalla ni hizo peticiones.', nodos['textos'])
        return r

    def escribir(self, texto):
        self._paso(f'escribir "{texto}"')
        send('text', [texto])
        self._recoger()

    def sabotear(self):
        """Pone la red pedida con --red desde aquí: la preparación del flujo corre con red normal."""
        if self.red != ['normal']:
            self._paso(f'red {" ".join(self.red)}')
            send('red', self.red)

    def red_normal(self):
        send('red', ['normal'])

    def esperar(self, ms):
        send('wait', [ms])
        self._recoger()

    def verificar(self, descripcion, condicion, segundos=30):
        """[condicion]() devuelve (ok, detalle) leyendo la API; si no se cumple a tiempo, la pantalla y el backend no coinciden."""
        self._paso(f'verificar en el backend: {descripcion}')
        ok, detalle = self._backend(condicion, segundos)
        if ok:
            return detalle
        self._anotar('pantalla-vs-backend', 'error', f'{descripcion}: {detalle}', self._textos()[:12])
        raise FlujoRoto(f'{descripcion}: {detalle}')

    def accion(self, boton, exito, descripcion, condicion, intentos=3):
        """Toca [boton] hasta que el backend confirme [condicion], como lo haría una persona con mala señal.

        Si la pantalla anuncia [exito] (regex) y el backend no cambió, es un falso éxito: la persona se va
        creyendo que ya quedó. Si falla sin aviso, el vigía ya lo anota como error-mudo. Con la red normal
        tiene que salir al primer intento; con la red saboteada se reintenta y, al final, con la red normal.
        """
        for intento in range(1, intentos + 1):
            if intento == intentos and self.red != ['normal']:
                self._paso('red normal para el último intento')
                self.red_normal()
            self.tocar(boton)
            anuncio = self.ver(exito, segundos=6, opcional=True)
            ok, detalle = self._backend(condicion, 12 if anuncio else 6)
            if ok:
                if intento > 1 and self.red == ['normal']:
                    self._anotar('reintento', 'sospecha', f'{descripcion}: quedó hasta el intento {intento} con la red normal.')
                return detalle
            if anuncio:
                self._anotar('falso-exito', 'error',
                             f'{descripcion}: la pantalla dijo "{anuncio.group(0)}" pero el backend sigue en: {detalle}',
                             self._textos()[:12])
            self._recoger()
        self._anotar('pantalla-vs-backend', 'error', f'{descripcion}: después de {intentos} intentos, {detalle}',
                     self._textos()[:12])
        raise FlujoRoto(f'{descripcion}: {detalle}')

    def _backend(self, condicion, segundos):
        limite = time.time() + segundos
        while True:
            try:
                ok, detalle = condicion()
            except BackendError as error:
                ok, detalle = False, str(error)
            if ok or time.time() > limite:
                return ok, detalle
            time.sleep(2)

    def al_final(self, descripcion, fn):
        """Limpieza que corre siempre al terminar, aunque el flujo se rompa (cancelar el pedido creado, etc.)."""
        self.limpiezas.append((descripcion, fn))

    def _roto(self, motivo):
        self._anotar('flujo-roto', 'error', motivo, self._textos()[:12])
        raise FlujoRoto(motivo)

    def limpiar(self):
        for descripcion, fn in reversed(self.limpiezas):
            try:
                fn()
                print(f'  🧹 {descripcion}')
            except Exception as error:  # noqa: BLE001 — una limpieza que falla se reporta y sigue con las demás
                self.hallazgos.append({
                    'n': len(self.hallazgos) + 1, 'flujo': self.nombre, 'rol': self.rol, 'tipo': 'limpieza',
                    'gravedad': 'sospecha', 'detalle': f'No se pudo {descripcion}: {error}', 'pantalla': [],
                    'red': [], 'pasos': list(self.pasos), 'captura': None,
                })
                print(f'  ⚠️  no se pudo {descripcion}: {error}')


def informe(out, red, resultados):
    rama = subprocess.run(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], capture_output=True, text=True).stdout.strip()
    commit = subprocess.run(['git', 'rev-parse', '--short', 'HEAD'], capture_output=True, text=True).stdout.strip()
    hallazgos = [h for r in resultados for h in r['hallazgos']]
    errores = [h for h in hallazgos if h['gravedad'] == 'error']
    lineas = [
        f'# Flujos · {datetime.datetime.now():%Y-%m-%d %H:%M}', '',
        f'- Rama `{rama}` ({commit}) · red: **{red}** · escrituras al sandbox de dev permitidas (cada flujo limpia lo suyo)',
        f'- {sum(r["ok"] for r in resultados)} de {len(resultados)} flujos completos · **{len(errores)} errores** · '
        f'{len(hallazgos) - len(errores)} sospechas', '',
        '| Flujo | Resultado | Pasos | Segundos |', '|---|---|---|---|',
    ]
    for r in resultados:
        lineas.append(f'| {r["nombre"]} | {"✅ completo" if r["ok"] else "❌ " + r["motivo"][:80]} | {r["pasos"]} | {r["segundos"]} |')
    lineas.append('')
    for h in hallazgos:
        lineas += [f'### {h["flujo"]} · {h["tipo"]} ({h["gravedad"]}) · {h["rol"]}', '', h['detalle'], '']
        if h['pantalla']:
            lineas += ['En pantalla: ' + ' · '.join(t[:40] for t in h['pantalla'][:8]), '']
        if h['red']:
            lineas += ['Red reciente:', ''] + [f'    {x}' for x in h['red']] + ['']
        lineas += ['Pasos:', ''] + [f'    {p}' for p in h['pasos'][-12:]] + ['']
        if h['captura']:
            lineas += [f'![{h["tipo"]}]({h["captura"]})', '']
    (out / 'informe.md').write_text('\n'.join(lineas) + '\n')
    (out / 'hallazgos.json').write_text(json.dumps(hallazgos, ensure_ascii=False, indent=2))
    print(f'\n{sum(r["ok"] for r in resultados)}/{len(resultados)} flujos completos · {len(errores)} errores · {out / "informe.md"}')
    return 1 if errores or not all(r['ok'] for r in resultados) else 0


def main():
    p = argparse.ArgumentParser(prog='flujos.py')
    p.add_argument('nombres', nargs='*')
    p.add_argument('--red', default='normal')
    p.add_argument('--lista', action='store_true')
    o = p.parse_args()
    import flujos_vaiinilla  # noqa: F401 — registra los flujos con @flujo
    if o.lista:
        for nombre, (descripcion, _) in FLUJOS.items():
            print(f'{nombre:28} {descripcion}')
        return 0
    elegidos = o.nombres or list(FLUJOS)
    desconocidos = [n for n in elegidos if n not in FLUJOS]
    if desconocidos:
        sys.exit(f'No conozco: {", ".join(desconocidos)}. Usa --lista.')
    out = OUT_ROOT / (datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '-flujos')
    out.mkdir(parents=True, exist_ok=True)
    api = Backend()
    resultados = []
    send('escrituras', ['si'])
    try:
        for nombre in elegidos:
            descripcion, fn = FLUJOS[nombre]
            print(f'\n=== {nombre}: {descripcion} ({o.red}) ===')
            send('red', ['normal'])
            f = Flujo(nombre, out, api, o.red.split())
            inicio = time.time()
            ok, motivo = True, ''
            try:
                fn(f)
            except FlujoRoto as error:
                ok, motivo = False, str(error)
            except Exception as error:  # noqa: BLE001 — un fallo del guion no debe tumbar los demás flujos
                ok, motivo = False, f'{type(error).__name__}: {error}'
                f.hallazgos.append({
                    'n': len(f.hallazgos) + 1, 'flujo': nombre, 'rol': f.rol, 'tipo': 'guion', 'gravedad': 'sospecha',
                    'detalle': traceback.format_exc(limit=3)[-600:], 'pantalla': [], 'red': [],
                    'pasos': list(f.pasos), 'captura': None,
                })
            finally:
                send('red', ['normal'])
                f.limpiar()
            resultados.append({'nombre': nombre, 'ok': ok, 'motivo': motivo, 'pasos': len(f.pasos),
                               'segundos': int(time.time() - inicio), 'hallazgos': f.hallazgos})
            print(f'  {"✅" if ok else "❌"} {nombre}' + ('' if ok else f': {motivo}'))
    finally:
        send('red', ['normal'])
    return informe(out, o.red, resultados)


if __name__ == '__main__':
    # Corre desde el módulo importado: si no, flujos_vaiinilla registraría en otra copia de FLUJOS y FlujoRoto.
    import flujos

    sys.exit(flujos.main())
