import React, { useState, useEffect, useCallback, useContext } from 'react';
import {
  View,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { AuthContext } from '../../context/AuthContext';
import { getOfflineTransactions } from '../../storage/offlineStorage';
import { OfflineTransactionItem } from '../../types/offline';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatDateDisplay } from '../../utils/dateFormatter';

type Props = NativeStackScreenProps<RootStackParamList, 'ConflictList'>;

export const ConflictListScreen: React.FC<Props> = ({ navigation }) => {
  const { user } = useContext(AuthContext);
  const userId = user?.userId;

  const [conflicts, setConflicts] = useState<OfflineTransactionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadConflicts = useCallback(async () => {
    if (!userId) return;
    setError(null);
    try {
      const items = await getOfflineTransactions(userId);
      const conflictItems = items.filter((i) => i.status === 'CONFLICT');
      setConflicts(conflictItems);
    } catch (err: any) {
      setError(err?.message || 'Failed to load transaction conflicts.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [userId]);

  useEffect(() => {
    loadConflicts();
  }, [loadConflicts]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadConflicts();
  };

  const handlePressConflict = (clientOperationId: string) => {
    navigation.navigate('ConflictResolution', { clientOperationId });
  };

  const renderItem = ({ item }: { item: OfflineTransactionItem }) => {
    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.7}
        onPress={() => handlePressConflict(item.clientOperationId)}
      >
        <View style={styles.cardHeader}>
          <AppText variant="body" bold color={colors.warning}>
            ⚡ {item.errorCode || 'CONFLICT'}
          </AppText>
          <AppText variant="caption" bold color={item.type === 'INCOME' ? colors.success : colors.error}>
            {item.type}
          </AppText>
        </View>

        <View style={styles.cardBody}>
          <View style={styles.leftCol}>
            <AppText variant="subheader" bold>
              {formatCurrency(item.amount)}
            </AppText>
            <AppText variant="caption" color={colors.textSecondary} numberOfLines={1}>
              {item.description || 'No description'}
            </AppText>
          </View>
          <View style={styles.rightCol}>
            <AppText variant="caption" color={colors.textMuted}>
              {formatDateDisplay(item.transactionDate)}
            </AppText>
            <AppText variant="caption" bold color={colors.primary} style={styles.resolveLink}>
              Resolve →
            </AppText>
          </View>
        </View>

        {item.errorMessage ? (
          <AppText variant="caption" color={colors.textMuted} style={styles.cardError} numberOfLines={2}>
            {item.errorMessage}
          </AppText>
        ) : null}
      </TouchableOpacity>
    );
  };

  if (loading) {
    return (
      <ScreenContainer>
        <LoadingIndicator message="Checking transaction conflicts..." />
      </ScreenContainer>
    );
  }

  if (error) {
    return (
      <ScreenContainer>
        <ErrorMessage message={error} onRetry={loadConflicts} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer scrollable={false}>
      <View style={styles.headerContainer}>
        <AppText variant="header" style={styles.title}>
          Transaction Conflicts
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Select a transaction needing attention to reconcile differences.
        </AppText>
      </View>

      <FlatList
        data={conflicts}
        keyExtractor={(item) => item.clientOperationId}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} colors={[colors.primary]} />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <AppText variant="subheader" color={colors.textMuted} style={styles.emptyText}>
              ✅ All transactions are up to date.
            </AppText>
            <AppText variant="caption" color={colors.textMuted}>
              No unresolved conflicts found.
            </AppText>
          </View>
        }
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  headerContainer: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
  },
  title: {
    marginBottom: spacing.xs,
  },
  listContent: {
    padding: spacing.md,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderLeftWidth: 4,
    borderLeftColor: colors.warning,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  cardBody: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  leftCol: {
    flex: 1,
    marginRight: spacing.sm,
  },
  rightCol: {
    alignItems: 'flex-end',
  },
  resolveLink: {
    marginTop: 4,
  },
  cardError: {
    marginTop: spacing.xs,
    fontStyle: 'italic',
  },
  emptyContainer: {
    paddingVertical: spacing.xxl,
    alignItems: 'center',
  },
  emptyText: {
    marginBottom: spacing.xs,
  },
});
