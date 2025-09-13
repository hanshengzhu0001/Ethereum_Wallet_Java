package com.ethereum.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WalletImportRequest {
    
    @NotBlank(message = "Wallet name is required")
    @Size(max = 255, message = "Wallet name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Private key or mnemonic is required")
    private String privateKeyOrMnemonic;
    
    private String mnemonicPassword; // Optional password for mnemonic
    
    public WalletImportRequest() {
    }
    
    public WalletImportRequest(String name, String password, String privateKeyOrMnemonic) {
        this.name = name;
        this.password = password;
        this.privateKeyOrMnemonic = privateKeyOrMnemonic;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPrivateKeyOrMnemonic() {
        return privateKeyOrMnemonic;
    }
    
    public void setPrivateKeyOrMnemonic(String privateKeyOrMnemonic) {
        this.privateKeyOrMnemonic = privateKeyOrMnemonic;
    }
    
    public String getMnemonicPassword() {
        return mnemonicPassword;
    }
    
    public void setMnemonicPassword(String mnemonicPassword) {
        this.mnemonicPassword = mnemonicPassword;
    }
}
