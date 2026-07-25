import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PhysicalPress } from '@/components/physical-press';
import { ProductImage } from '@/components/product-image';
import { moneyLabel, type Product } from '@/domain/models';
import { productUnitPreview } from '@/domain/contract-rules';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface ProductCardProps {
  product: Product;
  onPress: () => void;
}

export function ProductCard({ product, onPress }: ProductCardProps) {
  const preview = productUnitPreview(product, []);

  return (
    <PhysicalPress scale="card" style={styles.card} onPress={onPress}>
      <View style={styles.imageWrap}>
        <ProductImage
          imageUrl={product.imageUrl}
          style={styles.image}
          accessibilityLabel={product.name}
        />
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
  imageWrap: {
    height: 112,
    backgroundColor: colors.line,
    overflow: 'hidden',
  },
  image: {
    width: '100%',
    height: '100%',
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
