export interface Transfer {
  id: string;
  transferType: 'A2A' | 'P2P';
  sourceAccountId: string;
  destinationAccountId: string;
  sourceReference?: string;
  destinationReference?: string;
  amount: number;
  currencyCode: string;
  feeAmount: number;
  feeCurrency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  failureReason?: string;
  ledgerJournalId?: string;
  channel: string;
  originalTransferId?: string;
  createdAt: string;
  completedAt?: string;
}

export interface TransferLimit {
  id: string;
  transferType: string;
  perTransferMax: number;
  dailyMaxAmount: number;
  dailyMaxCount: number;
  currencyCode: string;
  status: string;
  createdAt: string;
}

export interface TransferBeneficiary {
  id: string;
  ownerCardholderId: string;
  alias: string;
  maskedPan?: string;
  accountNumber?: string;
  iban?: string;
  status: string;
  createdAt: string;
}
