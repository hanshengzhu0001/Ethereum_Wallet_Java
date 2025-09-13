#!/bin/bash

# Ethereum Wallet Startup Script

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
    echo -e "${BLUE}   Ethereum Wallet - Java${NC}"
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
    
    # Check PostgreSQL (optional check)
    if command -v psql &> /dev/null; then
        print_status "PostgreSQL client found ✓"
    else
        print_warning "PostgreSQL client not found. Make sure PostgreSQL server is running."
    fi
}

# Function to setup database
setup_database() {
    print_status "Setting up database..."
    
    # Check if we can connect to the database
    if command -v psql &> /dev/null; then
        DB_HOST=${DB_HOST:-localhost}
        DB_PORT=${DB_PORT:-5432}
        DB_NAME=${DB_NAME:-ethereum_wallet}
        DB_USER=${DB_USERNAME:-ethereum_user}
        
        # Try to connect to database
        if PGPASSWORD=${DB_PASSWORD:-ethereum_pass} psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c '\q' 2>/dev/null; then
            print_status "Database connection successful ✓"
        else
            print_warning "Could not connect to database. Please ensure PostgreSQL is running and configured correctly."
            print_warning "Database: $DB_NAME on $DB_HOST:$DB_PORT as user $DB_USER"
        fi
    fi
}

# Function to build the application
build_application() {
    print_status "Building application..."
    
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        print_status "Build successful ✓"
    else
        print_error "Build failed!"
        exit 1
    fi
}

# Function to run tests
run_tests() {
    print_status "Running tests..."
    mvn test
    
    if [ $? -eq 0 ]; then
        print_status "All tests passed ✓"
    else
        print_warning "Some tests failed. Check the output above."
    fi
}

# Function to start the application
start_application() {
    print_status "Starting Ethereum Wallet..."
    
    JAR_FILE="target/ethereum-wallet-1.0.0.jar"
    
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found: $JAR_FILE"
        print_error "Please build the application first."
        exit 1
    fi
    
    # Set default environment variables if not provided
    export DB_USERNAME=${DB_USERNAME:-ethereum_user}
    export DB_PASSWORD=${DB_PASSWORD:-ethereum_pass}
    export INFURA_PROJECT_ID=${INFURA_PROJECT_ID:-your-infura-project-id}
    export INFURA_PROJECT_SECRET=${INFURA_PROJECT_SECRET:-your-infura-project-secret}
    export ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin123}
    
    # Check if running in GUI or headless mode
    MODE=${1:-gui}
    
    case $MODE in
        "gui")
            print_status "Starting in GUI mode..."
            java -jar $JAR_FILE
            ;;
        "headless")
            print_status "Starting in headless mode (API only)..."
            java -Djava.awt.headless=true -Dethereum.wallet.mode=headless -jar $JAR_FILE
            ;;
        "javafx")
            print_status "Starting JavaFX application..."
            mvn javafx:run
            ;;
        *)
            print_error "Invalid mode: $MODE"
            print_error "Valid modes: gui, headless, javafx"
            exit 1
            ;;
    esac
}

# Function to show help
show_help() {
    echo "Ethereum Wallet Startup Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  build          Build the application"
    echo "  test           Run tests"
    echo "  start [MODE]   Start the application"
    echo "                 MODE: gui (default), headless, javafx"
    echo "  setup          Setup database and environment"
    echo "  docker         Start with Docker Compose"
    echo "  help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build                 # Build the application"
    echo "  $0 test                  # Run tests"
    echo "  $0 start gui             # Start GUI application"
    echo "  $0 start headless        # Start API server only"
    echo "  $0 docker                # Start with Docker"
    echo ""
    echo "Environment Variables:"
    echo "  DB_USERNAME              Database username (default: ethereum_user)"
    echo "  DB_PASSWORD              Database password (default: ethereum_pass)"
    echo "  INFURA_PROJECT_ID        Infura project ID"
    echo "  INFURA_PROJECT_SECRET    Infura project secret"
    echo "  ADMIN_PASSWORD           Admin password (default: admin123)"
}

# Function to start with Docker
start_docker() {
    print_status "Starting with Docker Compose..."
    
    if ! command -v docker-compose &> /dev/null && ! command -v docker &> /dev/null; then
        print_error "Docker or Docker Compose is not installed."
        exit 1
    fi
    
    # Check if .env file exists for Docker environment variables
    if [ ! -f ".env" ]; then
        print_warning ".env file not found. Creating template..."
        cat > .env << EOF
INFURA_PROJECT_ID=your-infura-project-id
INFURA_PROJECT_SECRET=your-infura-project-secret
ADMIN_PASSWORD=admin123
EOF
        print_warning "Please update .env file with your Infura credentials."
    fi
    
    if command -v docker-compose &> /dev/null; then
        docker-compose up --build
    else
        docker compose up --build
    fi
}

# Main script logic
main() {
    print_header
    
    case "${1:-start}" in
        "build")
            check_prerequisites
            build_application
            ;;
        "test")
            check_prerequisites
            run_tests
            ;;
        "start")
            check_prerequisites
            setup_database
            start_application "${2:-gui}"
            ;;
        "setup")
            check_prerequisites
            setup_database
            ;;
        "docker")
            start_docker
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
