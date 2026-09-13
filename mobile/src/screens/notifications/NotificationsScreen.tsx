import React from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  RefreshControl,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { useNotifications } from '../../hooks/useNotifications';
import { NotificationItem, NotificationType } from '../../types/notification';
import { colors, spacing, borderRadius } from '../../theme';
import { formatDateDisplay } from '../../utils/dateFormatter';

export const NotificationsScreen: React.FC = () => {
  const {
    notifications,
    unreadCount,
    loading,
    error,
    refreshNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
  } = useNotifications();

  const getTypeTheme = (type: NotificationType) => {
    switch (type) {
      case 'BUDGET_EXCEEDED':
        return { color: colors.error, icon: '🚨', label: 'Budget Exceeded' };
      case 'BUDGET_WARNING':
        return { color: colors.warning, icon: '⚠️', label: 'Budget Warning' };
      case 'RECURRING_DUE':
        return { color: colors.primary, icon: '📅', label: 'Recurring Due' };
      case 'SYSTEM_INFO':
      default:
        return { color: colors.success, icon: '⚙️', label: 'System Update' };
    }
  };

  const renderItem = ({ item }: { item: NotificationItem }) => {
    const { color, icon, label } = getTypeTheme(item.type);

    return (
      <TouchableOpacity
        activeOpacity={0.8}
        onPress={() => markAsRead(item.id)}
        style={[
          styles.card,
          !item.read && styles.unreadCard,
          { borderLeftColor: color },
        ]}
        accessibilityLabel={`Notification: ${item.title}. ${item.read ? 'Read' : 'Unread'}`}
        accessibilityRole="button"
      >
        <View style={styles.cardHeader}>
          <View style={styles.headerTitleRow}>
            <AppText variant="body" style={styles.iconPrefix}>
              {icon}
            </AppText>
            <AppText variant="subheader" style={styles.cardTitle} numberOfLines={1}>
              {item.title}
            </AppText>
          </View>

          <View style={styles.rightHeaderGroup}>
            {!item.read && <View style={[styles.unreadDot, { backgroundColor: color }]} />}
            <TouchableOpacity
              onPress={() => deleteNotification(item.id)}
              style={styles.deleteButton}
              accessibilityLabel="Delete notification"
              accessibilityRole="button"
            >
              <AppText variant="caption" color={colors.textMuted}>
                ✕
              </AppText>
            </TouchableOpacity>
          </View>
        </View>

        <AppText variant="body" color={colors.textSecondary} style={styles.messageText}>
          {item.message}
        </AppText>

        <View style={styles.cardFooter}>
          <View style={[styles.typeBadge, { backgroundColor: `${color}1A` }]}>
            <AppText variant="caption" bold style={{ color }}>
              {label}
            </AppText>
          </View>
          <AppText variant="caption" color={colors.textMuted}>
            {formatDateDisplay(item.timestamp)}
          </AppText>
        </View>
      </TouchableOpacity>
    );
  };

  const renderEmptyState = () => {
    if (loading) return null;
    return (
      <View style={styles.emptyContainer}>
        <AppText variant="header" style={styles.emptyIcon}>
          🔔
        </AppText>
        <AppText variant="subheader" style={styles.emptyTitle}>
          No Notifications
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.emptySubtitle}>
          You're all caught up! Budget warnings, recurring payment alerts, and system status updates will appear here.
        </AppText>
      </View>
    );
  };

  return (
    <ScreenContainer scrollable={false}>
      <View style={styles.topHeader}>
        <View style={styles.titleRow}>
          <AppText variant="header">Notifications</AppText>
          {unreadCount > 0 && (
            <View style={styles.countBadge}>
              <AppText variant="caption" color={colors.surface} style={styles.countText}>
                {unreadCount}
              </AppText>
            </View>
          )}
        </View>

        {unreadCount > 0 && (
          <TouchableOpacity
            onPress={markAllAsRead}
            style={styles.markAllBtn}
            accessibilityLabel="Mark all as read"
            accessibilityRole="button"
          >
            <AppText variant="caption" color={colors.primary} style={styles.markAllText}>
              Mark All Read
            </AppText>
          </TouchableOpacity>
        )}
      </View>

      {error && <ErrorMessage message={error} style={styles.errorBanner} />}

      {loading && notifications.length === 0 ? (
        <LoadingIndicator message="Loading notifications..." />
      ) : (
        <FlatList
          data={notifications}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          ListEmptyComponent={renderEmptyState}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refreshNotifications}
              colors={[colors.primary]}
              tintColor={colors.primary}
            />
          }
        />
      )}
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  countBadge: {
    backgroundColor: colors.primary,
    borderRadius: 10,
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    marginLeft: spacing.xs,
  },
  countText: {
    fontWeight: '700',
    fontSize: 11,
  },
  markAllBtn: {
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
  },
  markAllText: {
    fontWeight: '700',
  },
  errorBanner: {
    marginBottom: spacing.md,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  card: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderLeftWidth: 5,
    marginBottom: spacing.md,
  },
  unreadCard: {
    backgroundColor: `${colors.primary}08`,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: spacing.xs,
  },
  iconPrefix: {
    marginRight: spacing.xs,
  },
  cardTitle: {
    fontWeight: '700',
    flex: 1,
  },
  rightHeaderGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: spacing.xs,
  },
  deleteButton: {
    padding: spacing.xs,
  },
  messageText: {
    marginBottom: spacing.sm,
    lineHeight: 20,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  typeBadge: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    borderRadius: borderRadius.sm,
  },
  emptyContainer: {
    backgroundColor: colors.surface,
    padding: spacing.xl,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    marginVertical: spacing.md,
  },
  emptyIcon: {
    fontSize: 32,
    marginBottom: spacing.xs,
  },
  emptyTitle: {
    fontWeight: '700',
    marginBottom: spacing.xs,
  },
  emptySubtitle: {
    textAlign: 'center',
  },
});
