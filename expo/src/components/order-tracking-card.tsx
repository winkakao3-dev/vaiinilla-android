import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PhysicalPress } from '@/components/physical-press';
import { destinationDisplayLabel, trackingIndex, TRACKING_TIMELINE } from '@/domain/tracking-timeline';
import type { OrderDetail } from '@/domain/models';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface OrderTrackingCardProps {
  order: OrderDetail;
  onPress?: () => void;
  showEyebrow?: boolean;
}

export function OrderTrackingCard({ order, onPress, showEyebrow = true }: OrderTrackingCardProps) {
  const isReady = order.summary.state === 'listo';
  const cardBg = isReady ? colors.yolk : colors.ink;
  const cardText = isReady ? colors.ink : colors.paper;
  const currentIndex = trackingIndex(order.summary.state);

  const content = (
    <View style={[styles.card, { backgroundColor: cardBg }]}>
      <View style={styles.header}>
        <View>
          {showEyebrow ? <Text style={[styles.eyebrow, { color: cardText }]}>Pedido actual</Text> : null}
          <Text style={[styles.folio, { color: cardText }]}>#{order.summary.folio}</Text>
        </View>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{order.summary.state.replace('_', ' ')}</Text>
        </View>
      </View>

      <Text style={[styles.destination, { color: cardText }]}>
        {destinationDisplayLabel(order.summary.destination, order.summary.space?.name ?? null)}
      </Text>

      <View style={styles.timeline}>
        {TRACKING_TIMELINE.map((step, index) => {
          const active = index <= currentIndex;
          return (
            <View key={step.state} style={styles.step}>
              <View style={[styles.dot, active && styles.dotActive]} />
              <View style={styles.stepCopy}>
                <Text style={[styles.stepTitle, { color: cardText, opacity: active ? 1 : 0.45 }]}>
                  {step.title(order.summary.paymentMethod)}
                </Text>
                <Text style={[styles.stepBody, { color: cardText, opacity: active ? 0.75 : 0.35 }]}>
                  {step.description(order.summary.destination, order.summary.paymentMethod)}
                </Text>
              </View>
            </View>
          );
        })}
      </View>
    </View>
  );

  if (onPress) {
    return (
      <PhysicalPress onPress={onPress} scale="card">
        {content}
      </PhysicalPress>
    );
  }

  return content;
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.card,
    padding: spacing.lg,
    gap: spacing.md,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 0.8,
    opacity: 0.62,
  },
  folio: {
    fontFamily: fonts.displayBlack,
    fontSize: 24,
    marginTop: 4,
  },
  badge: {
    backgroundColor: 'rgba(255,255,255,0.45)',
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  badgeText: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    color: colors.ink,
    textTransform: 'uppercase',
  },
  destination: {
    fontFamily: fonts.body,
    fontSize: 14,
    opacity: 0.8,
  },
  timeline: { gap: spacing.sm, marginTop: spacing.sm },
  step: { flexDirection: 'row', gap: spacing.md },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginTop: 5,
    backgroundColor: 'rgba(255,255,255,0.25)',
  },
  dotActive: { backgroundColor: colors.accent },
  stepCopy: { flex: 1, gap: 2 },
  stepTitle: {
    fontFamily: fonts.bodyBold,
    fontSize: 12,
    letterSpacing: 0.6,
  },
  stepBody: {
    fontFamily: fonts.body,
    fontSize: 12,
    lineHeight: 17,
  },
});
