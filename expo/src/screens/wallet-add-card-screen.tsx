import { router } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { IconButton, PrimaryButton, TopBar } from '@/components/ui';
import { useWallet } from '@/hooks/use-wallet';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

/**
 * Pantalla 29 del demo.
 * .card-preview en tinta, campos .field, .mini-grid para vencimiento y CVV,
 * .notice-card y el boton .primary.wide.accent.
 */
export function WalletAddCardScreen() {
  const insets = useSafeAreaInsets();
  const wallet = useWallet();
  const [holder, setHolder] = useState('DANI ÁLVAREZ');
  const [number, setNumber] = useState('4242 4242 4242 4242');
  const [expiry, setExpiry] = useState('08/29');
  const [cvv, setCvv] = useState('123');
  const [busy, setBusy] = useState(false);

  const last4 = number.replace(/\D/g, '').slice(-4) || '4242';

  const submit = async () => {
    setBusy(true);
    try {
      await wallet.registerCard('visa', last4);
      router.back();
    } finally {
      setBusy(false);
    }
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
          title="Agregar tarjeta"
          left={
            <IconButton
              name="chevron-back"
              onPress={() => router.back()}
              accessibilityLabel="Volver"
            />
          }
        />

        {/* .card-preview { ink; radius:25; padding:18; min-height:145 } */}
        <View style={styles.cardPreview}>
          <Text style={styles.cardLogo}>VAIINILLA · VISA</Text>
          <Text style={styles.cardNumber}>•••• •••• •••• {last4}</Text>
          <View style={styles.cardMeta}>
            <Text style={styles.cardMetaText}>{holder}</Text>
            <Text style={styles.cardMetaText}>{expiry}</Text>
          </View>
        </View>

        <View style={styles.spacer16} />

        <Field label="Nombre del titular" value={holder} onChangeText={setHolder} />
        <View style={styles.spacer12} />
        <Field
          label="Número de tarjeta"
          value={number}
          onChangeText={setNumber}
          keyboardType="number-pad"
        />
        <View style={styles.spacer12} />

        {/* .mini-grid con los dos campos cortos */}
        <View style={styles.miniGrid}>
          <Field
            label="Vencimiento"
            value={expiry}
            onChangeText={setExpiry}
            style={styles.flex}
          />
          <Field
            label="CVV"
            value={cvv}
            onChangeText={setCvv}
            keyboardType="number-pad"
            style={styles.flex}
          />
        </View>

        <View style={styles.spacer12} />

        {/* .notice-card */}
        <View style={styles.noticeCard}>
          <Text style={styles.noticeStrong}>Interfaz demostrativa.</Text>
          <Text style={styles.noticeText}>
            {' '}
            Los datos no se procesan ni se envían a una pasarela bancaria.
          </Text>
        </View>

        <View style={styles.spacer16} />

        <PrimaryButton
          variant="accent"
          disabled={busy}
          label="Guardar tarjeta"
          onPress={submit}
        />
      </ScrollView>
    </View>
  );
}

/** `.field { gap:7 }` con label 11px/850 e input de borde 1px --line y radio 17. */
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
  flex: { flex: 1 },
  root: { flex: 1, backgroundColor: colors.paper },
  body: { paddingHorizontal: spacing.screen },

  cardPreview: {
    borderRadius: 25,
    backgroundColor: colors.ink,
    padding: 18,
    minHeight: 145,
    justifyContent: 'space-between',
  },
  cardLogo: {
    fontFamily,
    fontSize: 10,
    fontWeight: weight.black,
    letterSpacing: 0.8,
    color: colors.paper,
  },
  cardNumber: {
    fontFamily,
    fontSize: 18,
    letterSpacing: 2.16,
    color: colors.paper,
  },
  cardMeta: { flexDirection: 'row', justifyContent: 'space-between' },
  cardMetaText: { fontFamily, fontSize: 9, color: '#bbb' },

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

  noticeCard: { borderRadius: 18, backgroundColor: colors.paper2, padding: 12 },
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
