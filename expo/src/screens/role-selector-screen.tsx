import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, PrimaryButton, SectionHead, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

interface RoleSelectorScreenProps {
  testOnlyMode: boolean;
  onTestOnlyModeChange: (enabled: boolean) => void;
  onStudentSelected: () => void;
  onCashierSelected: () => void;
  onKitchenSelected: () => void;
  onWaiterSelected: () => void;
  onAdminSelected?: () => void;
  entering?: boolean;
  enterError?: string | null;
}

/**
 * Pantalla 01 del demo (portada, sin nav).
 * .hero en accent con marca de agua, .section-head "Elige una vista" y el
 * .role-grid de cinco tarjetas con su color por posicion.
 */
export function RoleSelectorScreen({
  testOnlyMode,
  onTestOnlyModeChange,
  onStudentSelected,
  onCashierSelected,
  onKitchenSelected,
  onWaiterSelected,
  onAdminSelected,
  entering = false,
  enterError = null,
}: RoleSelectorScreenProps) {
  const insets = useSafeAreaInsets();

  const roles = [
    {
      icon: '☻',
      title: 'Alumno',
      copy: 'Menú, pedido, seguimiento y saldo.',
      onPress: onStudentSelected,
      tone: { backgroundColor: colors.accent, color: colors.accentInk },
    },
    {
      icon: '▣',
      title: 'Caja',
      copy: 'Cobros, entregas y recargas.',
      onPress: onCashierSelected,
      tone: { backgroundColor: colors.yolk, color: '#28200b' },
    },
    {
      icon: '♨',
      title: 'Cocina',
      copy: 'Comandas y preparación.',
      onPress: onKitchenSelected,
      tone: { backgroundColor: '#262724', color: '#f7f4e9' },
    },
    {
      icon: '⌁',
      title: 'Mesero',
      copy: 'Pedidos listos para mesa.',
      onPress: onWaiterSelected,
      tone: { backgroundColor: colors.coral, color: '#2c100e' },
    },
    {
      icon: '⌘',
      title: 'Administración',
      copy: 'Reportes, menú, promociones e integraciones.',
      onPress: onAdminSelected ?? (() => router.push('/(ops)/admin')),
      tone: { backgroundColor: colors.paper2, color: colors.ink },
      wide: true,
    },
  ];

  return (
    <ScrollView
      style={styles.root}
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
        title="Vaiinilla"
        left={
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>VA</Text>
          </View>
        }
        right={<IconButton name="contrast-outline" accessibilityLabel="Cambiar tema" />}
      />

      {/* .hero { radius:34; padding:24; min-height:208; accent; justify-end } */}
      <View style={styles.hero}>
        <Text style={styles.heroWatermark}>V</Text>
        <Text style={styles.heroEyebrow}>Comedor conectado</Text>
        <Text style={styles.heroTitle}>Come mejor. Espera menos.</Text>
        <Text style={styles.heroCopy}>
          Una sola demo para pedir, cobrar, preparar, entregar y administrar.
        </Text>
        <View style={styles.heroActions}>
          <PrimaryButton
            label="Entrar como alumno"
            onPress={onStudentSelected}
            disabled={entering}
          />
        </View>
      </View>

      {enterError ? <Text style={styles.enterError}>{enterError}</Text> : null}

      <SectionHead title="Elige una vista" actionLabel="5 roles" />

      {/* .role-grid { gap:11 } */}
      <View style={styles.roleGrid}>
        {roles.map((role) => (
          <PhysicalPress
            key={role.title}
            style={[
              styles.roleCard,
              { backgroundColor: role.tone.backgroundColor },
              role.wide ? styles.roleCardWide : null,
            ]}
            onPress={role.onPress}
            disabled={entering}
          >
            <Text style={[styles.roleIcon, { color: role.tone.color }]}>{role.icon}</Text>
            <View>
              <Text style={[styles.roleTitle, { color: role.tone.color }]}>{role.title}</Text>
              <Text style={[styles.roleCopy, { color: role.tone.color }]}>{role.copy}</Text>
            </View>
          </PhysicalPress>
        ))}
      </View>

      {/* Controles propios de la app (no existen en el demo web) */}
      <View style={styles.testRow}>
        <View style={styles.flex}>
          <Text style={styles.testTitle}>Modo solo pruebas</Text>
          <Text style={styles.testCopy}>Usa fixtures locales, sin Firebase.</Text>
        </View>
        <Switch
          value={testOnlyMode}
          onValueChange={onTestOnlyModeChange}
          trackColor={{ false: colors.paper2, true: colors.accent }}
          thumbColor={colors.paper}
          disabled={entering}
        />
      </View>

      <PhysicalPress
        style={styles.galleryButton}
        disabled={entering}
        onPress={() => {
          onTestOnlyModeChange(true);
          router.push('/demo/gallery');
        }}
      >
        <Text style={styles.galleryTitle}>Ver todas las fases</Text>
        <Text style={styles.gallerySubtitle}>
          Salta a cualquier pantalla con fixtures locales.
        </Text>
      </PhysicalPress>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, minWidth: 0 },
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  // .avatar { 42x42; radius:15; ink }
  avatar: {
    width: 42,
    height: 42,
    borderRadius: 15,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.paper },

  // .hero
  hero: {
    borderRadius: radius.hero,
    padding: 24,
    minHeight: 208,
    backgroundColor: colors.accent,
    justifyContent: 'flex-end',
    overflow: 'hidden',
  },
  // .hero-watermark { right:-5; top:-38; 180px; opacity:.09 }
  heroWatermark: {
    position: 'absolute',
    right: -5,
    top: -38,
    fontFamily,
    fontSize: 180,
    fontWeight: weight.black,
    color: colors.accentInk,
    opacity: 0.09,
  },
  heroEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: colors.accentInk,
    opacity: 0.72,
  },
  // .display { 34px; line-height:.98; letter-spacing:-.055em; 950 }
  heroTitle: {
    ...typography.display,
    color: colors.accentInk,
    marginTop: 6,
  },
  // .hero-copy { max-width:28ch; margin-top:10; opacity:.72 }
  heroCopy: {
    fontFamily,
    fontSize: 14,
    lineHeight: 14 * 1.5,
    color: colors.accentInk,
    opacity: 0.72,
    maxWidth: 280,
    marginTop: 10,
  },
  heroActions: { flexDirection: 'row', gap: 8, marginTop: 20 },

  enterError: {
    fontFamily,
    fontSize: 12,
    color: colors.coral,
    marginTop: spacing.md,
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

  testRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginTop: 22,
    paddingVertical: 12,
  },
  testTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  testCopy: { fontFamily, fontSize: 11, color: colors.muted, marginTop: 2 },

  galleryButton: {
    borderRadius: radius.lineItem,
    backgroundColor: colors.paper2,
    padding: 16,
  },
  galleryTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  gallerySubtitle: { fontFamily, fontSize: 11, color: colors.muted, marginTop: 3 },
});
