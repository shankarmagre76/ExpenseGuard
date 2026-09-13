import { apiClient } from '../client';
import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  UserResponse,
} from '../../types/auth';

/**
 * Auth API service interacting with ExpenseGuard Spring Boot Auth endpoints.
 */

export const registerUser = async (data: RegisterRequest): Promise<RegisterResponse> => {
  return apiClient.post<RegisterResponse>('/api/v1/auth/register', data);
};

export const loginUser = async (data: LoginRequest): Promise<LoginResponse> => {
  return apiClient.post<LoginResponse>('/api/v1/auth/login', data);
};

export const fetchCurrentUser = async (): Promise<UserResponse> => {
  return apiClient.get<UserResponse>('/api/v1/me');
};
