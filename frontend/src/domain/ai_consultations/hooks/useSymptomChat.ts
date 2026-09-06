import { useCallback, useEffect, useRef, useState } from 'react';

import {
  nextChatMessageId,
  regenerateAnswer,
  requestSymptomAdvice,
  submitChecklistAnswers,
} from '../api/aiConsultations';
import { ChatMessage } from '../types';

export function useSymptomChat(initialMessage?: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const sentInitialRef = useRef(false);
  // 상담 세션을 이어가기 위한 값들. 화면(훅 인스턴스)이 살아있는 동안 대화 전체에서 공유한다.
  const sessionRef = useRef<{ sessionId?: string; guestCode?: string }>({});

  // 사용자 텍스트를 말풍선으로 추가하고, AI 응답(+있으면 체크리스트)을 이어서 붙인다.
  const sendText = useCallback(async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed) return;

    setMessages((prev) => [...prev, { id: nextChatMessageId(), type: 'user', text: trimmed }]);
    setLoading(true);
    try {
      const { answerMessage, checklistMessage, sessionId, guestCode } = await requestSymptomAdvice(
        trimmed,
        sessionRef.current.sessionId,
        sessionRef.current.guestCode,
      );
      sessionRef.current = { sessionId, guestCode: guestCode ?? sessionRef.current.guestCode };
      setMessages((prev) => [...prev, answerMessage, ...(checklistMessage ? [checklistMessage] : [])]);
    } catch (e) {
      console.error(e);
      const message = e instanceof Error ? e.message : '요청 중 오류가 발생했습니다.';
      setMessages((prev) => [
        ...prev,
        { id: nextChatMessageId(), type: 'answer', segments: [{ text: message, sourceIndexes: [] }], sources: [] },
      ]);
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

  // 체크한 항목을 서버에 제출하고, 그 결과를 반영한 재생성 답변을 이어서 받는다.
  const submitChecklist = useCallback(
    async (messageId: string) => {
      const target = messages.find(
        (m): m is Extract<ChatMessage, { type: 'checklist' }> => m.id === messageId && m.type === 'checklist',
      );
      if (!target || target.answered) return;

      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, answered: true } : m)));
      setLoading(true);
      try {
        const checkedLabels = target.items.filter((it) => it.checked).map((it) => it.label);
        await submitChecklistAnswers(target.consultationId, checkedLabels, sessionRef.current.guestCode);
        const regenerated = await regenerateAnswer(target.consultationId, sessionRef.current.guestCode);
        setMessages((prev) => [...prev, regenerated]);
      } catch (e) {
        console.error(e);
        const message = e instanceof Error ? e.message : '요청 중 오류가 발생했습니다.';
        setMessages((prev) => [...prev, { id: nextChatMessageId(), type: 'regenerated', message, sources: [], disclaimer: '' }]);
      } finally {
        setLoading(false);
      }
    },
    [messages],
  );

  return { messages, loading, sendText, toggleChecklistItem, submitChecklist };
}
