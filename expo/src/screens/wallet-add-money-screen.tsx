import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

const AMOUNTS = ['50.00', '100.00', '200.00'];

export function WalletAddMoneyScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const [message, setMessage] = useState<string | null>(null);

  const add = async (amount: string) => {
    await wallet.addMoney(amount);
    setMessage(`Agregaste $${amount} con tarjeta demo.`);
  };

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <PhysicalPress onPress={() => router.back()}>
        <Text style={styles.back}>← Cartera</Text>
      </PhysicalPress>
      <Text style={styles.title}>Agregar dinero</Text>
      <Text style={styles.subtitle}>Simula recarga con tarjeta o SPEI en MOCK.</Text>

      {AMOUNTS.map((amount) => (
        <PhysicalPress key={amount} style={styles.option} onPress={() => void add(amount)}>
          <Text style={styles.optionTitle}>${amount}</Text>
          <Text style={styles.optionBody}>Tarjeta · SPEI demo</Text>
        </PhysicalPress>
      ))}

      {message ? <Text style={styles.message}>{message}</Text> : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 14, color: colors.muted },
  option: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
  },
  optionTitle: { fontFamily: fonts.bodyBold, fontSize: 18, color: colors.ink },
  optionBody: { fontFamily: fonts.body, fontSize: 13, color: colors.muted },
  message: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.accentInk },
});
