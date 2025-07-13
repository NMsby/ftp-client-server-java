package client.gui.utils;

import client.FTPClient;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Background task for file transfers with progress tracking
 *
 * @author  Nelson Masbayi
 * @version 1.0
 */
public class FileTransferTask extends Task<Boolean> {
    private static final Logger logger = LogManager.getLogger("client");

    public enum TransferType {
        UPLOAD("Uploading"),
        DOWNLOAD("Downloading");

        private final String displayName;

        TransferType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final TransferType transferType;
    private final FTPClient ftpClient;
    private final String localFileName;
    private final String remoteFileName;
    private final long fileSize;

    private long bytesTransferred = 0;
    private long startTime;

    /**
     * Constructor
     * @param transferType Type of transfer (upload/download)
     * @param ftpClient FTP client instance
     * @param localFileName Local file name
     * @param remoteFileName Remote file name
     * @param fileSize File size for progress calculation
     */
    public FileTransferTask(TransferType transferType, FTPClient ftpClient,
                            String localFileName, String remoteFileName, long fileSize) {
        this.transferType = transferType;
        this.ftpClient = ftpClient;
        this.localFileName = localFileName;
        this.remoteFileName = remoteFileName;
        this.fileSize = Math.max(fileSize, 1); // Avoid division by zero
    }

    @Override
    protected Boolean call() throws Exception {
        startTime = System.currentTimeMillis();

        try {
            updateMessage(transferType.getDisplayName() + " " + getFileName() + "...");
            updateProgress(0, fileSize);

            boolean result;
            switch (transferType) {
                case UPLOAD:
                    result = ftpClient.uploadFile(localFileName, remoteFileName);
                    break;
                case DOWNLOAD:
                    result = ftpClient.downloadFile(remoteFileName, localFileName);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown transfer type: " + transferType);
            }

            if (result) {
                updateProgress(fileSize, fileSize);
                long duration = System.currentTimeMillis() - startTime;
                double speed = duration > 0 ? (fileSize / 1024.0) / (duration / 1000.0) : 0;
                updateMessage(String.format("%s completed (%.1f KB/s)",
                        transferType.getDisplayName(), speed));
            } else {
                updateMessage(transferType.getDisplayName() + " failed");
            }

            return result;

        } catch (Exception e) {
            logger.error("Error during file transfer", e);
            updateMessage(transferType.getDisplayName() + " error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Simulate progress updates for demonstration
     * In a real implementation, this would be called by the FTP client
     */
    private void simulateProgress() {
        // This is a simplified simulation
        // In a real implementation, the FTP client would call updateProgress
        for (int i = 0; i <= 100; i += 10) {
            if (isCancelled()) {
                break;
            }

            bytesTransferred = (fileSize * i) / 100;
            updateProgress(bytesTransferred, fileSize);
            updateMessage(String.format("%s %s... %d%%",
                    transferType.getDisplayName(), getFileName(), i));

            try {
                Thread.sleep(100); // Simulate transfer time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Get the filename being transferred
     */
    private String getFileName() {
        return transferType == TransferType.UPLOAD ? localFileName : remoteFileName;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("File transfer completed: {} {}", transferType.getDisplayName(), getFileName());
    }

    @Override
    protected void failed() {
        super.failed();
        logger.error("File transfer failed: {} {}", transferType.getDisplayName(), getFileName());
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage(transferType.getDisplayName() + " cancelled");
        logger.info("File transfer cancelled: {} {}", transferType.getDisplayName(), getFileName());
    }
}