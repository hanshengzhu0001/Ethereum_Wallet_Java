package com.ethereum.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class EthereumWalletApplication {

    public static void main(String[] args) {
        // Set system properties for JavaFX
        System.setProperty("javafx.preloader", "com.ethereum.wallet.ui.EthereumWalletApp");
        
        // Check if running in headless mode (for tests or server-only mode)
        boolean headless = Boolean.getBoolean("java.awt.headless") || 
                          System.getProperty("ethereum.wallet.mode", "").equals("headless");
        
        if (headless) {
            // Run Spring Boot application only
            SpringApplication.run(EthereumWalletApplication.class, args);
        } else {
            // Launch JavaFX application which will start Spring Boot internally
            javafx.application.Application.launch(com.ethereum.wallet.ui.EthereumWalletApp.class, args);
        }
    }
}
