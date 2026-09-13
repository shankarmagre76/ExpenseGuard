import {
  getNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  deleteNotificationApi,
} from '../src/api/endpoints/notificationApi';
import { apiClient } from '../src/api/client';
import { NotificationItem } from '../src/types/notification';

jest.mock('../src/api/client', () => ({
  apiClient: {
    get: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('Notification REST API Client', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches notification list from GET /api/v1/notifications', async () => {
    const mockNotifications: NotificationItem[] = [
      {
        id: 'notif-1',
        type: 'BUDGET_EXCEEDED',
        title: 'Budget Exceeded: Dining',
        message: 'You spent $250 out of $200 allocated.',
        timestamp: '2026-09-14T00:00:00Z',
        read: false,
      },
    ];

    (apiClient.get as jest.Mock).mockResolvedValueOnce(mockNotifications);

    const result = await getNotifications();
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications');
    expect(result).toEqual(mockNotifications);
  });

  it('returns empty array when backend notification endpoint returns error (e.g. 404)', async () => {
    (apiClient.get as jest.Mock).mockRejectedValueOnce(new Error('Request failed with status code 404'));

    const result = await getNotifications();
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications');
    expect(result).toEqual([]);
  });

  it('sends PUT request to mark notification as read', async () => {
    (apiClient.put as jest.Mock).mockResolvedValueOnce({});

    await markNotificationAsRead('notif-1');
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/notifications/notif-1/read');
  });

  it('sends PUT request to mark all notifications as read', async () => {
    (apiClient.put as jest.Mock).mockResolvedValueOnce({});

    await markAllNotificationsAsRead();
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/notifications/read-all');
  });

  it('sends DELETE request to delete notification', async () => {
    (apiClient.delete as jest.Mock).mockResolvedValueOnce({});

    await deleteNotificationApi('notif-1');
    expect(apiClient.delete).toHaveBeenCalledWith('/api/v1/notifications/notif-1');
  });
});
