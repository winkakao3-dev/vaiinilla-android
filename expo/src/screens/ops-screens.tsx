import { router } from 'expo-router';
import React, { useCallback, useEffect, useState, type ReactNode } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, SectionHead, Tab, TopBar } from '@/components/ui';
import {
  collectCash,
  generateIdempotencyKey,
  listOrders,
  transitionOrder,
  type OperationalRole,
} from '@/data/order-repository';
import {
  DESTINATION_LABELS,
  PAYMENT_LABELS,
  moneyLabel,
  type OrderDetail,
  type OrderState,
} from '@/domain/models';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

type CardTone = 'accent' | 'dark' | 'yellow' | 'coral';

interface OpsOrderListProps {
  role: OperationalRole;
  title: string;
  actionLabel: string;
  emptyIcon: string;
  emptyTitle: string;
  emptyBody: string;
  tone: CardTone;
  header?: ReactNode;
  topbarRight?: ReactNode;
  showCashInput?: boolean;
  resolveTargetState?: (order: OrderDetail) => OrderState;
}

/**
 * Lista compartida por Caja, Cocina y Mesero (pantallas 31 a 41 del demo).
 * Cada pedido se pinta como .task-card con .task-head, .status-badge,
 * .task-meta y .task-actions; el vacio usa el bloque .empty.
 */
function OpsOrderList({
  role,
  title,
  actionLabel,
  emptyIcon,
  emptyTitle,
  emptyBody,
  tone,
  header,
  topbarRight,
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

  const toneStyle = TONE_STYLES[tone];

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
          title={title}
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
          right={topbarRight}
        />

        {header}

        {loading ? <ActivityIndicator color={colors.ink} style={styles.loader} /> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {orders.length === 0 && !loading ? (
          // .empty { padding:30 20; radius:28; paper-2; centrado }
          <View style={styles.empty}>
            <View style={styles.emptyIcon}>
              <Text style={styles.emptyIconText}>{emptyIcon}</Text>
            </View>
            <Text style={styles.emptyTitle}>{emptyTitle}</Text>
            <Text style={styles.emptyBody}>{emptyBody}</Text>
          </View>
        ) : null}

        {orders.map((order) => {
          const busy = busyId === order.summary.id;
          return (
            // .task-card { radius:30; padding:20; margin-bottom:12 }
            <View key={order.summary.id} style={[styles.taskCard, toneStyle.card]}>
              <View style={styles.taskHead}>
                <View style={styles.flex}>
                  <Text style={[styles.taskEyebrow, toneStyle.text]}>
                    Pedido #{order.summary.folio}
                  </Text>
                  <Text style={[styles.taskTitle, toneStyle.text]}>
                    {order.items
                      .map((item) => `${item.quantity}× ${item.productName}`)
                      .join(', ')}
                  </Text>
                </View>
                {/* .status-badge */}
                <View style={[styles.statusBadge, toneStyle.badge]}>
                  <Text style={[styles.statusBadgeText, toneStyle.text]}>
                    {moneyLabel(order.summary.total)}
                  </Text>
                </View>
              </View>

              {/* .task-meta { gap:14; margin-top:18; 11px/800; opacity .72 } */}
              <View style={styles.taskMeta}>
                <Text style={[styles.taskMetaText, toneStyle.text]}>
                  {DESTINATION_LABELS[order.summary.destination]}
                </Text>
                <Text style={[styles.taskMetaText, toneStyle.text]}>
                  {PAYMENT_LABELS[order.summary.paymentMethod]}
                </Text>
              </View>

              {showCashInput ? (
                <TextInput
                  value={amounts[order.summary.id] ?? order.summary.total}
                  onChangeText={(value) =>
                    setAmounts((current) => ({ ...current, [order.summary.id]: value }))
                  }
                  keyboardType="decimal-pad"
                  style={styles.cashInput}
                  placeholder="Monto recibido"
                  placeholderTextColor={colors.muted}
                />
              ) : null}

              {/* .task-actions { gap:8; margin-top:17 } */}
              <View style={styles.taskActions}>
                <PhysicalPress
                  style={styles.taskActionButton}
                  disabled={busy}
                  onPress={() => void handleAction(order)}
                >
                  <Text style={styles.taskActionText}>
                    {busy ? 'Procesando…' : actionLabel}
                  </Text>
                </PhysicalPress>
                <PhysicalPress
                  style={[styles.taskActionButton, styles.taskActionSecondary]}
                  onPress={() => void refresh()}
                >
                  <Text style={[styles.taskActionText, toneStyle.text]}>Actualizar</Text>
                </PhysicalPress>
              </View>
            </View>
          );
        })}
      </ScrollView>
    </View>
  );
}

/** Pantallas 31 a 35: Caja con .tabs y tarjetas en accent. */
export function CajaScreen() {
  const [tab, setTab] = useState('Por cobrar');

  return (
    <OpsOrderList
      role="cajero"
      title="Caja"
      tone="accent"
      actionLabel="Cobrar pedido"
      showCashInput
      emptyIcon="✓"
      emptyTitle="Nada por cobrar"
      emptyBody="Los pedidos en efectivo aparecerán aquí."
      header={
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tabs}
        >
          {['Por cobrar', 'Entregas', 'Recargas'].map((label) => (
            <Tab key={label} label={label} active={tab === label} onPress={() => setTab(label)} />
          ))}
        </ScrollView>
      }
    />
  );
}

