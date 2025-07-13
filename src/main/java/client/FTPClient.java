package client;

import common.*;
import utils.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FTP Client implementation
 * Provides programmatic interface for FTP operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPClient {
    private static final Logger logger = LogManager.getLogger("client");

    private final FTPConfig config;
    private Socket controlSocket;
    private BufferedReader controlInput;
    private PrintWriter controlOutput;
    private final AtomicBoolean connected;
    private final AtomicBoolean authenticated;

    private String serverHost;
    private int serverPort;
    private String currentRemoteDirectory;
    private Path currentLocalDirectory;

    /**
     * Constructor with default configuration
     */
    public FTPClient() {
        this(FTPConfig.getInstance());
    }

    /**
     * Constructor with custom configuration
     * @param config FTP configuration
     */
    public FTPClient(FTPConfig config) {
        this.config = config;
        this.connected = new AtomicBoolean(false);
        this.authenticated = new AtomicBoolean(false);
        this.currentRemoteDirectory = "/";

        // Initialize local directory
        try {
            this.currentLocalDirectory = Paths.get(config.getClientRootDirectory()).toAbsolutePath();
            Files.createDirectories(currentLocalDirectory);
        } catch (IOException e) {
            this.currentLocalDirectory = Paths.get(".").toAbsolutePath();
            logger.warn("Could not create client root directory, using current directory");
        }

        logger.info("FTP Client initialized");
    }

    /**
     * Connect to FTP server
     * @param host Server hostname or IP
     * @param port Server port
     * @return true if connection successful
     */
    public boolean connect(String host, int port) {
        if (connected.get()) {
            logger.warn("Already connected to server");
            return false;
        }

        try {
            logger.info("Connecting to {}:{}", host, port);

            // Create socket connection
            controlSocket = NetworkUtils.createClientSocket(host, port, config.getClientTimeout());
            NetworkUtils.configureSocket(controlSocket, config.getBufferSize());

            // Initialize streams
            controlInput = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlOutput = new PrintWriter(controlSocket.getOutputStream(), true);

            // Read welcome message
            String response = readResponse();
            if (!isSuccessResponse(response)) {
                disconnect();
                return false;
            }

            this.serverHost = host;
            this.serverPort = port;
            connected.set(true);

            logger.info("Connected to FTP server: {}", response);
            return true;

        } catch (IOException e) {
            logger.error("Failed to connect to {}:{} - {}", host, port, e.getMessage());
            cleanup();
            return false;
        }
    }

    /**
     * Connect to FTP server with default port
     * @param host Server hostname or IP
     * @return true if connection successful
     */
    public boolean connect(String host) {
        return connect(host, config.getServerPort());
    }

    /**
     * Authenticate with username and password
     * @param username Username
     * @param password Password
     * @return true if authentication successful
     */
    public boolean login(String username, String password) {
        if (!connected.get()) {
            logger.error("Not connected to server");
            return false;
        }

        if (authenticated.get()) {
            logger.warn("Already authenticated");
            return true;
        }

        try {
            // Send USER command
            sendCommand("USER " + username);
            String response = readResponse();

            if (!isIntermediateResponse(response)) {
                logger.error("USER command failed: {}", response);
                return false;
            }

            // Send PASS command
            sendCommand("PASS " + (password != null ? password : ""));
            response = readResponse();

            if (isSuccessResponse(response)) {
                authenticated.set(true);
                logger.info("Authentication successful for user: {}", username);

                // Get initial directory
                getCurrentDirectory();
                return true;
            } else {
                logger.error("Authentication failed: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error during authentication: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current remote directory
     * @return Current directory path or null if error
     */
    public String getCurrentDirectory() {
        if (!isConnectedAndAuthenticated()) {
            return null;
        }

        try {
            sendCommand("PWD");
            String response = readResponse();

            if (isSuccessResponse(response)) {
                // Extract directory from response (format: "257 "/path" is current directory")
                int start = response.indexOf('"');
                int end = response.indexOf('"', start + 1);
                if (start != -1 && end != -1) {
                    currentRemoteDirectory = response.substring(start + 1, end);
                    return currentRemoteDirectory;
                }
            }

            logger.error("Failed to get current directory: {}", response);
            return null;

        } catch (IOException e) {
            logger.error("Error getting current directory: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Change remote directory
     * @param directory Directory path
     * @return true if successful
     */
    public boolean changeDirectory(String directory) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            sendCommand("CWD " + directory);
            String response = readResponse();

            if (isSuccessResponse(response)) {
                logger.info("Directory changed to: {}", directory);
                getCurrentDirectory(); // Update current directory
                return true;
            } else {
                logger.error("Failed to change directory: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error changing directory: {}", e.getMessage());
            return false;
        }
    }

    /**
     * List directory contents
     * @param path Directory path (null for current directory)
     * @return Directory listing or null if error
     */
    public String listDirectory(String path) {
        if (!isConnectedAndAuthenticated()) {
            return null;
        }

        try {
            String command = path != null ? "LIST " + path : "LIST";
            sendCommand(command);

            // Read response and directory listing
            StringBuilder listing = new StringBuilder();
            String line;

            // Read initial response
            String response = readResponse();
            if (!isSuccessResponse(response)) {
                logger.error("LIST command failed: {}", response);
                return null;
            }

            // Read directory data (sent via control connection in our simplified implementation)
            while ((line = controlInput.readLine()) != null) {
                if (line.matches("^\\d{3}.*")) {
                    // This is the closing response
                    if (isSuccessResponse(line)) {
                        logger.debug("Directory listing completed: {}", line);
                    }
                    break;
                } else {
                    listing.append(line).append("\n");
                }
            }

            return listing.toString();

        } catch (IOException e) {
            logger.error("Error listing directory: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Download file from server
     * @param remoteFilename Remote filename
     * @param localFilename Local filename (null to use same name)
     * @return true if successful
     */
    public boolean downloadFile(String remoteFilename, String localFilename) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            // Determine local file path
            Path localPath = localFilename != null ?
                    currentLocalDirectory.resolve(localFilename) :
                    currentLocalDirectory.resolve(remoteFilename);

            logger.info("Downloading {} to {}", remoteFilename, localPath);

            // Send RETR command
            sendCommand("RETR " + remoteFilename);
            String response = readResponse();

            if (!isSuccessResponse(response)) {
                logger.error("Download failed: {}", response);
                return false;
            }

            // Receive file data via control connection (simplified implementation)
            try (FileOutputStream fileOut = new FileOutputStream(localPath.toFile());
                 InputStream socketIn = controlSocket.getInputStream()) {

                long bytesReceived = NetworkUtils.transferData(socketIn, fileOut,
                        config.getBufferSize(), null, -1);

                // Read completion response
                response = readResponse();
                if (isSuccessResponse(response)) {
                    logger.info("Download completed: {} bytes received", bytesReceived);
                    return true;
                } else {
                    logger.error("Download completion error: {}", response);
                    Files.deleteIfExists(localPath); // Clean up partial file
                    return false;
                }
            }

        } catch (IOException e) {
            logger.error("Error downloading file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload file to server
     * @param localFilename Local filename
     * @param remoteFilename Remote filename (null to use same name)
     * @return true if successful
     */
    public boolean uploadFile(String localFilename, String remoteFilename) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            Path localPath = currentLocalDirectory.resolve(localFilename);

            if (!Files.exists(localPath)) {
                logger.error("Local file not found: {}", localPath);
                return false;
            }

            if (Files.isDirectory(localPath)) {
                logger.error("Cannot upload directory: {}", localPath);
                return false;
            }

            String targetName = remoteFilename != null ? remoteFilename : localFilename;
            logger.info("Uploading {} to {}", localPath, targetName);

            // Send STOR command
            sendCommand("STOR " + targetName);
            String response = readResponse();

            if (!isSuccessResponse(response)) {
                logger.error("Upload failed: {}", response);
                return false;
            }

            // Send file data via control connection (simplified implementation)
            try (FileInputStream fileIn = new FileInputStream(localPath.toFile());
                 OutputStream socketOut = controlSocket.getOutputStream()) {

                long bytesSent = NetworkUtils.transferData(fileIn, socketOut,
                        config.getBufferSize(), null, Files.size(localPath));

                socketOut.flush();

                // Read completion response
                response = readResponse();
                if (isSuccessResponse(response)) {
                    logger.info("Upload completed: {} bytes sent", bytesSent);
                    return true;
                } else {
                    logger.error("Upload completion error: {}", response);
                    return false;
                }
            }

        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Delete file on server
     * @param filename Filename to delete
     * @return true if successful
     */
    public boolean deleteFile(String filename) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            sendCommand("DELE " + filename);
            String response = readResponse();

            if (isSuccessResponse(response)) {
                logger.info("File deleted: {}", filename);
                return true;
            } else {
                logger.error("Delete failed: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create directory on server
     * @param dirname Directory name
     * @return true if successful
     */
    public boolean createDirectory(String dirname) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            sendCommand("MKD " + dirname);
            String response = readResponse();

            if (isSuccessResponse(response)) {
                logger.info("Directory created: {}", dirname);
                return true;
            } else {
                logger.error("Create directory failed: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error creating directory: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Remove directory on server
     * @param dirname Directory name
     * @return true if successful
     */
    public boolean removeDirectory(String dirname) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            sendCommand("RMD " + dirname);
            String response = readResponse();

            if (isSuccessResponse(response)) {
                logger.info("Directory removed: {}", dirname);
                return true;
            } else {
                logger.error("Remove directory failed: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error removing directory: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Rename file on server
     * @param oldName Current filename
     * @param newName New filename
     * @return true if successful
     */
    public boolean renameFile(String oldName, String newName) {
        if (!isConnectedAndAuthenticated()) {
            return false;
        }

        try {
            // Send RNFR command
            sendCommand("RNFR " + oldName);
            String response = readResponse();

            if (!isIntermediateResponse(response)) {
                logger.error("RNFR command failed: {}", response);
                return false;
            }

            // Send RNTO command
            sendCommand("RNTO " + newName);
            response = readResponse();

            if (isSuccessResponse(response)) {
                logger.info("File renamed from {} to {}", oldName, newName);
                return true;
            } else {
                logger.error("Rename failed: {}", response);
                return false;
            }

        } catch (IOException e) {
            logger.error("Error renaming file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get file size
     * @param filename Filename
     * @return File size or -1 if error
     */
    public long getFileSize(String filename) {
        if (!isConnectedAndAuthenticated()) {
            return -1;
        }

        try {
            sendCommand("SIZE " + filename);
            String response = readResponse();

            if (isSuccessResponse(response)) {
                // Extract size from response
                String[] parts = response.split("\\s+", 2);
                if (parts.length > 1) {
                    try {
                        return Long.parseLong(parts[1]);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid size response: {}", response);
                    }
                }
            } else {
                logger.error("SIZE command failed: {}", response);
            }

            return -1;

        } catch (IOException e) {
            logger.error("Error getting file size: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Send NOOP command (keep-alive)
     * @return true if successful
     */
    public boolean noop() {
        if (!connected.get()) {
            return false;
        }

        try {
            sendCommand("NOOP");
            String response = readResponse();
            return isSuccessResponse(response);

        } catch (IOException e) {
            logger.error("Error sending NOOP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get system information
     * @return System info or null if error
     */
    public String getSystemInfo() {
        if (!connected.get()) {
            return null;
        }

        try {
            sendCommand("SYST");
            String response = readResponse();

            if (isSuccessResponse(response)) {
                // Extract system info from response
                int spaceIndex = response.indexOf(' ');
                if (spaceIndex != -1 && spaceIndex < response.length() - 1) {
                    return response.substring(spaceIndex + 1);
                }
            }

            return null;

        } catch (IOException e) {
            logger.error("Error getting system info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (!connected.get()) {
            return;
        }

        try {
            if (authenticated.get()) {
                sendCommand("QUIT");
                String response = readResponse();
                logger.info("Server goodbye: {}", response);
            }
        } catch (IOException e) {
            logger.debug("Error sending QUIT command: {}", e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Send command to server
     * @param command Command string
     * @throws IOException if sending fails
     */
    private void sendCommand(String command) throws IOException {
        logger.debug("Sending command: {}", command);
        controlOutput.println(command);

        if (controlOutput.checkError()) {
            throw new IOException("Error sending command to server");
        }
    }

    /**
     * Read response from server
     * @return Response string
     * @throws IOException if reading fails
     */
    private String readResponse() throws IOException {
        String response = controlInput.readLine();
        if (response == null) {
            throw new IOException("Server closed connection");
        }

        logger.debug("Received response: {}", response);
        return response;
    }

    /**
     * Check if response indicates success (2xx codes)
     * @param response Response string
     * @return true if success response
     */
    private boolean isSuccessResponse(String response) {
        if (response == null || response.length() < 3) {
            return false;
        }

        try {
            int code = Integer.parseInt(response.substring(0, 3));
            return code >= 200 && code < 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if response is intermediate (3xx codes)
     * @param response Response string
     * @return true if intermediate response
     */
    private boolean isIntermediateResponse(String response) {
        if (response == null || response.length() < 3) {
            return false;
        }

        try {
            int code = Integer.parseInt(response.substring(0, 3));
            return code >= 300 && code < 400;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if client is connected and authenticated
     * @return true if connected and authenticated
     */
    private boolean isConnectedAndAuthenticated() {
        if (!connected.get()) {
            logger.error("Not connected to server");
            return false;
        }

        if (!authenticated.get()) {
            logger.error("Not authenticated");
            return false;
        }

        return true;
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        connected.set(false);
        authenticated.set(false);

        if (controlInput != null) {
            try {
                controlInput.close();
            } catch (IOException e) {
                logger.debug("Error closing input stream: {}", e.getMessage());
            }
        }

        if (controlOutput != null) {
            controlOutput.close();
        }

        NetworkUtils.closeSocket(controlSocket);

        logger.info("FTP client disconnected");
    }

    // Getter methods

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isAuthenticated() {
        return authenticated.get();
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getCurrentRemoteDirectory() {
        return currentRemoteDirectory;
    }

    public Path getCurrentLocalDirectory() {
        return currentLocalDirectory;
    }

    public void setCurrentLocalDirectory(String directory) {
        try {
            Path newPath = Paths.get(directory).toAbsolutePath();
            if (Files.exists(newPath) && Files.isDirectory(newPath)) {
                this.currentLocalDirectory = newPath;
                logger.info("Local directory changed to: {}", newPath);
            } else {
                logger.error("Invalid local directory: {}", directory);
            }
        } catch (Exception e) {
            logger.error("Error setting local directory: {}", e.getMessage());
        }
    }
}