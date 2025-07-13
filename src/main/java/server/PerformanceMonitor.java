package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring for FTP server
 * Tracks server performance metrics and resource usage
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class PerformanceMonitor {
    private static final Logger logger = LogManager.getLogger("server");

    private static PerformanceMonitor instance;

    // Performance counters
    private final AtomicLong totalConnections;
    private final AtomicLong currentConnections;
    private final AtomicLong totalCommands;
    private final AtomicLong totalUploads;
    private final AtomicLong totalDownloads;
    private final AtomicLong totalBytesUploaded;
    private final AtomicLong totalBytesDownloaded;
    private final AtomicLong totalErrors;

    // Timing
    private final LocalDateTime serverStartTime;
    private volatile LocalDateTime lastStatsReset;

    // System monitoring
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;

    /**
     * Private constructor for singleton pattern
     */
    private PerformanceMonitor() {
        this.totalConnections = new AtomicLong(0);
        this.currentConnections = new AtomicLong(0);
        this.totalCommands = new AtomicLong(0);
        this.totalUploads = new AtomicLong(0);
        this.totalDownloads = new AtomicLong(0);
        this.totalBytesUploaded = new AtomicLong(0);
        this.totalBytesDownloaded = new AtomicLong(0);
        this.totalErrors = new AtomicLong(0);

        this.serverStartTime = LocalDateTime.now();
        this.lastStatsReset = LocalDateTime.now();

        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();

        // Start monitoring thread
        startMonitoringThread();
    }

    /**
     * Get singleton instance
     *
     * @return PerformanceMonitor instance
     */
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }

    /**
     * Record new connection
     */
    public void recordConnection() {
        totalConnections.incrementAndGet();
        currentConnections.incrementAndGet();
        logger.debug("Connection recorded, total: {}, current: {}",
                totalConnections.get(), currentConnections.get());
    }

    /**
     * Record connection closed
     */
    public void recordDisconnection() {
        long current = currentConnections.decrementAndGet();
        if (current < 0) {
            currentConnections.set(0); // Safety check
        }
        logger.debug("Disconnection recorded, current connections: {}", Math.max(0, current));
    }

    /**
     * Record command execution
     */
    public void recordCommand() {
        totalCommands.incrementAndGet();
    }

    /**
     * Record file upload
     *
     * @param bytes Number of bytes uploaded
     */
    public void recordUpload(long bytes) {
        totalUploads.incrementAndGet();
        totalBytesUploaded.addAndGet(bytes);
        logger.debug("Upload recorded: {} bytes, total uploads: {}, total bytes: {}",
                bytes, totalUploads.get(), totalBytesUploaded.get());
    }

    /**
     * Record file download
     *
     * @param bytes Number of bytes downloaded
     */
    public void recordDownload(long bytes) {
        totalDownloads.incrementAndGet();
        totalBytesDownloaded.addAndGet(bytes);
        logger.debug("Download recorded: {} bytes, total downloads: {}, total bytes: {}",
                bytes, totalDownloads.get(), totalBytesDownloaded.get());
    }

    /**
     * Record error occurrence
     */
    public void recordError() {
        totalErrors.incrementAndGet();
    }

    /**
     * Get current performance statistics
     *
     * @return Performance statistics string
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();

        // Server uptime
        LocalDateTime now = LocalDateTime.now();
        long uptimeSeconds = java.time.Duration.between(serverStartTime, now).getSeconds();
        String uptime = formatDuration(uptimeSeconds);

        stats.append("=== FTP Server Performance Statistics ===\n");
        stats.append("Server Start Time: ").append(serverStartTime).append("\n");
        stats.append("Uptime: ").append(uptime).append("\n");
        stats.append("Last Stats Reset: ").append(lastStatsReset).append("\n");
        stats.append("\n");

        // Connection statistics
        stats.append("Connection Statistics:\n");
        stats.append("  Total Connections: ").append(totalConnections.get()).append("\n");
        stats.append("  Current Connections: ").append(currentConnections.get()).append("\n");
        stats.append("  Connection Rate: ").append(String.format("%.2f/hour",
                calculateRate(totalConnections.get(), uptimeSeconds))).append("\n");
        stats.append("\n");

        // Command statistics
        stats.append("Command Statistics:\n");
        stats.append("  Total Commands: ").append(totalCommands.get()).append("\n");
        stats.append("  Command Rate: ").append(String.format("%.2f/hour",
                calculateRate(totalCommands.get(), uptimeSeconds))).append("\n");
        stats.append("  Avg Commands/Connection: ").append(
                        totalConnections.get() > 0 ?
                                String.format("%.2f", (double) totalCommands.get() / totalConnections.get()) : "0.00")
                .append("\n");
        stats.append("\n");

        // Transfer statistics
        stats.append("Transfer Statistics:\n");
        stats.append("  Total Uploads: ").append(totalUploads.get()).append("\n");
        stats.append("  Total Downloads: ").append(totalDownloads.get()).append("\n");
        stats.append("  Bytes Uploaded: ").append(formatBytes(totalBytesUploaded.get())).append("\n");
        stats.append("  Bytes Downloaded: ").append(formatBytes(totalBytesDownloaded.get())).append("\n");
        stats.append("  Total Transfer: ").append(formatBytes(totalBytesUploaded.get() + totalBytesDownloaded.get())).append("\n");
        stats.append("  Upload Rate: ").append(String.format("%.2f MB/hour",
                calculateTransferRate(totalBytesUploaded.get(), uptimeSeconds))).append("\n");
        stats.append("  Download Rate: ").append(String.format("%.2f MB/hour",
                calculateTransferRate(totalBytesDownloaded.get(), uptimeSeconds))).append("\n");
        stats.append("\n");

        // Error statistics
        stats.append("Error Statistics:\n");
        stats.append("  Total Errors: ").append(totalErrors.get()).append("\n");
        stats.append("  Error Rate: ").append(String.format("%.2f%%",
                totalCommands.get() > 0 ?
                        (double) totalErrors.get() / totalCommands.get() * 100 : 0.0)).append("\n");
        stats.append("\n");

        // System resources
        stats.append("System Resources:\n");
        stats.append("  Memory Used: ").append(formatBytes(memoryBean.getHeapMemoryUsage().getUsed())).append("\n");
        stats.append("  Memory Max: ").append(formatBytes(memoryBean.getHeapMemoryUsage().getMax())).append("\n");
        stats.append("  Memory Usage: ").append(String.format("%.2f%%",
                (double) memoryBean.getHeapMemoryUsage().getUsed() /
                        memoryBean.getHeapMemoryUsage().getMax() * 100)).append("\n");
        stats.append("  Active Threads: ").append(threadBean.getThreadCount()).append("\n");
        stats.append("  Peak Threads: ").append(threadBean.getPeakThreadCount()).append("\n");
        stats.append("\n");

        stats.append("=========================================");

        return stats.toString();
    }

    /**
     * Calculate rate per hour
     *
     * @param total         Total count
     * @param uptimeSeconds Uptime in seconds
     * @return Rate per hour
     */
    private double calculateRate(long total, long uptimeSeconds) {
        if (uptimeSeconds <= 0) return 0.0;
        return (double) total / uptimeSeconds * 3600;
    }

    /**
     * Calculate transfer rate in MB per hour
     *
     * @param totalBytes    Total bytes transferred
     * @param uptimeSeconds Uptime in seconds
     * @return Transfer rate in MB/hour
     */
    private double calculateTransferRate(long totalBytes, long uptimeSeconds) {
        if (uptimeSeconds <= 0) return 0.0;
        double mbPerSecond = (double) totalBytes / (1024 * 1024) / uptimeSeconds;
        return mbPerSecond * 3600;
    }

    /**
     * Format duration in human-readable format
     *
     * @param seconds Duration in seconds
     * @return Formatted duration string
     */
    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, secs);
        } else if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Format bytes in human-readable format
     *
     * @param bytes Number of bytes
     * @return Formatted byte string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Reset statistics
     */
    public void resetStats() {
        totalConnections.set(0);
        totalCommands.set(0);
        totalUploads.set(0);
        totalDownloads.set(0);
        totalBytesUploaded.set(0);
        totalBytesDownloaded.set(0);
        totalErrors.set(0);
        lastStatsReset = LocalDateTime.now();

        logger.info("Performance statistics reset");
    }

    /**
     * Get simple status summary
     *
     * @return Status summary string
     */
    public String getStatusSummary() {
        return String.format("Connections: %d active, %d total | Commands: %d | Transfers: %s up, %s down | Errors: %d",
                currentConnections.get(),
                totalConnections.get(),
                totalCommands.get(),
                formatBytes(totalBytesUploaded.get()),
                formatBytes(totalBytesDownloaded.get()),
                totalErrors.get());
    }

    /**
     * Start monitoring thread for periodic logging
     */
    private void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300000); // Log stats every 5 minutes

                    if (totalConnections.get() > 0) {
                        logger.info("Performance Summary: {}", getStatusSummary());

                        // Log memory usage if high
                        double memoryUsage = (double) memoryBean.getHeapMemoryUsage().getUsed() /
                                memoryBean.getHeapMemoryUsage().getMax() * 100;
                        if (memoryUsage > 80) {
                            logger.warn("High memory usage: {:.2f}%", memoryUsage);
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.setName("PerformanceMonitor");
        monitorThread.start();
    }
}