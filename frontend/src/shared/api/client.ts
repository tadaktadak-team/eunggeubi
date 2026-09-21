import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../storage/tokenStorage';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

interface RequestOptions {
  auth?: boolean; //true면 access 토큰을 자동으로 헤더에 첨부
}

// 세션 종료를 앱에 알리는 통로 (AuthProvider가 콜백 등록)
let onSessionExpired: (() => void) | null = null;

export function setOnSessionExpired(handler: (() => void) | null) {
  onSessionExpired = handler;
}

// 진행 중인 재발급. refresh는 한 번 쓰면 폐기(회전)되므로 동시 재발급을 막는다(single-flight)
let refreshPromise: Promise<string | null> | null = null;

// 요청 1회. 재시도 때 그대로 다시 부르기 위해 분리
async function send(method: Method, path: string, body: unknown, token: string | null) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  // ▼▼▼ 여기 2줄 추가 ▼▼▼
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 35000);
  // ▲▲▲ 여기까지 추가 ▲▲▲

  // ▼ 이 1줄 추가
  console.log('[api] 요청 시작:', `${API_BASE_URL}${path}`);

  // ▼ 원래는 이 자리에 그냥 "return fetch(...)"가 있었음
  //   그걸 try/finally로 감싸는 것으로 변경
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal, // ← fetch 옵션 안에 이 줄 추가
    });
    console.log('[api] 응답 받음:', response.status); // ← 이 1줄 추가
    return response;
  } finally {
    // ▼ 이 블록 추가
    clearTimeout(timeoutId);
  }
}

// refresh로 새 access 발급 (순환 import 때문에 여기에 배치)
async function requestNewToken(): Promise<string | null> {
  try {
    const refreshToken = await getRefreshToken();
    if (!refreshToken) return null;

    const response = await send('POST', '/api/auth/reissue', { refreshToken }, null);
    if (!response.ok) return null; //refresh도 만료/폐기됨

    const data = await response.json();
    await saveTokens(data.accessToken, data.refreshToken); //회전된 refresh도 함께 저장
    return data.accessToken as string;
  } catch {
    return null; //네트워크 오류 등
  }
}

// 이미 진행 중이면 그 결과를 함께 기다린다 (요청은 1회)
function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = requestNewToken().finally(() => {
      refreshPromise = null; //비워야 다음 만료 때 재시도 가능
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

  //access 토큰 만료(401)면 재발급 후 딱 한 번만 재시도
  if (response.status === 401 && options.auth) {
    const newToken = await refreshAccessToken();

    if (newToken) {
      response = await send(method, path, body, newToken);
    } else {
      //refresh까지 실패 = 세션 종료, 토큰 정리 후 앱에 알림
      await clearTokens();
      onSessionExpired?.();
      throw new Error('세션이 만료되었어요. 다시 로그인해주세요.');
    }
  }

  //204(No Content) 등 본문 없는 응답 대비
  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    //백엔드 에러 형식 { "message": "..." } 에서 메시지 추출
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


