#!/usr/bin/env python3
"""Vigía: recorre la app de Android en la JVM (vbuild jvm-roles) tocando todo lo tocable y anota lo que se ve mal.

  vigia.py explorar [--roles alumno,caja,cocina,mesero] [--red normal|"lento 5000"|"fallas 40"|caido]
                    [--pasos 40] [--profundidad 4] [--limite 12000] [--semilla 7]
      Por cada rol: entra, toca cada cosa (salvo lo peligroso), espera mientras haya algo cargando y
      junta los hallazgos del arnés: carga-eterna, carga-larga, error-mudo, cierre. Además marca
      toque-sin-efecto y boton-sin-nombre. Deja informe.md, hallazgos.json y capturas en
      ~/.cache/vaiinilla/vigia/<fecha>/.

  vigia.py <comando> [args]   pasa el comando tal cual al arnés (red, vigia, nodos, tocar, atras, limite…)

Nunca toca lo que cierra sesión, cobra, paga, borra, rechaza, cambia disponibilidad o cierra caja: eso va
en flujos escritos a mano. Y por si algo se cuela, corre en solo lectura: las escrituras que pasan por el
cliente de la API (salvo sesión y latidos) vuelven como error sin llegar al sandbox de dev, que comparten
otros agentes. Unas pocas clases (sesión, presencia, correo de acceso) usan el cliente HTTP directo y no
pasan por ahí; por eso al final se revisa el log del arnés y se cuenta toda escritura que sí salió.
--escrituras quita el solo lectura.

Antes: tools/vigia/compilar.sh (heaps pequeños) y vbuild jvm-roles reiniciar.
"""
import argparse
import datetime
import json
import random
import re
import socket
import subprocess
import sys
from pathlib import Path

STATE = Path.home() / '.cache' / 'vaiinilla' / 'jvm-roles' / 'estado.json'
HARNESS_LOG = Path.home() / '.cache' / 'vaiinilla' / 'jvm-roles' / 'log.txt'
ESCRITURA = re.compile(r'VaiinillaHttp: (POST|PUT|PATCH|DELETE) (\S+) -> (\d+)')
DE_FONDO = ('sesiones/', 'latidos', 'dispositivos')
OUT_ROOT = Path.home() / '.cache' / 'vaiinilla' / 'vigia'
TIMEOUT_SECONDS = 180
ROLES = ['alumno', 'caja', 'cocina', 'mesero']

PELIGROSO = re.compile(
    r'cerrar sesi|salir|cerrar caja|cerrar turno|corte|eliminar|borrar|quitar|cancelar|pagar|cobrar|'
    r'confirmar|enviar|hacer pedido|realizar pedido|pedir|ordenar|recargar|devoluci|reembols|desactivar|'
    r'bloquear|suspender|cambiar de modo|cambiar modo|apagar|llamar|whatsapp|compartir|abrir en|'
    r'tarjeta|stripe|nfc|c[aá]mara|escanear|foto|permitir|ubicaci|notificaci|eliminar cuenta|'
    r'privacidad|t[eé]rminos|ayuda externa|desvincular|marcar|entregar|listo para|rechazar|aceptar|'
    r'abrir cuenta|tomar pedido|se acab|disponib|agotad|preparar|iniciar|terminar|cerrar manual|liberar|'
    r'asignar|mover|reservar|rentar|unir|invitar|guardar|crear|agregar al pedido|dividir|propina|descuento|'
    r'abrir caja|^voy$|atendida|\d+ min\b|renovar|abonar|abonado|cobrado en|apartar|^saldo$|efectivo en caja|'
    r'ya llegu|necesito algo|cubiertos|algo est[aá] mal|lo pago yo|pausad|reintentar pago|continuar pago|'
    r'revisar pago|entrar como|continuar como|reenviar|verificar c[oó]digo|continuar con google|desliza',
    re.IGNORECASE,
)
# Abren una hoja o pantalla sin escribir nada; lo que escribe adentro sigue bloqueado por PELIGROSO.
SOLO_NAVEGA = re.compile(
    r'^(rentar|recargar en caja|recargar saldo|cerrar caja|pagar|continuar para pagar|cobrar cuenta|'
    r'ver cuentas por cobrar|tomar pedido)$',
    re.IGNORECASE,
)
VOLVER = re.compile(r'^(atr[aá]s|regresar|volver|cerrar|navigate up)$', re.IGNORECASE)


