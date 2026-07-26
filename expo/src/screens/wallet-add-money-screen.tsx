import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { IconButton, PrimaryButton, SectionHead, TopBar } from '@/components/ui';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/** `.amount-chips` de las pantallas 26 / 27 del demo. */
const AMOUNTS = ['50', '100', '200', '500'];

type Method = 'tarjeta' | 'spei';

/**
 * Pantallas 26 y 27 del demo.
 * .amount-hero en accent con los chips de monto, .payment-choice-list y,
 * segun el metodo, la tarjeta guardada o los datos .bank-data con el aviso.
 */
export function WalletAddMoneyScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const [amount, setAmount] = useState('100');
  const [method, setMethod] = useState<Method>('tarjeta');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      await wallet.addMoney(`${amount}.00`);
      router.back();
    } finally {
      setBusy(false);
    }
  };

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
          title="Añadir dinero"
          left={
            <IconButton name="chevron-back" onPress={() => router.back()} accessibilityLabel="Volver" />
          }
        />

        {/* .amount-hero { accent; radius:27; padding:18; centrado } */}
        <View style={styles.amountHero}>
          <Text style={styles.amountEyebrow}>Monto a agregar</Text>
          <Text style={styles.amountValue}>${amount}</Text>
          <View style={styles.amountChips}>
            {AMOUNTS.map((value) => {
              const active = value === amount;
              return (
                <PhysicalPress
                  key={value}
                  style={[styles.amountChip, active && styles.amountChipActive]}
                  onPress={() => setAmount(value)}
                >
                  <Text style={[styles.amountChipText, active && styles.amountChipTextActive]}>
                    ${value}
                  </Text>
                </PhysicalPress>
              );
            })}
          </View>
        </View>

        <SectionHead title="¿Cómo quieres agregarlo?" />

        {/* .payment-choice-list */}
        <View style={styles.choiceList}>
          <PaymentChoice
            brand="VISA"
            title="Tarjeta •••• 4242"
            hint="Acreditación inmediata en la demo"
            active={method === 'tarjeta'}
            onPress={() => setMethod('tarjeta')}
          />
          <PaymentChoice
            brand="SPEI"
            transfer
            title="Transferencia bancaria"
            hint="Usa tu CLABE y referencia personal"
            active={method === 'spei'}
            onPress={() => setMethod('spei')}
          />
        </View>

        <View style={styles.spacer12} />

        {method === 'tarjeta' ? (
          <View style={styles.walletSection}>
            <View style={styles.paymentMethod}>
              <View style={styles.paymentBrand}>
                <Text style={styles.paymentBrandText}>VISA</Text>
              </View>
              <View style={styles.paymentCopy}>
                <Text style={styles.paymentTitle}>DANI ÁLVAREZ</Text>
                <Text style={styles.paymentHint}>•••• 4242 · vence 08/29</Text>
              </View>
            </View>
          </View>
        ) : (
          <>
            <View style={styles.walletSection}>
              {/* .bank-data */}
              <BankRow label="Banco receptor" value="STP" />
              <BankRow label="CLABE" value="646180157034852019" copyable />
              <BankRow label="Referencia" value="UTCH241087" copyable last />
            </View>
            {/* .notice-card */}
            <View style={styles.noticeCard}>
              <Text style={styles.noticeStrong}>
                La transferencia no paga el producto directamente.
              </Text>
              <Text style={styles.noticeText}>
                {' '}
                Primero se acredita al saldo y después eliges Saldo al confirmar.
              </Text>
            </View>
          </>
        )}

        <View style={styles.spacer16} />

        <PrimaryButton
          variant="accent"
          disabled={busy}
          label={method === 'tarjeta' ? 'Agregar al saldo' : 'Simular transferencia recibida'}
          onPress={submit}
        />
      </ScrollView>
    </View>
  );
}

/** `.payment-choice` compartido con el carrito. */
function PaymentChoice({
  brand,
  title,
  hint,
  active,
  transfer,
  onPress,
}: {
  brand: string;
  title: string;
  hint: string;
  active: boolean;
  transfer?: boolean;
  onPress: () => void;
}) {
  return (
    <PhysicalPress style={[styles.choice, active && styles.choiceActive]} onPress={onPress}>
      <View style={[styles.paymentBrand, transfer && styles.paymentBrandTransfer]}>
        <Text style={[styles.paymentBrandText, transfer && styles.paymentBrandTextTransfer]}>
          {brand}
        </Text>
      </View>
      <View style={styles.paymentCopy}>
        <Text style={[styles.choiceTitle, active && styles.choiceTitleActive]}>{title}</Text>
        <Text style={[styles.choiceHint, active && styles.choiceHintActive]}>{hint}</Text>
      </View>
      {active ? (
        <View style={styles.paymentCheck}>
          <Text style={styles.paymentCheckText}>✓</Text>
        </View>
      ) : null}
    </PhysicalPress>
  );
}

