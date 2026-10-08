// 백엔드 PasswordValidator와 같은 규칙.
// 한쪽만 고치면 어긋나니 반드시 같이 수정할 것.

const MIN_LENGTH = 8;
const MAX_LENGTH = 20;
const MAX_REPEAT = 3;
const MAX_SEQUENCE = 3;
const MIN_TOKEN_LENGTH = 4;
const SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?~`";

export const PASSWORD_RULE_TEXT = '8~20자, 영문·숫자·특수문자 중 2종 이상';

export type PasswordCheck = { ok: true } | { ok: false; message: string };

const isAlpha = (c: string) => (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
const isDigit = (c: string) => c >= '0' && c <= '9';
const isSpecial = (c: string) => SPECIAL_CHARS.includes(c);

const countCharTypes = (password: string) => {
  let alpha = false;
  let digit = false;
  let special = false;
  for (const c of password) {
    if (isAlpha(c)) alpha = true;
    else if (isDigit(c)) digit = true;
    else if (isSpecial(c)) special = true;
  }
  return Number(alpha) + Number(digit) + Number(special);
};

/** aaa, 111 처럼 같은 문자가 MAX_REPEAT번 연속되는지 */
const hasRepeatedChars = (password: string) => {
  let run = 1;
  for (let i = 1; i < password.length; i++) {
    run = password[i] === password[i - 1] ? run + 1 : 1;
    if (run >= MAX_REPEAT) return true;
  }
  return false;
};

/** abc, 123 (오름차순) / cba, 321 (내림차순) 둘 다 차단 */
const hasSequentialChars = (password: string) => {
  let ascending = 1;
  let descending = 1;
  for (let i = 1; i < password.length; i++) {
    const prev = password[i - 1];
    const curr = password[i];
    // 같은 문자 종류끼리만 연속으로 판단 ('9'→':' 를 연속으로 보지 않도록)
    const sameType = (isAlpha(prev) && isAlpha(curr)) || (isDigit(prev) && isDigit(curr));
    const diff = sameType ? curr.charCodeAt(0) - prev.charCodeAt(0) : 0;
    ascending = diff === 1 ? ascending + 1 : 1;
    descending = diff === -1 ? descending + 1 : 1;
    if (ascending >= MAX_SEQUENCE || descending >= MAX_SEQUENCE) return true;
  }
  return false;
};

export function validatePassword(
  password: string,
  userInfo?: { email?: string; phone?: string },
): PasswordCheck {
  if (password.length < MIN_LENGTH || password.length > MAX_LENGTH) {
    return { ok: false, message: `비밀번호는 ${MIN_LENGTH}~${MAX_LENGTH}자여야 합니다.` };
  }
  if (![...password].every((c) => isAlpha(c) || isDigit(c) || isSpecial(c))) {
    return { ok: false, message: '비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다.' };
  }
  if (countCharTypes(password) < 2) {
    return { ok: false, message: '비밀번호는 영문, 숫자, 특수문자 중 2종류 이상을 조합해야 합니다.' };
  }
  if (hasRepeatedChars(password)) {
    return { ok: false, message: `같은 문자를 ${MAX_REPEAT}번 이상 연속해서 사용할 수 없습니다.` };
  }
  if (hasSequentialChars(password)) {
    return { ok: false, message: `연속된 문자나 숫자를 ${MAX_SEQUENCE}자 이상 사용할 수 없습니다.` };
  }

  const lower = password.toLowerCase();

  if (userInfo?.email) {
    const at = userInfo.email.indexOf('@');
    const localPart = (at > 0 ? userInfo.email.slice(0, at) : userInfo.email).toLowerCase();
    if (localPart.length >= MIN_TOKEN_LENGTH && lower.includes(localPart)) {
      return { ok: false, message: '비밀번호에 이메일 아이디를 포함할 수 없습니다.' };
    }
  }

  if (userInfo?.phone) {
    const digits = userInfo.phone.replace(/\D/g, '');
    for (let i = 0; i + MIN_TOKEN_LENGTH <= digits.length; i++) {
      if (lower.includes(digits.slice(i, i + MIN_TOKEN_LENGTH))) {
        return { ok: false, message: '비밀번호에 전화번호를 포함할 수 없습니다.' };
      }
    }
  }

  return { ok: true };
}