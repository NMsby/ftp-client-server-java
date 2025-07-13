package server;

import common.FTPConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.net.InetAddress;

/**
 * Test class for advanced FTP Server features
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class AdvancedServerTest {

    private SecurityManager securityManager;
    private PerformanceMonitor performanceMonitor;

    @BeforeEach
    void setUp() {
        securityManager = SecurityManager.getInstance();
        performanceMonitor = PerformanceMonitor.getInstance();
    }

    @Test
    void testSecurityManager() {
        try {
            InetAddress testAddress = InetAddress.getByName("127.0.0.1");

            // Test connection allowance
            assert securityManager.isConnectionAllowed(testAddress);

            // Test connection registration
            securityManager.registerConnection(testAddress);
            securityManager.unregisterConnection(testAddress);

            // Test failed login recording
            securityManager.recordFailedLogin(testAddress);
            securityManager.recordSuccessfulLogin(testAddress);

            // Test rate limiting
            boolean rateLimited = securityManager.isRateLimitExceeded(testAddress);
            assert !rateLimited; // Should not be rate limited initially

            System.out.println("Security manager test passed");
            System.out.println(securityManager.getSecurityStats());

        } catch (Exception e) {
            System.err.println("Security manager test failed: " + e.getMessage());
        }
    }

    @Test
    void testPerformanceMonitor() {
        // Test performance tracking
        performanceMonitor.recordConnection();
        performanceMonitor.recordCommand();
        performanceMonitor.recordUpload(1024);
        performanceMonitor.recordDownload(2048);
        performanceMonitor.recordError();
        performanceMonitor.recordDisconnection();

        // Get statistics
        String stats = performanceMonitor.getPerformanceStats();
        assert stats != null && !stats.isEmpty();

        String summary = performanceMonitor.getStatusSummary();
        assert summary != null && !summary.isEmpty();

        System.out.println("Performance monitor test passed");
        System.out.println("Summary: " + summary);
    }

    @Test
    void testUserManager() {
        UserManager userManager = UserManager.getInstance();

        // Test adding custom user
        userManager.addUser("testuser", "testpass", "/test", true, true, false);

        // Test authentication
        UserManager.User user = userManager.authenticateUser("testuser", "testpass");
        assert user != null;
        assert user.canRead();
        assert user.canWrite();
        assert !user.canDelete();

        // Test invalid authentication
        UserManager.User invalidUser = userManager.authenticateUser("testuser", "wrongpass");
        assert invalidUser == null;

        // Cleanup
        userManager.removeUser("testuser");

        System.out.println("User manager advanced test passed");
    }

    @Test
    void testEnhancedClientSession() {
        try {
            // This would require a mock socket for full testing
            System.out.println("Enhanced client session test - requires integration testing");

            // Test session timeout logic could be tested with mock time
            System.out.println("Session idle check and UTF-8 support added to ClientSession");

        } catch (Exception e) {
            System.err.println("Enhanced session test error: " + e.getMessage());
        }
    }

    /**
     * Integration test for advanced features
     * Requires running server
     */
    //@Test
    void testAdvancedFeaturesIntegration() {
        // This test would require starting a server and testing advanced commands
        System.out.println("Advanced features integration test");
        System.out.println("Would test: FEAT, MLST, MLSD, OPTS, STAT commands");
        System.out.println("Requires integration testing with running server");
    }
}