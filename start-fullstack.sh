#!/bin/bash

# Ethereum Wallet Full-Stack Startup Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}   Ethereum Wallet Full-Stack${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or higher."
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java 17 or higher is required. Current version: $JAVA_VERSION"
        exit 1
    fi
    
    print_status "Java version: $JAVA_VERSION ✓"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.8 or higher."
        exit 1
    fi
    
    print_status "Maven found ✓"
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js 16 or higher."
        exit 1
    fi
    
    NODE_VERSION=$(node -v | sed 's/v//' | cut -d'.' -f1)
    if [ "$NODE_VERSION" -lt 16 ]; then
        print_error "Node.js 16 or higher is required. Current version: $NODE_VERSION"
        exit 1
    fi
    
    print_status "Node.js version: $(node -v) ✓"
    
    # Check npm
    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install npm."
        exit 1
    fi
    
    print_status "npm version: $(npm -v) ✓"
}

# Function to setup environment
setup_environment() {
    print_status "Setting up environment..."
    
    # Check if environment variables are set
    if [ -z "$INFURA_PROJECT_ID" ]; then
        print_warning "INFURA_PROJECT_ID not set. Using default value."
        export INFURA_PROJECT_ID="your-infura-project-id"
    fi
    
    if [ -z "$DB_USERNAME" ]; then
        export DB_USERNAME="ethereum_user"
    fi
    
    if [ -z "$DB_PASSWORD" ]; then
        export DB_PASSWORD="ethereum_pass"
    fi
    
    if [ -z "$ADMIN_PASSWORD" ]; then
        export ADMIN_PASSWORD="admin123"
    fi
    
    print_status "Environment variables configured ✓"
}

# Function to build backend
build_backend() {
    print_status "Building Spring Boot backend..."
    
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        print_status "Backend build successful ✓"
    else
        print_error "Backend build failed!"
        exit 1
    fi
}

# Function to setup frontend
setup_frontend() {
    print_status "Setting up React frontend..."
    
    if [ ! -d "frontend" ]; then
        print_error "Frontend directory not found."
        exit 1
    fi
    
    cd frontend
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install
        
        if [ $? -eq 0 ]; then
            print_status "Frontend dependencies installed ✓"
        else
            print_error "Failed to install frontend dependencies!"
            exit 1
        fi
    else
        print_status "Frontend dependencies already installed ✓"
    fi
    
    # Create .env file if it doesn't exist
    if [ ! -f ".env" ]; then
        print_status "Creating frontend .env file..."
        cat > .env << EOF
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_ETHERSCAN_BASE_URL=https://sepolia.etherscan.io
REACT_APP_NETWORK_NAME=Sepolia Testnet
REACT_APP_CHAIN_ID=11155111
EOF
        print_status "Frontend .env file created ✓"
    fi
    
    cd ..
}

# Function to start backend
start_backend() {
    print_status "Starting Spring Boot backend..."
    
    # Start backend in background
    nohup java -jar target/ethereum-wallet-1.0.0.jar > backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > backend.pid
    
    print_status "Backend started with PID: $BACKEND_PID"
    print_status "Backend logs: tail -f backend.log"
    
    # Wait for backend to start
    print_status "Waiting for backend to start..."
    sleep 10
    
    # Check if backend is running
    if curl -s http://localhost:8080/api/wallets > /dev/null 2>&1; then
        print_status "Backend is running and accessible ✓"
    else
        print_warning "Backend may still be starting. Check backend.log for details."
    fi
}

# Function to start frontend
start_frontend() {
    print_status "Starting React frontend..."
    
    cd frontend
    
    # Start frontend in background
    nohup npm start > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../frontend.pid
    
    print_status "Frontend started with PID: $FRONTEND_PID"
    print_status "Frontend logs: tail -f frontend.log"
    
    cd ..
    
    # Wait for frontend to start
    print_status "Waiting for frontend to start..."
    sleep 15
    
    print_status "Frontend should be available at: http://localhost:3000"
}

# Function to show status
show_status() {
    echo ""
    print_header
    echo ""
    print_status "🚀 Ethereum Wallet Full-Stack Application Started!"
    echo ""
    print_status "📊 Services:"
    print_status "  • Backend API: http://localhost:8080/api"
    print_status "  • Frontend Web App: http://localhost:3000"
    print_status "  • Database: PostgreSQL (localhost:5432)"
    echo ""
    print_status "📝 Logs:"
    print_status "  • Backend: tail -f backend.log"
    print_status "  • Frontend: tail -f frontend.log"
    echo ""
    print_status "🛑 To stop services:"
    print_status "  • ./stop-fullstack.sh"
    print_status "  • Or kill processes manually using PIDs in *.pid files"
    echo ""
    print_status "🔧 Environment:"
    print_status "  • Network: Sepolia Testnet"
    print_status "  • Database User: $DB_USERNAME"
    print_status "  • Infura Project: $INFURA_PROJECT_ID"
    echo ""
}

# Function to cleanup on exit
cleanup() {
    print_status "Cleaning up..."
    
    if [ -f "backend.pid" ]; then
        BACKEND_PID=$(cat backend.pid)
        if kill -0 $BACKEND_PID 2>/dev/null; then
            print_status "Stopping backend (PID: $BACKEND_PID)..."
            kill $BACKEND_PID
        fi
        rm -f backend.pid
    fi
    
    if [ -f "frontend.pid" ]; then
        FRONTEND_PID=$(cat frontend.pid)
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            print_status "Stopping frontend (PID: $FRONTEND_PID)..."
            kill $FRONTEND_PID
        fi
        rm -f frontend.pid
    fi
}

# Main execution
main() {
    print_header
    
    # Set trap for cleanup on exit
    trap cleanup EXIT INT TERM
    
    check_prerequisites
    setup_environment
    build_backend
    setup_frontend
    start_backend
    start_frontend
    show_status
    
    # Keep script running
    print_status "Press Ctrl+C to stop all services..."
    while true; do
        sleep 1
    done
}

# Check command line arguments
case "${1:-start}" in
    "start")
        main
        ;;
    "help"|"-h"|"--help")
        echo "Ethereum Wallet Full-Stack Startup Script"
        echo ""
        echo "Usage: $0 [COMMAND]"
        echo ""
        echo "Commands:"
        echo "  start          Start both backend and frontend (default)"
        echo "  help           Show this help message"
        echo ""
        echo "Environment Variables:"
        echo "  DB_USERNAME              Database username"
        echo "  DB_PASSWORD              Database password"
        echo "  INFURA_PROJECT_ID        Infura project ID"
        echo "  INFURA_PROJECT_SECRET    Infura project secret"
        echo "  ADMIN_PASSWORD           Admin password"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac
