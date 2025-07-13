package utils;

import client.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Health check utility for FTP server monitoring
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class HealthChecker {
    private static final Logger logger = LogManager.getLogger("client");

    /**
     * Perform comprehensive health check
     *
     * @param hostname Server hostname
     * @param port     Server port
     * @param username Test username
     * @param password Test password
     * @return Health check result
     */
    public static HealthCheckResult performHealthCheck(String hostname, int port,
                                                       String username, String password) {
        HealthCheckResult result = new HealthCheckResult(hostname, port);

        logger.info("Starting health check for {}:{}", hostname, port);

        // Test 1: Port connectivity
        result.portAccessible = testPortConnectivity(hostname, port);

        // Test 2: FTP protocol response
        if (result.portAccessible) {
            result.ftpResponding = testFTPProtocol(hostname, port);
        }

        // Test 3: Authentication
        if (result.ftpResponding) {
            result.authenticationWorking = testAuthentication(hostname, port, username, password);
        }

        // Test 4: Basic operations
        if (result.authenticationWorking) {
            result.operationsWorking = testBasicOperations(hostname, port, username, password);
        }

        result.healthy = result.portAccessible && result.ftpResponding &&
                result.authenticationWorking && result.operationsWorking;

        logger.info("Health check completed for {}:{} - Status: {}",
                hostname, port, result.healthy ? "HEALTHY" : "UNHEALTHY");

        return result;
    }

    /**
     * Test port connectivity
     */
    private static boolean testPortConnectivity(String hostname, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(hostname, port), 5000);
            return true;
        } catch (Exception e) {
            logger.debug("Port connectivity test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Test FTP protocol response
     */
    private static boolean testFTPProtocol(String hostname, int port) {
        FTPClient client = new FTPClient();
        try {
            boolean connected = client.connect(hostname, port);
            if (connected) {
                client.disconnect();
                return true;
            }
        } catch (Exception e) {
            logger.debug("FTP protocol test failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Test authentication
     */
    private static boolean testAuthentication(String hostname, int port, String username, String password) {
        FTPClient client = new FTPClient();
        try {
            if (client.connect(hostname, port)) {
                boolean authenticated = client.login(username, password);
                client.disconnect();
                return authenticated;
            }
        } catch (Exception e) {
            logger.debug("Authentication test failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Test basic operations
     */
    private static boolean testBasicOperations(String hostname, int port, String username, String password) {
        FTPClient client = new FTPClient();
        try {
            if (client.connect(hostname, port) && client.login(username, password)) {
                // Test PWD
                String currentDir = client.getCurrentDirectory();
                if (currentDir == null) return false;

                // Test LIST
                String listing = client.listDirectory(null);
                if (listing == null) return false;

                // Test NOOP
                boolean noop = client.noop();

                client.disconnect();
                return noop;
            }
        } catch (Exception e) {
            logger.debug("Basic operations test failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Health check result container
     */
    public static class HealthCheckResult {
        public final String hostname;
        public final int port;
        public final LocalDateTime timestamp;

        public boolean portAccessible = false;
        public boolean ftpResponding = false;
        public boolean authenticationWorking = false;
        public boolean operationsWorking = false;
        public boolean healthy = false;

        public HealthCheckResult(String hostname, int port) {
            this.hostname = hostname;
            this.port = port;
            this.timestamp = LocalDateTime.now();
        }

        /**
         * Get formatted health check report
         */
        public String getReport() {
            StringBuilder report = new StringBuilder();

            report.append("=== FTP Server Health Check Report ===\n");
            report.append("Server: ").append(hostname).append(":").append(port).append("\n");
            report.append("Timestamp: ").append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            report.append("Overall Status: ").append(healthy ? "HEALTHY" : "UNHEALTHY").append("\n\n");

            report.append("Detailed Results:\n");
            report.append("  Port Accessible: ").append(getStatusSymbol(portAccessible)).append("\n");
            report.append("  FTP Responding: ").append(getStatusSymbol(ftpResponding)).append("\n");
            report.append("  Authentication: ").append(getStatusSymbol(authenticationWorking)).append("\n");
            report.append("  Basic Operations: ").append(getStatusSymbol(operationsWorking)).append("\n");

            if (!healthy) {
                report.append("\nRecommendations:\n");
                if (!portAccessible) {
                    report.append("  - Check if server is running\n");
                    report.append("  - Verify firewall settings\n");
                    report.append("  - Check network connectivity\n");
                }
                if (portAccessible && !ftpResponding) {
                    report.append("  - Server may be overloaded\n");
                    report.append("  - Check server logs for errors\n");
                }
                if (ftpResponding && !authenticationWorking) {
                    report.append("  - Verify test credentials\n");
                    report.append("  - Check user configuration\n");
                }
                if (authenticationWorking && !operationsWorking) {
                    report.append("  - Server may have file system issues\n");
                    report.append("  - Check server permissions\n");
                }
            }

            return report.toString();
        }

        private String getStatusSymbol(boolean status) {
            return status ? "✅ PASS" : "❌ FAIL";
        }
    }

    /**
     * Main method for command-line health checking
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java HealthChecker <hostname> <port> [username] [password]");
            System.out.println("Example: java HealthChecker localhost 21 admin admin123");
            System.exit(1);
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args.length > 2 ? args[2] : "admin";
        String password = args.length > 3 ? args[3] : "admin123";

        HealthCheckResult result = performHealthCheck(hostname, port, username, password);
        System.out.println(result.getReport());

        System.exit(result.healthy ? 0 : 1);
    }
}