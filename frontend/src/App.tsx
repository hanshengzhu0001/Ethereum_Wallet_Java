import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Container } from '@mui/material';

import Layout from './components/Layout/Layout';
import Dashboard from './pages/Dashboard/Dashboard';
import Wallets from './pages/Wallets/Wallets';
import Transactions from './pages/Transactions/Transactions';
import Contracts from './pages/Contracts/Contracts';
import Settings from './pages/Settings/Settings';

const App: React.FC = () => {
  return (
    <Layout>
      <Container maxWidth="xl" sx={{ py: 3 }}>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/wallets" element={<Wallets />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/contracts" element={<Contracts />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Container>
    </Layout>
  );
};

export default App;
