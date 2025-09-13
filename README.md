# Ethereum Wallet - Java Implementation

A comprehensive Ethereum wallet application built with **Java**, **Spring Boot**, **Web3j**, **PostgreSQL**, and **JavaFX**. This wallet supports wallet creation/import, ETH transfers, smart contract interactions, and provides both REST API and desktop GUI interfaces.

## 🚀 Features

### Core Wallet Functionality
- **Wallet Creation**: Generate new Ethereum wallets with secure key generation
- **Wallet Import**: Import wallets from private keys or mnemonic phrases
- **ETH Transfers**: Send ETH with custom gas settings and transaction monitoring
- **Balance Tracking**: Real-time balance updates from Sepolia testnet

### Security & Cryptography
- **ECDSA Signing**: Secure transaction signing using secp256k1 elliptic curve
- **Keccak256 Hashing**: Ethereum-compatible hashing for addresses and data integrity
- **AES-256 Encryption**: Private keys encrypted with password-based encryption
- **Integrity Checks**: Data integrity verification for contract interactions

### Smart Contract Integration
- **ABI Contract Calls**: Execute read and write operations on smart contracts
- **ERC-20 Support**: Built-in support for ERC-20 token interactions
- **Gas Estimation**: Automatic gas limit estimation for transactions
- **Transaction Monitoring**: Real-time transaction status tracking

### Data Persistence
- **PostgreSQL Database**: Secure storage of wallet data and transaction history
- **JPA Entities**: Clean object-relational mapping with Hibernate
- **Transaction History**: Complete audit trail of all wallet operations

### User Interfaces
- **JavaFX Desktop GUI**: Modern, responsive desktop application
- **REST API**: Complete REST API for programmatic access
- **Web3j Integration**: Direct interaction with Ethereum networks via Infura

## 🛠 Technology Stack

- **Java 17+**: Modern Java with latest features
- **Spring Boot 3.x**: Application framework and dependency injection
- **Web3j 4.10.3**: Ethereum blockchain interaction library
- **PostgreSQL**: Relational database for data persistence
- **JavaFX 19**: Modern desktop GUI framework
- **Maven**: Build automation and dependency management
- **JUnit 5**: Comprehensive unit testing framework
- **Bouncy Castle**: Cryptographic operations and security

## 📋 Prerequisites

1. **Java Development Kit (JDK) 17 or higher**
2. **PostgreSQL 12 or higher**
3. **Maven 3.8 or higher**
4. **Infura Account** (for Ethereum network access)

## 🔧 Setup Instructions

### 1. Database Setup

Create a PostgreSQL database and user:

```sql
CREATE DATABASE ethereum_wallet;
CREATE USER ethereum_user WITH ENCRYPTED PASSWORD 'ethereum_pass';
GRANT ALL PRIVILEGES ON DATABASE ethereum_wallet TO ethereum_user;
```

### 2. Environment Configuration

Create environment variables or update `application.yml`:

```bash
export DB_USERNAME=ethereum_user
export DB_PASSWORD=ethereum_pass
export INFURA_PROJECT_ID=your-infura-project-id
export INFURA_PROJECT_SECRET=your-infura-project-secret
export ADMIN_PASSWORD=your-admin-password
```

### 3. Build and Run

#### Option A: Desktop Application (JavaFX GUI)

```bash
# Clone and build
git clone <repository-url>
cd Ethereum_Wallet_Java
mvn clean install

# Run JavaFX application
mvn javafx:run
```

#### Option B: Server Mode (REST API only)

```bash
# Run in headless mode
java -Djava.awt.headless=true -Dethereum.wallet.mode=headless -jar target/ethereum-wallet-1.0.0.jar
```

#### Option C: Development Mode

```bash
# Run with Spring Boot Maven plugin
mvn spring-boot:run
```

## 🌐 API Documentation

### Wallet Management

#### Create Wallet
```http
POST /api/wallets
Content-Type: application/json

{
  "name": "My Wallet",
  "password": "securePassword123"
}
```

#### Import Wallet
```http
POST /api/wallets/import
Content-Type: application/json

{
  "name": "Imported Wallet",
  "password": "securePassword123",
  "privateKeyOrMnemonic": "0x1234567890abcdef...",
  "mnemonicPassword": "optional"
}
```

#### Get All Wallets
```http
GET /api/wallets
```

#### Get Wallet Balance
```http
GET /api/wallets/{id}/balance
```

#### Transfer ETH
```http
POST /api/wallets/transfer
Content-Type: application/json

{
  "walletId": 1,
  "toAddress": "0x742d35Cc6634C0532925a3b8D5C4C0F3f3B6b6D3",
  "amount": "0.1",
  "password": "securePassword123",
  "gasPrice": "20",
  "gasLimit": 21000
}
```

### Transaction Management

#### Get Wallet Transactions
```http
GET /api/transactions/wallet/{walletId}?page=0&size=20
```

#### Get Transaction by Hash
```http
GET /api/transactions/{transactionHash}
```

#### Get Transaction Statistics
```http
GET /api/transactions/wallet/{walletId}/stats
```

