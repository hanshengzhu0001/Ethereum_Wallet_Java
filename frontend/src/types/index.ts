// Wallet Types
export interface Wallet {
  id: number;
  name: string;
  address: string;
  balance: string;
  createdAt: string;
  updatedAt: string;
  isActive: boolean;
  transactionCount: number;
}

export interface WalletCreateRequest {
  name: string;
  password: string;
}

export interface WalletImportRequest {
  name: string;
  password: string;
  privateKeyOrMnemonic: string;
  mnemonicPassword?: string;
}

export interface TransferRequest {
  walletId: number;
  toAddress: string;
  amount: string;
  password: string;
  gasPrice?: string;
  gasLimit?: number;
}

// Transaction Types
export interface Transaction {
  id: number;
  transactionHash: string;
  fromAddress: string;
  toAddress: string;
  amount: string;
  gasPrice?: string;
  gasLimit?: number;
  gasUsed?: number;
  status: TransactionStatus;
  blockNumber?: number;
  transactionIndex?: number;
  createdAt: string;
  confirmedAt?: string;
}

export enum TransactionStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface TransactionStats {
  totalCount: number;
  confirmedCount: number;
  pendingCount: number;
  failedCount: number;
  totalSent: string;
  totalReceived: string;
  totalGasFees: string;
}

// Contract Types
export interface ContractInteraction {
  id: number;
  contractAddress: string;
  functionName: string;
  functionSignature: string;
  inputData?: string;
  outputData?: string;
  transactionHash?: string;
  status: ContractInteractionStatus;
  createdAt: string;
}

export enum ContractInteractionStatus {
  PENDING = 'PENDING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  REVERTED = 'REVERTED'
}

export interface ContractCallRequest {
  walletId: number;
  contractAddress: string;
  functionName: string;
  [key: string]: any; // Additional function parameters
}

export interface ContractExecuteRequest extends ContractCallRequest {
  password: string;
  gasPrice?: string;
  gasLimit?: number;
  value?: string;
}

export interface ContractStats {
  totalCount: number;
  successCount: number;
  pendingCount: number;
  failedCount: number;
  uniqueContracts: number;
}

// API Response Types
export interface ApiResponse<T> {
  data?: T;
  error?: string;
  message?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// UI State Types
export interface LoadingState {
  [key: string]: boolean;
}

export interface ErrorState {
  [key: string]: string | null;
}

// Form Types
export interface CreateWalletForm {
  name: string;
  password: string;
  confirmPassword: string;
}

export interface ImportWalletForm {
  name: string;
  password: string;
  confirmPassword: string;
  importType: 'privateKey' | 'mnemonic';
  privateKeyOrMnemonic: string;
  mnemonicPassword?: string;
}

export interface SendEthForm {
  toAddress: string;
  amount: string;
  password: string;
  useCustomGas: boolean;
  gasPrice?: string;
  gasLimit?: string;
}

export interface ContractCallForm {
  contractAddress: string;
  functionName: string;
  parameters: { [key: string]: string };
}

export interface ContractExecuteForm extends ContractCallForm {
  password: string;
  useCustomGas: boolean;
  gasPrice?: string;
  gasLimit?: string;
  value?: string;
}

// Dashboard Types
export interface DashboardStats {
  totalWallets: number;
  totalBalance: string;
  totalTransactions: number;
  pendingTransactions: number;
  recentTransactions: Transaction[];
}

// Network Types
export interface NetworkInfo {
  name: string;
  chainId: number;
  blockNumber: number;
  gasPrice: string;
  isConnected: boolean;
}
