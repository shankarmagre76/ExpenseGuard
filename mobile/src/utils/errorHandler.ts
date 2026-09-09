import axios, { AxiosError } from 'axios';
import { AppError, ApiErrorResponse } from '../types/api';

export const parseApiError = (error: unknown): AppError => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;

    // Network level failures (Backend unreachable / offline)
    if (!axiosError.response) {
      if (axiosError.code === 'ECONNABORTED') {
        return {
          code: 'TIMEOUT_ERROR',
          message: 'Request timed out. Please check backend port 8081 server availability.',
        };
      }
      return {
        code: 'NETWORK_ERROR',
        message: 'Unable to connect to ExpenseGuard backend server. Please verify backend is running on port 8081.',
      };
    }

    const status = axiosError.response.status;
    const data = axiosError.response.data;

    let userMessage = data?.message || axiosError.message;

    switch (status) {
      case 400:
        userMessage = data?.message || 'Invalid request syntax or parameters.';
        break;
      case 401:
        userMessage = 'Session expired or unauthorized. Please sign in again.';
        break;
      case 403:
        userMessage = 'Access denied. You do not have permission to perform this action.';
        break;
      case 404:
        userMessage = 'The requested resource endpoint was not found.';
        break;
      case 409:
        userMessage = data?.message || 'A conflict occurred with the existing state.';
        break;
      case 500:
      default:
        userMessage = 'An unexpected server error occurred. Please try again later.';
        break;
    }

    return {
      code: data?.code || `HTTP_${status}`,
      message: userMessage,
      status,
      originalError: error,
    };
  }

  if (error instanceof Error) {
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message,
      originalError: error,
    };
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: 'An unknown error occurred.',
    originalError: error,
  };
};
