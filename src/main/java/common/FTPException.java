package common;

/**
 * Base exception class for FTP-related errors
 * Provides structured error handling for FTP operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPException extends Exception {
    private static final long serialVersionUID = 1L;

    private final FTPResponse responseCode;

    /**
     * Constructor with message only
     * @param message Error message
     */
    public FTPException(String message) {
        super(message);
        this.responseCode = null;
    }

    /**
     * Constructor with message and cause
     * @param message Error message
     * @param cause Underlying cause
     */
    public FTPException(String message, Throwable cause) {
        super(message, cause);
        this.responseCode = null;
    }

    /**
     * Constructor with FTP response code and message
     * @param responseCode FTP response code
     * @param message Error message
     */
    public FTPException(FTPResponse responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    /**
     * Constructor with FTP response code, message, and cause
     * @param responseCode FTP response code
     * @param message Error message
     * @param cause Underlying cause
     */
    public FTPException(FTPResponse responseCode, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    /**
     * Get associated FTP response code
     * @return FTP response code or null if not set
     */
    public FTPResponse getResponseCode() {
        return responseCode;
    }

    /**
     * Check if exception has an associated response code
     * @return true if response code is set
     */
    public boolean hasResponseCode() {
        return responseCode != null;
    }

    /**
     * Get formatted error response for FTP protocol
     * @return Formatted FTP response string
     */
    public String getFormattedResponse() {
        if (responseCode != null) {
            return responseCode.getFormattedResponse(getMessage());
        } else {
            return FTPResponse.ACTION_ABORTED.getFormattedResponse(getMessage());
        }
    }
}