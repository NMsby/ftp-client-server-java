# FTP Server Testing Guide

## Overview

This guide covers comprehensive testing procedures for the FTP server, including local testing, remote deployment testing, and performance benchmarking.

## Test Categories

### 1. Unit Tests
Basic functionality tests for individual components.

```bash
  # Run all unit tests
  mvn test

  # Run specific test class
  mvn test -Dtest=ClientTest
  mvn test -Dtest=ServerTest
```

### 2. Integration Tests
End-to-end functionality tests with running server.

```bash
  # Start server in one terminal
  java Main server

  # Run integration tests in another terminal
  java -cp target/classes:target/test-classes IntegrationTestRunner [hostname] [port]

  # Or run specific test suite
  mvn test -Dtest=RemoteTestSuite -Dtest.server.host=localhost -Dtest.server.port=21
```

### 3. Performance Benchmarks
Load testing and performance measurement.

```bash
  # Run performance benchmarks
  mvn test -Dtest=PerformanceBenchmark -Dtest.server.host=localhost -Dtest.server.port=21
```

### 4. Health Checks
Server health monitoring and diagnostics.

```bash
  # Quick health check
  java -cp target/classes utils.HealthChecker localhost 21 admin admin123

  # Continuous monitoring
  while true; do 
    java -cp target/classes utils.HealthChecker localhost 21 admin admin123
    sleep 60
  done
```

## Local Testing Scenarios

### Scenario 1: Basic Server Functionality
**Objective**: Verify core server operations work correctly.

**Steps**:
1. Start server: `java Main server`
2. Connect with CLI client: `java Main client localhost 21`
3. Test authentication: `login admin admin123`
4. Test directory operations: `pwd`, `list`, `mkdir test`, `cd test`
5. Test file operations: Create local file, upload, download, delete
6. Disconnect: `quit`

**Expected Results**: All operations complete successfully.

### Scenario 2: GUI Client Testing
**Objective**: Verify GUI client functionality.

**Steps**:
1. Start server: `java Main server`
2. Launch GUI: `java Main gui`
3. Enter connection details and connect
4. Test file browser navigation
5. Test file upload/download via GUI
6. Test folder creation/deletion
7. Test progress indicators

**Expected Results**: GUI responsive, operations complete, progress shown.

### Scenario 3: Multiple Client Connections
**Objective**: Test concurrent client support.

**Steps**:
1. Start server: `java Main server`
2. Open multiple CLI clients simultaneously
3. Authenticate each client
4. Perform operations from different clients
5. Monitor server logs for connection handling

**Expected Results**: All clients work independently without interference.

## Remote Testing Scenarios

### Scenario 4: Local Network Testing
**Objective**: Test server accessibility within local network.

**Prerequisites**:
- FTP server running on one machine
- Client machines on same network
- Firewall configured properly

**Steps**:
1. Deploy server on target machine
2. Configure firewall rules
3. Test connectivity: `./test-network.sh [server-ip] 21`
4. Connect from remote client: `java Main client [server-ip] 21`
5. Perform file transfer operations
6. Monitor server performance

**Expected Results**: Remote connections work, file transfers complete.

### Scenario 5: Internet Deployment Testing
**Objective**: Test server over internet connection.

**Prerequisites**:
- Server deployed on public IP or VPS
- Port forwarding configured
- Domain name (optional)

**Steps**:
1. Deploy server using deployment scripts
2. Configure firewall and port forwarding
3. Test external connectivity
4. Connect from different networks
5. Test with various client types
6. Monitor security logs

**Expected Results**: External connections work, security measures active.

### Scenario 6: Production Load Testing
**Objective**: Test server under realistic production load.

**Steps**:
1. Deploy server in production-like environment
2. Run performance benchmarks
3. Simulate multiple concurrent users
4. Test large file transfers
5. Monitor system resources
6. Test during peak usage periods

**Expected Results**: Server handles load gracefully, performance acceptable.

## Testing Checklists

### Pre-Deployment Checklist
- [ ] All unit tests pass
- [ ] Integration tests pass with local server
- [ ] GUI client works correctly
- [ ] Multiple client connections work
- [ ] Performance benchmarks complete
- [ ] Security features tested
- [ ] Documentation complete
- [ ] Configuration files validated

### Deployment Checklist
- [ ] Target environment prepared
- [ ] Java 17+ installed
- [ ] Firewall configured
- [ ] Network connectivity tested
- [ ] User accounts configured
- [ ] File system permissions set
- [ ] Logging configured
- [ ] Monitoring setup

### Post-Deployment Checklist
- [ ] Health check passes
- [ ] Remote connectivity confirmed
- [ ] Authentication working
- [ ] File transfers successful
- [ ] Performance acceptable
- [ ] Logs generating correctly
- [ ] Security measures active
- [ ] Backup procedures tested

## Test Data Management

