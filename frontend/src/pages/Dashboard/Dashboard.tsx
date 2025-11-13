import React from 'react';
import { useQuery } from 'react-query';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  AccountBalanceWallet,
  SwapHoriz,
  TrendingUp,
  Schedule,
  Launch,
  Refresh,
} from '@mui/icons-material';
import { format } from 'date-fns';

import { walletApi, transactionApi, formatEthAmount, formatEthAddress, getEtherscanUrl } from '../../services/api';
import { Wallet, Transaction, TransactionStatus } from '../../types';

const Dashboard: React.FC = () => {
  // Fetch wallets
  const {
    data: wallets,
    isLoading: walletsLoading,
    error: walletsError,
    refetch: refetchWallets,
  } = useQuery<Wallet[]>('wallets', async () => {
    const response = await walletApi.getWallets();
    return response.data;
  });

  // Fetch pending transactions
  const {
    data: pendingTransactions,
    isLoading: pendingLoading,
    error: pendingError,
    refetch: refetchPending,
  } = useQuery<Transaction[]>('pendingTransactions', async () => {
    const response = await transactionApi.getPendingTransactions();
    return response.data;
  });

  // Calculate dashboard statistics
  const totalWallets = wallets?.length || 0;
  const totalBalance = wallets?.reduce((sum, wallet) => sum + parseFloat(wallet.balance), 0) || 0;
  const totalTransactions = wallets?.reduce((sum, wallet) => sum + wallet.transactionCount, 0) || 0;
  const pendingCount = pendingTransactions?.length || 0;

  // Get recent transactions from all wallets
  const recentTransactions = React.useMemo(() => {
    if (!wallets) return [];
    
    const allTransactions: Transaction[] = [];
    // This would need to be fetched separately for each wallet in a real implementation
    // For now, we'll use pending transactions as a placeholder
    return pendingTransactions?.slice(0, 5) || [];
  }, [wallets, pendingTransactions]);

  const handleRefresh = () => {
    refetchWallets();
    refetchPending();
  };

  const getStatusColor = (status: TransactionStatus) => {
    switch (status) {
      case TransactionStatus.CONFIRMED:
        return 'success';
      case TransactionStatus.PENDING:
        return 'warning';
      case TransactionStatus.FAILED:
        return 'error';
      case TransactionStatus.CANCELLED:
        return 'default';
      default:
        return 'default';
    }
  };

  const StatCard: React.FC<{
    title: string;
    value: string | number;
    icon: React.ReactElement;
    color: string;
    subtitle?: string;
  }> = ({ title, value, icon, color, subtitle }) => (
    <Card className="hover-card">
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4" component="div" fontWeight={600}>
              {value}
            </Typography>
            {subtitle && (
              <Typography variant="body2" color="textSecondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              backgroundColor: color + '20',
              borderRadius: 2,
              p: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {React.cloneElement(icon, { sx: { color, fontSize: 24 } })}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  if (walletsLoading || pendingLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (walletsError || pendingError) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        Failed to load dashboard data. Please try again.
      </Alert>
    );
  }

  return (
    <Box className="fade-in">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={600}>
          Dashboard
        </Typography>
        <Tooltip title="Refresh Data">
          <IconButton onClick={handleRefresh} color="primary">
            <Refresh />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Statistics Cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Wallets"
            value={totalWallets}
            icon={<AccountBalanceWallet />}
            color="#3498db"
            subtitle={`${wallets?.filter(w => w.isActive).length || 0} active`}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Balance"
            value={`${formatEthAmount(totalBalance)} ETH`}
            icon={<TrendingUp />}
            color="#2ecc71"
            subtitle="Across all wallets"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Transactions"
            value={totalTransactions}
            icon={<SwapHoriz />}
            color="#9b59b6"
            subtitle="All time"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Pending Transactions"
            value={pendingCount}
            icon={<Schedule />}
            color="#f39c12"
            subtitle="Awaiting confirmation"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* Wallets Overview */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                Wallets Overview
              </Typography>
              {wallets && wallets.length > 0 ? (
                <List>
                  {wallets.slice(0, 5).map((wallet) => (
                    <ListItem key={wallet.id} divider>
                      <ListItemIcon>
                        <AccountBalanceWallet color="primary" />
                      </ListItemIcon>
                      <ListItemText
                        primary={wallet.name}
                        secondary={
                          <Box>
                            <Typography variant="body2" className="eth-address">
                              {formatEthAddress(wallet.address)}
                            </Typography>
                            <Typography variant="body2" color="primary" fontWeight={500}>
                              {formatEthAmount(wallet.balance)} ETH
                            </Typography>
                          </Box>
                        }
                      />
                      <Chip
                        label={wallet.isActive ? 'Active' : 'Inactive'}
                        color={wallet.isActive ? 'success' : 'default'}
                        size="small"
                      />
                    </ListItem>
                  ))}
                </List>
              ) : (
                <Box textAlign="center" py={4}>
                  <Typography color="textSecondary">
                    No wallets found. Create your first wallet to get started.
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Transactions */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom fontWeight={600}>
                Recent Transactions
              </Typography>
              {recentTransactions.length > 0 ? (
                <List>
                  {recentTransactions.map((transaction) => (
                    <ListItem key={transaction.id} divider>
                      <ListItemIcon>
                        <SwapHoriz color="primary" />
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Box display="flex" alignItems="center" gap={1}>
                            <Typography variant="body2" className="tx-hash">
                              {formatEthAddress(transaction.transactionHash)}
                            </Typography>
                            <IconButton
                              size="small"
                              onClick={() => window.open(getEtherscanUrl(transaction.transactionHash), '_blank')}
                            >
                              <Launch fontSize="small" />
                            </IconButton>
                          </Box>
                        }
                        secondary={
                          <Box>
                            <Typography variant="body2">
                              {formatEthAmount(transaction.amount)} ETH
                            </Typography>
                            <Typography variant="caption" color="textSecondary">
                              {format(new Date(transaction.createdAt), 'MMM dd, yyyy HH:mm')}
                            </Typography>
                          </Box>
                        }
                      />
                      <Chip
                        label={transaction.status}
                        color={getStatusColor(transaction.status)}
                        size="small"
                      />
                    </ListItem>
                  ))}
                </List>
              ) : (
                <Box textAlign="center" py={4}>
                  <Typography color="textSecondary">
                    No recent transactions found.
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
