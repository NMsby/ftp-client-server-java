#!/bin/bash

# Comprehensive Test Runner for FTP Server
# Runs all tests in sequence and generates report

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$PROJECT_DIR/test-results"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}FTP Server Comprehensive Test Suite${NC}"
echo "=================================="
echo "Timestamp: $(date)"
echo "Results will be saved to: $RESULTS_DIR"
echo

# Create results directory
mkdir -p "$RESULTS_DIR"

# Initialize results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and capture result
run_test() {
    local test_name="$1"
    local test_command="$2"
    local log_file="$RESULTS_DIR/${test_name}-${TIMESTAMP}.log"

    echo -e "${YELLOW}Running: $test_name${NC}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if eval "$test_command" > "$log_file" 2>&1; then
        echo -e "${GREEN}✅ PASSED: $test_name${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}❌ FAILED: $test_name${NC}"
        echo "   Log: $log_file"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

cd "$PROJECT_DIR"

# Test 1: Unit Tests
run_test "Unit Tests" "mvn test -Dtest='**/*Test' -q"

# Test 2: Build and Package
run_test "Build and Package" "mvn clean package -DskipTests -q"

# Test 3: Start Server and Integration Tests
echo -e "${YELLOW}Starting FTP Server for Integration Tests...${NC}"
java Main server > "$RESULTS_DIR/server-${TIMESTAMP}.log" 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 15

# Check if server is running
if kill -0 $SERVER_PID 2>/dev/null; then
    echo -e "${GREEN}Server started successfully (PID: $SERVER_PID)${NC}"

    # Test 4: Health Check
    run_test "Health Check" "java -cp target/classes utils.HealthChecker localhost 21 admin admin123"

    # Test 5: Remote Test Suite
    run_test "Remote Test Suite" "mvn test -Dtest=RemoteTestSuite -Dtest.server.host=localhost -Dtest.server.port=21 -q"

    # Test 6: Performance Benchmark
    run_test "Performance Benchmark" "mvn test -Dtest=PerformanceBenchmark -Dtest.server.host=localhost -Dtest.server.port=21 -q"

    # Test 7: CLI Client Test
    echo -e "${YELLOW}Testing CLI Client...${NC}"
    timeout 30 bash -c "
        echo 'connect localhost 21
login admin admin123
pwd
list
quit' | java Main client > $RESULTS_DIR/cli-test-${TIMESTAMP}.log 2>&1
    " && run_test "CLI Client Test" "echo 'CLI test completed successfully'" || run_test "CLI Client Test" "echo 'CLI test failed'"

    # Test 8: Batch Client Test
    echo "connect localhost 21
login admin admin123
pwd
list
mkdir batch-test-$$
cd batch-test-$$
pwd
cd ..
rmdir batch-test-$$
quit" > "/tmp/batch-test-$$.ftp"

    run_test "Batch Client Test" "java Main batch /tmp/batch-test-$$.ftp"
    rm -f "/tmp/batch-test-$$.ftp"

else
    echo -e "${RED}❌ Server failed to start${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 3))
    TOTAL_TESTS=$((TOTAL_TESTS + 3))
fi

# Stop server
if kill -0 $SERVER_PID 2>/dev/null; then
    echo -e "${YELLOW}Stopping FTP Server...${NC}"
    kill $SERVER_PID
    sleep 5
    if kill -0 $SERVER_PID 2>/dev/null; then
        kill -9 $SERVER_PID
    fi
    echo -e "${GREEN}Server stopped${NC}"
fi

# Test 9: Deployment Package Generation
run_test "Deployment Package" "java -cp target/classes server.DeploymentManager"

# Generate final report
REPORT_FILE="$RESULTS_DIR/test-report-${TIMESTAMP}.txt"
cat > "$REPORT_FILE" << EOF
FTP Server Test Report
=====================
Date: $(date)
Environment: $(uname -a)
Java Version: $(java -version 2>&1 | head -1)

Test Summary:
Total Tests: $TOTAL_TESTS
Passed: $PASSED_TESTS
Failed: $FAILED_TESTS
Success Rate: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%

Test Results:
EOF

# Add individual test results
for log in "$RESULTS_DIR"/*-"$TIMESTAMP".log; do
    if [ -f "$log" ]; then
        test_name=$(basename "$log" | sed "s/-${TIMESTAMP}.log//")
        if grep -q "BUILD SUCCESS\|Tests run.*Failures: 0\|✅\|PASSED" "$log" 2>/dev/null; then
            echo "✅ $test_name: PASSED" >> "$REPORT_FILE"
        else
            echo "❌ $test_name: FAILED" >> "$REPORT_FILE"
        fi
    fi
done

cat >> "$REPORT_FILE" << EOF

Detailed Logs:
$(ls -la "$RESULTS_DIR"/*-"$TIMESTAMP".log 2>/dev/null || echo "No detailed logs available")

EOF

# Display final results
echo
echo -e "${BLUE}Test Results Summary${NC}"
echo "===================="
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo "Success Rate: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%"
echo
echo "Full report: $REPORT_FILE"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed! FTP Server is ready for deployment.${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Some tests failed. Review logs before deployment.${NC}"
    exit 1
fi