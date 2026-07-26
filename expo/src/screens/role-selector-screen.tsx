import { router } from 'expo-router';
import React from 'react';
import {
  Image,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { DataModeChip } from '@/components/data-mode-chip';
import { PhysicalPress } from '@/components/physical-press';
import { LOGO_IMAGE } from '@/components/product-image';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface RoleSelectorScreenProps {
  testOnlyMode: boolean;
  onTestOnlyModeChange: (enabled: boolean) => void;
  onStudentSelected: () => void;
  onCashierSelected: () => void;
  onKitchenSelected: () => void;
  onWaiterSelected: () => void;
  entering?: boolean;
  enterError?: string | null;
}

export function RoleSelectorScreen({
  testOnlyMode,
  onTestOnlyModeChange,
  onStudentSelected,
  onCashierSelected,
  onKitchenSelected,
  onWaiterSelected,
  entering = false,
  enterError = null,
}: RoleSelectorScreenProps) {
  const insets = useSafeAreaInsets();

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.xl }]}
    >
      <View style={styles.hero}>
        <Image source={LOGO_IMAGE} style={styles.logo} accessibilityLabel="Vaiinilla" />
        <Text style={styles.brand}>Vaiinilla</Text>
        <Text style={styles.tagline}>Cafetería universitaria · pedido digital</Text>
        <DataModeChip />
      </View>

      <View style={styles.testCard}>
        <View style={styles.testCopy}>
          <Text style={styles.testTitle}>Solo pruebas</Text>
          <Text style={styles.testSubtitle}>Fixtures locales · sin backend</Text>
        </View>
        <Switch
          value={testOnlyMode}
          onValueChange={onTestOnlyModeChange}
          trackColor={{ false: colors.paper2, true: colors.accent }}
          thumbColor={colors.white}
          disabled={entering}
        />
      </View>

      {enterError ? <Text style={styles.enterError}>{enterError}</Text> : null}

      <PhysicalPress
        style={styles.galleryButton}
        disabled={entering}
        onPress={() => {
          onTestOnlyModeChange(true);
          router.push('/demo/gallery');
        }}
      >
        <Text style={styles.galleryTitle}>Ver todas las fases</Text>
        <Text style={styles.gallerySubtitle}>Salta a cualquier pantalla con fixtures locales.</Text>
      </PhysicalPress>

      <Text style={styles.section}>Entrar como</Text>

      <PhysicalPress
        style={[styles.roleCard, styles.roleStudent]}
        onPress={onStudentSelected}
        disabled={entering}
      >
        <Text style={styles.roleEyebrow}>Alumno</Text>
        <Text style={styles.roleTitle}>Menú, carrito y pedido</Text>
        <Text style={styles.roleBody}>Flujo estudiante con catálogo, asistente, cartera y tracking.</Text>
      </PhysicalPress>

      <PhysicalPress style={styles.roleCard} onPress={onCashierSelected} disabled={entering}>
        <Text style={styles.roleEyebrow}>Cajero</Text>
        <Text style={styles.roleTitle}>Caja y cobro</Text>
        <Text style={styles.roleBody}>Lista por cobrar y cobro en efectivo.</Text>
      </PhysicalPress>

      <PhysicalPress style={styles.roleCard} onPress={onKitchenSelected} disabled={entering}>
        <Text style={styles.roleEyebrow}>Cocina</Text>
        <Text style={styles.roleTitle}>Preparación</Text>
        <Text style={styles.roleBody}>Avanza pedidos cobrados a preparando/listo.</Text>
      </PhysicalPress>

      <PhysicalPress style={styles.roleCard} onPress={onWaiterSelected} disabled={entering}>
        <Text style={styles.roleEyebrow}>Mesero</Text>
        <Text style={styles.roleTitle}>Entrega en mesa</Text>
        <Text style={styles.roleBody}>Marca pedidos listos como entregados.</Text>
      </PhysicalPress>

      <PhysicalPress
        style={styles.loginLink}
        onPress={() => router.push('/login')}
        disabled={entering}
      >
        <Text style={styles.loginLinkText}>Iniciar sesión (Firebase seed)</Text>
      </PhysicalPress>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.paper,
  },
  content: {
    paddingHorizontal: spacing.screen,
    paddingBottom: spacing.xxl,
    gap: spacing.md,
  },
  hero: {
    alignItems: 'center',
    gap: spacing.sm,
    marginBottom: spacing.md,
    paddingVertical: spacing.lg,
  },
  logo: {
    width: 72,
    height: 72,
    borderRadius: 22,
  },
  brand: {
    fontFamily: fonts.displayBlack,
    fontSize: 32,
    color: colors.ink,
    letterSpacing: -0.5,
  },
  tagline: {
    fontFamily: fonts.body,
    fontSize: 14,
    color: colors.muted,
    textAlign: 'center',
  },
  testCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.lg,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
  },
  enterError: {
    fontFamily: fonts.body,
    fontSize: 14,
    color: colors.coral,
  },
  testCopy: {
    flex: 1,
    gap: 4,
  },
  testTitle: {
    fontFamily: fonts.bodyBold,
    fontSize: 16,
    color: colors.ink,
  },
  testSubtitle: {
    fontFamily: fonts.body,
    fontSize: 12,
    color: colors.muted,
  },
  galleryButton: {
    alignItems: 'center',
    paddingVertical: spacing.sm,
  },
  galleryTitle: {
    fontFamily: fonts.bodyBold,
    fontSize: 14,
    color: colors.ink,
  },
  gallerySubtitle: {
    fontFamily: fonts.body,
    fontSize: 12,
    color: colors.muted,
    marginTop: 4,
    textAlign: 'center',
  },
  section: {
    fontFamily: fonts.bodyBold,
    fontSize: 13,
    color: colors.muted,
    marginTop: spacing.sm,
  },
  roleCard: {
    backgroundColor: colors.paper2,
    borderRadius: radius.card,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 6,
  },
  roleStudent: {
    borderColor: colors.accent,
    backgroundColor: '#edf3d8',
  },
  roleEyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1,
    textTransform: 'uppercase',
    color: colors.muted,
  },
  roleTitle: {
    fontFamily: fonts.display,
    fontSize: 22,
    color: colors.ink,
  },
  roleBody: {
    fontFamily: fonts.body,
    fontSize: 14,
    lineHeight: 20,
    color: colors.ink2,
  },
  loginLink: {
    alignItems: 'center',
    paddingVertical: spacing.lg,
  },
  loginLinkText: {
    fontFamily: fonts.bodyBold,
    fontSize: 13,
    color: colors.ink,
    textDecorationLine: 'underline',
  },
});
