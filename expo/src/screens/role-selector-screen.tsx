import { router } from 'expo-router';
import React from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { DATA_SOURCE } from '@/core/config';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface RoleSelectorScreenProps {
  testOnlyMode: boolean;
  onTestOnlyModeChange: (enabled: boolean) => void;
  onStudentSelected: () => void;
}

export function RoleSelectorScreen({
  testOnlyMode,
  onTestOnlyModeChange,
  onStudentSelected,
}: RoleSelectorScreenProps) {
  const insets = useSafeAreaInsets();

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.xl }]}
    >
      <View style={styles.header}>
        <View style={styles.logo}>
          <Text style={styles.logoText}>VA</Text>
        </View>
        <Text style={styles.brand}>Vaiinilla</Text>
      </View>

      <View style={styles.testCard}>
        <View style={styles.testCopy}>
          <Text style={styles.testTitle}>Solo pruebas</Text>
          <Text style={styles.testSubtitle}>
            Fixtures locales · sin backend ({DATA_SOURCE})
          </Text>
        </View>
        <Switch
          value={testOnlyMode}
          onValueChange={onTestOnlyModeChange}
          trackColor={{ false: colors.paper2, true: colors.accent }}
          thumbColor={colors.white}
        />
      </View>

      <PhysicalPress
        style={styles.galleryButton}
        onPress={() =>
          Alert.alert('Ver todas las fases', 'La galería demo llegará en una fase posterior.')
        }
      >
        <Text style={styles.galleryTitle}>Ver todas las fases</Text>
        <Text style={styles.gallerySubtitle}>Salta a cualquier pantalla con fixtures locales.</Text>
      </PhysicalPress>

      <Text style={styles.section}>Entrar como</Text>

      <PhysicalPress style={[styles.roleCard, styles.roleStudent]} onPress={onStudentSelected}>
        <Text style={styles.roleEyebrow}>Alumno</Text>
        <Text style={styles.roleTitle}>Menú, carrito y pedido</Text>
        <Text style={styles.roleBody}>Flujo estudiante con catálogo y checkout en efectivo.</Text>
      </PhysicalPress>

      <PhysicalPress
        style={styles.roleCard}
        onPress={() => Alert.alert('Próximamente', 'Roles operativos llegarán en fases posteriores.')}
      >
        <Text style={styles.roleEyebrow}>Cajero</Text>
        <Text style={styles.roleTitle}>Caja y cobro</Text>
        <Text style={styles.roleBody}>Operación REMOTE en roadmap.</Text>
      </PhysicalPress>

      <PhysicalPress
        style={styles.roleCard}
        onPress={() => Alert.alert('Próximamente', 'Roles operativos llegarán en fases posteriores.')}
      >
        <Text style={styles.roleEyebrow}>Cocina / Mesero</Text>
        <Text style={styles.roleTitle}>Preparación y entrega</Text>
        <Text style={styles.roleBody}>Pantallas operativas en roadmap.</Text>
      </PhysicalPress>
      <PhysicalPress style={styles.loginLink} onPress={() => router.push('/login')}>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  logo: {
    width: 42,
    height: 42,
    borderRadius: 15,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoText: {
    color: colors.paper,
    fontFamily: fonts.displayBlack,
    fontSize: 13,
  },
  brand: {
    fontFamily: fonts.displayBlack,
    fontSize: 16,
    color: colors.ink,
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
