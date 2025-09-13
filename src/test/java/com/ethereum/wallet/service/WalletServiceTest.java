package com.ethereum.wallet.service;

import com.ethereum.wallet.dto.WalletCreateRequest;
import com.ethereum.wallet.dto.WalletImportRequest;
import com.ethereum.wallet.dto.WalletResponse;
import com.ethereum.wallet.entity.Wallet;
import com.ethereum.wallet.repository.TransactionRepository;
import com.ethereum.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private Web3jService web3jService;
    
    @Mock
    private TransactionService transactionService;
    
    @InjectMocks
    private WalletService walletService;
    
    private Wallet testWallet;
    
    @BeforeEach
    void setUp() {
        testWallet = new Wallet("Test Wallet", "0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3", "encrypted_private_key");
        testWallet.setId(1L);
    }
    
    @Test
    void testCreateWallet() {
        // Arrange
        WalletCreateRequest request = new WalletCreateRequest("New Wallet", "password123");
        when(walletRepository.existsByAddress(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(web3jService.getBalanceEth(anyString())).thenReturn(BigDecimal.ZERO);
        
        // Act
        WalletResponse response = walletService.createWallet(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("Test Wallet", response.getName());
        assertEquals("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3", response.getAddress());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        
        verify(walletRepository).existsByAddress(anyString());
        verify(walletRepository).save(any(Wallet.class));
        verify(web3jService).getBalanceEth(anyString());
    }
    
    @Test
    void testCreateWalletAddressCollision() {
        // Arrange
        WalletCreateRequest request = new WalletCreateRequest("New Wallet", "password123");
        when(walletRepository.existsByAddress(anyString())).thenReturn(true);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.createWallet(request);
        });
        
        assertTrue(exception.getMessage().contains("Address collision detected"));
        verify(walletRepository).existsByAddress(anyString());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void testImportWalletWithPrivateKey() {
        // Arrange
        String validPrivateKey = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        WalletImportRequest request = new WalletImportRequest("Imported Wallet", "password123", validPrivateKey);
        
        when(walletRepository.existsByAddress(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(web3jService.getBalanceEth(anyString())).thenReturn(new BigDecimal("1.5"));
        when(transactionRepository.countConfirmedTransactionsByWallet(any())).thenReturn(5L);
        
        // Act
        WalletResponse response = walletService.importWallet(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("Test Wallet", response.getName());
        assertEquals(new BigDecimal("1.5"), response.getBalance());
        assertEquals(5L, response.getTransactionCount());
        
        verify(walletRepository).existsByAddress(anyString());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void testImportWalletAlreadyExists() {
        // Arrange
        String validPrivateKey = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        WalletImportRequest request = new WalletImportRequest("Imported Wallet", "password123", validPrivateKey);
        
        when(walletRepository.existsByAddress(anyString())).thenReturn(true);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.importWallet(request);
        });
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(walletRepository).existsByAddress(anyString());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void testGetWallet() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(web3jService.getBalanceEth(anyString())).thenReturn(new BigDecimal("2.5"));
        when(transactionRepository.countConfirmedTransactionsByWallet(any())).thenReturn(10L);
        
        // Act
        WalletResponse response = walletService.getWallet(1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Wallet", response.getName());
        assertEquals(new BigDecimal("2.5"), response.getBalance());
        assertEquals(10L, response.getTransactionCount());
    }
    
    @Test
    void testGetWalletNotFound() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.getWallet(1L);
        });
        
        assertEquals("Wallet not found", exception.getMessage());
    }
    
    @Test
    void testGetAllWallets() {
        // Arrange
        Wallet wallet2 = new Wallet("Second Wallet", "0x123456789abcdef123456789abcdef123456789a", "encrypted_key_2");
        wallet2.setId(2L);
        
        List<Wallet> wallets = Arrays.asList(testWallet, wallet2);
        when(walletRepository.findByIsActiveTrue()).thenReturn(wallets);
        when(web3jService.getBalanceEth(testWallet.getAddress())).thenReturn(new BigDecimal("1.0"));
        when(web3jService.getBalanceEth(wallet2.getAddress())).thenReturn(new BigDecimal("0.5"));
        when(transactionRepository.countConfirmedTransactionsByWallet(any())).thenReturn(0L);
        
        // Act
        List<WalletResponse> responses = walletService.getAllWallets();
        
        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Test Wallet", responses.get(0).getName());
        assertEquals("Second Wallet", responses.get(1).getName());
    }
    
    @Test
    void testGetWalletBalance() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(web3jService.getBalanceEth(testWallet.getAddress())).thenReturn(new BigDecimal("3.14159"));
        
        // Act
        BigDecimal balance = walletService.getWalletBalance(1L);
        
        // Assert
        assertEquals(new BigDecimal("3.14159"), balance);
        verify(web3jService).getBalanceEth(testWallet.getAddress());
    }
    
    @Test
    void testGetWalletByAddress() {
        // Arrange
        when(walletRepository.findByAddress(testWallet.getAddress())).thenReturn(Optional.of(testWallet));
        when(web3jService.getBalanceEth(testWallet.getAddress())).thenReturn(new BigDecimal("1.23"));
        when(transactionRepository.countConfirmedTransactionsByWallet(testWallet)).thenReturn(7L);
        
        // Act
        Optional<WalletResponse> response = walletService.getWalletByAddress(testWallet.getAddress());
        
        // Assert
        assertTrue(response.isPresent());
        assertEquals(testWallet.getName(), response.get().getName());
        assertEquals(testWallet.getAddress(), response.get().getAddress());
        assertEquals(new BigDecimal("1.23"), response.get().getBalance());
        assertEquals(7L, response.get().getTransactionCount());
    }
    
    @Test
    void testGetWalletByAddressNotFound() {
        // Arrange
        when(walletRepository.findByAddress(anyString())).thenReturn(Optional.empty());
        
        // Act
        Optional<WalletResponse> response = walletService.getWalletByAddress("0x1234567890123456789012345678901234567890");
        
        // Assert
        assertFalse(response.isPresent());
    }
    
    @Test
    void testDeactivateWallet() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        
        // Act
        walletService.deactivateWallet(1L);
        
        // Assert
        verify(walletRepository).findById(1L);
        verify(walletRepository).save(argThat(wallet -> !wallet.getIsActive()));
    }
    
    @Test
    void testUpdateWalletName() {
        // Arrange
        String newName = "Updated Wallet Name";
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(web3jService.getBalanceEth(anyString())).thenReturn(BigDecimal.ONE);
        when(transactionRepository.countConfirmedTransactionsByWallet(any())).thenReturn(3L);
        
        // Act
        WalletResponse response = walletService.updateWalletName(1L, newName);
        
        // Assert
        assertNotNull(response);
        verify(walletRepository).save(argThat(wallet -> newName.equals(wallet.getName())));
    }
    
    @Test
    void testValidatePassword() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        
        // Act
        boolean isValid = walletService.validatePassword(1L, "wrongPassword");
        
        // Assert
        assertFalse(isValid); // Should be false because we can't decrypt with wrong password
    }
}
