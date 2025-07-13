import client.FTPClient;
import common.FTPConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Performance benchmarking tools for FTP server
 * Tests throughput, concurrent connections, and resource usage
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class PerformanceBenchmark {

    private static final String TEST_SERVER = System.getProperty("test.server.host", "localhost");
    private static final int TEST_PORT = Integer.parseInt(System.getProperty("test.server.port", "21"));
    private static final String TEST_USER = "admin";
    private static final String TEST_PASS = "admin123";

    @Test
    void benchmarkSequentialTransfers() {
        System.out.println("=== Sequential Transfer Benchmark ===");

        FTPClient client = new FTPClient();

        try {
            // Connect and authenticate
            if (!client.connect(TEST_SERVER, TEST_PORT) || !client.login(TEST_USER, TEST_PASS)) {
                System.err.println("Failed to connect/authenticate");
                return;
            }

            // Test different file sizes
            int[] fileSizes = {1024, 10240, 102400, 1048576}; // 1KB, 10KB, 100KB, 1MB
            String[] sizeNames = {"1KB", "10KB", "100KB", "1MB"};

            for (int i = 0; i < fileSizes.length; i++) {
                System.out.println("\nTesting " + sizeNames[i] + " files...");
                benchmarkFileSize(client, fileSizes[i], sizeNames[i]);
            }

        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }

    private void benchmarkFileSize(FTPClient client, int fileSize, String sizeName) {
        try {
            byte[] testData = new byte[fileSize];
            for (int i = 0; i < fileSize; i++) {
                testData[i] = (byte) (i % 256);
            }

            String fileName = "benchmark-" + sizeName + ".dat";
            Path localFile = Paths.get(FTPConfig.getInstance().getClientRootDirectory(), fileName);
            Files.write(localFile, testData);

            // Benchmark upload
            long uploadStart = System.nanoTime();
            boolean uploaded = client.uploadFile(fileName, fileName);
            long uploadEnd = System.nanoTime();

            if (uploaded) {
                double uploadTime = (uploadEnd - uploadStart) / 1_000_000.0; // milliseconds
                double uploadRate = (fileSize / 1024.0) / (uploadTime / 1000.0); // KB/s
                System.out.printf("  Upload: %.2f ms (%.2f KB/s)%n", uploadTime, uploadRate);

                // Benchmark download
                Files.delete(localFile);
                long downloadStart = System.nanoTime();
                boolean downloaded = client.downloadFile(fileName, fileName);
                long downloadEnd = System.nanoTime();

                if (downloaded) {
                    double downloadTime = (downloadEnd - downloadStart) / 1_000_000.0; // milliseconds
                    double downloadRate = (fileSize / 1024.0) / (downloadTime / 1000.0); // KB/s
                    System.out.printf("  Download: %.2f ms (%.2f KB/s)%n", downloadTime, downloadRate);
                }

                // Cleanup
                client.deleteFile(fileName);
            }

            Files.deleteIfExists(localFile);

        } catch (IOException e) {
            System.err.println("Error in benchmark: " + e.getMessage());
        }
    }

    @Test
    void benchmarkConcurrentConnections() {
        System.out.println("\n=== Concurrent Connections Benchmark ===");

        int[] connectionCounts = {5, 10, 20, 50};

        for (int connCount : connectionCounts) {
            System.out.println("\nTesting " + connCount + " concurrent connections...");
            benchmarkConcurrentCount(connCount);
        }
    }

    private void benchmarkConcurrentCount(int connectionCount) {
        ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
        List<Future<ConnectionResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit connection tasks
        for (int i = 0; i < connectionCount; i++) {
            final int clientId = i;
            Future<ConnectionResult> future = executor.submit(() -> {
                return testSingleConnection(clientId);
            });
            futures.add(future);
        }

        // Collect results
        int successful = 0;
        int failed = 0;
        long totalConnectionTime = 0;
        long totalOperationTime = 0;

        for (Future<ConnectionResult> future : futures) {
            try {
                ConnectionResult result = future.get(30, TimeUnit.SECONDS);
                if (result.success) {
                    successful++;
                    totalConnectionTime += result.connectionTime;
                    totalOperationTime += result.operationTime;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                System.err.println("Connection task failed: " + e.getMessage());
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.printf("  Results: %d successful, %d failed%n", successful, failed);
        System.out.printf("  Total time: %d ms%n", totalTime);
        if (successful > 0) {
            System.out.printf("  Avg connection time: %.2f ms%n", (double) totalConnectionTime / successful);
            System.out.printf("  Avg operation time: %.2f ms%n", (double) totalOperationTime / successful);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private ConnectionResult testSingleConnection(int clientId) {
        FTPClient client = new FTPClient();
        ConnectionResult result = new ConnectionResult();

        try {
            // Test connection
            long connStart = System.currentTimeMillis();
            boolean connected = client.connect(TEST_SERVER, TEST_PORT);
            boolean authenticated = connected && client.login(TEST_USER, TEST_PASS);
            long connEnd = System.currentTimeMillis();

            result.connectionTime = connEnd - connStart;

            if (authenticated) {
                // Perform basic operations
                long opStart = System.currentTimeMillis();

                String currentDir = client.getCurrentDirectory();
                String listing = client.listDirectory(null);

                // Create a small test file
                String testFileName = "concurrent-test-" + clientId + ".txt";
                Path localFile = Paths.get(FTPConfig.getInstance().getClientRootDirectory(), testFileName);
                Files.write(localFile, ("Test from client " + clientId).getBytes());

                boolean uploaded = client.uploadFile(testFileName, testFileName);
                if (uploaded) {
                    client.deleteFile(testFileName);
                }

                Files.deleteIfExists(localFile);

                long opEnd = System.currentTimeMillis();
                result.operationTime = opEnd - opStart;
                result.success = true;
            }

        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }

        return result;
    }

    private static class ConnectionResult {
        boolean success = false;
        long connectionTime = 0;
        long operationTime = 0;
        String error = null;
    }

    @Test
    void benchmarkThroughput() {
        System.out.println("\n=== Throughput Benchmark ===");

        FTPClient client = new FTPClient();

        try {
            if (!client.connect(TEST_SERVER, TEST_PORT) || !client.login(TEST_USER, TEST_PASS)) {
                System.err.println("Failed to connect/authenticate");
                return;
            }

            // Test sustained throughput with multiple large files
            int fileSize = 5 * 1024 * 1024; // 5MB
            int fileCount = 5;

            System.out.println("Testing sustained throughput with " + fileCount + " x 5MB files...");

            byte[] testData = new byte[fileSize];
            for (int i = 0; i < fileSize; i++) {
                testData[i] = (byte) (i % 256);
            }

            long totalUploadTime = 0;
            long totalDownloadTime = 0;
            long totalBytes = (long) fileSize * fileCount;

            for (int i = 0; i < fileCount; i++) {
                String fileName = "throughput-test-" + i + ".dat";
                Path localFile = Paths.get(FTPConfig.getInstance().getClientRootDirectory(), fileName);
                Files.write(localFile, testData);

                // Upload
                long uploadStart = System.nanoTime();
                boolean uploaded = client.uploadFile(fileName, fileName);
                long uploadEnd = System.nanoTime();

                if (uploaded) {
                    totalUploadTime += (uploadEnd - uploadStart);

                    // Download
                    Files.delete(localFile);
                    long downloadStart = System.nanoTime();
                    boolean downloaded = client.downloadFile(fileName, fileName);
                    long downloadEnd = System.nanoTime();

                    if (downloaded) {
                        totalDownloadTime += (downloadEnd - downloadStart);
                    }

                    // Cleanup
                    client.deleteFile(fileName);
                    Files.deleteIfExists(localFile);
                }

                System.out.printf("  File %d/%d completed%n", i + 1, fileCount);
            }

            // Calculate throughput
            double uploadThroughput = (totalBytes / 1024.0 / 1024.0) / (totalUploadTime / 1_000_000_000.0); // MB/s
            double downloadThroughput = (totalBytes / 1024.0 / 1024.0) / (totalDownloadTime / 1_000_000_000.0); // MB/s

            System.out.printf("  Upload throughput: %.2f MB/s%n", uploadThroughput);
            System.out.printf("  Download throughput: %.2f MB/s%n", downloadThroughput);

        } catch (IOException e) {
            System.err.println("Throughput benchmark failed: " + e.getMessage());
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }

    @Test
    void benchmarkServerLoad() {
        System.out.println("\n=== Server Load Benchmark ===");

        // This test simulates realistic server load
        int concurrentUsers = 10;
        int operationsPerUser = 20;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<LoadTestResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<LoadTestResult> future = executor.submit(() -> {
                return simulateUserLoad(userId, operationsPerUser);
            });
            futures.add(future);
        }

        // Collect results
        int totalOperations = 0;
        int successfulOperations = 0;
        long totalOperationTime = 0;

        for (Future<LoadTestResult> future : futures) {
            try {
                LoadTestResult result = future.get(120, TimeUnit.SECONDS);
                totalOperations += result.totalOperations;
                successfulOperations += result.successfulOperations;
                totalOperationTime += result.totalTime;
            } catch (Exception e) {
                System.err.println("Load test task failed: " + e.getMessage());
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.printf("  Total operations: %d%n", totalOperations);
        System.out.printf("  Successful operations: %d%n", successfulOperations);
        System.out.printf("  Success rate: %.2f%%%n", (double) successfulOperations / totalOperations * 100);
        System.out.printf("  Total test time: %d ms%n", totalTime);
        System.out.printf("  Operations per second: %.2f%n", (double) successfulOperations / (totalTime / 1000.0));

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private LoadTestResult simulateUserLoad(int userId, int operationCount) {
        LoadTestResult result = new LoadTestResult();
        FTPClient client = new FTPClient();

        try {
            if (!client.connect(TEST_SERVER, TEST_PORT) || !client.login(TEST_USER, TEST_PASS)) {
                return result;
            }

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < operationCount; i++) {
                result.totalOperations++;

                try {
                    // Mix of different operations
                    switch (i % 6) {
                        case 0:
                            // Directory listing
                            if (client.listDirectory(null) != null) {
                                result.successfulOperations++;
                            }
                            break;
                        case 1:
                            // Get current directory
                            if (client.getCurrentDirectory() != null) {
                                result.successfulOperations++;
                            }
                            break;
                        case 2:
                            // Create and remove directory
                            String dirName = "load-test-" + userId + "-" + i;
                            if (client.createDirectory(dirName) && client.removeDirectory(dirName)) {
                                result.successfulOperations++;
                            }
                            break;
                        case 3:
                        case 4:
                            // File upload and download
                            String fileName = "load-test-" + userId + "-" + i + ".txt";
                            Path localFile = Paths.get(FTPConfig.getInstance().getClientRootDirectory(), fileName);
                            Files.write(localFile, ("Load test data " + userId + "-" + i).getBytes());

                            if (client.uploadFile(fileName, fileName)) {
                                Files.delete(localFile);
                                if (client.downloadFile(fileName, fileName) && client.deleteFile(fileName)) {
                                    result.successfulOperations++;
                                }
                            }
                            Files.deleteIfExists(localFile);
                            break;
                        case 5:
                            // NOOP command
                            if (client.noop()) {
                                result.successfulOperations++;
                            }
                            break;
                    }

                    // Small delay between operations
                    Thread.sleep(50);

                } catch (Exception e) {
                    // Operation failed, continue with next
                }
            }

            result.totalTime = System.currentTimeMillis() - startTime;

        } catch (Exception e) {
            // Connection failed
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }

        return result;
    }

    private static class LoadTestResult {
        int totalOperations = 0;
        int successfulOperations = 0;
        long totalTime = 0;
    }
}