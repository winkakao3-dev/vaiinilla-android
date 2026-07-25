import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { isInstantDemoPayment } from '@/domain/checkout-fixtures';
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

  const instant = isInstantDemoPayment(order.summary.paymentMethod);
  const eyebrow = instant ? 'COMANDA ENVIADA' : 'PEDIDO CREADO';
  const title = instant
    ? '¡Listo! Tu pedido ya está en cocina.'
    : '¡Pedido registrado! Pasa a caja.';
  const subtitle = instant
    ? 'Saldo o tarjeta demo confirmados. Sigue el avance en Pedidos.'
    : 'Págalo en efectivo y usa el sticker para identificar tu orden.';

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.xl }]}
    >
      <View style={styles.successMark}>
        <Text style={styles.successGlyph}>✓</Text>
      </View>

      <Text style={styles.eyebrow}>{eyebrow}</Text>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.subtitle}>{subtitle}</Text>

      <View style={styles.folioCard}>
        <Text style={styles.folioLabel}>Folio</Text>
        <Text style={styles.folioValue}>#{order.summary.folio}</Text>
        <Text style={styles.folioState}>
          Estado: {instant ? 'cobrado' : 'por cobrar'}
        </Text>
      </View>

      <View style={styles.receipt}>
        <Text style={styles.receiptTitle}>DETALLE</Text>
        {order.items.map((item) => (
          <View key={item.id} style={styles.receiptLine}>
            <Text style={styles.receiptItem} numberOfLines={2}>
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
          router.replace('/(student)/orders');
        }}
      >
        <Text style={styles.ctaText}>Ver pedidos</Text>
      </PhysicalPress>

      <PhysicalPress
        style={styles.secondary}
        onPress={() =>
          router.push({
            pathname: '/(student)/sticker',
            params: { orderId: order.summary.id },
          })
        }
      >
        <Text style={styles.secondaryText}>Ver sticker de recibo</Text>
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
  successMark: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  successGlyph: {
    fontFamily: fonts.displayBlack,
    fontSize: 28,
    color: colors.accentInk,
    lineHeight: 32,
  },
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.2,
    color: colors.muted,
  },
  title: { fontFamily: fonts.displayBlack, fontSize: 30, lineHeight: 34, color: colors.ink },
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
  secondary: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
  },
  secondaryText: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
  cta: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
  },
  ctaText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
});
