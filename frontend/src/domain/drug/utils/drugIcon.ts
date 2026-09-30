import type { ComponentProps } from 'react';
import { MaterialCommunityIcons } from '@expo/vector-icons';

type IconName = ComponentProps<typeof MaterialCommunityIcons>['name'];

// e약은요 API는 제형을 구분된 필드로 내려주지 않는다. 이미지가 없을 때 정제 아이콘만
// 보여주면 액상/가루/외용제도 알약처럼 보여 오해를 줄 수 있어, 약품명에 흔히 붙는
// 제형 키워드로 대략 추정한다. 정확한 분류가 목적이 아니라 아이콘 힌트 용도.
//
// 약품명은 "제품명+제형+(성분명)" 구조라("염산염"처럼 성분명 괄호 안에 우연히 "산"
// 같은 글자가 끼어있으면 정제인데도 가루 아이콘으로 오탐될 수 있었다), 괄호를 전부
// 떼고 남은 문자열이 그 키워드로 "끝나는지"(포함이 아니라 접미사로)만 본다.
function stripTrailingParens(name: string): string {
  let stripped = name.trim();
  while (/\([^)]*\)\s*$/.test(stripped)) {
    stripped = stripped.replace(/\([^)]*\)\s*$/, '').trim();
  }
  return stripped;
}

// 제형 키워드 뒤에는 문자열이 끝나거나("정", "캡슐") 용량 숫자가 바로 붙는다
// ("산160밀리그램"). 뒤에 다른 한글 글자가 더 이어지면(예: "염산염"의 "산" 다음 "염")
// 성분명 일부일 뿐이라 제형이 아니다.
function endsWithForm(core: string, keywords: string): boolean {
  return new RegExp(`(${keywords})(?:[0-9]|$)`).test(core);
}

export function getDrugFormIconName(name?: string): IconName {
  if (!name) return 'pill';
  const core = stripTrailingParens(name);
  if (endsWithForm(core, '시럽|액|물약')) return 'bottle-tonic';
  if (endsWithForm(core, '산|가루|과립')) return 'grain';
  if (endsWithForm(core, '연고|크림|겔|로션|패치|파스|점안|점비|스프레이')) return 'bandage';
  return 'pill';
}
