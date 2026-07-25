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
import { cartLinePreview } from '@/domain/contract-rules';
import { cartLineKey, moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function CartScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();

  const handleConfirm = async () => {
    const order = await flow.confirmOrder();
    if (order) {
      router.push('/(student)/confirmation');
    }
  };

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: 140 },
        ]}
      >
        <Text style={styles.title}>Tu pedido</Text>

        {flow.cartLines.length === 0 ? (
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
                <View style={styles.lineCopy}>
                  <Text style={styles.lineTitle}>{line.product.name}</Text>
                  {line.selectedOptionIds.length > 0 ? (
                    <Text style={styles.lineMeta}>
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
            <View style={styles.pickerCard}>
              <Text style={styles.pickerActive}>Para llevar</Text>
              <Text style={styles.pickerHint}>Fase 0: solo para llevar.</Text>
            </View>

            <Text style={styles.section}>Pago</Text>
            <View style={styles.pickerCard}>
              <Text style={styles.pickerActive}>Efectivo</Text>
              <Text style={styles.pickerHint}>VAI-10: cobro en caja con pase por cobrar.</Text>
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
          </>
        )}
      </ScrollView>

      <BottomNav
        activeTab="cart"
        cartCount={flow.cartCount}
        onMenu={() => router.push('/(student)/menu')}
        onAssistant={() => {}}
        onOrders={() => {}}
        onWallet={() => {}}
        onCart={() => {}}
      />
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
    padding: spacing.lg,
    flexDirection: 'row',
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
  },
  lineCopy: { flex: 1, gap: 4 },
  lineTitle: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  lineMeta: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  linePrice: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink, marginTop: 4 },
  qtyRow: { alignItems: 'center', gap: spacing.sm },
  qtyButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.paper,
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyButtonText: { fontFamily: fonts.bodyBold, fontSize: 18, color: colors.ink },
  qtyValue: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  section: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  pickerCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.accent,
    gap: 4,
  },
  pickerActive: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  pickerHint: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
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
  confirmButton: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  confirmDisabled: { opacity: 0.45 },
  confirmText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
});