/** Pantallas 36 a 38: Cocina con badge KDS y .kpi-grid. */
export function CocinaScreen() {
  return (
    <OpsOrderList
      role="cocina"
      title="Cocina"
      tone="dark"
      actionLabel="Avanzar preparación"
      resolveTargetState={(order) => (order.summary.state === 'cobrado' ? 'preparando' : 'listo')}
      emptyIcon="♨"
      emptyTitle="Sin comandas"
      emptyBody="Los pedidos cobrados aparecerán automáticamente."
      topbarRight={
        <View style={styles.kdsBadge}>
          <Text style={styles.kdsBadgeText}>KDS</Text>
        </View>
      }
      header={
        <>
          {/* .kpi-grid { gap:8 } */}
          <View style={styles.kpiGrid}>
            <Kpi value="0" label="Activas" />
            <Kpi value="0" label="En curso" />
            <Kpi value="19" label="Entregadas" />
          </View>
          <SectionHead title="Comandas" actionLabel="Orden por llegada" />
        </>
      }
    />
  );
}

/** Pantallas 39 a 41: Mesero con .hero en coral. */
export function MeseroScreen() {
  return (
    <OpsOrderList
      role="mesero"
      title="Mesero"
      tone="coral"
      actionLabel="Marcar entregado"
      resolveTargetState={() => 'entregado'}
      emptyIcon="⌁"
      emptyTitle="No hay mesas esperando"
      emptyBody="Los pedidos listos para mesa aparecerán aquí."
      header={
        <>
          {/* .hero coral { min-height:155 } */}
          <View style={styles.meseroHero}>
            <Text style={styles.heroWatermark}>V</Text>
            <Text style={styles.heroEyebrow}>Servicio en mesa</Text>
            <Text style={styles.heroTitle}>Entrega clara, sin gritos en la barra.</Text>
          </View>
          <SectionHead title="Listos para salir" />
        </>
      }
    />
  );
}

/** `.kpi { radius:21; paper-2; padding:14 }` */
function Kpi({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.kpi}>
      <Text style={styles.kpiValue}>{value}</Text>
      <Text style={styles.kpiLabel}>{label}</Text>
    </View>
  );
}

const TONE_STYLES: Record<
  CardTone,
  { card: object; text: object; badge: object }
> = {
  accent: {
    card: { backgroundColor: colors.accent },
    text: { color: colors.accentInk },
    badge: { backgroundColor: 'rgba(255,255,255,0.45)' },
  },
  dark: {
    card: { backgroundColor: '#1c1d1b' },
    text: { color: '#f5f2e8' },
    badge: { backgroundColor: 'rgba(255,255,255,0.16)' },
  },
  yellow: {
    card: { backgroundColor: colors.yolk },
    text: { color: '#28200b' },
    badge: { backgroundColor: 'rgba(255,255,255,0.45)' },
  },
  coral: {
    card: { backgroundColor: colors.coral },
    text: { color: '#2d100e' },
    badge: { backgroundColor: 'rgba(255,255,255,0.45)' },
  },
};

