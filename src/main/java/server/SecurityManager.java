package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security manager for FTP server
 * Handles IP-based access control, rate limiting, and security monitoring
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class SecurityManager {
    private static final Logger logger = LogManager.getLogger("server");

    private static SecurityManager instance;

    // Security settings
    private final int maxLoginAttempts;
    private final int banDurationMinutes;
    private final int maxConnectionsPerIP;
    private final int rateLimitWindow;
    private final int maxRequestsPerWindow;

    // Tracking maps
    private final ConcurrentHashMap<String, AtomicInteger> loginAttempts;
    private final ConcurrentHashMap<String, LocalDateTime> bannedIPs;
    private final ConcurrentHashMap<String, AtomicInteger> connectionsPerIP;
    private final ConcurrentHashMap<String, RequestWindow> requestRateLimits;

    /**
     * Private constructor for singleton pattern
     */
    private SecurityManager() {
        this.maxLoginAttempts = 3;
        this.banDurationMinutes = 15;
        this.maxConnectionsPerIP = 5;
        this.rateLimitWindow = 60; // seconds
        this.maxRequestsPerWindow = 100;

        this.loginAttempts = new ConcurrentHashMap<>();
        this.bannedIPs = new ConcurrentHashMap<>();
        this.connectionsPerIP = new ConcurrentHashMap<>();
        this.requestRateLimits = new ConcurrentHashMap<>();

        // Start cleanup thread
        startCleanupThread();
    }

    /**
     * Get singleton instance
     * @return SecurityManager instance
     */
    public static synchronized SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }

    /**
     * Check if IP address is allowed to connect
     * @param clientAddress Client IP address
     * @return true if connection is allowed
     */
    public boolean isConnectionAllowed(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();

        // Check if IP is banned
        if (isIPBanned(ip)) {
            logger.warn("Connection denied for banned IP: {}", ip);
            return false;
        }

        // Check connection limit per IP
        AtomicInteger connections = connectionsPerIP.get(ip);
        if (connections != null && connections.get() >= maxConnectionsPerIP) {
            logger.warn("Connection denied for IP {} - too many connections: {}", ip, connections.get());
            return false;
        }

        return true;
    }

    /**
     * Register a new connection from IP
     * @param clientAddress Client IP address
     */
    public void registerConnection(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        connectionsPerIP.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
        logger.debug("Registered connection from {}, total: {}", ip, connectionsPerIP.get(ip).get());
    }

    /**
     * Unregister connection from IP
     * @param clientAddress Client IP address
     */
    public void unregisterConnection(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        AtomicInteger connections = connectionsPerIP.get(ip);
        if (connections != null) {
            int count = connections.decrementAndGet();
            if (count <= 0) {
                connectionsPerIP.remove(ip);
            }
            logger.debug("Unregistered connection from {}, remaining: {}", ip, Math.max(0, count));
        }
    }

    /**
     * Record failed login attempt
     * @param clientAddress Client IP address
     */
    public void recordFailedLogin(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        AtomicInteger attempts = loginAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int failureCount = attempts.incrementAndGet();

        logger.warn("Failed login attempt from {} (attempt {})", ip, failureCount);

        // Ban IP if too many attempts
        if (failureCount >= maxLoginAttempts) {
            banIP(ip);
        }
    }

    /**
     * Record successful login (clear failed attempts)
     * @param clientAddress Client IP address
     */
    public void recordSuccessfulLogin(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        loginAttempts.remove(ip);
        logger.info("Successful login from {}, cleared failed attempts", ip);
    }

    /**
     * Check if request rate limit is exceeded
     * @param clientAddress Client IP address
     * @return true if rate limit is exceeded
     */
    public boolean isRateLimitExceeded(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        RequestWindow window = requestRateLimits.computeIfAbsent(ip, k -> new RequestWindow());

        return !window.allowRequest();
    }

    /**
     * Ban an IP address
     * @param ip IP address to ban
     */
    private void banIP(String ip) {
        bannedIPs.put(ip, LocalDateTime.now().plusMinutes(banDurationMinutes));
        logger.warn("Banned IP {} for {} minutes due to excessive failed login attempts", ip, banDurationMinutes);
    }

    /**
     * Check if IP is currently banned
     * @param ip IP address
     * @return true if IP is banned
     */
    private boolean isIPBanned(String ip) {
        LocalDateTime banExpiry = bannedIPs.get(ip);
        if (banExpiry == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(banExpiry)) {
            bannedIPs.remove(ip);
            logger.info("Ban expired for IP: {}", ip);
            return false;
        }

        return true;
    }

    /**
     * Start cleanup thread to remove expired entries
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        cleanupThread.setDaemon(true);
        cleanupThread.setName("SecurityManager-Cleanup");
        cleanupThread.start();
    }

    /**
     * Clean up expired security entries
     */
    private void cleanupExpiredEntries() {
        // Clean up expired bans
        bannedIPs.entrySet().removeIf(entry -> {
            boolean expired = LocalDateTime.now().isAfter(entry.getValue());
            if (expired) {
                logger.debug("Removed expired ban for IP: {}", entry.getKey());
            }
            return expired;
        });

        // Clean up old rate limit windows
        requestRateLimits.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                logger.debug("Removed expired rate limit window for IP: {}", entry.getKey());
            }
            return expired;
        });

        // Clean up IPs with zero connections
        connectionsPerIP.entrySet().removeIf(entry -> {
            boolean zero = entry.getValue().get() <= 0;
            if (zero) {
                logger.debug("Removed zero connection entry for IP: {}", entry.getKey());
            }
            return zero;
        });
    }

    /**
     * Get security statistics
     * @return Security statistics string
     */
    public String getSecurityStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Security Statistics ===\n");
        stats.append("Active connections by IP:\n");
        connectionsPerIP.forEach((ip, count) ->
                stats.append("  ").append(ip).append(": ").append(count.get()).append(" connections\n"));

        stats.append("Failed login attempts:\n");
        loginAttempts.forEach((ip, count) ->
                stats.append("  ").append(ip).append(": ").append(count.get()).append(" attempts\n"));

        stats.append("Banned IPs:\n");
        bannedIPs.forEach((ip, expiry) ->
                stats.append("  ").append(ip).append(": banned until ").append(expiry).append("\n"));

        stats.append("Rate limit windows: ").append(requestRateLimits.size()).append("\n");
        stats.append("==========================");

        return stats.toString();
    }

    /**
     * Request rate limiting window
     */
    private class RequestWindow {
        private final LocalDateTime windowStart;
        private final AtomicInteger requestCount;

        public RequestWindow() {
            this.windowStart = LocalDateTime.now();
            this.requestCount = new AtomicInteger(0);
        }

        public boolean allowRequest() {
            if (isExpired()) {
                return true; // Window expired, allow request
            }

            return requestCount.incrementAndGet() <= maxRequestsPerWindow;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(windowStart.plusSeconds(rateLimitWindow));
        }
    }
}