package client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch FTP client for scripted operations
 * Allows execution of FTP commands from script files
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class BatchClient {
    private static final Logger logger = LogManager.getLogger("client");

    private final FTPClient ftpClient;
    private final List<String> executionLog;
    private boolean stopOnError;

    /**
     * Constructor
     */
    public BatchClient() {
        this.ftpClient = new FTPClient();
        this.executionLog = new ArrayList<>();
        this.stopOnError = true;
    }

    /**
     * Execute commands from script file
     *
     * @param scriptPath Path to script file
     * @return true if execution completed successfully
     */
    public boolean executeScript(String scriptPath) {
        return executeScript(scriptPath, true);
    }

    /**
     * Execute commands from script file
     *
     * @param scriptPath  Path to script file
     * @param stopOnError Whether to stop execution on first error
     * @return true if execution completed successfully
     */
    public boolean executeScript(String scriptPath, boolean stopOnError) {
        this.stopOnError = stopOnError;
        executionLog.clear();

        Path path = Paths.get(scriptPath);
        if (!Files.exists(path)) {
            logError("Script file not found: " + scriptPath);
            return false;
        }

        logInfo("Executing script: " + scriptPath);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;
            boolean success = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                logInfo("Line " + lineNumber + ": " + line);

                if (!executeCommand(line)) {
                    success = false;
                    if (stopOnError) {
                        logError("Execution stopped at line " + lineNumber + " due to error");
                        break;
                    }
                }
            }

            return success;

        } catch (IOException e) {
            logError("Error reading script file: " + e.getMessage());
            return false;
        } finally {
            // Cleanup
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        }
    }

    /**
     * Execute a single command
     *
     * @param command Command string
     * @return true if command executed successfully
     */
    private boolean executeCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return true;
        }

        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "connect":
                    return handleConnect(parts);
                case "login":
                    return handleLogin(parts);
                case "cd":
                case "cwd":
                    return handleChangeDirectory(parts);
                case "get":
                case "download":
                    return handleDownload(parts);
                case "put":
                case "upload":
                    return handleUpload(parts);
                case "delete":
                case "del":
                    return handleDelete(parts);
                case "mkdir":
                    return handleMakeDirectory(parts);
                case "rmdir":
                    return handleRemoveDirectory(parts);
                case "rename":
                    return handleRename(parts);
                case "list":
                case "ls":
                    return handleList(parts);
                case "pwd":
                    return handlePwd();
                case "lcd":
                    return handleLocalChangeDirectory(parts);
                case "sleep":
                    return handleSleep(parts);
                case "echo":
                    return handleEcho(parts);
                case "quit":
                case "disconnect":
                    return handleQuit();
                default:
                    logError("Unknown command: " + cmd);
                    return false;
            }
        } catch (Exception e) {
            logError("Error executing command '" + command + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle connect command
     */
    private boolean handleConnect(String[] parts) {
        if (parts.length < 2) {
            logError("connect: hostname required");
            return false;
        }

        String host = parts[1];
        int port = parts.length > 2 ? Integer.parseInt(parts[2]) : 21;

        logInfo("Connecting to " + host + ":" + port);

        if (ftpClient.connect(host, port)) {
            logInfo("Connected successfully");
            return true;
        } else {
            logError("Connection failed");
            return false;
        }
    }

    /**
     * Handle login command
     */
    private boolean handleLogin(String[] parts) {
        if (parts.length < 3) {
            logError("login: username and password required");
            return false;
        }

        String username = parts[1];
        String password = parts[2];

        logInfo("Logging in as " + username);

        if (ftpClient.login(username, password)) {
            logInfo("Login successful");
            return true;
        } else {
            logError("Login failed");
            return false;
        }
    }

    /**
     * Handle change directory command
     */
    private boolean handleChangeDirectory(String[] parts) {
        if (parts.length < 2) {
            logError("cd: directory required");
            return false;
        }

        String directory = parts[1];

        if (ftpClient.changeDirectory(directory)) {
            logInfo("Changed directory to: " + directory);
            return true;
        } else {
            logError("Failed to change directory");
            return false;
        }
    }

    /**
     * Handle download command
     */
    private boolean handleDownload(String[] parts) {
        if (parts.length < 2) {
            logError("get: remote filename required");
            return false;
        }

        String remoteFile = parts[1];
        String localFile = parts.length > 2 ? parts[2] : null;

        logInfo("Downloading " + remoteFile);

        if (ftpClient.downloadFile(remoteFile, localFile)) {
            logInfo("Download completed");
            return true;
        } else {
            logError("Download failed");
            return false;
        }
    }

    /**
     * Handle upload command
     */
    private boolean handleUpload(String[] parts) {
        if (parts.length < 2) {
            logError("put: local filename required");
            return false;
        }

        String localFile = parts[1];
        String remoteFile = parts.length > 2 ? parts[2] : null;

        logInfo("Uploading " + localFile);

        if (ftpClient.uploadFile(localFile, remoteFile)) {
            logInfo("Upload completed");
            return true;
        } else {
            logError("Upload failed");
            return false;
        }
    }

    /**
     * Handle delete command
     */
    private boolean handleDelete(String[] parts) {
        if (parts.length < 2) {
            logError("delete: filename required");
            return false;
        }

        String filename = parts[1];

        if (ftpClient.deleteFile(filename)) {
            logInfo("Deleted: " + filename);
            return true;
        } else {
            logError("Delete failed");
            return false;
        }
    }

    /**
     * Handle make directory command
     */
    private boolean handleMakeDirectory(String[] parts) {
        if (parts.length < 2) {
            logError("mkdir: directory name required");
            return false;
        }

        String dirname = parts[1];

        if (ftpClient.createDirectory(dirname)) {
            logInfo("Created directory: " + dirname);
            return true;
        } else {
            logError("Failed to create directory");
            return false;
        }
    }

    /**
     * Handle remove directory command
     */
    private boolean handleRemoveDirectory(String[] parts) {
        if (parts.length < 2) {
            logError("rmdir: directory name required");
            return false;
        }

        String dirname = parts[1];

        if (ftpClient.removeDirectory(dirname)) {
            logInfo("Removed directory: " + dirname);
            return true;
        } else {
            logError("Failed to remove directory");
            return false;
        }
    }

    /**
     * Handle rename command
     */
    private boolean handleRename(String[] parts) {
        if (parts.length < 3) {
            logError("rename: old and new names required");
            return false;
        }

        String oldName = parts[1];
        String newName = parts[2];

        if (ftpClient.renameFile(oldName, newName)) {
            logInfo("Renamed " + oldName + " to " + newName);
            return true;
        } else {
            logError("Rename failed");
            return false;
        }
    }

    /**
     * Handle list command
     */
    private boolean handleList(String[] parts) {
        String path = parts.length > 1 ? parts[1] : null;
        String listing = ftpClient.listDirectory(path);

        if (listing != null) {
            logInfo("Directory listing:");
            String[] lines = listing.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    logInfo("  " + line);
                }
            }
            return true;
        } else {
            logError("Failed to list directory");
            return false;
        }
    }

    /**
     * Handle pwd command
     */
    private boolean handlePwd() {
        String directory = ftpClient.getCurrentDirectory();
        if (directory != null) {
            logInfo("Current directory: " + directory);
            return true;
        } else {
            logError("Failed to get current directory");
            return false;
        }
    }

    /**
     * Handle local change directory command
     */
    private boolean handleLocalChangeDirectory(String[] parts) {
        if (parts.length < 2) {
            logError("lcd: directory required");
            return false;
        }

        String directory = parts[1];
        ftpClient.setCurrentLocalDirectory(directory);
        logInfo("Local directory: " + ftpClient.getCurrentLocalDirectory());
        return true;
    }

    /**
     * Handle sleep command
     */
    private boolean handleSleep(String[] parts) {
        if (parts.length < 2) {
            logError("sleep: duration in seconds required");
            return false;
        }

        try {
            int seconds = Integer.parseInt(parts[1]);
            logInfo("Sleeping for " + seconds + " seconds");
            Thread.sleep(seconds * 1000L);
            return true;
        } catch (NumberFormatException e) {
            logError("sleep: invalid duration");
            return false;
        } catch (InterruptedException e) {
            logError("sleep: interrupted");
            return false;
        }
    }

    /**
     * Handle echo command
     */
    private boolean handleEcho(String[] parts) {
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) message.append(" ");
            message.append(parts[i]);
        }
        logInfo(message.toString());
        return true;
    }

    /**
     * Handle quit command
     */
    private boolean handleQuit() {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
            logInfo("Disconnected");
        }
        return true;
    }

    /**
     * Log information message
     */
    private void logInfo(String message) {
        String logEntry = "[INFO] " + message;
        System.out.println(logEntry);
        executionLog.add(logEntry);
        logger.info(message);
    }

    /**
     * Log error message
     */
    private void logError(String message) {
        String logEntry = "[ERROR] " + message;
        System.err.println(logEntry);
        executionLog.add(logEntry);
        logger.error(message);
    }

    /**
     * Get execution log
     *
     * @return List of log entries
     */
    public List<String> getExecutionLog() {
        return new ArrayList<>(executionLog);
    }

    /**
     * Save execution log to file
     *
     * @param logPath Path to save log file
     */
    public void saveExecutionLog(String logPath) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(logPath)))) {
            for (String logEntry : executionLog) {
                writer.println(logEntry);
            }
            System.out.println("Execution log saved to: " + logPath);
        } catch (IOException e) {
            System.err.println("Failed to save log: " + e.getMessage());
        }
    }

    /**
     * Main method for batch execution
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: BatchClient <script-file> [options]");
            System.out.println("Options:");
            System.out.println("  --continue-on-error  Continue execution even if commands fail");
            System.out.println("  --log <file>         Save execution log to file");
            System.exit(1);
        }

        String scriptFile = args[0];
        boolean stopOnError = true;
        String logFile = null;

        // Parse command line options
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--continue-on-error":
                    stopOnError = false;
                    break;
                case "--log":
                    if (i + 1 < args.length) {
                        logFile = args[++i];
                    }
                    break;
            }
        }

        BatchClient batchClient = new BatchClient();
        boolean success = batchClient.executeScript(scriptFile, stopOnError);

        if (logFile != null) {
            batchClient.saveExecutionLog(logFile);
        }

        System.out.println("Script execution " + (success ? "completed successfully" : "failed"));
        System.exit(success ? 0 : 1);
    }
}