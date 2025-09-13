package com.ethereum.wallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_interactions")
public class ContractInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;
    
    @Column(name = "function_name", nullable = false)
    private String functionName;
    
    @Column(name = "function_signature", nullable = false, length = 10)
    private String functionSignature;
    
    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;
    
    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;
    
    @Column(name = "transaction_hash", length = 66)
    private String transactionHash;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InteractionStatus status = InteractionStatus.PENDING;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum InteractionStatus {
        PENDING, SUCCESS, FAILED, REVERTED
    }
    
    public ContractInteraction() {
    }
    
    public ContractInteraction(Wallet wallet, String contractAddress, String functionName, 
                              String functionSignature, String inputData) {
        this.wallet = wallet;
        this.contractAddress = contractAddress;
        this.functionName = functionName;
        this.functionSignature = functionSignature;
        this.inputData = inputData;
        this.createdAt = LocalDateTime.now();
    }
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Wallet getWallet() {
        return wallet;
    }
    
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }
    
    public String getContractAddress() {
        return contractAddress;
    }
    
    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
    
    public String getFunctionSignature() {
        return functionSignature;
    }
    
    public void setFunctionSignature(String functionSignature) {
        this.functionSignature = functionSignature;
    }
    
    public String getInputData() {
        return inputData;
    }
    
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }
    
    public String getOutputData() {
        return outputData;
    }
    
    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
    
    public InteractionStatus getStatus() {
        return status;
    }
    
    public void setStatus(InteractionStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "ContractInteraction{" +
                "id=" + id +
                ", contractAddress='" + contractAddress + '\'' +
                ", functionName='" + functionName + '\'' +
                ", functionSignature='" + functionSignature + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
