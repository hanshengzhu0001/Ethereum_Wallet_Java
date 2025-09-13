package com.ethereum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "java.awt.headless=true",
    "ethereum.wallet.mode=headless"
})
@ActiveProfiles("test")
class EthereumWalletApplicationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
        // with all beans and configurations properly initialized
    }
}
