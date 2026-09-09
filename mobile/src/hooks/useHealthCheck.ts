import { useState, useCallback, useEffect } from 'react';
import { fetchHealthStatus } from '../api/endpoints/health';
import { HealthState } from '../types/health';

export const useHealthCheck = () => {
  const [state, setState] = useState<HealthState>({
    status: 'idle',
  });

  const checkHealth = useCallback(async () => {
    setState((prev) => ({ ...prev, status: 'loading', error: undefined }));
    try {
      const data = await fetchHealthStatus();
      setState({
        status: 'connected',
        data,
        lastChecked: new Date().toLocaleTimeString(),
      });
    } catch (err: any) {
      setState({
        status: 'failed',
        error: err.message || 'Failed to connect to backend server',
        lastChecked: new Date().toLocaleTimeString(),
      });
    }
  }, []);

  useEffect(() => {
    checkHealth();
  }, [checkHealth]);

  return {
    ...state,
    checkHealth,
  };
};
