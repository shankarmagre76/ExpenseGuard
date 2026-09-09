export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp?: string;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  timestamp?: string;
  details?: Record<string, string>;
}

export interface AppError {
  code: string;
  message: string;
  status?: number;
  originalError?: unknown;
}