### Smart Contract Interactions

#### Call Contract Function (Read-only)
```http
POST /api/contracts/call
Content-Type: application/json

{
  "walletId": 1,
  "contractAddress": "0x...",
  "functionName": "balanceOf",
  "address": "0x..."
}
```

#### Execute Contract Function (State-changing)
```http
POST /api/contracts/execute
Content-Type: application/json

{
  "walletId": 1,
  "contractAddress": "0x...",
  "functionName": "transfer",
  "toAddress": "0x...",
  "amount": "1000000000000000000",
  "password": "securePassword123"
}
```

## 🖥 Desktop Application Usage

### Main Window Features
1. **Wallet List**: View all created/imported wallets
2. **Wallet Details**: Selected wallet information and balance
3. **Transaction History**: Complete transaction history with status
4. **Action Buttons**: Create, Import, Send ETH, and Refresh

### Creating a New Wallet
1. Click "Create Wallet"
2. Enter wallet name and secure password
3. Wallet address and keys are generated automatically
4. Private key is encrypted and stored securely

### Importing a Wallet
1. Click "Import Wallet"
2. Choose import type (Private Key or Mnemonic)
3. Enter wallet details and password
4. Wallet is imported and ready to use

### Sending ETH
1. Select a wallet from the list
2. Click "Send ETH"
3. Enter recipient address and amount
4. Optionally configure custom gas settings
5. Enter wallet password and confirm

## 🧪 Testing

Run the comprehensive test suite:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest

# Run tests with coverage
mvn test jacoco:report
```

### Test Coverage
- **Unit Tests**: Service layer, utilities, and controllers
- **Integration Tests**: Database operations and API endpoints
- **Security Tests**: Cryptographic operations and key management
- **Mock Tests**: External service interactions (Web3j, Infura)

## 🔒 Security Features

### Private Key Management
- Private keys are never stored in plain text
- AES-256-GCM encryption with password-based key derivation
- PBKDF2 with 100,000 iterations for key strengthening
- Secure random number generation for key creation

### Transaction Security
- All transactions signed locally with ECDSA
- Keccak256 hashing for data integrity
- Gas limit estimation to prevent failed transactions
- Transaction status monitoring and confirmation tracking

### Network Security
- HTTPS connections to Infura endpoints
- Input validation and sanitization
- SQL injection prevention with JPA
- CORS configuration for API security

## 📊 Database Schema

### Wallets Table
```sql
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(42) NOT NULL UNIQUE,
    encrypted_private_key TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
```

### Transactions Table
```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_hash VARCHAR(66) NOT NULL UNIQUE,
    from_address VARCHAR(42) NOT NULL,
    to_address VARCHAR(42) NOT NULL,
    amount DECIMAL(36, 18) NOT NULL,
    gas_price DECIMAL(36, 18),
    gas_limit BIGINT,
    gas_used BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    block_number BIGINT,
    transaction_index INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);
```

### Contract Interactions Table
```sql
CREATE TABLE contract_interactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    function_name VARCHAR(255) NOT NULL,
    function_signature VARCHAR(10) NOT NULL,
    input_data TEXT,
    output_data TEXT,
    transaction_hash VARCHAR(66),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);
```

## 🔧 Configuration

### Application Properties
Key configuration options in `application.yml`:

```yaml
ethereum:
  network:
    name: sepolia
    chain-id: 11155111
    infura:
      project-id: ${INFURA_PROJECT_ID}
      endpoint: https://sepolia.infura.io/v3/${INFURA_PROJECT_ID}
  gas:
    price: 20000000000  # 20 Gwei
    limit: 21000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ethereum_wallet
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## 🚀 Deployment

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim

COPY target/ethereum-wallet-1.0.0.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Dethereum.wallet.mode=headless", "-jar", "/app.jar"]
```

### Production Considerations
1. Use environment-specific configuration files
2. Enable SSL/TLS for API endpoints
3. Configure database connection pooling
4. Set up monitoring and logging
5. Use secrets management for sensitive data

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Troubleshooting

### Common Issues

1. **JavaFX Module Issues**
   ```bash
   # Add JavaFX modules to JVM
   --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
   ```

2. **Database Connection Issues**
   - Verify PostgreSQL is running
   - Check database credentials and connection string
   - Ensure database exists and user has permissions

3. **Infura Connection Issues**
   - Verify Infura project ID and secret
   - Check network connectivity
   - Ensure Sepolia testnet is accessible

4. **Memory Issues**
   ```bash
   # Increase JVM heap size
   java -Xmx2g -jar ethereum-wallet-1.0.0.jar
   ```

### Support

For support and questions:
- Create an issue in the repository
- Check the documentation and FAQ
- Review the test cases for usage examples

## 🎯 Roadmap

- [ ] Multi-network support (Mainnet, Polygon, BSC)
- [ ] Hardware wallet integration (Ledger, Trezor)
- [ ] DeFi protocol integrations
- [ ] NFT support and management
- [ ] Mobile application (React Native)
- [ ] Advanced analytics and reporting
