package client.gui.utils;

/**
 * Interface for file transfer progress callbacks
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
@FunctionalInterface
public interface ProgressCallback {
    /**
     * Called when transfer progress is updated
     * @param bytesTransferred Number of bytes transferred
     * @param totalBytes Total bytes to transfer
     */
    void onProgress(long bytesTransferred, long totalBytes);
}