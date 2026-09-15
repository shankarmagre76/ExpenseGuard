import React, { useState, useEffect, useCallback, useContext } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { TransactionForm } from '../../components/TransactionForm';
import { AuthContext } from '../../context/AuthContext';
import { SyncContext } from '../../context/SyncContext';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { getOfflineTransactions } from '../../storage/offlineStorage';
import { getTransactionById } from '../../api/endpoints/transactionApi';
import { syncService } from '../../services/syncService';
import { OfflineTransactionItem } from '../../types/offline';
import { TransactionResponse, TransactionRequest } from '../../types/transaction';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatDateDisplay } from '../../utils/dateFormatter';

type Props = NativeStackScreenProps<RootStackParamList, 'ConflictResolution'>;

export const ConflictResolutionScreen: React.FC<Props> = ({ route, navigation }) => {
  const { clientOperationId } = route.params;
  const { user } = useContext(AuthContext);
  const { refreshSyncSummary } = useContext(SyncContext);
  const userId = user?.userId;

  const { accounts } = useAccounts();
  const { allCategories: categories } = useCategories();

  const [localItem, setLocalItem] = useState<OfflineTransactionItem | null>(null);
  const [serverTx, setServerTx] = useState<TransactionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [resolving, setResolving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  const loadDetails = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);

    try {
      const items = await getOfflineTransactions(userId);
      const item = items.find((i) => i.clientOperationId === clientOperationId);
      if (!item) {
        setError('Conflicting record no longer exists.');
        setLoading(false);
        return;
      }
      setLocalItem(item);

      if (item.transactionId) {
        try {
          const remote = await getTransactionById(item.transactionId);
          setServerTx(remote);
        } catch {
          // Server item might be missing or deleted
          setServerTx(null);
        }
      }
    } catch (err: any) {
      setError(err?.message || 'Failed to load conflict details.');
    } finally {
      setLoading(false);
    }
  }, [clientOperationId, userId]);

  useEffect(() => {
    loadDetails();
  }, [loadDetails]);

  const getAccountName = (accId: string) => {
    const acc = accounts.find((a) => a.id === accId);
    return acc ? acc.name : accId || 'Account';
  };

  const getCategoryName = (catId: string) => {
    const cat = categories.find((c) => c.id === catId);
    return cat ? cat.name : catId || 'Category';
  };

  const handleKeepServer = async () => {
    Alert.alert(
      'Accept Server Version',
      'This will discard your offline edit and accept the server version. Continue?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Accept Server',
          onPress: async () => {
            setResolving(true);
            try {
              await syncService.resolveKeepServer(clientOperationId);
              await refreshSyncSummary();
              navigation.goBack();
            } catch (err: any) {
              Alert.alert('Error', err?.message || 'Failed to resolve conflict');
            } finally {
              setResolving(false);
            }
          },
        },
      ]
    );
  };

  const handleKeepLocal = async () => {
    if (!userId) return;
    setResolving(true);
    try {
      await syncService.resolveKeepLocal(clientOperationId, userId);
      await refreshSyncSummary();
      navigation.goBack();
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Failed to resynchronize local version');
    } finally {
      setResolving(false);
    }
  };

  const handleEditSubmit = async (data: TransactionRequest) => {
    if (!userId) return;
    setResolving(true);
    try {
      const numAmt = typeof data.amount === 'string' ? parseFloat(data.amount) : data.amount;
      await syncService.resolveEditAndResync(clientOperationId, userId, {
        accountId: data.accountId,
        categoryId: data.categoryId,
        type: data.type as 'EXPENSE' | 'INCOME',
        amount: numAmt,
        transactionDate: data.transactionDate,
        description: data.description,
      });
      await refreshSyncSummary();
      navigation.goBack();
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Failed to resync edited transaction');
    } finally {
      setResolving(false);
    }
  };

  if (loading) {
    return (
      <ScreenContainer>
        <LoadingIndicator message="Loading conflict details..." />
      </ScreenContainer>
    );
  }

  if (error || !localItem) {
    return (
      <ScreenContainer>
        <ErrorMessage message={error || 'Conflict details not found'} onRetry={loadDetails} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer scrollable>
      <ScrollView contentContainerStyle={styles.container}>
        {/* Header */}
        <AppText variant="header" style={styles.title}>
          Transaction Needs Attention
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.subtitle}>
          This transaction was modified on the server while you were offline. Review the differences below and select how to resolve it.
        </AppText>

        {/* Warning Banner */}
        <View style={styles.warningCard}>
          <AppText variant="body" bold color={colors.warning} style={styles.warningTitle}>
            ⚡ {localItem.errorCode || 'TRANSACTION_CONFLICT'}
          </AppText>
          <AppText variant="caption" color={colors.textPrimary}>
            {localItem.errorMessage || 'The transaction version on the server differs from your local edit.'}
          </AppText>
        </View>

        {/* Inline Edit Mode */}
        {isEditing ? (
          <View style={styles.editSection}>
            <View style={styles.editHeader}>
              <AppText variant="subheader" bold>
                Edit & Resync Transaction
              </AppText>
              <TouchableOpacity onPress={() => setIsEditing(false)}>
                <AppText variant="caption" color={colors.error} bold>
                  Cancel Edit
                </AppText>
              </TouchableOpacity>
            </View>
            <TransactionForm
              type={localItem.type}
              initialValues={{
                id: localItem.clientOperationId,
                accountId: localItem.accountId,
                categoryId: localItem.categoryId,
                categoryName: getCategoryName(localItem.categoryId),
                type: localItem.type,
                amount: localItem.amount,
                transactionDate: localItem.transactionDate,
                description: localItem.description,
                createdAt: localItem.createdAt,
                updatedAt: localItem.createdAt,
              }}
              accounts={accounts}
              categories={categories}
              onSubmit={handleEditSubmit}
              onCancel={() => setIsEditing(false)}
            />
          </View>
        ) : (
          <>
            {/* Side-by-side or Stacked Comparison Cards */}
            <View style={styles.comparisonContainer}>
              {/* Local Offline Version */}
              <View style={[styles.versionCard, styles.localCard]}>
                <View style={styles.versionHeader}>
                  <AppText variant="subheader" bold color={colors.primary}>
                    📱 Your Offline Version
                  </AppText>
                  <AppText variant="caption" color={colors.textMuted}>
                    Local
                  </AppText>
                </View>

                <View style={styles.fieldRow}>
                  <AppText variant="caption" color={colors.textMuted}>Type:</AppText>
                  <AppText variant="body" bold color={localItem.type === 'INCOME' ? colors.success : colors.error}>
                    {localItem.type}
                  </AppText>
                </View>

                <View style={styles.fieldRow}>
                  <AppText variant="caption" color={colors.textMuted}>Amount:</AppText>
                  <AppText variant="subheader" bold>
                    {formatCurrency(localItem.amount)}
                  </AppText>
                </View>

                <View style={styles.fieldRow}>
                  <AppText variant="caption" color={colors.textMuted}>Date:</AppText>
                  <AppText variant="body">{formatDateDisplay(localItem.transactionDate)}</AppText>
                </View>

                <View style={styles.fieldRow}>
                  <AppText variant="caption" color={colors.textMuted}>Account:</AppText>
                  <AppText variant="body" numberOfLines={1}>{getAccountName(localItem.accountId)}</AppText>
                </View>

                <View style={styles.fieldRow}>
                  <AppText variant="caption" color={colors.textMuted}>Category:</AppText>
                  <AppText variant="body" numberOfLines={1}>{getCategoryName(localItem.categoryId)}</AppText>
                </View>

                {localItem.description ? (
                  <View style={styles.fieldRow}>
                    <AppText variant="caption" color={colors.textMuted}>Notes:</AppText>
                    <AppText variant="body" numberOfLines={2}>{localItem.description}</AppText>
                  </View>
                ) : null}
              </View>

              {/* Server Version */}
              <View style={[styles.versionCard, styles.serverCard]}>
                <View style={styles.versionHeader}>
                  <AppText variant="subheader" bold color={colors.success}>
                    🌐 Current Server Version
                  </AppText>
                  <AppText variant="caption" color={colors.textMuted}>
                    {serverTx ? `v${serverTx.version ?? 0}` : 'Server'}
                  </AppText>
                </View>

                {serverTx ? (
                  <>
                    <View style={styles.fieldRow}>
                      <AppText variant="caption" color={colors.textMuted}>Type:</AppText>
                      <AppText variant="body" bold color={serverTx.type === 'INCOME' ? colors.success : colors.error}>
                        {serverTx.type}
                      </AppText>
                    </View>

                    <View style={styles.fieldRow}>
                      <AppText variant="caption" color={colors.textMuted}>Amount:</AppText>
                      <AppText variant="subheader" bold>
                        {formatCurrency(serverTx.amount)}
                      </AppText>
                    </View>

                    <View style={styles.fieldRow}>
                      <AppText variant="caption" color={colors.textMuted}>Date:</AppText>
                      <AppText variant="body">{formatDateDisplay(serverTx.transactionDate)}</AppText>
                    </View>

                    <View style={styles.fieldRow}>
                      <AppText variant="caption" color={colors.textMuted}>Account:</AppText>
                      <AppText variant="body" numberOfLines={1}>{getAccountName(serverTx.accountId)}</AppText>
                    </View>

                    <View style={styles.fieldRow}>
                      <AppText variant="caption" color={colors.textMuted}>Category:</AppText>
                      <AppText variant="body" numberOfLines={1}>{serverTx.categoryName || getCategoryName(serverTx.categoryId)}</AppText>
                    </View>

                    {serverTx.description ? (
                      <View style={styles.fieldRow}>
                        <AppText variant="caption" color={colors.textMuted}>Notes:</AppText>
                        <AppText variant="body" numberOfLines={2}>{serverTx.description}</AppText>
                      </View>
                    ) : null}
                  </>
                ) : (
                  <View style={styles.missingServerView}>
                    <AppText variant="body" color={colors.textMuted}>
                      Transaction state could not be loaded from server or was removed on backend.
                    </AppText>
                  </View>
                )}
              </View>
            </View>

            {/* Resolution Action Buttons */}
            <View style={styles.actionContainer}>
              <PrimaryButton
                title="Keep Server Version"
                onPress={handleKeepServer}
                isLoading={resolving}
                disabled={resolving}
                style={[styles.btnMargin, { backgroundColor: colors.success }]}
              />

              <PrimaryButton
                title="Keep My Offline Version"
                onPress={handleKeepLocal}
                isLoading={resolving}
                disabled={resolving}
                variant="outline"
                style={styles.btnMargin}
              />

              <PrimaryButton
                title="Edit & Resync"
                onPress={() => setIsEditing(true)}
                disabled={resolving}
                variant="outline"
                style={{ borderColor: colors.primary }}
              />
            </View>
          </>
        )}
      </ScrollView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingBottom: spacing.xxl,
  },
  title: {
    marginBottom: spacing.xs,
  },
  subtitle: {
    marginBottom: spacing.md,
  },
  warningCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.lg,
    borderWidth: 1.5,
    borderColor: colors.warning,
  },
  warningTitle: {
    marginBottom: 4,
  },
  comparisonContainer: {
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
  versionCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  localCard: {
    borderColor: colors.primary,
  },
  serverCard: {
    borderColor: colors.success,
  },
  versionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    paddingBottom: spacing.xs,
  },
  fieldRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  missingServerView: {
    paddingVertical: spacing.md,
  },
  actionContainer: {
    marginTop: spacing.sm,
  },
  btnMargin: {
    marginBottom: spacing.md,
  },
  editSection: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  editHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
});
