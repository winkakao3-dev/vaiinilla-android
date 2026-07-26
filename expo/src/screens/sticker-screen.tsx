import { router, useLocalSearchParams } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import {
  orderToStickerData,
  STICKER_STYLES,
  StickerStyleContent,
} from '@/components/sticker-styles';
import { IconButton, TopBar } from '@/components/ui';
import { getSampleOrder } from '@/data/order-repository';
import { PAYMENT_LABELS } from '@/domain/models';
import { destinationDisplayLabel } from '@/domain/tracking-timeline';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

/**
 * Pantallas 51 a 56 del demo (.ticket-screen, sin nav).
 * topbar volver / "Tu receipt sticker" / compartir, y el .ticket-stage con
 * la variante seleccionada.
 */
export function StickerScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const params = useLocalSearchParams<{ orderId?: string; style?: string }>();
  const [styleId, setStyleId] = useState(Number(params.style ?? 0));

  const order = useMemo(() => {
    if (params.orderId) {
      return (
        flow.clientOrders.find((item) => item.summary.id === params.orderId) ??
        flow.createdOrder ??
        getSampleOrder()
      );
    }
    return flow.createdOrder ?? getSampleOrder();
  }, [flow.clientOrders, flow.createdOrder, params.orderId]);

  const stickerData = useMemo(
    () => ({
      ...orderToStickerData(order),
      destinationLabel: destinationDisplayLabel(
        order.summary.destination,
        order.summary.space?.name ?? null,
      ),
      paymentLabel: PAYMENT_LABELS[order.summary.paymentMethod],
    }),
    [order],
  );

  return (
    <View style={styles.root}>
      <View
        style={[
          styles.header,
          { paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md) },
        ]}
      >
        <TopBar
          title="Tu receipt sticker"
          left={
            <IconButton
              name="chevron-back"
              accessibilityLabel="Volver"
              onPress={() => router.back()}
            />
          }
          right={
            <IconButton name="share-outline" accessibilityLabel="Compartir receipt sticker" />
          }
        />

        {/* Selector de variante (el demo usa una pantalla por estilo) */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chips}
        >
          {STICKER_STYLES.map((style) => (
            <PhysicalPress
              key={style.id}
              style={[styles.chip, styleId === style.id ? styles.chipActive : null]}
              onPress={() => setStyleId(style.id)}
            >
              <Text
                style={[styles.chipText, styleId === style.id ? styles.chipTextActive : null]}
              >
                {style.label}
              </Text>
            </PhysicalPress>
          ))}
        </ScrollView>
      </View>

      {/* .ticket-stage { padding:10; align-items:flex-start; scroll } */}
      <ScrollView
        style={styles.stage}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.stageContent,
          { paddingBottom: spacing.screenBottomNoNav + insets.bottom },
        ]}
      >
        <StickerStyleContent styleId={styleId} order={stickerData} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  header: { paddingHorizontal: spacing.screen },

  // .chips { gap:8; padding:0 20 4 }
  chips: { gap: 8, paddingBottom: 4 },
  chip: {
    height: 36,
    paddingHorizontal: 14,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipActive: { backgroundColor: colors.ink },
  chipText: { fontFamily, fontSize: 12, fontWeight: weight.bold, color: colors.ink },
  chipTextActive: { color: colors.paper },

  stage: { flex: 1, marginTop: spacing.md },
  stageContent: { paddingHorizontal: 10, paddingTop: 10 },
});
