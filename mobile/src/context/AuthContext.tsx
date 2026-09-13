import React, { createContext, useState, useEffect, useCallback } from 'react';
import {
  AuthState,
  AuthContextType,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
} from '../types/auth';
import {
  saveSecureToken,
  getSecureToken,
  saveSecureUser,
  getSecureUser,
  clearSecureSession,
} from '../storage/secureStorage';
import { loginUser, registerUser, fetchCurrentUser } from '../api/endpoints/authApi';
import { apiClient } from '../api/client';
import { AppError } from '../types/api';

const initialAuthState: AuthState = {
  status: 'restoring',
  user: null,
  token: null,
  error: null,
};

export const AuthContext = createContext<AuthContextType>({
  ...initialAuthState,
  login: async () => {},
  register: async () => ({ message: '', userId: '', name: '', email: '', createdAt: '' }),
  logout: async () => {},
  restoreSession: async () => {},
  clearError: () => {},
});

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>(initialAuthState);

  const logout = useCallback(async () => {
    apiClient.setAuthToken(null);
    await clearSecureSession();
    setState({
      status: 'unauthenticated',
      user: null,
      token: null,
      error: null,
    });
  }, []);

  const restoreSession = useCallback(async () => {
    setState((prev) => ({ ...prev, status: 'restoring', error: null }));
    try {
      const storedToken = await getSecureToken();
      if (!storedToken) {
        setState({
          status: 'unauthenticated',
          user: null,
          token: null,
          error: null,
        });
        return;
      }

      apiClient.setAuthToken(storedToken);
      const user = await fetchCurrentUser();
      await saveSecureUser(user);

      setState({
        status: 'authenticated',
        user,
        token: storedToken,
        error: null,
      });
    } catch (err: any) {
      // Failed to restore session (token expired, revoked, or server 401)
      const cachedUser = await getSecureUser();
      const isNetworkErr = err?.code === 'NETWORK_ERROR' || err?.code === 'TIMEOUT_ERROR';
      const storedToken = await getSecureToken();

      if (isNetworkErr && cachedUser && storedToken) {
        // Maintain cached session when offline
        setState({
          status: 'authenticated',
          user: cachedUser,
          token: storedToken,
          error: null,
        });
      } else {
        await logout();
      }
    }
  }, [logout]);

  useEffect(() => {
    apiClient.setUnauthorizedHandler(() => {
      logout();
    });
    restoreSession();
  }, [restoreSession, logout]);

  const login = useCallback(async (credentials: LoginRequest) => {
    setState((prev) => ({ ...prev, status: 'authenticating', error: null }));
    try {
      const loginRes = await loginUser(credentials);
      const token = loginRes.accessToken;

      await saveSecureToken(token);
      apiClient.setAuthToken(token);

      const user = await fetchCurrentUser();
      await saveSecureUser(user);

      setState({
        status: 'authenticated',
        user,
        token,
        error: null,
      });
    } catch (err: any) {
      const appErr = err as AppError;
      const errorMsg = appErr.message || 'Login failed. Please check your credentials.';
      setState({
        status: 'unauthenticated',
        user: null,
        token: null,
        error: errorMsg,
      });
      throw err;
    }
  }, []);

  const register = useCallback(
    async (data: RegisterRequest): Promise<RegisterResponse> => {
      setState((prev) => ({ ...prev, status: 'authenticating', error: null }));
      try {
        const registerRes = await registerUser(data);
        // Automatically attempt login after successful registration
        try {
          await login({ email: data.email, password: data.password });
        } catch {
          // If auto login fails, user can manually log in
          setState({
            status: 'unauthenticated',
            user: null,
            token: null,
            error: null,
          });
        }
        return registerRes;
      } catch (err: any) {
        const appErr = err as AppError;
        const errorMsg = appErr.message || 'Registration failed. Please try again.';
        setState({
          status: 'unauthenticated',
          user: null,
          token: null,
          error: errorMsg,
        });
        throw err;
      }
    },
    [login]
  );

  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: null }));
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        restoreSession,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
