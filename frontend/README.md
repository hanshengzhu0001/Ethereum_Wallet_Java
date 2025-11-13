# Ethereum Wallet - React Frontend

A modern React.js frontend for the Ethereum Wallet application, providing a responsive web interface for wallet management, ETH transfers, and smart contract interactions.

## 🚀 Features

### 🔐 Wallet Management
- **Create Wallets**: Generate new Ethereum wallets with secure key generation
- **Import Wallets**: Import from private keys or mnemonic phrases
- **Wallet Overview**: View balance, address, and transaction count
- **Export Private Keys**: Secure private key export with password verification

### 💸 Transaction Management
- **Send ETH**: Transfer ETH with custom gas settings
- **Transaction History**: Complete transaction history with filtering
- **Real-time Status**: Live transaction status updates
- **Etherscan Integration**: Direct links to view transactions on Etherscan

### 🔗 Smart Contract Integration
- **Contract Calls**: Execute read-only contract functions
- **Contract Execution**: State-changing contract interactions
- **ERC-20 Support**: Built-in ERC-20 token function templates
- **Interaction History**: Track all contract interactions

### 📊 Dashboard & Analytics
- **Portfolio Overview**: Total balance across all wallets
- **Transaction Statistics**: Comprehensive transaction analytics
- **Recent Activity**: Latest transactions and contract interactions
- **Network Status**: Real-time network connection status

## 🛠 Technology Stack

- **React 18** - Modern React with hooks and concurrent features
- **TypeScript** - Type-safe JavaScript for better development experience
- **Material-UI (MUI)** - Comprehensive React component library
- **React Query** - Powerful data fetching and caching
- **React Hook Form** - Performant forms with easy validation
- **React Router** - Declarative routing for React applications
- **Axios** - Promise-based HTTP client for API calls
- **React Toastify** - Beautiful toast notifications

## 📋 Prerequisites

- **Node.js 16+** - JavaScript runtime
- **npm or yarn** - Package manager
- **Ethereum Wallet Backend** - Spring Boot API server running

## 🔧 Installation & Setup

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Environment Configuration

Create a `.env` file in the frontend directory:

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_ETHERSCAN_BASE_URL=https://sepolia.etherscan.io
```

### 3. Development Server

```bash
npm start
```

The application will open at `http://localhost:3000`

### 4. Build for Production

```bash
npm run build
```

This creates an optimized production build in the `build` folder.

## 🎨 UI Components & Design

### Design System
- **Color Palette**: Material Design inspired with Ethereum branding
- **Typography**: Roboto font family for consistency
- **Spacing**: 8px grid system for consistent layouts
- **Elevation**: Subtle shadows and depth for card components

### Key Components
- **Layout**: Responsive sidebar navigation with mobile support
- **Dashboard**: Statistical cards with real-time data
- **Wallet Cards**: Interactive wallet overview with actions
- **Transaction Table**: Sortable and filterable transaction history
- **Contract Interface**: Dynamic form generation for contract interactions

### Responsive Design
- **Mobile First**: Optimized for mobile devices
- **Tablet Support**: Adaptive layouts for tablet screens
- **Desktop**: Full-featured desktop experience
- **Touch Friendly**: Large touch targets and gestures

## 🔌 API Integration

