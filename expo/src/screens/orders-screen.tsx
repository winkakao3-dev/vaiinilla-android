import { router } from 'expo-router';
import React, { useEffect } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { OrderTrackingCard } from '@/components/order-tracking-card';
import { PhysicalPress } from '@/components/physical-press';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { useStudentNav } from '@/hooks/use-student-nav';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function OrdersScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const nav = useStudentNav('orders');

  useEffect(() => {
    void flow.refreshClientOrders();
  }, [flow.refreshClientOrders]);

  const order = flow.selectedOrder;

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: 140 },
        ]}
      >
        <Text style={styles.title}>Tus pedidos</Text>
        <Text style={styles.subtitle}>Sigue el estado en tiempo real del flujo demo.</Text>

        {flow.clientOrders.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>Aún no tienes pedidos activos</Text>
            <Text style={styles.emptyBody}>Cuando confirmes un pedido aparecerá aquí con su timeline.</Text>
            <PhysicalPress style={styles.emptyButton} onPress={() => router.push('/(student)/menu')}>
              <Text style={styles.emptyButtonText}>Ir al menú</Text>
            </PhysicalPress>
          </View>
        ) : (
          <>
            {order ? (
              <OrderTrackingCard
                order={order}
                onPress={() =>
                  router.push({
                    pathname: '/(student)/sticker',
                    params: { orderId: order.summary.id },
                  })
                }
              />
            ) : null}

            <Text style={styles.section}>Historial</Text>
            {flow.clientOrders.map((item) => (
              <PhysicalPress
                key={item.summary.id}
                style={[
                  styles.historyCard,
                  flow.selectedOrderId === item.summary.id && styles.historyCardActive,
                ]}
                onPress={() => flow.selectOrder(item.summary.id)}
              >
                <Text style={styles.historyFolio}>#{item.summary.folio}</Text>
                <Text style={styles.historyMeta}>
                  {item.summary.state} · {item.summary.paymentMethod}
                </Text>
              </PhysicalPress>
            ))}
          </>
        )}
      </ScrollView>

      <BottomNav {...nav} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 14, color: colors.muted, marginBottom: spacing.sm },
  empty: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
  },
  emptyTitle: { fontFamily: fonts.bodyBold, fontSize: 18, color: colors.ink },
  emptyBody: { fontFamily: fonts.body, fontSize: 14, lineHeight: 20, color: colors.muted },
  emptyButton: {
    alignSelf: 'flex-start',
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
  },
  emptyButtonText: { fontFamily: fonts.bodyBold, color: colors.paper },
  section: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  historyCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
  },
  historyCardActive: { borderColor: colors.accent },
  historyFolio: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.ink },
  historyMeta: { fontFamily: fonts.body, fontSize: 12, color: colors.muted, textTransform: 'capitalize' },
});
