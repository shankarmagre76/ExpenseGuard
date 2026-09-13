import { parseApiError } from '../src/utils/errorHandler';
import {
  saveSecureToken,
  getSecureToken,
  removeSecureToken,
  saveSecureUser,
  getSecureUser,
  clearSecureSession,
} from '../src/storage/secureStorage';

describe('Auth Utility & Security Tests', () => {
  beforeEach(async () => {
    await clearSecureSession();
  });

  describe('Secure Storage & Token Persistence', () => {
    it('should securely store and retrieve JWT access token', async () => {
      const mockToken = 'eyJhbGciOiJIUzI1NiJ9.test.jwt';
      const saveResult = await saveSecureToken(mockToken);
      expect(saveResult).toBe(true);

      const retrievedToken = await getSecureToken();
      expect(retrievedToken).toBe(mockToken);
    });

    it('should securely clear token on logout', async () => {
      await saveSecureToken('test-token-123');
      await removeSecureToken();

      const tokenAfterLogout = await getSecureToken();
      expect(tokenAfterLogout).toBeNull();
    });

    it('should securely store and retrieve user profile payload', async () => {
      const mockUser = {
        userId: '11111111-2222-3333-4444-555555555555',
        name: 'Test User',
        email: 'test@example.com',
        status: 'ACTIVE',
        createdAt: '2026-09-13T23:00:00Z',
      };

      await saveSecureUser(mockUser);
      const user = await getSecureUser();
      expect(user).toEqual(mockUser);
    });

    it('should clear user data on session clear', async () => {
      await saveSecureUser({
        userId: '123',
        name: 'Alice',
        email: 'alice@example.com',
        status: 'ACTIVE',
        createdAt: '2026-01-01',
      });
      await clearSecureSession();

      const user = await getSecureUser();
      expect(user).toBeNull();
    });
  });

  describe('API Error Handling & Security Messages', () => {
    it('should format 401 Unauthorized errors correctly without exposing stack trace', () => {
      const mockAxiosError = {
        isAxiosError: true,
        response: {
          status: 401,
          data: { message: 'Invalid email or password' },
        },
      };

      const parsed = parseApiError(mockAxiosError);
      expect(parsed.status).toBe(401);
      expect(parsed.message).toBe('Session expired or unauthorized. Please sign in again.');
      expect(parsed.code).toBe('HTTP_401');
    });

    it('should handle 409 Conflict for duplicate email registration', () => {
      const mockAxiosError = {
        isAxiosError: true,
        response: {
          status: 409,
          data: { message: 'An account with this email address already exists' },
        },
      };

      const parsed = parseApiError(mockAxiosError);
      expect(parsed.status).toBe(409);
      expect(parsed.message).toBe('An account with this email address already exists');
    });

    it('should handle network connection failure gracefully', () => {
      const mockNetworkError = {
        isAxiosError: true,
        response: undefined,
        code: 'ERR_NETWORK',
      };

      const parsed = parseApiError(mockNetworkError);
      expect(parsed.code).toBe('NETWORK_ERROR');
      expect(parsed.message).toContain('Unable to connect to ExpenseGuard backend server');
    });

    it('should handle timeout errors gracefully', () => {
      const mockTimeoutError = {
        isAxiosError: true,
        response: undefined,
        code: 'ECONNABORTED',
      };

      const parsed = parseApiError(mockTimeoutError);
      expect(parsed.code).toBe('TIMEOUT_ERROR');
      expect(parsed.message).toContain('Request timed out');
    });
  });

  describe('Password Security Audit', () => {
    it('should never include raw password fields in token storage or user responses', async () => {
      const user = {
        userId: '123',
        name: 'Bob',
        email: 'bob@example.com',
        status: 'ACTIVE',
        createdAt: '2026-01-01',
      };

      await saveSecureUser(user);
      const stored = await getSecureUser();

      expect(stored).not.toHaveProperty('password');
      expect(stored).not.toHaveProperty('passwordHash');
    });
  });
});
