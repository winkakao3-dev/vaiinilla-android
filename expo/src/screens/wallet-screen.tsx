import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { moneyLabel } from '@/domain/models';
import { useStudentNav } from '@/hooks/use-student-nav';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function WalletScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const nav = useStudentNav('wallet');

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: 140 },
        ]}
      >
        <Text style={styles.eyebrow}>Cartera Vaiinilla</Text>
        <Text style={styles.title}>Tu saldo</Text>

        <View style={styles.balanceCard}>
          <Text style={styles.balanceLabel}>Disponible</Text>
          <Text style={styles.balanceValue}>{moneyLabel(wallet.balance)}</Text>
          <PhysicalPress style={styles.addButton} onPress={() => router.push('/(student)/wallet-add-money')}>
            <Text style={styles.addButtonText}>Agregar dinero</Text>
          </PhysicalPress>
        </View>

        <Text style={styles.section}>Accesos rápidos</Text>
        <PhysicalPress style={styles.linkCard} onPress={() => router.push('/(student)/wallet-methods')}>
          <Text style={styles.linkTitle}>Métodos de pago</Text>
          <Text style={styles.linkBody}>{wallet.cards.length} tarjeta(s) guardadas</Text>
        </PhysicalPress>
        <PhysicalPress style={styles.linkCard} onPress={() => router.push('/(student)/wallet-add-card')}>
          <Text style={styles.linkTitle}>Agregar tarjeta</Text>
          <Text style={styles.linkBody}>Visa o Mastercard demo</Text>
        </PhysicalPress>
        <PhysicalPress style={styles.linkCard} onPress={() => router.push('/(student)/wallet-account')}>
          <Text style={styles.linkTitle}>Cuenta</Text>
          <Text style={styles.linkBody}>Datos de la cartera demo</Text>
        </PhysicalPress>
      </ScrollView>

      <BottomNav {...nav} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md },
  eyebrow: { fontFamily: fonts.bodyBold, fontSize: 11, letterSpacing: 1, color: colors.muted },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  balanceCard: {
    backgroundColor: colors.ink,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: spacing.sm,
  },
  balanceLabel: { fontFamily: fonts.body, fontSize: 13, color: colors.paper, opacity: 0.7 },
  balanceValue: { fontFamily: fonts.displayBlack, fontSize: 40, color: colors.accent },
  addButton: {
    alignSelf: 'flex-start',
    backgroundColor: colors.accent,
    borderRadius: radius.button,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
    marginTop: spacing.sm,
  },
  addButtonText: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.accentInk },
  section: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  linkCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
  },
  linkTitle: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  linkBody: { fontFamily: fonts.body, fontSize: 13, color: colors.muted },
});