def peligroso(texto):
    texto = texto.strip()
    return not SOLO_NAVEGA.match(texto) and bool(PELIGROSO.search(texto))


def send(cmd, args=()):
    if not STATE.exists():
        sys.exit('jvm-roles no está corriendo. Arráncalo con: vbuild jvm-roles')
    state = json.loads(STATE.read_text())
    with socket.create_connection(('127.0.0.1', state['port']), timeout=TIMEOUT_SECONDS) as conn:
        payload = {'token': state['token'], 'cmd': cmd, 'args': [str(a) for a in args]}
        conn.sendall((json.dumps(payload) + '\n').encode())
        reply = json.loads(conn.makefile().readline())
    if not reply['ok']:
        raise RuntimeError(reply['out'])
    return reply['out']


def send_json(cmd, args=()):
    return json.loads(send(cmd, args))


def clave(texto):
    return re.sub(r'[0-9]+', '#', texto).strip().lower()


class Explorador:
    def __init__(self, opciones):
        self.o = opciones
        self.rng = random.Random(opciones.semilla)
        stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
        self.out = OUT_ROOT / stamp
        self.out.mkdir(parents=True, exist_ok=True)
        self.hallazgos = []
        self.vistos = set()
        self.pantallas = {}
        self.historia = []
        self.rol = None
        self.toques = 0
        self.totales_inicio = None
        self.log_inicio = HARNESS_LOG.stat().st_size if HARNESS_LOG.exists() else 0
        self.escrituras_reales = []
        self.totales = {}

    # --- registro ---------------------------------------------------------------------------------------

    def anotar(self, tipo, gravedad, detalle, pantalla=(), red=(), pantalla_id=''):
        firma = (self.rol, tipo, clave(detalle), pantalla_id)
        if firma in self.vistos:
            return
        self.vistos.add(firma)
        n = len(self.hallazgos) + 1
        captura = f'{n:02d}-{self.rol}-{tipo}.png'
        try:
            send('shot', [str(self.out / captura)])
        except RuntimeError:
            captura = None
        self.hallazgos.append({
            'n': n, 'rol': self.rol, 'tipo': tipo, 'gravedad': gravedad, 'detalle': detalle,
            'pantalla': list(pantalla), 'red': list(red), 'pasos': list(self.historia), 'captura': captura,
        })
        print(f'  ⚠️  [{gravedad}] {tipo}: {detalle}')

    def recoger(self, pantalla_id=''):
        """Junta lo que vio el arnés; si algo sigue cargando, espera a que termine o a que el vigía lo marque."""
        datos = send_json('vigia')
        self.contar(datos)
        esperado = 0
        tope = self.o.limite * 3 + 3000
        while datos.get('cargando') and esperado < tope:
            for h in datos['hallazgos']:
                self.anotar(h['tipo'], h['gravedad'], h['detalle'], h['pantalla'], h['red'], pantalla_id)
            send('wait', [2000])
            esperado += 2000
            datos = send_json('vigia')
            self.contar(datos)
        for h in datos['hallazgos']:
            self.anotar(h['tipo'], h['gravedad'], h['detalle'], h['pantalla'], h['red'], pantalla_id)

    def contar(self, datos):
        totales = datos.get('totales') or {}
        if self.totales_inicio is None:
            self.totales_inicio = dict(totales)
        self.totales = {k: v - self.totales_inicio.get(k, 0) for k, v in totales.items()}

    # --- recorrido --------------------------------------------------------------------------------------

    def entrar(self):
        self.historia = [f'role {self.rol}']
        print(f'→ {send("role", [self.rol])[:160]}')
        self.recoger()

    def explorar_rol(self, rol):
        self.rol = rol
        print(f'\n=== {rol} ({self.o.red}) ===')
        self.entrar()
        probados = {}
        profundidad = 0
        pasos = 0
        reentradas = 0
        while pasos < self.o.pasos:
            nodos = send_json('nodos')
            if not nodos['activa']:
                if reentradas >= 3:
                    break
                reentradas += 1
                self.entrar()
                profundidad = 0
                continue
            pid = nodos['pantalla']
            if (rol, pid) not in self.pantallas:
                # Primera vez en esta pantalla: se revisa su diseño quieta (texto cortado, encimados, fuera).
                for h in send_json('revisar')['hallazgos']:
                    self.anotar(h['tipo'], h['gravedad'], h['detalle'], h['pantalla'], h['red'], pid)
            self.pantallas.setdefault((rol, pid), nodos['textos'][:6])
            ya = probados.setdefault(pid, set())
            for nodo in nodos['nodos']:
                if not nodo['texto'].strip():
                    self.anotar(
                        'boton-sin-nombre', 'sospecha',
                        f'Hay algo tocable sin texto ni descripción en ({nodo["x"]}, {nodo["y"]}); TalkBack no sabe qué es.',
                        nodos['textos'], (), pid,
                    )
            candidatos = [
                n for n in nodos['nodos']
                if n['texto'].strip() and not n.get('campo') and not peligroso(n['texto'])
                and clave(n['texto']) not in ya
            ]
            # Primero lo que lleva a otro lado; "atrás" y "cerrar" al final, para no salir antes de ver la pantalla.
            self.rng.shuffle(candidatos)
            candidatos.sort(key=lambda n: bool(VOLVER.match(n['texto'].strip())))
            if not candidatos or profundidad >= self.o.profundidad:
                if profundidad == 0 and not candidatos:
                    break
                r = send_json('atras')
                self.historia.append('atras')
                self.recoger(pid)
                if r['efecto'] == 'atras' and r.get('pantalla') == pid and profundidad > 0:
                    self.anotar(
                        'atras-no-regresa', 'sospecha',
                        'El botón de atrás del teléfono no sacó de esta pantalla.',
                        nodos['textos'], (), pid,
                    )
                if r['efecto'] == 'salio':
                    if reentradas >= 3:
                        break
                    reentradas += 1
                    self.entrar()
                    profundidad = 0
                else:
                    profundidad = max(0, profundidad - 1)
                continue
            nodo = candidatos[0]
            ya.add(clave(nodo['texto']))
            try:
                r = send_json('tocar', [nodo['id']])
            except RuntimeError as error:
                # Casi siempre la pantalla se movió sola (polling) y el nodo ya no está; si fue un cierre, el vigía lo trae.
                self.historia.append(f'tocar "{nodo["texto"]}" (falló: {str(error)[:80]})')
                send('wait', [500])
                self.recoger(pid)
                continue
            pasos += 1
            self.toques += 1
            self.historia.append(f'tocar "{nodo["texto"]}"')
            print(f'  {pasos:>3}. {nodo["texto"][:50]!r} → {r["efecto"]}')
            self.recoger(pid)
            if r['efecto'] == 'nada' and not nodo['alterna']:
                self.anotar(
                    'toque-sin-efecto', 'sospecha',
                    f'Tocar "{nodo["texto"]}" no cambió nada en pantalla ni hizo peticiones.',
                    nodos['textos'], (), pid,
                )
            if r['efecto'] == 'red' and self.o.red.startswith('lento'):
                # Con la red lenta, lo que el usuario ve en ese segundo y medio es todo lo que sabe de su toque.
                self.anotar(
                    'sin-respuesta-visible', 'sospecha',
                    f'Tocar "{nodo["texto"]}" mandó {r["peticiones"]} petición(es) y la pantalla no mostró nada '
                    'mientras esperaba (ni carga, ni botón deshabilitado).',
                    nodos['textos'], (), pid,
                )
            if r['efecto'] == 'pantalla-nueva':
                profundidad += 1
            elif r['efecto'] == 'salio':
                if reentradas >= 3:
                    break
                reentradas += 1
                self.entrar()
                profundidad = 0

    def revisar_escrituras(self):
        """Toda escritura que el cliente HTTP real mandó durante la corrida, salvo sesión y latidos."""
        if not HARNESS_LOG.exists():
            return
        with HARNESS_LOG.open('rb') as log:
            log.seek(self.log_inicio)
            texto = log.read().decode('utf-8', 'replace')
        self.escrituras_reales = [
            f'{m.group(1)} {m.group(2)} -> {m.group(3)}'
            for m in ESCRITURA.finditer(texto) if not m.group(2).startswith(DE_FONDO)
        ]

    # --- informe ----------------------------------------------------------------------------------------

    def informe(self):
        rama = subprocess.run(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], capture_output=True, text=True).stdout.strip()
        commit = subprocess.run(['git', 'rev-parse', '--short', 'HEAD'], capture_output=True, text=True).stdout.strip()
        self.revisar_escrituras()
        errores = [h for h in self.hallazgos if h['gravedad'] == 'error']
        sospechas = [h for h in self.hallazgos if h['gravedad'] != 'error']
        lineas = [
            f'# Vigía · {datetime.datetime.now():%Y-%m-%d %H:%M}',
            '',
            f'- Rama `{rama}` ({commit}) · red: **{self.o.red}** · roles: {", ".join(self.o.roles)} · '
            + ('escrituras al sandbox **permitidas**' if self.o.escrituras else 'solo lectura (las escrituras vuelven como error)'),
            f'- {self.toques} toques · {len(self.pantallas)} pantallas distintas · '
            f'**{len(errores)} errores** · {len(sospechas)} sospechas',
            f'- Red: {self.totales.get("peticiones", 0)} peticiones · {self.totales.get("saboteadas", 0)} saboteadas · '
            f'{self.totales.get("bloqueadas", 0)} escrituras frenadas (solo lectura) · '
            f'**{len(self.escrituras_reales)} escrituras que sí llegaron al sandbox**',
            '',
        ]
        for titulo, grupo in (('Errores', errores), ('Sospechas', sospechas)):
            if not grupo:
                continue
            lineas += [f'## {titulo}', '']
            for h in grupo:
                lineas += [f'### {h["n"]}. {h["tipo"]} · {h["rol"]}', '', h['detalle'], '']
                if h['pantalla']:
                    lineas += ['En pantalla: ' + ' · '.join(t[:40] for t in h['pantalla'][:8]), '']
                if h['red']:
                    lineas += ['Red reciente:', ''] + [f'    {r}' for r in h['red']] + ['']
                lineas += ['Para repetirlo:', ''] + [f'    {p}' for p in h['pasos'][-12:]] + ['']
                if h['captura']:
                    lineas += [f'![{h["tipo"]}]({h["captura"]})', '']
        if self.escrituras_reales:
            lineas += ['## Escrituras que llegaron al sandbox de dev', ''] + [f'    {e}' for e in self.escrituras_reales] + ['']
        lineas += ['## Pantallas recorridas', '']
        for (rol, pid), textos in sorted(self.pantallas.items()):
            lineas.append(f'- {rol} `{pid}`: ' + ' · '.join(t[:30] for t in textos))
        (self.out / 'informe.md').write_text('\n'.join(lineas) + '\n')
        (self.out / 'hallazgos.json').write_text(json.dumps(self.hallazgos, ensure_ascii=False, indent=2))
        print(f'\n{len(errores)} errores, {len(sospechas)} sospechas · {self.out / "informe.md"}')
        if self.escrituras_reales and not self.o.escrituras:
            print(f'⛔ {len(self.escrituras_reales)} escrituras llegaron al sandbox en solo lectura:')
            for e in self.escrituras_reales:
                print(f'   {e}')
            return 2
        return 1 if errores else 0