### HTTP Client Configuration
```typescript
// Axios instance with interceptors
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '/api',
  timeout: 30000,
});

// Request interceptor for auth tokens
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### API Services
- **walletApi**: Wallet CRUD operations and ETH transfers
- **transactionApi**: Transaction history and monitoring
- **contractApi**: Smart contract interactions
- **dashboardApi**: Dashboard statistics and network info

### Error Handling
- **Global Error Interceptor**: Automatic error toast notifications
- **Retry Logic**: Automatic retry for failed requests
- **Loading States**: UI feedback during API calls
- **Offline Support**: Graceful handling of network issues

## 📱 Pages & Features

### Dashboard (`/dashboard`)
- Portfolio overview with total balance
- Recent transactions and activity
- Wallet statistics and quick actions
- Network status and connection info

### Wallets (`/wallets`)
- Grid view of all wallets with balances
- Create new wallet with secure password
- Import wallet from private key or mnemonic
- Send ETH with custom gas settings
- Export private keys with password verification

### Transactions (`/transactions`)
- Paginated transaction history
- Filter by wallet, status, and date range
- Search by transaction hash or address
- Direct links to Etherscan for verification
- Real-time status updates

### Contracts (`/contracts`)
- Smart contract interaction interface
- ERC-20 token function templates
- Dynamic parameter input forms
- Contract interaction history
- Gas estimation and custom gas settings

### Settings (`/settings`)
- System information and network status
- Security features overview
- Technology stack details
- API documentation links

## 🔒 Security Features

### Frontend Security
- **Input Validation**: Client-side form validation with react-hook-form
- **XSS Prevention**: Proper data sanitization and encoding
- **CSRF Protection**: CSRF tokens for state-changing operations
- **Secure Storage**: Sensitive data handling best practices

### Password Handling
- **No Storage**: Passwords never stored in browser storage
- **Secure Transmission**: HTTPS-only password transmission
- **Validation**: Strong password requirements
- **Auto-clear**: Password fields cleared after use

### Private Key Security
- **Display Warning**: Clear warnings about private key security
- **Copy Protection**: Secure clipboard handling
- **Auto-hide**: Private keys hidden by default
- **Session Timeout**: Automatic logout for security

## 🧪 Testing

### Unit Tests
```bash
npm test
```

### Test Coverage
```bash
npm test -- --coverage
```

### E2E Tests (Future)
- Cypress integration for end-to-end testing
- User journey testing
- Cross-browser compatibility

## 🚀 Deployment

### Development Deployment
```bash
npm start
```

### Production Build
```bash
npm run build
npm install -g serve
serve -s build -l 3000
```

### Docker Deployment
```dockerfile
FROM node:16-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Environment Variables
- `REACT_APP_API_BASE_URL`: Backend API URL
- `REACT_APP_ETHERSCAN_BASE_URL`: Etherscan base URL
- `REACT_APP_NETWORK_NAME`: Network name (Sepolia)

## 📊 Performance Optimization

### Code Splitting
- Route-based code splitting with React.lazy
- Component-level splitting for large components
- Dynamic imports for heavy libraries

### Caching Strategy
- React Query for API response caching
- Browser caching for static assets
- Service worker for offline support (future)

### Bundle Optimization
- Tree shaking for unused code elimination
- Minification and compression
- Asset optimization and lazy loading

## 🔧 Development Tools

### Code Quality
```bash
npm run lint          # ESLint for code linting
npm run lint:fix      # Auto-fix linting issues
npm run format        # Prettier for code formatting
```

### Development Scripts
```bash
npm start             # Development server
npm run build         # Production build
npm test              # Run tests
npm run eject         # Eject from Create React App
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## 🆘 Troubleshooting

### Common Issues

1. **API Connection Issues**
   - Verify backend server is running on port 8080
   - Check CORS configuration in Spring Boot
   - Ensure API base URL is correct in environment variables

2. **Build Issues**
   - Clear node_modules and reinstall: `rm -rf node_modules && npm install`
   - Clear npm cache: `npm cache clean --force`
   - Check Node.js version compatibility

3. **TypeScript Errors**
   - Ensure all dependencies have type definitions
   - Check tsconfig.json configuration
   - Verify import paths and module resolution

### Performance Issues
- Enable React DevTools Profiler
- Check bundle size with `npm run build`
- Monitor network requests in browser DevTools
- Use React Query DevTools for cache inspection

## 📚 Additional Resources

- [React Documentation](https://reactjs.org/docs)
- [Material-UI Documentation](https://mui.com/)
- [React Query Documentation](https://react-query.tanstack.com/)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [Ethereum Development Resources](https://ethereum.org/developers/)
