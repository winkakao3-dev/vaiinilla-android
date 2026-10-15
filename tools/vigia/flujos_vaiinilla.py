"""Los flujos de Vaiinilla que cruzan roles, escritos contra el sandbox de agentes de dev.

Producto de prueba: Chocolate frío (pasa por cocina; Café americano y Galleta de avena son de caja y nunca
llegan a cocina). Cada flujo deja sus pedidos entregados o cancelados al terminar, aunque se rompa.

El sandbox es compartido: otros agentes pueden cobrar o preparar nuestro pedido antes que nosotros (pasó el
2026-10-15: un pedido de mostrador llegó a entregado en 42 s sin que el flujo lo tocara). Por eso la limpieza
se registra antes de crear nada y un paso que otro ya hizo se anota como interferencia, no como error.
"""
import datetime
import re
import urllib.parse

from backend import BackendError
from flujos import FlujoRoto, flujo

PRODUCTO = 'Chocolate frío'
MOTIVO = 'Prueba automática del vigía'
ORDEN = ['pendiente_pago', 'por_cobrar', 'cobrado', 'preparando', 'listo', 'entregado']


def _ahora():
    return datetime.datetime.now(datetime.timezone.utc)


def _pedido(f, pedido_id):
    return f.api.get('admin', f'pedidos/{pedido_id}')


def _creados(f, desde, rol='admin'):
    """Pedidos de Chocolate frío creados desde [desde] que [rol] puede ver (admin ve todos; cliente, los suyos)."""
    marca = desde.strftime('%Y-%m-%dT%H:%M:%S.000Z')
    pedidos = f.api.get(rol, f'pedidos?actualizado_desde={urllib.parse.quote(marca)}&limit=50')
    return [p for p in pedidos if p['creado_en'] >= marca and any(i['nombre_producto'] == PRODUCTO for i in p['items'])]


def _estado(f, pedido_id, *esperados):
    def condicion():
        p = _pedido(f, pedido_id)
        return p['estado'] in esperados, f'estado {p["estado"]} (versión {p["version"]})'
    return condicion


def _ya_paso(f, pedido_id, estado):
    """True si el pedido ya está en [estado] o más adelante: alguien más del sandbox hizo el paso."""
    actual = _pedido(f, pedido_id)['estado']
    if actual == 'cancelado':
        raise FlujoRoto(f'otro actor del sandbox canceló el pedido antes de "{estado}"')
    return ORDEN.index(actual) >= ORDEN.index(estado)


def _precondiciones(f):
    op = f.api.get('cajero', 'estado-operativo')
    faltan = [nombre for nombre, ok in [
        ('la caja del sandbox abierta', op['sesion_caja_abierta']),
        ('recibiendo pedidos', op['recibiendo_pedidos'] and op['dentro_de_franja']),
        ('pago en caja permitido', op['permite_pago_al_final']),
    ] if not ok]
    if faltan:
        f._anotar('precondicion', 'sospecha', 'El sandbox no está listo: falta ' + ', '.join(faltan) + '.')
        raise FlujoRoto('precondición: ' + ', '.join(faltan))


def _limpieza(f, desde):
    """Cierra al final lo que el flujo creó, aunque se rompa antes de saber el folio."""
    f.al_final('cerrar los pedidos del flujo', lambda: [_cerrar_pedido(f, p['id']) for p in _creados(f, desde)])


def _cerrar_pedido(f, pedido_id):
    """Lleva el pedido a un estado final sin dejar dinero colgado: cancela lo no cobrado y entrega lo cobrado."""
    for _ in range(8):
        p = _pedido(f, pedido_id)
        estado, version = p['estado'], p['version']
        if estado in ('entregado', 'cancelado'):
            return
        try:
            if estado in ('por_cobrar', 'pendiente_pago'):
                f.api.post('cajero', f'pedidos/{pedido_id}/cancelaciones',
                           {'version_esperada': version, 'motivo': MOTIVO})
            elif estado in ('cobrado', 'preparando'):
                siguiente = 'preparando' if estado == 'cobrado' else 'listo'
                f.api.post('cocina', f'pedidos/{pedido_id}/transiciones',
                           {'estado_objetivo': siguiente, 'version_esperada': version})
            else:
                cuerpo = {'estado_objetivo': 'entregado', 'version_esperada': version}
                token = _qr(f, pedido_id)
                if token:
                    cuerpo['qr_token'] = token
                quien = 'cajero' if p['destino'] == 'para_llevar' else 'mesero'
                f.api.post(quien, f'pedidos/{pedido_id}/transiciones', cuerpo)
        except BackendError as error:
            if error.status != 409:  # 409: otro actor lo movió mientras tanto; se vuelve a leer
                raise
    raise RuntimeError(f'el pedido sigue en {_pedido(f, pedido_id)["estado"]}')


