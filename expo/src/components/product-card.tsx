import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PhysicalPress } from '@/components/physical-press';
import { moneyLabel, type Product } from '@/domain/models';
import { productUnitPreview } from '@/domain/contract-rules';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

const PRODUCT_COLORS: Record<string, string> = {
  jamaica: '#c94b4b',
  waffle: '#d9a441',
  burrito_norteno: '#8b5a2b',
  burrito_barbacoa: '#6d3b1f',
  burrito_frijol_queso: '#c9a227',
  burrito_machaca: '#a0522d',
};

interface ProductCardProps {
  product: Product;
  onPress: () => void;
}

export function ProductCard({ product, onPress }: ProductCardProps) {
  const imageKey = product.imageUrl.replace('fixture://', '');
  const swatch = PRODUCT_COLORS[imageKey] ?? colors.accentSoft;
  const preview = productUnitPreview(product, []);

  return (
    <PhysicalPress scale="card" style={styles.card} onPress={onPress}>
      <View style={[styles.image, { backgroundColor: swatch }]}>
        <Text style={styles.imageMark}>{product.name.slice(0, 1)}</Text>
      </View>
      <View style={styles.body}>
        <Text style={styles.name} numberOfLines={2}>
          {product.name}
        </Text>
        <Text style={styles.meta}>{product.estimatedTimeMinutes} min</Text>
        <Text style={styles.price}>{moneyLabel(preview)}</Text>
      </View>
    </PhysicalPress>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.line,
  },
  image: {
    height: 112,
    alignItems: 'center',
    justifyContent: 'center',
  },
  imageMark: {
    fontFamily: fonts.displayBlack,
    fontSize: 34,
    color: colors.white,
    opacity: 0.85,
  },
  body: {
    padding: spacing.md,
    gap: 4,
  },
  name: {
    fontFamily: fonts.bodyBold,
    fontSize: 15,
    color: colors.ink,
    minHeight: 40,
  },
  meta: {
    fontFamily: fonts.body,
    fontSize: 12,
    color: colors.muted,
  },
  price: {
    fontFamily: fonts.bodyBold,
    fontSize: 16,
    color: colors.ink,
    marginTop: 2,
  },
});
