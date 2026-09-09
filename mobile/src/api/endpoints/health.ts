import { apiClient } from '../client';
import { HealthCheckResponse } from '../../types/health';

export const fetchHealthStatus = async (): Promise<HealthCheckResponse> => {
  return apiClient.get<HealthCheckResponse>('/api/v1/health');
};
