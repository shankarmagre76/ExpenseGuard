import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { useHealthCheck } from '../../hooks/useHealthCheck';
import { colors, spacing, borderRadius } from '../../theme';
import { apiClient } from '../../api/client';

export const DashboardScreen: React.FC<any> = ({ navigation }) => {
  const { status, data, error, checkHealth, lastChecked } = useHealthCheck();

  const getStatusBadgeColor = () => {
    switch (status) {
      case 'connected':
        return colors.success;
      case 'failed':
        return colors.error;
      case 'loading':
      default:
        return colors.warning;
    }
  };

  return (
    <ScreenContainer scrollable>
      {/* Header Banner */}
      <View style={styles.heroCard}>
        <AppText variant="title" color={colors.surface} style={styles.heroTitle}>
          ExpenseGuard
        </AppText>
        <AppText variant="body" color={colors.primaryLight}>
          Smart Personal Finance Management Mobile App
        </AppText>
      </View>

      {/* Backend Status Card */}
      <View style={styles.statusCard}>
        <View style={styles.statusHeader}>
          <AppText variant="header">Backend Status</AppText>
          <View style={[styles.statusBadge, { backgroundColor: getStatusBadgeColor() }]}>
            <AppText variant="caption" color={colors.surface} bold>
              {status.toUpperCase()}
            </AppText>
          </View>
        </View>

        <AppText variant="body" color={colors.textSecondary} style={styles.urlText}>
          Base URL: {apiClient.getBaseUrl()}
        </AppText>

        {status === 'connected' && data ? (
          <View style={styles.successBox}>
            <AppText variant="body" color={colors.success} bold>
              ✓ {data.message}
            </AppText>
            <AppText variant="caption" color={colors.textMuted} style={styles.timestamp}>
              Timestamp: {data.timestamp}
            </AppText>
          </View>
        ) : null}

        {status === 'failed' && error ? (
          <View style={styles.errorBox}>
            <AppText variant="body" color={colors.error} bold>
              ✕ Connection Failed
            </AppText>
            <AppText variant="caption" color={colors.textSecondary}>
              {error}
            </AppText>
          </View>
        ) : null}

        {lastChecked ? (
          <AppText variant="caption" color={colors.textMuted} style={styles.lastCheckedText}>
            Last checked: {lastChecked}
          </AppText>
        ) : null}

        <PrimaryButton
          title="Re-check Connection"
          onPress={checkHealth}
          variant="outline"
          isLoading={status === 'loading'}
          style={styles.checkButton}
        />
      </View>

      {/* Quick Navigation to Debug Screen */}
      <TouchableOpacity
        style={styles.debugCard}
        onPress={() => navigation.navigate('HealthCheck')}
      >
        <AppText variant="subheader" bold color={colors.primary}>
          Open API Health Diagnostics →
        </AppText>
        <AppText variant="caption" color={colors.textSecondary}>
          Detailed inspection of REST endpoint /api/v1/health
        </AppText>
      </TouchableOpacity>

      {/* Feature Placeholders */}
      <View style={styles.sectionHeader}>
        <AppText variant="header">Upcoming Features</AppText>
      </View>
      <View style={styles.grid}>
        {['Transactions', 'Accounts', 'Budgets', 'Analytics', 'Receipt Scanner', 'Notifications'].map(
          (feature, index) => (
            <View key={index} style={styles.featureChip}>
              <AppText variant="body" semibold color={colors.textPrimary}>
                {feature}
              </AppText>
              <AppText variant="caption" color={colors.textMuted}>
                Phase {index < 3 ? '2' : '3'}
              </AppText>
            </View>
          )
        )}
      </View>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  heroCard: {
    backgroundColor: colors.primary,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.md,
  },
  heroTitle: {
    marginBottom: spacing.xs,
  },
  statusCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  statusHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  statusBadge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.full,
  },
  urlText: {
    marginBottom: spacing.sm,
  },
  successBox: {
    backgroundColor: colors.successLight,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.sm,
  },
  errorBox: {
    backgroundColor: colors.errorLight,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.sm,
  },
  timestamp: {
    marginTop: spacing.xs,
  },
  lastCheckedText: {
    marginBottom: spacing.sm,
  },
  checkButton: {
    marginTop: spacing.xs,
  },
  debugCard: {
    backgroundColor: colors.primaryLight,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  sectionHeader: {
    marginVertical: spacing.sm,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  featureChip: {
    width: '48%',
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
});
