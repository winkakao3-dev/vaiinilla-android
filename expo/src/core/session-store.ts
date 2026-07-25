import AsyncStorage from '@react-native-async-storage/async-storage';

const ACCESS_TOKEN_KEY = 'vaiinilla.access_token';
const CONTEXT_KEY = 'vaiinilla.session_context';

export interface SessionContext {
  usuarioId: string;
  membresiaId: string;
  establecimientoId: string;
  rol: string;
}

export async function getAccessToken(): Promise<string | null> {
  return AsyncStorage.getItem(ACCESS_TOKEN_KEY);
}

export async function setAccessToken(token: string | null): Promise<void> {
  if (token) {
    await AsyncStorage.setItem(ACCESS_TOKEN_KEY, token);
  } else {
    await AsyncStorage.removeItem(ACCESS_TOKEN_KEY);
  }
}

export async function getSessionContext(): Promise<SessionContext | null> {
  const raw = await AsyncStorage.getItem(CONTEXT_KEY);
  return raw ? (JSON.parse(raw) as SessionContext) : null;
}

export async function setSessionContext(context: SessionContext | null): Promise<void> {
  if (context) {
    await AsyncStorage.setItem(CONTEXT_KEY, JSON.stringify(context));
  } else {
    await AsyncStorage.removeItem(CONTEXT_KEY);
  }
}

export async function clearSession(): Promise<void> {
  await Promise.all([setAccessToken(null), setSessionContext(null)]);
}
