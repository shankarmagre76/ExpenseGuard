import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { colors, spacing, borderRadius } from '../../theme';

export const ProfileScreen: React.FC = () => {
  return (
    <ScreenContainer scrollable>
      <View style={styles.profileHeader}>
        <View style={styles.avatar}>
          <AppText variant="title" color={colors.surface}>
            EG
          </AppText>
        </View>
        <AppText variant="header">ExpenseGuard User</AppText>
        <AppText variant="caption" color={colors.textSecondary}>
          Phase 1 Mobile Foundation
        </AppText>
      </View>

      <View style={styles.card}>
        <AppText variant="subheader" bold style={styles.cardTitle}>
          Account Preferences
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          User settings, JWT credentials, and security controls will be attached in future phases.
        </AppText>
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
    width: 72,
    height: 72,
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
    marginTop: spacing.md,
  },
  cardTitle: {
    marginBottom: spacing.xs,
  },
});
