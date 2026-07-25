import { router } from 'expo-router';
import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { ProductCard } from '@/components/product-card';
import { ProductImage } from '@/components/product-image';
import { filterByChip } from '@/domain/assistant-local-replies';
import { moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { useStudentNav } from '@/hooks/use-student-nav';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

const CHIPS = ['Menos de $60', 'Algo ligero', 'Combo con bebida', 'Para compartir'];

export function AssistantScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const nav = useStudentNav('assistant');
  const products = flow.catalog?.products ?? [];

  const recommendations = useMemo(
    () => filterByChip('Para compartir', products),
    [products],
  );

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: 140 },
        ]}
      >
        <Text style={styles.eyebrow}>Pide sin pensarlo tanto</Text>
        <Text style={styles.title}>¿Qué necesitas hoy?</Text>
        <Text style={styles.subtitle}>
          El asistente te ayuda a elegir rápido según tu antojo, presupuesto o restricciones.
        </Text>

        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
          {CHIPS.map((chip) => (
            <PhysicalPress
              key={chip}
              style={styles.chip}
              onPress={() => router.push({ pathname: '/(student)/assistant-chat', params: { chip } })}
            >
              <Text style={styles.chipText}>{chip}</Text>
            </PhysicalPress>
          ))}
        </ScrollView>

        <PhysicalPress
          style={styles.chatCard}
          onPress={() => router.push('/(student)/assistant-chat')}
        >
          <Text style={styles.chatEyebrow}>Chat</Text>
          <Text style={styles.chatTitle}>Pregúntame lo que quieras</Text>
          <Text style={styles.chatBody}>Recomendaciones, alérgenos y combos en segundos.</Text>
        </PhysicalPress>

        <Text style={styles.section}>Recomendado para ti</Text>
        <View style={styles.grid}>
          {recommendations.map((item) => (
            <PhysicalPress
              key={item.name}
              style={styles.recoCard}
              onPress={() => item.productId && flow.openProduct(item.productId)}
            >
              <View style={styles.recoThumb}>
                <ProductImage imageUrl={item.imageUrl} style={styles.recoImage} accessibilityLabel={item.name} />
              </View>
              <Text style={styles.recoName} numberOfLines={2}>
                {item.name}
              </Text>
              <Text style={styles.recoMeta} numberOfLines={1}>
                {item.meta}
              </Text>
              <Text style={styles.recoPrice}>{moneyLabel(item.price)}</Text>
            </PhysicalPress>
          ))}
        </View>

        <View style={styles.grid}>
          {flow.filteredProducts.slice(0, 2).map((product) => (
            <ProductCard key={product.id} product={product} onPress={() => flow.openProduct(product.id)} />
          ))}
        </View>
      </ScrollView>

      <BottomNav {...nav} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, gap: spacing.md },
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.1,
    textTransform: 'uppercase',
    color: colors.muted,
  },
  title: { fontFamily: fonts.displayBlack, fontSize: 30, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 15, lineHeight: 22, color: colors.muted },
  chips: { gap: spacing.sm, paddingVertical: spacing.sm },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  chipText: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.ink },
  chatCard: {
    backgroundColor: colors.ink,
    borderRadius: radius.card,
    padding: spacing.xl,
    gap: 6,
  },
  chatEyebrow: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.accent, letterSpacing: 1 },
  chatTitle: { fontFamily: fonts.display, fontSize: 24, color: colors.paper },
  chatBody: { fontFamily: fonts.body, fontSize: 14, color: colors.paper, opacity: 0.75 },
  section: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md },
  recoCard: {
    flexBasis: '48%',
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 4,
    overflow: 'hidden',
  },
  recoThumb: {
    height: 72,
    borderRadius: 10,
    overflow: 'hidden',
    backgroundColor: colors.line,
    marginBottom: 4,
  },
  recoImage: { width: '100%', height: '100%' },
  recoName: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink, minHeight: 36 },
  recoMeta: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  recoPrice: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink, marginTop: 2 },
});
