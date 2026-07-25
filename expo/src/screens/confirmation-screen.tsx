import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function ConfirmationScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const order = flow.createdOrder;

  if (!order) {
    return (
      <View style={styles.centered}>
        <PhysicalPress
          style={styles.cta}
          onPress={() => {
            flow.clearCreatedOrder();
            router.replace('/(student)/menu');
          }}
        >
          <Text style={styles.ctaText}>Volver al menú</Text>
        </PhysicalPress>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.xl }]}
    >
      <Text style={styles.eyebrow}>PEDIDO CREADO</Text>
      <Text style={styles.title}>Tu pase de Caja acaba de salir.</Text>
      <Text style={styles.subtitle}>
        Págalo en efectivo y usa este receipt sticker para identificar la orden.
      </Text>

      <View style={styles.folioCard}>
        <Text style={styles.folioLabel}>Folio</Text>
        <Text style={styles.folioValue}>#{order.summary.folio}</Text>
        <Text style={styles.folioState}>Estado: por cobrar</Text>
      </View>

      <View style={styles.receipt}>
        <Text style={styles.receiptTitle}>DETALLE</Text>
        {order.items.map((item) => (
          <View key={item.id} style={styles.receiptLine}>
            <Text style={styles.receiptItem}>
              {item.quantity} × {item.productName}
            </Text>
            <Text style={styles.receiptPrice}>{moneyLabel(item.subtotal)}</Text>
          </View>
        ))}
        <View style={styles.receiptTotalRow}>
          <Text style={styles.receiptTotalLabel}>TOTAL</Text>
          <Text style={styles.receiptTotal}>{moneyLabel(order.summary.total)}</Text>
        </View>
      </View>

      <PhysicalPress
        style={styles.cta}
        onPress={() => {
          flow.clearCreatedOrder();
          router.replace('/(student)/menu');
        }}
      >
        <Text style={styles.ctaText}>Volver al menú</Text>
      </PhysicalPress>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, paddingBottom: spacing.xxl, gap: spacing.lg },
  centered: {
    flex: 1,
    backgroundColor: colors.paper,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.screen,
  },
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.2,
    color: colors.muted,
  },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, lineHeight: 30, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 15, lineHeight: 22, color: colors.muted },
  folioCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.xl,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 6,
  },
  folioLabel: { fontFamily: fonts.bodyBold, fontSize: 12, color: colors.muted },
  folioValue: { fontFamily: fonts.displayBlack, fontSize: 42, color: colors.ink },
  folioState: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.coral },
  receipt: {
    backgroundColor: colors.ink,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: spacing.md,
  },
  receiptTitle: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.2,
    color: colors.paper,
    opacity: 0.7,
  },
  receiptLine: { flexDirection: 'row', justifyContent: 'space-between', gap: spacing.md },
  receiptItem: { flex: 1, fontFamily: fonts.body, fontSize: 14, color: colors.paper },
  receiptPrice: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.paper },
  receiptTotalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.12)',
    paddingTop: spacing.md,
    marginTop: spacing.sm,
  },
  receiptTotalLabel: { fontFamily: fonts.bodyBold, fontSize: 12, color: colors.paper, opacity: 0.7 },
  receiptTotal: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.accent },
  cta: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
    marginTop: spacing.md,
  },
  ctaText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
});
