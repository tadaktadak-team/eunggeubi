import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useRef, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import AiAvatar from '../components/AiAvatar';
import ChatBubbleUser from '../components/ChatBubbleUser';
import ChecklistCard from '../components/ChecklistCard';
import DisclaimerFooter from '../components/DisclaimerFooter';
import ReferenceInfoCard from '../components/ReferenceInfoCard';
import { useSymptomChat } from '../hooks/useSymptomChat';
import { AiConsultationStackParamList } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;
type ChatRoute = RouteProp<AiConsultationStackParamList, 'SymptomChat'>;

export default function SymptomChatScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<ChatRoute>();
  const insets = useSafeAreaInsets();
  const { messages, loading, sendText, toggleChecklistItem, submitChecklist } = useSymptomChat(params?.initialMessage);

  const [inputText, setInputText] = useState('');
  const scrollRef = useRef<ScrollView>(null);

  const onSend = () => {
    if (!inputText.trim()) return;
    sendText(inputText);
    setInputText('');
  };

  const showDisclaimer = () =>
    Alert.alert('상담 유의사항', 'AI 증상 상담은 참고 정보만 제공해요. 진단이 아니며, 의료 전문가의 상담을 대체하지 않습니다.');

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={insets.top}
    >
      <View style={[styles.header, { paddingTop: insets.top }]}>
        <View style={styles.headerRow}>
          <Pressable style={styles.side} onPress={() => navigation.goBack()} hitSlop={8}>
            <Ionicons name="chevron-back" size={26} color={colors.text} />
          </Pressable>
          <AiAvatar />
          <View style={styles.headerText}>
            <Text style={styles.headerTitle}>AI 증상 상담</Text>
            <Text style={styles.headerSubtitle}>참고 정보만 제공해요 · 진단 아님</Text>
          </View>
          <Pressable style={styles.side} onPress={showDisclaimer} hitSlop={8}>
            <Ionicons name="alert-circle-outline" size={22} color={colors.textSub} />
          </Pressable>
        </View>
      </View>

      <ScrollView
        ref={scrollRef}
        style={styles.list}
        contentContainerStyle={styles.listContent}
        keyboardShouldPersistTaps="handled"
        onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
      >
        {messages.map((message) => {
          if (message.type === 'user') {
            return <ChatBubbleUser key={message.id} text={message.text} />;
          }
          if (message.type === 'reference') {
            return <ReferenceInfoCard key={message.id} message={message} />;
          }
          return (
            <ChecklistCard
              key={message.id}
              message={message}
              onToggleItem={(itemId) => toggleChecklistItem(message.id, itemId)}
              onSubmit={() => submitChecklist(message.id)}
            />
          );
        })}

        {loading && (
          <View style={styles.typingRow}>
            <AiAvatar />
            <Text style={styles.typingText}>입력 중...</Text>
          </View>
        )}
      </ScrollView>

      <View style={styles.inputBar}>
        <TextInput
          style={styles.textInput}
          placeholder="증상을 입력하세요..."
          placeholderTextColor={colors.placeholder}
          value={inputText}
          onChangeText={setInputText}
          onSubmitEditing={onSend}
          returnKeyType="send"
        />
        <Pressable style={styles.sendBtn} onPress={onSend} hitSlop={8}>
          <Ionicons name="arrow-forward" size={18} color={colors.white} />
        </Pressable>
      </View>

      <DisclaimerFooter />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: { backgroundColor: colors.white, borderBottomWidth: 1, borderBottomColor: colors.border },
  headerRow: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    gap: spacing.sm,
  },
  side: { width: 32, alignItems: 'center', justifyContent: 'center' },
  headerText: { flex: 1 },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  headerSubtitle: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },
  list: { flex: 1 },
  listContent: { padding: spacing.lg, gap: spacing.lg },
  typingRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  typingText: { fontSize: font.sub, color: colors.placeholder },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.white,
  },
  textInput: {
    flex: 1,
    backgroundColor: colors.inputBg,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm + 2,
    fontSize: font.body,
    color: colors.text,
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
