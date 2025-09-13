package com.ethereum.wallet.service;

import com.ethereum.wallet.entity.ContractInteraction;
import com.ethereum.wallet.entity.Wallet;
import com.ethereum.wallet.repository.ContractInteractionRepository;
import com.ethereum.wallet.repository.WalletRepository;
import com.ethereum.wallet.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContractService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);
    
    private final ContractInteractionRepository contractInteractionRepository;
    private final WalletRepository walletRepository;
    private final Web3jService web3jService;
    
    @Autowired
    public ContractService(ContractInteractionRepository contractInteractionRepository,
                          WalletRepository walletRepository,
                          Web3jService web3jService) {
        this.contractInteractionRepository = contractInteractionRepository;
        this.walletRepository = walletRepository;
        this.web3jService = web3jService;
    }
    
    /**
     * Call a read-only contract function
     */
    @Transactional(readOnly = true)
    public String callFunction(Long walletId, String contractAddress, Function function) {
        logger.info("Calling read-only function {} on contract {}", function.getName(), contractAddress);
        
        try {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            // Validate contract address
            if (!CryptoUtil.isValidAddress(contractAddress)) {
                throw new IllegalArgumentException("Invalid contract address");
            }
            
            // Encode function call
            String encodedFunction = FunctionEncoder.encode(function);
            
            // Calculate integrity hash
            String integrityHash = CryptoUtil.calculateIntegrityHash(encodedFunction);
            logger.debug("Function call integrity hash: {}", integrityHash);
            
            // Make the call
            String result = web3jService.call(wallet.getAddress(), contractAddress, encodedFunction);
            
            // Verify result integrity
            String resultHash = CryptoUtil.calculateIntegrityHash(result);
            logger.debug("Function result integrity hash: {}", resultHash);
            
            // Save interaction record
            ContractInteraction interaction = new ContractInteraction(
                wallet, contractAddress, function.getName(), 
                encodedFunction.substring(0, Math.min(10, encodedFunction.length())), encodedFunction);
            interaction.setOutputData(result);
            interaction.setStatus(ContractInteraction.InteractionStatus.SUCCESS);
            contractInteractionRepository.save(interaction);
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to call contract function", e);
            throw new RuntimeException("Failed to call contract function: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute a state-changing contract function
     */
    public String executeFunction(Long walletId, String contractAddress, Function function, 
                                String password, BigInteger gasPrice, BigInteger gasLimit, BigInteger value) {
        logger.info("Executing function {} on contract {}", function.getName(), contractAddress);
        
        try {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            // Validate contract address
            if (!CryptoUtil.isValidAddress(contractAddress)) {
                throw new IllegalArgumentException("Invalid contract address");
            }
            
            // Decrypt private key
            String privateKeyHex = CryptoUtil.decryptPrivateKey(wallet.getEncryptedPrivateKey(), password);
            BigInteger privateKey = Numeric.toBigInt(privateKeyHex);
            ECKeyPair keyPair = ECKeyPair.create(privateKey);
            
            // Verify wallet address matches
            String derivedAddress = CryptoUtil.generateAddress(keyPair);
            if (!derivedAddress.equalsIgnoreCase(wallet.getAddress())) {
                throw new RuntimeException("Invalid password or corrupted wallet data");
            }
            
            // Encode function call
            String encodedFunction = FunctionEncoder.encode(function);
            
            // Calculate integrity hash for input
            String inputIntegrityHash = CryptoUtil.calculateIntegrityHash(encodedFunction);
            logger.debug("Function input integrity hash: {}", inputIntegrityHash);
            
            // Get transaction parameters
            BigInteger nonce = web3jService.getTransactionCount(wallet.getAddress());
            
            if (gasPrice == null) {
                gasPrice = web3jService.getGasPrice();
            }
            
            if (gasLimit == null) {
                gasLimit = web3jService.estimateGas(wallet.getAddress(), contractAddress, value, encodedFunction);
            }
            
            if (value == null) {
                value = BigInteger.ZERO;
            }
            
            // Create transaction
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, value, encodedFunction);
            
            // Sign transaction
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, web3jService.getChainId(), keyPair);
            String hexValue = Numeric.toHexString(signedMessage);
            
            // Send transaction
            String transactionHash = web3jService.sendRawTransaction(hexValue);
            
            // Save interaction record
            ContractInteraction interaction = new ContractInteraction(
                wallet, contractAddress, function.getName(), 
                encodedFunction.substring(0, Math.min(10, encodedFunction.length())), encodedFunction);
            interaction.setTransactionHash(transactionHash);
            interaction.setStatus(ContractInteraction.InteractionStatus.PENDING);
            contractInteractionRepository.save(interaction);
            
            logger.info("Successfully executed contract function. Transaction hash: {}", transactionHash);
            
            return transactionHash;
        } catch (Exception e) {
            logger.error("Failed to execute contract function", e);
            throw new RuntimeException("Failed to execute contract function: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decode function result
     */
    public List<Type> decodeFunctionResult(String encodedResult, Function function) {
        try {
            return FunctionReturnDecoder.decode(encodedResult, function.getOutputParameters());
        } catch (Exception e) {
            logger.error("Failed to decode function result", e);
            throw new RuntimeException("Failed to decode function result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get contract interactions for a wallet
     */
    @Transactional(readOnly = true)
    public List<ContractInteraction> getWalletContractInteractions(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        return contractInteractionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }
    
    /**
     * Get contract interactions for a specific contract
     */
    @Transactional(readOnly = true)
    public List<ContractInteraction> getContractInteractions(String contractAddress) {
        return contractInteractionRepository.findByContractAddressOrderByCreatedAtDesc(contractAddress);
    }
    
    /**
     * Get contract interaction by transaction hash
     */
    @Transactional(readOnly = true)
    public Optional<ContractInteraction> getContractInteractionByTxHash(String transactionHash) {
        return contractInteractionRepository.findByTransactionHash(transactionHash);
    }
    
    /**
     * Update contract interaction with transaction receipt
     */
    public ContractInteraction updateContractInteractionWithReceipt(String transactionHash, TransactionReceipt receipt) {
        ContractInteraction interaction = contractInteractionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new RuntimeException("Contract interaction not found"));
        
        // Update status based on receipt
        if (receipt.isStatusOK()) {
            interaction.setStatus(ContractInteraction.InteractionStatus.SUCCESS);
            
            // If there are logs, extract output data
            if (!receipt.getLogs().isEmpty()) {
                // This is a simplified approach - in practice, you'd decode specific events
                interaction.setOutputData(receipt.getLogs().toString());
            }
        } else {
            interaction.setStatus(ContractInteraction.InteractionStatus.REVERTED);
        }
        
        return contractInteractionRepository.save(interaction);
    }
    
    /**
     * Verify contract interaction integrity
     */
    public boolean verifyInteractionIntegrity(Long interactionId, String expectedHash) {
        try {
            ContractInteraction interaction = contractInteractionRepository.findById(interactionId)
                    .orElseThrow(() -> new RuntimeException("Contract interaction not found"));
            
            String dataToVerify = interaction.getInputData();
            if (interaction.getOutputData() != null) {
                dataToVerify += interaction.getOutputData();
            }
            
            return CryptoUtil.verifyIntegrity(dataToVerify, expectedHash);
        } catch (Exception e) {
            logger.error("Failed to verify contract interaction integrity", e);
            return false;
        }
    }
    
    /**
     * Get contract interaction statistics for a wallet
     */
    @Transactional(readOnly = true)
    public ContractInteractionStats getWalletContractStats(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        List<ContractInteraction> interactions = contractInteractionRepository.findByWalletOrderByCreatedAtDesc(wallet);
        
        long totalCount = interactions.size();
        long successCount = interactions.stream()
                .mapToLong(ci -> ci.getStatus() == ContractInteraction.InteractionStatus.SUCCESS ? 1 : 0)
                .sum();
        long pendingCount = interactions.stream()
                .mapToLong(ci -> ci.getStatus() == ContractInteraction.InteractionStatus.PENDING ? 1 : 0)
                .sum();
        long failedCount = interactions.stream()
                .mapToLong(ci -> ci.getStatus() == ContractInteraction.InteractionStatus.FAILED || 
                              ci.getStatus() == ContractInteraction.InteractionStatus.REVERTED ? 1 : 0)
                .sum();
        
        long uniqueContracts = interactions.stream()
                .map(ContractInteraction::getContractAddress)
                .distinct()
                .count();
        
        return new ContractInteractionStats(totalCount, successCount, pendingCount, failedCount, uniqueContracts);
    }
    
    /**
     * Contract interaction statistics class
     */
    public static class ContractInteractionStats {
        private final long totalCount;
        private final long successCount;
        private final long pendingCount;
        private final long failedCount;
        private final long uniqueContracts;
        
        public ContractInteractionStats(long totalCount, long successCount, long pendingCount, 
                                      long failedCount, long uniqueContracts) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.pendingCount = pendingCount;
            this.failedCount = failedCount;
            this.uniqueContracts = uniqueContracts;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getSuccessCount() { return successCount; }
        public long getPendingCount() { return pendingCount; }
        public long getFailedCount() { return failedCount; }
        public long getUniqueContracts() { return uniqueContracts; }
    }
}
