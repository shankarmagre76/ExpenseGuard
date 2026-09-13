import { apiClient } from '../client';
import { NotificationItem } from '../../types/notification';

/**
 * Fetches notifications from the backend REST endpoint GET /api/v1/notifications.
 * If the backend endpoint returns 404 or an error (e.g. notification service not deployed),
 * returns an empty array instead of synthesizing fake client-side alerts.
 */
export const getNotifications = async (): Promise<NotificationItem[]> => {
  try {
    return await apiClient.get<NotificationItem[]>('/api/v1/notifications');
  } catch {
    return [];
  }
};

/**
 * Marks a notification as read on the backend.
 */
export const markNotificationAsRead = async (id: string): Promise<void> => {
  try {
    await apiClient.put(`/api/v1/notifications/${id}/read`);
  } catch {
    // Ignore error if backend endpoint is unavailable
  }
};

/**
 * Marks all notifications as read on the backend.
 */
export const markAllNotificationsAsRead = async (): Promise<void> => {
  try {
    await apiClient.put('/api/v1/notifications/read-all');
  } catch {
    // Ignore error if backend endpoint is unavailable
  }
};

/**
 * Deletes a notification on the backend.
 */
export const deleteNotificationApi = async (id: string): Promise<void> => {
  try {
    await apiClient.delete(`/api/v1/notifications/${id}`);
  } catch {
    // Ignore error if backend endpoint is unavailable
  }
};
