import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { IconButton, MiniCard, SectionHead, Tab, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

interface ReportData {
  sales: string;
  orders: string;
  average: string;
  cashShare: string;
  repeatShare: string;
  bars: Array<{ day: string; height: number }>;
  top: Array<{ name: string; orders: string; total: string }>;
}

/** Pantallas 43, 44 y 45: la misma vista con el rango cambiado. */
const REPORTS: Record<string, ReportData> = {
  Hoy: {
    sales: '$410',
    orders: '7',
    average: '$59',
    cashShare: '43%',
    repeatShare: '31%',
    bars: [
      { day: 'L', height: 35 },
      { day: 'M', height: 50 },
      { day: 'X', height: 46 },
      { day: 'J', height: 75 },
      { day: 'V', height: 68 },
      { day: 'S', height: 84 },
      { day: 'D', height: 71 },
    ],
    top: [
      { name: 'Waffle de la casa', orders: '12 pedidos', total: '$660' },
      { name: 'Torta de jamón', orders: '10 pedidos', total: '$450' },
      { name: 'Quesadillas (2)', orders: '8 pedidos', total: '$320' },
      { name: 'Agua de jamaica', orders: '6 pedidos', total: '$108' },
      { name: 'Vaso de fruta', orders: '4 pedidos', total: '$120' },
    ],
  },
  '7 días': {
    sales: '$2 870',
    orders: '49',
    average: '$58',
    cashShare: '46%',
    repeatShare: '38%',
    bars: [
      { day: 'L', height: 42 },
      { day: 'M', height: 61 },
      { day: 'X', height: 55 },
      { day: 'J', height: 80 },
      { day: 'V', height: 74 },
      { day: 'S', height: 92 },
      { day: 'D', height: 66 },
    ],
    top: [
      { name: 'Waffle de la casa', orders: '84 pedidos', total: '$4 620' },
      { name: 'Burrito norteño', orders: '71 pedidos', total: '$4 544' },
      { name: 'Torta de jamón', orders: '63 pedidos', total: '$2 835' },
      { name: 'Quesadillas (2)', orders: '52 pedidos', total: '$2 080' },
      { name: 'Agua de jamaica', orders: '44 pedidos', total: '$792' },
    ],
  },
  '30 días': {
    sales: '$11 240',
    orders: '196',
    average: '$57',
    cashShare: '41%',
    repeatShare: '44%',
    bars: [
      { day: 'S1', height: 48 },
      { day: 'S2', height: 63 },
      { day: 'S3', height: 71 },
      { day: 'S4', height: 88 },
      { day: 'S5', height: 59 },
      { day: 'S6', height: 77 },
      { day: 'S7', height: 82 },
    ],
    top: [
      { name: 'Waffle de la casa', orders: '318 pedidos', total: '$17 490' },
      { name: 'Burrito norteño', orders: '284 pedidos', total: '$18 176' },
      { name: 'Montado norteño', orders: '212 pedidos', total: '$16 536' },
      { name: 'Torta de jamón', orders: '198 pedidos', total: '$8 910' },
      { name: 'Quesadillas (2)', orders: '176 pedidos', total: '$7 040' },
    ],
  },
};

const RANGES = ['Hoy', '7 días', '30 días'];

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
 * Pantallas 43 a 45 del demo.
 * .tabs de rango, .kpi-grid, .mini-grid, .chart y la lista de mas vendidos
 * con filas .movement numeradas.
 */
export function AdminReportsScreen() {
  const insets = useSafeAreaInsets();
  const [range, setRange] = useState('Hoy');
  const data = REPORTS[range] ?? REPORTS.Hoy;

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
          title="Reportes"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        {/* .tabs */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tabs}
        >
          {RANGES.map((label) => (
            <Tab
              key={label}
              label={label}
              active={range === label}
              onPress={() => setRange(label)}
            />
          ))}
        </ScrollView>

        {/* .kpi-grid */}
        <View style={styles.kpiGrid}>
          <Kpi value={data.sales} label="Ventas" />
          <Kpi value={data.orders} label="Pedidos" />
          <Kpi value={data.average} label="Ticket prom." />
        </View>

        <View style={styles.spacer12} />

        {/* .mini-grid */}
        <View style={styles.miniGrid}>
          <MiniCard value={data.cashShare} label="Pago en efectivo" />
          <MiniCard value={data.repeatShare} label="Clientes que repiten" />
        </View>

        <SectionHead title="Ritmo de ventas" actionLabel="Exportar CSV" onAction={() => {}} />

        {/* .chart */}
        <View style={styles.chart}>
          {data.bars.map((bar, index) => (
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

        <SectionHead title="Más vendidos" />

        {/* .movement con el numero de posicion en el icono */}
        <View>
          {data.top.map((item, index) => (
            <View key={item.name} style={styles.movement}>
              <View style={styles.movementIcon}>
                <Text style={styles.movementIconText}>{index + 1}</Text>
              </View>
              <View style={styles.movementCopy}>
                <Text style={styles.movementTitle}>{item.name}</Text>
                <Text style={styles.movementMeta}>{item.orders}</Text>
              </View>
              <Text style={styles.movementAmount}>{item.total}</Text>
            </View>
          ))}
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

  tabs: { gap: 8, paddingBottom: 4, marginBottom: 16 },

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

  miniGrid: { flexDirection: 'row', gap: 10 },
  spacer12: { height: 12 },

  // .chart
  chart: {
    height: 160,
    backgroundColor: colors.paper2,
    borderRadius: 26,
    padding: 20,
    paddingBottom: 32,
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 9,
  },
  barColumn: { flex: 1, height: '100%', justifyContent: 'flex-end' },
  bar: {
    width: '100%',
    minHeight: 14,
    borderTopLeftRadius: 10,
    borderTopRightRadius: 10,
    borderBottomLeftRadius: 4,
    borderBottomRightRadius: 4,
  },
  barLabel: {
    position: 'absolute',
    bottom: -18,
    alignSelf: 'center',
    fontFamily,
    fontSize: 8,
    color: colors.muted,
  },

  // .movement
  movement: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 13,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  movementIcon: {
    width: 34,
    height: 34,
    borderRadius: 12,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  movementIconText: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  movementCopy: { flex: 1 },
  movementTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  movementMeta: { fontFamily, fontSize: 10, color: colors.muted, marginTop: 2 },
  movementAmount: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
});
