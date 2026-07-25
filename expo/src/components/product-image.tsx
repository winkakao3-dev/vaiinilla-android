import React from 'react';
import {
  Image,
  ImageResizeMode,
  ImageSourcePropType,
  ImageStyle,
  StyleProp,
  StyleSheet,
} from 'react-native';

const PRODUCT_IMAGES: Record<string, ImageSourcePropType> = {
  jamaica: require('../../assets/products/jamaica.jpg'),
  burrito_norteno: require('../../assets/products/burrito_norteno.webp'),
  waffle: require('../../assets/products/waffle.jpg'),
  burrito_barbacoa: require('../../assets/products/burrito_barbacoa.webp'),
  burrito_frijol_queso: require('../../assets/products/burrito_frijol_queso.webp'),
  burrito_machaca: require('../../assets/products/burrito_machaca.webp'),
  fruta: require('../../assets/products/fruta.webp'),
  montado_asada: require('../../assets/products/montado_asada.webp'),
  montado_chorizo: require('../../assets/products/montado_chorizo.webp'),
  montado_machaca: require('../../assets/products/montado_machaca.webp'),
  montado_norteno: require('../../assets/products/montado_norteno.webp'),
  quesa: require('../../assets/products/quesa.webp'),
  quesadilla_harina: require('../../assets/products/quesadilla_harina.webp'),
  sincronizada_nortena: require('../../assets/products/sincronizada_nortena.webp'),
  torta: require('../../assets/products/torta.webp'),
};

export const LOGO_IMAGE = require('../../assets/products/logo.webp');

export function productImageKey(imageUrl: string): string {
  const key = imageUrl.replace(/^fixture:\/\//, '');
  return key in PRODUCT_IMAGES ? key : 'waffle';
}

export function productImageSource(imageUrl: string): ImageSourcePropType {
  return PRODUCT_IMAGES[productImageKey(imageUrl)] ?? PRODUCT_IMAGES.waffle;
}

interface ProductImageProps {
  imageUrl: string;
  style?: StyleProp<ImageStyle>;
  contentFit?: 'cover' | 'contain';
  accessibilityLabel?: string;
}

export function ProductImage({
  imageUrl,
  style,
  contentFit = 'cover',
  accessibilityLabel,
}: ProductImageProps) {
  const resizeMode: ImageResizeMode = contentFit === 'contain' ? 'contain' : 'cover';

  return (
    <Image
      source={productImageSource(imageUrl)}
      style={[styles.image, style]}
      resizeMode={resizeMode}
      accessibilityLabel={accessibilityLabel}
    />
  );
}

const styles = StyleSheet.create({
  image: {
    width: '100%',
    height: '100%',
  },
});
