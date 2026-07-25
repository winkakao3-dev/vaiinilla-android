import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function WalletAddCardScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const [last4, setLast4] = useState('4242');
  const [brand, setBrand] = useState<'visa' | 'mastercard'>('visa');

  const save = async () => {
    if (last4.length !== 4) {
      return;
    }
    await wallet.registerCard(brand, last4);
    router.back();
  };

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <PhysicalPress onPress={() => router.back()}>
        <Text style={styles.back}>← Métodos</Text>
      </PhysicalPress>
      <Text style={styles.title}>Agregar tarjeta</Text>

      <View style={styles.row}>
        <PhysicalPress
          style={[styles.brandChip, brand === 'visa' && styles.brandChipActive]}
          onPress={() => setBrand('visa')}
        >
          <Text style={styles.brandText}>VISA</Text>
        </PhysicalPress>
        <PhysicalPress
          style={[styles.brandChip, brand === 'mastercard' && styles.brandChipActive]}
          onPress={() => setBrand('mastercard')}
        >
          <Text style={styles.brandText}>MC</Text>
        </PhysicalPress>
      </View>

      <TextInput
        value={last4}
        onChangeText={setLast4}
        keyboardType="number-pad"
        maxLength={4}
        placeholder="Últimos 4 dígitos"
        placeholderTextColor={colors.muted}
        style={styles.input}
      />

      <PhysicalPress style={styles.save} onPress={() => void save()}>
        <Text style={styles.saveText}>Guardar tarjeta demo</Text>
      </PhysicalPress>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  row: { flexDirection: 'row', gap: spacing.sm },
  brandChip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  brandChipActive: { backgroundColor: colors.accent, borderColor: colors.accent },
  brandText: { fontFamily: fonts.bodyBold, fontSize: 12, color: colors.ink },
  input: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    fontFamily: fonts.body,
    fontSize: 16,
    color: colors.ink,
  },
  save: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
  },
  saveText: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.paper },
});
