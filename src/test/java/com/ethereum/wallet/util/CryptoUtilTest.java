package com.ethereum.wallet.util;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {
    
    @Test
    void testGenerateKeyPair() {
        ECKeyPair keyPair = CryptoUtil.generateKeyPair();
        
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivateKey());
        assertNotNull(keyPair.getPublicKey());
        assertTrue(keyPair.getPrivateKey().compareTo(BigInteger.ZERO) > 0);
        assertTrue(keyPair.getPublicKey().compareTo(BigInteger.ZERO) > 0);
    }
    
    @Test
    void testGenerateAddress() {
        ECKeyPair keyPair = CryptoUtil.generateKeyPair();
        String address = CryptoUtil.generateAddress(keyPair);
        
        assertNotNull(address);
        assertTrue(address.startsWith("0x"));
        assertEquals(42, address.length());
        assertTrue(CryptoUtil.isValidAddress(address));
    }
    
    @Test
    void testKeccak256Hash() {
        String input = "Hello, Ethereum!";
        byte[] hash = CryptoUtil.keccak256(input);
        
        assertNotNull(hash);
        assertEquals(32, hash.length); // Keccak256 produces 32-byte hash
        
        String hexHash = CryptoUtil.keccak256Hex(input);
        assertNotNull(hexHash);
        assertTrue(hexHash.startsWith("0x"));
        assertEquals(66, hexHash.length()); // 0x + 64 hex characters
    }
    
    @Test
    void testSignAndVerifyMessage() {
        ECKeyPair keyPair = CryptoUtil.generateKeyPair();
        String address = CryptoUtil.generateAddress(keyPair);
        String message = "Test message for signing";
        byte[] messageHash = CryptoUtil.keccak256(message);
        
        // Sign the message
        Sign.SignatureData signature = CryptoUtil.signMessageHash(messageHash, keyPair);
        
        assertNotNull(signature);
        assertNotNull(signature.getR());
        assertNotNull(signature.getS());
        assertNotNull(signature.getV());
        
        // Verify the signature
        boolean isValid = CryptoUtil.verifySignature(messageHash, signature, address);
        assertTrue(isValid);
        
        // Verify with wrong address should fail
        ECKeyPair wrongKeyPair = CryptoUtil.generateKeyPair();
        String wrongAddress = CryptoUtil.generateAddress(wrongKeyPair);
        boolean isInvalid = CryptoUtil.verifySignature(messageHash, signature, wrongAddress);
        assertFalse(isInvalid);
    }
    
    @Test
    void testEncryptDecryptPrivateKey() {
        ECKeyPair keyPair = CryptoUtil.generateKeyPair();
        String privateKeyHex = keyPair.getPrivateKey().toString(16);
        String password = "testPassword123";
        
        // Encrypt private key
        String encrypted = CryptoUtil.encryptPrivateKey("0x" + privateKeyHex, password);
        assertNotNull(encrypted);
        assertNotEquals(privateKeyHex, encrypted);
        
        // Decrypt private key
        String decrypted = CryptoUtil.decryptPrivateKey(encrypted, password);
        assertNotNull(decrypted);
        assertTrue(decrypted.startsWith("0x"));
        assertEquals("0x" + privateKeyHex, decrypted);
        
        // Wrong password should fail
        assertThrows(RuntimeException.class, () -> {
            CryptoUtil.decryptPrivateKey(encrypted, "wrongPassword");
        });
    }
    
    @Test
    void testValidateAddress() {
        // Valid addresses
        assertTrue(CryptoUtil.isValidAddress("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3"));
        assertTrue(CryptoUtil.isValidAddress("0x0000000000000000000000000000000000000000"));
        
        // Invalid addresses
        assertFalse(CryptoUtil.isValidAddress("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D")); // Too short
        assertFalse(CryptoUtil.isValidAddress("742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3")); // Missing 0x
        assertFalse(CryptoUtil.isValidAddress("0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6DG")); // Invalid hex character
        assertFalse(CryptoUtil.isValidAddress(""));
        assertFalse(CryptoUtil.isValidAddress(null));
    }
    
    @Test
    void testValidatePrivateKey() {
        // Valid private keys
        assertTrue(CryptoUtil.isValidPrivateKey("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"));
        assertTrue(CryptoUtil.isValidPrivateKey("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"));
        
        // Invalid private keys
        assertFalse(CryptoUtil.isValidPrivateKey("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcd")); // Too short
        assertFalse(CryptoUtil.isValidPrivateKey("0x1234567890abcdefg234567890abcdef1234567890abcdef1234567890abcdef")); // Invalid hex
        assertFalse(CryptoUtil.isValidPrivateKey(""));
        assertFalse(CryptoUtil.isValidPrivateKey(null));
    }
    
    @Test
    void testMnemonicGeneration() {
        String mnemonic = CryptoUtil.generateMnemonic();
        assertNotNull(mnemonic);
        assertFalse(mnemonic.isEmpty());
        assertTrue(CryptoUtil.isValidMnemonic(mnemonic));
        
        // Should generate different mnemonics each time
        String mnemonic2 = CryptoUtil.generateMnemonic();
        assertNotEquals(mnemonic, mnemonic2);
    }
    
    @Test
    void testCreateKeyPairFromMnemonic() {
        String mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        String password = "";
        
        ECKeyPair keyPair1 = CryptoUtil.createKeyPairFromMnemonic(mnemonic, password);
        ECKeyPair keyPair2 = CryptoUtil.createKeyPairFromMnemonic(mnemonic, password);
        
        // Same mnemonic should generate same key pair
        assertEquals(keyPair1.getPrivateKey(), keyPair2.getPrivateKey());
        assertEquals(keyPair1.getPublicKey(), keyPair2.getPublicKey());
        
        // Different password should generate different key pair
        ECKeyPair keyPair3 = CryptoUtil.createKeyPairFromMnemonic(mnemonic, "different");
        assertNotEquals(keyPair1.getPrivateKey(), keyPair3.getPrivateKey());
    }
    
    @Test
    void testIntegrityHashAndVerification() {
        String data = "Important contract data";
        String hash = CryptoUtil.calculateIntegrityHash(data);
        
        assertNotNull(hash);
        assertTrue(hash.startsWith("0x"));
        
        // Verify correct data
        assertTrue(CryptoUtil.verifyIntegrity(data, hash));
        
        // Verify tampered data
        assertFalse(CryptoUtil.verifyIntegrity(data + "tampered", hash));
        
        // Verify with wrong hash
        assertFalse(CryptoUtil.verifyIntegrity(data, "0x1234567890abcdef"));
    }
}
