import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

interface MenuRow {
  id: string;
  name: string;
  category: string;
  price: string;
  active: boolean;
}

/** `.admin-row` de la pantalla 46 del demo. */
const INITIAL_ROWS: MenuRow[] = [
  { id: 'waffle', name: 'Waffle de la casa', category: 'Comida', price: '55', active: true },
  { id: 'torta', name: 'Torta de jamón', category: 'Comida', price: '45', active: true },
  { id: 'quesa', name: 'Quesadillas (2)', category: 'Comida', price: '40', active: true },
  { id: 'jamaica', name: 'Agua de jamaica', category: 'Bebidas', price: '18', active: false },
  { id: 'fruta', name: 'Vaso de fruta', category: 'Snacks', price: '30', active: true },
  { id: 'burrito', name: 'Burrito norteño', category: 'Comida', price: '64', active: true },
  { id: 'montado', name: 'Montado norteño', category: 'Comida', price: '78', active: true },
  { id: 'machaca', name: 'Burrito de machaca', category: 'Comida', price: '68', active: true },
  { id: 'barbacoa', name: 'Burrito de barbacoa', category: 'Comida', price: '72', active: true },
  { id: 'frijol', name: 'Burrito de frijol con queso', category: 'Comida', price: '56', active: true },
];

/**
 * Pantalla 46 del demo.
 * Filas .admin-row con precio editable de 76 de ancho y .toggle activo/agotado.
 */
export function AdminMenuScreen() {
  const insets = useSafeAreaInsets();
  const [rows, setRows] = useState<MenuRow[]>(INITIAL_ROWS);

  const setPrice = (id: string, price: string) => {
    setRows((current) => current.map((row) => (row.id === id ? { ...row, price } : row)));
  };

  const toggleRow = (id: string) => {
    setRows((current) =>
      current.map((row) => (row.id === id ? { ...row, active: !row.active } : row)),
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
          title="Menú y precios"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
          right={<IconButton name="add" accessibilityLabel="Nuevo producto" />}
        />

        <Text style={styles.intro}>
          Los cambios se reflejan en el menú del alumno dentro de esta sesión.
        </Text>

        <View style={styles.spacer12} />

        {rows.map((row) => (
          // .admin-row { padding:12 0; borde inferior line }
          <View key={row.id} style={styles.adminRow}>
            <View style={styles.rowCopy}>
              <Text style={styles.rowTitle}>{row.name}</Text>
              <Text style={styles.rowCategory}>{row.category}</Text>
            </View>

            {/* .admin-row input { width:76; radius:12; padding:10 } */}
            <TextInput
              value={row.price}
              onChangeText={(value) => setPrice(row.id, value)}
              keyboardType="number-pad"
              style={styles.priceInput}
              accessibilityLabel={`Precio de ${row.name}`}
            />

            <PhysicalPress
              style={[styles.toggle, row.active ? styles.toggleOn : null]}
              onPress={() => toggleRow(row.id)}
            >
              <Text style={[styles.toggleText, row.active ? styles.toggleTextOn : null]}>
                {row.active ? 'ACTIVO' : 'AGOTADO'}
              </Text>
            </PhysicalPress>
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },
  intro: { ...typography.body },
  spacer12: { height: 12 },

  adminRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  rowCopy: { flex: 1, minWidth: 0 },
  rowTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  rowCategory: { fontFamily, fontSize: 10, color: colors.muted, marginTop: 2 },

  priceInput: {
    width: 76,
    borderRadius: 12,
    padding: 10,
    backgroundColor: colors.paper2,
    fontFamily,
    fontSize: 13,
    fontWeight: weight.bold,
    color: colors.ink,
    textAlign: 'right',
  },

  toggle: {
    width: 80,
    height: 34,
    borderRadius: 12,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  toggleOn: { backgroundColor: colors.accent },
  toggleText: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.ink },
  toggleTextOn: { color: colors.accentInk },
});
