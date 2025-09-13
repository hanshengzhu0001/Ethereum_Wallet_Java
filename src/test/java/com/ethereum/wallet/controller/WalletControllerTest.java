package com.ethereum.wallet.controller;

import com.ethereum.wallet.dto.WalletCreateRequest;
import com.ethereum.wallet.dto.WalletResponse;
import com.ethereum.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@ActiveProfiles("test")
class WalletControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private WalletService walletService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private WalletResponse testWalletResponse;
    
    @BeforeEach
    void setUp() {
        testWalletResponse = new WalletResponse();
        testWalletResponse.setId(1L);
        testWalletResponse.setName("Test Wallet");
        testWalletResponse.setAddress("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3");
        testWalletResponse.setBalance(new BigDecimal("1.5"));
        testWalletResponse.setCreatedAt(LocalDateTime.now());
        testWalletResponse.setUpdatedAt(LocalDateTime.now());
        testWalletResponse.setIsActive(true);
        testWalletResponse.setTransactionCount(5L);
    }
    
    @Test
    void testCreateWallet() throws Exception {
        // Arrange
        WalletCreateRequest request = new WalletCreateRequest("New Wallet", "password123");
        when(walletService.createWallet(any(WalletCreateRequest.class))).thenReturn(testWalletResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Wallet"))
                .andExpect(jsonPath("$.address").value("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3"))
                .andExpect(jsonPath("$.balance").value(1.5))
                .andExpect(jsonPath("$.transactionCount").value(5));
    }
    
    @Test
    void testCreateWalletValidationError() throws Exception {
        // Arrange - Invalid request (missing name)
        WalletCreateRequest request = new WalletCreateRequest("", "password123");
        
        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCreateWalletServiceError() throws Exception {
        // Arrange
        WalletCreateRequest request = new WalletCreateRequest("New Wallet", "password123");
        when(walletService.createWallet(any(WalletCreateRequest.class)))
                .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to create wallet"))
                .andExpect(jsonPath("$.message").value("Service error"));
    }
    
    @Test
    void testGetAllWallets() throws Exception {
        // Arrange
        WalletResponse wallet2 = new WalletResponse();
        wallet2.setId(2L);
        wallet2.setName("Second Wallet");
        wallet2.setAddress("0x123456789abcdef123456789abcdef123456789a");
        wallet2.setBalance(new BigDecimal("0.5"));
        
        List<WalletResponse> wallets = Arrays.asList(testWalletResponse, wallet2);
        when(walletService.getAllWallets()).thenReturn(wallets);
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Wallet"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Second Wallet"));
    }
    
    @Test
    void testGetWallet() throws Exception {
        // Arrange
        when(walletService.getWallet(1L)).thenReturn(testWalletResponse);
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Wallet"))
                .andExpect(jsonPath("$.address").value("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3"))
                .andExpect(jsonPath("$.balance").value(1.5));
    }
    
    @Test
    void testGetWalletNotFound() throws Exception {
        // Arrange
        when(walletService.getWallet(999L)).thenThrow(new RuntimeException("Wallet not found"));
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets/999"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testGetWalletByAddress() throws Exception {
        // Arrange
        String address = "0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3";
        when(walletService.getWalletByAddress(address)).thenReturn(Optional.of(testWalletResponse));
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets/address/" + address))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value(address));
    }
    
    @Test
    void testGetWalletByAddressNotFound() throws Exception {
        // Arrange
        String address = "0x1234567890123456789012345678901234567890";
        when(walletService.getWalletByAddress(address)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets/address/" + address))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testGetWalletBalance() throws Exception {
        // Arrange
        when(walletService.getWalletBalance(1L)).thenReturn(new BigDecimal("2.75"));
        
        // Act & Assert
        mockMvc.perform(get("/api/wallets/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2.75));
    }
    
    @Test
    void testUpdateWalletName() throws Exception {
        // Arrange
        String newName = "Updated Wallet Name";
        testWalletResponse.setName(newName);
        when(walletService.updateWalletName(1L, newName)).thenReturn(testWalletResponse);
        
        // Act & Assert
        mockMvc.perform(put("/api/wallets/1/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + newName + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
    }
    
    @Test
    void testUpdateWalletNameEmptyName() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/wallets/1/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required"));
    }
    
    @Test
    void testDeactivateWallet() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wallet deactivated successfully"));
    }
    
    @Test
    void testValidatePassword() throws Exception {
        // Arrange
        when(walletService.validatePassword(1L, "correctPassword")).thenReturn(true);
        when(walletService.validatePassword(1L, "wrongPassword")).thenReturn(false);
        
        // Act & Assert - Correct password
        mockMvc.perform(post("/api/wallets/1/validate-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"correctPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
        
        // Act & Assert - Wrong password
        mockMvc.perform(post("/api/wallets/1/validate-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"wrongPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void testValidatePasswordMissingPassword() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/wallets/1/validate-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password is required"));
    }
    
    @Test
    void testExportPrivateKey() throws Exception {
        // Arrange
        String privateKey = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        when(walletService.exportPrivateKey(1L, "correctPassword")).thenReturn(privateKey);
        
        // Act & Assert
        mockMvc.perform(post("/api/wallets/1/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"correctPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateKey").value(privateKey));
    }
    
    @Test
    void testExportPrivateKeyWrongPassword() throws Exception {
        // Arrange
        when(walletService.exportPrivateKey(1L, "wrongPassword"))
                .thenThrow(new RuntimeException("Invalid password"));
        
        // Act & Assert
        mockMvc.perform(post("/api/wallets/1/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"wrongPassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid password"));
    }
}
