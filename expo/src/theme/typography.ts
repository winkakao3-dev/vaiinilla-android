import { Platform, TextStyle } from 'react-native';

import { colors } from '@/theme/colors';

/**
 * El demo usa "Avenir Next" con pesos 800-950 (una grotesca pesada), NO una
 * serif. Por eso aqui se usa la fuente del sistema con pesos numericos:
 * es lo mas cercano al demo en RN. Fraunces (serif) quedo fuera a proposito.
 */
export const fontFamily = Platform.select({
  ios: 'System',
  android: 'sans-serif',
  default: 'System',
}) as string;

/** Pesos del demo mapeados a los que RN puede renderizar. */
export const weight = {
  regular: '400',
  medium: '600',
  bold: '800', // 850 del demo
  black: '900', // 900/950 del demo
} as const satisfies Record<string, TextStyle['fontWeight']>;

/** Fuentes legacy: se mantienen para no romper imports existentes. */
export const fonts = {
  body: fontFamily,
  bodyMedium: fontFamily,
  bodyBold: fontFamily,
  display: fontFamily,
  displayBlack: fontFamily,
} as const;

const base: TextStyle = { fontFamily, color: colors.ink };

export const typography = {
  /** .display { font-size:34; line-height:.98; letter-spacing:-.055em; weight:950 } */
  display: {
    ...base,
    fontSize: 34,
    lineHeight: 34 * 0.98,
    letterSpacing: -34 * 0.055,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .success .display { font-size:42 } */
  displayLg: {
    ...base,
    fontSize: 42,
    lineHeight: 42 * 0.98,
    letterSpacing: -42 * 0.055,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .headline { font-size:26; line-height:1.03; letter-spacing:-.04em; weight:950 } */
  headline: {
    ...base,
    fontSize: 26,
    lineHeight: 26 * 1.03,
    letterSpacing: -26 * 0.04,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .section-head h2 { font-size:19; letter-spacing:-.025em } */
  sectionTitle: {
    ...base,
    fontSize: 19,
    letterSpacing: -19 * 0.025,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .topbar-title { font-weight:900; font-size:16 } */
  topbarTitle: {
    ...base,
    fontSize: 16,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .product-card h3 { font-size:15; line-height:1.02; letter-spacing:-.02em } */
  cardTitle: {
    ...base,
    fontSize: 15,
    lineHeight: 15 * 1.02,
    letterSpacing: -15 * 0.02,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .product-card .price { font-size:22; font-weight:950 } */
  price: {
    ...base,
    fontSize: 22,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .balance { font-size:50; line-height:1; font-weight:950 } */
  balance: {
    ...base,
    fontSize: 50,
    lineHeight: 50,
    fontWeight: weight.black,
  } satisfies TextStyle,
  body: {
    ...base,
    fontSize: 14,
    lineHeight: 20,
    color: colors.ink2,
    fontWeight: weight.regular,
  } satisfies TextStyle,
  /** .small { font-size:12; color:var(--muted) } */
  small: {
    ...base,
    fontSize: 12,
    color: colors.muted,
    fontWeight: weight.regular,
  } satisfies TextStyle,
  /** .chip / .tab { font-size:12; font-weight:850 } */
  chip: {
    ...base,
    fontSize: 12,
    fontWeight: weight.bold,
  } satisfies TextStyle,
  /** .primary { font-weight:900 } */
  button: {
    ...base,
    fontSize: 14,
    fontWeight: weight.black,
  } satisfies TextStyle,
  /** .admin-row span / .mini-card span { font-size:10-11; muted } */
  micro: {
    ...base,
    fontSize: 10,
    color: colors.muted,
    fontWeight: weight.medium,
  } satisfies TextStyle,
  eyebrow: {
    ...base,
    fontSize: 11,
    letterSpacing: 1.2,
    textTransform: 'uppercase',
    color: colors.muted,
    fontWeight: weight.bold,
  } satisfies TextStyle,
  title: {
    ...base,
    fontSize: 28,
    lineHeight: 28,
    letterSpacing: -1.2,
    fontWeight: weight.black,
  } satisfies TextStyle,
  label: {
    ...base,
    fontSize: 14,
    fontWeight: weight.bold,
  } satisfies TextStyle,
} as const;
