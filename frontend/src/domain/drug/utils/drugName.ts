// 약품명은 "제품명+제형+용량(성분명)" 형태가 많아서(예: "인데놀정10mg(프로프라놀롤염산염)"),
// 상세 화면 제목에 그대로 쓰면 괄호 부분 때문에 줄이 어색한 위치에서 꺾인다. 이름 끝에 붙은 괄호
// 묶음을 떼어내 둘째 줄로 내릴 수 있게 나눈다.
//
// - 소괄호 ()와 대괄호 []를 모두 처리한다(예: "나노크라정375mg[아목시실린,클라불란산칼륨(2:1)]").
// - 괄호 안에 괄호가 있어도 짝을 맞춰 처리한다(예: "가스칸정(애엽95%에탄올연조엑스(20→1))(수출용)"
//   → "가스칸정" / "(애엽95%에탄올연조엑스(20→1))(수출용)").
// - 괄호 묶음이 여러 개 연달아 붙어 있으면 모두 둘째 줄로 보낸다.
// - 괄호가 이름 중간에만 있거나, 짝이 맞지 않거나, 떼어내면 제품명이 비는 경우에는 나누지 않는다.
export interface SplitDrugName {
  main: string;
  sub: string | null;
}

const OPEN_OF: Record<string, string> = { ')': '(', ']': '[' };

export function splitDrugName(name: string): SplitDrugName {
  const text = name.trim();
  let cut = text.length;

  for (;;) {
    let close = cut - 1;
    while (close >= 0 && text[close] === ' ') close--; // 괄호 묶음 사이의 공백은 허용
    const closeChar = close >= 0 ? text[close] : '';
    const openChar = OPEN_OF[closeChar];
    if (!openChar) break;

    // 같은 종류의 괄호만 세서 짝을 찾는다(대괄호 안의 소괄호 등 다른 종류는 무시).
    let depth = 0;
    let open = -1;
    for (let i = close; i >= 0; i--) {
      if (text[i] === closeChar) {
        depth++;
      } else if (text[i] === openChar) {
        depth--;
        if (depth === 0) {
          open = i;
          break;
        }
      }
    }
    // 짝이 없거나(open < 0), 이름 전체가 괄호로 시작하면(open === 0) 더 떼지 않는다.
    if (open <= 0) break;
    cut = open;
  }

  const main = text.slice(0, cut).trim();
  const sub = text.slice(cut).trim();
  if (!main || !sub) return { main: text, sub: null };
  return { main, sub };
}
