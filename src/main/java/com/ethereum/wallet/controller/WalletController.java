package com.ethereum.wallet.controller;

import com.ethereum.wallet.dto.*;
import com.ethereum.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "*")
public class WalletController {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
    
    private final WalletService walletService;
    
    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    /**
     * Create a new wallet
     */
    @PostMapping
    public ResponseEntity<?> createWallet(@Valid @RequestBody WalletCreateRequest request) {
        try {
            logger.info("Creating wallet: {}", request.getName());
            WalletResponse wallet = walletService.createWallet(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
        } catch (Exception e) {
            logger.error("Error creating wallet", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create wallet", "message", e.getMessage()));
        }
    }
    
    /**
     * Import a wallet
     */
    @PostMapping("/import")
    public ResponseEntity<?> importWallet(@Valid @RequestBody WalletImportRequest request) {
        try {
            logger.info("Importing wallet: {}", request.getName());
            WalletResponse wallet = walletService.importWallet(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
        } catch (Exception e) {
            logger.error("Error importing wallet", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to import wallet", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all wallets
     */
    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        try {
            List<WalletResponse> wallets = walletService.getAllWallets();
            return ResponseEntity.ok(wallets);
        } catch (Exception e) {
            logger.error("Error getting wallets", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get wallet by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWallet(@PathVariable Long id) {
        try {
            WalletResponse wallet = walletService.getWallet(id);
            return ResponseEntity.ok(wallet);
        } catch (RuntimeException e) {
            logger.error("Error getting wallet {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting wallet {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get wallet by address
     */
    @GetMapping("/address/{address}")
    public ResponseEntity<?> getWalletByAddress(@PathVariable String address) {
        try {
            Optional<WalletResponse> wallet = walletService.getWalletByAddress(address);
            if (wallet.isPresent()) {
                return ResponseEntity.ok(wallet.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting wallet by address {}", address, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get wallet balance
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getWalletBalance(@PathVariable Long id) {
        try {
            BigDecimal balance = walletService.getWalletBalance(id);
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (RuntimeException e) {
            logger.error("Error getting balance for wallet {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting balance for wallet {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Transfer ETH
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transferEth(@Valid @RequestBody TransferRequest request) {
        try {
            logger.info("Processing ETH transfer from wallet {} to {}", request.getWalletId(), request.getToAddress());
            String transactionHash = walletService.transferEth(request);
            return ResponseEntity.ok(Map.of("transactionHash", transactionHash));
        } catch (Exception e) {
            logger.error("Error transferring ETH", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to transfer ETH", "message", e.getMessage()));
        }
    }
    
    /**
     * Update wallet name
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateWalletName(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newName = request.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Name is required"));
            }
            
            WalletResponse wallet = walletService.updateWalletName(id, newName.trim());
            return ResponseEntity.ok(wallet);
        } catch (RuntimeException e) {
            logger.error("Error updating wallet name for {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating wallet name for {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Deactivate wallet
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateWallet(@PathVariable Long id) {
        try {
            walletService.deactivateWallet(id);
            return ResponseEntity.ok(Map.of("message", "Wallet deactivated successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deactivating wallet {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deactivating wallet {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Export private key
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<?> exportPrivateKey(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }
            
            String privateKey = walletService.exportPrivateKey(id, password);
            return ResponseEntity.ok(Map.of("privateKey", privateKey));
        } catch (RuntimeException e) {
            logger.error("Error exporting private key for wallet {}", id, e);
            if (e.getMessage().contains("password")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid password"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error exporting private key for wallet {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Validate password
     */
    @PostMapping("/{id}/validate-password")
    public ResponseEntity<?> validatePassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }
            
            boolean isValid = walletService.validatePassword(id, password);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            logger.error("Error validating password for wallet {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
}
