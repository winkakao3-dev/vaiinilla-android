import { apiRequest } from '@/core/http-client';
import { DATA_SOURCE } from '@/core/config';
import {
  clearSession,
  setAccessToken,
  setSessionContext,
  type SessionContext,
} from '@/core/session-store';
import { findSeedAccount, SEED_PASSWORD } from '@/data/auth/seed-accounts';
import { signInWithSeedEmail, signOutFirebase } from '@/data/auth/firebase';

export interface AuthResult {
  accessToken: string;
  context: SessionContext;
}

export async function authenticateSeedUser(email: string, password: string): Promise<AuthResult> {
  const account = findSeedAccount(email);
  if (!account) {
    throw new Error('Cuenta seed no reconocida.');
  }
  if (password !== SEED_PASSWORD) {
    throw new Error('Contraseña incorrecta.');
  }

  if (DATA_SOURCE === 'MOCK') {
    const mockToken = `mock.${account.email}`;
    const context: SessionContext = {
      usuarioId: '032819a8-8dbd-4aef-a728-2e1be9ef09ab',
      membresiaId: account.membresiaId,
      establecimientoId: '8246ff44-aad0-4e49-9268-b71c997893fe',
      rol: account.role,
    };
    await setAccessToken(mockToken);
    await setSessionContext(context);
    return { accessToken: mockToken, context };
  }

  const firebaseToken = await signInWithSeedEmail(account.email, password);
  const data = await apiRequest<{
    access_token: string;
    contexto: {
      usuario_id: string;
      membresia_id: string;
      establecimiento_id: string;
      rol: string;
    };
  }>('sesiones/contexto', {
    method: 'POST',
    token: firebaseToken,
    body: { membresia_id: account.membresiaId },
  });

  const context: SessionContext = {
    usuarioId: data.contexto.usuario_id,
    membresiaId: data.contexto.membresia_id,
    establecimientoId: data.contexto.establecimiento_id,
    rol: data.contexto.rol,
  };

  await setAccessToken(data.access_token);
  await setSessionContext(context);
  return { accessToken: data.access_token, context };
}

export async function signOutSession(): Promise<void> {
  await signOutFirebase();
  await clearSession();
}
