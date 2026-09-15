import { OfflineTransactionItem } from '../types/offline';

const STORAGE_KEY = '@expenseguard_offline_transactions';

let AsyncStorageModule: any = null;
try {
  AsyncStorageModule = require('@react-native-async-storage/async-storage').default;
} catch {
  AsyncStorageModule = null;
}

// In-memory store fallback for Jest runner or environments without native AsyncStorage
let inMemoryStore: OfflineTransactionItem[] = [];

const loadStore = async (): Promise<OfflineTransactionItem[]> => {
  try {
    if (AsyncStorageModule && typeof AsyncStorageModule.getItem === 'function') {
      const raw = await AsyncStorageModule.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as OfflineTransactionItem[];
        inMemoryStore = parsed;
        return parsed;
      }
    }
    return inMemoryStore;
  } catch {
    return inMemoryStore;
  }
};

const saveStore = async (store: OfflineTransactionItem[]): Promise<void> => {
  inMemoryStore = store;
  try {
    if (AsyncStorageModule && typeof AsyncStorageModule.setItem === 'function') {
      await AsyncStorageModule.setItem(STORAGE_KEY, JSON.stringify(store));
    }
  } catch {
    // Fallback retains inMemoryStore
  }
};

export const saveOfflineTransaction = async (item: OfflineTransactionItem): Promise<void> => {
  const store = await loadStore();
  const index = store.findIndex((i) => i.clientOperationId === item.clientOperationId);
  if (index >= 0) {
    store[index] = { ...store[index], ...item };
  } else {
    store.push(item);
  }
  await saveStore(store);
};

export const getOfflineTransactions = async (userId: string): Promise<OfflineTransactionItem[]> => {
  const store = await loadStore();
  return store.filter((item) => item.userId === userId);
};

export const getAllOfflineTransactions = async (): Promise<OfflineTransactionItem[]> => {
  return loadStore();
};

export const getPendingSyncQueue = async (userId: string): Promise<OfflineTransactionItem[]> => {
  const userItems = await getOfflineTransactions(userId);
  return userItems.filter((item) => item.status === 'PENDING' || item.status === 'SYNCING');
};

export const updateOfflineTransactionStatus = async (
  clientOperationId: string,
  updates: Partial<OfflineTransactionItem>
): Promise<void> => {
  const store = await loadStore();
  const index = store.findIndex((i) => i.clientOperationId === clientOperationId);
  if (index >= 0) {
    store[index] = { ...store[index], ...updates };
    await saveStore(store);
  }
};

export const removeOfflineTransaction = async (clientOperationId: string): Promise<void> => {
  const store = await loadStore();
  const filtered = store.filter((i) => i.clientOperationId !== clientOperationId);
  await saveStore(filtered);
};

export const clearUserOfflineData = async (userId: string): Promise<void> => {
  const store = await loadStore();
  const filtered = store.filter((i) => i.userId !== userId);
  await saveStore(filtered);
};

export const clearAllOfflineStorage = async (): Promise<void> => {
  inMemoryStore = [];
  try {
    if (AsyncStorageModule && typeof AsyncStorageModule.removeItem === 'function') {
      await AsyncStorageModule.removeItem(STORAGE_KEY);
    }
  } catch {
    // Ignore error
  }
};
