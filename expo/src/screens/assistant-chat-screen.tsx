import { router, useLocalSearchParams } from 'expo-router';
import React, { useMemo, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PhysicalPress } from '@/components/physical-press';
import { assistantReply } from '@/domain/assistant-local-replies';
import { useOrderFlow } from '@/hooks/use-order-flow';
import { colors } from '@/theme/colors';
import { radius, spacing } from '@/theme/spacing';
import { fonts } from '@/theme/typography';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
}

const SUGGESTIONS = ['¿Qué me recomiendas?', 'Algo ligero', '¿Hay opciones sin gluten?'];

export function AssistantChatScreen() {
  const insets = useSafeAreaInsets();
  const flow = useOrderFlow();
  const params = useLocalSearchParams<{ chip?: string }>();
  const products = flow.catalog?.products ?? [];

  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      text: 'Hola, soy tu asistente Vaiinilla. Puedo recomendarte del menú o ayudarte con alérgenos.',
    },
  ]);

  const chipHint = useMemo(() => params.chip, [params.chip]);

  const sendMessage = (text: string) => {
    const trimmed = text.trim();
    if (!trimmed) {
      return;
    }
    const userMessage: ChatMessage = { id: `u-${Date.now()}`, role: 'user', text: trimmed };
    const reply: ChatMessage = {
      id: `a-${Date.now()}`,
      role: 'assistant',
      text: assistantReply(trimmed, products),
    };
    setMessages((current) => [...current, userMessage, reply]);
    setInput('');
  };

  return (
    <KeyboardAvoidingView
      style={styles.root}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={insets.top}
    >
      <View style={[styles.header, { paddingTop: insets.top + spacing.md }]}>
        <PhysicalPress onPress={() => router.back()}>
          <Text style={styles.back}>← Volver</Text>
        </PhysicalPress>
        <Text style={styles.title}>Asistente</Text>
        {chipHint ? <Text style={styles.chipHint}>Filtro: {chipHint}</Text> : null}
      </View>

      <ScrollView contentContainerStyle={styles.messages}>
        {messages.map((message) => (
          <View
            key={message.id}
            style={[
              styles.bubble,
              message.role === 'user' ? styles.userBubble : styles.assistantBubble,
            ]}
          >
            <Text
              style={[
                styles.bubbleText,
                message.role === 'user' ? styles.userText : styles.assistantText,
              ]}
            >
              {message.text}
            </Text>
          </View>
        ))}
      </ScrollView>

      <View style={styles.suggestions}>
        {SUGGESTIONS.map((suggestion) => (
          <PhysicalPress key={suggestion} style={styles.suggestion} onPress={() => sendMessage(suggestion)}>
            <Text style={styles.suggestionText}>{suggestion}</Text>
          </PhysicalPress>
        ))}
      </View>

      <View style={[styles.composer, { paddingBottom: Math.max(insets.bottom, spacing.md) }]}>
        <TextInput
          value={input}
          onChangeText={setInput}
          placeholder="Escribe tu pregunta…"
          placeholderTextColor={colors.muted}
          style={styles.input}
          onSubmitEditing={() => sendMessage(input)}
        />
        <PhysicalPress style={styles.send} onPress={() => sendMessage(input)}>
          <Text style={styles.sendText}>Enviar</Text>
        </PhysicalPress>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.paper },
  header: { paddingHorizontal: spacing.screen, gap: 4, paddingBottom: spacing.md },
  back: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.ink },
  title: { fontFamily: fonts.displayBlack, fontSize: 28, color: colors.ink },
  chipHint: { fontFamily: fonts.body, fontSize: 12, color: colors.muted },
  messages: { paddingHorizontal: spacing.screen, gap: spacing.md, paddingBottom: spacing.lg },
  bubble: {
    maxWidth: '88%',
    borderRadius: radius.card,
    padding: spacing.lg,
  },
  userBubble: {
    alignSelf: 'flex-end',
    backgroundColor: colors.ink,
  },
  assistantBubble: {
    alignSelf: 'flex-start',
    backgroundColor: colors.paper2,
    borderWidth: 1,
    borderColor: colors.line,
  },
  bubbleText: { fontFamily: fonts.body, fontSize: 15, lineHeight: 21 },
  userText: { color: colors.paper },
  assistantText: { color: colors.ink },
  suggestions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    paddingHorizontal: spacing.screen,
    paddingBottom: spacing.sm,
  },
  suggestion: {
    backgroundColor: colors.paper2,
    borderRadius: radius.chip,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderWidth: 1,
    borderColor: colors.line,
  },
  suggestionText: { fontFamily: fonts.bodyMedium, fontSize: 12, color: colors.ink },
  composer: {
    flexDirection: 'row',
    gap: spacing.sm,
    paddingHorizontal: spacing.screen,
    paddingTop: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    backgroundColor: colors.paper,
  },
  input: {
    flex: 1,
    backgroundColor: colors.paper2,
    borderRadius: radius.button,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    fontFamily: fonts.body,
    fontSize: 15,
    color: colors.ink,
    borderWidth: 1,
    borderColor: colors.line,
  },
  send: {
    backgroundColor: colors.ink,
    borderRadius: radius.button,
    paddingHorizontal: spacing.lg,
    justifyContent: 'center',
  },
  sendText: { fontFamily: fonts.bodyBold, fontSize: 14, color: colors.paper },
});
