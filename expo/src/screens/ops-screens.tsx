import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import {
  collectCash,
  generateIdempotencyKey,
  listOrders,
  transitionOrder,
  type OperationalRole,
} from '@/data/order-repository';
import type { OrderDetail, OrderState } from '@/domain/models';
import { moneyLabel } from '@/domain/models';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface OpsOrderListProps {
  role: OperationalRole;
  title: string;
  actionLabel: string;
  showCashInput?: boolean;
  resolveTargetState?: (order: OrderDetail) => OrderState;
}

function OpsOrderList({
  role,
  title,
  actionLabel,
  showCashInput = false,
  resolveTargetState,
}: OpsOrderListProps) {
  const insets = useSafeAreaInsets();
  const [orders, setOrders] = useState<OrderDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [amounts, setAmounts] = useState<Record<string, string>>({});

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setOrders(await listOrders(role));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No pudimos cargar pedidos.');
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const handleAction = async (order: OrderDetail) => {
    setBusyId(order.summary.id);
    setError(null);
    try {
      const key = await generateIdempotencyKey();
      if (showCashInput) {
        const amount = amounts[order.summary.id] ?? order.summary.total;
        await collectCash(order.summary.id, amount, order.summary.version, key);
      } else if (resolveTargetState) {
        await transitionOrder(
          order.summary.id,
          resolveTargetState(order),
          order.summary.version,
          key,
        );
      }
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No pudimos actualizar el pedido.');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <Text style={styles.title}>{title}</Text>
      <PhysicalPress style={styles.refresh} onPress={() => void refresh()}>
        <Text style={styles.refreshText}>Actualizar</Text>
      </PhysicalPress>

      {loading ? <ActivityIndicator color={colors.ink} /> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      {orders.length === 0 && !loading ? (
        <Text style={styles.empty}>No hay pedidos para esta estación.</Text>
      ) : null}

      {orders.map((order) => (
        <View key={order.summary.id} style={styles.card}>
          <Text style={styles.folio}>#{order.summary.folio}</Text>
          <Text style={styles.meta}>
            {order.summary.state} · {moneyLabel(order.summary.total)}
          </Text>
          {order.items.map((item) => (
            <Text key={item.id} style={styles.line}>
              {item.quantity}× {item.productName}
            </Text>
          ))}
          {showCashInput ? (
            <TextInput
              value={amounts[order.summary.id] ?? order.summary.total}
              onChangeText={(value) =>
                setAmounts((current) => ({ ...current, [order.summary.id]: value }))
              }
              keyboardType="decimal-pad"
              style={styles.input}
              placeholder="Monto recibido"
              placeholderTextColor={colors.muted}
            />
          ) : null}
          <PhysicalPress
            style={styles.action}
            disabled={busyId === order.summary.id}
            onPress={() => void handleAction(order)}
          >
            <Text style={styles.actionText}>
              {busyId === order.summary.id ? 'Procesando…' : actionLabel}
            </Text>
          </PhysicalPress>
        </View>
      ))}
    </ScrollView>
  );
}

export function CajaScreen() {
  return (
    <OpsOrderList
      role="cajero"
      title="Caja"
      actionLabel="Cobrar efectivo"
      showCashInput
    />
  );
}

export function CocinaScreen() {
  return (
    <OpsOrderList
      role="cocina"
      title="Cocina"
      actionLabel="Avanzar preparación"
      resolveTargetState={(order) => (order.summary.state === 'cobrado' ? 'preparando' : 'listo')}
    />
  );
}

export function MeseroScreen() {
  return (
    <OpsOrderList
      role="mesero"
      title="Mesero"
      actionLabel="Marcar entregado"
      resolveTargetState={() => 'entregado'}
    />
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  refresh: { alignSelf: 'flex-start' },
  refreshText: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.ink, textDecorationLine: 'underline' },
  empty: { fontFamily: fonts.body, fontSize: 14, color: colors.muted },
  error: { fontFamily: fonts.body, color: colors.coral },
  card: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: spacing.sm,
  },
  folio: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.ink },
  meta: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, textTransform: 'capitalize' },
  line: { fontFamily: fonts.body, fontSize: 14, color: colors.ink },
  action: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.md,
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  actionText: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.paper },
  input: {
    backgroundColor: colors.paper,
    borderRadius: radius.button,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
    fontFamily: fonts.body,
    fontSize: 15,
    color: colors.ink,
  },
});
