import { router, type Href } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { DATA_SOURCE } from '@/core/config';
import { authenticateSeedUser } from '@/data/auth/seed-auth-repository';
import { SEED_ACCOUNTS, SEED_PASSWORD } from '@/data/auth/seed-accounts';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { RoleSelectorScreen } from '@/screens/role-selector-screen';
import { colors } from '@/theme/colors';
import { fonts } from '@/theme/typography';

type SeedRole = 'cliente' | 'cajero' | 'cocina' | 'mesero';

const ROLE_ROUTES: Record<SeedRole, Href> = {
  cliente: '/(student)/menu',
  cajero: '/(ops)/caja',
  cocina: '/(ops)/cocina',
  mesero: '/(ops)/mesero',
};

export default function RolesRoute() {
  const flow = useOrderFlow();
  const [entering, setEntering] = useState(false);
  const [enterError, setEnterError] = useState<string | null>(null);

  const enterRole = async (role: SeedRole) => {
    setEnterError(null);
    setEntering(true);
    try {
      // Solo pruebas / MOCK: fixtures, no Firebase.
      if (!flow.testOnlyMode && DATA_SOURCE === 'REMOTE') {
        const account = SEED_ACCOUNTS.find((item) => item.role === role);
        if (!account) {
          throw new Error(`No hay cuenta seed para ${role}.`);
        }
        await authenticateSeedUser(account.email, SEED_PASSWORD);
        await flow.loadCatalog();
        await flow.refreshClientOrders();
      } else {
        // Ensure fixtures are loaded when entering from roles.
        await flow.loadCatalog();
      }
      router.push(ROLE_ROUTES[role]);
    } catch (error) {
      setEnterError(error instanceof Error ? error.message : 'No pudimos entrar al rol.');
    } finally {
      setEntering(false);
    }
  };

  return (
    <View style={styles.root}>
      <RoleSelectorScreen
        testOnlyMode={flow.testOnlyMode}
        onTestOnlyModeChange={flow.setTestOnlyMode}
        onStudentSelected={() => void enterRole('cliente')}
        onCashierSelected={() => void enterRole('cajero')}
        onKitchenSelected={() => void enterRole('cocina')}
        onWaiterSelected={() => void enterRole('mesero')}
        onAdminSelected={() => router.push('/(ops)/admin')}
        entering={entering}
        enterError={enterError}
      />
      {entering ? (
        <View style={styles.overlay}>
          <ActivityIndicator color={colors.paper} size="large" />
          <Text style={styles.overlayText}>
            {DATA_SOURCE === 'REMOTE' && !flow.testOnlyMode
              ? 'Entrando con Firebase…'
              : 'Cargando…'}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(23,24,23,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  overlayText: {
    fontFamily: fonts.bodyBold,
    color: colors.paper,
    fontSize: 15,
  },
});
