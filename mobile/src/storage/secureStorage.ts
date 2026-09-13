import * as Keychain from 'react-native-keychain';
import { UserResponse } from '../types/auth';

const TOKEN_SERVICE_KEY = 'com.expenseguard.auth.token';
const USER_SERVICE_KEY = 'com.expenseguard.auth.user';

// In-memory fallback for testing / environments without Keychain native module
let inMemoryToken: string | null = null;
let inMemoryUser: string | null = null;

export const saveSecureToken = async (token: string): Promise<boolean> => {
  try {
    inMemoryToken = token;
    await Keychain.setGenericPassword('JWT_ACCESS_TOKEN', token, {
      service: TOKEN_SERVICE_KEY,
    });
    return true;
  } catch (error) {
    // Fallback if Keychain is unavailable (e.g., Jest runner)
    inMemoryToken = token;
    return true;
  }
};

export const getSecureToken = async (): Promise<string | null> => {
  try {
    const credentials = await Keychain.getGenericPassword({
      service: TOKEN_SERVICE_KEY,
    });
    if (credentials && credentials.password) {
      inMemoryToken = credentials.password;
      return credentials.password;
    }
    return inMemoryToken;
  } catch (error) {
    return inMemoryToken;
  }
};

export const removeSecureToken = async (): Promise<boolean> => {
  try {
    inMemoryToken = null;
    await Keychain.resetGenericPassword({ service: TOKEN_SERVICE_KEY });
    return true;
  } catch (error) {
    inMemoryToken = null;
    return true;
  }
};

export const saveSecureUser = async (user: UserResponse): Promise<boolean> => {
  try {
    const jsonValue = JSON.stringify(user);
    inMemoryUser = jsonValue;
    await Keychain.setGenericPassword('USER_DATA', jsonValue, {
      service: USER_SERVICE_KEY,
    });
    return true;
  } catch (error) {
    inMemoryUser = JSON.stringify(user);
    return true;
  }
};

export const getSecureUser = async (): Promise<UserResponse | null> => {
  try {
    const credentials = await Keychain.getGenericPassword({
      service: USER_SERVICE_KEY,
    });
    const jsonStr = credentials ? credentials.password : inMemoryUser;
    if (jsonStr) {
      return JSON.parse(jsonStr) as UserResponse;
    }
    return null;
  } catch (error) {
    if (inMemoryUser) {
      try {
        return JSON.parse(inMemoryUser) as UserResponse;
      } catch {
        return null;
      }
    }
    return null;
  }
};

export const clearSecureSession = async (): Promise<void> => {
  await removeSecureToken();
  try {
    inMemoryUser = null;
    await Keychain.resetGenericPassword({ service: USER_SERVICE_KEY });
  } catch {
    inMemoryUser = null;
  }
};
