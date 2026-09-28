import type { ComponentProps } from 'react';
import { MaterialCommunityIcons } from '@expo/vector-icons';

type IconName = ComponentProps<typeof MaterialCommunityIcons>['name'];

// e약은요 API는 제형을 구분된 필드로 내려주지 않는다. 이미지가 없을 때 정제 아이콘만
// 보여주면 액상/가루/외용제도 알약처럼 보여 오해를 줄 수 있어, 약품명에 흔히 붙는
// 제형 키워드로 대략 추정한다. 정확한 분류가 목적이 아니라 아이콘 힌트 용도.
export function getDrugFormIconName(name?: string): IconName {
  if (!name) return 'pill';
  if (/시럽|액|물약/.test(name)) return 'bottle-tonic';
  if (/산|가루|과립/.test(name)) return 'grain';
  if (/연고|크림|겔|로션|패치|파스|점안|점비|스프레이/.test(name)) return 'bandage';
  return 'pill';
}
