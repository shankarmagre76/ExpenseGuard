export type NotificationType = 'BUDGET_WARNING' | 'BUDGET_EXCEEDED' | 'RECURRING_DUE' | 'SYSTEM_INFO';

export interface NotificationItem {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  metadata?: Record<string, any>;
}
