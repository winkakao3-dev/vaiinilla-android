import React, { useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { DataModeChip } from '@/components/data-mode-chip';
import { PhysicalPress } from '@/components/physical-press';
import { DATA_SOURCE, isFirebaseConfigured } from '@/core/config';
import { authenticateSeedUser } from '@/data/auth/seed-auth-repository';
import { SEED_ACCOUNTS, SEED_PASSWORD } from '@/data/auth/seed-accounts';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface LoginScreenProps {
  onSuccess?: () => void;
}

export function LoginScreen({ onSuccess }: LoginScreenProps) {
  const insets = useSafeAreaInsets();
  const [email, setEmail] = useState(SEED_ACCOUNTS[0]?.email ?? '');
  const [password, setPassword] = useState(SEED_PASSWORD);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const firebaseReady = isFirebaseConfigured();
  const isRemote = DATA_SOURCE === 'REMOTE';

  const handleLogin = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const result = await authenticateSeedUser(email, password);
      setSuccess(`Sesión lista · rol ${result.context.rol}`);
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No pudimos iniciar sesión.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView
      style={styles.root}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + spacing.xl }]}
      keyboardShouldPersistTaps="handled"
    >
      <Text style={styles.eyebrow}>Firebase seed</Text>
      <Text style={styles.title}>Iniciar sesión</Text>
      <Text style={styles.subtitle}>
        Usa las cuentas `@vaiinilla.test` con contraseña `{SEED_PASSWORD}`.
      </Text>

      <DataModeChip />

      {isRemote && !firebaseReady ? (
        <View style={styles.banner}>
          <Text style={styles.bannerTitle}>REMOTE sin Firebase</Text>
          <Text style={styles.bannerBody}>
            Copia `expo/.env.example` → `.env`, completa `EXPO_PUBLIC_FIREBASE_*`, reinicia Expo y vuelve a intentar.
          </Text>
        </View>
      ) : !firebaseReady ? (
        <View style={styles.banner}>
          <Text style={styles.bannerTitle}>Firebase no configurado</Text>
          <Text style={styles.bannerBody}>
            En MOCK la app funciona sin Firebase. Para REMOTE, agrega las variables en `.env`.
          </Text>
        </View>
      ) : null}

      <Text style={styles.label}>Correo</Text>
      <TextInput
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        style={styles.input}
      />

      <Text style={styles.label}>Contraseña</Text>
      <TextInput
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        style={styles.input}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}
      {success ? <Text style={styles.success}>{success}</Text> : null}

      <PhysicalPress style={styles.button} onPress={() => void handleLogin()} disabled={loading}>
        {loading ? <ActivityIndicator color={colors.paper} /> : <Text style={styles.buttonText}>Entrar</Text>}
      </PhysicalPress>

      <View style={styles.accounts}>
        <Text style={styles.accountsTitle}>Cuentas seed</Text>
        {SEED_ACCOUNTS.map((account) => (
          <PhysicalPress
            key={account.email}
            style={styles.accountRow}
            onPress={() => setEmail(account.email)}
          >
            <Text style={styles.accountEmail}>{account.email}</Text>
            <Text style={styles.accountRole}>{account.label}</Text>
          </PhysicalPress>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  content: { paddingHorizontal: spacing.screen, paddingBottom: spacing.xxl, gap: spacing.md },
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.2,
    color: colors.muted,
    textTransform: 'uppercase',
  },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  subtitle: { fontFamily: fonts.body, fontSize: 14, lineHeight: 20, color: colors.muted },
  banner: {
    backgroundColor: colors.yolk,
    borderRadius: radius.button,
    padding: spacing.lg,
    gap: 4,
  },
  bannerTitle: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  bannerBody: { fontFamily: fonts.body, fontSize: 13, color: colors.ink2, lineHeight: 18 },
  label: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted, marginTop: spacing.sm },
  input: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderWidth: 1,
    borderColor: colors.line,
    fontFamily: fonts.body,
    fontSize: 15,
    color: colors.ink,
  },
  button: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingVertical: spacing.lg,
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  buttonText: { fontFamily: fonts.bodyBold, fontSize: 16, color: colors.paper },
  error: { fontFamily: fonts.body, color: colors.coral },
  success: { fontFamily: fonts.bodyBold, color: colors.accentInk },
  accounts: { marginTop: spacing.lg, gap: spacing.sm },
  accountsTitle: { fontFamily: fonts.bodyBold, fontSize: 13, color: colors.muted },
  accountRow: {
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    gap: 2,
  },
  accountEmail: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  accountRole: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
});
