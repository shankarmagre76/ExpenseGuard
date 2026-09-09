export type HealthStatusType = 'UP' | 'DOWN' | 'UNKNOWN';

export interface HealthCheckResponse {
  status: string;
  message: string;
  timestamp: string;
}

export interface HealthState {
  status: 'idle' | 'loading' | 'connected' | 'failed';
  data?: HealthCheckResponse;
  error?: string;
  lastChecked?: string;
}
