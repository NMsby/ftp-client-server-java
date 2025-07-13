import common.FTPConfig;
import server.FTPServer;
import server.UserManager;

/**
 * Main entry point for the FTP application
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("FTP Client-Server Application");
        System.out.println("Phase 3: Basic FTP Server Implementation");
        System.out.println();

        // Print configuration
        FTPConfig config = FTPConfig.getInstance();
        config.printConfiguration();

        // Print available users
        System.out.println();
        UserManager.getInstance().printAllUsers();

        // Check if we should start the server
        if (args.length > 0 && "server".equals(args[0])) {
            startServer();
        } else {
            System.out.println("\nTo start the FTP server, run: java Main server");
            System.out.println("Or run the FTPServer class directly");
        }
    }

    private static void startServer() {
        System.out.println("\nStarting FTP Server...");
        FTPServer.main(new String[0]);
    }
}