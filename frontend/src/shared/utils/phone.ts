// 전화번호는 DB에 숫자만 저장한다(중복 조회·SMS 발송·본인인증 대조가 형식에 흔들리지 않도록).
// 화면에 보여줄 때와 입력 중에만 하이픈을 붙인다.

export const toDigits = (value: string) => value.replace(/\D/g, '');

// 01012345678 → 010-1234-5678 (입력 중에도 자리수에 맞춰 점진적으로 붙는다)
export function formatPhone(value: string) {
  const d = toDigits(value).slice(0, 11);
  if (d.length < 4) return d;
  if (d.length < 8) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
}
