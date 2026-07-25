import { DMSans_400Regular, DMSans_500Medium, DMSans_700Bold } from '@expo-google-fonts/dm-sans';
import { Fraunces_700Bold, Fraunces_900Black } from '@expo-google-fonts/fraunces';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { View } from 'react-native';

import { OrderFlowProvider } from '@/hooks/use-order-flow';
import { WalletProvider } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';

SplashScreen.preventAutoHideAsync().catch(() => undefined);

export default function RootLayout() {
  const [loaded] = useFonts({
    DMSans_400Regular,
    DMSans_500Medium,
    DMSans_700Bold,
    Fraunces_700Bold,
    Fraunces_900Black,
  });

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync().catch(() => undefined);
    }
  }, [loaded]);

  if (!loaded) {
    return <View style={{ flex: 1, backgroundColor: colors.paper }} />;
  }

  return (
    <WalletProvider>
      <OrderFlowProvider>
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: colors.paper },
            animation: 'fade',
          }}
        />
      </OrderFlowProvider>
    </WalletProvider>
  );
}
