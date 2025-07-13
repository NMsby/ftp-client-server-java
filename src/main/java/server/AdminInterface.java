package server;

import common.FTPConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Administrative interface for FTP server management
 * Provides runtime server management and monitoring capabilities
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class AdminInterface {
    private static final Logger logger = LogManager.getLogger("server");

    private final FTPServer server;
    private final Scanner scanner;
    private boolean running;

    /**
     * Constructor
     * @param server FTP server instance
     */
    public AdminInterface(FTPServer server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    /**
     * Start the administrative interface
     */
    public void start() {
        printWelcome();
        printHelp();

        while (running && server.isRunning()) {
            try {
                System.out.print("admin> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                processCommand(input);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                logger.error("Admin command error", e);
            }
        }

        scanner.close();
    }

    /**
     * Process administrative command
     * @param input User input
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "status":
                handleStatus();
                break;
            case "stats":
                handleStats();
                break;
            case "connections":
                handleConnections();
                break;
            case "users":
                handleUsers();
                break;
            case "security":
                handleSecurity();
                break;
            case "performance":
                handlePerformance();
                break;
            case "config":
                handleConfig();
                break;
            case "reset":
                handleReset(parts);
                break;
            case "ban":
                handleBan(parts);
                break;
            case "unban":
                handleUnban(parts);
                break;
            case "kick":
                handleKick(parts);
                break;
            case "shutdown":
                handleShutdown(parts);
                break;
            case "help":
            case "?":
                printHelp();
                break;
            case "quit":
            case "exit":
                handleQuit();
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
        }
    }

    /**
     * Handle status command
     */
    private void handleStatus() {
        System.out.println("=== Server Status ===");
        System.out.println("Running: " + server.isRunning());
        System.out.println("Active Connections: " + server.getActiveConnections());
        System.out.println("Performance: " + server.getPerformanceMonitor().getStatusSummary());
        System.out.println("==================");
    }

    /**
     * Handle stats command
     */
    private void handleStats() {
        System.out.println(server.getServerStatistics());
    }

    /**
     * Handle connections command
     */
    private void handleConnections() {
        System.out.println("Active Connections: " + server.getActiveConnections());
        System.out.println("Maximum Connections: " + server.getConfig().getMaxConnections());

        // Additional connection details would require tracking in server
        System.out.println("Use 'security' command for IP-based connection info");
    }

    /**
     * Handle users command
     */
    private void handleUsers() {
        System.out.println("=== User Management ===");
        UserManager.getInstance().printAllUsers();
        System.out.println("=====================");
    }

    /**
     * Handle security command
     */
    private void handleSecurity() {
        System.out.println(server.getSecurityManager().getSecurityStats());
    }

    /**
     * Handle performance command
     */
    private void handlePerformance() {
        System.out.println(server.getPerformanceMonitor().getPerformanceStats());
    }

    /**
     * Handle config command
     */
    private void handleConfig() {
        FTPConfig.getInstance().printConfiguration();
    }

    /**
     * Handle reset command
     */
    private void handleReset(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: reset <stats|security>");
            return;
        }

        String target = parts[1].toLowerCase();
        switch (target) {
            case "stats":
                server.getPerformanceMonitor().resetStats();
                System.out.println("Performance statistics reset");
                break;
            case "security":
                System.out.println("Security reset not implemented (requires restart)");
                break;
            default:
                System.out.println("Unknown reset target: " + target);
        }
    }

    /**
     * Handle ban command
     */
    private void handleBan(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: ban <ip-address>");
            return;
        }

        // This would require extending SecurityManager with manual ban functionality
        System.out.println("Manual IP banning not implemented in current version");
        System.out.println("IPs are automatically banned after failed login attempts");
    }

    /**
     * Handle unban command
     */
    private void handleUnban(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: unban <ip-address>");
            return;
        }

        System.out.println("Manual IP unbanning not implemented in current version");
        System.out.println("Bans expire automatically after configured duration");
    }

    /**
     * Handle kick command
     */
    private void handleKick(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: kick <username|ip-address>");
            return;
        }

        System.out.println("Client kicking not implemented in current version");
        System.out.println("This would require connection tracking by username/IP");
    }

    /**
     * Handle shutdown command
     */
    private void handleShutdown(String[] parts) {
        boolean force = parts.length > 1 && "force".equals(parts[1]);

        if (!force) {
            System.out.print("Are you sure you want to shutdown the server? (y/N): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!"y".equals(confirm) && !"yes".equals(confirm)) {
                System.out.println("Shutdown cancelled");
                return;
            }
        }

        System.out.println("Shutting down server...");
        server.stop();
        running = false;
    }

    /**
     * Handle quit command
     */
    private void handleQuit() {
        System.out.println("Exiting admin interface...");
        running = false;
    }

    /**
     * Print welcome message
     */
    private void printWelcome() {
        System.out.println();
        System.out.println("=" .repeat(50));
        System.out.println("      FTP SERVER ADMINISTRATION");
        System.out.println("=" .repeat(50));
        System.out.println("Administrative interface for FTP server management");
        System.out.println("Type 'help' for available commands");
        System.out.println();
    }

    /**
     * Print help information
     */
    private void printHelp() {
        System.out.println("Administrative Commands:");
        System.out.println();

        System.out.println("Server Status:");
        System.out.println("  status                 - Show server status summary");
        System.out.println("  stats                  - Show detailed statistics");
        System.out.println("  connections            - Show connection information");
        System.out.println();

        System.out.println("Monitoring:");
        System.out.println("  performance            - Show performance statistics");
        System.out.println("  security               - Show security statistics");
        System.out.println("  users                  - Show registered users");
        System.out.println("  config                 - Show server configuration");
        System.out.println();

        System.out.println("Management:");
        System.out.println("  reset <stats|security> - Reset statistics or security data");
        System.out.println("  ban <ip>               - Ban IP address (not implemented)");
        System.out.println("  unban <ip>             - Unban IP address (not implemented)");
        System.out.println("  kick <user|ip>         - Kick client (not implemented)");
        System.out.println();

        System.out.println("Server Control:");
        System.out.println("  shutdown [force]       - Shutdown server");
        System.out.println("  quit                   - Exit admin interface");
        System.out.println("  help                   - Show this help");
        System.out.println();
    }

    /**
     * Start admin interface in separate thread
     * @param server FTP server instance
     */
    public static void startAdminInterface(FTPServer server) {
        Thread adminThread = new Thread(() -> {
            AdminInterface admin = new AdminInterface(server);
            admin.start();
        });

        adminThread.setName("AdminInterface");
        adminThread.setDaemon(false); // Keep JVM alive
        adminThread.start();
    }
}