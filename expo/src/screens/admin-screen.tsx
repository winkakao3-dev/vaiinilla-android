import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, SectionHead, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

/** `.chart .bar` de la pantalla 42: alto en porcentaje y etiqueta debajo. */
const WEEK_BARS = [
  { day: 'L', height: 32 },
  { day: 'M', height: 56 },
  { day: 'X', height: 41 },
  { day: 'J', height: 78 },
  { day: 'V', height: 62 },
  { day: 'S', height: 91 },
  { day: 'D', height: 68 },
];

/** `.role-grid` de gestion. El color depende de la posicion, como en el CSS. */
const MANAGE_CARDS = [
  { icon: '▥', title: 'Reportes', copy: 'Ventas y productos.', route: '/(ops)/admin-reports' },
  { icon: '☰', title: 'Menú', copy: 'Precios y disponibilidad.', route: '/(ops)/admin-menu' },
  { icon: '%', title: 'Promoción', copy: 'Cashback y horario.', route: '/(ops)/admin-promo' },
  {
    icon: '⌁',
    title: 'Integraciones',
    copy: 'Servicios conectados.',
    route: '/(ops)/admin-integrations',
  },
  { icon: '●', title: 'Servicio en mesa', copy: 'Activo.', route: null },
] as const;

/** Colores por posicion: .role-card:nth-child(1..5) */
const CARD_TONES = [
  { backgroundColor: colors.accent, color: colors.accentInk },
  { backgroundColor: colors.yolk, color: '#28200b' },
  { backgroundColor: '#262724', color: '#f7f4e9' },
  { backgroundColor: colors.coral, color: '#2c100e' },
  { backgroundColor: colors.paper2, color: colors.ink },
];

/** Colores de barra: .bar, :nth-child(2n) yolk, :nth-child(3n) coral */
function barColor(index: number): string {
  const position = index + 1;
  if (position % 3 === 0) {
    return colors.coral;
  }
  if (position % 2 === 0) {
    return colors.yolk;
  }
  return colors.accent;
}

/**
 * Pantalla 42 del demo.
 * .kpi-grid, la grafica semanal .chart con .bar y el .role-grid de gestion.
 */
export function AdminHomeScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.root}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.body,
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingBottom: spacing.screenBottomNoNav + insets.bottom,
          },
        ]}
      >
        <TopBar
          title="Administración"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        {/* .kpi-grid { gap:8 } */}
        <View style={styles.kpiGrid}>
          <Kpi value="$1240" label="Ventas" />
          <Kpi value="0" label="Activos" />
          <Kpi value="19" label="Entregados" />
        </View>

        <SectionHead
          title="Esta semana"
          actionLabel="Ver reporte"
          onAction={() => router.push('/(ops)/admin-reports')}
        />

        {/* .chart { height:160; radius:26; paper-2; padding:20; align-items:end; gap:9 } */}
        <View style={styles.chart}>
          {WEEK_BARS.map((bar, index) => (
            <View key={bar.day} style={styles.barColumn}>
              <View
                style={[
                  styles.bar,
                  { height: `${bar.height}%`, backgroundColor: barColor(index) },
                ]}
              />
              <Text style={styles.barLabel}>{bar.day}</Text>
            </View>
          ))}
        </View>

        <SectionHead title="Gestionar" />

        {/* .role-grid { gap:11 } con tarjetas al 50% menos 5.5 */}
        <View style={styles.roleGrid}>
          {MANAGE_CARDS.map((card, index) => {
            const tone = CARD_TONES[index] ?? CARD_TONES[4];
            const isFullWidth = index === 4;
            return (
              <PhysicalPress
                key={card.title}
                style={[
                  styles.roleCard,
                  { backgroundColor: tone.backgroundColor },
                  isFullWidth ? styles.roleCardWide : null,
                ]}
                onPress={card.route ? () => router.push(card.route as never) : undefined}
              >
                <Text style={[styles.roleIcon, { color: tone.color }]}>{card.icon}</Text>
                <View>
                  <Text style={[styles.roleTitle, { color: tone.color }]}>{card.title}</Text>
                  <Text style={[styles.roleCopy, { color: tone.color }]}>{card.copy}</Text>
                </View>
              </PhysicalPress>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
}

/** `.kpi { radius:21; paper-2; padding:14 }` */
function Kpi({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.kpi}>
      <Text style={styles.kpiValue} numberOfLines={1}>
        {value}
      </Text>
      <Text style={styles.kpiLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  kpiGrid: { flexDirection: 'row', gap: 8 },
  kpi: {
    flex: 1,
    minWidth: 0,
    borderRadius: 21,
    backgroundColor: colors.paper2,
    padding: 14,
  },
  kpiValue: { fontFamily, fontSize: 21, fontWeight: weight.black, color: colors.ink },
  kpiLabel: { fontFamily, fontSize: 9, color: colors.muted, letterSpacing: 0.72, marginTop: 2 },

  // .chart
  chart: {
    height: 160,
    backgroundColor: colors.paper2,
    borderRadius: 26,
    padding: 20,
    paddingBottom: 32, // hueco para las etiquetas de .bar span
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 9,
  },
  barColumn: { flex: 1, height: '100%', justifyContent: 'flex-end', alignItems: 'stretch' },
  // .bar { radius:10 10 4 4; min-height:14 }
  bar: {
    width: '100%',
    minHeight: 14,
    borderTopLeftRadius: 10,
    borderTopRightRadius: 10,
    borderBottomLeftRadius: 4,
    borderBottomRightRadius: 4,
  },
  // .bar span { bottom:-18; 8px; muted }
  barLabel: {
    position: 'absolute',
    bottom: -18,
    alignSelf: 'center',
    fontFamily,
    fontSize: 8,
    color: colors.muted,
  },

  // .role-grid / .role-card
  roleGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 11 },
  roleCard: {
    width: '48%',
    flexGrow: 1,
    borderRadius: radius.card,
    padding: 17,
    minHeight: 148,
    justifyContent: 'space-between',
    overflow: 'hidden',
  },
  roleCardWide: { width: '100%' },
  roleIcon: { fontFamily, fontSize: 29 },
  roleTitle: { fontFamily, fontSize: 19, lineHeight: 19, fontWeight: weight.black },
  roleCopy: { fontFamily, fontSize: 12, lineHeight: 12 * 1.3, marginTop: 5, opacity: 0.65 },
});
