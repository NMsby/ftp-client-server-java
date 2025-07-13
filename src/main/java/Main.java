import common.FTPConfig;
import server.FTPServer;
import server.UserManager;
import client.CommandLineClient;
import client.BatchClient;

/**
 * Main entry point for the FTP application
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("FTP Client-Server Application");
        System.out.println("Phase 4: Command-Line FTP Client Implementation");
        System.out.println();

        if (args.length == 0) {
            showUsage();
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "server":
                startServer();
                break;
            case "client":
                startClient(args);
                break;
            case "batch":
                startBatchClient(args);
                break;
            case "config":
                showConfiguration();
                break;
            default:
                System.out.println("Unknown mode: " + mode);
                showUsage();
        }
    }

    private static void showUsage() {
        System.out.println("Usage: java Main <mode> [options]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  server                 - Start FTP server");
        System.out.println("  client [host] [port]   - Start interactive FTP client");
        System.out.println("  batch <script>         - Execute FTP script");
        System.out.println("  config                 - Show configuration");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main server");
        System.out.println("  java Main client");
        System.out.println("  java Main client localhost 21");
        System.out.println("  java Main batch test-script.ftp");
    }

    private static void startServer() {
        System.out.println("Starting FTP Server...");
        FTPServer.main(new String[0]);
    }

    private static void startClient(String[] args) {
        System.out.println("Starting FTP Client...");

        if (args.length >= 3) {
            // Auto-connect mode
            String[] clientArgs = new String[args.length - 1];
            clientArgs[0] = "connect";
            System.arraycopy(args, 1, clientArgs, 1, args.length - 1);
            CommandLineClient.main(clientArgs);
        } else {
            // Interactive mode
            CommandLineClient.main(new String[0]);
        }
    }

    private static void startBatchClient(String[] args) {
        if (args.length < 2) {
            System.out.println("Batch mode requires script file");
            System.out.println("Usage: java Main batch <script-file>");
            return;
        }

        System.out.println("Starting Batch FTP Client...");

        // Pass remaining arguments to batch client
        String[] batchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, batchArgs, 0, args.length - 1);

        BatchClient.main(batchArgs);
    }

    private static void showConfiguration() {
        System.out.println("FTP Configuration:");
        FTPConfig config = FTPConfig.getInstance();
        config.printConfiguration();

        System.out.println();
        System.out.println("Available Users:");
        UserManager.getInstance().printAllUsers();
    }
}