### Test Files
Create standardized test files for consistent testing:

```bash
  # Small text file (1KB)
  echo "Small test file content" > test-small.txt

  # Medium text file (100KB)
  base64 /dev/urandom | head -c 100000 > test-medium.txt

  # Large binary file (10MB)
  dd if=/dev/urandom of=test-large.bin bs=1024 count=10240
```

### Test User Accounts
Use consistent test accounts across environments:

- **admin/admin123**: Full administrative access
- **user/user123**: Standard user access
- **test/test**: Read-only access
- **guest/**: Anonymous access (if enabled)

### Test Directories
Create standard directory structure:

```
/
├── public/          # Public files
├── users/           # User home directories
│   ├── admin/
│   ├── user/
│   └── test/
├── uploads/         # Upload area
└── temp/           # Temporary files
```

## Performance Testing

### Baseline Measurements
Establish baseline performance metrics:

- **Connection Time**: < 1 second
- **Authentication Time**: < 500ms
- **Directory Listing**: < 2 seconds
- **Small File Transfer** (1KB): < 100ms
- **Large File Transfer** (1MB): < 10 seconds on local network

### Load Testing Scenarios

#### Light Load
- 5 concurrent connections
- 10 operations per connection
- Mix of file and directory operations
- Duration: 5 minutes

#### Medium Load
- 20 concurrent connections
- 50 operations per connection
- Emphasis on file transfers
- Duration: 15 minutes

#### Heavy Load
- 50 concurrent connections
- 100 operations per connection
- Large file transfers included
- Duration: 30 minutes

### Performance Monitoring
Monitor these metrics during testing:

- **CPU Usage**: Should not exceed 80%
- **Memory Usage**: Should not exceed 90%
- **Network Bandwidth**: Monitor utilization
- **Disk I/O**: Watch for bottlenecks
- **Connection Count**: Track active connections
- **Error Rate**: Should be < 1%

## Security Testing

### Authentication Testing
- [ ] Valid credentials accepted
- [ ] Invalid credentials rejected
- [ ] Brute force protection active
- [ ] Account lockout working
- [ ] Session timeout enforced

### Access Control Testing
- [ ] File permissions enforced
- [ ] Directory traversal prevented
- [ ] User isolation maintained
- [ ] Administrative functions protected

### Network Security Testing
- [ ] Firewall rules effective
- [ ] Port scanning detection
- [ ] Rate limiting functional
- [ ] IP-based restrictions working

## Troubleshooting Common Issues

### Connection Problems
**Symptoms**: Cannot connect to server
**Causes**: Firewall, network, server not running
**Solutions**: Check firewall, verify server status, test network

### Authentication Failures
**Symptoms**: Login rejected
**Causes**: Wrong credentials, user not configured
**Solutions**: Verify credentials, check user configuration

### Transfer Issues
**Symptoms**: File transfers fail or timeout
**Causes**: Large files, network issues, permissions
**Solutions**: Check file size limits, test network, verify permissions

### Performance Problems
**Symptoms**: Slow operations, timeouts
**Causes**: High load, insufficient resources, network latency
**Solutions**: Monitor resources, optimize configuration, upgrade hardware

## Continuous Testing

### Automated Testing Pipeline
Set up automated testing for continuous integration:

```bash
  #!/bin/bash
  # ci-test.sh - Continuous Integration Test Script

  # Build and test
  mvn clean test

  # Start server in background
  java Main server &
  SERVER_PID=$!

  # Wait for server startup
  sleep 10

  # Run integration tests
  mvn test -Dtest=RemoteTestSuite

  # Run performance tests
  mvn test -Dtest=PerformanceBenchmark

  # Stop server
  kill $SERVER_PID

  # Generate test report
  echo "CI Tests completed at $(date)"
```

### Monitoring and Alerting
Set up monitoring for production environments:

- Health check every 5 minutes
- Performance metrics collection
- Log analysis for errors
- Alert on failure conditions
- Weekly performance reports

## Test Reporting

### Test Results Documentation
Document test results with:

- Test environment details
- Test execution timestamps
- Pass/fail status for each test
- Performance metrics
- Issues encountered
- Recommendations for improvement

### Sample Test Report Format
```
FTP Server Test Report
=====================
Date: 2025-01-13
Environment: Local Development
Server Version: 1.0.0

Test Summary:
- Unit Tests: 45/45 PASSED
- Integration Tests: 12/12 PASSED
- Performance Tests: 8/8 PASSED
- Security Tests: 15/15 PASSED

Performance Metrics:
- Average Connection Time: 0.3s
- Average Transfer Rate: 15.2 MB/s
- Maximum Concurrent Connections: 47
- Memory Usage Peak: 512MB

Issues: None

Recommendations: Ready for deployment
```

This comprehensive testing approach ensures the FTP server is thoroughly validated before and after deployment, providing confidence in its reliability and performance.