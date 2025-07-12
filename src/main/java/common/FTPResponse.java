package common;

/**
 * Enumeration of FTP response codes and messages
 * Based on RFC 959 FTP Protocol Specification
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public enum FTPResponse {
    // 1xx - Positive Preliminary Reply

    // 2xx - Positive Completion Reply
    COMMAND_OK(200, "Command okay"),
    SYSTEM_STATUS(211, "System status, or system help reply"),
    DIRECTORY_STATUS(212, "Directory status"),
    FILE_STATUS(213, "File status"),
    HELP_MESSAGE(214, "Help message"),
    SYSTEM_TYPE(215, "NAME system type"),
    SERVICE_READY(220, "Service ready for new user"),
    SERVICE_CLOSING(221, "Service closing control connection"),
    DATA_CONNECTION_OPEN(225, "Data connection open; no transfer in progress"),
    CLOSING_DATA_CONNECTION(226, "Closing data connection. Requested file action successful"),
    ENTERING_PASSIVE_MODE(227, "Entering Passive Mode"),
    USER_LOGGED_IN(230, "User logged in, proceed"),
    FILE_ACTION_OK(250, "Requested file action okay, completed"),
    PATHNAME_CREATED(257, "PATHNAME created"),

    // 3xx - Positive Intermediate Reply
    USERNAME_OK(331, "User name okay, need password"),
    NEED_ACCOUNT(332, "Need account for login"),
    FILE_ACTION_PENDING(350, "Requested file action pending further information"),

    // 4xx - Transient Negative Completion Reply
    SERVICE_NOT_AVAILABLE(421, "Service not available, closing control connection"),
    CANNOT_OPEN_DATA_CONNECTION(425, "Can't open data connection"),
    CONNECTION_CLOSED(426, "Connection closed; transfer aborted"),
    FILE_ACTION_NOT_TAKEN(450, "Requested file action not taken"),
    ACTION_ABORTED(451, "Requested action aborted: local error in processing"),
    INSUFFICIENT_STORAGE(452, "Requested action not taken. Insufficient storage space in system"),

    // 5xx - Permanent Negative Completion Reply
    SYNTAX_ERROR_COMMAND(500, "Syntax error, command unrecognized"),
    SYNTAX_ERROR_PARAMETERS(501, "Syntax error in parameters or arguments"),
    COMMAND_NOT_IMPLEMENTED(502, "Command not implemented"),
    BAD_COMMAND_SEQUENCE(503, "Bad sequence of commands"),
    PARAMETER_NOT_IMPLEMENTED(504, "Command not implemented for that parameter"),
    NOT_LOGGED_IN(530, "Not logged in"),
    NEED_ACCOUNT_FOR_STORING(532, "Need account for storing files"),
    FILE_UNAVAILABLE(550, "Requested action not taken. File unavailable"),
    PAGE_TYPE_UNKNOWN(551, "Requested action aborted. Page type unknown"),
    EXCEEDED_STORAGE(552, "Requested file action aborted. Exceeded storage allocation"),
    FILE_NAME_NOT_ALLOWED(553, "Requested action not taken. File name not allowed");

    private final int code;
    private final String message;

    /**
     * Constructor for FTP response enum
     * @param code Response code
     * @param message Response message
     */
    FTPResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Get response code
     * @return Response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get response message
     * @return Response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Create formatted response string
     * @return Formatted response string
     */
    public String getFormattedResponse() {
        return code + " " + message;
    }

    /**
     * Create formatted response string with custom message
     * @param customMessage Custom message to append
     * @return Formatted response string
     */
    public String getFormattedResponse(String customMessage) {
        if (customMessage == null || customMessage.trim().isEmpty()) {
            return getFormattedResponse();
        }
        return code + " " + customMessage;
    }

    /**
     * Check if response indicates success (2xx codes)
     * @return true if response is successful
     */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }

    /**
     * Check if response indicates error (4xx or 5xx codes)
     * @return true if response is an error
     */
    public boolean isError() {
        return code >= 400;
    }

    /**
     * Check if response is intermediate (3xx codes)
     * @return true if response is intermediate
     */
    public boolean isIntermediate() {
        return code >= 300 && code < 400;
    }

    /**
     * Find response by code
     * @param code Response code to search for
     * @return FTPResponse enum or null if not found
     */
    public static FTPResponse fromCode(int code) {
        for (FTPResponse response : FTPResponse.values()) {
            if (response.getCode() == code) {
                return response;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getFormattedResponse();
    }
}