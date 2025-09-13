package com.ethereum.wallet.util;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

public class CryptoUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    static {
        // Add BouncyCastle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Generate a new ECDSA key pair for Ethereum
     */
    public static ECKeyPair generateKeyPair() {
        try {
            return Keys.createEcKeyPair();
        } catch (Exception e) {
            logger.error("Failed to generate key pair", e);
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }
    
    /**
     * Generate Ethereum address from public key
     */
    public static String generateAddress(BigInteger publicKey) {
        return Keys.getAddress(publicKey);
    }
    
    /**
     * Generate Ethereum address from ECKeyPair
     */
    public static String generateAddress(ECKeyPair keyPair) {
        return Keys.getAddress(keyPair);
    }
    
    /**
     * Compute Keccak256 hash
     */
    public static byte[] keccak256(byte[] input) {
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(input, 0, input.length);
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return output;
    }
    
    /**
     * Compute Keccak256 hash of string
     */
    public static byte[] keccak256(String input) {
        return keccak256(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Compute Keccak256 hash and return as hex string
     */
    public static String keccak256Hex(byte[] input) {
        return Numeric.toHexString(keccak256(input));
    }
    
    /**
     * Compute Keccak256 hash and return as hex string
     */
    public static String keccak256Hex(String input) {
        return keccak256Hex(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Sign message with ECDSA
     */
    public static Sign.SignatureData signMessage(byte[] message, ECKeyPair keyPair) {
        return Sign.signMessage(message, keyPair);
    }
    
    /**
     * Sign message hash with ECDSA (for Ethereum transactions)
     */
    public static Sign.SignatureData signMessageHash(byte[] messageHash, ECKeyPair keyPair) {
        return Sign.signMessage(messageHash, keyPair, false);
    }
    
    /**
     * Recover public key from signature
     */
    public static BigInteger recoverPublicKey(byte[] messageHash, Sign.SignatureData signature) {
        try {
            return Sign.recoverFromSignature(
                signature.getV()[0] - 27,
                signature,
                messageHash
            );
        } catch (Exception e) {
            logger.error("Failed to recover public key from signature", e);
            throw new RuntimeException("Failed to recover public key", e);
        }
    }
    
    /**
     * Verify ECDSA signature
     */
    public static boolean verifySignature(byte[] messageHash, Sign.SignatureData signature, String expectedAddress) {
        try {
            BigInteger publicKey = recoverPublicKey(messageHash, signature);
            String recoveredAddress = Keys.getAddress(publicKey);
            return expectedAddress.equalsIgnoreCase(recoveredAddress);
        } catch (Exception e) {
            logger.error("Failed to verify signature", e);
            return false;
        }
    }
    
    /**
     * Encrypt private key with password using AES-GCM
     */
    public static String encryptPrivateKey(String privateKeyHex, String password) {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Derive key from password
            SecretKey key = deriveKeyFromPassword(password, salt);
            
            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            
            // Encrypt
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] privateKeyBytes = Numeric.hexStringToByteArray(privateKeyHex);
            byte[] encryptedData = cipher.doFinal(privateKeyBytes);
            
            // Combine salt + iv + encrypted data
            byte[] result = new byte[salt.length + iv.length + encryptedData.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(iv, 0, result, salt.length, iv.length);
            System.arraycopy(encryptedData, 0, result, salt.length + iv.length, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            logger.error("Failed to encrypt private key", e);
            throw new RuntimeException("Failed to encrypt private key", e);
        }
    }
    
    /**
     * Decrypt private key with password
     */
    public static String decryptPrivateKey(String encryptedPrivateKey, String password) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedPrivateKey);
            
            // Extract components
            byte[] salt = Arrays.copyOfRange(data, 0, 16);
            byte[] iv = Arrays.copyOfRange(data, 16, 16 + GCM_IV_LENGTH);
            byte[] encryptedData = Arrays.copyOfRange(data, 16 + GCM_IV_LENGTH, data.length);
            
            // Derive key from password
            SecretKey key = deriveKeyFromPassword(password, salt);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return Numeric.toHexString(decryptedData);
        } catch (Exception e) {
            logger.error("Failed to decrypt private key", e);
            throw new RuntimeException("Failed to decrypt private key", e);
        }
    }
    
    /**
     * Derive AES key from password using PBKDF2
     */
    private static SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            password.toCharArray(), salt, 100000, 256);
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }
    
    /**
     * Validate Ethereum address format
     */
    public static boolean isValidAddress(String address) {
        try {
            return WalletUtils.isValidAddress(address);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate private key format
     */
    public static boolean isValidPrivateKey(String privateKeyHex) {
        try {
            if (!privateKeyHex.startsWith("0x")) {
                privateKeyHex = "0x" + privateKeyHex;
            }
            byte[] privateKeyBytes = Numeric.hexStringToByteArray(privateKeyHex);
            return privateKeyBytes.length == 32;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate secure random mnemonic phrase
     */
    public static String generateMnemonic() {
        try {
            return MnemonicUtils.generateMnemonic(128); // 12 words
        } catch (Exception e) {
            logger.error("Failed to generate mnemonic", e);
            throw new RuntimeException("Failed to generate mnemonic", e);
        }
    }
    
    /**
     * Create ECKeyPair from mnemonic
     */
    public static ECKeyPair createKeyPairFromMnemonic(String mnemonic, String password) {
        try {
            byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
            return ECKeyPair.create(Hash.sha256(seed));
        } catch (Exception e) {
            logger.error("Failed to create key pair from mnemonic", e);
            throw new RuntimeException("Failed to create key pair from mnemonic", e);
        }
    }
    
    /**
     * Validate mnemonic phrase
     */
    public static boolean isValidMnemonic(String mnemonic) {
        try {
            MnemonicUtils.validateMnemonic(mnemonic);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculate integrity hash for verification
     */
    public static String calculateIntegrityHash(String data) {
        return keccak256Hex(data);
    }
    
    /**
     * Verify data integrity
     */
    public static boolean verifyIntegrity(String data, String expectedHash) {
        String actualHash = calculateIntegrityHash(data);
        return actualHash.equals(expectedHash);
    }
}
