import React, { useEffect, useRef } from 'react';
import { AccessibilityInfo, Animated, StyleSheet, View } from 'react-native';

import { colors } from '@/theme/colors';

interface SplashScreenProps {
  onFinished: () => void;
}

export function SplashScreen({ onFinished }: SplashScreenProps) {
  const opacity = useRef(new Animated.Value(0)).current;
  const scale = useRef(new Animated.Value(0.7)).current;
  const splashOpacity = useRef(new Animated.Value(1)).current;
  const onFinishedRef = useRef(onFinished);
  const finishedRef = useRef(false);

  onFinishedRef.current = onFinished;

  const finish = () => {
    if (finishedRef.current) return;
    finishedRef.current = true;
    onFinishedRef.current();
  };

  useEffect(() => {
    let cancelled = false;

    const hardTimeout = setTimeout(() => {
      finish();
    }, 3500);

    const run = async () => {
      const reducedMotion = await AccessibilityInfo.isReduceMotionEnabled();
      if (reducedMotion) {
        if (!cancelled) {
          finish();
        }
        return;
      }

      Animated.parallel([
        Animated.timing(opacity, { toValue: 1, duration: 450, useNativeDriver: true }),
        Animated.timing(scale, { toValue: 1, duration: 450, useNativeDriver: true }),
      ]).start(() => {
        setTimeout(() => {
          Animated.parallel([
            Animated.timing(scale, { toValue: 2.75, duration: 700, useNativeDriver: true }),
            Animated.timing(opacity, { toValue: 0, duration: 700, useNativeDriver: true }),
          ]).start(() => {
            Animated.timing(splashOpacity, {
              toValue: 0,
              duration: 400,
              useNativeDriver: true,
            }).start(() => {
              if (!cancelled) {
                finish();
              }
            });
          });
        }, 500);
      });
    };

    void run();
    return () => {
      cancelled = true;
    };
  }, [opacity, scale, splashOpacity]);

  return (
    <Animated.View style={[styles.root, { opacity: splashOpacity }]}>
      <Animated.View style={[styles.markWrap, { opacity, transform: [{ scale }] }]}>
        <View style={styles.mark}>
          <View style={styles.triangle} />
        </View>
      </Animated.View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.paper,
    alignItems: 'center',
    justifyContent: 'center',
  },
  markWrap: {
    width: 96,
    height: 96,
    alignItems: 'center',
    justifyContent: 'center',
  },
  mark: {
    width: 72,
    height: 72,
    borderRadius: 22,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  triangle: {
    width: 0,
    height: 0,
    borderLeftWidth: 14,
    borderRightWidth: 14,
    borderBottomWidth: 24,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderBottomColor: colors.accent,
    transform: [{ rotate: '12deg' }],
  },
});
