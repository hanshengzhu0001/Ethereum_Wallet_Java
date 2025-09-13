package com.ethereum.wallet.controller;

import com.ethereum.wallet.entity.ContractInteraction;
import com.ethereum.wallet.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
public class ContractController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);
    
    private final ContractService contractService;
    
    @Autowired
    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }
    
    /**
     * Call a read-only contract function (example: ERC20 balanceOf)
     */
    @PostMapping("/call")
    public ResponseEntity<?> callContractFunction(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            String contractAddress = request.get("contractAddress").toString();
            String functionName = request.get("functionName").toString();
            
            // This is a simplified example - in practice, you'd have a more sophisticated
            // way to construct Function objects based on ABI
            Function function = createFunction(functionName, request);
            
            String result = contractService.callFunction(walletId, contractAddress, function);
            
            // Decode result if needed
            List<Type> decodedResult = contractService.decodeFunctionResult(result, function);
            
            return ResponseEntity.ok(Map.of(
                "result", result,
                "decodedResult", decodedResult.toString()
            ));
        } catch (Exception e) {
            logger.error("Error calling contract function", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to call contract function", "message", e.getMessage()));
        }
    }
    
    /**
     * Execute a state-changing contract function (example: ERC20 transfer)
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeContractFunction(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            String contractAddress = request.get("contractAddress").toString();
            String functionName = request.get("functionName").toString();
            String password = request.get("password").toString();
            
            // Optional parameters
            BigInteger gasPrice = request.containsKey("gasPrice") ? 
                new BigInteger(request.get("gasPrice").toString()) : null;
            BigInteger gasLimit = request.containsKey("gasLimit") ? 
                new BigInteger(request.get("gasLimit").toString()) : null;
            BigInteger value = request.containsKey("value") ? 
                new BigInteger(request.get("value").toString()) : BigInteger.ZERO;
            
            Function function = createFunction(functionName, request);
            
            String transactionHash = contractService.executeFunction(
                walletId, contractAddress, function, password, gasPrice, gasLimit, value);
            
            return ResponseEntity.ok(Map.of("transactionHash", transactionHash));
        } catch (Exception e) {
            logger.error("Error executing contract function", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to execute contract function", "message", e.getMessage()));
        }
    }
    
    /**
     * Get contract interactions for a wallet
     */
    @GetMapping("/wallet/{walletId}/interactions")
    public ResponseEntity<?> getWalletContractInteractions(@PathVariable Long walletId) {
        try {
            List<ContractInteraction> interactions = contractService.getWalletContractInteractions(walletId);
            return ResponseEntity.ok(interactions);
        } catch (RuntimeException e) {
            logger.error("Error getting contract interactions for wallet {}", walletId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting contract interactions for wallet {}", walletId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get contract interactions for a specific contract
     */
    @GetMapping("/{contractAddress}/interactions")
    public ResponseEntity<?> getContractInteractions(@PathVariable String contractAddress) {
        try {
            List<ContractInteraction> interactions = contractService.getContractInteractions(contractAddress);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            logger.error("Error getting interactions for contract {}", contractAddress, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get contract interaction by transaction hash
     */
    @GetMapping("/interaction/{txHash}")
    public ResponseEntity<?> getContractInteractionByTxHash(@PathVariable String txHash) {
        try {
            Optional<ContractInteraction> interaction = contractService.getContractInteractionByTxHash(txHash);
            if (interaction.isPresent()) {
                return ResponseEntity.ok(interaction.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting contract interaction for tx {}", txHash, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get contract interaction statistics for a wallet
     */
    @GetMapping("/wallet/{walletId}/stats")
    public ResponseEntity<?> getWalletContractStats(@PathVariable Long walletId) {
        try {
            ContractService.ContractInteractionStats stats = contractService.getWalletContractStats(walletId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            logger.error("Error getting contract stats for wallet {}", walletId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting contract stats for wallet {}", walletId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Verify contract interaction integrity
     */
    @PostMapping("/interaction/{interactionId}/verify")
    public ResponseEntity<?> verifyInteractionIntegrity(
            @PathVariable Long interactionId,
            @RequestBody Map<String, String> request) {
        try {
            String expectedHash = request.get("expectedHash");
            if (expectedHash == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Expected hash is required"));
            }
            
            boolean isValid = contractService.verifyInteractionIntegrity(interactionId, expectedHash);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (RuntimeException e) {
            logger.error("Error verifying interaction integrity for {}", interactionId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error verifying interaction integrity for {}", interactionId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    /**
     * Helper method to create Function objects based on function name and parameters
     * This is a simplified example - in practice, you'd parse ABI files
     */
    private Function createFunction(String functionName, Map<String, Object> request) {
        switch (functionName.toLowerCase()) {
            case "balanceof":
                String address = request.get("address").toString();
                return new Function(
                    "balanceOf",
                    Arrays.asList(new Address(address)),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
            case "transfer":
                String toAddress = request.get("toAddress").toString();
                BigInteger amount = new BigInteger(request.get("amount").toString());
                return new Function(
                    "transfer",
                    Arrays.asList(new Address(toAddress), new Uint256(amount)),
                    Arrays.asList(new TypeReference<Bool>() {})
                );
                
            case "approve":
                String spenderAddress = request.get("spenderAddress").toString();
                BigInteger approveAmount = new BigInteger(request.get("amount").toString());
                return new Function(
                    "approve",
                    Arrays.asList(new Address(spenderAddress), new Uint256(approveAmount)),
                    Arrays.asList(new TypeReference<Bool>() {})
                );
                
            case "allowance":
                String ownerAddress = request.get("ownerAddress").toString();
                String spender = request.get("spenderAddress").toString();
                return new Function(
                    "allowance",
                    Arrays.asList(new Address(ownerAddress), new Address(spender)),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
            case "totalsupply":
                return new Function(
                    "totalSupply",
                    Arrays.asList(),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
            case "name":
                return new Function(
                    "name",
                    Arrays.asList(),
                    Arrays.asList(new TypeReference<Utf8String>() {})
                );
                
            case "symbol":
                return new Function(
                    "symbol",
                    Arrays.asList(),
                    Arrays.asList(new TypeReference<Utf8String>() {})
                );
                
            case "decimals":
                return new Function(
                    "decimals",
                    Arrays.asList(),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
            default:
                throw new IllegalArgumentException("Unsupported function: " + functionName);
        }
    }
}
