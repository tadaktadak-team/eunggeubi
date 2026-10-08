// 의약품 제품 허가정보의 허가 상태(CANCEL_NAME) → 화면 배지 문구.
// null/undefined(아직 확인 못 함)와 "정상"은 배지를 안 보여준다. 집에 남아있는 예전 약을 검색하는
// 경우가 있어 숨기지 않고 상태만 알려주는 방식이라, 문구는 "판매 중단"처럼 단정하지 않고
// 허가 상태 그대로 쓴다(유효기간만료는 위험한 약이라는 뜻이 아니다).
export function getPermitStatusLabel(cancelName?: string | null): string | null {
  if (!cancelName || cancelName === '정상') return null;
  switch (cancelName) {
    case '취하':
      return '허가 취하';
    case '유효기간만료':
      return '허가 만료';
    case '행정(취소)':
    case '취소':
      return '허가 취소';
    case '폐업':
      return '업체 폐업';
    default:
      return `허가 ${cancelName}`;
  }
}

// 전문의약품 구분. 상세 화면의 본문 숨김, 목록의 효능 미리보기 숨김이 같은 기준을 써야 해서 한 곳에 둔다.
export function isPrescriptionDrug(drugType?: string | null): boolean {
  return drugType === '전문의약품' || drugType === '전문,희귀';
}