def _qr(f, pedido_id):
    try:
        datos = f.api.get('cliente', f'pedidos/{pedido_id}/qr') or {}
    except BackendError:
        return None
    return next((v for k, v in datos.items() if 'token' in k and isinstance(v, str)), None)


def _paso_de_personal(f, pedido_id, estado, boton, exito, descripcion):
    if _ya_paso(f, pedido_id, estado):
        f._anotar('interferencia', 'sospecha', f'{descripcion}: otro actor del sandbox ya lo dejó en "{estado}" o más.')
        return False
    f.accion(boton, exito, descripcion, _estado(f, pedido_id, *ORDEN[ORDEN.index(estado):]))
    return True


def _seleccionar(f, folio):
    """Caja y cocina actúan sobre el pedido al frente; si el nuestro está en la fila, se toca para traerlo."""
    f.tocar(rf'#{folio}\b', segundos=15, opcional=True)
    f.ver(rf'^#{folio}$')


def _pedir_en_caja(f):
    """El alumno pide Chocolate frío para pagar en caja. Devuelve (id, folio)."""
    desde = _ahora()
    _limpieza(f, desde)
    f.entrar('alumno')
    f.deeplink('https://vaiinilla.app/e/sandbox-agentes')
    f.tocar(r'Buscar productos', campo=True)
    f.escribir(PRODUCTO)
    f.tocar(rf'{PRODUCTO}.*\$\d')
    f.tocar(r'^Agregar · \$')
    f.tocar(r'Carrito')
    f.tocar(r'^Pagar\b|^Continuar para pagar')
    f.ver(r'¿Cómo quieres pagar\?')
    f.tocar(r'^Pago en caja', opcional=True, segundos=3)
    f.tocar(r'^Pedir · pagas \$\S+ en caja$')
    folio = int(f.ver(r'Pedido #(\d+)', segundos=30).group(1))
    nuevos = []

    def creado():
        nuevos[:] = _creados(f, desde, 'cliente')
        return len(nuevos) == 1, f'{len(nuevos)} pedidos nuevos del alumno: {[p["folio"] for p in nuevos]}'
    f.verificar('el pedido del alumno existe', creado)
    pedido = nuevos[0]
    f.verificar(f'la pantalla dice #{folio} y el backend también',
                lambda: (pedido['folio'] == folio, f'pantalla #{folio}, backend #{pedido["folio"]}'))
    return pedido['id'], folio


def _cobrar_y_preparar(f, pedido_id, folio):
    f.entrar('caja')
    if not _ya_paso(f, pedido_id, 'cobrado'):
        _seleccionar(f, folio)
        f.sabotear()
        _paso_de_personal(f, pedido_id, 'cobrado', r'^Cobrar \$', r'Cobro confirmado', f'Caja cobra el #{folio}')
        f.red_normal()

    f.entrar('cocina')
    if not _ya_paso(f, pedido_id, 'listo'):
        _seleccionar(f, folio)
        f.sabotear()
        _paso_de_personal(f, pedido_id, 'preparando', r'^Empezar a preparar$', rf'Comanda #{folio} en preparaci',
                          f'Cocina empieza el #{folio}')
        _paso_de_personal(f, pedido_id, 'listo', r'^Marcar lista$', rf'Comanda #{folio} lista',
                          f'Cocina marca listo el #{folio}')
        f.red_normal()


