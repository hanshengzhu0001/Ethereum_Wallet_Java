import React from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Chip,
  Alert,
  Link,
} from '@mui/material';
import {
  Info,
  Security,
  Storage,
  Cloud,
  Code,
  GitHub,
  Language,
  Speed,
} from '@mui/icons-material';

const Settings: React.FC = () => {
  const systemInfo = [
    { label: 'Network', value: 'Sepolia Testnet', icon: <Language /> },
    { label: 'Chain ID', value: '11155111', icon: <Info /> },
    { label: 'Provider', value: 'Infura', icon: <Cloud /> },
    { label: 'Database', value: 'PostgreSQL', icon: <Storage /> },
    { label: 'Backend', value: 'Spring Boot + Web3j', icon: <Code /> },
    { label: 'Frontend', value: 'React.js + TypeScript', icon: <Code /> },
  ];

  const securityFeatures = [
    'AES-256-GCM encryption for private keys',
    'PBKDF2 key derivation with 100,000 iterations',
    'ECDSA transaction signing with secp256k1',
    'Keccak256 hashing for data integrity',
    'Secure random number generation',
    'Input validation and sanitization',
  ];

  const technologies = [
    { name: 'Java 17+', description: 'Modern Java with latest features' },
    { name: 'Spring Boot 3.x', description: 'Application framework and REST API' },
    { name: 'Web3j 4.10.3', description: 'Ethereum blockchain integration' },
    { name: 'PostgreSQL', description: 'Relational database for persistence' },
    { name: 'React.js 18', description: 'Modern frontend framework' },
    { name: 'Material-UI', description: 'React component library' },
    { name: 'TypeScript', description: 'Type-safe JavaScript' },
    { name: 'JavaFX', description: 'Desktop application UI' },
  ];

  return (
    <Box className="fade-in">
      <Typography variant="h4" fontWeight={600} gutterBottom>
        Settings & Information
      </Typography>

      <Grid container spacing={3}>
        {/* System Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                <Info sx={{ mr: 1, verticalAlign: 'middle' }} />
                System Information
              </Typography>
              <List>
                {systemInfo.map((item, index) => (
                  <ListItem key={index} divider={index < systemInfo.length - 1}>
                    <ListItemIcon>{item.icon}</ListItemIcon>
                    <ListItemText
                      primary={item.label}
                      secondary={item.value}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* Security Features */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                <Security sx={{ mr: 1, verticalAlign: 'middle' }} />
                Security Features
              </Typography>
              <List>
                {securityFeatures.map((feature, index) => (
                  <ListItem key={index} divider={index < securityFeatures.length - 1}>
                    <ListItemText primary={feature} />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* Network Status */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                <Speed sx={{ mr: 1, verticalAlign: 'middle' }} />
                Network Status
              </Typography>
              <Box display="flex" gap={2} flexWrap="wrap" mb={2}>
                <Chip label="Connected" color="success" />
                <Chip label="Sepolia Testnet" color="info" />
                <Chip label="Web3j Active" color="primary" />
                <Chip label="Database Online" color="success" />
              </Box>
              <Alert severity="info">
                You are connected to the Ethereum Sepolia testnet. This is a test network where ETH has no real value.
                Use this network for testing and development purposes only.
              </Alert>
            </CardContent>
          </Card>
        </Grid>

        {/* Technology Stack */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                <Code sx={{ mr: 1, verticalAlign: 'middle' }} />
                Technology Stack
              </Typography>
              <Grid container spacing={2}>
                {technologies.map((tech, index) => (
                  <Grid item xs={12} sm={6} md={3} key={index}>
                    <Box
                      sx={{
                        p: 2,
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                        height: '100%',
                      }}
                    >
                      <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                        {tech.name}
                      </Typography>
                      <Typography variant="body2" color="textSecondary">
                        {tech.description}
                      </Typography>
                    </Box>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* API Documentation */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                API Documentation
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                The wallet provides a comprehensive REST API for programmatic access.
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" gutterBottom>
                Available Endpoints:
              </Typography>
              <List dense>
                <ListItem>
                  <ListItemText
                    primary="Wallet Management"
                    secondary="/api/wallets - Create, import, and manage wallets"
                  />
                </ListItem>
                <ListItem>
                  <ListItemText
                    primary="Transactions"
                    secondary="/api/transactions - View and monitor transactions"
                  />
                </ListItem>
                <ListItem>
                  <ListItemText
                    primary="Smart Contracts"
                    secondary="/api/contracts - Interact with smart contracts"
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* About */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                <GitHub sx={{ mr: 1, verticalAlign: 'middle' }} />
                About This Project
              </Typography>
              <Typography variant="body2" color="textSecondary" paragraph>
                This Ethereum Wallet is a comprehensive implementation featuring both desktop (JavaFX) 
                and web (React.js) interfaces, backed by a robust Spring Boot API with Web3j integration.
              </Typography>
              <Typography variant="body2" color="textSecondary" paragraph>
                The project demonstrates modern full-stack development practices with secure 
                cryptographic operations, database persistence, and responsive user interfaces.
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" gutterBottom>
                Key Features:
              </Typography>
              <Typography variant="body2" color="textSecondary">
                • Wallet creation and import (private key/mnemonic)
                <br />
                • ETH transfers with custom gas settings
                <br />
                • Smart contract interactions (ERC-20 support)
                <br />
                • Transaction monitoring and history
                <br />
                • Secure private key encryption
                <br />
                • Multi-platform support (Desktop & Web)
              </Typography>
              <Box mt={2}>
                <Link
                  href="https://github.com/hanshengzhu0001/Ethereum_Wallet_Java"
                  target="_blank"
                  rel="noopener noreferrer"
                  color="primary"
                >
                  View on GitHub
                </Link>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Disclaimer */}
        <Grid item xs={12}>
          <Alert severity="warning">
            <Typography variant="subtitle2" gutterBottom>
              Important Disclaimer
            </Typography>
            <Typography variant="body2">
              This wallet is designed for educational and development purposes. While it implements 
              industry-standard security practices, always exercise caution when handling real cryptocurrency. 
              The Sepolia testnet ETH used in this application has no monetary value.
            </Typography>
          </Alert>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Settings;
