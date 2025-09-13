package com.ethereum.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;
    
    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;
    
    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;
    
    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal amount;
    
    @Column(name = "gas_price", precision = 36, scale = 18)
    private BigDecimal gasPrice;
    
    @Column(name = "gas_limit")
    private Long gasLimit;
    
    @Column(name = "gas_used")
    private Long gasUsed;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "block_number")
    private Long blockNumber;
    
    @Column(name = "transaction_index")
    private Integer transactionIndex;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    public enum TransactionStatus {
        PENDING, CONFIRMED, FAILED, CANCELLED
    }
    
    public Transaction() {
    }
    
    public Transaction(Wallet wallet, String transactionHash, String fromAddress, 
                      String toAddress, BigDecimal amount) {
        this.wallet = wallet;
        this.transactionHash = transactionHash;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
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
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public String getToAddress() {
        return toAddress;
    }
    
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getGasPrice() {
        return gasPrice;
    }
    
    public void setGasPrice(BigDecimal gasPrice) {
        this.gasPrice = gasPrice;
    }
    
    public Long getGasLimit() {
        return gasLimit;
    }
    
    public void setGasLimit(Long gasLimit) {
        this.gasLimit = gasLimit;
    }
    
    public Long getGasUsed() {
        return gasUsed;
    }
    
    public void setGasUsed(Long gasUsed) {
        this.gasUsed = gasUsed;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
    
    public Long getBlockNumber() {
        return blockNumber;
    }
    
    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }
    
    public Integer getTransactionIndex() {
        return transactionIndex;
    }
    
    public void setTransactionIndex(Integer transactionIndex) {
        this.transactionIndex = transactionIndex;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionHash='" + transactionHash + '\'' +
                ", fromAddress='" + fromAddress + '\'' +
                ", toAddress='" + toAddress + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
