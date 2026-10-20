import { fetch as expoFetch } from 'expo/fetch';

import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../storage/tokenStorage';
import { API_BASE_URL } from './config';

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

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
    return response;
  } finally {
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

// 토큰을 붙여 보내고, access 토큰 만료(401)면 재발급 후 딱 한 번만 재시도
async function sendWithAuth<R extends { status: number }>(
  options: RequestOptions,
  doSend: (token: string | null) => Promise<R>,
): Promise<R> {
  const token = options.auth ? await getAccessToken() : null;
  const response = await doSend(token);
  if (response.status !== 401 || !options.auth) return response;

  const newToken = await refreshAccessToken();
  if (newToken) return doSend(newToken);

  //refresh까지 실패 = 세션 종료, 토큰 정리 후 앱에 알림
  await clearTokens();
  onSessionExpired?.();
  throw new Error('세션이 만료되었어요. 다시 로그인해주세요.');
}

// 실패 응답을 백엔드 에러 형식 { "message": "...", "code": "..." } 그대로 Error로 바꾼다
function toApiError(status: number, data: { message?: string; code?: string } | undefined) {
  const message = (data && data.message) || '요청에 실패했습니다.';
  return Object.assign(new Error(message), { status, code: data?.code });
}

async function request<T>(
  method: Method,
  path: string,
  body?: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const response = await sendWithAuth(options, (token) => send(method, path, body, token));

  //204(No Content) 등 본문 없는 응답 대비
  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) throw toApiError(response.status, data);

  return data as T;
}

// SSE(text/event-stream)로 POST하고 이벤트가 올 때마다 onEvent(이름, JSON 데이터)를 부른다. 스트림이 끝나면
// 끝난다. RN 기본 fetch는 응답 본문을 스트림으로 못 읽어서 expo/fetch를 쓴다. 답변 생성이 오래 걸릴 수
// 있어 일반 요청의 15초 타임아웃은 걸지 않는다(서버가 제한 시간에 끊는다).
async function postStream(
  path: string,
  body: unknown,
  onEvent: (event: string, data: unknown) => void,
  options: RequestOptions = {},
): Promise<void> {
  const response = await sendWithAuth(options, (token) =>
    expoFetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
    }),
  );

  if (!response.ok) {
    const text = await response.text();
    throw toApiError(response.status, text ? JSON.parse(text) : undefined);
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
    // 이벤트는 빈 줄로 끝난다. 마지막 조각은 아직 덜 왔을 수 있어 남겨둔다.
    const events = buffer.split('\n\n');
    buffer = events.pop() ?? '';
    for (const raw of events) {
      let event = 'message';
      const data: string[] = [];
      for (const line of raw.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim();
        else if (line.startsWith('data:')) data.push(line.slice(5));
      }
      if (data.length > 0) onEvent(event, JSON.parse(data.join('\n')));
    }
  }
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, body, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, body, options),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, undefined, options),
  postStream,
};


