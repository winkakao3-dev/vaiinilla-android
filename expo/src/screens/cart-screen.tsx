import { router } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { ProductImage } from '@/components/product-image';
import { DEMO_SPACES } from '@/domain/checkout-fixtures';
import { cartLinePreview } from '@/domain/contract-rules';
import { cartLineKey, moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { useStudentNav } from '@/hooks/use-student-nav';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

const PAYMENT_OPTIONS = [
  { key: 'efectivo' as const, label: 'Efectivo', badge: 'CASH', hint: 'Paga en caja al recoger' },
  { key: 'saldo' as const, label: 'Saldo Vaiinilla', badge: 'SALDO', hint: null },
  { key: 'tarjeta' as const, label: 'Tarjeta', badge: 'VISA', hint: 'Demo sin cargo real' },
];

export function CartScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const wallet = useWallet();
  const nav = useStudentNav('cart');

  const handleConfirm = async () => {
    const order = await flow.confirmOrder();
    if (order) {
      router.push('/(student)/confirmation');
    }
  };

  const hasItems = flow.cartLines.length > 0;

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: hasItems ? 120 : 140 },
        ]}
      >
        <Text style={styles.title}>Tu pedido</Text>

        {!hasItems ? (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>Tu carrito está vacío</Text>
            <PhysicalPress style={styles.emptyButton} onPress={() => router.push('/(student)/menu')}>
              <Text style={styles.emptyButtonText}>Ir al menú</Text>
            </PhysicalPress>
          </View>
        ) : (
          <>
            {flow.cartLines.map((line) => (
              <View key={`${line.product.id}-${line.selectedOptionIds.join('-')}`} style={styles.lineCard}>
                <View style={styles.thumbWrap}>
                  <ProductImage
                    imageUrl={line.product.imageUrl}
                    style={styles.thumb}
                    accessibilityLabel={line.product.name}
                  />
                </View>
                <View style={styles.lineCopy}>
                  <Text style={styles.lineTitle}>{line.product.name}</Text>
                  {line.selectedOptionIds.length > 0 ? (
                    <Text style={styles.lineMeta} numberOfLines={2}>
                      {line.product.optionGroups
                        .flatMap((group) => group.options)
                        .filter((option) => line.selectedOptionIds.includes(option.id))
                        .map((option) => option.name)
                        .join(' · ')}
                    </Text>
                  ) : null}
                  <Text style={styles.linePrice}>{moneyLabel(cartLinePreview(line))}</Text>
                </View>
                <View style={styles.qtyRow}>
                  <PhysicalPress
                    style={styles.qtyButton}
                    onPress={() => flow.updateCartQuantity(cartLineKey(line), -1)}
                  >
                    <Text style={styles.qtyButtonText}>−</Text>
                  </PhysicalPress>
                  <Text style={styles.qtyValue}>{line.quantity}</Text>
                  <PhysicalPress
                    style={styles.qtyButton}
                    onPress={() => flow.updateCartQuantity(cartLineKey(line), 1)}
                  >
                    <Text style={styles.qtyButtonText}>+</Text>
                  </PhysicalPress>
                </View>
              </View>
            ))}

            <Text style={styles.section}>Entrega</Text>
            <View style={styles.optionRow}>
              <PhysicalPress
                style={[
                  styles.optionCard,
                  flow.checkoutDestination === 'para_llevar' && styles.optionCardActive,
                ]}
                onPress={() => flow.setCheckoutDestination('para_llevar')}
              >
                <Text style={styles.optionTitle}>Para llevar</Text>
              </PhysicalPress>
              <PhysicalPress
                style={[
                  styles.optionCard,
                  flow.checkoutDestination === 'en_espacio' && styles.optionCardActive,
                ]}
                onPress={() => flow.setCheckoutDestination('en_espacio')}
              >
                <Text style={styles.optionTitle}>En mesa</Text>
              </PhysicalPress>
            </View>

            {flow.checkoutDestination === 'en_espacio' ? (
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.spaceRow}>
                {DEMO_SPACES.map((space) => {
                  const active = flow.selectedSpaceId === space.id;
                  return (
                    <PhysicalPress
                      key={space.id}
                      style={[styles.spaceChip, active && styles.spaceChipActive]}
                      onPress={() => flow.setSelectedSpaceId(space.id)}
                    >
                      <Text style={[styles.spaceText, active && styles.spaceTextActive]}>{space.name}</Text>
                    </PhysicalPress>
                  );
                })}
              </ScrollView>
            ) : null}

            <Text style={styles.section}>Pago</Text>
            <View style={styles.paymentColumn}>
              {PAYMENT_OPTIONS.map((option) => {
                const active = flow.checkoutPayment === option.key;
                return (
                  <PhysicalPress
                    key={option.key}
                    style={[styles.paymentCard, active && styles.paymentCardActive]}
                    onPress={() => flow.setCheckoutPayment(option.key)}
                  >
                    <View style={styles.paymentHeader}>
                      <Text style={styles.optionTitle}>{option.label}</Text>
                      <View style={[styles.badge, active && styles.badgeActive]}>
                        <Text style={[styles.badgeText, active && styles.badgeTextActive]}>
                          {option.badge}
                        </Text>
                      </View>
                    </View>
                    {option.key === 'saldo' ? (
                      <Text style={styles.paymentHint}>Saldo: {moneyLabel(wallet.balance)}</Text>
                    ) : option.hint ? (
                      <Text style={styles.paymentHint}>{option.hint}</Text>
                    ) : null}
                  </PhysicalPress>
                );
              })}
            </View>

            <Text style={styles.section}>Notas para cocina</Text>
            <TextInput
              value={flow.kitchenNotes}
              onChangeText={flow.setKitchenNotes}
              placeholder="Ej. Salsa aparte"
              placeholderTextColor={colors.muted}
              style={styles.notes}
              multiline
            />

            <View style={styles.totalRow}>
              <Text style={styles.totalLabel}>Total</Text>
              <Text style={styles.totalValue}>{moneyLabel(flow.cartTotal)}</Text>
            </View>

            {flow.submitError ? <Text style={styles.error}>{flow.submitError}</Text> : null}
          </>
        )}
      </ScrollView>

      {hasItems ? (
        <View style={[styles.stickyBar, { paddingBottom: insets.bottom + 72 }]}>
          <View style={styles.stickyTotal}>
            <Text style={styles.stickyLabel}>Total</Text>
            <Text style={styles.stickyValue}>{moneyLabel(flow.cartTotal)}</Text>
          </View>
          <PhysicalPress
            style={[styles.confirmButton, (!flow.canCreateOrder || flow.submitting) && styles.confirmDisabled]}
            disabled={!flow.canCreateOrder || flow.submitting}
            onPress={() => void handleConfirm()}
          >
            {flow.submitting ? (
              <ActivityIndicator color={colors.paper} />
            ) : (
              <Text style={styles.confirmText}>Confirmar pedido</Text>
            )}
          </PhysicalPress>
        </View>
      ) : null}

      <BottomNav {...nav} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  empty: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.xl,
    alignItems: 'center',
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
  },
  emptyTitle: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  emptyButton: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
  },
  emptyButtonText: { fontFamily: fonts.bodyBold, color: colors.paper },
  lineCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.md,
    flexDirection: 'row',
    gap: spacing.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
  },
  thumbWrap: {
    width: 52,
    height: 52,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: colors.line,
  },
  thumb: { width: 52, height: 52 },
  lineCopy: { flex: 1, gap: 2 },
  lineTitle: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
  lineMeta: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  linePrice: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink, marginTop: 2 },
  qtyRow: { alignItems: 'center', gap: spacing.xs },
  qtyButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: colors.paper,
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyButtonText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  qtyValue: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  section: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  optionRow: { flexDirection: 'row', gap: spacing.sm },
  optionCard: {
    flex: 1,
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
  },
  optionCardActive: { borderColor: colors.accent, backgroundColor: '#edf3d8' },
  optionTitle: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
  spaceRow: { gap: spacing.sm, paddingVertical: spacing.sm },
  spaceChip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  spaceChipActive: { backgroundColor: colors.ink, borderColor: colors.ink },
  spaceText: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.ink },
  spaceTextActive: { color: colors.paper },
  paymentColumn: { gap: spacing.sm },
  paymentCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
  },
  paymentCardActive: { borderColor: colors.accent, backgroundColor: '#edf3d8' },
  paymentHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  badge: {
    backgroundColor: colors.ink,
    borderRadius: 8,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
  },
  badgeActive: { backgroundColor: colors.accentInk },
  badgeText: { fontFamily: fonts.bodyBold, fontSize: 10, color: colors.paper, letterSpacing: 0.6 },
  badgeTextActive: { color: colors.paper },
  paymentHint: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  notes: {
    minHeight: 88,
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    fontFamily: fonts.body,
    fontSize: 14,
    color: colors.ink,
    textAlignVertical: 'top',
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    marginTop: spacing.sm,
  },
  totalLabel: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.muted },
  totalValue: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  error: { fontFamily: fonts.body, color: colors.coral },
  stickyBar: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: colors.paper,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    paddingHorizontal: spacing.screen,
    paddingTop: spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  stickyTotal: { flex: 1, gap: 2 },
  stickyLabel: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  stickyValue: { fontFamily: fonts.displayBlack, fontSize: 22, color: colors.ink },
  confirmButton: {
    flex: 1.2,
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
  },
  confirmDisabled: { opacity: 0.45 },
  confirmText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
});
