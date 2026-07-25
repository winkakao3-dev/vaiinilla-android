import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { DATA_SOURCE, isFirebaseConfigured } from '@/core/config';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

export function DataModeChip() {
  const firebaseReady = isFirebaseConfigured();
  const isRemote = DATA_SOURCE === 'REMOTE';

  return (
    <View style={styles.row}>
      <View style={[styles.chip, isRemote ? styles.chipRemote : styles.chipMock]}>
        <Text style={[styles.chipText, isRemote ? styles.chipTextRemote : styles.chipTextMock]}>
          {DATA_SOURCE}
        </Text>
      </View>
      <Text style={styles.meta}>
        Firebase {firebaseReady ? 'configurado' : 'no configurado'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    flexWrap: 'wrap',
  },
  chip: {
    borderRadius: radius.chip,
    paddingHorizontal: spacing.md,
    paddingVertical: 4,
    borderWidth: 1,
  },
  chipMock: {
    backgroundColor: colors.paper2,
    borderColor: colors.line,
  },
  chipRemote: {
    backgroundColor: '#edf3d8',
    borderColor: colors.accent,
  },
  chipText: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 0.8,
  },
  chipTextMock: {
    color: colors.ink,
  },
  chipTextRemote: {
    color: colors.accentInk,
  },
  meta: {
    fontFamily: fonts.body,
    fontSize: 12,
    color: colors.muted,
  },
});
