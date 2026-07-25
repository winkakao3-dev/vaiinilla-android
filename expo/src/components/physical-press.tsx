import React, { useRef } from 'react';
import {
  Animated,
  Pressable,
  type PressableProps,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

export type PhysicalPressScale = 'default' | 'nav' | 'card';

const SCALE_MAP: Record<PhysicalPressScale, number> = {
  default: 0.965,
  nav: 0.97,
  card: 0.955,
};

interface PhysicalPressProps extends Omit<PressableProps, 'style'> {
  scale?: PhysicalPressScale;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
}

export function PhysicalPress({
  scale = 'default',
  disabled,
  onPress,
  style,
  children,
  ...rest
}: PhysicalPressProps) {
  const animated = useRef(new Animated.Value(1)).current;

  const animateTo = (value: number) => {
    Animated.timing(animated, {
      toValue: value,
      duration: value < 1 ? 90 : 240,
      useNativeDriver: true,
    }).start();
  };

  return (
    <Pressable
      disabled={disabled}
      onPress={onPress}
      onPressIn={() => animateTo(SCALE_MAP[scale])}
      onPressOut={() => animateTo(1)}
      accessibilityRole="button"
      {...rest}
    >
      <Animated.View style={[style, { transform: [{ scale: animated }] }]}>{children}</Animated.View>
    </Pressable>
  );
}
