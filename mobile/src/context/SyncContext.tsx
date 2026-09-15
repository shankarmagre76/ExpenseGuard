import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import { AuthContext } from './AuthContext';
import { networkService } from '../services/networkService';
import { syncService, SyncResult } from '../services/syncService';

export interface SyncContextType {
  isOnline: boolean;
  isSyncing: boolean;
  pendingCount: number;
  failedCount: number;
  conflictCount: number;
  triggerSync: () => Promise<SyncResult | null>;
  refreshSyncSummary: () => Promise<void>;
}

export const SyncContext = createContext<SyncContextType>({
  isOnline: true,
  isSyncing: false,
  pendingCount: 0,
  failedCount: 0,
  conflictCount: 0,
  triggerSync: async () => null,
  refreshSyncSummary: async () => {},
});

export const SyncProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, status } = useContext(AuthContext);
  const [isOnline, setIsOnline] = useState<boolean>(true);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [pendingCount, setPendingCount] = useState<number>(0);
  const [failedCount, setFailedCount] = useState<number>(0);
  const [conflictCount, setConflictCount] = useState<number>(0);

  const userId = user?.userId;

  const refreshSyncSummary = useCallback(async () => {
    if (!userId) {
      setPendingCount(0);
      setFailedCount(0);
      setConflictCount(0);
      return;
    }
    const summary = await syncService.getSyncSummary(userId);
    setPendingCount(summary.pendingCount);
    setFailedCount(summary.failedCount);
    setConflictCount(summary.conflictCount);
  }, [userId]);

  const triggerSync = useCallback(async (): Promise<SyncResult | null> => {
    if (!userId || status !== 'authenticated') return null;

    setIsSyncing(true);
    try {
      const result = await syncService.syncPendingOperations(userId);
      await refreshSyncSummary();
      return result;
    } finally {
      setIsSyncing(false);
    }
  }, [userId, status, refreshSyncSummary]);

  // Network listener & transition OFFLINE -> ONLINE
  useEffect(() => {
    networkService.isOnline().then((online) => {
      setIsOnline(online);
    });

    const unsubscribe = networkService.subscribe((onlineStatus: boolean) => {
      setIsOnline((prevOnline) => {
        // Transition from OFFLINE -> ONLINE
        if (!prevOnline && onlineStatus && userId && status === 'authenticated') {
          triggerSync();
        }
        return onlineStatus;
      });
    });

    return () => {
      unsubscribe();
    };
  }, [userId, status, triggerSync]);

  // Refresh summary when user changes or session restores
  useEffect(() => {
    refreshSyncSummary();
  }, [userId, refreshSyncSummary]);

  return (
    <SyncContext.Provider
      value={{
        isOnline,
        isSyncing,
        pendingCount,
        failedCount,
        conflictCount,
        triggerSync,
        refreshSyncSummary,
      }}
    >
      {children}
    </SyncContext.Provider>
  );
};
