import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import {
  Box,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  CardActions,
  IconButton,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Chip,
  Alert,
  CircularProgress,
  Tooltip,
  Fab,
} from '@mui/material';
import {
  Add,
  MoreVert,
  AccountBalanceWallet,
  Send,
  GetApp,
  Edit,
  Delete,
  Refresh,
  ContentCopy,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';

import { walletApi, formatEthAmount, formatEthAddress } from '../../services/api';
import { Wallet, CreateWalletForm, ImportWalletForm, SendEthForm } from '../../types';

const Wallets: React.FC = () => {
  const queryClient = useQueryClient();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedWallet, setSelectedWallet] = useState<Wallet | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [sendDialogOpen, setSendDialogOpen] = useState(false);
  const [showPrivateKey, setShowPrivateKey] = useState(false);
  const [privateKey, setPrivateKey] = useState('');

  // Fetch wallets
  const {
    data: wallets,
    isLoading,
    error,
    refetch,
  } = useQuery<Wallet[]>('wallets', async () => {
    const response = await walletApi.getWallets();
    return response.data;
  });

  // Create wallet mutation
  const createWalletMutation = useMutation(walletApi.createWallet, {
    onSuccess: () => {
      queryClient.invalidateQueries('wallets');
      setCreateDialogOpen(false);
      toast.success('Wallet created successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create wallet');
    },
  });

  // Import wallet mutation
  const importWalletMutation = useMutation(walletApi.importWallet, {
    onSuccess: () => {
      queryClient.invalidateQueries('wallets');
      setImportDialogOpen(false);
      toast.success('Wallet imported successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to import wallet');
    },
  });

  // Transfer ETH mutation
  const transferMutation = useMutation(walletApi.transferEth, {
    onSuccess: (response) => {
      queryClient.invalidateQueries('wallets');
      setSendDialogOpen(false);
      toast.success(`Transaction sent! Hash: ${response.data.transactionHash}`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to send transaction');
    },
  });

  // Export private key mutation
  const exportKeyMutation = useMutation(
    ({ walletId, password }: { walletId: number; password: string }) =>
      walletApi.exportPrivateKey(walletId, password),
    {
      onSuccess: (response) => {
        setPrivateKey(response.data.privateKey);
        setShowPrivateKey(true);
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || 'Failed to export private key');
      },
    }
  );

  // Form controls
  const createForm = useForm<CreateWalletForm>();
  const importForm = useForm<ImportWalletForm>({
    defaultValues: { importType: 'privateKey' },
  });
  const sendForm = useForm<SendEthForm>();

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, wallet: Wallet) => {
    setAnchorEl(event.currentTarget);
    setSelectedWallet(wallet);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedWallet(null);
  };

  const handleCreateWallet = (data: CreateWalletForm) => {
    if (data.password !== data.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    createWalletMutation.mutate({
      name: data.name,
      password: data.password,
    });
  };

  const handleImportWallet = (data: ImportWalletForm) => {
    if (data.password !== data.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    importWalletMutation.mutate({
      name: data.name,
      password: data.password,
      privateKeyOrMnemonic: data.privateKeyOrMnemonic,
      mnemonicPassword: data.mnemonicPassword,
    });
  };

  const handleSendEth = (data: SendEthForm) => {
    if (!selectedWallet) return;
    
    transferMutation.mutate({
      walletId: selectedWallet.id,
      toAddress: data.toAddress,
      amount: data.amount,
      password: data.password,
      gasPrice: data.useCustomGas ? data.gasPrice : undefined,
      gasLimit: data.useCustomGas ? parseInt(data.gasLimit || '21000') : undefined,
    });
  };

  const handleExportPrivateKey = () => {
    if (!selectedWallet) return;
    
    const password = prompt('Enter wallet password:');
    if (password) {
      exportKeyMutation.mutate({ walletId: selectedWallet.id, password });
    }
    handleMenuClose();
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success('Copied to clipboard!');
  };

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
        Failed to load wallets. Please try again.
      </Alert>
    );
  }

  return (
    <Box className="fade-in">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={600}>
          Wallets
        </Typography>
        <Box display="flex" gap={1}>
          <Button
            variant="outlined"
            startIcon={<GetApp />}
            onClick={() => setImportDialogOpen(true)}
          >
            Import Wallet
          </Button>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => setCreateDialogOpen(true)}
          >
            Create Wallet
          </Button>
          <IconButton onClick={() => refetch()} color="primary">
            <Refresh />
          </IconButton>
        </Box>
      </Box>

      {wallets && wallets.length > 0 ? (
        <Grid container spacing={3}>
          {wallets.map((wallet) => (
            <Grid item xs={12} sm={6} md={4} key={wallet.id}>
              <Card className="hover-card">
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <AccountBalanceWallet color="primary" />
                      <Typography variant="h6" fontWeight={600}>
                        {wallet.name}
                      </Typography>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={(e) => handleMenuOpen(e, wallet)}
                    >
                      <MoreVert />
                    </IconButton>
                  </Box>
                  
                  <Box mb={2}>
                    <Typography variant="body2" color="textSecondary" gutterBottom>
                      Address
                    </Typography>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="body2" className="eth-address" sx={{ flexGrow: 1 }}>
                        {formatEthAddress(wallet.address)}
                      </Typography>
                      <IconButton
                        size="small"
                        onClick={() => copyToClipboard(wallet.address)}
                      >
                        <ContentCopy fontSize="small" />
                      </IconButton>
                    </Box>
                  </Box>

                  <Box mb={2}>
                    <Typography variant="body2" color="textSecondary" gutterBottom>
                      Balance
                    </Typography>
                    <Typography variant="h5" className="balance-display">
                      {formatEthAmount(wallet.balance)} ETH
                    </Typography>
                  </Box>

                  <Box display="flex" gap={1} mb={2}>
                    <Chip
                      label={wallet.isActive ? 'Active' : 'Inactive'}
                      color={wallet.isActive ? 'success' : 'default'}
                      size="small"
                    />
                    <Chip
                      label={`${wallet.transactionCount} txns`}
                      variant="outlined"
                      size="small"
                    />
                  </Box>
                </CardContent>
                
                <CardActions>
                  <Button
                    size="small"
                    startIcon={<Send />}
                    onClick={() => {
                      setSelectedWallet(wallet);
                      setSendDialogOpen(true);
                    }}
                    disabled={!wallet.isActive}
                  >
                    Send ETH
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <Box textAlign="center" py={8}>
          <AccountBalanceWallet sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="textSecondary" gutterBottom>
            No wallets found
          </Typography>
          <Typography variant="body2" color="textSecondary" mb={3}>
            Create your first wallet to get started with Ethereum transactions
          </Typography>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => setCreateDialogOpen(true)}
          >
            Create Wallet
          </Button>
        </Box>
      )}

      {/* Floating Action Button for mobile */}
      <Fab
        color="primary"
        aria-label="add wallet"
        sx={{
          position: 'fixed',
          bottom: 16,
          right: 16,
          display: { xs: 'flex', md: 'none' },
        }}
        onClick={() => setCreateDialogOpen(true)}
      >
        <Add />
      </Fab>

      {/* Wallet Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => {
          setSelectedWallet(selectedWallet);
          setSendDialogOpen(true);
          handleMenuClose();
        }}>
          <Send sx={{ mr: 1 }} /> Send ETH
        </MenuItem>
        <MenuItem onClick={handleExportPrivateKey}>
          <Visibility sx={{ mr: 1 }} /> Export Private Key
        </MenuItem>
        <MenuItem onClick={handleMenuClose}>
          <Edit sx={{ mr: 1 }} /> Edit Name
        </MenuItem>
        <MenuItem onClick={handleMenuClose} sx={{ color: 'error.main' }}>
          <Delete sx={{ mr: 1 }} /> Deactivate
        </MenuItem>
      </Menu>

      {/* Create Wallet Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={createForm.handleSubmit(handleCreateWallet)}>
          <DialogTitle>Create New Wallet</DialogTitle>
          <DialogContent>
            <Controller
              name="name"
              control={createForm.control}
              rules={{ required: 'Wallet name is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Wallet Name"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            <Controller
              name="password"
              control={createForm.control}
              rules={{ required: 'Password is required', minLength: { value: 8, message: 'Password must be at least 8 characters' } }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Password"
                  type="password"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            <Controller
              name="confirmPassword"
              control={createForm.control}
              rules={{ required: 'Please confirm password' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Confirm Password"
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
            <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createWalletMutation.isLoading}
            >
              {createWalletMutation.isLoading ? <CircularProgress size={20} /> : 'Create Wallet'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Import Wallet Dialog */}
      <Dialog open={importDialogOpen} onClose={() => setImportDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={importForm.handleSubmit(handleImportWallet)}>
          <DialogTitle>Import Wallet</DialogTitle>
          <DialogContent>
            <Controller
              name="name"
              control={importForm.control}
              rules={{ required: 'Wallet name is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Wallet Name"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            
            <FormControl component="fieldset" margin="normal">
              <FormLabel component="legend">Import Type</FormLabel>
              <Controller
                name="importType"
                control={importForm.control}
                render={({ field }) => (
                  <RadioGroup {...field} row>
                    <FormControlLabel value="privateKey" control={<Radio />} label="Private Key" />
                    <FormControlLabel value="mnemonic" control={<Radio />} label="Mnemonic Phrase" />
                  </RadioGroup>
                )}
              />
            </FormControl>

            <Controller
              name="privateKeyOrMnemonic"
              control={importForm.control}
              rules={{ required: 'Private key or mnemonic is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label={importForm.watch('importType') === 'mnemonic' ? 'Mnemonic Phrase' : 'Private Key'}
                  multiline
                  rows={3}
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />

            {importForm.watch('importType') === 'mnemonic' && (
              <Controller
                name="mnemonicPassword"
                control={importForm.control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Mnemonic Password (optional)"
                    type="password"
                    fullWidth
                    margin="normal"
                  />
                )}
              />
            )}

            <Controller
              name="password"
              control={importForm.control}
              rules={{ required: 'Password is required', minLength: { value: 8, message: 'Password must be at least 8 characters' } }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Password"
                  type="password"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            <Controller
              name="confirmPassword"
              control={importForm.control}
              rules={{ required: 'Please confirm password' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Confirm Password"
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
            <Button onClick={() => setImportDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={importWalletMutation.isLoading}
            >
              {importWalletMutation.isLoading ? <CircularProgress size={20} /> : 'Import Wallet'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Send ETH Dialog */}
      <Dialog open={sendDialogOpen} onClose={() => setSendDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={sendForm.handleSubmit(handleSendEth)}>
          <DialogTitle>Send ETH</DialogTitle>
          <DialogContent>
            {selectedWallet && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Sending from: {selectedWallet.name} ({formatEthAddress(selectedWallet.address)})
                <br />
                Balance: {formatEthAmount(selectedWallet.balance)} ETH
              </Alert>
            )}
            
            <Controller
              name="toAddress"
              control={sendForm.control}
              rules={{ required: 'Recipient address is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="To Address"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />
            
            <Controller
              name="amount"
              control={sendForm.control}
              rules={{ required: 'Amount is required' }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Amount (ETH)"
                  type="number"
                  fullWidth
                  margin="normal"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                />
              )}
            />

            <Controller
              name="password"
              control={sendForm.control}
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
            <Button onClick={() => setSendDialogOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={transferMutation.isLoading}
            >
              {transferMutation.isLoading ? <CircularProgress size={20} /> : 'Send ETH'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Private Key Dialog */}
      <Dialog open={showPrivateKey} onClose={() => setShowPrivateKey(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Private Key</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Keep your private key secure! Never share it with anyone.
          </Alert>
          <TextField
            value={privateKey}
            fullWidth
            multiline
            rows={3}
            InputProps={{
              readOnly: true,
              endAdornment: (
                <IconButton onClick={() => copyToClipboard(privateKey)}>
                  <ContentCopy />
                </IconButton>
              ),
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowPrivateKey(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Wallets;
