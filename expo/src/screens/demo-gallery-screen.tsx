import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import {
  seedCartEmpty,
  seedCartWithFirstProduct,
  seedCatalogActiveOrder,
  seedCatalogCleared,
  seedCatalogEmptySearch,
  seedCatalogProductSheet,
  seedCheckout,
  seedConfirmation,
  seedTrackingEmpty,
  seedTrackingOrder,
} from '@/data/demo-gallery-seeder';
import { getSampleOrder, seedMockOrder } from '@/data/order-repository';
import { PhysicalPress } from '@/components/physical-press';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface GalleryItem {
  title: string;
  subtitle: string;
  onPress: () => void;
}

export function DemoGalleryScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const products = flow.catalog?.products ?? [];

  const api = {
    setSearchQuery: flow.setSearchQuery,
    setSelectedCategoryId: flow.setSelectedCategoryId,
    openProduct: flow.openProduct,
    closeProduct: flow.closeProduct,
    clearCart: flow.clearCart,
    setCartLines: flow.setCartLines,
    setCheckoutDestination: flow.setCheckoutDestination,
    setCheckoutPayment: flow.setCheckoutPayment,
    setSelectedSpaceId: flow.setSelectedSpaceId,
    setCreatedOrder: flow.setCreatedOrder,
    setClientOrders: flow.setClientOrders,
  };

  const sections: Array<{ title: string; items: GalleryItem[] }> = [
    {
      title: 'Catálogo',
      items: [
        {
          title: 'Menú completo',
          subtitle: 'Catálogo limpio',
          onPress: () => {
            seedCatalogCleared(api);
            router.push('/(student)/menu');
          },
        },
        {
          title: 'Búsqueda vacía',
          subtitle: 'Sin resultados',
          onPress: () => {
            seedCatalogEmptySearch(api);
            router.push('/(student)/menu');
          },
        },
        {
          title: 'Ficha de producto',
          subtitle: 'Sheet abierto',
          onPress: () => {
            seedCatalogProductSheet(api, products);
            router.push('/(student)/menu');
          },
        },
      ],
    },
    {
      title: 'Asistente',
      items: [
        {
          title: 'Hub asistente',
          subtitle: 'Recomendaciones',
          onPress: () => router.push('/(student)/assistant'),
        },
        {
          title: 'Chat',
          subtitle: 'Sugerencias locales',
          onPress: () => router.push('/(student)/assistant-chat'),
        },
      ],
    },
    {
      title: 'Carrito y checkout',
      items: [
        {
          title: 'Carrito vacío',
          subtitle: 'Sin líneas',
          onPress: () => {
            seedCartEmpty(api);
            router.push('/(student)/cart');
          },
        },
        {
          title: 'Carrito con producto',
          subtitle: 'Primera opción',
          onPress: () => {
            seedCartWithFirstProduct(api, products);
            router.push('/(student)/cart');
          },
        },
        {
          title: 'Checkout efectivo',
          subtitle: 'Para llevar',
          onPress: () => {
            seedCheckout(api, products, 'para_llevar', 'efectivo');
            router.push('/(student)/cart');
          },
        },
        {
          title: 'Checkout mesa + saldo',
          subtitle: 'Mesa 4',
          onPress: () => {
            seedCheckout(api, products, 'en_espacio', 'saldo');
            router.push('/(student)/cart');
          },
        },
        {
          title: 'Checkout tarjeta',
          subtitle: 'Para llevar',
          onPress: () => {
            seedCheckout(api, products, 'para_llevar', 'tarjeta');
            router.push('/(student)/cart');
          },
        },
        {
          title: 'Confirmación efectivo',
          subtitle: 'Pase por cobrar',
          onPress: () => {
            seedConfirmation(api, 'efectivo');
            router.push('/(student)/confirmation');
          },
        },
        {
          title: 'Confirmación saldo',
          subtitle: 'Comanda enviada',
          onPress: () => {
            seedConfirmation(api, 'saldo');
            router.push('/(student)/confirmation');
          },
        },
      ],
    },
    {
      title: 'Pedidos',
      items: [
        {
          title: 'Tracking vacío',
          subtitle: 'Sin pedidos',
          onPress: () => {
            seedTrackingEmpty(api);
            router.push('/(student)/orders');
          },
        },
        {
          title: 'Tracking activo',
          subtitle: 'Preparando',
          onPress: () => {
            seedTrackingOrder(api, 'preparando', 'efectivo');
            router.push('/(student)/orders');
          },
        },
        {
          title: 'Catálogo + pedido activo',
          subtitle: 'Banner tracking',
          onPress: () => {
            seedCatalogActiveOrder(api, products);
            router.push('/(student)/menu');
          },
        },
      ],
    },
    {
      title: 'Cartera y stickers',
      items: [
        {
          title: 'Cartera',
          subtitle: 'Saldo demo',
          onPress: () => router.push('/(student)/wallet'),
        },
        {
          title: 'Agregar dinero',
          subtitle: 'Recarga MOCK',
          onPress: () => router.push('/(student)/wallet-add-money'),
        },
        {
          title: 'Sticker estilos',
          subtitle: '0–5',
          onPress: () => router.push('/(student)/sticker'),
        },
      ],
    },
    {
      title: 'Operación',
      items: [
        {
          title: 'Caja',
          subtitle: 'Cobrar por cobrar',
          onPress: () => {
            seedTrackingOrder(api, 'por_cobrar', 'efectivo');
            router.push('/(ops)/caja');
          },
        },
        {
          title: 'Cocina',
          subtitle: 'Preparar pedido',
          onPress: () => {
            seedTrackingOrder(api, 'cobrado', 'efectivo');
            router.push('/(ops)/cocina');
          },
        },
        {
          title: 'Mesero',
          subtitle: 'Entregar en mesa',
          onPress: () => {
            const order = getSampleOrder({
              state: 'listo',
              paymentMethod: 'efectivo',
              destination: 'en_espacio',
              spaceId: 704,
            });
            seedMockOrder(order);
            api.setClientOrders([order], order.summary.id);
            router.push('/(ops)/mesero');
          },
        },
      ],
    },
  ];

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.lg }]}
    >
      <PhysicalPress onPress={() => router.back()}>
        <Text style={styles.back}>← Roles</Text>
      </PhysicalPress>
      <Text style={styles.title}>Galería demo</Text>
      <Text style={styles.subtitle}>Solo pruebas · fixtures locales · salta a cualquier pantalla.</Text>

      {sections.map((section) => (
        <View key={section.title} style={styles.section}>
          <Text style={styles.sectionTitle}>{section.title}</Text>
          {section.items.map((item) => (
            <PhysicalPress key={item.title} style={styles.item} onPress={item.onPress}>
              <Text style={styles.itemTitle}>{item.title}</Text>
              <Text style={styles.itemSubtitle}>{item.subtitle}</Text>
            </PhysicalPress>
          ))}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.lg, paddingBottom: spacing.xxl },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 14, color: colors.muted },
  section: { gap: spacing.sm },
  sectionTitle: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, letterSpacing: 0.6 },
  item: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
  },
  itemTitle: { fontFamily: fonts.bodyBold, fontSize: 15, color: colors.ink },
  itemSubtitle: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
});
