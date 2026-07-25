import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PAYMENT_LABELS, moneyLabel } from '@/domain/models';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export const STICKER_STYLES = [
  { id: 0, label: 'Editorial' },
  { id: 1, label: 'Core' },
  { id: 2, label: 'Limited' },
  { id: 3, label: 'Breakfast' },
  { id: 4, label: 'QR Live' },
  { id: 5, label: 'Térmico' },
] as const;

export interface StickerOrderData {
  folio: number;
  total: string;
  productName: string;
  paymentLabel: string;
  destinationLabel: string;
  date: string;
}

interface StickerStyleContentProps {
  styleId: number;
  order: StickerOrderData;
}

export function StickerStyleContent({ styleId, order }: StickerStyleContentProps) {
  switch (styleId) {
    case 0:
      return <EditorialSticker order={order} />;
    case 1:
      return <CoreSticker order={order} />;
    case 2:
      return <LimitedSticker order={order} />;
    case 3:
      return <BreakfastSticker order={order} />;
    case 4:
      return <QrLiveSticker order={order} />;
    default:
      return <ThermalSticker order={order} />;
  }
}

function EditorialSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.editorial]}>
      <Text style={styles.editorialTitle}>ANTOJO</Text>
      <Text style={styles.editorialFolio}>#{order.folio}</Text>
      <Text style={styles.editorialProduct}>{order.productName}</Text>
      <Text style={styles.editorialMeta}>
        {order.paymentLabel} · {order.destinationLabel}
      </Text>
      <Text style={styles.editorialTotal}>{moneyLabel(order.total)}</Text>
    </View>
  );
}

function CoreSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.core]}>
      <Text style={styles.coreBrand}>VAIINILLA</Text>
      <Text style={styles.coreFolio}>#{order.folio}</Text>
      <View style={styles.corePill}>
        <Text style={styles.corePillText}>{order.paymentLabel}</Text>
      </View>
      <Text style={styles.coreProduct}>{order.productName}</Text>
      <Text style={styles.coreTotal}>{moneyLabel(order.total)}</Text>
    </View>
  );
}

function LimitedSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.limited]}>
      <Text style={styles.limitedTag}>LIMITED DROP</Text>
      <Text style={styles.limitedFolio}>#{order.folio}</Text>
      <Text style={styles.limitedProduct}>{order.productName}</Text>
      <Text style={styles.limitedTotal}>{moneyLabel(order.total)}</Text>
    </View>
  );
}

function BreakfastSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.breakfast]}>
      <Text style={styles.breakfastTitle}>BREAKFAST CLUB</Text>
      <Text style={styles.breakfastFolio}>#{order.folio}</Text>
      <Text style={styles.breakfastProduct}>{order.productName}</Text>
      <Text style={styles.breakfastMeta}>{order.destinationLabel}</Text>
    </View>
  );
}

function QrLiveSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.qr]}>
      <View style={styles.qrBox}>
        <Text style={styles.qrGlyph}>▦</Text>
      </View>
      <View style={styles.qrCopy}>
        <Text style={styles.qrFolio}>#{order.folio}</Text>
        <Text style={styles.qrProduct}>{order.productName}</Text>
        <Text style={styles.qrMeta}>{PAYMENT_LABELS.efectivo === order.paymentLabel ? order.paymentLabel : order.paymentLabel}</Text>
      </View>
    </View>
  );
}

function ThermalSticker({ order }: { order: StickerOrderData }) {
  return (
    <View style={[styles.base, styles.thermal]}>
      <Text style={styles.thermalMono}>VAIINILLA CAFETERIA</Text>
      <Text style={styles.thermalMono}>FOLIO {order.folio}</Text>
      <Text style={styles.thermalMono}>{order.productName.toUpperCase()}</Text>
      <Text style={styles.thermalMono}>TOTAL {moneyLabel(order.total)}</Text>
      <Text style={styles.thermalMono}>{order.date}</Text>
    </View>
  );
}

export function orderToStickerData(order: {
  summary: { folio: number; total: string; paymentMethod: keyof typeof PAYMENT_LABELS };
  items: Array<{ productName: string }>;
  summaryDestination?: string;
}): StickerOrderData {
  return {
    folio: order.summary.folio,
    total: order.summary.total,
    productName: order.items[0]?.productName ?? 'Pedido',
    paymentLabel: PAYMENT_LABELS[order.summary.paymentMethod],
    destinationLabel: 'Para llevar',
    date: '23/07/26',
  };
}

const styles = StyleSheet.create({
  base: {
    borderRadius: radius.card,
    padding: spacing.xl,
    minHeight: 220,
    justifyContent: 'center',
    gap: spacing.sm,
  },
  editorial: { backgroundColor: '#171817' },
  editorialTitle: { fontFamily: fonts.displayBlack, fontSize: 42, color: '#f4f1e7', letterSpacing: -1 },
  editorialFolio: { fontFamily: fonts.bodyBold, fontSize: 14, color: '#a9aaa4' },
  editorialProduct: { fontFamily: fonts.display, fontSize: 22, color: '#f4f1e7' },
  editorialMeta: { fontFamily: fonts.body, fontSize: 13, color: '#a9aaa4' },
  editorialTotal: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.accent },
  core: { backgroundColor: colors.accent, alignItems: 'center' },
  coreBrand: { fontFamily: fonts.bodyBold, fontSize: 12, letterSpacing: 2, color: colors.accentInk },
  coreFolio: { fontFamily: fonts.displayBlack, fontSize: 36, color: colors.accentInk },
  corePill: { backgroundColor: colors.ink, borderRadius: 999, paddingHorizontal: spacing.md, paddingVertical: 4 },
  corePillText: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.paper },
  coreProduct: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.accentInk },
  coreTotal: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.accentInk },
  limited: { backgroundColor: colors.coral },
  limitedTag: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.paper, letterSpacing: 1.2 },
  limitedFolio: { fontFamily: fonts.displayBlack, fontSize: 34, color: colors.paper },
  limitedProduct: { fontFamily: fonts.bodyBold, fontSize: 18, color: colors.paper },
  limitedTotal: { fontFamily: fonts.displayBlack, fontSize: 26, color: colors.paper },
  breakfast: { backgroundColor: colors.yolk },
  breakfastTitle: { fontFamily: fonts.bodyBold, fontSize: 12, color: colors.ink, letterSpacing: 1 },
  breakfastFolio: { fontFamily: fonts.displayBlack, fontSize: 32, color: colors.ink },
  breakfastProduct: { fontFamily: fonts.bodyBold, fontSize: 18, color: colors.ink },
  breakfastMeta: { fontFamily: fonts.body, fontSize: 13, color: colors.ink2 },
  qr: { backgroundColor: colors.paper2, flexDirection: 'row', gap: spacing.lg, alignItems: 'center' },
  qrBox: {
    width: 88,
    height: 88,
    borderRadius: 12,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  qrGlyph: { fontSize: 42, color: colors.paper },
  qrCopy: { flex: 1, gap: 4 },
  qrFolio: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.ink },
  qrProduct: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
  qrMeta: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  thermal: { backgroundColor: '#fffef8', borderWidth: 1, borderColor: colors.line },
  thermalMono: { fontFamily: fonts.body, fontSize: 13, color: colors.ink, letterSpacing: 0.5 },
});
