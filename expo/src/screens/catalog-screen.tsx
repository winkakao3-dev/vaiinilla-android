import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { ProductCard } from '@/components/product-card';
import { moneyLabel } from '@/domain/models';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { useStudentNav } from '@/hooks/use-student-nav';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function CatalogScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const nav = useStudentNav('menu');

  if (flow.loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator color={colors.ink} />
      </View>
    );
  }

  if (flow.errorMessage) {
    return (
      <View style={styles.centered}>
        <Text style={styles.error}>{flow.errorMessage}</Text>
        <PhysicalPress style={styles.retry} onPress={() => void flow.loadCatalog()}>
          <Text style={styles.retryText}>Reintentar</Text>
        </PhysicalPress>
      </View>
    );
  }

  const categories = flow.catalog?.categories ?? [];

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + spacing.lg, paddingBottom: 140 },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.hero}>
          <Text style={styles.greeting}>Hola Dani</Text>
          <Text style={styles.headline}>¿Qué se te antoja?</Text>
        </View>

        <View style={styles.search}>
          <Ionicons name="search-outline" size={18} color={colors.muted} />
          <TextInput
            value={flow.searchQuery}
            onChangeText={flow.setSearchQuery}
            placeholder="Buscar burritos, bebidas…"
            placeholderTextColor={colors.muted}
            style={styles.searchInput}
          />
        </View>

        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
          <PhysicalPress
            style={[styles.chip, flow.selectedCategoryId === null && styles.chipActive]}
            onPress={() => flow.setSelectedCategoryId(null)}
          >
            <Text style={[styles.chipText, flow.selectedCategoryId === null && styles.chipTextActive]}>
              Todo
            </Text>
          </PhysicalPress>
          {categories.map((category) => {
            const active = flow.selectedCategoryId === category.id;
            return (
              <PhysicalPress
                key={category.id}
                style={[styles.chip, active && styles.chipActive]}
                onPress={() => flow.setSelectedCategoryId(category.id)}
              >
                <Text style={[styles.chipText, active && styles.chipTextActive]}>{category.name}</Text>
              </PhysicalPress>
            );
          })}
        </ScrollView>

        <View style={styles.quickRow}>
          <PhysicalPress style={styles.quickCard} onPress={() => flow.openProduct(103)}>
            <Text style={styles.quickEyebrow}>Combo rápido</Text>
            <Text style={styles.quickTitle}>Burrito + jamaica</Text>
          </PhysicalPress>
          <PhysicalPress style={styles.quickCardAlt} onPress={() => flow.openProduct(102)}>
            <Text style={styles.quickEyebrow}>Dulce</Text>
            <Text style={styles.quickTitle}>Waffle de la casa</Text>
          </PhysicalPress>
        </View>

        <FlatList
          data={flow.filteredProducts}
          keyExtractor={(item) => String(item.id)}
          numColumns={2}
          scrollEnabled={false}
          columnWrapperStyle={styles.gridRow}
          contentContainerStyle={styles.grid}
          renderItem={({ item }) => (
            <ProductCard product={item} onPress={() => flow.openProduct(item.id)} />
          )}
          ListEmptyComponent={
            <Text style={styles.empty}>No encontramos productos con ese filtro.</Text>
          }
        />
      </ScrollView>

      <BottomNav {...nav} />

      <ProductSheet />
    </View>
  );
}

