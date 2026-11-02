/** Movimiento del ledger del vendedor. */
export interface LedgerEntry {
  id: number;
  type: string;
  reference?: string | null;
  description?: string | null;
  amount: number;
  commission: number;
  balanceAfter: number;
  createdAt: string;
}

/** Estado de una solicitud de retiro. */
export type WithdrawalStatus = 'PENDING' | 'REVIEW' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';

/** Método de retiro. */
export type WithdrawalMethod = 'BRE_B' | 'BANK_ACCOUNT';

/** Solicitud de retiro. */
export interface Withdrawal {
  id: number;
  sellerName: string;
  amount: number;
  fee: number;
  netAmount: number;
  method: WithdrawalMethod;
  breBKey?: string | null;
  bankName?: string | null;
  accountType?: string | null;
  accountNumber?: string | null;
  accountHolder?: string | null;
  status: WithdrawalStatus;
  notes?: string | null;
  reference?: string | null;
  createdAt: string;
  resolvedAt?: string | null;
}

/** Resumen financiero del vendedor. */
export interface SellerFinance {
  available: number;
  pending: number;
  reserved: number;
  withdrawnTotal: number;
  totalSales: number;
  commissionTotal: number;
  movements: LedgerEntry[];
  withdrawals: Withdrawal[];
  minWithdrawal: number;
  bankFee: number;
}
