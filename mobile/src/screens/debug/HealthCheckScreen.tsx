import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { ErrorMessage } from '../../components/ErrorMessage';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { useHealthCheck } from '../../hooks/useHealthCheck';
import { apiClient } from '../../api/client';
import { colors, spacing, borderRadius } from '../../theme';

export const HealthCheckScreen: React.FC = () => {
  const { status, data, error, checkHealth, lastChecked } = useHealthCheck();

  return (
    <ScreenContainer scrollable>
      <View style={styles.header}>
        <AppText variant="header">Backend Health Diagnostics</AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Tests endpoint GET /api/v1/health on ExpenseGuard Spring Boot Backend.
        </AppText>
      </View>

      <View style={styles.infoCard}>
        <AppText variant="caption" color={colors.textMuted}>
          TARGET API ENDPOINT
        </AppText>
        <AppText variant="subheader" bold color={colors.primary}>
          {apiClient.getBaseUrl()}/api/v1/health
        </AppText>
      </View>

      {status === 'loading' ? (
        <LoadingIndicator message="Connecting to Spring Boot backend on port 8081..." />
      ) : null}

      {status === 'failed' && error ? (
        <ErrorMessage
          title="Backend Unreachable"
          message={error}
          onRetry={checkHealth}
        />
      ) : null}

      {status === 'connected' && data ? (
        <View style={styles.card}>
          <AppText variant="subheader" bold color={colors.success} style={styles.statusTitle}>
            ✓ Server Response: {data.status}
          </AppText>
          <View style={styles.row}>
            <AppText variant="caption" color={colors.textMuted}>
              Message:
            </AppText>
            <AppText variant="body" style={styles.value}>
              {data.message}
            </AppText>
          </View>
          <View style={styles.row}>
            <AppText variant="caption" color={colors.textMuted}>
              Timestamp:
            </AppText>
            <AppText variant="body" style={styles.value}>
              {data.timestamp}
            </AppText>
          </View>
        </View>
      ) : null}

      {lastChecked ? (
        <AppText variant="caption" color={colors.textMuted} style={styles.lastChecked}>
          Last check timestamp: {lastChecked}
        </AppText>
      ) : null}

      <PrimaryButton
        title="Trigger Health Check"
        onPress={checkHealth}
        isLoading={status === 'loading'}
        style={styles.actionButton}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  header: {
    marginBottom: spacing.md,
  },
  infoCard: {
    backgroundColor: colors.surfaceVariant,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.md,
  },
  card: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.success,
    marginBottom: spacing.md,
  },
  statusTitle: {
    marginBottom: spacing.sm,
  },
  row: {
    marginVertical: spacing.xs,
  },
  value: {
    marginTop: 2,
  },
  lastChecked: {
    textAlign: 'center',
    marginBottom: spacing.md,
  },
  actionButton: {
    marginTop: spacing.sm,
  },
});