def explorar(argv):
    p = argparse.ArgumentParser(prog='vigia.py explorar')
    p.add_argument('--roles', default=','.join(ROLES))
    p.add_argument('--red', default='normal')
    p.add_argument('--pasos', type=int, default=40)
    p.add_argument('--profundidad', type=int, default=4)
    p.add_argument('--limite', type=int, default=12000)
    p.add_argument('--semilla', type=int, default=7)
    p.add_argument('--escrituras', action='store_true',
                   help='deja que las escrituras lleguen al sandbox de dev (por defecto vuelven como error)')
    o = p.parse_args(argv)
    o.roles = [r.strip() for r in o.roles.split(',') if r.strip()]
    send('red', o.red.split())
    send('escrituras', ['si' if o.escrituras else 'no'])
    send('limite', [o.limite])
    e = Explorador(o)
    try:
        for rol in o.roles:
            try:
                e.explorar_rol(rol)
            except RuntimeError as error:
                e.anotar('arnes', 'sospecha', f'El recorrido se detuvo: {str(error)[:300]}')
    finally:
        send('red', ['normal'])
        send('escrituras', ['si'])
    return e.informe()


def main():
    if len(sys.argv) < 2 or sys.argv[1] in ('-h', '--help', 'help'):
        sys.exit(__doc__)
    if sys.argv[1] == 'explorar':
        sys.exit(explorar(sys.argv[2:]))
    try:
        print(send(sys.argv[1], sys.argv[2:]))
    except RuntimeError as error:
        sys.exit(str(error))


if __name__ == '__main__':
    main()
