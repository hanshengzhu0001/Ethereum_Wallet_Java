package com.ethereum.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransferRequest {
    
    @NotNull(message = "Wallet ID is required")
    private Long walletId;
    
    @NotBlank(message = "Recipient address is required")
    private String toAddress;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private BigDecimal gasPrice; // Optional, will use network default if not provided
    private Long gasLimit; // Optional, will estimate if not provided
    
    public TransferRequest() {
    }
    
    public TransferRequest(Long walletId, String toAddress, BigDecimal amount, String password) {
        this.walletId = walletId;
        this.toAddress = toAddress;
        this.amount = amount;
        this.password = password;
    }
    
    public Long getWalletId() {
        return walletId;
    }
    
    public void setWalletId(Long walletId) {
        this.walletId = walletId;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
}
