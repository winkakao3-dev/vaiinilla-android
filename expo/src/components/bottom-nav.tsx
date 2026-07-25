import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import React, { useEffect, useRef } from 'react';
import {
  Alert,
  Animated,
  Platform,
  StyleSheet,
  Text,
  View,
  type LayoutChangeEvent,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

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
}> = [
  { key: 'menu', label: 'Menú', icon: 'home-outline' },
  { key: 'assistant', label: 'Asistente', icon: 'sparkles-outline' },
  { key: 'orders', label: 'Pedidos', icon: 'receipt-outline' },
  { key: 'wallet', label: 'Cartera', icon: 'wallet-outline' },
  { key: 'cart', label: 'Carrito', icon: 'cart-outline' },
];

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
  const activeIndex = TABS.findIndex((tab) => tab.key === activeTab);
  const pillX = useRef(new Animated.Value(0)).current;
  const tabWidth = useRef(0);

  useEffect(() => {
    Animated.timing(pillX, {
      toValue: activeIndex * tabWidth.current,
      duration: 340,
      useNativeDriver: true,
    }).start();
  }, [activeIndex, pillX]);

  const handlers: Record<StudentTab, () => void> = {
    menu: onMenu,
    assistant: onAssistant,
    orders: onOrders,
    wallet: onWallet,
    cart: onCart,
  };

  const onLayout = (event: LayoutChangeEvent) => {
    tabWidth.current = (event.nativeEvent.layout.width - spacing.md * 2) / TABS.length;
    pillX.setValue(activeIndex * tabWidth.current);
  };

  const handlePress = (tab: StudentTab) => {
    if (tab === 'menu' || tab === 'cart') {
      handlers[tab]();
      return;
    }
    Alert.alert('Próximamente', 'Esta fase llegará en una siguiente entrega.');
  };

  const shell = (
    <View style={styles.shell} onLayout={onLayout}>
      <Animated.View
        pointerEvents="none"
        style={[
          styles.pill,
          {
            width: tabWidth.current || '20%',
            transform: [{ translateX: pillX }],
          },
        ]}
      />
      {TABS.map((tab) => {
        const selected = tab.key === activeTab;
        return (
          <PhysicalPress
            key={tab.key}
            scale="nav"
            style={styles.tab}
            onPress={() => handlePress(tab.key)}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
          >
            <View style={styles.iconWrap}>
              <Ionicons
                name={tab.icon}
                size={22}
                color={selected ? colors.navTextActive : colors.navTextIdle}
              />
              {tab.key === 'cart' && cartCount > 0 ? (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{cartCount}</Text>
                </View>
              ) : null}
            </View>
            <Text style={[styles.label, selected && styles.labelActive]}>{tab.label}</Text>
          </PhysicalPress>
        );
      })}
    </View>
  );

  return (
    <View style={[styles.dock, { paddingBottom: Math.max(insets.bottom, spacing.md) }]}>
      {Platform.OS === 'ios' ? (
        <BlurView intensity={40} tint="dark" style={styles.blur}>
          {shell}
        </BlurView>
      ) : (
        <View style={styles.androidShell}>{shell}</View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  dock: {
    position: 'absolute',
    left: spacing.md,
    right: spacing.md,
    bottom: 0,
  },
  blur: {
    borderRadius: radius.nav,
    overflow: 'hidden',
  },
  androidShell: {
    borderRadius: radius.nav,
    overflow: 'hidden',
    backgroundColor: colors.navGlass,
    borderWidth: 1,
    borderColor: colors.navBorder,
    shadowColor: '#000',
    shadowOpacity: 0.5,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: 18 },
    elevation: 18,
  },
  shell: {
    minHeight: 88,
    padding: 9,
    flexDirection: 'row',
    alignItems: 'stretch',
    backgroundColor: Platform.OS === 'ios' ? colors.navGlass : 'transparent',
    borderWidth: Platform.OS === 'ios' ? 1 : 0,
    borderColor: colors.navBorder,
    borderRadius: radius.nav,
  },
  pill: {
    position: 'absolute',
    top: 9,
    bottom: 9,
    left: 9,
    borderRadius: 999,
    backgroundColor: colors.navPill,
    borderWidth: 1,
    borderColor: colors.navInsetHighlight,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    zIndex: 1,
  },
  iconWrap: {
    position: 'relative',
  },
  badge: {
    position: 'absolute',
    top: -6,
    right: -10,
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: colors.coral,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  badgeText: {
    color: colors.white,
    fontFamily: fonts.bodyBold,
    fontSize: 10,
  },
  label: {
    fontFamily: fonts.bodyMedium,
    fontSize: 11,
    color: colors.navTextIdle,
  },
  labelActive: {
    color: colors.navTextActive,
    fontFamily: fonts.bodyBold,
  },
});
