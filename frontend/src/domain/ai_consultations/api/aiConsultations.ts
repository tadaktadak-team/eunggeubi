import { ChatMessage } from '../types';

// TODO: 백엔드 AI 상담 API 연동 전까지 사용하는 목업입니다.
// 실제 연동 시 emergency/api/emergency.ts 처럼
// `api.post<...>('/api/ai-consultations/messages', { text }, { auth: true })` 형태로 교체하세요.

interface SymptomKnowledge {
  reference: { title: string; body: string; source: string };
  checklistTitle: string;
  checklistItems: string[];
}

const KNOWLEDGE: Record<string, SymptomKnowledge> = {
  두통: {
    reference: {
      title: '참고 정보예요',
      body: '두통은 긴장성, 편두통, 군발성 등 다양한 원인으로 알려져 있어요. 대부분 휴식으로 호전된다고 알려져 있지만, 갑작스럽고 심한 두통은 주의가 필요해요.',
      source: '질병관리청 가이드라인',
    },
    checklistTitle: '몇 가지 확인해볼게요',
    checklistItems: ['발열이 동반되나요?', '구토가 있나요?', '갑자기 심해졌나요?'],
  },
  복통: {
    reference: {
      title: '참고 정보예요',
      body: '복통은 소화불량, 장염, 생리통 등 흔한 원인부터 주의가 필요한 원인까지 다양해요. 통증 위치와 양상에 따라 원인이 크게 달라질 수 있어요.',
      source: '질병관리청 가이드라인',
    },
    checklistTitle: '몇 가지 확인해볼게요',
    checklistItems: ['통증이 배 전체가 아닌 한쪽에 집중되나요?', '구토나 설사가 있나요?', '식은땀이 날 정도로 심한가요?'],
  },
  발열: {
    reference: {
      title: '참고 정보예요',
      body: '발열은 감염에 대한 우리 몸의 정상적인 반응인 경우가 많아요. 수분을 충분히 섭취하고 휴식하면 대부분 호전되지만, 고열이 지속되면 진료가 필요해요.',
      source: '질병관리청 가이드라인',
    },
    checklistTitle: '몇 가지 확인해볼게요',
    checklistItems: ['체온이 38도 이상인가요?', '하루 이상 열이 지속되나요?', '오한이나 근육통이 동반되나요?'],
  },
  어지럼: {
    reference: {
      title: '참고 정보예요',
      body: '어지럼은 이석증 같은 귀 문제, 저혈압, 빈혈 등 다양한 원인으로 나타날 수 있어요. 대부분 크게 위험하지 않지만 반복되면 원인 확인이 필요해요.',
      source: '질병관리청 가이드라인',
    },
    checklistTitle: '몇 가지 확인해볼게요',
    checklistItems: ['빙빙 도는 느낌인가요?', '두통이나 이명이 동반되나요?', '갑자기 쓰러질 뻔했나요?'],
  },
};

const DEFAULT_KNOWLEDGE: SymptomKnowledge = {
  reference: {
    title: '참고 정보예요',
    body: '입력하신 증상에 대한 일반적인 참고 정보를 안내해드릴게요. 증상이 계속되거나 심해지면 병원 진료를 받아보세요.',
    source: '질병관리청 가이드라인',
  },
  checklistTitle: '몇 가지 확인해볼게요',
  checklistItems: ['증상이 하루 이상 지속되나요?', '다른 통증이 동반되나요?', '갑자기 심해졌나요?'],
};

function findKnowledge(text: string): SymptomKnowledge {
  const hitKeyword = Object.keys(KNOWLEDGE).find((keyword) => text.includes(keyword));
  return hitKeyword ? KNOWLEDGE[hitKeyword] : DEFAULT_KNOWLEDGE;
}

let idSeq = 0;
function nextId() {
  idSeq += 1;
  return `msg-${Date.now()}-${idSeq}`;
}

// 증상 텍스트를 보내면 [참고정보 카드, 체크리스트 카드]를 응답으로 준다.
export function requestSymptomAdvice(text: string): Promise<ChatMessage[]> {
  const knowledge = findKnowledge(text);

  return new Promise((resolve) => {
    setTimeout(() => {
      resolve([
        {
          id: nextId(),
          type: 'reference',
          title: knowledge.reference.title,
          body: knowledge.reference.body,
          source: knowledge.reference.source,
        },
        {
          id: nextId(),
          type: 'checklist',
          title: knowledge.checklistTitle,
          items: knowledge.checklistItems.map((label) => ({ id: nextId(), label, checked: false })),
          answered: false,
        },
      ]);
    }, 500); // 실제 응답을 기다리는 느낌을 주기 위한 지연
  });
}

export function nextChatMessageId() {
  return nextId();
}
