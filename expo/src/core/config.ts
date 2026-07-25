export type DataSource = 'MOCK' | 'REMOTE';

/**
 * Client defaults match Android `google-services.json` from the Jesús REMOTE build
 * (project `vaiinilla-b3a70`). Override via EXPO_PUBLIC_* in `.env` if needed.
 */
const JESUS_FIREBASE = {
  apiKey: 'AIzaSyDyTQEoziF4FQiZ3MuzPW9ywymaLDwU-5A',
  authDomain: 'vaiinilla-b3a70.firebaseapp.com',
  projectId: 'vaiinilla-b3a70',
  storageBucket: 'vaiinilla-b3a70.firebasestorage.app',
  messagingSenderId: '697485438624',
  appId: '1:697485438624:android:4ea7499636c7157b97a3c5',
} as const;

const rawSource = process.env.EXPO_PUBLIC_VAIINILLA_DATA_SOURCE?.toUpperCase();

export const DATA_SOURCE: DataSource = rawSource === 'REMOTE' ? 'REMOTE' : 'MOCK';

export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_BASE_URL ??
  'https://vaiinillaback-development-3f6c.up.railway.app/api/v1/';

export const FIREBASE_CONFIG = {
  apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY || JESUS_FIREBASE.apiKey,
  authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN || JESUS_FIREBASE.authDomain,
  projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID || JESUS_FIREBASE.projectId,
  storageBucket:
    process.env.EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET || JESUS_FIREBASE.storageBucket,
  messagingSenderId:
    process.env.EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || JESUS_FIREBASE.messagingSenderId,
  appId: process.env.EXPO_PUBLIC_FIREBASE_APP_ID || JESUS_FIREBASE.appId,
};

export function isFirebaseConfigured(): boolean {
  return Boolean(
    FIREBASE_CONFIG.apiKey &&
      FIREBASE_CONFIG.authDomain &&
      FIREBASE_CONFIG.projectId &&
      FIREBASE_CONFIG.appId,
  );
}
