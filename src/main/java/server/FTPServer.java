package server;

import common.FTPConfig;
import utils.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main FTP Server class
 * Handles incoming client connections and manages the server lifecycle
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPServer {
    private static final Logger logger = LogManager.getLogger("server");

    private final FTPConfig config;
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private final AtomicBoolean isRunning;
    private final AtomicInteger activeConnections;
    private final int port;
    private final int maxConnections;
    private final SecurityManager securityManager;
    private final PerformanceMonitor performanceMonitor;

    /**
     * Constructor with default configuration
     */
    public FTPServer() {
        this(FTPConfig.getInstance());
    }

    /**
     * Constructor with custom configuration
     * @param config FTP configuration
     */
    public FTPServer(FTPConfig config) {
        this.config = config;
        this.port = config.getServerPort();
        this.maxConnections = config.getMaxConnections();
        this.isRunning = new AtomicBoolean(false);
        this.activeConnections = new AtomicInteger(0);
        this.securityManager = SecurityManager.getInstance();
        this.performanceMonitor = PerformanceMonitor.getInstance();

        logger.info("FTP Server initialized on port {}", port);
    }

    /**
     * Start the FTP server
     * @throws IOException if server cannot be started
     */
    public void start() throws IOException {
        if (isRunning.get()) {
            logger.warn("Server is already running");
            return;
        }

        // Create server socket
        serverSocket = NetworkUtils.createServerSocket(port, maxConnections);

        // Create thread pool for client connections
        clientThreadPool = Executors.newFixedThreadPool(maxConnections);

        isRunning.set(true);

        logger.info("FTP Server started on port {} with max {} connections", port, maxConnections);

        // Print server information
        printServerInfo();

        // Start accepting connections
        acceptConnections();
    }

    /**
     * Accept incoming client connections
     */
    private void acceptConnections() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                InetAddress clientAddress = clientSocket.getInetAddress();

                // Security checks
                if (!securityManager.isConnectionAllowed(clientAddress)) {
                    logger.warn("Connection rejected for security reasons: {}", clientAddress);
                    rejectConnection(clientSocket);
                    continue;
                }

                // Check rate limiting
                if (securityManager.isRateLimitExceeded(clientAddress)) {
                    logger.warn("Connection rejected due to rate limiting: {}", clientAddress);
                    rejectConnection(clientSocket);
                    continue;
                }

                // Check connection limit
                if (activeConnections.get() >= maxConnections) {
                    logger.warn("Maximum connections reached, rejecting client: {}", clientAddress);
                    rejectConnection(clientSocket);
                    continue;
                }

                // Configure client socket
                NetworkUtils.configureSocket(clientSocket, config.getBufferSize());
                clientSocket.setSoTimeout(config.getTimeout());

                // Register with security manager and performance monitor
                securityManager.registerConnection(clientAddress);
                performanceMonitor.recordConnection();

                // Create and submit client handler
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientThreadPool.submit(clientHandler);

                int connectionCount = activeConnections.incrementAndGet();
                logger.info("New client connected from {}, active connections: {}",
                        clientAddress, connectionCount);

            } catch (SocketException e) {
                if (isRunning.get()) {
                    logger.error("Socket error while accepting connections", e);
                    performanceMonitor.recordError();
                }
                // If server is stopping, this is expected
            } catch (IOException e) {
                if (isRunning.get()) {
                    logger.error("Error accepting client connection", e);
                    performanceMonitor.recordError();
                }
            }
        }
    }

    /**
     * Reject a client connection due to server limits
     * @param clientSocket Client socket to reject
     */
    private void rejectConnection(Socket clientSocket) {
        try {
            NetworkUtils.sendMessage(clientSocket, "421 Server busy, try again later.");
        } catch (IOException e) {
            logger.debug("Error sending rejection message to client", e);
        } finally {
            NetworkUtils.closeSocket(clientSocket);
        }
    }

    /**
     * Stop the FTP server
     */
    public void stop() {
        if (!isRunning.get()) {
            logger.warn("Server is not running");
            return;
        }

        logger.info("Stopping FTP Server...");
        isRunning.set(false);

        // Close server socket
        NetworkUtils.closeServerSocket(serverSocket);

        // Shutdown thread pool
        if (clientThreadPool != null) {
            clientThreadPool.shutdown();
            try {
                if (!clientThreadPool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    clientThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("FTP Server stopped");
    }

    /**
     * Called when a client disconnects
     * @param clientAddress Client IP address
     */
    public void onClientDisconnected(InetAddress clientAddress) {
        int connectionCount = activeConnections.decrementAndGet();
        securityManager.unregisterConnection(clientAddress);
        performanceMonitor.recordDisconnection();
        logger.info("Client disconnected from {}, active connections: {}", clientAddress, connectionCount);
    }

    /**
     * Check if server is running
     * @return true if server is running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Get current number of active connections
     * @return Number of active connections
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * Get server configuration
     * @return FTP configuration
     */
    public FTPConfig getConfig() {
        return config;
    }

    /**
     * Print server information
     */
    private void printServerInfo() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("          FTP SERVER STARTED");
        System.out.println("=".repeat(50));
        System.out.println("Port: " + port);
        System.out.println("Max Connections: " + maxConnections);
        System.out.println("Root Directory: " + config.getServerRootDirectory());
        System.out.println("Local IP: " + NetworkUtils.getLocalIPAddress());
        System.out.println("Status: RUNNING");
        System.out.println("=".repeat(50));
        System.out.println("Server is ready to accept connections...");
        System.out.println("Press Ctrl+C to stop the server\n");
    }

    /**
     * Get security manager
     * @return Security manager instance
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * Get performance monitor
     * @return Performance monitor instance
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    /**
     * Get server statistics
     * @return Server statistics string
     */
    public String getServerStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== FTP Server Statistics ===\n");
        stats.append("Server Status: ").append(isRunning.get() ? "RUNNING" : "STOPPED").append("\n");
        stats.append("Port: ").append(port).append("\n");
        stats.append("Max Connections: ").append(maxConnections).append("\n");
        stats.append("Active Connections: ").append(activeConnections.get()).append("\n");
        stats.append("\n");

        stats.append(performanceMonitor.getPerformanceStats()).append("\n\n");
        stats.append(securityManager.getSecurityStats());

        return stats.toString();
    }

    /**
     * Main method to run the server
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        FTPServer server = new FTPServer();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            server.stop();
        }));

        try {
            server.start();

            // Start admin interface in parallel
            if (args.length == 0 || !"--no-admin".equals(args[0])) {
                System.out.println("\nStarting administrative interface...");
                System.out.println("Type 'help' for admin commands or 'quit' to exit admin mode");
                AdminInterface.startAdminInterface(server);
            }

        } catch (IOException e) {
            logger.error("Failed to start FTP server", e);
            System.err.println("Failed to start FTP server: " + e.getMessage());
            System.exit(1);
        }
    }
}