@flujo('pedido-caja-cocina', 'Alumno pide para pagar en caja → caja cobra → cocina prepara → el alumno lo ve listo')
def pedido_caja_cocina(f):
    _precondiciones(f)
    pedido_id, folio = _pedir_en_caja(f)
    _cobrar_y_preparar(f, pedido_id, folio)
    estado = _pedido(f, pedido_id)['estado']
    if estado != 'listo':
        f._anotar('interferencia', 'sospecha', f'El #{folio} ya está "{estado}": no se puede revisar que el alumno lo vea listo.')
        return
    f.entrar('alumno')
    f.tocar(r'^Pedidos$')
    f.ver(rf'#{folio}\b')
    f.ver(r'^listo$|¡listo!')


@flujo('pedido-mostrador', 'Caja crea un pedido de mostrador → lo cobra → cocina lo prepara')
def pedido_mostrador(f):
    _precondiciones(f)
    desde = _ahora()
    _limpieza(f, desde)
    f.entrar('caja')
    f.tocar(r'^Nuevo pedido$')
    f.tocar(r'Buscar en el menú', campo=True)
    f.escribir(PRODUCTO)
    f.tocar(rf'{PRODUCTO}')
    f.tocar(r'Crear pedido')
    nuevos = []

    def creado():
        nuevos[:] = _creados(f, desde)
        return len(nuevos) == 1, f'{len(nuevos)} pedidos nuevos: {[p["folio"] for p in nuevos]}'
    f.verificar('el pedido de mostrador existe', creado)
    pedido = nuevos[0]
    f.ver(rf'#{pedido["folio"]}\b')
    _cobrar_y_preparar(f, pedido['id'], pedido['folio'])


# --- Acciones con las escrituras bloqueadas: no cambian el sandbox ni dependen de quién más lo opere ---------------


@flujo('cocina-sin-escrituras', 'Cocina avanza una comanda de la fila sin poder escribir: avisa el error, no dice "lista"')
def cocina_sin_escrituras(f):
    f.entrar('cocina')
    # La comanda al frente puede estar lista (sin botón para avanzar): se elige una de la fila que espere cocina.
    fila = f.tocar(r'^#\d+\b.*(en espera|preparando)', segundos=30)
    folio = re.match(r'#(\d+)', fila['texto']).group(1)
    f.ver(rf'^#{folio}$')
    f.sin_escrituras()
    f.accion_que_falla(r'^(Empezar a preparar|Marcar lista)$', rf'Comanda #{folio} (en preparaci|lista)',
                       f'Cocina avanza el #{folio}', 'transiciones')


@flujo('alumno-pedir-sin-escrituras', 'El alumno toca Pedir sin poder escribir: avisa el error, no dice "Pedido recibido"')
def alumno_pedir_sin_escrituras(f):
    f.entrar('alumno')
    f.deeplink('https://vaiinilla.app/e/sandbox-agentes')
    f.tocar(r'Buscar productos', campo=True)
    f.escribir(PRODUCTO)
    f.tocar(rf'{PRODUCTO}.*\$\d')
    f.tocar(r'^Agregar · \$')
    f.tocar(r'Carrito')
    f.tocar(r'^Pagar\b|^Continuar para pagar')
    f.ver(r'¿Cómo quieres pagar\?')
    f.tocar(r'^Pago en caja', opcional=True, segundos=3)
    f.sin_escrituras()
    f.accion_que_falla(r'^Pedir · pagas \$\S+ en caja$', r'Pedido recibido|Pedido #\d+', 'El alumno pide', 'pedidos')


@flujo('caja-cobrar-sin-escrituras', 'Caja crea un pedido de mostrador y lo cobra sin poder escribir: no dice "Cobro confirmado"')
def caja_cobrar_sin_escrituras(f):
    _precondiciones(f)
    _limpieza(f, _ahora())
    f.entrar('caja')
    f.tocar(r'^Nuevo pedido$')
    f.tocar(r'Buscar en el menú', campo=True)
    f.escribir(PRODUCTO)
    f.tocar(rf'{PRODUCTO}')
    f.tocar(r'Crear pedido')
    f.sin_escrituras()
    f.accion_que_falla(r'^Cobrar \$', r'Cobro confirmado', 'Caja cobra el pedido de mostrador', 'cobros-efectivo')
