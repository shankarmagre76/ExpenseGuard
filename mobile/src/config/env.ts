import { Platform } from 'react-native';

/**
 * Environment configuration for ExpenseGuard Mobile
 *
 * Host Configuration:
 * - Android Emulator: http://10.0.2.2:8081 (10.0.2.2 points to host machine localhost)
 * - iOS Simulator: http://localhost:8081
 * - Physical Device: http://<DEVELOPMENT_MACHINE_LAN_IP>:8081
 */

declare const process: { env: Record<string, string | undefined> };

const DEFAULT_DEV_HOST = Platform.OS === 'android' ? 'http://10.0.2.2:8081' : 'http://localhost:8081';

export const ENV = {
  API_BASE_URL: process.env.API_BASE_URL || DEFAULT_DEV_HOST,
  API_TIMEOUT: 15000,
  IS_DEV: __DEV__,
};

export const getApiBaseUrl = (): string => ENV.API_BASE_URL;
