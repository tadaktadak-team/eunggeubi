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

// 제형 키워드는 문자열 끝에 오거나, 그 뒤에 "용량(숫자+단위)"만 붙은 형태여야 한다
// ("산160밀리그램", "액 5%"). 숫자 하나만 보고 통과시키면 "가상산5mg정"처럼 성분 뒤에
// 용량이 오고 진짜 제형("정")이 뒤따르는 이름도 가루로 오탐되므로, 끝까지(`$`) 이어지는지 확인한다.
// 뒤에 다른 한글 글자가 더 이어지면(예: "염산염"의 "산" 다음 "염") 성분명 일부라 제형이 아니다.
const DOSE_UNITS = '밀리그램|밀리그람|마이크로그램|킬로그램|그램|단위|밀리리터|리터|mg|mcg|ug|g|ml|㎎|㎖|%';

function endsWithForm(core: string, keywords: string): boolean {
  return new RegExp(`(?:${keywords})(?:[0-9][0-9.,/~ -]*(?:${DOSE_UNITS})*)?$`).test(core);
}

export function getDrugFormIconName(name?: string): IconName {
  if (!name) return 'pill';
  const core = stripTrailingParens(name);
  if (endsWithForm(core, '시럽|액|물약')) return 'bottle-tonic';
  if (endsWithForm(core, '산|가루|과립')) return 'grain';
  if (endsWithForm(core, '연고|크림|겔|로션|패치|파스|점안|점비|스프레이')) return 'bandage';
  return 'pill';
}
