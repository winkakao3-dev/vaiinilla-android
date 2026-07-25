import { initializeApp, getApps, type FirebaseApp } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  type Auth,
} from 'firebase/auth';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

import { FIREBASE_CONFIG, isFirebaseConfigured } from '@/core/config';

let app: FirebaseApp | null = null;
let auth: Auth | null = null;

export function getFirebaseAuth(): Auth | null {
  if (!isFirebaseConfigured()) {
    return null;
  }
  if (auth) {
    return auth;
  }

  app = getApps().length > 0 ? getApps()[0]! : initializeApp(FIREBASE_CONFIG);
  auth = getAuth(app);
  return auth;
}

export async function signInWithSeedEmail(email: string, password: string): Promise<string> {
  const firebaseAuth = getFirebaseAuth();
  if (!firebaseAuth) {
    throw new Error('Firebase no está configurado. Agrega EXPO_PUBLIC_FIREBASE_* al entorno.');
  }
  const credential = await signInWithEmailAndPassword(firebaseAuth, email, password);
  const token = await credential.user.getIdToken();
  if (Platform.OS !== 'web') {
    await AsyncStorage.setItem('vaiinilla.firebase_email', email);
  }
  return token;
}

export async function signOutFirebase(): Promise<void> {
  const firebaseAuth = getFirebaseAuth();
  if (firebaseAuth) {
    await signOut(firebaseAuth);
  }
  await AsyncStorage.removeItem('vaiinilla.firebase_email');
}
