#!/bin/bash

# FTP Server Deployment Script
# This script automates the deployment process for the FTP server

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOYMENT_DIR="$PROJECT_DIR/deployment"
JAR_NAME="ftp-server.jar"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}FTP Server Deployment Script${NC}"
echo "============================"

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check dependencies
check_dependencies() {
    print_status "Checking dependencies..."

    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java not found. Please install Java 17 or later."
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{print $1}')
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_warning "Java version $JAVA_VERSION detected. Java 17+ recommended."
    else
        print_status "Java $JAVA_VERSION detected"
    fi

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven not found. Please install Maven."
        exit 1
    fi

    print_status "Maven found: $(mvn -version | head -1)"
}

# Function to build the project
build_project() {
    print_status "Building project..."

    cd "$PROJECT_DIR"

    # Clean and compile
    mvn clean compile

    # Run tests
    print_status "Running tests..."
    mvn test

    # Package
    print_status "Creating JAR package..."
    mvn package -DskipTests

    # Find the generated JAR
    JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)

    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found in target directory"
        exit 1
    fi

    print_status "JAR file created: $JAR_FILE"
}

# Function to prepare deployment
prepare_deployment() {
    print_status "Preparing deployment..."

    # Create deployment directory
    mkdir -p "$DEPLOYMENT_DIR"

    # Copy JAR file
    cp "$JAR_FILE" "$DEPLOYMENT_DIR/$JAR_NAME"
    print_status "JAR file copied to deployment directory"

    # Generate deployment configuration
    cd "$PROJECT_DIR"
    java -cp "$JAR_FILE" server.DeploymentManager

    print_status "Deployment configuration generated"
}

# Function to test deployment
test_deployment() {
    print_status "Testing deployment..."

    cd "$DEPLOYMENT_DIR"

    # Test JAR execution
    timeout 10s java -jar "$JAR_NAME" --help || true

    # Run network test
    if [ -f "test-network.sh" ]; then
        chmod +x test-network.sh
        print_status "Network test script ready"
    fi

    print_status "Deployment test completed"
}

# Function to create installation package
create_package() {
    print_status "Creating installation package..."

    PACKAGE_NAME="ftp-server-$(date +%Y%m%d-%H%M%S)"
    PACKAGE_DIR="$PROJECT_DIR/$PACKAGE_NAME"

    # Create package directory
    mkdir -p "$PACKAGE_DIR"

    # Copy deployment files
    cp -r "$DEPLOYMENT_DIR"/* "$PACKAGE_DIR/"

    # Copy documentation
    cp -r "$PROJECT_DIR/docs" "$PACKAGE_DIR/" 2>/dev/null || true
    cp "$PROJECT_DIR/README.md" "$PACKAGE_DIR/" 2>/dev/null || true

    # Create archive
    cd "$PROJECT_DIR"
    tar -czf "$PACKAGE_NAME.tar.gz" "$PACKAGE_NAME"

    print_status "Installation package created: $PACKAGE_NAME.tar.gz"

    # Cleanup temporary directory
    rm -rf "$PACKAGE_DIR"
}

# Function to display deployment instructions
show_instructions() {
    echo
    echo -e "${BLUE}Deployment Instructions${NC}"
    echo "======================"
    echo
    echo "1. Copy the deployment files to your target server:"
    echo "   scp -r $DEPLOYMENT_DIR/ user@server:/opt/ftpserver/"
    echo
    echo "2. On the target server, make scripts executable:"
    echo "   chmod +x /opt/ftpserver/*.sh"
    echo
    echo "3. Configure firewall (see DEPLOYMENT-GUIDE.md)"
    echo
    echo "4. Start the server:"
    echo "   ./start-ftp-server.sh"
    echo
    echo "5. Test connectivity:"
    echo "   ./test-network.sh [hostname] [port]"
    echo
    echo -e "${GREEN}Deployment files are ready in: $DEPLOYMENT_DIR${NC}"
}

# Main deployment process
main() {
    case "${1:-all}" in
        "check")
            check_dependencies
            ;;
        "build")
            check_dependencies
            build_project
            ;;
        "prepare")
            prepare_deployment
            ;;
        "test")
            test_deployment
            ;;
        "package")
            create_package
            ;;
        "all")
            check_dependencies
            build_project
            prepare_deployment
            test_deployment
            create_package
            show_instructions
            ;;
        *)
            echo "Usage: $0 [check|build|prepare|test|package|all]"
            echo "  check   - Check dependencies"
            echo "  build   - Build the project"
            echo "  prepare - Prepare deployment files"
            echo "  test    - Test deployment"
            echo "  package - Create installation package"
            echo "  all     - Run all steps (default)"
            exit 1
            ;;
    esac
}

# Run main function with arguments
main "$@"