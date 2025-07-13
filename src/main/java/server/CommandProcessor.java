package server;

import common.*;
import utils.FileUtils;
import utils.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Processes FTP commands from clients
 * Implements the core FTP command functionality
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class CommandProcessor {
    private static final Logger logger = LogManager.getLogger("server");

    private final ClientSession session;

    /**
     * Constructor
     *
     * @param session Client session
     */
    public CommandProcessor(ClientSession session) {
        this.session = session;
    }

    /**
     * Process an FTP command
     *
     * @param message       FTP command message
     * @param clientHandler Client handler for sending responses
     * @throws FTPException if command processing fails
     */
    public void processCommand(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        FTPCommand command = message.getCommand();

        logger.debug("Processing command {} for user {}", command, session.getUsername());

        switch (command) {
            case USER:
                handleUser(message, clientHandler);
                break;
            case PASS:
                handlePass(message, clientHandler);
                break;
            case PWD:
                handlePwd(clientHandler);
                break;
            case CWD:
                handleCwd(message, clientHandler);
                break;
            case LIST:
                handleList(message, clientHandler);
                break;
            case RETR:
                handleRetr(message, clientHandler);
                break;
            case STOR:
                handleStor(message, clientHandler);
                break;
            case DELE:
                handleDele(message, clientHandler);
                break;
            case MKD:
                handleMkd(message, clientHandler);
                break;
            case RMD:
                handleRmd(message, clientHandler);
                break;
            case RNFR:
                handleRnfr(message, clientHandler);
                break;
            case RNTO:
                handleRnto(message, clientHandler);
                break;
            case SIZE:
                handleSize(message, clientHandler);
                break;
            case MDTM:
                handleMdtm(message, clientHandler);
                break;
            case SYST:
                handleSyst(clientHandler);
                break;
            case NOOP:
                handleNoop(clientHandler);
                break;
            case QUIT:
                handleQuit(clientHandler);
                break;
            case FEAT:
                handleFeat(clientHandler);
                break;
            case MLST:
                handleMlst(message, clientHandler);
                break;
            case MLSD:
                handleMlsd(message, clientHandler);
                break;
            case OPTS:
                handleOpts(message, clientHandler);
                break;
            case STAT:
                handleStat(message, clientHandler);
                break;
            default:
                clientHandler.sendResponse(FTPResponse.COMMAND_NOT_IMPLEMENTED,
                        "Command " + command + " not implemented");
        }
    }

    /**
     * Handle USER command
     */
    private void handleUser(FTPMessage message, ClientHandler clientHandler) {
        String username = message.getFirstParameter();

        if (username == null || username.trim().isEmpty()) {
            clientHandler.sendResponse(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Username required");
            return;
        }

        session.setTempUsername(username.trim());
        clientHandler.sendResponse(FTPResponse.USERNAME_OK, "User " + username + " OK, password required");

        logger.info("User {} attempting login from {}", username,
                session.getClientSocket().getRemoteSocketAddress());
    }

    /**
     * Handle PASS command
     */
    private void handlePass(FTPMessage message, ClientHandler clientHandler) throws AuthenticationException {
        String password = message.getFirstParameter();

        if (session.getTempUsername() == null) {
            throw new AuthenticationException("Send USER command first");
        }

        if (password == null) {
            password = ""; // Allow empty passwords
        }

        if (session.authenticate(password)) {
            clientHandler.sendResponse(FTPResponse.USER_LOGGED_IN,
                    "User " + session.getUsername() + " logged in successfully");
            logger.info("User {} logged in successfully from {}",
                    session.getUsername(), session.getClientSocket().getRemoteSocketAddress());
        } else {
            session.logout();
            throw new AuthenticationException("Login incorrect");
        }
    }

    /**
     * Handle PWD command
     */
    private void handlePwd(ClientHandler clientHandler) {
        String currentPath = session.getCurrentDirectoryPath();
        clientHandler.sendResponse(FTPResponse.PATHNAME_CREATED, "\"" + currentPath + "\" is current directory");
    }

    /**
     * Handle CWD command
     */
    private void handleCwd(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String path = message.getFirstParameter();

        if (path == null || path.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Directory path required");
        }

        if (session.changeDirectory(path)) {
            clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK,
                    "Directory changed to " + session.getCurrentDirectoryPath());
        } else {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Directory not found or access denied");
        }
    }

    /**
     * Handle LIST command
     */
    private void handleList(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        try {
            Path listPath = session.getCurrentDirectory();

            // If parameter provided, resolve it
            if (message.hasParameters()) {
                String path = message.getFirstParameter();
                listPath = session.resolveFilePath(path);
            }

            if (!Files.exists(listPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Path not found");
            }

            // Send list via data connection
            sendListData(listPath, clientHandler);

        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error listing directory: " + e.getMessage());
        }
    }

    /**
     * Send directory listing via data connection
     */
    private void sendListData(Path path, ClientHandler clientHandler) throws IOException, FTPException {
        // For now, send via control connection (simplified implementation)
        // In a full implementation, this would use a separate data connection

        clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK, "Here comes the directory listing");

        if (Files.isDirectory(path)) {
            List<FileInfo> files = FileUtils.listDirectory(path);

            for (FileInfo file : files) {
                // Send each file info line
                try {
                    clientHandler.getClientSocket().getOutputStream().write(
                            (file.toListFormat() + "\r\n").getBytes());
                } catch (IOException e) {
                    throw new FTPException(FTPResponse.CONNECTION_CLOSED, "Error sending directory listing");
                }
            }
        } else {
            // Single file
            FileInfo file = FileUtils.getFileInfo(path);
            if (file != null) {
                clientHandler.getClientSocket().getOutputStream().write(
                        (file.toListFormat() + "\r\n").getBytes());
            }
        }

        clientHandler.sendResponse(FTPResponse.CLOSING_DATA_CONNECTION, "Directory send OK");
    }

    /**
     * Handle RETR command (download)
     */
    private void handleRetr(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            if (Files.isDirectory(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot download directory");
            }

            // Send file via control connection (simplified)
            sendFileData(filePath, clientHandler);

        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error retrieving file: " + e.getMessage());
        }
    }

    /**
     * Send file data via connection
     */
    private void sendFileData(Path filePath, ClientHandler clientHandler) throws IOException, FTPException {
        clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK,
                "Opening data connection for " + filePath.getFileName());

        try (InputStream fileInput = Files.newInputStream(filePath);
             OutputStream output = clientHandler.getClientSocket().getOutputStream()) {

            long bytesTransferred = NetworkUtils.transferData(fileInput, output, 8192, null, -1);

            logger.info("File {} sent to client, {} bytes transferred",
                    filePath.getFileName(), bytesTransferred);

            clientHandler.sendResponse(FTPResponse.CLOSING_DATA_CONNECTION, "Transfer complete");

        } catch (IOException e) {
            throw new FTPException(FTPResponse.CONNECTION_CLOSED, "Error transferring file");
        }
    }

    /**
     * Handle STOR command (upload)
     */
    private void handleStor(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        if (!FileUtils.isValidFilename(filename)) {
            throw new FTPException(FTPResponse.FILE_NAME_NOT_ALLOWED, "Invalid filename");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            // Check if file already exists
            if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot overwrite directory");
            }

            // Receive file data
            receiveFileData(filePath, clientHandler);

        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error storing file: " + e.getMessage());
        }
    }

    /**
     * Receive file data via connection
     */
    private void receiveFileData(Path filePath, ClientHandler clientHandler) throws IOException, FTPException {
        clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK,
                "Ready to receive file " + filePath.getFileName());

        try (InputStream input = clientHandler.getClientSocket().getInputStream();
             OutputStream fileOutput = Files.newOutputStream(filePath)) {

            long bytesTransferred = NetworkUtils.transferData(input, fileOutput, 8192, null, -1);

            logger.info("File {} received from client, {} bytes transferred",
                    filePath.getFileName(), bytesTransferred);

            clientHandler.sendResponse(FTPResponse.CLOSING_DATA_CONNECTION, "Transfer complete");

        } catch (IOException e) {
            // Clean up partial file on error
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }

            throw new FTPException(FTPResponse.CONNECTION_CLOSED, "Error receiving file");
        }
    }

    /**
     * Handle DELE command
     */
    private void handleDele(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            if (Files.isDirectory(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot delete directory with DELE");
            }

            Files.delete(filePath);
            clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK, "File deleted successfully");

            logger.info("File {} deleted by user {}", filename, session.getUsername());

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Handle MKD command
     */
    private void handleMkd(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String dirname = message.getFirstParameter();

        if (dirname == null || dirname.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Directory name required");
        }

        if (!FileUtils.isValidFilename(dirname)) {
            throw new FTPException(FTPResponse.FILE_NAME_NOT_ALLOWED, "Invalid directory name");
        }

        try {
            Path dirPath = session.resolveFilePath(dirname);

            if (Files.exists(dirPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Directory already exists");
            }

            Files.createDirectory(dirPath);
            clientHandler.sendResponse(FTPResponse.PATHNAME_CREATED,
                    "\"" + dirname + "\" directory created");

            logger.info("Directory {} created by user {}", dirname, session.getUsername());

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error creating directory: " + e.getMessage());
        }
    }

    /**
     * Handle RMD command
     */
    private void handleRmd(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String dirname = message.getFirstParameter();

        if (dirname == null || dirname.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Directory name required");
        }

        try {
            Path dirPath = session.resolveFilePath(dirname);

            if (!Files.exists(dirPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Directory not found");
            }

            if (!Files.isDirectory(dirPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Not a directory");
            }

            // Check if directory is empty
            try (var stream = Files.list(dirPath)) {
                if (stream.findAny().isPresent()) {
                    throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Directory not empty");
                }
            }

            Files.delete(dirPath);
            clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK, "Directory removed successfully");

            logger.info("Directory {} removed by user {}", dirname, session.getUsername());

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error removing directory: " + e.getMessage());
        }
    }

    /**
     * Handle RNFR command
     */
    private void handleRnfr(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            session.setRenameFromPath(filePath.toString());
            clientHandler.sendResponse(FTPResponse.FILE_ACTION_PENDING,
                    "File exists, ready for destination name");

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Handle RNTO command
     */
    private void handleRnto(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String newFilename = message.getFirstParameter();

        if (newFilename == null || newFilename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "New filename required");
        }

        if (session.getRenameFromPath() == null) {
            throw new FTPException(FTPResponse.BAD_COMMAND_SEQUENCE, "Send RNFR command first");
        }

        if (!FileUtils.isValidFilename(newFilename)) {
            session.clearRenameFromPath();
            throw new FTPException(FTPResponse.FILE_NAME_NOT_ALLOWED, "Invalid filename");
        }

        try {
            Path sourcePath = Path.of(session.getRenameFromPath());
            Path targetPath = session.resolveFilePath(newFilename);

            if (Files.exists(targetPath)) {
                session.clearRenameFromPath();
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Target file already exists");
            }

            Files.move(sourcePath, targetPath);
            session.clearRenameFromPath();

            clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK, "Rename successful");

            logger.info("File renamed from {} to {} by user {}",
                    sourcePath.getFileName(), newFilename, session.getUsername());

        } catch (IOException e) {
            session.clearRenameFromPath();
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error renaming file: " + e.getMessage());
        }
    }

    /**
     * Handle SIZE command
     */
    private void handleSize(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            if (Files.isDirectory(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot get size of directory");
            }

            long size = Files.size(filePath);
            clientHandler.sendResponse(FTPResponse.FILE_STATUS, String.valueOf(size));

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error getting file size: " + e.getMessage());
        }
    }

    /**
     * Handle MDTM command
     */
    private void handleMdtm(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        if (filename == null || filename.trim().isEmpty()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Filename required");
        }

        try {
            Path filePath = session.resolveFilePath(filename);

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            var modTime = Files.getLastModifiedTime(filePath);
            var instant = modTime.toInstant();
            var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            var formattedTime = formatter.format(instant.atZone(java.time.ZoneOffset.UTC));

            clientHandler.sendResponse(FTPResponse.FILE_STATUS, formattedTime);

        } catch (IOException e) {
            throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Error getting file time: " + e.getMessage());
        }
    }

    /**
     * Handle SYST command
     */
    private void handleSyst(ClientHandler clientHandler) {
        String osName = System.getProperty("os.name", "Unknown");
        clientHandler.sendResponse(FTPResponse.SYSTEM_TYPE, "UNIX Type: L8 (" + osName + ")");
    }

    /**
     * Handle NOOP command
     */
    private void handleNoop(ClientHandler clientHandler) {
        clientHandler.sendResponse(FTPResponse.COMMAND_OK, "NOOP command successful");
    }

    /**
     * Handle QUIT command
     */
    private void handleQuit(ClientHandler clientHandler) {
        logger.info("User {} disconnecting from {}",
                session.getUsername(), session.getClientSocket().getRemoteSocketAddress());

        clientHandler.sendResponse(FTPResponse.SERVICE_CLOSING, "Goodbye");
        session.logout();
    }

    /**
     * Handle FEAT command (Feature negotiation)
     */
    private void handleFeat(ClientHandler clientHandler) {
        // Send multi-line response for supported features
        try {
            clientHandler.getClientSocket().getOutputStream().write("211-Features:\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" SIZE\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" MDTM\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" REST STREAM\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" MLST type*;size*;modify*;\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" MLSD\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" AUTH TLS\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" PBSZ\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" PROT\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write(" UTF8\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().write("211 End\r\n".getBytes());
            clientHandler.getClientSocket().getOutputStream().flush();
        } catch (IOException e) {
            clientHandler.sendResponse(FTPResponse.ACTION_ABORTED, "Error sending feature list");
        }
    }

    /**
     * Handle MLST command (Machine-readable file listing)
     */
    private void handleMlst(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        String filename = message.getFirstParameter();

        try {
            Path filePath;
            if (filename != null && !filename.trim().isEmpty()) {
                filePath = session.resolveFilePath(filename);
            } else {
                filePath = session.getCurrentDirectory();
            }

            if (!Files.exists(filePath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
            }

            FileInfo fileInfo = FileUtils.getFileInfo(filePath);
            if (fileInfo != null) {
                String mlstLine = generateMLSTLine(fileInfo);
                clientHandler.sendResponse(FTPResponse.FILE_STATUS, mlstLine);
            } else {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot access file information");
            }

        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error processing MLST: " + e.getMessage());
        }
    }

    /**
     * Handle MLSD command (Machine-readable directory listing)
     */
    private void handleMlsd(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        try {
            Path listPath = session.getCurrentDirectory();

            if (message.hasParameters()) {
                String path = message.getFirstParameter();
                listPath = session.resolveFilePath(path);
            }

            if (!Files.exists(listPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Path not found");
            }

            if (!Files.isDirectory(listPath)) {
                throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Not a directory");
            }

            sendMLSDData(listPath, clientHandler);

        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error listing directory: " + e.getMessage());
        }
    }

    /**
     * Generate MLST format line for a file
     */
    private String generateMLSTLine(FileInfo fileInfo) {
        StringBuilder sb = new StringBuilder();

        // Type fact
        sb.append("type=").append(fileInfo.isDirectory() ? "dir" : "file").append(";");

        // Size fact (only for files)
        if (!fileInfo.isDirectory()) {
            sb.append("size=").append(fileInfo.getSize()).append(";");
        }

        // Modify fact
        sb.append("modify=").append(fileInfo.getLastModified()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))).append(";");

        // Permissions
        sb.append("perm=");
        if (fileInfo.isDirectory()) {
            sb.append(fileInfo.isWritable() ? "flcdmp" : "flr");
        } else {
            sb.append(fileInfo.isWritable() ? "adfr" : "r");
        }
        sb.append(";");

        // Add filename
        sb.append(" ").append(fileInfo.getName());

        return sb.toString();
    }

    /**
     * Send MLSD data via control connection
     */
    private void sendMLSDData(Path path, ClientHandler clientHandler) throws IOException, FTPException {
        clientHandler.sendResponse(FTPResponse.FILE_ACTION_OK, "Here comes the directory listing");

        List<FileInfo> files = FileUtils.listDirectory(path);

        try (OutputStream output = clientHandler.getClientSocket().getOutputStream()) {
            for (FileInfo file : files) {
                String mlsdLine = generateMLSTLine(file) + "\r\n";
                output.write(mlsdLine.getBytes());
            }
            output.flush();
        }

        clientHandler.sendResponse(FTPResponse.CLOSING_DATA_CONNECTION, "Directory send OK");
    }

    /**
     * Handle OPTS command (Options)
     */
    private void handleOpts(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        if (!message.hasParameters()) {
            throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "OPTS requires parameters");
        }

        String option = message.getFirstParameter().toUpperCase();

        switch (option) {
            case "UTF8":
                String value = message.getParameterCount() > 1 ? message.getParameter(1).toUpperCase() : "ON";
                if ("ON".equals(value)) {
                    session.setUtf8Enabled(true);
                    clientHandler.sendResponse(FTPResponse.COMMAND_OK, "UTF8 set to on");
                } else if ("OFF".equals(value)) {
                    session.setUtf8Enabled(false);
                    clientHandler.sendResponse(FTPResponse.COMMAND_OK, "UTF8 set to off");
                } else {
                    throw new FTPException(FTPResponse.SYNTAX_ERROR_PARAMETERS, "Invalid UTF8 option");
                }
                break;
            default:
                throw new FTPException(FTPResponse.PARAMETER_NOT_IMPLEMENTED, "Option not supported: " + option);
        }
    }

    /**
     * Handle STAT command (Status)
     */
    private void handleStat(FTPMessage message, ClientHandler clientHandler) throws FTPException {
        try {
            if (message.hasParameters()) {
                // STAT with path - show file/directory info
                String path = message.getFirstParameter();
                Path filePath = session.resolveFilePath(path);

                if (!Files.exists(filePath)) {
                    throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "File not found");
                }

                FileInfo fileInfo = FileUtils.getFileInfo(filePath);
                if (fileInfo != null) {
                    clientHandler.sendResponse(FTPResponse.FILE_STATUS, fileInfo.toListFormat());
                } else {
                    throw new FTPException(FTPResponse.FILE_UNAVAILABLE, "Cannot access file information");
                }
            } else {
                // STAT without parameters - show server status
                sendServerStatus(clientHandler);
            }
        } catch (IOException e) {
            throw new FTPException(FTPResponse.ACTION_ABORTED, "Error getting status: " + e.getMessage());
        }
    }

    /**
     * Send server status information
     */
    private void sendServerStatus(ClientHandler clientHandler) throws IOException {
        StringBuilder status = new StringBuilder();
        status.append("FTP Server Status:\r\n");
        status.append("Connected from: ").append(session.getClientSocket().getRemoteSocketAddress()).append("\r\n");
        status.append("Logged in as: ").append(session.getUsername() != null ? session.getUsername() : "Not logged in").append("\r\n");
        status.append("Current directory: ").append(session.getCurrentDirectoryPath()).append("\r\n");
        status.append("Server version: FTP Server 1.0\r\n");
        status.append("Connection time: ").append(session.getConnectionTime()).append("\r\n");

        try (OutputStream output = clientHandler.getClientSocket().getOutputStream()) {
            output.write(("211-Status of " + session.getClientSocket().getLocalSocketAddress() + ":\r\n").getBytes());
            output.write(status.toString().getBytes());
            output.write("211 End of status\r\n".getBytes());
            output.flush();
        }
    }
}