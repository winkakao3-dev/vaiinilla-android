import { router } from 'expo-router';

import type { StudentTab } from '@/components/bottom-nav';
import { useOrderFlow } from '@/hooks/use-order-flow';

export function useStudentNav(activeTab: StudentTab) {
  const flow = useOrderFlow();

  return {
    activeTab,
    cartCount: flow.cartCount,
    onMenu: () => router.push('/(student)/menu'),
    onAssistant: () => router.push('/(student)/assistant'),
    onOrders: () => router.push('/(student)/orders'),
    onWallet: () => router.push('/(student)/wallet'),
    onCart: () => router.push('/(student)/cart'),
  };
}
