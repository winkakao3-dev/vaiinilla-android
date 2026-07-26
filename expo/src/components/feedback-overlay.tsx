import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';

import { PhysicalPress } from '@/components/physical-press';
import { colors } from '@/theme/colors';
import { fontFamily, weight } from '@/theme/typography';

interface FeedbackOverlayProps {
  /** Nombre de la pantalla que se muestra en el panel, ej. "student-menu". */
  screenName: string;
  /** Comentarios ya guardados en la sesion (solo demo). */
  initialCount?: number;
}

/**
 * Modo comentarios del demo (pantalla 50).
 * .feedback-pin flotante en yolk y .feedback-static, el panel oscuro anclado
 * abajo con nombre del tester, comentario y acciones.
 */
export function FeedbackOverlay({ screenName, initialCount = 3 }: FeedbackOverlayProps) {
  const [open, setOpen] = useState(false);
  const [minimized, setMinimized] = useState(false);
  const [tester, setTester] = useState('Dani');
  const [comment, setComment] = useState(
    'La tarjeta principal se entiende muy bien, pero probaría un poco más de espacio entre categorías.',
  );
  const [count, setCount] = useState(initialCount);

  return (
    <>
      {/* .feedback-pin { 54x54; radius:20; yolk; right:18; bottom:392 } */}
      {!open ? (
        <PhysicalPress
          style={styles.pin}
          onPress={() => {
            setOpen(true);
            setMinimized(false);
          }}
          accessibilityLabel="Abrir panel de comentarios"
        >
          <Text style={styles.pinGlyph}>✎</Text>
        </PhysicalPress>
      ) : null}

      {/* .feedback-static { left:18; right:18; bottom:20; radius:26; ink } */}
      {open ? (
        <View style={[styles.panel, minimized ? styles.panelMinimized : null]}>
          <View style={styles.panelActions}>
            <PhysicalPress
              style={styles.panelAction}
              onPress={() => setMinimized((value) => !value)}
              accessibilityLabel="Minimizar panel de comentarios"
            >
              <Text style={styles.panelActionText}>{minimized ? '+' : '−'}</Text>
            </PhysicalPress>
            <PhysicalPress
              style={styles.panelAction}
              onPress={() => setOpen(false)}
              accessibilityLabel="Cerrar panel de comentarios"
            >
              <Text style={styles.panelActionText}>×</Text>
            </PhysicalPress>
          </View>

          <Text style={styles.title}>Comentarios de prueba</Text>

          {minimized ? null : (
            <>
              <Text style={styles.subtitle}>Pantalla actual: {screenName}</Text>

              <TextInput
                style={styles.input}
                value={tester}
                onChangeText={setTester}
                accessibilityLabel="Nombre del tester"
                placeholderTextColor="#888"
              />
              <TextInput
                style={[styles.input, styles.textarea]}
                value={comment}
                onChangeText={setComment}
                multiline
                accessibilityLabel="Comentario"
                placeholderTextColor="#888"
              />

              {/* .feedback-actions { botones h38 r12 } */}
              <View style={styles.actions}>
                <PhysicalPress style={styles.actionButton}>
                  <Text style={styles.actionText}>Elegir elemento</Text>
                </PhysicalPress>
                <PhysicalPress
                  style={[styles.actionButton, styles.actionPrimary]}
                  onPress={() => setCount((value) => value + 1)}
                >
                  <Text style={[styles.actionText, styles.actionPrimaryText]}>Guardar</Text>
                </PhysicalPress>
                <PhysicalPress style={styles.actionButton}>
                  <Text style={styles.actionText}>Reenviar</Text>
                </PhysicalPress>
              </View>

              <View style={styles.actions}>
                <PhysicalPress style={styles.actionButton}>
                  <Text style={styles.actionText}>Exportar JSON</Text>
                </PhysicalPress>
                <PhysicalPress style={styles.actionButton} onPress={() => setCount(0)}>
                  <Text style={styles.actionText}>Borrar</Text>
                </PhysicalPress>
              </View>

              <Text style={styles.count}>
                {count} comentarios guardados · sincronización pendiente
              </Text>
            </>
          )}
        </View>
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  pin: {
    position: 'absolute',
    right: 18,
    bottom: 392,
    width: 54,
    height: 54,
    borderRadius: 20,
    backgroundColor: colors.yolk,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 181,
    shadowColor: '#000',
    shadowOpacity: 0.28,
    shadowRadius: 34,
    shadowOffset: { width: 0, height: 12 },
    elevation: 10,
  },
  pinGlyph: { fontFamily, fontSize: 22, color: '#281e06' },

  panel: {
    position: 'absolute',
    left: 18,
    right: 18,
    bottom: 20,
    borderRadius: 26,
    backgroundColor: '#171817',
    padding: 17,
    paddingTop: 52,
    zIndex: 180,
    shadowColor: '#000',
    shadowOpacity: 0.38,
    shadowRadius: 48,
    shadowOffset: { width: 0, height: 18 },
    elevation: 14,
  },
  panelMinimized: { paddingTop: 20, paddingBottom: 16, paddingRight: 92, paddingLeft: 17 },

  panelActions: { position: 'absolute', top: 12, right: 12, flexDirection: 'row', gap: 7 },
  panelAction: {
    width: 34,
    height: 34,
    borderRadius: 12,
    backgroundColor: '#2a2b28',
    alignItems: 'center',
    justifyContent: 'center',
  },
  panelActionText: { fontFamily, fontSize: 16, color: '#f6f2e8' },

  title: { fontFamily, fontSize: 18, fontWeight: weight.black, color: '#fff' },
  subtitle: { fontFamily, fontSize: 11, color: '#aaa', marginTop: 4 },

  input: {
    width: '100%',
    marginTop: 10,
    backgroundColor: '#272825',
    color: '#fff',
    borderWidth: 1,
    borderColor: '#444',
    borderRadius: 14,
    padding: 11,
    fontFamily,
    fontSize: 12,
  },
  textarea: { height: 92, textAlignVertical: 'top' },

  actions: { flexDirection: 'row', gap: 7, marginTop: 10, flexWrap: 'wrap' },
  actionButton: {
    height: 38,
    borderRadius: 12,
    paddingHorizontal: 12,
    backgroundColor: '#2b2c29',
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionText: { fontFamily, fontSize: 11, fontWeight: weight.bold, color: '#fff' },
  actionPrimary: { backgroundColor: colors.accent },
  actionPrimaryText: { color: colors.accentInk },

  count: { fontFamily, fontSize: 10, color: '#aaa', marginTop: 10 },
});
