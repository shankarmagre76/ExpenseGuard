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
import { CategoryModal } from '../../components/CategoryModal';
import { useCategories } from '../../hooks/useCategories';
import { CategoryResponse, CategoryRequest, CategoryType } from '../../types/category';
import { colors, spacing, borderRadius } from '../../theme';

export const CategoriesScreen: React.FC = () => {
  const [activeTab, setActiveTab] = useState<CategoryType>('EXPENSE');

  const {
    categories,
    loading,
    error,
    refresh,
    addCategory,
    editCategory,
    removeCategory,
  } = useCategories(activeTab);

  const [modalVisible, setModalVisible] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<CategoryResponse | null>(null);

  const handleOpenCreate = () => {
    setSelectedCategory(null);
    setModalVisible(true);
  };

  const handleOpenEdit = (category: CategoryResponse) => {
    setSelectedCategory(category);
    setModalVisible(true);
  };

  const handleDelete = (category: CategoryResponse) => {
    Alert.alert(
      'Delete Category',
      `Are you sure you want to delete "${category.name}"? Transactions using this category will remain unaffected.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => removeCategory(category.id),
        },
      ]
    );
  };

  const handleFormSubmit = async (data: CategoryRequest) => {
    if (selectedCategory) {
      await editCategory(selectedCategory.id, data);
    } else {
      await addCategory(data);
    }
  };

  const renderCategoryItem = ({ item }: { item: CategoryResponse }) => (
    <View style={styles.card}>
      <View style={styles.cardRow}>
        <View style={styles.leftCol}>
          <AppText variant="subheader" bold style={styles.catName}>
            {item.name}
          </AppText>
          <AppText variant="caption" color={colors.textMuted}>
            Type: {item.type}
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
    </View>
  );

  return (
    <ScreenContainer style={styles.container}>
      {/* Header */}
      <View style={styles.headerRow}>
        <View>
          <AppText variant="header">Categories</AppText>
          <AppText variant="caption" color={colors.textSecondary}>
            Organize expenses and income streams
          </AppText>
        </View>
        <PrimaryButton title="+ Add" onPress={handleOpenCreate} style={styles.createBtn} />
      </View>

      {/* Tabs */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'EXPENSE' ? styles.tabActiveExpense : null]}
          onPress={() => setActiveTab('EXPENSE')}
        >
          <AppText
            variant="body"
            bold={activeTab === 'EXPENSE'}
            color={activeTab === 'EXPENSE' ? colors.surface : colors.textPrimary}
          >
            Expenses
          </AppText>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'INCOME' ? styles.tabActiveIncome : null]}
          onPress={() => setActiveTab('INCOME')}
        >
          <AppText
            variant="body"
            bold={activeTab === 'INCOME'}
            color={activeTab === 'INCOME' ? colors.surface : colors.textPrimary}
          >
            Income
          </AppText>
        </TouchableOpacity>
      </View>

      {error ? <ErrorMessage message={error} onRetry={refresh} /> : null}

      {loading && categories.length === 0 ? (
        <LoadingIndicator message={`Loading ${activeTab.toLowerCase()} categories...`} />
      ) : (
        <FlatList
          data={categories}
          keyExtractor={(item) => item.id}
          renderItem={renderCategoryItem}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} />}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <AppText variant="subheader" style={styles.emptyTitle}>
                No {activeTab.toLowerCase()} Categories
              </AppText>
              <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
                No {activeTab.toLowerCase()} categories found. Tap "+ Add" above to create one.
              </AppText>
              <PrimaryButton title={`Add ${activeTab} Category`} onPress={handleOpenCreate} style={styles.emptyBtn} />
            </View>
          }
        />
      )}

      {/* Category Modal */}
      <CategoryModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        onSubmit={handleFormSubmit}
        categoryToEdit={selectedCategory}
        defaultType={activeTab}
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
  tabContainer: {
    flexDirection: 'row',
    borderRadius: borderRadius.md,
    backgroundColor: colors.surface,
    padding: 3,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    alignItems: 'center',
    borderRadius: borderRadius.sm,
  },
  tabActiveExpense: {
    backgroundColor: colors.error,
  },
  tabActiveIncome: {
    backgroundColor: colors.success,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  cardRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  leftCol: {
    flex: 1,
  },
  catName: {
    fontSize: 16,
    marginBottom: 2,
  },
  actionRow: {
    flexDirection: 'row',
    gap: spacing.sm,
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
