import React, { createContext, useContext, useEffect, useState } from 'react';

import {
  clearTokens,
  getRefreshToken,
  saveTokens,
} from '../../../shared/storage/tokenStorage';
import * as authApi from '../api/auth';
import * as userApi from '../../user/api/user';

interface AuthContextValue {
  isLoggedIn: boolean;
  userId: number | null;
  loading: boolean; // 앱 시작 시 자동 로그인 확인 중 여부
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  withdraw: (password: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [userId, setUserId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  // 앱 시작 시: 저장된 refresh 토큰으로 자동 로그인 시도
  useEffect(() => {
    (async () => {
      try {
        const refresh = await getRefreshToken();
        if (refresh) {
          const res = await authApi.reissue(refresh);
          await saveTokens(res.accessToken, res.refreshToken);
          setUserId(res.userId);
        }
      } catch {
        await clearTokens(); // 만료/폐기된 토큰이면 정리
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const signIn = async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    await saveTokens(res.accessToken, res.refreshToken);
    setUserId(res.userId);
  };

  const signOut = async () => {
    try {
      const refresh = await getRefreshToken();
      if (refresh) await authApi.logout(refresh);
    } finally {
      await clearTokens();
      setUserId(null);
    }
  };
  const withdraw = async (password: string) => {
    await userApi.withdraw(password);
    await clearTokens();   // 기기 저장 토큰 삭제
    setUserId(null);       // 로그아웃 상태로 전환
  };

  return (
    <AuthContext.Provider
      value={{ isLoggedIn: userId !== null, userId, loading, signIn, signOut, withdraw }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있어요');
  return ctx;
}