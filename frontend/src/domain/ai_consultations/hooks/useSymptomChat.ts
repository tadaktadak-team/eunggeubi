import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useRef, useState } from 'react';

import {
  nextChatMessageId,
  regenerateAnswer,
  requestSymptomAdvice,
  submitChecklistAnswers,
} from '../api/aiConsultations';
import { getGuestCode, saveGuestCode } from '../../../shared/storage/guestCodeStorage';
import { getConsultationDetail } from '../../mypage/api/consultation';
import { ChatMessage } from '../types';

export function useSymptomChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  // 상담 세션을 이어가기 위한 값들. 화면(훅 인스턴스)이 살아있는 동안 대화 전체에서 공유한다.
  const sessionRef = useRef<{ sessionId?: string; guestCode?: string }>({});
  // 비회원 무료 상담 횟수를 다 써서 서버가 403을 준 상태 - 화면이 회원가입 안내 시트를 띄운다.
  const [guestLimitReached, setGuestLimitReached] = useState(false);
  const [limitSheetVisible, setLimitSheetVisible] = useState(false);
  // 안내 시트에서 가입/로그인하고 돌아오면 더 이상 막으면 안 된다 - 화면에 돌아올 때마다 풀어준다.
  // 여전히 비회원이면 다음 전송에서 서버가 다시 403을 주고 시트가 다시 뜬다.
  useFocusEffect(useCallback(() => setGuestLimitReached(false), []));

  // 사용자 텍스트를 말풍선으로 추가하고, AI 응답(+있으면 체크리스트)을 이어서 붙인다.
  // false = 비회원 한도 초과로 거절됨(질문이 저장도 안 됐으니 화면이 입력창에 되돌려 놓는다).
  const sendText = useCallback(async (text: string): Promise<boolean> => {
    const trimmed = text.trim();
    if (!trimmed) return true;

    setMessages((prev) => [...prev, { id: nextChatMessageId(), type: 'user', text: trimmed }]);
    setLoading(true);
    try {
      // 기기에 저장된 guestCode를 세션이 바뀌어도 계속 쓴다(횟수 제한/가입 시 기록 이전의 기준).
      // 로그인 상태면 서버가 무시한다.
      // 메모리(sessionRef)보다 저장소가 우선 - 가입하면 저장소만 비워지므로, 메모리 값을 먼저 쓰면
      // 이미 계정으로 이전된 옛 코드를 계속 보내게 된다.
      const storedGuestCode = (await getGuestCode()) ?? undefined;
      const { answerMessage, checklistMessage, sessionId, guestCode } = await requestSymptomAdvice(
        trimmed,
        sessionRef.current.sessionId,
        storedGuestCode,
      );
      if (guestCode) await saveGuestCode(guestCode);
      sessionRef.current = { sessionId, guestCode: guestCode ?? storedGuestCode };
      setMessages((prev) => [...prev, answerMessage, ...(checklistMessage ? [checklistMessage] : [])]);
    } catch (e) {
      // 403 전체가 아니라 서버가 한도 초과로 표시한 것만 - 다른 403까지 가입 안내로 새지 않게.
      if ((e as { code?: string }).code === 'GUEST_LIMIT') {
        // 답변 못 받은 질문 말풍선은 치운다 - 저장도 안 됐다.
        setMessages((prev) => prev.slice(0, -1));
        setGuestLimitReached(true);
        setLimitSheetVisible(true);
        return false;
      }
      console.error(e);
      const message = e instanceof Error ? e.message : '요청 중 오류가 발생했습니다.';
      setMessages((prev) => [
        ...prev,
        { id: nextChatMessageId(), type: 'answer', segments: [{ text: message, sourceIndexes: [] }], sources: [] },
      ]);
    } finally {
      setLoading(false);
    }
    return true;
  }, []);

  const toggleChecklistItem = useCallback((messageId: string, itemId: string) => {
    setMessages((prev) =>
      prev.map((m) =>
        m.id === messageId && m.type === 'checklist'
          ? { ...m, items: m.items.map((it) => (it.id === itemId ? { ...it, checked: !it.checked } : it)) }
          : m,
      ),
    );
  }, []);

  // checkedLabels를 서버에 제출하고, 그 결과를 반영한 재생성 답변을 이어서 받는다. submitChecklist(현재
  // 체크된 항목 그대로)와 submitChecklistNone("해당사항 없음" - 체크 여부와 상관없이 빈 응답)이 공유한다.
  const finishChecklist = useCallback(
    async (messageId: string, checkedLabels: string[]) => {
      const target = messages.find(
        (m): m is Extract<ChatMessage, { type: 'checklist' }> => m.id === messageId && m.type === 'checklist',
      );
      if (!target || target.answered) return;

      // 체크박스도 실제로 제출하는 값과 맞춰둔다 - "해당사항 없음"을 누르면 화면상 체크도 다 풀린다.
      setMessages((prev) =>
        prev.map((m) =>
          m.id === messageId && m.type === 'checklist'
            ? { ...m, answered: true, items: m.items.map((it) => ({ ...it, checked: checkedLabels.includes(it.label) })) }
            : m,
        ),
      );
      setLoading(true);
      try {
        await submitChecklistAnswers(target.consultationId, checkedLabels, sessionRef.current.guestCode);
        const regenerated = await regenerateAnswer(target.consultationId, sessionRef.current.guestCode);
        setMessages((prev) => [...prev, regenerated]);
      } catch (e) {
        // 비회원은 답변당 재생성 1회까지다 - 넘으면 에러 말풍선 대신 가입 안내를 띄우고, 가입 후 다시
        // 답변할 수 있게 체크리스트는 풀어둔다.
        if ((e as { code?: string }).code === 'GUEST_LIMIT') {
          setMessages((prev) => prev.map((m) => (m.id === messageId && m.type === 'checklist' ? { ...m, answered: false } : m)));
          setGuestLimitReached(true);
          setLimitSheetVisible(true);
          return;
        }
        console.error(e);
        const message = e instanceof Error ? e.message : '요청 중 오류가 발생했습니다.';
        // answered를 다시 false로 되돌려서 재시도(다시 "답변하기")할 수 있게 한다 - 안 그러면
        // 실패한 채로 체크리스트가 영구히 잠긴다. 체크된 항목은 이미 시도한 값과 같으니 그대로 둔다.
        setMessages((prev) => [
          ...prev.map((m) => (m.id === messageId && m.type === 'checklist' ? { ...m, answered: false } : m)),
          { id: nextChatMessageId(), type: 'regenerated', message, sources: [], disclaimer: '' },
        ]);
      } finally {
        setLoading(false);
      }
    },
    [messages],
  );

  const submitChecklist = useCallback(
    (messageId: string) => {
      const target = messages.find(
        (m): m is Extract<ChatMessage, { type: 'checklist' }> => m.id === messageId && m.type === 'checklist',
      );
      if (!target) return;
      return finishChecklist(messageId, target.items.filter((it) => it.checked).map((it) => it.label));
    },
    [messages, finishChecklist],
  );

  const submitChecklistNone = useCallback(
    (messageId: string) => finishChecklist(messageId, []),
    [finishChecklist],
  );

  // 지난 상담을 불러와 이어간다. 다음 질문부터 이 sessionId로 보내면 서버가 이전 대화를 맥락으로 붙여 답한다.
  // 상세 API는 본문만 줘서 지난 답변은 출처·응급처치 바로보기 없이 글만 보인다. 답 안 한 체크리스트도
  // 다시 띄울 수 없어 빼고, 답한 체크리스트는 고른 항목을 사용자 말풍선으로 보여준다(상담 이력 상세와 같은 방식).
  const resume = useCallback(async (sessionId: string) => {
    setLoading(true);
    try {
      const detail = await getConsultationDetail(sessionId);
      sessionRef.current = { ...sessionRef.current, sessionId };
      setMessages(
        detail.messages.flatMap((m): ChatMessage[] => {
          if (m.senderType === 'USER') return [{ id: nextChatMessageId(), type: 'user', text: m.content }];
          const answer: ChatMessage = {
            id: nextChatMessageId(),
            type: 'regenerated',
            message: m.content,
            sources: [],
            disclaimer: m.disclaimer ?? '',
          };
          if (!m.checklistAnswered) return [answer];
          const picked = m.checkedItems.length > 0 ? m.checkedItems.join(', ') : '해당하는 항목이 없어요';
          return [answer, { id: nextChatMessageId(), type: 'user', text: picked }];
        }),
      );
    } catch (e) {
      console.error(e);
      // 못 불러와도 새 상담으로는 쓸 수 있게 안내만 남긴다.
      setMessages([{ id: nextChatMessageId(), type: 'regenerated', message: '지난 상담을 불러오지 못했어요. 새로 질문해주세요.', sources: [], disclaimer: '' }]);
    } finally {
      setLoading(false);
    }
  }, []);

  return { messages, loading, guestLimitReached, limitSheetVisible, setLimitSheetVisible, sendText, resume, toggleChecklistItem, submitChecklist, submitChecklistNone };
}
