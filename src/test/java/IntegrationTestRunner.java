import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Integration test suite runner
 * Runs all integration tests in the correct order
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
@Suite
@SelectClasses({
        RemoteTestSuite.class,
        PerformanceBenchmark.class
})
public class IntegrationTestRunner {

    /**
     * Main method to run integration tests from command line
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting FTP Server Integration Tests");
        System.out.println("====================================");

        // Parse command line arguments
        String serverHost = "localhost";
        String serverPort = "21";

        if (args.length >= 1) {
            serverHost = args[0];
        }
        if (args.length >= 2) {
            serverPort = args[1];
        }

        // Set system properties for tests
        System.setProperty("test.server.host", serverHost);
        System.setProperty("test.server.port", serverPort);

        System.out.println("Test target: " + serverHost + ":" + serverPort);
        System.out.println();

        // Run the test suite
        org.junit.platform.launcher.Launcher launcher = org.junit.platform.launcher.LauncherFactory.create();

        org.junit.platform.launcher.LauncherDiscoveryRequest request =
                org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                        .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(RemoteTestSuite.class))
                        .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(PerformanceBenchmark.class))
                        .build();

        launcher.execute(request);

        System.out.println();
        System.out.println("Integration tests completed");
    }
}