const styles = StyleSheet.create({
  flex: { flex: 1, minWidth: 0 },
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },
  loader: { marginVertical: spacing.md },
  error: { fontFamily, fontSize: 12, color: colors.coral, marginBottom: spacing.sm },

  // .tabs { gap:8; margin:0 -20 16 }
  tabs: { gap: 8, paddingBottom: 4, marginBottom: 16 },

  // .task-card
  taskCard: { borderRadius: 30, padding: 20, marginBottom: 12 },
  taskHead: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  taskEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    opacity: 0.72,
  },
  // .task-card h3 { 22px; line-height:.98; max-width:12ch }
  taskTitle: {
    fontFamily,
    fontSize: 22,
    lineHeight: 22 * 0.98,
    letterSpacing: -0.44,
    fontWeight: weight.black,
    marginTop: 4,
  },
  statusBadge: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 12,
  },
  statusBadgeText: {
    fontFamily,
    fontSize: 10,
    fontWeight: weight.black,
    letterSpacing: 0.8,
  },
  taskMeta: { flexDirection: 'row', gap: 14, flexWrap: 'wrap', marginTop: 18 },
  taskMetaText: { fontFamily, fontSize: 11, fontWeight: weight.bold, opacity: 0.72 },

  cashInput: {
    marginTop: 14,
    backgroundColor: 'rgba(255,255,255,0.6)',
    borderRadius: 15,
    paddingVertical: 11,
    paddingHorizontal: 14,
    fontFamily,
    fontSize: 13,
    color: colors.ink,
  },

  // .task-actions button { height:43; radius:15; #171817; 12px/900 }
  taskActions: { flexDirection: 'row', gap: 8, marginTop: 17 },
  taskActionButton: {
    height: 43,
    borderRadius: 15,
    paddingHorizontal: 14,
    backgroundColor: '#171817',
    alignItems: 'center',
    justifyContent: 'center',
  },
  taskActionSecondary: { backgroundColor: 'rgba(255,255,255,0.45)' },
  taskActionText: { fontFamily, fontSize: 12, fontWeight: weight.black, color: '#fff' },

  // .empty
  empty: {
    paddingVertical: 30,
    paddingHorizontal: 20,
    borderRadius: radius.lg,
    backgroundColor: colors.paper2,
    alignItems: 'center',
  },
  emptyIcon: {
    width: 72,
    height: 72,
    borderRadius: 25,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 15,
  },
  emptyIconText: { fontFamily, fontSize: 32, color: colors.accentInk },
  emptyTitle: { fontFamily, fontSize: 19, fontWeight: weight.black, color: colors.ink },
  emptyBody: {
    ...typography.body,
    textAlign: 'center',
    marginTop: 7,
    maxWidth: 260,
  },

  // .kpi-grid / .kpi
  kpiGrid: { flexDirection: 'row', gap: 8 },
  kpi: {
    flex: 1,
    minWidth: 0,
    borderRadius: 21,
    backgroundColor: colors.paper2,
    padding: 14,
  },
  kpiValue: { fontFamily, fontSize: 21, fontWeight: weight.black, color: colors.ink },
  kpiLabel: { fontFamily, fontSize: 9, color: colors.muted, letterSpacing: 0.72, marginTop: 2 },

  kdsBadge: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 12,
    backgroundColor: colors.paper2,
  },
  kdsBadgeText: {
    fontFamily,
    fontSize: 10,
    fontWeight: weight.black,
    letterSpacing: 0.8,
    color: colors.ink,
  },

  // .hero coral del mesero
  meseroHero: {
    borderRadius: radius.xl,
    padding: 24,
    minHeight: 155,
    backgroundColor: colors.coral,
    justifyContent: 'flex-end',
    overflow: 'hidden',
  },
  heroWatermark: {
    position: 'absolute',
    right: -5,
    top: -38,
    fontFamily,
    fontSize: 180,
    fontWeight: weight.black,
    color: '#2b100d',
    opacity: 0.09,
  },
  heroEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: '#2b100d',
    opacity: 0.65,
  },
  heroTitle: {
    ...typography.headline,
    color: '#2b100d',
    marginTop: 10,
  },
});
