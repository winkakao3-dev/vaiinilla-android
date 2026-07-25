import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function WalletMethodsScreen() {
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
      <Text style={styles.title}>Métodos de pago</Text>

      {wallet.cards.map((card) => (
        <View key={card.id} style={styles.card}>
          <Text style={styles.cardBrand}>{card.brand.toUpperCase()}</Text>
          <Text style={styles.cardLabel}>{card.label}</Text>
        </View>
      ))}

      <PhysicalPress style={styles.add} onPress={() => router.push('/(student)/wallet-add-card')}>
        <Text style={styles.addText}>Agregar tarjeta</Text>
      </PhysicalPress>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  card: {
    backgroundColor: colors.ink,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: 4,
  },
  cardBrand: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.accent, letterSpacing: 1 },
  cardLabel: { fontFamily: fonts.display, fontSize: 22, color: colors.paper },
  add: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
  },
  addText: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
});
