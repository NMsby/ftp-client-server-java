package server;

import common.*;
import utils.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Handles individual client connections
 * Each client gets its own ClientHandler instance running in a separate thread
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger("server");

    private final Socket clientSocket;
    private final FTPServer server;
    private BufferedReader input;
    private PrintWriter output;
    private ClientSession session;
    private CommandProcessor commandProcessor;
    private volatile boolean isConnected;

    /**
     * Constructor
     * @param clientSocket Client socket connection
     * @param server Reference to the main server
     */
    public ClientHandler(Socket clientSocket, FTPServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.isConnected = true;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        logger.info("Starting client handler for {}", clientAddress);

        try {
            // Initialize streams
            initializeStreams();

            // Create client session
            session = new ClientSession(clientSocket, server.getConfig());

            // Create command processor
            commandProcessor = new CommandProcessor(session);

            // Send welcome message
            sendResponse(FTPResponse.SERVICE_READY, "FTP Server ready");

            // Process client commands
            processCommands();

        } catch (IOException e) {
            logger.error("IO error handling client {}: {}", clientAddress, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error handling client {}", clientAddress, e);
        } finally {
            cleanup();
        }
    }

    /**
     * Initialize input/output streams
     * @throws IOException if stream initialization fails
     */
    private void initializeStreams() throws IOException {
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    /**
     * Process client commands in a loop
     */
    private void processCommands() {
        while (isConnected && !Thread.currentThread().isInterrupted()) {
            try {
                // Read command from client
                String rawCommand = input.readLine();

                if (rawCommand == null) {
                    // Client disconnected
                    logger.info("Client {} disconnected", clientSocket.getRemoteSocketAddress());
                    break;
                }

                rawCommand = rawCommand.trim();
                if (rawCommand.isEmpty()) {
                    continue;
                }

                logger.debug("Received command from {}: {}", clientSocket.getRemoteSocketAddress(), rawCommand);

                // Parse and process command
                FTPMessage message = new FTPMessage(rawCommand);
                processCommand(message);

                // Check if client requested to quit
                if (message.getCommand() == FTPCommand.QUIT) {
                    break;
                }

            } catch (SocketTimeoutException e) {
                logger.debug("Socket timeout for client {}", clientSocket.getRemoteSocketAddress());
                sendResponse(FTPResponse.SERVICE_CLOSING, "Connection timeout");
                break;
            } catch (IOException e) {
                if (isConnected) {
                    logger.warn("IO error reading from client {}: {}",
                            clientSocket.getRemoteSocketAddress(), e.getMessage());
                }
                break;
            } catch (Exception e) {
                logger.error("Error processing command from client {}",
                        clientSocket.getRemoteSocketAddress(), e);
                sendResponse(FTPResponse.ACTION_ABORTED, "Internal server error");
            }
        }
    }

    /**
     * Process a single FTP command
     * @param message FTP message containing the command
     */
    private void processCommand(FTPMessage message) {
        try {
            // Validate message
            if (!message.isValid()) {
                sendResponse(FTPResponse.SYNTAX_ERROR_COMMAND, "Invalid command format");
                return;
            }

            // Check authentication requirements
            if (message.getCommand().requiresAuthentication() && !session.isAuthenticated()) {
                sendResponse(FTPResponse.NOT_LOGGED_IN, "Please login first");
                return;
            }

            // Process command using command processor
            commandProcessor.processCommand(message, this);

        } catch (AuthenticationException e) {
            logger.warn("Authentication error for client {}: {}",
                    clientSocket.getRemoteSocketAddress(), e.getMessage());
            sendResponse(e.getResponseCode(), e.getMessage());
        } catch (FTPException e) {
            logger.warn("FTP error for client {}: {}",
                    clientSocket.getRemoteSocketAddress(), e.getMessage());
            sendResponse(e.getResponseCode() != null ? e.getResponseCode() : FTPResponse.ACTION_ABORTED,
                    e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing command for client {}",
                    clientSocket.getRemoteSocketAddress(), e);
            sendResponse(FTPResponse.ACTION_ABORTED, "Internal server error");
        }
    }

    /**
     * Send response to client
     * @param response FTP response code
     * @param message Custom message (optional)
     */
    public void sendResponse(FTPResponse response, String message) {
        try {
            String responseText = message != null ?
                    response.getFormattedResponse(message) :
                    response.getFormattedResponse();

            output.println(responseText);
            logger.debug("Sent response to {}: {}", clientSocket.getRemoteSocketAddress(), responseText);

        } catch (Exception e) {
            logger.error("Error sending response to client {}",
                    clientSocket.getRemoteSocketAddress(), e);
        }
    }

    /**
     * Send response to client with default message
     * @param response FTP response code
     */
    public void sendResponse(FTPResponse response) {
        sendResponse(response, null);
    }

    /**
     * Get client session
     * @return Client session
     */
    public ClientSession getSession() {
        return session;
    }

    /**
     * Get client socket
     * @return Client socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Disconnect the client
     */
    public void disconnect() {
        isConnected = false;
        NetworkUtils.closeSocket(clientSocket);
    }

    /**
     * Cleanup resources and notify server
     */
    private void cleanup() {
        isConnected = false;

        // Close streams
        try {
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (IOException e) {
            logger.debug("Error closing streams for client {}",
                    clientSocket.getRemoteSocketAddress(), e);
        }

        // Close socket
        NetworkUtils.closeSocket(clientSocket);

        // Notify server of disconnection
        server.onClientDisconnected();

        logger.info("Client handler cleanup completed for {}",
                clientSocket.getRemoteSocketAddress());
    }
}