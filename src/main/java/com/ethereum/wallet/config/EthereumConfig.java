package com.ethereum.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class EthereumConfig {
    
    @Value("${ethereum.network.infura.endpoint}")
    private String infuraEndpoint;
    
    @Value("${ethereum.network.chain-id}")
    private Long chainId;
    
    @Value("${ethereum.gas.price}")
    private Long gasPrice;
    
    @Value("${ethereum.gas.limit}")
    private Long gasLimit;
    
    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(infuraEndpoint));
    }
    
    @Bean
    public EthereumProperties ethereumProperties() {
        EthereumProperties properties = new EthereumProperties();
        properties.setChainId(chainId);
        properties.setGasPrice(gasPrice);
        properties.setGasLimit(gasLimit);
        properties.setInfuraEndpoint(infuraEndpoint);
        return properties;
    }
    
    public static class EthereumProperties {
        private Long chainId;
        private Long gasPrice;
        private Long gasLimit;
        private String infuraEndpoint;
        
        // Getters and Setters
        public Long getChainId() {
            return chainId;
        }
        
        public void setChainId(Long chainId) {
            this.chainId = chainId;
        }
        
        public Long getGasPrice() {
            return gasPrice;
        }
        
        public void setGasPrice(Long gasPrice) {
            this.gasPrice = gasPrice;
        }
        
        public Long getGasLimit() {
            return gasLimit;
        }
        
        public void setGasLimit(Long gasLimit) {
            this.gasLimit = gasLimit;
        }
        
        public String getInfuraEndpoint() {
            return infuraEndpoint;
        }
        
        public void setInfuraEndpoint(String infuraEndpoint) {
            this.infuraEndpoint = infuraEndpoint;
        }
    }
}
