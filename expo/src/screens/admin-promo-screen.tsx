import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { Chip, IconButton, PrimaryButton, SectionHead, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

const DAYS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

/**
 * Pantallas 47 y 48 del demo.
 * .task-card.yellow con el estado, campos .field, .chips de dias activos,
 * .segment de activa/pausada y el boton .primary.wide.accent.
 */
export function AdminPromoScreen() {
  const insets = useSafeAreaInsets();
  const [name, setName] = useState('Happy hour');
  const [rate, setRate] = useState('15');
  const [start, setStart] = useState('13:00');
  const [end, setEnd] = useState('15:00');
  const [activeDays, setActiveDays] = useState<string[]>(['Lun', 'Mar', 'Mié', 'Jue', 'Vie']);
  const [active, setActive] = useState(true);

  const toggleDay = (day: string) => {
    setActiveDays((current) =>
      current.includes(day) ? current.filter((item) => item !== day) : [...current, day],
    );
  };

  return (
    <View style={styles.root}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={[
          styles.body,
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingBottom: spacing.screenBottomNoNav + insets.bottom,
          },
        ]}
      >
        <TopBar
          title="Promoción"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        {/* .task-card.yellow con el estado actual */}
        <View style={styles.taskCard}>
          <View style={styles.taskHead}>
            <View style={styles.flex}>
              <Text style={styles.taskEyebrow}>Estado actual</Text>
              <Text style={styles.taskTitle}>{name}</Text>
            </View>
            <View style={styles.statusBadge}>
              <Text style={styles.statusBadgeText}>{active ? 'ACTIVA' : 'PAUSADA'}</Text>
            </View>
          </View>
          <View style={styles.taskMeta}>
            <Text style={styles.taskMetaText}>{rate}% cashback</Text>
            <Text style={styles.taskMetaText}>
              {start}–{end}
            </Text>
          </View>
        </View>

        <Field label="Nombre" value={name} onChangeText={setName} />
        <View style={styles.spacer12} />
        <Field
          label="Cashback (%)"
          value={rate}
          onChangeText={setRate}
          keyboardType="number-pad"
        />
        <View style={styles.spacer12} />

        {/* .mini-grid con las dos horas */}
        <View style={styles.miniGrid}>
          <Field label="Inicio" value={start} onChangeText={setStart} style={styles.flex} />
          <Field label="Fin" value={end} onChangeText={setEnd} style={styles.flex} />
        </View>

        <SectionHead title="Días activos" />

        {/* .chips.promo-days */}
        <View style={styles.chips}>
          {DAYS.map((day) => (
            <Chip
              key={day}
              label={day}
              active={activeDays.includes(day)}
              onPress={() => toggleDay(day)}
            />
          ))}
        </View>

        <View style={styles.spacer16} />

        {/* .segment { paper-2; padding:5; radius:18 } */}
        <View style={styles.segment}>
          {[
            { label: 'Activa', value: true },
            { label: 'Pausada', value: false },
          ].map((option) => (
            <PhysicalPress
              key={option.label}
              style={[styles.segmentButton, active === option.value ? styles.segmentActive : null]}
              onPress={() => setActive(option.value)}
            >
              <Text
                style={[
                  styles.segmentText,
                  active === option.value ? styles.segmentTextActive : null,
                ]}
              >
                {option.label}
              </Text>
            </PhysicalPress>
          ))}
        </View>

        <View style={styles.spacer16} />

        <PrimaryButton variant="accent" label="Guardar promoción" onPress={() => router.back()} />
      </ScrollView>
    </View>
  );
}

/** `.field` con label 11px/850 e input de borde 1px --line y radio 17. */
function Field({
  label,
  value,
  onChangeText,
  keyboardType,
  style,
}: {
  label: string;
  value: string;
  onChangeText: (next: string) => void;
  keyboardType?: 'default' | 'number-pad';
  style?: object;
}) {
  return (
    <View style={[styles.field, style]}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        keyboardType={keyboardType ?? 'default'}
        style={styles.input}
        placeholderTextColor={colors.muted}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, minWidth: 0 },
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  // .task-card.yellow
  taskCard: {
    borderRadius: 30,
    padding: 20,
    backgroundColor: colors.yolk,
    marginBottom: 12,
  },
  taskHead: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  taskEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: '#28200b',
    opacity: 0.72,
  },
  taskTitle: {
    fontFamily,
    fontSize: 22,
    lineHeight: 22 * 0.98,
    fontWeight: weight.black,
    color: '#28200b',
    marginTop: 4,
  },
  statusBadge: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.45)',
  },
  statusBadgeText: {
    fontFamily,
    fontSize: 10,
    fontWeight: weight.black,
    letterSpacing: 0.8,
    color: '#28200b',
  },
  taskMeta: { flexDirection: 'row', gap: 14, flexWrap: 'wrap', marginTop: 18 },
  taskMetaText: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    color: '#28200b',
    opacity: 0.72,
  },

  // .field
  field: { gap: 7 },
  fieldLabel: { fontFamily, fontSize: 11, fontWeight: weight.bold, color: colors.muted },
  input: {
    width: '100%',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 17,
    paddingVertical: 13,
    paddingHorizontal: 14,
    fontFamily,
    fontSize: 13,
    color: colors.ink,
  },
  miniGrid: { flexDirection: 'row', gap: 10 },

  // .chips { gap:8 }
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },

  // .segment
  segment: {
    flexDirection: 'row',
    backgroundColor: colors.paper2,
    padding: 5,
    borderRadius: 18,
    gap: 4,
  },
  segmentButton: {
    flex: 1,
    height: 42,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  segmentActive: { backgroundColor: colors.ink },
  segmentText: { fontFamily, fontSize: 12, fontWeight: weight.bold, color: colors.ink },
  segmentTextActive: { color: colors.paper },

  spacer12: { height: 12 },
  spacer16: { height: 16 },
});
