import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../storage/tokenStorage';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

interface RequestOptions {
  auth?: boolean; // true면 access 토큰을 자동으로 헤더에 첨부
}

// 재발급까지 실패했을 때(= 세션 종료) 앱에 알리는 통로.
// client.ts는 React 밖이라 화면 전환을 직접 못 하므로, AuthProvider가 콜백을 등록해 둔다.
let onSessionExpired: (() => void) | null = null;

export function setOnSessionExpired(handler: (() => void) | null) {
  onSessionExpired = handler;
}

// 진행 중인 재발급 요청. 동시에 여러 요청이 401을 받아도 재발급은 한 번만 나가게 한다(single-flight).
// 우리 서버는 refresh 토큰을 한 번 쓰면 폐기(회전)하므로, 동시에 여러 번 재발급하면
// 두 번째부터 "이미 폐기된 토큰"으로 실패해 오히려 강제 로그아웃이 된다.
let refreshPromise: Promise<string | null> | null = null;

// 실제 요청 한 번. 재시도할 때 그대로 다시 부를 수 있도록 분리했다.
async function send(method: Method, path: string, body: unknown, token: string | null) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  return fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

// refresh 토큰으로 새 access 토큰을 발급(순환 import 문제로 여기에 배치)
async function requestNewToken(): Promise<string | null> {
  try {
    const refreshToken = await getRefreshToken();
    if (!refreshToken) return null;

    const response = await send('POST', '/api/auth/reissue', { refreshToken }, null);
    if (!response.ok) return null; // refresh도 만료/폐기됨

    const data = await response.json();
    await saveTokens(data.accessToken, data.refreshToken); // 회전된 refresh도 함께 저장
    return data.accessToken as string;
  } catch {
    return null; // 네트워크 오류 등
  }
}

// 재발급이 이미 진행 중이면 그 결과를 함께 기다린다(요청은 1회만 발생).
function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = requestNewToken().finally(() => {
      refreshPromise = null; // 성공/실패와 무관하게 비워야 다음 만료 때 다시 시도할 수 있다
    });
  }
  return refreshPromise;
}

async function request<T>(
  method: Method,
  path: string,
  body?: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const token = options.auth ? await getAccessToken() : null;
  let response = await send(method, path, body, token);

  // access 토큰 만료(401)면 재발급 후 딱 한 번만 재시도한다.
  if (response.status === 401 && options.auth) {
    const newToken = await refreshAccessToken();

    if (newToken) {
      response = await send(method, path, body, newToken);
    } else {
      // refresh까지 실패 = 세션 종료. 토큰을 정리하고 앱에 알린다.
      await clearTokens();
      onSessionExpired?.();
      throw new Error('세션이 만료되었어요. 다시 로그인해주세요.');
    }
  }

  // 204(No Content) 등 본문 없는 응답 대비
  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    // 백엔드 에러 형식 { "message": "..." } 에서 메시지 추출
    const message = (data && data.message) || '요청에 실패했습니다.';
    throw new Error(message);
  }

  return data as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, body, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, body, options),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, undefined, options),
};