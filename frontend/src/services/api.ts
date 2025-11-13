import axios, { AxiosResponse } from 'axios';
import { toast } from 'react-toastify';

import {
  Wallet,
  WalletCreateRequest,
  WalletImportRequest,
  TransferRequest,
  Transaction,
  TransactionStats,
  ContractInteraction,
  ContractCallRequest,
  ContractExecuteRequest,
  ContractStats,
  PaginatedResponse,
  DashboardStats,
  NetworkInfo
} from '../types';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || 'An error occurred';
    
    // Don't show toast for certain error types
    if (error.response?.status !== 404) {
      toast.error(message);
    }
    
    return Promise.reject(error);
  }
);

// Wallet API
export const walletApi = {
  // Get all wallets
  getWallets: (): Promise<AxiosResponse<Wallet[]>> =>
    api.get('/wallets'),

  // Get wallet by ID
  getWallet: (id: number): Promise<AxiosResponse<Wallet>> =>
    api.get(`/wallets/${id}`),

  // Get wallet by address
  getWalletByAddress: (address: string): Promise<AxiosResponse<Wallet>> =>
    api.get(`/wallets/address/${address}`),

  // Create new wallet
  createWallet: (data: WalletCreateRequest): Promise<AxiosResponse<Wallet>> =>
    api.post('/wallets', data),

  // Import wallet
  importWallet: (data: WalletImportRequest): Promise<AxiosResponse<Wallet>> =>
    api.post('/wallets/import', data),

  // Get wallet balance
  getWalletBalance: (id: number): Promise<AxiosResponse<{ balance: string }>> =>
    api.get(`/wallets/${id}/balance`),

  // Transfer ETH
  transferEth: (data: TransferRequest): Promise<AxiosResponse<{ transactionHash: string }>> =>
    api.post('/wallets/transfer', data),

  // Update wallet name
  updateWalletName: (id: number, name: string): Promise<AxiosResponse<Wallet>> =>
    api.put(`/wallets/${id}/name`, { name }),

  // Deactivate wallet
  deactivateWallet: (id: number): Promise<AxiosResponse<{ message: string }>> =>
    api.delete(`/wallets/${id}`),

  // Export private key
  exportPrivateKey: (id: number, password: string): Promise<AxiosResponse<{ privateKey: string }>> =>
    api.post(`/wallets/${id}/export`, { password }),

  // Validate password
  validatePassword: (id: number, password: string): Promise<AxiosResponse<{ valid: boolean }>> =>
    api.post(`/wallets/${id}/validate-password`, { password }),
};

// Transaction API
export const transactionApi = {
  // Get transaction by hash
  getTransaction: (hash: string): Promise<AxiosResponse<Transaction>> =>
    api.get(`/transactions/${hash}`),

  // Get wallet transactions (paginated)
  getWalletTransactions: (
    walletId: number,
    page: number = 0,
    size: number = 20
  ): Promise<AxiosResponse<PaginatedResponse<Transaction>>> =>
    api.get(`/transactions/wallet/${walletId}?page=${page}&size=${size}`),

  // Get all wallet transactions
  getAllWalletTransactions: (walletId: number): Promise<AxiosResponse<Transaction[]>> =>
    api.get(`/transactions/wallet/${walletId}/all`),

  // Get pending transactions
  getPendingTransactions: (): Promise<AxiosResponse<Transaction[]>> =>
    api.get('/transactions/pending'),

  // Get transaction statistics
  getTransactionStats: (walletId: number): Promise<AxiosResponse<TransactionStats>> =>
    api.get(`/transactions/wallet/${walletId}/stats`),

  // Get recent transactions by address
  getRecentTransactionsByAddress: (
    address: string,
    limit: number = 10
  ): Promise<AxiosResponse<Transaction[]>> =>
    api.get(`/transactions/address/${address}/recent?limit=${limit}`),

  // Cancel transaction
  cancelTransaction: (hash: string): Promise<AxiosResponse<Transaction>> =>
    api.post(`/transactions/${hash}/cancel`),

  // Update transaction status (admin)
  updateTransactionStatus: (
    hash: string,
    status: string
  ): Promise<AxiosResponse<Transaction>> =>
    api.put(`/transactions/${hash}/status`, { status }),
};

// Contract API
export const contractApi = {
  // Call contract function (read-only)
  callFunction: (data: ContractCallRequest): Promise<AxiosResponse<{ result: string; decodedResult: string }>> =>
    api.post('/contracts/call', data),

  // Execute contract function (state-changing)
  executeFunction: (data: ContractExecuteRequest): Promise<AxiosResponse<{ transactionHash: string }>> =>
    api.post('/contracts/execute', data),

  // Get wallet contract interactions
  getWalletContractInteractions: (walletId: number): Promise<AxiosResponse<ContractInteraction[]>> =>
    api.get(`/contracts/wallet/${walletId}/interactions`),

  // Get contract interactions
  getContractInteractions: (contractAddress: string): Promise<AxiosResponse<ContractInteraction[]>> =>
    api.get(`/contracts/${contractAddress}/interactions`),

  // Get contract interaction by transaction hash
  getContractInteractionByTxHash: (txHash: string): Promise<AxiosResponse<ContractInteraction>> =>
    api.get(`/contracts/interaction/${txHash}`),

  // Get contract statistics
  getContractStats: (walletId: number): Promise<AxiosResponse<ContractStats>> =>
    api.get(`/contracts/wallet/${walletId}/stats`),

  // Verify interaction integrity
  verifyInteractionIntegrity: (
    interactionId: number,
    expectedHash: string
  ): Promise<AxiosResponse<{ valid: boolean }>> =>
    api.post(`/contracts/interaction/${interactionId}/verify`, { expectedHash }),
};

// Dashboard API
export const dashboardApi = {
  // Get dashboard statistics
  getDashboardStats: (): Promise<AxiosResponse<DashboardStats>> =>
    api.get('/dashboard/stats'),

  // Get network information
  getNetworkInfo: (): Promise<AxiosResponse<NetworkInfo>> =>
    api.get('/dashboard/network'),
};

// Utility functions
export const formatEthAddress = (address: string): string => {
  if (!address || address.length < 10) return address;
  return `${address.slice(0, 6)}...${address.slice(-4)}`;
};

export const formatEthAmount = (amount: string | number): string => {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (num === 0) return '0';
  if (num < 0.000001) return '< 0.000001';
  if (num < 1) return num.toFixed(6);
  if (num < 1000) return num.toFixed(4);
  return num.toLocaleString(undefined, { maximumFractionDigits: 2 });
};

export const formatTransactionHash = (hash: string): string => {
  if (!hash || hash.length < 10) return hash;
  return `${hash.slice(0, 8)}...${hash.slice(-6)}`;
};

export const getEtherscanUrl = (hash: string, type: 'tx' | 'address' = 'tx'): string => {
  const baseUrl = 'https://sepolia.etherscan.io';
  return `${baseUrl}/${type}/${hash}`;
};

export const isValidEthAddress = (address: string): boolean => {
  return /^0x[a-fA-F0-9]{40}$/.test(address);
};

export const isValidPrivateKey = (key: string): boolean => {
  const cleanKey = key.startsWith('0x') ? key.slice(2) : key;
  return /^[a-fA-F0-9]{64}$/.test(cleanKey);
};

export default api;
