package server;

import common.FTPConfig;
import utils.FileUtils;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Represents a client session with authentication and state information
 * Maintains per-client state including authentication status and current directory
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ClientSession {
    private final Socket clientSocket;
    private final FTPConfig config;
    private final LocalDateTime connectionTime;

    // Authentication state
    private boolean authenticated;
    private String username;
    private String tempUsername; // For USER command before PASS

    // Directory state
    private Path currentDirectory;
    private Path rootDirectory;

    // Transfer state
    private String renameFromPath; // For RNFR/RNTO commands

    /**
     * Constructor
     * @param clientSocket Client socket
     * @param config Server configuration
     */
    public ClientSession(Socket clientSocket, FTPConfig config) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.connectionTime = LocalDateTime.now();
        this.authenticated = false;

        // Initialize directory paths
        initializeDirectories();
    }

    /**
     * Initialize root and current directories
     */
    private void initializeDirectories() {
        try {
            this.rootDirectory = Paths.get(config.getServerRootDirectory()).normalize().toAbsolutePath();
            this.currentDirectory = rootDirectory;

            // Ensure root directory exists
            FileUtils.ensureDirectoryExists(rootDirectory);

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize server directories", e);
        }
    }

    /**
     * Set temporary username (for USER command)
     * @param username Username
     */
    public void setTempUsername(String username) {
        this.tempUsername = username;
        this.authenticated = false;
    }

    /**
     * Authenticate user with password
     * @param password Password
     * @return true if authentication successful
     */
    public boolean authenticate(String password) {
        // Simple authentication logic - in real implementation, use proper user database
        if (tempUsername != null && isValidCredentials(tempUsername, password)) {
            this.username = tempUsername;
            this.tempUsername = null;
            this.authenticated = true;
            return true;
        }

        this.tempUsername = null;
        this.authenticated = false;
        return false;
    }

    /**
     * Validate user credentials
     * @param username Username
     * @param password Password
     * @return true if credentials are valid
     */
    private boolean isValidCredentials(String username, String password) {
        // Simple hardcoded authentication for demo purposes
        // In a real implementation, this would check against a database or LDAP
        return ("admin".equals(username) && "admin123".equals(password)) ||
                ("user".equals(username) && "user123".equals(password)) ||
                ("test".equals(username) && "test".equals(password));
    }

    /**
     * Change current directory
     * @param path New directory path (can be relative or absolute)
     * @return true if directory change successful
     */
    public boolean changeDirectory(String path) {
        try {
            Path newPath;

            if (path.equals("/") || path.equals("\\")) {
                // Root directory
                newPath = rootDirectory;
            } else if (path.equals("..")) {
                // Parent directory
                newPath = currentDirectory.getParent();
                if (newPath == null || !newPath.startsWith(rootDirectory)) {
                    newPath = rootDirectory;
                }
            } else if (path.equals(".")) {
                // Current directory
                newPath = currentDirectory;
            } else {
                // Resolve path relative to current directory
                newPath = FileUtils.resolvePath(currentDirectory.toString(), path);
            }

            // Verify path is safe and exists
            if (FileUtils.isSafePath(rootDirectory.toString(), newPath.toString()) &&
                    java.nio.file.Files.exists(newPath) &&
                    java.nio.file.Files.isDirectory(newPath)) {

                currentDirectory = newPath;
                return true;
            }

        } catch (IOException e) {
            // Path resolution failed
        }

        return false;
    }

    /**
     * Get current directory relative to root
     * @return Current directory path
     */
    public String getCurrentDirectoryPath() {
        try {
            Path relativePath = rootDirectory.relativize(currentDirectory);
            String pathStr = "/" + relativePath.toString().replace('\\', '/');
            return pathStr.equals("/.") ? "/" : pathStr;
        } catch (Exception e) {
            return "/";
        }
    }

    /**
     * Resolve file path relative to current directory
     * @param filename Filename or relative path
     * @return Resolved absolute path
     * @throws IOException if path resolution fails
     */
    public Path resolveFilePath(String filename) throws IOException {
        return FileUtils.resolvePath(currentDirectory.toString(), filename);
    }

    /**
     * Check if user is authenticated
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Get authenticated username
     * @return Username or null if not authenticated
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get temporary username (set by USER command)
     * @return Temporary username
     */
    public String getTempUsername() {
        return tempUsername;
    }

    /**
     * Get current directory path object
     * @return Current directory path
     */
    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * Get root directory path object
     * @return Root directory path
     */
    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Get client socket
     * @return Client socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Get connection time
     * @return Connection establishment time
     */
    public LocalDateTime getConnectionTime() {
        return connectionTime;
    }

    /**
     * Set rename from path (for RNFR command)
     * @param path Path to rename from
     */
    public void setRenameFromPath(String path) {
        this.renameFromPath = path;
    }

    /**
     * Get rename from path
     * @return Path to rename from
     */
    public String getRenameFromPath() {
        return renameFromPath;
    }

    /**
     * Clear rename from path
     */
    public void clearRenameFromPath() {
        this.renameFromPath = null;
    }

    /**
     * Get session information as string
     * @return Session information
     */
    public String getSessionInfo() {
        return String.format("User: %s, Connected: %s, Directory: %s, Authenticated: %s",
                username != null ? username : "Anonymous",
                connectionTime,
                getCurrentDirectoryPath(),
                authenticated);
    }

    /**
     * Logout the user
     */
    public void logout() {
        this.authenticated = false;
        this.username = null;
        this.tempUsername = null;
        this.renameFromPath = null;
        // Reset to root directory
        this.currentDirectory = rootDirectory;
    }
}