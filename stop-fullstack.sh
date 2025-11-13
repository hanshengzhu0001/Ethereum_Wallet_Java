#!/bin/bash

# Ethereum Wallet Full-Stack Stop Script

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
    echo -e "${BLUE}   Stopping Ethereum Wallet${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Function to stop backend
stop_backend() {
    if [ -f "backend.pid" ]; then
        BACKEND_PID=$(cat backend.pid)
        if kill -0 $BACKEND_PID 2>/dev/null; then
            print_status "Stopping backend (PID: $BACKEND_PID)..."
            kill $BACKEND_PID
            
            # Wait for process to stop
            sleep 3
            
            # Force kill if still running
            if kill -0 $BACKEND_PID 2>/dev/null; then
                print_warning "Force killing backend process..."
                kill -9 $BACKEND_PID
            fi
            
            print_status "Backend stopped ✓"
        else
            print_warning "Backend process not running"
        fi
        rm -f backend.pid
    else
        print_warning "Backend PID file not found"
    fi
    
    # Also try to kill any Java processes running the wallet
    WALLET_PIDS=$(pgrep -f "ethereum-wallet-1.0.0.jar" || true)
    if [ ! -z "$WALLET_PIDS" ]; then
        print_status "Found additional wallet processes: $WALLET_PIDS"
        echo $WALLET_PIDS | xargs kill -9 2>/dev/null || true
    fi
}

# Function to stop frontend
stop_frontend() {
    if [ -f "frontend.pid" ]; then
        FRONTEND_PID=$(cat frontend.pid)
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            print_status "Stopping frontend (PID: $FRONTEND_PID)..."
            kill $FRONTEND_PID
            
            # Wait for process to stop
            sleep 3
            
            # Force kill if still running
            if kill -0 $FRONTEND_PID 2>/dev/null; then
                print_warning "Force killing frontend process..."
                kill -9 $FRONTEND_PID
            fi
            
            print_status "Frontend stopped ✓"
        else
            print_warning "Frontend process not running"
        fi
        rm -f frontend.pid
    else
        print_warning "Frontend PID file not found"
    fi
    
    # Also try to kill any npm/node processes for the frontend
    REACT_PIDS=$(pgrep -f "react-scripts start" || true)
    if [ ! -z "$REACT_PIDS" ]; then
        print_status "Found additional React processes: $REACT_PIDS"
        echo $REACT_PIDS | xargs kill -9 2>/dev/null || true
    fi
}

# Function to cleanup log files
cleanup_logs() {
    if [ "$1" = "--clean-logs" ]; then
        print_status "Cleaning up log files..."
        rm -f backend.log frontend.log
        print_status "Log files cleaned ✓"
    fi
}

# Function to show final status
show_status() {
    echo ""
    print_header
    echo ""
    print_status "🛑 Ethereum Wallet services stopped!"
    echo ""
    print_status "📝 Log files preserved:"
    if [ -f "backend.log" ]; then
        print_status "  • Backend: backend.log ($(wc -l < backend.log) lines)"
    fi
    if [ -f "frontend.log" ]; then
        print_status "  • Frontend: frontend.log ($(wc -l < frontend.log) lines)"
    fi
    echo ""
    print_status "🧹 To clean logs: $0 --clean-logs"
    print_status "🚀 To restart: ./start-fullstack.sh"
    echo ""
}

# Main execution
main() {
    print_header
    
    stop_backend
    stop_frontend
    cleanup_logs "$1"
    show_status
}

# Check command line arguments
case "${1:-stop}" in
    "stop"|"")
        main
        ;;
    "--clean-logs")
        main "--clean-logs"
        ;;
    "help"|"-h"|"--help")
        echo "Ethereum Wallet Full-Stack Stop Script"
        echo ""
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  (no args)      Stop all services"
        echo "  --clean-logs   Stop services and clean log files"
        echo "  help           Show this help message"
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac
