import { OfflineTransactionItem } from './offline';
import { TransactionResponse } from './transaction';

export type ConflictResolutionChoice = 'KEEP_SERVER' | 'KEEP_LOCAL' | 'EDIT_RESYNC';

export interface ConflictDetail {
  clientOperationId: string;
  localItem: OfflineTransactionItem;
  serverTransaction?: TransactionResponse | null;
  errorCode?: string;
  errorMessage?: string;
  serverVersion?: number;
}
