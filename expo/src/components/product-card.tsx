import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PhysicalPress } from '@/components/physical-press';
import { ProductImage } from '@/components/product-image';
import { productUnitPreview } from '@/domain/contract-rules';
import { moneyLabel, type Product } from '@/domain/models';
import { colors } from '@/theme/colors';
import { radius } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

interface ProductCardProps {
  product: Product;
  onPress: () => void;
  /** `.product-card .new` — badge amarillo arriba a la izquierda. */
  badge?: string;
}

/**
 * Replica de `.product-card` del demo:
 * radius 28, fondo --paper-2, foto con aspect-ratio 1/1.08,
 * info con padding 13/13/15, titulo 15px y precio 22px peso 950.
 */
export function ProductCard({ product, onPress, badge }: ProductCardProps) {
  const preview = productUnitPreview(product, []);

  return (
    <PhysicalPress scale="card" style={styles.card} onPress={onPress}>
      <View style={styles.photo}>
        <ProductImage
          imageUrl={product.imageUrl}
          style={styles.image}
          accessibilityLabel={product.name}
        />
        {badge ? (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{badge}</Text>
          </View>
        ) : null}
      </View>
      <View style={styles.info}>
        <Text style={typography.cardTitle} numberOfLines={2}>
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
    minWidth: 0,
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    overflow: 'hidden',
  },
  photo: {
    width: '100%',
    aspectRatio: 1 / 1.08,
    backgroundColor: '#d7d9c6',
    overflow: 'hidden',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  badge: {
    position: 'absolute',
    top: 10,
    left: 10,
    backgroundColor: colors.yolk,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 10,
  },
  badgeText: {
    fontFamily,
    fontSize: 9,
    fontWeight: weight.black,
    color: '#251d08',
  },
  info: {
    paddingHorizontal: 13,
    paddingTop: 13,
    paddingBottom: 15,
  },
  meta: {
    fontFamily,
    fontSize: 11,
    color: colors.muted,
    marginTop: 6,
    fontWeight: weight.medium,
  },
  price: {
    ...typography.price,
    marginTop: 9,
  },
});
