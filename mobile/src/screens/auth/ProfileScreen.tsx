import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { useAuth } from '../../hooks/useAuth';
import { colors, spacing, borderRadius } from '../../theme';

export const ProfileScreen: React.FC = () => {
  const { user, logout } = useAuth();

  const getInitials = (name?: string) => {
    if (!name) return 'EG';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  return (
    <ScreenContainer scrollable>
      {/* Profile Banner */}
      <View style={styles.profileHeader}>
        <View style={styles.avatar}>
          <AppText variant="title" color={colors.surface}>
            {getInitials(user?.name)}
          </AppText>
        </View>
        <AppText variant="header">{user?.name || 'ExpenseGuard User'}</AppText>
        <AppText variant="body" color={colors.textSecondary}>
          {user?.email || 'user@example.com'}
        </AppText>
      </View>

      {/* Account Info Card */}
      <View style={styles.card}>
        <AppText variant="subheader" bold style={styles.cardTitle}>
          Account Details
        </AppText>

        <View style={styles.infoRow}>
          <AppText variant="caption" color={colors.textMuted}>
            USER ID
          </AppText>
          <AppText variant="body" color={colors.textPrimary} style={styles.valueText}>
            {user?.userId || 'N/A'}
          </AppText>
        </View>

        <View style={styles.infoRow}>
          <AppText variant="caption" color={colors.textMuted}>
            ACCOUNT STATUS
          </AppText>
          <AppText variant="body" color={colors.success} bold style={styles.valueText}>
            {user?.status || 'ACTIVE'}
          </AppText>
        </View>

        {user?.createdAt ? (
          <View style={styles.infoRow}>
            <AppText variant="caption" color={colors.textMuted}>
              MEMBER SINCE
            </AppText>
            <AppText variant="body" color={colors.textPrimary} style={styles.valueText}>
              {new Date(user.createdAt).toLocaleDateString()}
            </AppText>
          </View>
        ) : null}
      </View>

      {/* Security Actions Card */}
      <View style={styles.card}>
        <AppText variant="subheader" bold style={styles.cardTitle}>
          Security & Session
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.description}>
          Active JWT session is stored securely in encrypted device storage. Logging out will clear local credentials.
        </AppText>

        <PrimaryButton
          title="Sign Out"
          onPress={logout}
          variant="outline"
          style={styles.logoutButton}
        />
      </View>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  profileHeader: {
    alignItems: 'center',
    paddingVertical: spacing.lg,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  card: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  cardTitle: {
    marginBottom: spacing.md,
  },
  infoRow: {
    marginBottom: spacing.sm,
  },
  valueText: {
    marginTop: 2,
  },
  description: {
    marginBottom: spacing.md,
  },
  logoutButton: {
    borderColor: colors.error,
  },
});
