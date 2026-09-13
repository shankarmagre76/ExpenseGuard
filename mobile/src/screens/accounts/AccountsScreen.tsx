import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  RefreshControl,
  Alert,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { AccountModal } from '../../components/AccountModal';
import { useAccounts } from '../../hooks/useAccounts';
import { AccountResponse, AccountRequest } from '../../types/account';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';

export const AccountsScreen: React.FC = () => {
  const { accounts, loading, error, refresh, addAccount, editAccount, removeAccount } = useAccounts();

  const [modalVisible, setModalVisible] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<AccountResponse | null>(null);

  const handleOpenCreate = () => {
    setSelectedAccount(null);
    setModalVisible(true);
  };

  const handleOpenEdit = (account: AccountResponse) => {
    setSelectedAccount(account);
    setModalVisible(true);
  };

  const handleDelete = (account: AccountResponse) => {
    Alert.alert(
      'Delete Account',
      `Are you sure you want to delete "${account.name}"? This action cannot be undone.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => removeAccount(account.id),
        },
      ]
    );
  };

  const handleFormSubmit = async (data: AccountRequest) => {
    if (selectedAccount) {
      await editAccount(selectedAccount.id, data);
    } else {
      await addAccount(data);
    }
  };

  const renderAccountItem = ({ item }: { item: AccountResponse }) => (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.titleRow}>
          <AppText variant="subheader" bold style={styles.accountName}>
            {item.name}
          </AppText>
          <View style={styles.typeBadge}>
            <AppText variant="caption" bold color={colors.primary}>
              {item.type}
            </AppText>
          </View>
        </View>
        <AppText variant="title" color={colors.textPrimary} style={styles.balanceText}>
          {formatCurrency(item.balance, item.currency)}
        </AppText>
      </View>

      <View style={styles.actionRow}>
        <TouchableOpacity style={styles.actionBtn} onPress={() => handleOpenEdit(item)}>
          <AppText variant="caption" color={colors.primary} bold>
            Edit
          </AppText>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn} onPress={() => handleDelete(item)}>
          <AppText variant="caption" color={colors.error} bold>
            Delete
          </AppText>
        </TouchableOpacity>
      </View>
    </View>
  );

  return (
    <ScreenContainer style={styles.container}>
      {/* Top Header */}
      <View style={styles.headerRow}>
        <View>
          <AppText variant="header">Financial Accounts</AppText>
          <AppText variant="caption" color={colors.textSecondary}>
            Manage bank accounts, cards & cash balances
          </AppText>
        </View>
        <PrimaryButton
          title="+ Add"
          onPress={handleOpenCreate}
          style={styles.createBtn}
        />
      </View>

      {error ? (
        <ErrorMessage message={error} onRetry={refresh} />
      ) : null}

      {loading && accounts.length === 0 ? (
        <LoadingIndicator message="Loading financial accounts..." />
      ) : (
        <FlatList
          data={accounts}
          keyExtractor={(item) => item.id}
          renderItem={renderAccountItem}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} />}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <AppText variant="subheader" style={styles.emptyTitle}>
                No Accounts Found
              </AppText>
              <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
                You haven't created any financial accounts yet. Tap "+ Add" above to add your first account.
              </AppText>
              <PrimaryButton title="Create First Account" onPress={handleOpenCreate} style={styles.emptyBtn} />
            </View>
          }
        />
      )}

      {/* Account Modal */}
      <AccountModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        onSubmit={handleFormSubmit}
        accountToEdit={selectedAccount}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  createBtn: {
    height: 38,
    paddingHorizontal: spacing.md,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  cardHeader: {
    marginBottom: spacing.sm,
  },
  titleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  accountName: {
    fontSize: 18,
  },
  typeBadge: {
    backgroundColor: colors.primaryLight,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.full,
  },
  balanceText: {
    fontSize: 22,
    marginTop: spacing.xs,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingTop: spacing.sm,
  },
  actionBtn: {
    paddingHorizontal: spacing.xs,
    paddingVertical: spacing.xs,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.xxl,
    paddingHorizontal: spacing.lg,
  },
  emptyTitle: {
    marginBottom: spacing.xs,
  },
  emptyText: {
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  emptyBtn: {
    width: '100%',
  },
});
