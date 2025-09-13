package com.ethereum.wallet.service;

import com.ethereum.wallet.dto.*;
import com.ethereum.wallet.entity.Transaction;
import com.ethereum.wallet.entity.Wallet;
import com.ethereum.wallet.repository.TransactionRepository;
import com.ethereum.wallet.repository.WalletRepository;
import com.ethereum.wallet.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.*;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletService {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final Web3jService web3jService;
    private final TransactionService transactionService;
    
    @Autowired
    public WalletService(WalletRepository walletRepository, 
                        TransactionRepository transactionRepository,
                        Web3jService web3jService,
                        TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.web3jService = web3jService;
        this.transactionService = transactionService;
    }
    
    /**
     * Create a new wallet
     */
    public WalletResponse createWallet(WalletCreateRequest request) {
        logger.info("Creating new wallet with name: {}", request.getName());
        
        try {
            // Generate new key pair
            ECKeyPair keyPair = CryptoUtil.generateKeyPair();
            String address = CryptoUtil.generateAddress(keyPair);
            
            // Check if address already exists (extremely unlikely but good to check)
            if (walletRepository.existsByAddress(address)) {
                throw new RuntimeException("Address collision detected. Please try again.");
            }
            
            // Encrypt private key
            String privateKeyHex = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());
            String encryptedPrivateKey = CryptoUtil.encryptPrivateKey(privateKeyHex, request.getPassword());
            
            // Create and save wallet
            Wallet wallet = new Wallet(request.getName(), address, encryptedPrivateKey);
            wallet = walletRepository.save(wallet);
            
            logger.info("Successfully created wallet with address: {}", address);
            
            // Return response with balance
            WalletResponse response = new WalletResponse(wallet);
            response.setBalance(web3jService.getBalanceEth(address));
            response.setTransactionCount(0L);
            
            return response;
        } catch (Exception e) {
            logger.error("Failed to create wallet", e);
            throw new RuntimeException("Failed to create wallet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Import wallet from private key or mnemonic
     */
    public WalletResponse importWallet(WalletImportRequest request) {
        logger.info("Importing wallet with name: {}", request.getName());
        
        try {
            ECKeyPair keyPair;
            
            // Determine if input is mnemonic or private key
            if (CryptoUtil.isValidMnemonic(request.getPrivateKeyOrMnemonic())) {
                // Import from mnemonic
                String mnemonicPassword = request.getMnemonicPassword() != null ? request.getMnemonicPassword() : "";
                keyPair = CryptoUtil.createKeyPairFromMnemonic(request.getPrivateKeyOrMnemonic(), mnemonicPassword);
            } else if (CryptoUtil.isValidPrivateKey(request.getPrivateKeyOrMnemonic())) {
                // Import from private key
                String privateKeyHex = request.getPrivateKeyOrMnemonic();
                if (!privateKeyHex.startsWith("0x")) {
                    privateKeyHex = "0x" + privateKeyHex;
                }
                BigInteger privateKey = Numeric.toBigInt(privateKeyHex);
                keyPair = ECKeyPair.create(privateKey);
            } else {
                throw new IllegalArgumentException("Invalid private key or mnemonic phrase");
            }
            
            String address = CryptoUtil.generateAddress(keyPair);
            
            // Check if wallet already exists
            if (walletRepository.existsByAddress(address)) {
                throw new RuntimeException("Wallet with this address already exists");
            }
            
            // Encrypt private key
            String privateKeyHex = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());
            String encryptedPrivateKey = CryptoUtil.encryptPrivateKey(privateKeyHex, request.getPassword());
            
            // Create and save wallet
            Wallet wallet = new Wallet(request.getName(), address, encryptedPrivateKey);
            wallet = walletRepository.save(wallet);
            
            logger.info("Successfully imported wallet with address: {}", address);
            
            // Return response with balance
            WalletResponse response = new WalletResponse(wallet);
            response.setBalance(web3jService.getBalanceEth(address));
            response.setTransactionCount(transactionRepository.countConfirmedTransactionsByWallet(wallet));
            
            return response;
        } catch (Exception e) {
            logger.error("Failed to import wallet", e);
            throw new RuntimeException("Failed to import wallet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get wallet by ID
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        WalletResponse response = new WalletResponse(wallet);
        response.setBalance(web3jService.getBalanceEth(wallet.getAddress()));
        response.setTransactionCount(transactionRepository.countConfirmedTransactionsByWallet(wallet));
        
        return response;
    }
    
    /**
     * Get all active wallets
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {
        List<Wallet> wallets = walletRepository.findByIsActiveTrue();
        
        return wallets.stream()
                .map(wallet -> {
                    WalletResponse response = new WalletResponse(wallet);
                    response.setBalance(web3jService.getBalanceEth(wallet.getAddress()));
                    response.setTransactionCount(transactionRepository.countConfirmedTransactionsByWallet(wallet));
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Transfer ETH from wallet
     */
    public String transferEth(TransferRequest request) {
        logger.info("Processing ETH transfer from wallet ID: {} to address: {}", 
                   request.getWalletId(), request.getToAddress());
        
        try {
            // Get wallet
            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            // Validate recipient address
            if (!CryptoUtil.isValidAddress(request.getToAddress())) {
                throw new IllegalArgumentException("Invalid recipient address");
            }
            
            // Decrypt private key
            String privateKeyHex = CryptoUtil.decryptPrivateKey(wallet.getEncryptedPrivateKey(), request.getPassword());
            BigInteger privateKey = Numeric.toBigInt(privateKeyHex);
            ECKeyPair keyPair = ECKeyPair.create(privateKey);
            
            // Verify wallet address matches
            String derivedAddress = CryptoUtil.generateAddress(keyPair);
            if (!derivedAddress.equalsIgnoreCase(wallet.getAddress())) {
                throw new RuntimeException("Invalid password or corrupted wallet data");
            }
            
            // Check balance
            BigDecimal balance = web3jService.getBalanceEth(wallet.getAddress());
            if (balance.compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            
            // Get transaction parameters
            BigInteger gasPrice = request.getGasPrice() != null 
                ? Convert.toWei(request.getGasPrice(), Convert.Unit.GWEI).toBigInteger()
                : web3jService.getGasPrice();
            
            BigInteger gasLimit = request.getGasLimit() != null 
                ? BigInteger.valueOf(request.getGasLimit())
                : web3jService.estimateGas(wallet.getAddress(), request.getToAddress(), 
                                         Convert.toWei(request.getAmount(), Convert.Unit.ETHER).toBigInteger(), "0x");
            
            BigInteger nonce = web3jService.getTransactionCount(wallet.getAddress());
            BigInteger value = Convert.toWei(request.getAmount(), Convert.Unit.ETHER).toBigInteger();
            
            // Create transaction
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, request.getToAddress(), value);
            
            // Sign transaction
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, web3jService.getChainId(), keyPair);
            String hexValue = Numeric.toHexString(signedMessage);
            
            // Send transaction
            String transactionHash = web3jService.sendRawTransaction(hexValue);
            
            // Save transaction record
            Transaction transaction = new Transaction(wallet, transactionHash, wallet.getAddress(), 
                                                    request.getToAddress(), request.getAmount());
            transaction.setGasPrice(Convert.fromWei(gasPrice.toString(), Convert.Unit.ETHER));
            transaction.setGasLimit(gasLimit.longValue());
            transactionRepository.save(transaction);
            
            logger.info("Successfully sent ETH transfer. Transaction hash: {}", transactionHash);
            
            return transactionHash;
        } catch (Exception e) {
            logger.error("Failed to transfer ETH", e);
            throw new RuntimeException("Failed to transfer ETH: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get wallet balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        return web3jService.getBalanceEth(wallet.getAddress());
    }
    
    /**
     * Get wallet by address
     */
    @Transactional(readOnly = true)
    public Optional<WalletResponse> getWalletByAddress(String address) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        
        if (walletOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Wallet wallet = walletOpt.get();
        WalletResponse response = new WalletResponse(wallet);
        response.setBalance(web3jService.getBalanceEth(wallet.getAddress()));
        response.setTransactionCount(transactionRepository.countConfirmedTransactionsByWallet(wallet));
        
        return Optional.of(response);
    }
    
    /**
     * Deactivate wallet
     */
    public void deactivateWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        wallet.setIsActive(false);
        walletRepository.save(wallet);
        
        logger.info("Deactivated wallet with ID: {}", walletId);
    }
    
    /**
     * Update wallet name
     */
    public WalletResponse updateWalletName(Long walletId, String newName) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        wallet.setName(newName);
        wallet = walletRepository.save(wallet);
        
        WalletResponse response = new WalletResponse(wallet);
        response.setBalance(web3jService.getBalanceEth(wallet.getAddress()));
        response.setTransactionCount(transactionRepository.countConfirmedTransactionsByWallet(wallet));
        
        return response;
    }
    
    /**
     * Export wallet private key (encrypted)
     */
    public String exportPrivateKey(Long walletId, String password) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        try {
            // Decrypt and return private key
            return CryptoUtil.decryptPrivateKey(wallet.getEncryptedPrivateKey(), password);
        } catch (Exception e) {
            logger.error("Failed to export private key", e);
            throw new RuntimeException("Invalid password or failed to decrypt private key");
        }
    }
    
    /**
     * Validate wallet password
     */
    public boolean validatePassword(Long walletId, String password) {
        try {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            CryptoUtil.decryptPrivateKey(wallet.getEncryptedPrivateKey(), password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
