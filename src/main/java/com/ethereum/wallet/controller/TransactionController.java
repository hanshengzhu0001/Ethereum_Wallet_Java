package com.ethereum.wallet.controller;

import com.ethereum.wallet.entity.Transaction;
import com.ethereum.wallet.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    
    private final TransactionService transactionService;
    
    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    /**
     * Get transaction by hash
     */
    @GetMapping("/{hash}")
    public ResponseEntity<?> getTransaction(@PathVariable String hash) {
        try {
            Optional<Transaction> transaction = transactionService.getTransactionByHash(hash);
            if (transaction.isPresent()) {
                return ResponseEntity.ok(transaction.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting transaction {}", hash, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get transactions for a wallet
     */
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<?> getWalletTransactions(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (size <= 0 || size > 100) {
                size = 20; // Default size
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = transactionService.getWalletTransactions(walletId, pageable);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            logger.error("Error getting transactions for wallet {}", walletId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting transactions for wallet {}", walletId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all transactions for a wallet (without pagination)
     */
    @GetMapping("/wallet/{walletId}/all")
    public ResponseEntity<?> getAllWalletTransactions(@PathVariable Long walletId) {
        try {
            List<Transaction> transactions = transactionService.getWalletTransactions(walletId);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            logger.error("Error getting all transactions for wallet {}", walletId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting all transactions for wallet {}", walletId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get pending transactions
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTransactions() {
        try {
            List<Transaction> transactions = transactionService.getPendingTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error getting pending transactions", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get transaction statistics for a wallet
     */
    @GetMapping("/wallet/{walletId}/stats")
    public ResponseEntity<?> getWalletTransactionStats(@PathVariable Long walletId) {
        try {
            TransactionService.TransactionStats stats = transactionService.getWalletTransactionStats(walletId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            logger.error("Error getting transaction stats for wallet {}", walletId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting transaction stats for wallet {}", walletId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get recent transactions by address
     */
    @GetMapping("/address/{address}/recent")
    public ResponseEntity<?> getRecentTransactionsByAddress(
            @PathVariable String address,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // Default limit
            }
            
            List<Transaction> transactions = transactionService.getRecentTransactionsByAddress(address, limit);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error getting recent transactions for address {}", address, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Cancel a pending transaction
     */
    @PostMapping("/{hash}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable String hash) {
        try {
            Transaction transaction = transactionService.cancelTransaction(hash);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            logger.error("Error cancelling transaction {}", hash, e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to cancel transaction", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling transaction {}", hash, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Update transaction status (admin endpoint)
     */
    @PutMapping("/{hash}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable String hash,
            @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }
            
            Transaction.TransactionStatus status;
            try {
                status = Transaction.TransactionStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status value"));
            }
            
            Transaction transaction = transactionService.updateTransactionStatus(hash, status);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            logger.error("Error updating transaction status for {}", hash, e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update transaction status", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating transaction status for {}", hash, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
