import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/** `.integration` de la pantalla 49 del demo. */
const INTEGRATIONS = [
  { id: 'terminal', icon: '▣', title: 'Terminal bancaria', copy: 'Cobros con tarjeta', on: true },
  { id: 'sheets', icon: '⌁', title: 'Google Sheets', copy: 'Exportación de reportes', on: false },
  { id: 'push', icon: '◉', title: 'Notificaciones push', copy: 'Avisos de pedido', on: true },
  { id: 'school', icon: '⌁', title: 'Sistema escolar', copy: 'Matrículas y alumnos', on: false },
  { id: 'books', icon: '⌁', title: 'Contabilidad', copy: 'Cierre y conciliación', on: false },
];

/**
 * Pantalla 49 del demo.
 * Lista de .integration con icono en tinta y .toggle de 80 de ancho.
 */
export function AdminIntegrationsScreen() {
  const insets = useSafeAreaInsets();
  const [states, setStates] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(INTEGRATIONS.map((item) => [item.id, item.on])),
  );

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
          title="Integraciones"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        <Text style={styles.intro}>
          En esta demo los estados son locales. No se envían datos a servicios externos.
        </Text>

        <View style={styles.spacer16} />

        {INTEGRATIONS.map((item) => {
          const on = states[item.id];
          return (
            // .integration { gap:12; paper-2; padding:14; radius:20; margin-bottom:10 }
            <View key={item.id} style={styles.integration}>
              <View style={styles.integrationIcon}>
                <Text style={styles.integrationIconText}>{item.icon}</Text>
              </View>
              <View style={styles.integrationCopy}>
                <Text style={styles.integrationTitle}>{item.title}</Text>
                <Text style={styles.integrationMeta}>{item.copy}</Text>
              </View>
              {/* .toggle { 80x34; radius:12 } / .toggle.on { accent } */}
              <PhysicalPress
                style={[styles.toggle, on ? styles.toggleOn : null]}
                onPress={() =>
                  setStates((current) => ({ ...current, [item.id]: !current[item.id] }))
                }
              >
                <Text style={[styles.toggleText, on ? styles.toggleTextOn : null]}>
                  {on ? 'CONECTADO' : 'CONECTAR'}
                </Text>
              </PhysicalPress>
            </View>
          );
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },
  intro: { ...typography.body },
  spacer16: { height: 16 },

  integration: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.paper2,
    padding: 14,
    borderRadius: 20,
    marginBottom: 10,
  },
  integrationIcon: {
    width: 44,
    height: 44,
    borderRadius: 15,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  integrationIconText: { fontFamily, fontSize: 19, color: colors.paper },
  integrationCopy: { flex: 1, minWidth: 0 },
  integrationTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  integrationMeta: { fontFamily, fontSize: 10, color: colors.muted, marginTop: 2 },

  toggle: {
    width: 80,
    height: 34,
    borderRadius: 12,
    backgroundColor: colors.paper,
    alignItems: 'center',
    justifyContent: 'center',
  },
  toggleOn: { backgroundColor: colors.accent },
  toggleText: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.ink },
  toggleTextOn: { color: colors.accentInk },
});
