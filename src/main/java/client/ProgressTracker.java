package client;

/**
 * Utility class for tracking file transfer progress
 * Provides visual feedback for long-running operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ProgressTracker {
    private final String operation;
    private final long totalSize;
    private long currentSize;
    private long startTime;
    private boolean completed;

    /**
     * Constructor
     * @param operation Operation name (e.g., "Downloading", "Uploading")
     * @param totalSize Total size in bytes (-1 if unknown)
     */
    public ProgressTracker(String operation, long totalSize) {
        this.operation = operation;
        this.totalSize = totalSize;
        this.currentSize = 0;
        this.startTime = System.currentTimeMillis();
        this.completed = false;
    }

    /**
     * Update progress
     * @param bytesTransferred Bytes transferred so far
     */
    public void updateProgress(long bytesTransferred) {
        this.currentSize = bytesTransferred;
        displayProgress();
    }

    /**
     * Mark operation as completed
     */
    public void complete() {
        this.completed = true;
        displayProgress();
        System.out.println(); // New line after progress
    }

    /**
     * Display current progress
     */
    private void displayProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        double elapsedSeconds = elapsed / 1000.0;

        if (totalSize > 0) {
            // Show percentage and progress bar
            double percentage = (double) currentSize / totalSize * 100;
            int progressWidth = 30;
            int filled = (int) (percentage / 100 * progressWidth);

            StringBuilder progressBar = new StringBuilder();
            progressBar.append("[");
            for (int i = 0; i < progressWidth; i++) {
                if (i < filled) {
                    progressBar.append("=");
                } else if (i == filled) {
                    progressBar.append(">");
                } else {
                    progressBar.append(" ");
                }
            }
            progressBar.append("]");

            String speed = calculateSpeed(currentSize, elapsedSeconds);
            String eta = calculateETA(currentSize, totalSize, elapsedSeconds);

            System.out.printf("\r%s %s %.1f%% (%s/%s) %s ETA: %s",
                    operation, progressBar, percentage,
                    formatSize(currentSize), formatSize(totalSize),
                    speed, eta);

        } else {
            // Show only current size and speed
            String speed = calculateSpeed(currentSize, elapsedSeconds);
            System.out.printf("\r%s %s %s", operation, formatSize(currentSize), speed);
        }

        if (completed) {
            System.out.printf(" - Completed in %.2f seconds", elapsedSeconds);
        }
    }

    /**
     * Calculate transfer speed
     * @param bytes Bytes transferred
     * @param seconds Time elapsed in seconds
     * @return Formatted speed string
     */
    private String calculateSpeed(long bytes, double seconds) {
        if (seconds <= 0) {
            return "0 B/s";
        }

        double bytesPerSecond = bytes / seconds;
        return formatSize((long) bytesPerSecond) + "/s";
    }

    /**
     * Calculate estimated time of arrival
     * @param current Current bytes
     * @param total Total bytes
     * @param elapsed Elapsed seconds
     * @return Formatted ETA string
     */
    private String calculateETA(long current, long total, double elapsed) {
        if (current <= 0 || elapsed <= 0) {
            return "Unknown";
        }

        double remaining = total - current;
        double rate = current / elapsed;
        double etaSeconds = remaining / rate;

        if (etaSeconds < 60) {
            return String.format("%.0fs", etaSeconds);
        } else if (etaSeconds < 3600) {
            return String.format("%.0fm %.0fs", etaSeconds / 60, etaSeconds % 60);
        } else {
            return String.format("%.0fh %.0fm", etaSeconds / 3600, (etaSeconds % 3600) / 60);
        }
    }

    /**
     * Format file size in human-readable format
     * @param bytes Size in bytes
     * @return Formatted size string
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Create a simple text-based progress indicator
     * @param message Message to display
     * @return ProgressIndicator instance
     */
    public static ProgressIndicator createSimpleIndicator(String message) {
        return new ProgressIndicator(message);
    }

    /**
     * Simple progress indicator for operations without known size
     */
    public static class ProgressIndicator {
        private final String message;
        private volatile boolean running;
        private Thread animationThread;

        public ProgressIndicator(String message) {
            this.message = message;
            this.running = false;
        }

        public void start() {
            if (running) return;

            running = true;
            animationThread = new Thread(() -> {
                String[] spinner = {"|", "/", "-", "\\"};
                int index = 0;

                while (running) {
                    System.out.printf("\r%s %s", message, spinner[index]);
                    index = (index + 1) % spinner.length;

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });

            animationThread.start();
        }

        public void stop() {
            running = false;
            if (animationThread != null) {
                animationThread.interrupt();
                try {
                    animationThread.join(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            System.out.print("\r"); // Clear the line
        }

        public void stopWithMessage(String finalMessage) {
            stop();
            System.out.println(finalMessage);
        }
    }
}