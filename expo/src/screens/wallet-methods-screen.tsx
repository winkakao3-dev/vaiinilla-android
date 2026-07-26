import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { IconButton, PrimaryButton, SectionHead, TopBar } from '@/components/ui';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/**
 * Pantalla 28 del demo.
 * Parrafo .body, .section-head "Tarjetas" con .wallet-section de tarjetas,
 * boton .primary.wide y la seccion de transferencia con .bank-data.
 */
export function WalletMethodsScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();

  return (
    <View style={styles.root}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.body,
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingBottom: spacing.screenBottomNoNav + insets.bottom,
          },
        ]}
      >
        <TopBar
          title="Métodos de pago"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        <Text style={typography.body}>
          La tarjeta puede pagar un pedido directamente o añadir dinero. La transferencia sólo
          recarga el saldo.
        </Text>

        <SectionHead
          title="Tarjetas"
          actionLabel="Agregar"
          onAction={() => router.push('/(student)/wallet-add-card')}
        />
        <View style={styles.walletSection}>
          {wallet.cards.map((card) => (
            <View key={card.id} style={styles.paymentMethod}>
              <View style={styles.paymentBrand}>
                <Text style={styles.paymentBrandText}>{card.brand.toUpperCase()}</Text>
              </View>
              <View style={styles.paymentCopy}>
                <Text style={styles.paymentTitle}>•••• {card.last4}</Text>
                <Text style={styles.paymentHint}>DANI ÁLVAREZ · vence 08/29</Text>
              </View>
              <View style={styles.paymentCheck}>
                <Text style={styles.paymentCheckText}>✓</Text>
              </View>
            </View>
          ))}
        </View>

        <PrimaryButton
          label="Agregar método de pago"
          onPress={() => router.push('/(student)/wallet-add-card')}
        />

        <SectionHead title="Transferencia" />
        <View style={styles.walletSection}>
          <View style={styles.paymentMethod}>
            <View style={[styles.paymentBrand, styles.paymentBrandTransfer]}>
              <Text style={[styles.paymentBrandText, styles.paymentBrandTextTransfer]}>SPEI</Text>
            </View>
            <View style={styles.paymentCopy}>
              <Text style={styles.paymentTitle}>Cuenta para recargas</Text>
              <Text style={styles.paymentHint}>STP · CLABE terminación 2019</Text>
            </View>
          </View>

          <View style={styles.bankData}>
            <View style={styles.bankRow}>
              <Text style={styles.bankLabel}>CLABE</Text>
              <Text style={styles.bankValue}>646180157034852019</Text>
            </View>
            <View style={[styles.bankRow, styles.bankRowLast]}>
              <Text style={styles.bankLabel}>Referencia</Text>
              <Text style={styles.bankValue}>UTCH241087</Text>
            </View>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  walletSection: {
    borderRadius: radius.lineItem,
    backgroundColor: colors.paper2,
    padding: 11,
    marginBottom: 8,
  },
  paymentMethod: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderRadius: 17,
    backgroundColor: colors.paper,
    padding: 10,
    marginTop: 5,
  },
  paymentBrand: {
    width: 42,
    height: 31,
    borderRadius: 10,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paymentBrandTransfer: { backgroundColor: colors.accent },
  paymentBrandText: {
    fontFamily,
    fontSize: 8,
    fontWeight: weight.black,
    letterSpacing: 0.32,
    color: colors.paper,
  },
  paymentBrandTextTransfer: { color: colors.accentInk },
  paymentCopy: { flex: 1, minWidth: 0 },
  paymentTitle: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  paymentHint: { fontFamily, fontSize: 8, lineHeight: 10, color: colors.muted, marginTop: 2 },
  paymentCheck: {
    width: 20,
    height: 20,
    borderRadius: 999,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paymentCheckText: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.accentInk },

  bankData: { marginTop: 6, paddingHorizontal: 4 },
  bankRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    paddingVertical: 8,
  },
  bankRowLast: { borderBottomWidth: 0 },
  bankLabel: { fontFamily, fontSize: 9, color: colors.muted },
  bankValue: {
    fontFamily,
    fontSize: 9,
    fontWeight: weight.bold,
    color: colors.ink,
    marginLeft: 'auto',
    textAlign: 'right',
  },
});
