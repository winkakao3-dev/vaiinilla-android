import AsyncStorage from '@react-native-async-storage/async-storage';
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import {
  addWalletCard,
  creditWallet,
  debitWallet,
  getWalletBalance,
  getWalletCards,
  type WalletCard,
} from '@/data/wallet-store';

interface WalletState {
  balance: string;
  cards: WalletCard[];
  loading: boolean;
}

interface WalletContextValue extends WalletState {
  refreshWallet: () => Promise<void>;
  addMoney: (amount: string) => Promise<void>;
  debitForOrder: (amount: string) => Promise<void>;
  registerCard: (brand: WalletCard['brand'], last4: string) => Promise<void>;
}

const WalletContext = createContext<WalletContextValue | null>(null);

export function WalletProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<WalletState>({
    balance: '0.00',
    cards: [],
    loading: true,
  });

  const refreshWallet = useCallback(async () => {
    const [balance, cards] = await Promise.all([getWalletBalance(), getWalletCards()]);
    setState({ balance, cards, loading: false });
  }, []);

  useEffect(() => {
    void refreshWallet();
  }, [refreshWallet]);

  const addMoney = useCallback(async (amount: string) => {
    const balance = await creditWallet(amount);
    setState((current) => ({ ...current, balance }));
  }, []);

  const debitForOrder = useCallback(async (amount: string) => {
    const balance = await debitWallet(amount);
    setState((current) => ({ ...current, balance }));
  }, []);

  const registerCard = useCallback(async (brand: WalletCard['brand'], last4: string) => {
    const cards = await addWalletCard({
      brand,
      last4,
      label: `${brand === 'visa' ? 'Visa' : 'Mastercard'} ···· ${last4}`,
    });
    setState((current) => ({ ...current, cards }));
  }, []);

  const value = useMemo(
    () => ({
      ...state,
      refreshWallet,
      addMoney,
      debitForOrder,
      registerCard,
    }),
    [state, refreshWallet, addMoney, debitForOrder, registerCard],
  );

  return <WalletContext.Provider value={value}>{children}</WalletContext.Provider>;
}

export function useWallet(): WalletContextValue {
  const context = useContext(WalletContext);
  if (!context) {
    throw new Error('useWallet must be used within WalletProvider');
  }
  return context;
}

export async function getTestOnlyMode(): Promise<boolean> {
  const value = await AsyncStorage.getItem('vaiinilla.test_only_mode');
  return value !== 'false';
}
