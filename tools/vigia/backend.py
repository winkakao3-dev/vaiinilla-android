"""El backend de dev visto como cada rol del sandbox, para comparar la pantalla con la verdad.

Pide los tokens a /dev/test-setup con el secreto de ~/.vaiinilla-test-accounts.json (lo lee en cada uso, no
lo copia) y los renueva antes de que venzan (duran 15 min). Nunca imprime tokens ni el secreto.
Solo apunta a development: el endpoint de test-setup no existe en producción.
"""
import json
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

API = 'https://vaiinillaback-development.up.railway.app/api/v1'
SECRET_FILE = Path.home() / '.vaiinilla-test-accounts.json'
# Nombres del arnés → nombres de test-setup.
ROLES = {'alumno': 'cliente', 'cliente': 'cliente', 'caja': 'cajero', 'cajero': 'cajero', 'cocina': 'cocina',
         'mesero': 'mesero', 'admin': 'admin'}
TOKEN_TTL_SECONDS = 10 * 60


class BackendError(RuntimeError):
    def __init__(self, status, body):
        super().__init__(f'HTTP {status}: {body[:300]}')
        self.status = status
        self.body = body


class Backend:
    def __init__(self):
        self._roles = None
        self._fetched_at = 0.0

    def _setup(self):
        if self._roles is None or time.time() - self._fetched_at > TOKEN_TTL_SECONDS:
            secret = json.loads(SECRET_FILE.read_text())['test_setup_secret']
            req = urllib.request.Request(f'{API}/dev/test-setup', method='POST',
                                         headers={'X-Test-Setup-Secret': secret})
            with urllib.request.urlopen(req, timeout=30) as res:
                self._roles = json.load(res)['data']['roles']
            self._fetched_at = time.time()
        return self._roles

    def contexto(self, rol):
        """usuario_id, membresia_id, establecimiento_id y rol de la persona del sandbox."""
        return self._setup()[ROLES[rol]]['contexto']

    def llamar(self, rol, metodo, ruta, cuerpo=None, idempotente=False):
        token = self._setup()[ROLES[rol]]['access_token']
        headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/json'}
        datos = None
        if cuerpo is not None:
            headers['Content-Type'] = 'application/json'
            datos = json.dumps(cuerpo).encode()
        if idempotente:
            headers['Idempotency-Key'] = str(uuid.uuid4())
        req = urllib.request.Request(f'{API}/{ruta.lstrip("/")}', data=datos, method=metodo, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30) as res:
                texto = res.read().decode()
        except urllib.error.HTTPError as error:
            raise BackendError(error.code, error.read().decode('utf-8', 'replace')) from None
        if not texto:
            return None
        cuerpo = json.loads(texto)
        return cuerpo.get('data', cuerpo) if isinstance(cuerpo, dict) else cuerpo

    def get(self, rol, ruta):
        return self.llamar(rol, 'GET', ruta)

    def post(self, rol, ruta, cuerpo=None):
        return self.llamar(rol, 'POST', ruta, cuerpo, idempotente=True)
