import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { colors, navShadow } from '@/theme/colors';
import { nav as navToken, radius } from '@/theme/spacing';
import { fontFamily, weight } from '@/theme/typography';

export type StudentTab = 'menu' | 'assistant' | 'orders' | 'wallet' | 'cart';

interface BottomNavProps {
  activeTab: StudentTab;
  cartCount: number;
  onMenu: () => void;
  onAssistant: () => void;
  onOrders: () => void;
  onWallet: () => void;
  onCart: () => void;
}

const TABS: Array<{
  key: StudentTab;
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  activeIcon: keyof typeof Ionicons.glyphMap;
}> = [
  { key: 'menu', label: 'Menú', icon: 'home-outline', activeIcon: 'home' },
  { key: 'assistant', label: 'Asistente', icon: 'sparkles-outline', activeIcon: 'sparkles' },
  { key: 'orders', label: 'Pedidos', icon: 'receipt-outline', activeIcon: 'receipt' },
  { key: 'wallet', label: 'Cartera', icon: 'wallet-outline', activeIcon: 'wallet' },
  { key: 'cart', label: 'Carrito', icon: 'cart-outline', activeIcon: 'cart' },
];

/**
 * Replica de `.nav` del demo:
 * barra solida #171817, 68px de alto, radio 24, padding 7, gap 4,
 * item activo con fondo --accent y radio 18.
 */
export function BottomNav({
  activeTab,
  cartCount,
  onMenu,
  onAssistant,
  onOrders,
  onWallet,
  onCart,
}: BottomNavProps) {
  const insets = useSafeAreaInsets();

  const handlers: Record<StudentTab, () => void> = {
    menu: onMenu,
    assistant: onAssistant,
    orders: onOrders,
    wallet: onWallet,
    cart: onCart,
  };

  return (
    <View
      style={[styles.nav, { bottom: navToken.bottom + insets.bottom }]}
      accessibilityRole="tablist"
    >
      {TABS.map((tab) => {
        const selected = tab.key === activeTab;
        return (
          <PhysicalPress
            key={tab.key}
            scale="nav"
            style={[styles.item, selected && styles.itemActive]}
            onPress={handlers[tab.key]}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
          >
            <View>
              <Ionicons
                name={selected ? tab.activeIcon : tab.icon}
                size={22}
                color={selected ? colors.accentInk : colors.navTextIdle}
              />
              {tab.key === 'cart' && cartCount > 0 ? (
                <View style={styles.cartDot}>
                  <Text style={styles.cartDotText}>{cartCount}</Text>
                </View>
              ) : null}
            </View>
            <Text
              style={[styles.label, { color: selected ? colors.accentInk : colors.navTextIdle }]}
              numberOfLines={1}
            >
              {tab.label}
            </Text>
          </PhysicalPress>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  nav: {
    position: 'absolute',
    zIndex: 70,
    left: navToken.inset,
    right: navToken.inset,
    height: navToken.height,
    borderRadius: radius.nav,
    backgroundColor: colors.navBg,
    flexDirection: 'row',
    alignItems: 'center',
    padding: navToken.padding,
    gap: navToken.gap,
    ...navShadow,
  },
  item: {
    flex: 1,
    height: navToken.itemHeight,
    borderRadius: radius.navItem,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 3,
  },
  itemActive: {
    backgroundColor: colors.accent,
  },
  label: {
    fontFamily,
    fontSize: 10,
    fontWeight: weight.bold,
  },
  cartDot: {
    position: 'absolute',
    top: -6,
    left: 14,
    minWidth: 17,
    height: 17,
    paddingHorizontal: 4,
    borderRadius: 99,
    backgroundColor: colors.coral,
    borderWidth: 2,
    borderColor: colors.navBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cartDotText: {
    fontFamily,
    fontSize: 9,
    fontWeight: weight.black,
    color: '#28100d',
  },
});
