package com.ethereum.wallet.service;

import com.ethereum.wallet.entity.Transaction;
import com.ethereum.wallet.entity.Wallet;
import com.ethereum.wallet.repository.TransactionRepository;
import com.ethereum.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final Web3jService web3jService;
    
    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                             WalletRepository walletRepository,
                             Web3jService web3jService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.web3jService = web3jService;
    }
    
    /**
     * Get transaction by hash
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionByHash(String transactionHash) {
        return transactionRepository.findByTransactionHash(transactionHash);
    }
    
    /**
     * Get transactions for a wallet
     */
    @Transactional(readOnly = true)
    public List<Transaction> getWalletTransactions(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }
    
    /**
     * Get transactions for a wallet with pagination
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getWalletTransactions(Long walletId, Pageable pageable) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable);
    }
    
    /**
     * Get pending transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus(Transaction.TransactionStatus.PENDING);
    }
    
    /**
     * Update transaction status
     */
    public Transaction updateTransactionStatus(String transactionHash, Transaction.TransactionStatus status) {
        Transaction transaction = transactionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        transaction.setStatus(status);
        
        if (status == Transaction.TransactionStatus.CONFIRMED) {
            transaction.setConfirmedAt(LocalDateTime.now());
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Update transaction with receipt information
     */
    public Transaction updateTransactionWithReceipt(String transactionHash, TransactionReceipt receipt) {
        Transaction transaction = transactionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        transaction.setBlockNumber(receipt.getBlockNumber().longValue());
        transaction.setTransactionIndex(receipt.getTransactionIndex().intValue());
        transaction.setGasUsed(receipt.getGasUsed().longValue());
        
        // Update status based on receipt
        if (receipt.isStatusOK()) {
            transaction.setStatus(Transaction.TransactionStatus.CONFIRMED);
            transaction.setConfirmedAt(LocalDateTime.now());
        } else {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Monitor and update pending transactions
     */
    @Async
    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void monitorPendingTransactions() {
        logger.debug("Checking pending transactions...");
        
        List<Transaction> pendingTransactions = getPendingTransactions();
        
        for (Transaction transaction : pendingTransactions) {
            try {
                Optional<TransactionReceipt> receiptOpt = web3jService.getTransactionReceipt(transaction.getTransactionHash());
                
                if (receiptOpt.isPresent()) {
                    TransactionReceipt receipt = receiptOpt.get();
                    updateTransactionWithReceipt(transaction.getTransactionHash(), receipt);
                    logger.info("Updated transaction {} with receipt", transaction.getTransactionHash());
                } else {
                    // Check if transaction exists on network
                    Optional<org.web3j.protocol.core.methods.response.Transaction> txOpt = 
                        web3jService.getTransaction(transaction.getTransactionHash());
                    
                    if (txOpt.isEmpty()) {
                        // Transaction not found on network, might have been dropped
                        // Only mark as failed if it's been more than 10 minutes
                        if (transaction.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                            transaction.setStatus(Transaction.TransactionStatus.FAILED);
                            transactionRepository.save(transaction);
                            logger.warn("Marked transaction {} as failed - not found on network", transaction.getTransactionHash());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error monitoring transaction {}", transaction.getTransactionHash(), e);
            }
        }
    }
    
    /**
     * Get transaction statistics for a wallet
     */
    @Transactional(readOnly = true)
    public TransactionStats getWalletTransactionStats(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        List<Transaction> transactions = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
        
        long totalCount = transactions.size();
        long confirmedCount = transactions.stream()
                .mapToLong(tx -> tx.getStatus() == Transaction.TransactionStatus.CONFIRMED ? 1 : 0)
                .sum();
        long pendingCount = transactions.stream()
                .mapToLong(tx -> tx.getStatus() == Transaction.TransactionStatus.PENDING ? 1 : 0)
                .sum();
        long failedCount = transactions.stream()
                .mapToLong(tx -> tx.getStatus() == Transaction.TransactionStatus.FAILED ? 1 : 0)
                .sum();
        
        BigDecimal totalSent = transactions.stream()
                .filter(tx -> tx.getFromAddress().equalsIgnoreCase(wallet.getAddress()))
                .filter(tx -> tx.getStatus() == Transaction.TransactionStatus.CONFIRMED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReceived = transactions.stream()
                .filter(tx -> tx.getToAddress().equalsIgnoreCase(wallet.getAddress()))
                .filter(tx -> tx.getStatus() == Transaction.TransactionStatus.CONFIRMED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalGasFees = transactions.stream()
                .filter(tx -> tx.getFromAddress().equalsIgnoreCase(wallet.getAddress()))
                .filter(tx -> tx.getStatus() == Transaction.TransactionStatus.CONFIRMED)
                .filter(tx -> tx.getGasPrice() != null && tx.getGasUsed() != null)
                .map(tx -> {
                    BigDecimal gasPrice = tx.getGasPrice();
                    BigDecimal gasUsed = BigDecimal.valueOf(tx.getGasUsed());
                    return gasPrice.multiply(gasUsed);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new TransactionStats(totalCount, confirmedCount, pendingCount, failedCount, 
                                  totalSent, totalReceived, totalGasFees);
    }
    
    /**
     * Get recent transactions for an address
     */
    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactionsByAddress(String address, int limit) {
        List<Transaction> transactions = transactionRepository.findByFromAddressOrToAddressOrderByCreatedAtDesc(address);
        return transactions.stream().limit(limit).toList();
    }
    
    /**
     * Cancel pending transaction (mark as cancelled)
     */
    public Transaction cancelTransaction(String transactionHash) {
        Transaction transaction = transactionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be cancelled");
        }
        
        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
        return transactionRepository.save(transaction);
    }
    
    /**
     * Transaction statistics class
     */
    public static class TransactionStats {
        private final long totalCount;
        private final long confirmedCount;
        private final long pendingCount;
        private final long failedCount;
        private final BigDecimal totalSent;
        private final BigDecimal totalReceived;
        private final BigDecimal totalGasFees;
        
        public TransactionStats(long totalCount, long confirmedCount, long pendingCount, long failedCount,
                              BigDecimal totalSent, BigDecimal totalReceived, BigDecimal totalGasFees) {
            this.totalCount = totalCount;
            this.confirmedCount = confirmedCount;
            this.pendingCount = pendingCount;
            this.failedCount = failedCount;
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
            this.totalGasFees = totalGasFees;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getConfirmedCount() { return confirmedCount; }
        public long getPendingCount() { return pendingCount; }
        public long getFailedCount() { return failedCount; }
        public BigDecimal getTotalSent() { return totalSent; }
        public BigDecimal getTotalReceived() { return totalReceived; }
        public BigDecimal getTotalGasFees() { return totalGasFees; }
    }
}
