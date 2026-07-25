import AsyncStorage from '@react-native-async-storage/async-storage';

import { formatMoney, isValidMoney, parseMoney } from '@/domain/contract-rules';

const BALANCE_KEY = 'vaiinilla.wallet.balance';
const CARDS_KEY = 'vaiinilla.wallet.cards';

export interface WalletCard {
  id: string;
  brand: 'visa' | 'mastercard';
  last4: string;
  label: string;
}

const DEFAULT_BALANCE = '250.00';
const DEFAULT_CARDS: WalletCard[] = [
  { id: 'card-1', brand: 'visa', last4: '4242', label: 'Visa ···· 4242' },
];

export async function getWalletBalance(): Promise<string> {
  const stored = await AsyncStorage.getItem(BALANCE_KEY);
  return stored && isValidMoney(stored) ? stored : DEFAULT_BALANCE;
}

export async function setWalletBalance(balance: string): Promise<void> {
  await AsyncStorage.setItem(BALANCE_KEY, balance);
}

export async function debitWallet(amount: string): Promise<string> {
  const current = parseMoney(await getWalletBalance());
  const debit = parseMoney(amount);
  if (current.lt(debit)) {
    throw new Error('Saldo insuficiente en la cartera.');
  }
  const next = formatMoney(current.minus(debit));
  await setWalletBalance(next);
  return next;
}

export async function creditWallet(amount: string): Promise<string> {
  const next = formatMoney(parseMoney(await getWalletBalance()).plus(parseMoney(amount)));
  await setWalletBalance(next);
  return next;
}

export async function getWalletCards(): Promise<WalletCard[]> {
  const raw = await AsyncStorage.getItem(CARDS_KEY);
  if (!raw) {
    return DEFAULT_CARDS;
  }
  try {
    return JSON.parse(raw) as WalletCard[];
  } catch {
    return DEFAULT_CARDS;
  }
}

export async function addWalletCard(card: Omit<WalletCard, 'id'>): Promise<WalletCard[]> {
  const cards = await getWalletCards();
  const next = [...cards, { ...card, id: `card-${Date.now()}` }];
  await AsyncStorage.setItem(CARDS_KEY, JSON.stringify(next));
  return next;
}

export async function resetWallet(): Promise<void> {
  await AsyncStorage.multiSet([
    [BALANCE_KEY, DEFAULT_BALANCE],
    [CARDS_KEY, JSON.stringify(DEFAULT_CARDS)],
  ]);
}
