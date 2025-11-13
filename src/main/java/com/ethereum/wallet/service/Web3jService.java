package com.ethereum.wallet.service;

import com.ethereum.wallet.config.EthereumConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

@Service
public class Web3jService {
    
    private static final Logger logger = LoggerFactory.getLogger(Web3jService.class);
    
    private final Web3j web3j;
    private final EthereumConfig.EthereumProperties ethereumProperties;
    
    @Autowired
    public Web3jService(Web3j web3j, EthereumConfig.EthereumProperties ethereumProperties) {
        this.web3j = web3j;
        this.ethereumProperties = ethereumProperties;
    }
    
    /**
     * Get the current network version
     */
    public String getNetworkVersion() {
        try {
            NetVersion netVersion = web3j.netVersion().send();
            return netVersion.getNetVersion();
        } catch (IOException e) {
            logger.error("Failed to get network version", e);
            throw new RuntimeException("Failed to get network version", e);
        }
    }
    
    /**
     * Check if connected to the network
     */
    public boolean isConnected() {
        try {
            Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
            return web3ClientVersion.getWeb3ClientVersion() != null;
        } catch (Exception e) {
            logger.error("Failed to check network connection", e);
            return false;
        }
    }
    
    /**
     * Get balance of an address in Wei
     */
    public BigInteger getBalanceWei(String address) {
        try {
            EthGetBalance ethGetBalance = web3j
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send();
            
            if (ethGetBalance.hasError()) {
                throw new RuntimeException("Error getting balance: " + ethGetBalance.getError().getMessage());
            }
            
            return ethGetBalance.getBalance();
        } catch (IOException e) {
            logger.error("Failed to get balance for address: {}", address, e);
            throw new RuntimeException("Failed to get balance", e);
        }
    }
    
    /**
     * Get balance of an address in ETH
     */
    public BigDecimal getBalanceEth(String address) {
        BigInteger balanceWei = getBalanceWei(address);
        return Convert.fromWei(balanceWei.toString(), Convert.Unit.ETHER);
    }
    
    /**
     * Get current gas price
     */
    public BigInteger getGasPrice() {
        try {
            EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
            
            if (ethGasPrice.hasError()) {
                throw new RuntimeException("Error getting gas price: " + ethGasPrice.getError().getMessage());
            }
            
            return ethGasPrice.getGasPrice();
        } catch (IOException e) {
            logger.error("Failed to get gas price", e);
            return BigInteger.valueOf(ethereumProperties.getGasPrice());
        }
    }
    
    /**
     * Get transaction count (nonce) for an address
     */
    public BigInteger getTransactionCount(String address) {
        try {
            EthGetTransactionCount ethGetTransactionCount = web3j
                    .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                    .send();
            
            if (ethGetTransactionCount.hasError()) {
                throw new RuntimeException("Error getting transaction count: " + ethGetTransactionCount.getError().getMessage());
            }
            
            return ethGetTransactionCount.getTransactionCount();
        } catch (IOException e) {
            logger.error("Failed to get transaction count for address: {}", address, e);
            throw new RuntimeException("Failed to get transaction count", e);
        }
    }
    
    /**
     * Send raw transaction
     */
    public String sendRawTransaction(String signedTransactionData) {
        try {
            EthSendTransaction ethSendTransaction = web3j
                    .ethSendRawTransaction(signedTransactionData)
                    .send();
            
            if (ethSendTransaction.hasError()) {
                throw new RuntimeException("Error sending transaction: " + ethSendTransaction.getError().getMessage());
            }
            
            return ethSendTransaction.getTransactionHash();
        } catch (IOException e) {
            logger.error("Failed to send raw transaction", e);
            throw new RuntimeException("Failed to send transaction", e);
        }
    }
    
    /**
     * Get transaction receipt
     */
    public Optional<TransactionReceipt> getTransactionReceipt(String transactionHash) {
        try {
            EthGetTransactionReceipt ethGetTransactionReceipt = web3j
                    .ethGetTransactionReceipt(transactionHash)
                    .send();
            
            if (ethGetTransactionReceipt.hasError()) {
                logger.error("Error getting transaction receipt: {}", ethGetTransactionReceipt.getError().getMessage());
                return Optional.empty();
            }
            
            return ethGetTransactionReceipt.getTransactionReceipt();
        } catch (IOException e) {
            logger.error("Failed to get transaction receipt for hash: {}", transactionHash, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get transaction by hash
     */
    public Optional<org.web3j.protocol.core.methods.response.Transaction> getTransaction(String transactionHash) {
        try {
            EthTransaction ethTransaction = web3j
                    .ethGetTransactionByHash(transactionHash)
                    .send();
            
            if (ethTransaction.hasError()) {
                logger.error("Error getting transaction: {}", ethTransaction.getError().getMessage());
                return Optional.empty();
            }
            
            return ethTransaction.getTransaction();
        } catch (IOException e) {
            logger.error("Failed to get transaction for hash: {}", transactionHash, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get current block number
     */
    public BigInteger getBlockNumber() {
        try {
            EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
            
            if (ethBlockNumber.hasError()) {
                throw new RuntimeException("Error getting block number: " + ethBlockNumber.getError().getMessage());
            }
            
            return ethBlockNumber.getBlockNumber();
        } catch (IOException e) {
            logger.error("Failed to get block number", e);
            throw new RuntimeException("Failed to get block number", e);
        }
    }
    
    /**
     * Estimate gas for a transaction
     */
    public BigInteger estimateGas(String from, String to, BigInteger value, String data) {
        try {
            org.web3j.protocol.core.methods.request.Transaction transaction = 
                org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                    from, null, null, null, to, value, data
                );
            
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();
            
            if (ethEstimateGas.hasError()) {
                logger.warn("Error estimating gas: {}", ethEstimateGas.getError().getMessage());
                return BigInteger.valueOf(ethereumProperties.getGasLimit());
            }
            
            return ethEstimateGas.getAmountUsed();
        } catch (IOException e) {
            logger.error("Failed to estimate gas", e);
            return BigInteger.valueOf(ethereumProperties.getGasLimit());
        }
    }
    
    /**
     * Call a contract function (read-only)
     */
    public String call(String from, String to, String data) {
        try {
            org.web3j.protocol.core.methods.request.Transaction transaction = 
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(from, to, data);
            
            EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            
            if (ethCall.hasError()) {
                throw new RuntimeException("Error calling contract: " + ethCall.getError().getMessage());
            }
            
            return ethCall.getValue();
        } catch (IOException e) {
            logger.error("Failed to call contract", e);
            throw new RuntimeException("Failed to call contract", e);
        }
    }
    
    /**
     * Get chain ID
     */
    public long getChainId() {
        return ethereumProperties.getChainId();
    }
}

