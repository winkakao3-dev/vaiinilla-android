import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useEffect } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomNav } from '@/components/bottom-nav';
import { PhysicalPress } from '@/components/physical-press';
import { PrimaryButton, SectionHead, TopBar } from '@/components/ui';
import { moneyLabel } from '@/domain/models';
import { useStudentNav } from '@/hooks/use-student-nav';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/** `.wallet-actions` de la pantalla 25 del demo. */
const WALLET_ACTIONS: Array<{
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  hint: string;
  route: string;
}> = [
  {
    icon: 'add-circle-outline',
    title: 'Añadir dinero',
    hint: 'Tarjeta o transferencia',
    route: '/(student)/wallet-add-money',
  },
  {
    icon: 'card-outline',
    title: 'Métodos de pago',
    hint: 'Tarjetas y SPEI',
    route: '/(student)/wallet-methods',
  },
  {
    icon: 'person-outline',
    title: 'Mi cuenta',
    hint: 'Perfil y matrícula',
    route: '/(student)/wallet-account',
  },
];

/** `.movement` de la pantalla 25 (datos fijos del demo). */
const MOVEMENTS = [
  { icon: '↗', title: 'Transferencia SPEI', when: 'Hoy, 13:05', amount: '+$100', positive: true },
  {
    icon: '↙',
    title: 'Pedido #3411 · VISA •••• 4242',
    when: 'Hoy, 11:42',
    amount: '-$42',
    positive: false,
  },
];

/** `.sticker-collection` de la pantalla 25. */
const STICKERS = [
  { swatch: '#141414', title: 'Editorial', hint: 'Receipt sticker' },
  { swatch: '#c7f24d', title: 'Core', hint: 'Vaiinilla Core' },
  { swatch: '#ff4d3d', title: 'Edición limitada', hint: 'Serie fuego' },
  { swatch: '#f2e6c8', title: 'Breakfast', hint: 'Vaiinilla AM' },
  { swatch: '#0b0b0b', title: 'QR digital', hint: 'Ticket con código' },
  { swatch: '#ece5d8', title: 'Térmico', hint: 'Ticket de caja' },
];

/**
 * Pantalla 25 del demo.
 * .balance-card en accent con marca de agua, .wallet-actions, .mini-grid,
 * .wallet-section para metodos y cuenta, .movement y .sticker-collection.
 */
