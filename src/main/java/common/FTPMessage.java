package common;

import java.io.Serializable;

/**
 * Represents an FTP protocol message (command or response)
 * Used for communication between client and server
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String rawMessage;
    private final FTPCommand command;
    private final String[] parameters;
    private final boolean isCommand;

    /**
     * Constructor for command messages
     * @param command FTP command
     * @param parameters Command parameters
     */
    public FTPMessage(FTPCommand command, String... parameters) {
        this.command = command;
        this.parameters = parameters != null ? parameters.clone() : new String[0];
        this.isCommand = true;
        this.rawMessage = buildCommandString();
    }

    /**
     * Constructor for raw message parsing
     * @param rawMessage Raw message string from client/server
     */
    public FTPMessage(String rawMessage) {
        this.rawMessage = rawMessage != null ? rawMessage.trim() : "";
        this.isCommand = true;

        // Parse the raw message
        String[] parts = this.rawMessage.split("\\s+", 2);
        if (parts.length > 0) {
            this.command = FTPCommand.fromString(parts[0]);
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                // Split parameters by space, but preserve quoted strings
                this.parameters = parseParameters(parts[1]);
            } else {
                this.parameters = new String[0];
            }
        } else {
            this.command = FTPCommand.UNKNOWN;
            this.parameters = new String[0];
        }
    }

    /**
     * Parse parameters from command string, handling quoted strings
     * @param paramString Parameter string
     * @return Array of parameters
     */
    private String[] parseParameters(String paramString) {
        if (paramString == null || paramString.trim().isEmpty()) {
            return new String[0];
        }

        // Simple parsing - can be enhanced for complex scenarios
        String trimmed = paramString.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            // Handle quoted parameter
            return new String[]{trimmed.substring(1, trimmed.length() - 1)};
        } else {
            // Split by spaces
            return trimmed.split("\\s+");
        }
    }

    /**
     * Build command string from command and parameters
     * @return Formatted command string
     */
    private String buildCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append(command.getCommand());

        for (String param : parameters) {
            sb.append(" ");
            if (param.contains(" ")) {
                sb.append("\"").append(param).append("\"");
            } else {
                sb.append(param);
            }
        }

        return sb.toString();
    }

    /**
     * Get the FTP command
     * @return FTP command enum
     */
    public FTPCommand getCommand() {
        return command;
    }

    /**
     * Get command parameters
     * @return Array of parameters
     */
    public String[] getParameters() {
        return parameters.clone();
    }

    /**
     * Get specific parameter by index
     * @param index Parameter index
     * @return Parameter value or null if index out of bounds
     */
    public String getParameter(int index) {
        if (index >= 0 && index < parameters.length) {
            return parameters[index];
        }
        return null;
    }

    /**
     * Get first parameter (commonly used)
     * @return First parameter or null if no parameters
     */
    public String getFirstParameter() {
        return getParameter(0);
    }

    /**
     * Get number of parameters
     * @return Parameter count
     */
    public int getParameterCount() {
        return parameters.length;
    }

    /**
     * Check if message has parameters
     * @return true if message has parameters
     */
    public boolean hasParameters() {
        return parameters.length > 0;
    }

    /**
     * Get raw message string
     * @return Raw message
     */
    public String getRawMessage() {
        return rawMessage;
    }

    /**
     * Check if this is a command message
     * @return true if command message
     */
    public boolean isCommand() {
        return isCommand;
    }

    /**
     * Validate message format and requirements
     * @return true if message is valid
     */
    public boolean isValid() {
        if (command == FTPCommand.UNKNOWN) {
            return false;
        }

        // Check if command requires parameters but none provided
        if (command.requiresParameters() && parameters.length == 0) {
            return false;
        }

        return true;
    }

    /**
     * Get formatted message for transmission
     * @return Formatted message with CRLF
     */
    public String getFormattedMessage() {
        return rawMessage + "\r\n";
    }

    @Override
    public String toString() {
        return rawMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FTPMessage that = (FTPMessage) obj;
        return rawMessage.equals(that.rawMessage);
    }

    @Override
    public int hashCode() {
        return rawMessage.hashCode();
    }
}