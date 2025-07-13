package client;

import common.FTPConfig;
import utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Interactive command-line FTP client
 * Provides user-friendly interface for FTP operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class CommandLineClient {
    private static final Logger logger = LogManager.getLogger("client");

    private final FTPClient ftpClient;
    private final Scanner scanner;
    private boolean running;

    /**
     * Constructor
     */
    public CommandLineClient() {
        this.ftpClient = new FTPClient();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    /**
     * Start the interactive client
     */
    public void start() {
        printWelcome();
        printHelp();

        while (running) {
            try {
                System.out.print(getPrompt());
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                processCommand(input);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                logger.error("Command processing error", e);
            }
        }

        // Cleanup
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
        scanner.close();
    }

    /**
     * Process user command
     *
     * @param input User input
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "connect":
            case "open":
                handleConnect(parts);
                break;
            case "login":
            case "user":
                handleLogin(parts);
                break;
            case "pwd":
                handlePwd();
                break;
            case "cd":
            case "cwd":
                handleChangeDirectory(parts);
                break;
            case "ls":
            case "list":
            case "dir":
                handleList(parts);
                break;
            case "get":
            case "download":
                handleDownload(parts);
                break;
            case "put":
            case "upload":
                handleUpload(parts);
                break;
            case "delete":
            case "del":
            case "rm":
                handleDelete(parts);
                break;
            case "mkdir":
            case "md":
                handleMakeDirectory(parts);
                break;
            case "rmdir":
            case "rd":
                handleRemoveDirectory(parts);
                break;
            case "rename":
            case "ren":
                handleRename(parts);
                break;
            case "size":
                handleSize(parts);
                break;
            case "lcd":
                handleLocalChangeDirectory(parts);
                break;
            case "lls":
            case "ldir":
                handleLocalList(parts);
                break;
            case "lpwd":
                handleLocalPwd();
                break;
            case "status":
                handleStatus();
                break;
            case "syst":
            case "system":
                handleSystem();
                break;
            case "noop":
                handleNoop();
                break;
            case "help":
            case "?":
                printHelp();
                break;
            case "quit":
            case "exit":
            case "bye":
                handleQuit();
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
        }
    }

    /**
     * Handle connect command
     */
    private void handleConnect(String[] parts) {
        if (ftpClient.isConnected()) {
            System.out.println("Already connected. Use 'quit' to disconnect first.");
            return;
        }

        String host;
        int port = 21;

        if (parts.length < 2) {
            System.out.print("Host: ");
            host = scanner.nextLine().trim();
            if (host.isEmpty()) {
                System.out.println("Host required");
                return;
            }
        } else {
            host = parts[1];
            if (parts.length > 2) {
                try {
                    port = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number: " + parts[2]);
                    return;
                }
            }
        }

        System.out.println("Connecting to " + host + ":" + port + "...");

        if (ftpClient.connect(host, port)) {
            System.out.println("Connected to " + host);
            System.out.println("Use 'login' command to authenticate");
        } else {
            System.out.println("Connection failed");
        }
    }

    /**
     * Handle login command
     */
    private void handleLogin(String[] parts) {
        if (!ftpClient.isConnected()) {
            System.out.println("Not connected. Use 'connect' command first.");
            return;
        }

        if (ftpClient.isAuthenticated()) {
            System.out.println("Already authenticated");
            return;
        }

        String username;
        String password;

        if (parts.length < 2) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();
        } else {
            username = parts[1];
        }

        if (username.isEmpty()) {
            System.out.println("Username required");
            return;
        }

        // Use Console for password input if available (hides input)
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("Password: ");
            password = new String(passwordChars);
        } else {
            System.out.print("Password: ");
            password = scanner.nextLine();
        }

        if (ftpClient.login(username, password)) {
            System.out.println("Login successful");
            System.out.println("Current directory: " + ftpClient.getCurrentDirectory());
        } else {
            System.out.println("Login failed");
        }
    }

    /**
     * Handle pwd command
     */
    private void handlePwd() {
        if (!checkAuthenticated()) return;

        String directory = ftpClient.getCurrentDirectory();
        if (directory != null) {
            System.out.println("Remote directory: " + directory);
        } else {
            System.out.println("Failed to get current directory");
        }
    }

    /**
     * Handle change directory command
     */
    private void handleChangeDirectory(String[] parts) {
        if (!checkAuthenticated()) return;

        String directory;
        if (parts.length < 2) {
            System.out.print("Directory: ");
            directory = scanner.nextLine().trim();
        } else {
            directory = parts[1];
        }

        if (directory.isEmpty()) {
            System.out.println("Directory required");
            return;
        }

        if (ftpClient.changeDirectory(directory)) {
            System.out.println("Directory changed to: " + ftpClient.getCurrentDirectory());
        } else {
            System.out.println("Failed to change directory");
        }
    }

    /**
     * Handle list command
     */
    private void handleList(String[] parts) {
        if (!checkAuthenticated()) return;

        String path = parts.length > 1 ? parts[1] : null;
        String listing = ftpClient.listDirectory(path);

        if (listing != null) {
            if (listing.trim().isEmpty()) {
                System.out.println("Directory is empty");
            } else {
                System.out.println("Directory listing:");
                System.out.println(listing);
            }
        } else {
            System.out.println("Failed to list directory");
        }
    }

    /**
     * Handle download command
     */
    private void handleDownload(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: get <remote-file> [local-file]");
            return;
        }

        String remoteFile = parts[1];
        String localFile = parts.length > 2 ? parts[2] : null;

        System.out.println("Downloading " + remoteFile + "...");

        if (ftpClient.downloadFile(remoteFile, localFile)) {
            System.out.println("Download completed");
        } else {
            System.out.println("Download failed");
        }
    }

    /**
     * Handle upload command
     */
    private void handleUpload(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: put <local-file> [remote-file]");
            return;
        }

        String localFile = parts[1];
        String remoteFile = parts.length > 2 ? parts[2] : null;

        System.out.println("Uploading " + localFile + "...");

        if (ftpClient.uploadFile(localFile, remoteFile)) {
            System.out.println("Upload completed");
        } else {
            System.out.println("Upload failed");
        }
    }

    /**
     * Handle delete command
     */
    private void handleDelete(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: delete <filename>");
            return;
        }

        String filename = parts[1];
        System.out.print("Delete " + filename + "? (y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            if (ftpClient.deleteFile(filename)) {
                System.out.println("File deleted: " + filename);
            } else {
                System.out.println("Delete failed");
            }
        } else {
            System.out.println("Delete cancelled");
        }
    }

    /**
     * Handle make directory command
     */
    private void handleMakeDirectory(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: mkdir <directory>");
            return;
        }

        String dirname = parts[1];

        if (ftpClient.createDirectory(dirname)) {
            System.out.println("Directory created: " + dirname);
        } else {
            System.out.println("Failed to create directory");
        }
    }

    /**
     * Handle remove directory command
     */
    private void handleRemoveDirectory(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: rmdir <directory>");
            return;
        }

        String dirname = parts[1];
        System.out.print("Remove directory " + dirname + "? (y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            if (ftpClient.removeDirectory(dirname)) {
                System.out.println("Directory removed: " + dirname);
            } else {
                System.out.println("Failed to remove directory");
            }
        } else {
            System.out.println("Remove cancelled");
        }
    }

    /**
     * Handle rename command
     */
    private void handleRename(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 3) {
            System.out.println("Usage: rename <old-name> <new-name>");
            return;
        }

        String oldName = parts[1];
        String newName = parts[2];

        if (ftpClient.renameFile(oldName, newName)) {
            System.out.println("File renamed from " + oldName + " to " + newName);
        } else {
            System.out.println("Rename failed");
        }
    }

    /**
     * Handle size command
     */
    private void handleSize(String[] parts) {
        if (!checkAuthenticated()) return;

        if (parts.length < 2) {
            System.out.println("Usage: size <filename>");
            return;
        }

        String filename = parts[1];
        long size = ftpClient.getFileSize(filename);

        if (size >= 0) {
            System.out.println("Size of " + filename + ": " + size + " bytes (" +
                    FileUtils.formatFileSize(size) + ")");
        } else {
            System.out.println("Failed to get file size");
        }
    }

    /**
     * Handle local change directory command
     */
    private void handleLocalChangeDirectory(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: lcd <directory>");
            return;
        }

        String directory = parts[1];
        ftpClient.setCurrentLocalDirectory(directory);
        System.out.println("Local directory: " + ftpClient.getCurrentLocalDirectory());
    }

    /**
     * Handle local list command
     */
    private void handleLocalList(String[] parts) {
        try {
            Path currentDir = ftpClient.getCurrentLocalDirectory();
            String pathToList = parts.length > 1 ? parts[1] : ".";
            Path listPath = currentDir.resolve(pathToList);

            if (Files.exists(listPath)) {
                if (Files.isDirectory(listPath)) {
                    System.out.println("Local directory: " + listPath);
                    Files.list(listPath).forEach(path -> {
                        try {
                            String type = Files.isDirectory(path) ? "<DIR>" : "FILE";
                            long size = Files.isDirectory(path) ? 0 : Files.size(path);
                            System.out.printf("%-10s %10s %s%n", type,
                                    FileUtils.formatFileSize(size), path.getFileName());
                        } catch (Exception e) {
                            System.out.println("ERROR  " + path.getFileName());
                        }
                    });
                } else {
                    System.out.println("FILE   " + FileUtils.formatFileSize(Files.size(listPath)) +
                            " " + listPath.getFileName());
                }
            } else {
                System.out.println("Path not found: " + listPath);
            }
        } catch (Exception e) {
            System.out.println("Error listing local directory: " + e.getMessage());
        }
    }

    /**
     * Handle local pwd command
     */
    private void handleLocalPwd() {
        System.out.println("Local directory: " + ftpClient.getCurrentLocalDirectory());
    }

    /**
     * Handle status command
     */
    private void handleStatus() {
        System.out.println("=== FTP Client Status ===");
        System.out.println("Connected: " + ftpClient.isConnected());
        System.out.println("Authenticated: " + ftpClient.isAuthenticated());

        if (ftpClient.isConnected()) {
            System.out.println("Server: " + ftpClient.getServerHost() + ":" + ftpClient.getServerPort());
            System.out.println("Remote directory: " + ftpClient.getCurrentRemoteDirectory());
        }

        System.out.println("Local directory: " + ftpClient.getCurrentLocalDirectory());
        System.out.println("========================");
    }

    /**
     * Handle system command
     */
    private void handleSystem() {
        if (!ftpClient.isConnected()) {
            System.out.println("Not connected");
            return;
        }

        String sysInfo = ftpClient.getSystemInfo();
        if (sysInfo != null) {
            System.out.println("Server system: " + sysInfo);
        } else {
            System.out.println("Failed to get system information");
        }
    }

    /**
     * Handle noop command
     */
    private void handleNoop() {
        if (!ftpClient.isConnected()) {
            System.out.println("Not connected");
            return;
        }

        if (ftpClient.noop()) {
            System.out.println("Server responded to NOOP");
        } else {
            System.out.println("NOOP failed");
        }
    }

    /**
     * Handle quit command
     */
    private void handleQuit() {
        if (ftpClient.isConnected()) {
            System.out.println("Disconnecting...");
            ftpClient.disconnect();
        }

        System.out.println("Goodbye!");
        running = false;
    }

    /**
     * Check if client is authenticated
     *
     * @return true if authenticated
     */
    private boolean checkAuthenticated() {
        if (!ftpClient.isConnected()) {
            System.out.println("Not connected. Use 'connect' command first.");
            return false;
        }

        if (!ftpClient.isAuthenticated()) {
            System.out.println("Not authenticated. Use 'login' command first.");
            return false;
        }

        return true;
    }

    /**
     * Get command prompt
     *
     * @return Prompt string
     */
    private String getPrompt() {
        if (ftpClient.isConnected()) {
            String host = ftpClient.getServerHost();
            String dir = ftpClient.getCurrentRemoteDirectory();
            return String.format("ftp:%s:%s> ", host, dir != null ? dir : "?");
        } else {
            return "ftp> ";
        }
    }

    /**
     * Print welcome message
     */
    private void printWelcome() {
        System.out.println();
        System.out.println("=".repeat(50));
        System.out.println("        FTP COMMAND-LINE CLIENT");
        System.out.println("=".repeat(50));
        System.out.println("University FTP Client v1.0");
        System.out.println("Type 'help' for available commands");
        System.out.println("Type 'quit' to exit");
        System.out.println();
    }

    /**
     * Print help information
     */
    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println();

        System.out.println("Connection commands:");
        System.out.println("  connect <host> [port]  - Connect to FTP server");
        System.out.println("  login [username]       - Login to server");
        System.out.println("  quit                   - Disconnect and exit");
        System.out.println("  status                 - Show connection status");
        System.out.println("  noop                   - Send keep-alive to server");
        System.out.println("  system                 - Get server system info");
        System.out.println();

        System.out.println("Remote directory commands:");
        System.out.println("  pwd                    - Show current remote directory");
        System.out.println("  cd <directory>         - Change remote directory");
        System.out.println("  ls [path]              - List remote directory");
        System.out.println("  mkdir <directory>      - Create remote directory");
        System.out.println("  rmdir <directory>      - Remove remote directory");
        System.out.println();

        System.out.println("File transfer commands:");
        System.out.println("  get <file> [local]     - Download file from server");
        System.out.println("  put <file> [remote]    - Upload file to server");
        System.out.println("  delete <file>          - Delete remote file");
        System.out.println("  rename <old> <new>     - Rename remote file");
        System.out.println("  size <file>            - Get remote file size");
        System.out.println();

        System.out.println("Local directory commands:");
        System.out.println("  lpwd                   - Show current local directory");
        System.out.println("  lcd <directory>        - Change local directory");
        System.out.println("  lls [path]             - List local directory");
        System.out.println();

        System.out.println("Other commands:");
        System.out.println("  help                   - Show this help");
        System.out.println();
    }

    /**
     * Main method to run the command-line client
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Handle command line arguments for quick connection
        if (args.length >= 2 && "connect".equals(args[0])) {
            CommandLineClient client = new CommandLineClient();

            String host = args[1];
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 21;

            System.out.println("Auto-connecting to " + host + ":" + port + "...");

            if (client.ftpClient.connect(host, port)) {
                System.out.println("Connected successfully!");
                client.start();
            } else {
                System.out.println("Connection failed");
                System.exit(1);
            }
        } else {
            // Normal interactive mode
            CommandLineClient client = new CommandLineClient();
            client.start();
        }
    }
}