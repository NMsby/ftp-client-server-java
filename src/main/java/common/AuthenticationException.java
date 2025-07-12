package common;

/**
 * Exception thrown for authentication-related errors
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class AuthenticationException extends FTPException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message
     * @param message Error message
     */
    public AuthenticationException(String message) {
        super(FTPResponse.NOT_LOGGED_IN, message);
    }

    /**
     * Constructor with message and cause
     * @param message Error message
     * @param cause Underlying cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(FTPResponse.NOT_LOGGED_IN, message, cause);
    }
}