/** `.bank-row { space-between; border-bottom 1px line; padding:8 0; 9px }` */
function BankRow({
  label,
  value,
  copyable,
  last,
}: {
  label: string;
  value: string;
  copyable?: boolean;
  last?: boolean;
}) {
  return (
    <View style={[styles.bankRow, last && styles.bankRowLast]}>
      <Text style={styles.bankLabel}>{label}</Text>
      <Text style={styles.bankValue}>{value}</Text>
      {copyable ? <Text style={styles.copyButton}>Copiar</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  // .amount-hero
  amountHero: {
    borderRadius: 27,
    backgroundColor: colors.accent,
    padding: 18,
    alignItems: 'center',
  },
  amountEyebrow: {
    fontFamily,
    fontSize: 11,
    fontWeight: weight.bold,
    letterSpacing: 1.32,
    color: colors.accentInk,
    opacity: 0.62,
  },
  // .amount-value { 46px; line-height:1; 950; margin-top:8 }
  amountValue: {
    fontFamily,
    fontSize: 46,
    lineHeight: 46,
    fontWeight: weight.black,
    color: colors.accentInk,
    marginTop: 8,
  },
  // .amount-chips { gap:7; centrado; margin-top:12 }
  amountChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 7,
    marginTop: 12,
  },
  amountChip: {
    height: 32,
    borderRadius: 12,
    paddingHorizontal: 11,
    backgroundColor: 'rgba(255,255,255,0.52)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  amountChipActive: { backgroundColor: colors.accentInk },
  amountChipText: {
    fontFamily,
    fontSize: 9,
    fontWeight: weight.black,
    color: colors.accentInk,
  },
  amountChipTextActive: { color: colors.accent },

  // .payment-choice-list { gap:7 }
  choiceList: { gap: 7 },
  choice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
    borderRadius: 17,
    backgroundColor: colors.paper2,
    padding: 9,
  },
  choiceActive: { backgroundColor: colors.accent },
  choiceTitle: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.ink },
  choiceTitleActive: { color: colors.accentInk },
  choiceHint: { fontFamily, fontSize: 8, lineHeight: 10, color: colors.muted, marginTop: 2 },
  choiceHintActive: { color: colors.accentInk, opacity: 0.66 },

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
  paymentCheck: {
    width: 20,
    height: 20,
    borderRadius: 999,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paymentCheckText: { fontFamily, fontSize: 10, fontWeight: weight.black, color: colors.accentInk },

  // .wallet-section { radius:22; paper-2; padding:11 }
  walletSection: {
    borderRadius: radius.lineItem,
    backgroundColor: colors.paper2,
    padding: 11,
    marginBottom: 8,
  },
  paymentMethod: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderRadius: 17,
    backgroundColor: colors.paper,
    padding: 10,
  },
  paymentTitle: { fontFamily, fontSize: 11, fontWeight: weight.black, color: colors.ink },
  paymentHint: { fontFamily, fontSize: 8, lineHeight: 10, color: colors.muted, marginTop: 2 },

  // .bank-row
  bankRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    paddingVertical: 8,
  },
  bankRowLast: { borderBottomWidth: 0 },
  bankLabel: { fontFamily, fontSize: 9, color: colors.muted },
  bankValue: {
    fontFamily,
    fontSize: 9,
    fontWeight: weight.bold,
    color: colors.ink,
    marginLeft: 'auto',
    textAlign: 'right',
  },
  copyButton: { fontFamily, fontSize: 9, fontWeight: weight.bold, color: colors.muted },

  // .notice-card { radius:18; paper-2; padding:12; 9px }
  noticeCard: {
    borderRadius: 18,
    backgroundColor: colors.paper2,
    padding: 12,
  },
  noticeStrong: {
    fontFamily,
    fontSize: 9,
    lineHeight: 13,
    fontWeight: weight.black,
    color: colors.ink,
  },
  noticeText: { fontFamily, fontSize: 9, lineHeight: 13, color: colors.muted },

  spacer12: { height: 12 },
  spacer16: { height: 16 },
});
