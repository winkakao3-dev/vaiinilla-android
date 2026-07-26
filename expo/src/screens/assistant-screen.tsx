import { router } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { ProductImage } from '@/components/product-image';
import { Chip, IconButton, SectionHead, TopBar } from '@/components/ui';
import { filterByChip } from '@/domain/assistant-local-replies';
import { moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { useStudentNav } from '@/hooks/use-student-nav';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/** Chips exactos de las pantallas 09 / 10 / 11 del demo. */
const CHIPS = ['Rápido y llenador', 'Menos de $60', 'Algo ligero', 'Combo con bebida'];

/**
 * Pantallas 09 / 10 / 11 del demo.
 * .assistant-hero en tinta con la mascota y los chips dentro,
 * seguido de .section-head y la lista .recommendations.
 */
export function AssistantScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const nav = useStudentNav('assistant');
  const [activeChip, setActiveChip] = useState(CHIPS[0]);

  const products = flow.catalog?.products ?? [];
  const recommendations = useMemo(
    () => filterByChip(activeChip, products),
    [activeChip, products],
  );

  return (
    <View style={styles.root}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.body,
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingBottom: spacing.screenBottom + insets.bottom,
          },
        ]}
      >
        <TopBar
          title="Asistente Vaiinilla"
          right={
            <IconButton
              name="notifications-outline"
              onPress={() => router.push('/(student)/assistant-chat')}
              accessibilityLabel="Notificaciones"
            />
          }
        />

        {/* .assistant-hero { #171817; radius:32; padding:24 } */}
        <View style={styles.assistantHero}>
          <Text style={styles.heroEyebrow}>Pide sin pensarlo tanto</Text>
          <Mascot />
          <Text style={styles.heroTitle}>¿Qué necesitas hoy?</Text>

          {/* .chips dentro del hero, margin-top:14 */}
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.heroChips}
          >
            {CHIPS.map((chip) => (
              <Chip
                key={chip}
                label={chip}
                active={activeChip === chip}
                onPress={() => setActiveChip(chip)}
              />
            ))}
          </ScrollView>
        </View>

        <SectionHead title="Te recomendamos" actionLabel="Según tu elección" />

        {/* .recommendations { gap:10 } */}
        <View style={styles.recommendations}>
          {recommendations.map((item) => (
            <PhysicalPress
              key={`${item.productId ?? item.name}`}
              style={styles.recommendation}
              onPress={() => {
                if (item.productId !== null) {
                  flow.openProduct(item.productId);
                  router.push('/(student)/menu');
                }
              }}
            >
              <ProductImage
                imageUrl={item.imageUrl}
                style={styles.recommendationImage}
                accessibilityLabel={item.name}
              />
              <View style={styles.recommendationCopy}>
                <Text style={styles.recommendationTitle}>{item.name}</Text>
                <Text style={styles.recommendationMeta}>{item.meta}</Text>
              </View>
              <Text style={styles.recommendationPrice}>{moneyLabel(item.price)}</Text>
            </PhysicalPress>
          ))}
        </View>

        <PhysicalPress
          style={styles.chatLink}
          onPress={() => router.push('/(student)/assistant-chat')}
        >
          <Text style={styles.chatLinkText}>Abrir chat con el asistente</Text>
        </PhysicalPress>
      </ScrollView>

      <BottomNav {...nav} />
    </View>
  );
}

/**
 * .mascot-svg del demo (92x82): triángulo accent, ojos #1b2410 y sonrisa.
 * El proyecto no tiene react-native-svg, así que se arma con Views.
 */
function Mascot() {
  return (
    <View style={styles.mascot}>
      <View style={styles.mascotTriangle} />
      <View style={styles.mascotFace}>
        <View style={styles.mascotEyes}>
          <View style={styles.mascotEye} />
          <View style={styles.mascotEye} />
        </View>
        <View style={styles.mascotSmile} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  // .assistant-hero
  assistantHero: {
    backgroundColor: colors.ink,
    borderRadius: radius.assistantHero,
    padding: 24,
    overflow: 'hidden',
  },
  heroEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: '#9fa19a',
  },
  // .headline centrado dentro del hero
  heroTitle: {
    ...typography.headline,
    color: '#f6f1e5',
    textAlign: 'center',
  },
  heroChips: { gap: 8, marginTop: 14, paddingBottom: 2 },

  // .mascot-svg { 92x82; margin:10 auto 12 }
  mascot: {
    width: 92,
    height: 82,
    marginTop: 10,
    marginBottom: 12,
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  mascotTriangle: {
    position: 'absolute',
    top: 0,
    width: 0,
    height: 0,
    borderLeftWidth: 46,
    borderRightWidth: 46,
    borderBottomWidth: 82,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderBottomColor: colors.accent,
  },
  mascotFace: { alignItems: 'center', paddingBottom: 14 },
  mascotEyes: { flexDirection: 'row', gap: 14 },
  mascotEye: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: '#1b2410',
  },
  // sonrisa: media circunferencia con trazo 3 como en el SVG
  mascotSmile: {
    width: 26,
    height: 13,
    marginTop: 6,
    borderBottomWidth: 3,
    borderLeftWidth: 3,
    borderRightWidth: 3,
    borderColor: '#1b2410',
    borderBottomLeftRadius: 13,
    borderBottomRightRadius: 13,
  },

  // .recommendations { gap:10 } .recommendation { gap:12; paper-2; radius:22; padding:9 }
  recommendations: { gap: 10 },
  recommendation: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.paper2,
    borderRadius: radius.lineItem,
    padding: 9,
  },
  recommendationImage: { width: 68, height: 68, borderRadius: 17 },
  recommendationCopy: { flex: 1, minWidth: 0 },
  recommendationTitle: {
    fontFamily,
    fontSize: 14,
    fontWeight: weight.black,
    color: colors.ink,
  },
  recommendationMeta: { fontFamily, fontSize: 11, color: colors.muted, marginTop: 3 },
  recommendationPrice: {
    fontFamily,
    fontSize: 15,
    fontWeight: weight.black,
    color: colors.ink,
    paddingRight: 8,
  },

  chatLink: { marginTop: 18, alignItems: 'center', paddingVertical: 8 },
  chatLinkText: {
    fontFamily,
    fontSize: 12,
    fontWeight: weight.bold,
    color: colors.muted,
  },
});
