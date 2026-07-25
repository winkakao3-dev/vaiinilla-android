export type DataSource = 'MOCK' | 'REMOTE';

const rawSource = process.env.EXPO_PUBLIC_VAIINILLA_DATA_SOURCE?.toUpperCase();

export const DATA_SOURCE: DataSource = rawSource === 'REMOTE' ? 'REMOTE' : 'MOCK';

export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_BASE_URL ??
  'https://vaiinillaback-development-3f6c.up.railway.app/api/v1/';

export const FIREBASE_CONFIG = {
  apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY ?? '',
  authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN ?? '',
  projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID ?? '',
  storageBucket: process.env.EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET ?? '',
  messagingSenderId: process.env.EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID ?? '',
  appId: process.env.EXPO_PUBLIC_FIREBASE_APP_ID ?? '',
};

export function isFirebaseConfigured(): boolean {
  return Boolean(
    FIREBASE_CONFIG.apiKey &&
      FIREBASE_CONFIG.authDomain &&
      FIREBASE_CONFIG.projectId &&
      FIREBASE_CONFIG.appId,
  );
}
