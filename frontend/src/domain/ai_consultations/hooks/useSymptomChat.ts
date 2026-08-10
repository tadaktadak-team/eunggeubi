import { useCallback, useEffect, useRef, useState } from 'react';

import { nextChatMessageId, requestSymptomAdvice } from '../api/aiConsultations';
import { ChatMessage } from '../types';

export function useSymptomChat(initialMessage?: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const sentInitialRef = useRef(false);

  // 사용자 텍스트를 말풍선으로 추가하고, AI 응답(참고정보+체크리스트)을 이어서 붙인다.
  const sendText = useCallback(async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed) return;

    setMessages((prev) => [...prev, { id: nextChatMessageId(), type: 'user', text: trimmed }]);
    setLoading(true);
    try {
      const aiMessages = await requestSymptomAdvice(trimmed);
      setMessages((prev) => [...prev, ...aiMessages]);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  // 홈 화면에서 증상을 들고 들어온 경우, 진입 시 한 번만 자동 전송
  useEffect(() => {
    if (initialMessage && !sentInitialRef.current) {
      sentInitialRef.current = true;
      sendText(initialMessage);
    }
  }, [initialMessage, sendText]);

  const toggleChecklistItem = useCallback((messageId: string, itemId: string) => {
    setMessages((prev) =>
      prev.map((m) =>
        m.id === messageId && m.type === 'checklist'
          ? { ...m, items: m.items.map((it) => (it.id === itemId ? { ...it, checked: !it.checked } : it)) }
          : m,
      ),
    );
  }, []);

  // 체크한 항목들을 요약해서 다음 사용자 메시지로 보내고, 이어지는 AI 응답을 받는다.
  const submitChecklist = useCallback(
    (messageId: string) => {
      const target = messages.find((m): m is Extract<ChatMessage, { type: 'checklist' }> => m.id === messageId && m.type === 'checklist');
      if (!target || target.answered) return;

      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, answered: true } : m)));

      const checkedLabels = target.items.filter((it) => it.checked).map((it) => it.label.replace(/\?$/, ''));
      const summary = checkedLabels.length > 0 ? `${checkedLabels.join(', ')} 있어요` : '해당 사항 없어요';
      sendText(summary);
    },
    [messages, sendText],
  );

  return { messages, loading, sendText, toggleChecklistItem, submitChecklist };
}