function ProductSheet() {
  const flow = useOrderFlow();
  const product = flow.selectedProduct;
  if (!product) {
    return null;
  }

  return (
    <Modal visible transparent animationType="slide" onRequestClose={flow.closeProduct}>
      <Pressable style={styles.sheetBackdrop} onPress={flow.closeProduct} />
      <View style={styles.sheet}>
        <View style={styles.sheetHandle} />
        <Text style={styles.sheetTitle}>{product.name}</Text>
        <Text style={styles.sheetDescription}>{product.description}</Text>

        {product.optionGroups.map((group) => (
          <View key={group.id} style={styles.optionGroup}>
            <Text style={styles.optionTitle}>
              {group.name} · {group.minimumSelections}-{group.maximumSelections}
            </Text>
            <View style={styles.optionList}>
              {group.options.map((option) => {
                const selected = flow.selectedOptionIds.includes(option.id);
                return (
                  <PhysicalPress
                    key={option.id}
                    style={[styles.optionChip, selected && styles.optionChipActive]}
                    onPress={() => flow.toggleOption(group.id, option.id)}
                  >
                    <Text style={[styles.optionText, selected && styles.optionTextActive]}>
                      {option.name}
                      {option.extraPrice !== '0.00' ? ` +${moneyLabel(option.extraPrice)}` : ''}
                    </Text>
                  </PhysicalPress>
                );
              })}
            </View>
          </View>
        ))}

        <View style={styles.qtyRow}>
          <PhysicalPress
            style={styles.qtyButton}
            onPress={() => flow.setSelectedQuantity(flow.selectedQuantity - 1)}
          >
            <Text style={styles.qtyButtonText}>−</Text>
          </PhysicalPress>
          <Text style={styles.qtyValue}>{flow.selectedQuantity}</Text>
          <PhysicalPress
            style={styles.qtyButton}
            onPress={() => flow.setSelectedQuantity(flow.selectedQuantity + 1)}
          >
            <Text style={styles.qtyButtonText}>+</Text>
          </PhysicalPress>
        </View>

        <PhysicalPress
          style={[styles.addButton, !flow.isSelectedProductValid && styles.addButtonDisabled]}
          disabled={!flow.isSelectedProductValid}
          onPress={flow.addSelectedProductToCart}
        >
          <Text style={styles.addButtonText}>
            Agregar · {moneyLabel(flow.selectedProductPreviewTotal)}
          </Text>
        </PhysicalPress>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen },
  hero: { gap: 6, marginBottom: spacing.lg },
  greeting: { fontFamily: fonts.body, fontSize: 14, color: colors.muted },
  headline: { fontFamily: fonts.displayBlack, fontSize: 30, color: colors.ink },
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
    marginBottom: spacing.md,
  },
  searchInput: { flex: 1, fontFamily: fonts.body, fontSize: 15, color: colors.ink },
  chips: { gap: spacing.sm, paddingBottom: spacing.md },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  chipActive: { backgroundColor: colors.ink, borderColor: colors.ink },
  chipText: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.ink },
  chipTextActive: { color: colors.paper },
  quickRow: { flexDirection: 'row', gap: spacing.md, marginBottom: spacing.lg },
  quickCard: {
    flex: 1,
    backgroundColor: colors.coral,
    borderRadius: radius.card,
    padding: spacing.lg,
    minHeight: 96,
  },
  quickCardAlt: {
    flex: 1,
    backgroundColor: colors.yolk,
    borderRadius: radius.card,
    padding: spacing.lg,
    minHeight: 96,
  },
  quickEyebrow: { fontFamily: fonts.bodyBold, fontSize: 11, color: colors.ink, opacity: 0.7 },
  quickTitle: { fontFamily: fonts.display, fontSize: 18, color: colors.ink, marginTop: 6 },
  grid: { gap: spacing.md },
  gridRow: { gap: spacing.md },
  empty: { fontFamily: fonts.body, color: colors.muted, textAlign: 'center', paddingVertical: spacing.xl },
  error: { fontFamily: fonts.body, color: colors.coral, marginBottom: spacing.md },
  retry: {
    backgroundColor: colors.ink,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.md,
    borderRadius: radius.button,
  },
  retryText: { color: colors.paper, fontFamily: fonts.bodyBold },
  sheetBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: {
    backgroundColor: colors.paper,
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    padding: spacing.screen,
    paddingBottom: spacing.xxl,
    gap: spacing.md,
  },
  sheetHandle: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.line,
    marginBottom: spacing.sm,
  },
  sheetTitle: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.ink },
  sheetDescription: { fontFamily: fonts.body, fontSize: 14, lineHeight: 20, color: colors.ink2 },
  optionGroup: { gap: spacing.sm },
  optionTitle: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted },
  optionList: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  optionChip: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  optionChipActive: { backgroundColor: colors.accent, borderColor: colors.accent },
  optionText: { fontFamily: fonts.bodyMedium, fontSize: 13, color: colors.ink },
  optionTextActive: { color: colors.accentInk },
  qtyRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.lg },
  qtyButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyButtonText: { fontFamily: fonts.bodyBold, fontSize: 22, color: colors.ink },
  qtyValue: { fontFamily: fonts.displayBlack, fontSize: 24, color: colors.ink, minWidth: 28, textAlign: 'center' },
  addButton: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
  },
  addButtonDisabled: { opacity: 0.45 },
  addButtonText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
});
