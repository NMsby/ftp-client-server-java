package server;

import common.FTPConfig;
import org.junit.jupiter.api.Test;
import utils.NetworkUtils;

import java.io.IOException;

/**
 * Test class for FTP Server functionality
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ServerTest {

    @Test
    public void testServerConfiguration() {
        FTPConfig config = FTPConfig.getInstance();
        FTPServer server = new FTPServer(config);

        // Basic server creation test
        assert !server.isRunning();
        assert server.getActiveConnections() == 0;
        assert server.getConfig() != null;

        System.out.println("Server configuration test passed");
    }

    @Test
    public void testUserManager() {
        UserManager userManager = UserManager.getInstance();

        // Test user authentication
        UserManager.User admin = userManager.authenticateUser("admin", "admin123");
        assert admin != null;
        assert admin.canRead();
        assert admin.canWrite();
        assert admin.canDelete();

        UserManager.User user = userManager.authenticateUser("user", "user123");
        assert user != null;
        assert user.canRead();
        assert user.canWrite();
        assert !user.canDelete();

        // Test invalid credentials
        UserManager.User invalid = userManager.authenticateUser("admin", "wrongpass");
        assert invalid == null;

        System.out.println("User manager test passed");
        userManager.printAllUsers();
    }

    @Test
    public void testNetworkUtilities() {
        // Test port availability check
        int availablePort = NetworkUtils.findAvailablePort(20000, 21000);
        assert availablePort != -1;

        // Test IP address resolution
        String localIP = NetworkUtils.getLocalIPAddress();
        assert localIP != null && !localIP.isEmpty();

        System.out.println("Network utilities test passed");
        System.out.println("Available port: " + availablePort);
        System.out.println("Local IP: " + localIP);
    }

    /**
     * Manual test method to start server for testing
     * This is not a JUnit test but can be run manually
     */
    public static void startTestServer() {
        System.out.println("Starting test FTP server...");

        FTPServer server = new FTPServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down test server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start test server: " + e.getMessage());
        }
    }
}