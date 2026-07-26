import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type StyleProp,
  type TextStyle,
  type ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fontFamily, typography, weight } from '@/theme/typography';

/* ------------------------------------------------------------------ *
 * Primitivas portadas del demo (clases .screen, .topbar, .hero, etc.)
 * Cada componente lleva en el comentario la regla CSS que replica.
 * ------------------------------------------------------------------ */

/** `.screen` + `.screen-body { padding: 50px 20px 108px }` */
export function ScreenBody({
  children,
  hasNav = true,
  style,
  contentStyle,
}: {
  children: React.ReactNode;
  hasNav?: boolean;
  style?: StyleProp<ViewStyle>;
  contentStyle?: StyleProp<ViewStyle>;
}) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.screen, style]}>
      <ScrollView
        style={styles.flex}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          {
            paddingTop: Math.max(spacing.screenTop, insets.top + spacing.md),
            paddingHorizontal: spacing.screen,
            paddingBottom:
              (hasNav ? spacing.screenBottom : spacing.screenBottomNoNav) + insets.bottom,
          },
          contentStyle,
        ]}
      >
        {children}
      </ScrollView>
    </View>
  );
}

/** `.topbar { height:48; gap:10; margin-bottom:14 }` */
export function TopBar({
  title,
  left,
  right,
  style,
}: {
  title?: string;
  left?: React.ReactNode;
  right?: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.topbar, style]}>
      {left}
      {title ? (
        <Text style={styles.topbarTitle} numberOfLines={1}>
          {title}
        </Text>
      ) : (
        <View style={styles.flex} />
      )}
      {right}
    </View>
  );
}

/** `.icon-btn { 42x42; radius:16; background:var(--paper-2) }` */
export function IconButton({
  name,
  onPress,
  tint = colors.ink,
  background = colors.paper2,
  accessibilityLabel,
}: {
  name: keyof typeof Ionicons.glyphMap;
  onPress?: () => void;
  tint?: string;
  background?: string;
  accessibilityLabel?: string;
}) {
  return (
    <PhysicalPress
      style={[styles.iconBtn, { backgroundColor: background }]}
      onPress={onPress}
      accessibilityLabel={accessibilityLabel}
    >
      <Ionicons name={name} size={21} color={tint} />
    </PhysicalPress>
  );
}

/** `.hero { radius:34; padding:24; min-height:208; bg:--accent; color:--accent-ink }` */
export function Hero({
  children,
  style,
  background = colors.accent,
}: {
  children: React.ReactNode;
  style?: StyleProp<ViewStyle>;
  background?: string;
}) {
  return <View style={[styles.hero, { backgroundColor: background }, style]}>{children}</View>;
}

/** `.section-head { margin:26px 4px 12px }` + `h2 19px` + boton muted 12px/850 */
export function SectionHead({
  title,
  actionLabel,
  onAction,
  style,
}: {
  title: string;
  actionLabel?: string;
  onAction?: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.sectionHead, style]}>
      <Text style={typography.sectionTitle}>{title}</Text>
      {actionLabel ? (
        <PhysicalPress onPress={onAction}>
          <Text style={styles.sectionAction}>{actionLabel}</Text>
        </PhysicalPress>
      ) : null}
    </View>
  );
}

/** `.primary { min-height:50; radius:18; padding:0 18; bg:--ink; color:--paper; 900 }` */
export function PrimaryButton({
  label,
  onPress,
  variant = 'ink',
  icon,
  disabled,
  style,
  textStyle,
}: {
  label: string;
  onPress?: () => void;
  /** `ink` = .primary · `accent` = .primary.primary-mini · `soft` = .ghost */
  variant?: 'ink' | 'accent' | 'soft';
  icon?: keyof typeof Ionicons.glyphMap;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
}) {
  const palette =
    variant === 'accent'
      ? { bg: colors.accent, fg: colors.accentInk }
      : variant === 'soft'
        ? { bg: colors.paper2, fg: colors.ink }
        : { bg: colors.ink, fg: colors.paper };

  return (
    <PhysicalPress
      onPress={onPress}
      disabled={disabled}
      style={[styles.primary, { backgroundColor: palette.bg, opacity: disabled ? 0.45 : 1 }, style]}
    >
      {icon ? <Ionicons name={icon} size={18} color={palette.fg} /> : null}
      <Text style={[typography.button, { color: palette.fg }, textStyle]}>{label}</Text>
    </PhysicalPress>
  );
}

/** `.chip { height:36; radius:13; padding:0 14; bg:--paper-2; 12px/850 }` */
export function Chip({
  label,
  active,
  onPress,
}: {
  label: string;
  active?: boolean;
  onPress?: () => void;
}) {
  return (
    <PhysicalPress
      onPress={onPress}
      style={[styles.chip, active && { backgroundColor: colors.ink }]}
    >
      <Text style={[typography.chip, active && { color: colors.paper }]}>{label}</Text>
    </PhysicalPress>
  );
}

