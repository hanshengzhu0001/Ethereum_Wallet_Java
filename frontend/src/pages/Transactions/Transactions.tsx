import React, { useState } from 'react';
import { useQuery } from 'react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  Launch,
  Refresh,
  Search,
  FilterList,
} from '@mui/icons-material';
import { format } from 'date-fns';

import { transactionApi, walletApi, formatEthAmount, formatTransactionHash, getEtherscanUrl } from '../../services/api';
import { Transaction, TransactionStatus, Wallet } from '../../types';

const Transactions: React.FC = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [selectedWallet, setSelectedWallet] = useState<number | ''>('');
  const [statusFilter, setStatusFilter] = useState<TransactionStatus | ''>('');
  const [searchTerm, setSearchTerm] = useState('');

  // Fetch wallets for filter
  const { data: wallets } = useQuery<Wallet[]>('wallets', async () => {
    const response = await walletApi.getWallets();
    return response.data;
  });

  // Fetch transactions
  const {
    data: transactionsData,
    isLoading,
    error,
    refetch,
  } = useQuery(
    ['transactions', selectedWallet, page, rowsPerPage],
    async () => {
      if (selectedWallet) {
        const response = await transactionApi.getWalletTransactions(
          selectedWallet as number,
          page,
          rowsPerPage
        );
        return response.data;
      } else {
        // Get all transactions from all wallets
        const response = await transactionApi.getPendingTransactions();
        return {
          content: response.data,
          totalElements: response.data.length,
          totalPages: 1,
          size: response.data.length,
          number: 0,
          first: true,
          last: true,
        };
      }
    },
    {
      keepPreviousData: true,
    }
  );

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
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

  const filteredTransactions = React.useMemo(() => {
    if (!transactionsData?.content) return [];
    
    return transactionsData.content.filter((transaction) => {
      const matchesStatus = !statusFilter || transaction.status === statusFilter;
      const matchesSearch = !searchTerm || 
        transaction.transactionHash.toLowerCase().includes(searchTerm.toLowerCase()) ||
        transaction.fromAddress.toLowerCase().includes(searchTerm.toLowerCase()) ||
        transaction.toAddress.toLowerCase().includes(searchTerm.toLowerCase());
      
      return matchesStatus && matchesSearch;
    });
  }, [transactionsData?.content, statusFilter, searchTerm]);

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        Failed to load transactions. Please try again.
      </Alert>
    );
  }

  return (
    <Box className="fade-in">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={600}>
          Transactions
        </Typography>
        <IconButton onClick={() => refetch()} color="primary">
          <Refresh />
        </IconButton>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Wallet</InputLabel>
              <Select
                value={selectedWallet}
                label="Wallet"
                onChange={(e) => setSelectedWallet(e.target.value as number | '')}
              >
                <MenuItem value="">All Wallets</MenuItem>
                {wallets?.map((wallet) => (
                  <MenuItem key={wallet.id} value={wallet.id}>
                    {wallet.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value as TransactionStatus | '')}
              >
                <MenuItem value="">All Status</MenuItem>
                <MenuItem value={TransactionStatus.PENDING}>Pending</MenuItem>
                <MenuItem value={TransactionStatus.CONFIRMED}>Confirmed</MenuItem>
                <MenuItem value={TransactionStatus.FAILED}>Failed</MenuItem>
                <MenuItem value={TransactionStatus.CANCELLED}>Cancelled</MenuItem>
              </Select>
            </FormControl>

            <TextField
              size="small"
              placeholder="Search by hash or address..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
              sx={{ minWidth: 250 }}
            />
          </Box>
        </CardContent>
      </Card>

      {/* Transactions Table */}
      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Transaction Hash</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredTransactions.length > 0 ? (
                filteredTransactions.map((transaction) => (
                  <TableRow key={transaction.id} hover>
                    <TableCell>
                      <Typography variant="body2" className="tx-hash">
                        {formatTransactionHash(transaction.transactionHash)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="eth-address">
                        {formatTransactionHash(transaction.fromAddress)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="eth-address">
                        {formatTransactionHash(transaction.toAddress)}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" fontWeight={500}>
                        {formatEthAmount(transaction.amount)} ETH
                      </Typography>
                      {transaction.gasUsed && transaction.gasPrice && (
                        <Typography variant="caption" color="textSecondary" display="block">
                          Gas: {formatEthAmount(
                            (parseFloat(transaction.gasPrice) * transaction.gasUsed).toString()
                          )} ETH
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={transaction.status}
                        color={getStatusColor(transaction.status)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        {format(new Date(transaction.createdAt), 'MMM dd, yyyy')}
                      </Typography>
                      <Typography variant="caption" color="textSecondary">
                        {format(new Date(transaction.createdAt), 'HH:mm:ss')}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="View on Etherscan">
                        <IconButton
                          size="small"
                          onClick={() => window.open(getEtherscanUrl(transaction.transactionHash), '_blank')}
                        >
                          <Launch fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                    <Typography color="textSecondary">
                      No transactions found
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        
        {transactionsData && (
          <TablePagination
            rowsPerPageOptions={[5, 10, 25, 50]}
            component="div"
            count={transactionsData.totalElements}
            rowsPerPage={rowsPerPage}
            page={page}
            onPageChange={handleChangePage}
            onRowsPerPageChange={handleChangeRowsPerPage}
          />
        )}
      </Card>
    </Box>
  );
};

export default Transactions;
