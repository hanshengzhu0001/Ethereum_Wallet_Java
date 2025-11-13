import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  TextField,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Alert,
  CircularProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Divider,
} from '@mui/material';
import {
  Add,
  ExpandMore,
  Code,
  PlayArrow,
  Refresh,
  Launch,
} from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';

import { contractApi, walletApi, getEtherscanUrl } from '../../services/api';
import { Wallet, ContractInteraction, ContractCallForm, ContractExecuteForm } from '../../types';

const Contracts: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedWallet, setSelectedWallet] = useState<number | ''>('');
  const [callDialogOpen, setCallDialogOpen] = useState(false);
  const [executeDialogOpen, setExecuteDialogOpen] = useState(false);
  const [callResult, setCallResult] = useState<string>('');

  // Fetch wallets
  const { data: wallets } = useQuery<Wallet[]>('wallets', async () => {
    const response = await walletApi.getWallets();
    return response.data;
  });

  // Fetch contract interactions
  const {
    data: interactions,
    isLoading,
    error,
    refetch,
  } = useQuery(
    ['contractInteractions', selectedWallet],
    async () => {
      if (selectedWallet) {
        const response = await contractApi.getWalletContractInteractions(selectedWallet as number);
        return response.data;
      }
      return [];
    },
    {
      enabled: !!selectedWallet,
    }
  );

  // Contract call mutation
  const callMutation = useMutation(contractApi.callFunction, {
    onSuccess: (response) => {
      setCallResult(response.data.result);
      toast.success('Contract function called successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to call contract function');
    },
  });

  // Contract execute mutation
  const executeMutation = useMutation(contractApi.executeFunction, {
    onSuccess: (response) => {
      queryClient.invalidateQueries('contractInteractions');
      setExecuteDialogOpen(false);
      toast.success(`Transaction sent! Hash: ${response.data.transactionHash}`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to execute contract function');
    },
  });

  // Form controls
  const callForm = useForm<ContractCallForm>();
  const executeForm = useForm<ContractExecuteForm>();

  const handleContractCall = (data: ContractCallForm) => {
    if (!selectedWallet) {
      toast.error('Please select a wallet first');
      return;
    }

    callMutation.mutate({
      walletId: selectedWallet as number,
      contractAddress: data.contractAddress,
      functionName: data.functionName,
      ...data.parameters,
    });
  };

  const handleContractExecute = (data: ContractExecuteForm) => {
    if (!selectedWallet) {
      toast.error('Please select a wallet first');
      return;
    }

    executeMutation.mutate({
      walletId: selectedWallet as number,
      contractAddress: data.contractAddress,
      functionName: data.functionName,
      password: data.password,
      gasPrice: data.useCustomGas ? data.gasPrice : undefined,
      gasLimit: data.useCustomGas ? parseInt(data.gasLimit || '100000') : undefined,
      value: data.value || '0',
      ...data.parameters,
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'FAILED':
      case 'REVERTED':
        return 'error';
      default:
        return 'default';
    }
  };

  // Common ERC-20 functions
  const erc20Functions = [
    { name: 'name', params: [] },
    { name: 'symbol', params: [] },
    { name: 'decimals', params: [] },
    { name: 'totalSupply', params: [] },
    { name: 'balanceOf', params: ['address'] },
    { name: 'allowance', params: ['ownerAddress', 'spenderAddress'] },
    { name: 'transfer', params: ['toAddress', 'amount'] },
    { name: 'approve', params: ['spenderAddress', 'amount'] },
  ];

  return (
    <Box className="fade-in">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={600}>
          Smart Contracts
        </Typography>
        <Box display="flex" gap={1}>
          <Button
            variant="outlined"
            startIcon={<Code />}
            onClick={() => setCallDialogOpen(true)}
            disabled={!selectedWallet}
          >
            Call Function
          </Button>
          <Button
            variant="contained"
            startIcon={<PlayArrow />}
            onClick={() => setExecuteDialogOpen(true)}
            disabled={!selectedWallet}
          >
            Execute Function
          </Button>
          <IconButton onClick={() => refetch()} color="primary">
            <Refresh />
          </IconButton>
        </Box>
      </Box>

      {/* Wallet Selection */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <FormControl fullWidth>
            <InputLabel>Select Wallet</InputLabel>
            <Select
              value={selectedWallet}
              label="Select Wallet"
              onChange={(e) => setSelectedWallet(e.target.value as number | '')}
            >
              <MenuItem value="">Select a wallet...</MenuItem>
              {wallets?.map((wallet) => (
                <MenuItem key={wallet.id} value={wallet.id}>
                  {wallet.name} ({wallet.address.slice(0, 6)}...{wallet.address.slice(-4)})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </CardContent>
      </Card>

      {/* ERC-20 Quick Actions */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            ERC-20 Token Quick Actions
          </Typography>
          <Typography variant="body2" color="textSecondary" gutterBottom>
            Common ERC-20 token functions for quick access
          </Typography>
          
          <Grid container spacing={2} sx={{ mt: 1 }}>
            {erc20Functions.slice(0, 4).map((func) => (
              <Grid item xs={6} sm={3} key={func.name}>
                <Button
                  variant="outlined"
                  fullWidth
                  size="small"
                  onClick={() => {
                    callForm.setValue('functionName', func.name);
                    setCallDialogOpen(true);
                  }}
                  disabled={!selectedWallet}
                >
                  {func.name}
                </Button>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      {/* Contract Interactions History */}
      {selectedWallet && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Contract Interactions History
            </Typography>
            
            {isLoading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : error ? (
              <Alert severity="error">
                Failed to load contract interactions.
              </Alert>
            ) : interactions && interactions.length > 0 ? (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Contract</TableCell>
                      <TableCell>Function</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {interactions.map((interaction) => (
                      <TableRow key={interaction.id} hover>
                        <TableCell>
                          <Typography variant="body2" className="eth-address">
                            {interaction.contractAddress.slice(0, 6)}...{interaction.contractAddress.slice(-4)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight={500}>
                            {interaction.functionName}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            {interaction.functionSignature}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={interaction.status}
                            color={getStatusColor(interaction.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {new Date(interaction.createdAt).toLocaleDateString()}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          {interaction.transactionHash && (
                            <IconButton
                              size="small"
                              onClick={() => window.open(getEtherscanUrl(interaction.transactionHash!), '_blank')}
                            >
                              <Launch fontSize="small" />
                            </IconButton>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Box textAlign="center" py={4}>
                <Typography color="textSecondary">
                  No contract interactions found for this wallet.
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {/* Call Function Dialog */}
      <Dialog open={callDialogOpen} onClose={() => setCallDialogOpen(false)} maxWidth="md" fullWidth>
        <form onSubmit={callForm.handleSubmit(handleContractCall)}>
          <DialogTitle>Call Contract Function (Read-Only)</DialogTitle>
          <DialogContent>
            <Controller
              name="contractAddress"
              control={callForm.control}
              rules={{ required: 'Contract address is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Contract Address"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            
            <Controller
              name="functionName"
              control={callForm.control}
              rules={{ required: 'Function name is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Function Name"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />

            {/* Dynamic parameter fields based on function */}
            <Accordion sx={{ mt: 2 }}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography>Function Parameters</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="address (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => callForm.setValue('parameters.address', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="ownerAddress (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => callForm.setValue('parameters.ownerAddress', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="spenderAddress (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => callForm.setValue('parameters.spenderAddress', e.target.value)}
                    />
                  </Grid>
                </Grid>
              </AccordionDetails>
            </Accordion>

            {callResult && (
              <Alert severity="success" sx={{ mt: 2 }}>
                <Typography variant="body2" fontWeight={500}>Result:</Typography>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', mt: 1 }}>
                  {callResult}
                </Typography>
              </Alert>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCallDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={callMutation.isLoading}
            >
              {callMutation.isLoading ? <CircularProgress size={20} /> : 'Call Function'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Execute Function Dialog */}
      <Dialog open={executeDialogOpen} onClose={() => setExecuteDialogOpen(false)} maxWidth="md" fullWidth>
        <form onSubmit={executeForm.handleSubmit(handleContractExecute)}>
          <DialogTitle>Execute Contract Function (State-Changing)</DialogTitle>
          <DialogContent>
            <Alert severity="warning" sx={{ mb: 2 }}>
              This will create a transaction and consume gas. Make sure you have enough ETH for gas fees.
            </Alert>
            
            <Controller
              name="contractAddress"
              control={executeForm.control}
              rules={{ required: 'Contract address is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Contract Address"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            
            <Controller
              name="functionName"
              control={executeForm.control}
              rules={{ required: 'Function name is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Function Name"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />

            <Accordion sx={{ mt: 2 }}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography>Function Parameters</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="toAddress (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => executeForm.setValue('parameters.toAddress', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="amount (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => executeForm.setValue('parameters.amount', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="spenderAddress (if needed)"
                      fullWidth
                      size="small"
                      onChange={(e) => executeForm.setValue('parameters.spenderAddress', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="value (ETH to send)"
                      fullWidth
                      size="small"
                      onChange={(e) => executeForm.setValue('value', e.target.value)}
                    />
                  </Grid>
                </Grid>
              </AccordionDetails>
            </Accordion>

            <Divider sx={{ my: 2 }} />

            <Controller
              name="password"
              control={executeForm.control}
              rules={{ required: 'Password is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Wallet Password"
                  type="password"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setExecuteDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={executeMutation.isLoading}
            >
              {executeMutation.isLoading ? <CircularProgress size={20} /> : 'Execute Function'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default Contracts;
