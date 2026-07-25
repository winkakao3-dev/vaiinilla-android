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
import { destinationDisplayLabel } from '@/domain/tracking-timeline';
import { PAYMENT_LABELS } from '@/domain/models';
import { getSampleOrder } from '@/data/order-repository';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

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
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <PhysicalPress onPress={() => router.back()}>
        <Text style={styles.back}>← Volver</Text>
      </PhysicalPress>
      <Text style={styles.title}>Receipt sticker</Text>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
        {STICKER_STYLES.map((style) => (
          <PhysicalPress
            key={style.id}
            style={[styles.chip, styleId === style.id && styles.chipActive]}
            onPress={() => setStyleId(style.id)}
          >
            <Text style={[styles.chipText, styleId === style.id && styles.chipTextActive]}>
              {style.label}
            </Text>
          </PhysicalPress>
        ))}
      </ScrollView>

      <StickerStyleContent styleId={styleId} order={stickerData} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  chips: { gap: spacing.sm, paddingVertical: spacing.sm },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  chipActive: { backgroundColor: colors.ink, borderColor: colors.ink },
  chipText: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.ink },
  chipTextActive: { color: colors.paper },
});
