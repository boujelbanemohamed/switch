import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import type { AuthUser, LoginRequest, LoginResponse, RegisterRequest } from '../types';

interface AuthContextType {
  token: string | null;
  refreshTokenValue: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (req: LoginRequest) => Promise<boolean>;
  completeMfa: (code: string) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<void>;
  fetchUsers: () => Promise<AuthUser[]>;
  updateUser: (id: string, data: Partial<AuthUser>) => Promise<AuthUser>;
  deleteUser: (id: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

function decodeTokenPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    return JSON.parse(atob(parts[1]));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [refreshTokenValue, setRefreshTokenValue] = useState<string | null>(() => localStorage.getItem('refreshToken'));
  const [mfaUsername, setMfaUsername] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      const payload = decodeTokenPayload(token);
      if (payload && payload.sub && payload.role) {
        const authUser: AuthUser = {
          id: payload.sub as string,
          username: payload.sub as string,
          email: (payload.email as string) || '',
          displayName: (payload.displayName as string) || (payload.sub as string),
          role: payload.role as AuthUser['role'],
          enabled: true,
          lastLogin: undefined,
          createdAt: '',
        };
        setUser(authUser);
        localStorage.setItem('user', JSON.stringify(authUser));
      }
    }
    setLoading(false);
  }, [token]);

  const setAuthState = useCallback((data: LoginResponse) => {
    setToken(data.accessToken);
    setRefreshTokenValue(data.refreshToken);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    const payload = decodeTokenPayload(data.accessToken);
    if (payload) {
      const authUser: AuthUser = {
        id: payload.sub as string,
        username: data.username,
        email: data.email,
        displayName: data.displayName || data.username,
        role: data.role as AuthUser['role'],
        enabled: true,
        lastLogin: undefined,
        createdAt: '',
      };
      setUser(authUser);
      localStorage.setItem('user', JSON.stringify(authUser));
    }
  }, []);

  const login = useCallback(async (req: LoginRequest): Promise<boolean> => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || 'Login failed');
    }
    const data: LoginResponse = await res.json();
    if (data.mfaRequired) {
      setMfaUsername(data.username);
      return true;
    }
    setAuthState(data);
    return false;
  }, [setAuthState]);

  const completeMfa = useCallback(async (code: string) => {
    if (!mfaUsername) throw new Error('No MFA pending');
    const res = await fetch('/api/v1/auth/mfa/authenticate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: mfaUsername, code }),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || 'MFA verification failed');
    }
    const data: LoginResponse = await res.json();
    setMfaUsername(null);
    setAuthState(data);
  }, [mfaUsername, setAuthState]);

  const register = useCallback(async (req: RegisterRequest) => {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || 'Registration failed');
    }
  }, [token]);

  const logout = useCallback(() => {
    setToken(null);
    setRefreshTokenValue(null);
    setUser(null);
    setMfaUsername(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  }, []);

  const refreshAccessToken = useCallback(async () => {
    if (!refreshTokenValue) {
      logout();
      return;
    }
    try {
      const res = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: refreshTokenValue }),
      });
      if (!res.ok) throw new Error('Refresh failed');
      const data: LoginResponse = await res.json();
      setToken(data.accessToken);
      setRefreshTokenValue(data.refreshToken);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
    } catch {
      logout();
    }
  }, [refreshTokenValue, logout]);

  const authFetch = useCallback(async (path: string, options?: RequestInit) => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options?.headers as Record<string, string>),
    };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/v1${path}`, { ...options, headers });
    if (res.status === 401 && refreshTokenValue) {
      await refreshAccessToken();
      const newToken = localStorage.getItem('accessToken');
      if (newToken) {
        headers['Authorization'] = `Bearer ${newToken}`;
        const retryRes = await fetch(`/api/v1${path}`, { ...options, headers });
        if (!retryRes.ok) {
          const err = await retryRes.text();
          throw new Error(`API error ${retryRes.status}: ${err}`);
        }
        return retryRes.json();
      }
    }
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`API error ${res.status}: ${err}`);
    }
    return res.json();
  }, [token, refreshTokenValue, refreshAccessToken]);

  const fetchUsers = useCallback(async () => {
    return authFetch('/auth/users') as Promise<AuthUser[]>;
  }, [authFetch]);

  const updateUser = useCallback(async (id: string, data: Partial<AuthUser>) => {
    return authFetch(`/auth/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }) as Promise<AuthUser>;
  }, [authFetch]);

  const deleteUser = useCallback(async (id: string) => {
    await authFetch(`/auth/users/${id}`, { method: 'DELETE' });
  }, [authFetch]);

  return (
    <AuthContext.Provider value={{
      token, refreshTokenValue, user, isAuthenticated: !!token && !mfaUsername,
      loading, login, register, logout, refreshAccessToken,
      fetchUsers, updateUser, deleteUser, completeMfa,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
