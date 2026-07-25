import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { moneyLabel } from '@/domain/models';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function WalletAccountScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <PhysicalPress onPress={() => router.back()}>
        <Text style={styles.back}>← Cartera</Text>
      </PhysicalPress>
      <Text style={styles.title}>Cuenta demo</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Titular</Text>
        <Text style={styles.value}>Dani Alumno</Text>
        <Text style={styles.label}>Saldo</Text>
        <Text style={styles.value}>{moneyLabel(wallet.balance)}</Text>
        <Text style={styles.label}>Modo</Text>
        <Text style={styles.value}>MOCK / Solo pruebas</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  card: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: colors.line,
  },
  label: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.muted, letterSpacing: 0.8 },
  value: { fontFamily: fonts.body, fontSize: 16, color: colors.ink, marginBottom: spacing.sm },
});