/** `.tab { height:40; radius:14; padding:0 15 }` · `.tab.active { bg:--ink; color:--paper }` */
export function Tab({
  label,
  active,
  onPress,
}: {
  label: string;
  active?: boolean;
  onPress?: () => void;
}) {
  return (
    <PhysicalPress onPress={onPress} style={[styles.tab, active && { backgroundColor: colors.ink }]}>
      <Text style={[typography.chip, active && { color: colors.paper }]}>{label}</Text>
    </PhysicalPress>
  );
}

/** `.search { height:52; radius:19; bg:--paper-2; padding:0 16; gap:10 }` */
export function SearchField({
  value,
  onChangeText,
  placeholder = 'Buscar',
}: {
  value: string;
  onChangeText: (next: string) => void;
  placeholder?: string;
}) {
  return (
    <View style={styles.search}>
      <Ionicons name="search-outline" size={19} color={colors.muted} />
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
        style={styles.searchInput}
      />
    </View>
  );
}

/** `.mini-card { padding:17; radius:22; bg:--paper-2 }` */
export function MiniCard({
  value,
  label,
  style,
}: {
  value: string;
  label: string;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.miniCard, style]}>
      <Text style={styles.miniCardValue}>{value}</Text>
      <Text style={typography.micro}>{label}</Text>
    </View>
  );
}

/** `.quick-card { min-width:156; min-height:150; radius:27; padding:18; bg:--paper-2 }` */
export function QuickCard({
  title,
  caption,
  onPress,
  background = colors.paper2,
  foreground = colors.ink,
}: {
  title: string;
  caption?: string;
  onPress?: () => void;
  background?: string;
  foreground?: string;
}) {
  return (
    <PhysicalPress
      scale="card"
      onPress={onPress}
      style={[styles.quickCard, { backgroundColor: background }]}
    >
      <View style={styles.flex} />
      <View>
        <Text style={[styles.quickCardTitle, { color: foreground }]}>{title}</Text>
        {caption ? (
          <Text style={[styles.quickCardCaption, { color: foreground }]}>{caption}</Text>
        ) : null}
      </View>
    </PhysicalPress>
  );
}

/** `.line-item { gap:12; bg:--paper-2; padding:10; radius:22 }` */
export function LineItem({
  children,
  style,
}: {
  children: React.ReactNode;
  style?: StyleProp<ViewStyle>;
}) {
  return <View style={[styles.lineItem, style]}>{children}</View>;
}

/** `.grabber { 42x5; radius:99 }` */
export function Grabber() {
  return (
    <View style={styles.grabberWrap}>
      <View style={styles.grabber} />
    </View>
  );
}

export const styles = StyleSheet.create({
  flex: { flex: 1 },
  screen: {
    flex: 1,
    backgroundColor: colors.paper,
  },
  topbar: {
    height: 48,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 14,
  },
  topbarTitle: {
    ...typography.topbarTitle,
    flex: 1,
  },
  iconBtn: {
    width: 42,
    height: 42,
    borderRadius: radius.iconBtn,
    alignItems: 'center',
    justifyContent: 'center',
  },
  hero: {
    borderRadius: radius.hero,
    padding: 24,
    minHeight: 208,
    overflow: 'hidden',
    justifyContent: 'flex-end',
  },
  sectionHead: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: 10,
    marginTop: 26,
    marginBottom: 12,
    marginHorizontal: 4,
  },
  sectionAction: {
    fontFamily,
    fontSize: 12,
    fontWeight: weight.bold,
    color: colors.muted,
  },
  primary: {
    minHeight: 50,
    borderRadius: radius.primary,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  chip: {
    height: 36,
    paddingHorizontal: 14,
    borderRadius: radius.chip,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tab: {
    height: 40,
    paddingHorizontal: 15,
    borderRadius: radius.tab,
    backgroundColor: colors.paper2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  search: {
    height: 52,
    borderRadius: radius.search,
    backgroundColor: colors.paper2,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    gap: 10,
  },
  searchInput: {
    flex: 1,
    fontFamily,
    fontSize: 14,
    color: colors.ink,
    padding: 0,
  },
  miniCard: {
    flex: 1,
    padding: 17,
    borderRadius: radius.miniCard,
    backgroundColor: colors.paper2,
  },
  miniCardValue: {
    fontFamily,
    fontSize: 22,
    fontWeight: weight.black,
    color: colors.ink,
  },
  quickCard: {
    minWidth: 156,
    minHeight: 150,
    borderRadius: 27,
    padding: 18,
    justifyContent: 'space-between',
  },
  quickCardTitle: {
    fontFamily,
    fontSize: 16,
    lineHeight: 16 * 1.03,
    fontWeight: weight.black,
  },
  quickCardCaption: {
    fontFamily,
    fontSize: 11,
    marginTop: 5,
    opacity: 0.66,
  },
  lineItem: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'center',
    backgroundColor: colors.paper2,
    padding: 10,
    borderRadius: radius.lineItem,
  },
  grabberWrap: {
    alignItems: 'center',
    paddingVertical: 8,
  },
  grabber: {
    width: 42,
    height: 5,
    borderRadius: 99,
    backgroundColor: colors.line,
  },
});
