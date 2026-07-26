import { router } from 'expo-router';

import { useOrderFlow } from '@/hooks/use-order-flow';
import { LoginScreen } from '@/screens/login-screen';

export default function LoginRoute() {
  const flow = useOrderFlow();

  return (
    <LoginScreen
      onSuccess={() => {
        void flow.loadCatalog();
        void flow.refreshClientOrders();
        router.replace('/roles');
      }}
    />
  );
}
