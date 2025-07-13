package client;

import common.FTPConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test class for FTP Client functionality
 * Note: These tests require a running FTP server
 *
 * @author Nelson Masbayi
 * @version 1.0
 */

public class ClientTest {

    private FTPClient client;
    private FTPConfig config;

    @BeforeEach
    void setUp() {
        config = FTPConfig.getInstance();
        client = new FTPClient(config);
    }

    @AfterEach
    void tearDown() {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    void testClientCreation() {
        assert client != null;
        assert !client.isConnected();
        assert !client.isAuthenticated();
        assert client.getCurrentLocalDirectory() != null;

        System.out.println("Client creation test passed");
    }

    @Test
    void testConnectionFailure() {
        // Test connection to non-existent server
        boolean connected = client.connect("nonexistent.server", 21);
        assert !connected;
        assert !client.isConnected();

        System.out.println("Connection failure test passed");
    }

    @Test
    void testLocalDirectoryOperations() {
        Path currentDir = client.getCurrentLocalDirectory();
        assert currentDir != null;

        // Test setting local directory
        String tempDir = System.getProperty("java.io.tmpdir");
        client.setCurrentLocalDirectory(tempDir);

        // Should be able to set to valid directory
        Path newDir = client.getCurrentLocalDirectory();
        assert newDir != null;

        System.out.println("Local directory operations test passed");
        System.out.println("Current local directory: " + newDir);
    }

    @Test
    void testProgressTracker() {
        ProgressTracker tracker = new ProgressTracker("Testing", 1024);

        // Simulate progress updates
        for (int i = 0; i <= 10; i++) {
            tracker.updateProgress(i * 102);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        tracker.complete();
        System.out.println("Progress tracker test completed");
    }

    @Test
    void testBatchScriptValidation() {
        BatchClient batchClient = new BatchClient();

        // Create a simple test script
        try {
            Path scriptPath = Paths.get(config.getClientRootDirectory(), "test-validation.ftp");
            Files.write(scriptPath, "# Test script\necho Hello World\n".getBytes());

            // This should work even without server connection for basic validation
            boolean result = batchClient.executeScript(scriptPath.toString(), false);

            // Clean up
            Files.deleteIfExists(scriptPath);

            System.out.println("Batch script validation test completed");

        } catch (IOException e) {
            System.err.println("Error in batch script test: " + e.getMessage());
        }
    }

    /**
     * Integration test - requires running server
     * This test is disabled by default since it requires external setup
     */
    //@Test
    void testFullIntegration() {
        // Connect to localhost server (if running)
        if (client.connect("localhost", 21)) {
            System.out.println("Connected to test server");

            // Try to login
            if (client.login("test", "test")) {
                System.out.println("Logged in successfully");

                // Test basic operations
                String directory = client.getCurrentDirectory();
                System.out.println("Current directory: " + directory);

                String listing = client.listDirectory(null);
                System.out.println("Directory listing:");
                System.out.println(listing);

                client.disconnect();
                System.out.println("Integration test completed successfully");
            } else {
                System.out.println("Login failed");
            }
        } else {
            System.out.println("Could not connect to test server (this is expected if server is not running)");
        }
    }

    /**
     * Manual test method to test command-line client
     */
    public static void startTestClient() {
        System.out.println("Starting test command-line client...");
        System.out.println("Note: You can test with 'connect localhost 21' if server is running");

        CommandLineClient.main(new String[0]);
    }
}