export function WalletScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const nav = useStudentNav('wallet');

  useEffect(() => {
    void wallet.refreshWallet();
  }, [wallet.refreshWallet]);

  return (
    <View style={styles.root}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.body,
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingBottom: spacing.screenBottom + insets.bottom,
          },
        ]}
      >
        <TopBar title="Cartera" />

        {/* .balance-card { accent; radius:32; padding:24 } */}
        <View style={styles.balanceCard}>
          <Text style={styles.balanceWatermark}>$</Text>
          <Text style={styles.balanceEyebrow}>Saldo disponible</Text>
          <Text style={styles.balance}>{moneyLabel(wallet.balance)}</Text>
          <Text style={styles.balanceMeta}>UTCH-241087</Text>
          <View style={styles.balanceActions}>
            <PrimaryButton
              variant="soft"
              label="Añadir dinero"
              onPress={() => router.push('/(student)/wallet-add-money')}
              style={styles.balanceButton}
            />
          </View>
        </View>

        {/* .wallet-actions { gap:8; margin:10 0 } */}
        <View style={styles.walletActions}>
          {WALLET_ACTIONS.map((action) => (
            <PhysicalPress
              key={action.title}
              style={styles.walletAction}
              onPress={() => router.push(action.route as never)}
            >
              <Ionicons name={action.icon} size={20} color={colors.ink} />
              <Text style={styles.walletActionTitle}>{action.title}</Text>
              <Text style={styles.walletActionHint}>{action.hint}</Text>
            </PhysicalPress>
          ))}
        </View>

        {/* .mini-grid { gap:10 } */}
        <View style={styles.miniGrid}>
          <View style={styles.miniCard}>
            <Text style={styles.miniValue}>$29</Text>
            <Text style={styles.miniLabel}>Cashback acumulado</Text>
          </View>
          <View style={styles.miniCard}>
            <Text style={styles.miniValue}>{wallet.cards.length}</Text>
            <Text style={styles.miniLabel}>Métodos guardados</Text>
          </View>
        </View>

        <SectionHead
          title="Métodos de pago"
          actionLabel="Administrar"
          onAction={() => router.push('/(student)/wallet-methods')}
        />
        <View style={styles.walletSection}>
          {wallet.cards.map((card) => (
            <View key={card.id} style={styles.paymentMethod}>
              <View style={styles.paymentBrand}>
                <Text style={styles.paymentBrandText}>{card.brand.toUpperCase()}</Text>
              </View>
              <View style={styles.paymentCopy}>
                <Text style={styles.paymentTitle}>•••• {card.last4}</Text>
                <Text style={styles.paymentHint}>
                  Vence 08/29 · disponible para pagar pedidos
                </Text>
              </View>
              <View style={styles.paymentCheck}>
                <Text style={styles.paymentCheckText}>✓</Text>
              </View>
            </View>
          ))}
          <View style={styles.paymentMethod}>
            <View style={[styles.paymentBrand, styles.paymentBrandTransfer]}>
              <Text style={[styles.paymentBrandText, styles.paymentBrandTextTransfer]}>SPEI</Text>
            </View>
            <View style={styles.paymentCopy}>
              <Text style={styles.paymentTitle}>Transferencia bancaria</Text>
              <Text style={styles.paymentHint}>Sólo para añadir dinero al saldo</Text>
            </View>
          </View>
        </View>

        <SectionHead
          title="Cuenta"
          actionLabel="Ver cuenta"
          onAction={() => router.push('/(student)/wallet-account')}
        />
        <PhysicalPress
          style={[styles.walletSection, styles.accountSummary]}
          onPress={() => router.push('/(student)/wallet-account')}
        >
          <View style={styles.accountAvatar}>
            <Text style={styles.accountAvatarText}>DA</Text>
          </View>
          <View style={styles.accountCopy}>
            <Text style={styles.accountName}>Dani</Text>
            <Text style={styles.accountMeta}>dani.alvarez@utch.mx{'\n'}UTCH-241087</Text>
          </View>
          <Text style={styles.chevron}>{'›'}</Text>
        </PhysicalPress>

        <SectionHead title="Movimientos recientes" actionLabel="Mostrar código" />
        <View style={styles.movements}>
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

        <SectionHead
          title="Mis stickers"
          actionLabel="Ver colección"
          onAction={() => router.push('/(student)/sticker')}
        />
        {/* .sticker-collection { grid 1fr 1fr; gap:7 } */}
        <View style={styles.stickerCollection}>
          {STICKERS.map((sticker) => (
            <View key={sticker.title} style={styles.stickerChip}>
              <View style={[styles.stickerSwatch, { backgroundColor: sticker.swatch }]} />
              <View style={styles.stickerCopy}>
                <Text style={styles.stickerTitle}>{sticker.title}</Text>
                <Text style={styles.stickerHint}>{sticker.hint}</Text>
              </View>
            </View>
          ))}
        </View>
      </ScrollView>

      <BottomNav {...nav} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  // .balance-card
  balanceCard: {
    borderRadius: radius.balanceCard,
    backgroundColor: colors.accent,
    padding: 24,
    overflow: 'hidden',
  },
  // .balance-watermark { right:-10; bottom:-65; 170px; opacity:.09 }
  balanceWatermark: {
    position: 'absolute',
    right: -10,
    bottom: -65,
    fontFamily,
    fontSize: 170,
    fontWeight: weight.black,
    color: colors.accentInk,
    opacity: 0.09,
  },
  balanceEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: colors.accentInk,
    opacity: 0.65,
  },
  // .balance { 50px; line-height:1; 950; margin-top:6 }
  balance: {
    fontFamily,
    fontSize: 50,
    lineHeight: 50,
    fontWeight: weight.black,
    color: colors.accentInk,
    marginTop: 6,
  },
  balanceMeta: {
    fontFamily,
    fontSize: 12,
    color: colors.accentInk,
    opacity: 0.7,
    marginTop: 6,
  },
  // .wallet-balance-actions { gap:8; margin-top:14 }
  balanceActions: { flexDirection: 'row', gap: 8, marginTop: 14 },
  // .primary.light { background:--paper; color:--ink }
  balanceButton: { flex: 1, backgroundColor: colors.paper },

  // .wallet-actions { gap:8; margin:10 0 }
  walletActions: { flexDirection: 'row', gap: 8, marginVertical: 10 },
  // .wallet-action { radius:19; paper-2; padding:11 9; gap:6 }
  walletAction: {
    flex: 1,
    minWidth: 0,
    borderRadius: 19,
    backgroundColor: colors.paper2,
    paddingVertical: 11,
    paddingHorizontal: 9,
    gap: 6,
    alignItems: 'flex-start',
  },
  walletActionTitle: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  walletActionHint: { fontFamily, fontSize: 9, color: colors.muted, lineHeight: 11 },

  // .mini-grid / .mini-card
  miniGrid: { flexDirection: 'row', gap: 10 },
  miniCard: {
    flex: 1,
    minWidth: 0,
    padding: 17,
    borderRadius: radius.miniCard,
    backgroundColor: colors.paper2,
  },
  miniValue: { fontFamily, fontSize: 22, fontWeight: weight.black, color: colors.ink },
  miniLabel: { fontFamily, fontSize: 11, color: colors.muted, marginTop: 2 },

  // .wallet-section { radius:22; paper-2; padding:11; margin-bottom:8 }
  walletSection: {
    borderRadius: radius.lineItem,
    backgroundColor: colors.paper2,
    padding: 11,
    marginBottom: 8,
  },
  // .payment-method { radius:17; background:--paper; padding:10; gap:10 }
  paymentMethod: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderRadius: 17,
    backgroundColor: colors.paper,
    padding: 10,
    marginTop: 5,
  },
  paymentBrand: {
    width: 42,
    height: 31,
    borderRadius: 10,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paymentBrandTransfer: { backgroundColor: colors.accent },
  paymentBrandText: {
    fontFamily,
    fontSize: 8,
    fontWeight: weight.black,
    letterSpacing: 0.32,
    color: colors.paper,
  },
  paymentBrandTextTransfer: { color: colors.accentInk },
  paymentCopy: { flex: 1, minWidth: 0 },
  paymentTitle: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  paymentHint: { fontFamily, fontSize: 8, lineHeight: 10, color: colors.muted, marginTop: 2 },
  paymentCheck: {
    width: 20,
    height: 20,
    borderRadius: 999,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paymentCheckText: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.accentInk },

  // .account-summary { gap:10 } .account-avatar { 44x44; radius:15; accent }
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
  chevron: { fontFamily, fontSize: 18, color: colors.muted },

  // .movement { gap:12; padding:13 0; border-bottom 1px line }
  movements: { marginBottom: 4 },
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

  // .sticker-collection { 2 columnas; gap:7 }
  stickerCollection: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginVertical: 7 },
  stickerChip: {
    width: '48%',
    flexGrow: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderRadius: 14,
    backgroundColor: colors.paper2,
    paddingVertical: 8,
    paddingHorizontal: 9,
  },
  stickerSwatch: { width: 26, height: 26, borderRadius: 9 },
  stickerCopy: { flex: 1, minWidth: 0 },
  stickerTitle: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  stickerHint: { fontFamily, fontSize: 9, color: colors.muted, marginTop: 1 },
});
