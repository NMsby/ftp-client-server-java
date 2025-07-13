package server;

import utils.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Network configuration utilities for FTP server deployment
 * Handles network discovery, port configuration, and firewall guidance
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class NetworkConfiguration {
    private static final Logger logger = LogManager.getLogger("server");

    /**
     * Get all available network interfaces and IP addresses
     * @return List of network interface information
     */
    public static List<NetworkInterfaceInfo> getNetworkInterfaces() {
        List<NetworkInterfaceInfo> interfaces = new ArrayList<>();

        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (netInterface.isUp() && !netInterface.isLoopback()) {
                    for (InetAddress addr : Collections.list(netInterface.getInetAddresses())) {
                        if (addr instanceof Inet4Address) { // IPv4 only for simplicity
                            interfaces.add(new NetworkInterfaceInfo(
                                    netInterface.getName(),
                                    netInterface.getDisplayName(),
                                    addr.getHostAddress(),
                                    isPrivateIP(addr.getHostAddress())
                            ));
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Error discovering network interfaces", e);
        }

        return interfaces;
    }

    /**
     * Check if IP address is in private range
     * @param ip IP address string
     * @return true if IP is private
     */
    public static boolean isPrivateIP(String ip) {
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                (ip.startsWith("172.") && isInRange172(ip)) ||
                ip.equals("127.0.0.1");
    }

    /**
     * Check if IP is in 172.16.0.0 - 172.31.255.255 range
     */
    private static boolean isInRange172(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int secondOctet = Integer.parseInt(parts[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            }
        } catch (NumberFormatException e) {
            // Invalid IP format
        }
        return false;
    }

    /**
     * Test port accessibility from external networks
     * @param port Port to test
     * @return Port test results
     */
    public static PortTestResult testPortAccessibility(int port) {
        PortTestResult result = new PortTestResult(port);

        // Test local binding
        try (ServerSocket testSocket = new ServerSocket(port)) {
            result.setLocalBindSuccess(true);
            result.setLocalBindAddress(testSocket.getInetAddress().getHostAddress());
        } catch (Exception e) {
            result.setLocalBindSuccess(false);
            result.setLocalBindError(e.getMessage());
        }

        // Test external accessibility (simplified check)
        result.setExternalAccessible(checkExternalAccess(port));

        return result;
    }

    /**
     * Check if port is accessible externally (simplified)
     */
    private static boolean checkExternalAccess(int port) {
        // This is a simplified check - in reality, you'd need external testing
        // For now, we'll just check if the port is not in use
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get recommended firewall configuration
     * @param serverPort FTP server port
     * @param dataPortStart Data port range start
     * @param dataPortEnd Data port range end
     * @return Firewall configuration guide
     */
    public static FirewallConfig getFirewallConfiguration(int serverPort, int dataPortStart, int dataPortEnd) {
        return new FirewallConfig(serverPort, dataPortStart, dataPortEnd);
    }

    /**
     * Generate deployment instructions
     * @param config Server configuration
     * @return Deployment instructions
     */
    public static String generateDeploymentInstructions(common.FTPConfig config) {
        StringBuilder instructions = new StringBuilder();

        instructions.append("=== FTP Server Deployment Instructions ===\n\n");

        // Network configuration
        instructions.append("1. NETWORK CONFIGURATION\n");
        instructions.append("Server Port: ").append(config.getServerPort()).append("\n");
        instructions.append("Data Port Range: ").append(config.getDataPortStart())
                .append("-").append(config.getDataPortEnd()).append("\n\n");

        // Available interfaces
        instructions.append("2. AVAILABLE NETWORK INTERFACES\n");
        List<NetworkInterfaceInfo> interfaces = getNetworkInterfaces();
        for (NetworkInterfaceInfo iface : interfaces) {
            instructions.append("   ").append(iface.getDisplayName())
                    .append(" (").append(iface.getName()).append("): ")
                    .append(iface.getIpAddress())
                    .append(iface.isPrivate() ? " [Private]" : " [Public]")
                    .append("\n");
        }
        instructions.append("\n");

        // Firewall configuration
        FirewallConfig firewallConfig = getFirewallConfiguration(
                config.getServerPort(), config.getDataPortStart(), config.getDataPortEnd());
        instructions.append("3. FIREWALL CONFIGURATION\n");
        instructions.append(firewallConfig.getInstructions()).append("\n");

        // Client connection examples
        instructions.append("4. CLIENT CONNECTION EXAMPLES\n");
        for (NetworkInterfaceInfo iface : interfaces) {
            if (!iface.getIpAddress().equals("127.0.0.1")) {
                instructions.append("   Connect from network: ftp://")
                        .append(iface.getIpAddress()).append(":").append(config.getServerPort())
                        .append("\n");
                instructions.append("   Command line: java Main client ")
                        .append(iface.getIpAddress()).append(" ").append(config.getServerPort())
                        .append("\n");
            }
        }

        return instructions.toString();
    }

    /**
     * Network interface information
     */
    public static class NetworkInterfaceInfo {
        private final String name;
        private final String displayName;
        private final String ipAddress;
        private final boolean isPrivate;

        public NetworkInterfaceInfo(String name, String displayName, String ipAddress, boolean isPrivate) {
            this.name = name;
            this.displayName = displayName;
            this.ipAddress = ipAddress;
            this.isPrivate = isPrivate;
        }

        // Getters
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getIpAddress() { return ipAddress; }
        public boolean isPrivate() { return isPrivate; }

        @Override
        public String toString() {
            return String.format("%s (%s): %s %s",
                    displayName, name, ipAddress, isPrivate ? "[Private]" : "[Public]");
        }
    }

    /**
     * Port accessibility test results
     */
    public static class PortTestResult {
        private final int port;
        private boolean localBindSuccess;
        private String localBindAddress;
        private String localBindError;
        private boolean externalAccessible;

        public PortTestResult(int port) {
            this.port = port;
        }

        // Getters and setters
        public int getPort() { return port; }
        public boolean isLocalBindSuccess() { return localBindSuccess; }
        public void setLocalBindSuccess(boolean localBindSuccess) { this.localBindSuccess = localBindSuccess; }
        public String getLocalBindAddress() { return localBindAddress; }
        public void setLocalBindAddress(String localBindAddress) { this.localBindAddress = localBindAddress; }
        public String getLocalBindError() { return localBindError; }
        public void setLocalBindError(String localBindError) { this.localBindError = localBindError; }
        public boolean isExternalAccessible() { return externalAccessible; }
        public void setExternalAccessible(boolean externalAccessible) { this.externalAccessible = externalAccessible; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Port ").append(port).append(" Test Results:\n");
            sb.append("  Local Bind: ").append(localBindSuccess ? "SUCCESS" : "FAILED");
            if (localBindSuccess) {
                sb.append(" (").append(localBindAddress).append(")");
            } else {
                sb.append(" (").append(localBindError).append(")");
            }
            sb.append("\n");
            sb.append("  External Access: ").append(externalAccessible ? "ACCESSIBLE" : "NOT ACCESSIBLE");
            return sb.toString();
        }
    }

    /**
     * Firewall configuration recommendations
     */
    public static class FirewallConfig {
        private final int serverPort;
        private final int dataPortStart;
        private final int dataPortEnd;

        public FirewallConfig(int serverPort, int dataPortStart, int dataPortEnd) {
            this.serverPort = serverPort;
            this.dataPortStart = dataPortStart;
            this.dataPortEnd = dataPortEnd;
        }

        public String getInstructions() {
            StringBuilder sb = new StringBuilder();

            sb.append("Required firewall rules:\n");
            sb.append("  - Allow inbound TCP port ").append(serverPort).append(" (FTP Control)\n");
            sb.append("  - Allow inbound TCP ports ").append(dataPortStart).append("-").append(dataPortEnd).append(" (FTP Data)\n\n");

            sb.append("Windows Firewall (PowerShell as Administrator):\n");
            sb.append("  New-NetFirewallRule -DisplayName \"FTP Server Control\" -Direction Inbound -Protocol TCP -LocalPort ").append(serverPort).append(" -Action Allow\n");
            sb.append("  New-NetFirewallRule -DisplayName \"FTP Server Data\" -Direction Inbound -Protocol TCP -LocalPort ").append(dataPortStart).append("-").append(dataPortEnd).append(" -Action Allow\n\n");

            sb.append("Linux iptables:\n");
            sb.append("  sudo iptables -A INPUT -p tcp --dport ").append(serverPort).append(" -j ACCEPT\n");
            sb.append("  sudo iptables -A INPUT -p tcp --dport ").append(dataPortStart).append(":").append(dataPortEnd).append(" -j ACCEPT\n\n");

            sb.append("Linux ufw:\n");
            sb.append("  sudo ufw allow ").append(serverPort).append("/tcp\n");
            sb.append("  sudo ufw allow ").append(dataPortStart).append(":").append(dataPortEnd).append("/tcp\n");

            return sb.toString();
        }
    }
}