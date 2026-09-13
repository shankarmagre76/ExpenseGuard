/**
 * Authentication type definitions strictly matching Spring Boot Backend DTOs.
 */

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  message: string;
  userId: string;
  name: string;
  email: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserResponse {
  userId: string;
  name: string;
  email: string;
  status: string;
  createdAt: string;
}

export type AuthStatus = 'unauthenticated' | 'authenticating' | 'authenticated' | 'restoring';

export interface AuthState {
  status: AuthStatus;
  user: UserResponse | null;
  token: string | null;
  error: string | null;
}

export interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<RegisterResponse>;
  logout: () => Promise<void>;
  restoreSession: () => Promise<void>;
  clearError: () => void;
}
