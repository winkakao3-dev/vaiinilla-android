import { router } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { IconButton, SectionHead, TopBar } from '@/components/ui';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

/** `.profile-list` de la pantalla 30 del demo. */
const PROFILE_ROWS = [
  { label: 'Matrícula', value: 'UTCH-241087' },
  { label: 'Correo', value: 'dani.alvarez@utch.mx' },
  { label: 'Teléfono', value: '614 555 0187' },
  { label: 'Plantel', value: 'Campus Chihuahua' },
];

/** `.movement` de la seccion "Actividad reciente". */
const MOVEMENTS = [
  { icon: '↗', title: 'Transferencia SPEI', when: 'Hoy, 13:05', amount: '+$100', positive: true },
  {
    icon: '↙',
    title: 'Pedido #3411',
    when: 'Hoy, 11:42',
    amount: '-$42',
    positive: false,
  },
];

/**
 * Pantalla 30 del demo.
 * .wallet-section.account-summary, .profile-list, .task-card.yellow con el
 * identificador escolar y la lista de .movement.
 */
export function WalletAccountScreen() {
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
          title="Mi cuenta"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        {/* .wallet-section.account-summary */}
        <View style={[styles.walletSection, styles.accountSummary]}>
          <View style={styles.accountAvatar}>
            <Text style={styles.accountAvatarText}>DA</Text>
          </View>
          <View style={styles.accountCopy}>
            <Text style={styles.accountName}>Dani</Text>
            <Text style={styles.accountMeta}>Cuenta de estudiante activa</Text>
          </View>
        </View>

        <SectionHead title="Datos de la cuenta" />
        {/* .profile-list { gap:7 } */}
        <View style={styles.profileList}>
          {PROFILE_ROWS.map((row) => (
            <View key={row.label} style={styles.profileRow}>
              <Text style={styles.profileLabel}>{row.label}</Text>
              <Text style={styles.profileValue}>{row.value}</Text>
            </View>
          ))}
        </View>

        <SectionHead title="Código para Caja" />
        {/* .task-card.yellow */}
        <View style={styles.taskCardYellow}>
          <View style={styles.taskHead}>
            <View style={styles.flex}>
              <Text style={styles.taskEyebrow}>Identificador escolar</Text>
              <Text style={styles.taskTitle}>UTCH-241087</Text>
            </View>
            <View style={styles.statusBadge}>
              <Text style={styles.statusBadgeText}>DA</Text>
            </View>
          </View>
          <View style={styles.taskMeta}>
            <Text style={styles.taskMetaText}>Muéstralo para recargas en efectivo</Text>
          </View>
        </View>

        <SectionHead title="Actividad reciente" />
        <View>
          {MOVEMENTS.map((movement) => (
            <View key={movement.title} style={styles.movement}>
              <View style={styles.movementIcon}>
                <Text style={styles.movementIconText}>{movement.icon}</Text>
              </View>
              <View style={styles.movementCopy}>
                <Text style={styles.movementTitle}>{movement.title}</Text>
                <Text style={styles.movementWhen}>{movement.when}</Text>
              </View>
              <Text
                style={[
                  styles.movementAmount,
                  movement.positive ? styles.movementPositive : styles.movementNegative,
                ]}
              >
                {movement.amount}
              </Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, minWidth: 0 },
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  walletSection: {
    borderRadius: radius.lineItem,
    backgroundColor: colors.paper2,
    padding: 11,
    marginBottom: 8,
  },
  accountSummary: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  accountAvatar: {
    width: 44,
    height: 44,
    borderRadius: 15,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  accountAvatarText: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.accentInk },
  accountCopy: { flex: 1, minWidth: 0 },
  accountName: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  accountMeta: { fontFamily, fontSize: 8, lineHeight: 10, color: colors.muted, marginTop: 2 },

  // .profile-list / .profile-row { padding:11 12; radius:16; paper-2 }
  profileList: { gap: 7 },
  profileRow: {
    paddingVertical: 11,
    paddingHorizontal: 12,
    borderRadius: 16,
    backgroundColor: colors.paper2,
  },
  profileLabel: { fontFamily, fontSize: 8, color: colors.muted, marginBottom: 3 },
  profileValue: { fontFamily, fontSize: 10, fontWeight: weight.bold, color: colors.ink },

  // .task-card.yellow { radius:30; padding:20; yolk }
  taskCardYellow: {
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
  // .status-badge { padding:8 10; radius:12; rgba blanco }
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
  movementIconText: { fontFamily, fontSize: 14, color: colors.ink },
  movementCopy: { flex: 1 },
  movementTitle: { fontFamily, fontSize: 13, fontWeight: weight.black, color: colors.ink },
  movementWhen: { fontFamily, fontSize: 10, color: colors.muted, marginTop: 2 },
  movementAmount: { fontFamily, fontSize: 13, fontWeight: weight.black },
  movementPositive: { color: colors.ink },
  movementNegative: { color: colors.coral },
});
