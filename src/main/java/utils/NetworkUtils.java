package utils;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for network operations
 * Provides common networking functionality for FTP operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class NetworkUtils {

    /**
     * Send a message over a socket
     * @param socket Socket to send message to
     * @param message Message to send
     * @throws IOException if sending fails
     */
    public static void sendMessage(Socket socket, String message) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is null or closed");
        }

        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println(message);
            if (writer.checkError()) {
                throw new IOException("Error writing to socket");
            }
        }
    }

    /**
     * Receive a message from a socket
     * @param socket Socket to receive message from
     * @return Received message or null if connection closed
     * @throws IOException if receiving fails
     */
    public static String receiveMessage(Socket socket) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is null or closed");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            return reader.readLine();
        }
    }

    /**
     * Send a message over a socket with specific encoding
     * @param socket Socket to send message to
     * @param message Message to send
     * @param encoding Character encoding
     * @throws IOException if sending fails
     */
    public static void sendMessage(Socket socket, String message, String encoding) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is null or closed");
        }

        byte[] messageBytes = (message + "\r\n").getBytes(encoding);
        socket.getOutputStream().write(messageBytes);
        socket.getOutputStream().flush();
    }

    /**
     * Receive a message from a socket with specific encoding
     * @param socket Socket to receive message from
     * @param encoding Character encoding
     * @return Received message or null if connection closed
     * @throws IOException if receiving fails
     */
    public static String receiveMessage(Socket socket, String encoding) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is null or closed");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), encoding))) {
            return reader.readLine();
        }
    }

    /**
     * Transfer data from input stream to output stream
     * @param input Input stream
     * @param output Output stream
     * @param bufferSize Buffer size for transfer
     * @param progressCallback Progress callback (can be null)
     * @param totalSize Total size for progress calculation (can be -1 if unknown)
     * @return Number of bytes transferred
     * @throws IOException if transfer fails
     */
    public static long transferData(InputStream input, OutputStream output, int bufferSize,
                                    ProgressCallback progressCallback, long totalSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long totalTransferred = 0;
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            totalTransferred += bytesRead;

            if (progressCallback != null) {
                progressCallback.onProgress(totalTransferred, totalSize);
            }
        }

        output.flush();
        return totalTransferred;
    }

    /**
     * Create a server socket with specified port and backlog
     * @param port Port number
     * @param backlog Connection backlog
     * @return ServerSocket instance
     * @throws IOException if socket creation fails
     */
    public static ServerSocket createServerSocket(int port, int backlog) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port), backlog);
        return serverSocket;
    }

    /**
     * Create a client socket with timeout
     * @param host Host address
     * @param port Port number
     * @param timeoutMs Connection timeout in milliseconds
     * @return Socket instance
     * @throws IOException if connection fails
     */
    public static Socket createClientSocket(String host, int port, int timeoutMs) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        return socket;
    }

    /**
     * Find an available port within a range
     * @param startPort Start of port range
     * @param endPort End of port range
     * @return Available port number or -1 if none found
     */
    public static int findAvailablePort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                // Port is not available, continue searching
            }
        }
        return -1;
    }

    /**
     * Check if a host is reachable
     * @param host Host address
     * @param port Port number
     * @param timeoutMs Timeout in milliseconds
     * @return true if host is reachable
     */
    public static boolean isHostReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get local IP address
     * @return Local IP address string
     */
    public static String getLocalIPAddress() {
        try {
            // Try to connect to a remote address to determine local IP
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 80));
                return socket.getLocalAddress().getHostAddress();
            }
        } catch (IOException e) {
            // Fallback to localhost
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                return "127.0.0.1";
            }
        }
    }

    /**
     * Parse IP address and port from string
     * @param address Address string in format "ip:port"
     * @return InetSocketAddress or null if parsing fails
     */
    public static InetSocketAddress parseAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        String[] parts = address.trim().split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            return new InetSocketAddress(host, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Close socket safely without throwing exceptions
     * @param socket Socket to close
     */
    public static void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore close errors
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    /**
     * Close server socket safely without throwing exceptions
     * @param serverSocket ServerSocket to close
     */
    public static void closeServerSocket(ServerSocket serverSocket) {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore close errors
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Set socket options for optimal performance
     * @param socket Socket to configure
     * @param bufferSize Buffer size for socket
     * @throws SocketException if socket configuration fails
     */
    public static void configureSocket(Socket socket, int bufferSize) throws SocketException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setReceiveBufferSize(bufferSize);
        socket.setSendBufferSize(bufferSize);
    }

    /**
     * Progress callback interface for data transfer operations
     */
    public interface ProgressCallback {
        void onProgress(long bytesTransferred, long totalBytes);
    }
}