import { TextStyle } from 'react-native';

import { colors } from '@/theme/colors';

export const fonts = {
  body: 'DMSans_400Regular',
  bodyMedium: 'DMSans_500Medium',
  bodyBold: 'DMSans_700Bold',
  display: 'Fraunces_700Bold',
  displayBlack: 'Fraunces_900Black',
} as const;

export const typography = {
  eyebrow: {
    fontFamily: fonts.bodyBold,
    fontSize: 11,
    letterSpacing: 1.2,
    textTransform: 'uppercase',
    color: colors.muted,
  } satisfies TextStyle,
  title: {
    fontFamily: fonts.displayBlack,
    fontSize: 28,
    lineHeight: 30,
    color: colors.ink,
  } satisfies TextStyle,
  body: {
    fontFamily: fonts.body,
    fontSize: 15,
    lineHeight: 22,
    color: colors.ink2,
  } satisfies TextStyle,
  label: {
    fontFamily: fonts.bodyBold,
    fontSize: 14,
    color: colors.ink,
  } satisfies TextStyle,
} as const;
