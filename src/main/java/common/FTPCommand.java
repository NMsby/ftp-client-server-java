package common;

/**
 * Enumeration of supported FTP commands
 * Based on RFC 959 FTP Protocol Specification
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public enum FTPCommand {
    // Authentication Commands
    USER("USER", "Specify username for authentication"),
    PASS("PASS", "Specify password for authentication"),

    // Directory Navigation Commands
    LIST("LIST", "List directory contents"),
    CWD("CWD", "Change working directory"),
    PWD("PWD", "Print working directory"),
    MKD("MKD", "Create directory"),
    RMD("RMD", "Remove directory"),

    // File Transfer Commands
    RETR("RETR", "Download file from server"),
    STOR("STOR", "Upload file to server"),
    DELE("DELE", "Delete file on server"),

    // File Information Commands
    SIZE("SIZE", "Get file size"),
    MDTM("MDTM", "Get file modification time"),

    // File Management Commands
    RNFR("RNFR", "Rename from (source filename)"),
    RNTO("RNTO", "Rename to (destination filename)"),

    // Connection Commands
    QUIT("QUIT", "Terminate connection gracefully"),
    NOOP("NOOP", "No operation (keep-alive)"),

    // Advanced Directory Commands
    MLST("MLST", "Machine readable file listing"),
    MLSD("MLSD", "Machine readable directory listing"),

    // Feature and Options Commands
    FEAT("FEAT", "List server features"),
    OPTS("OPTS", "Set options"),

    // Status Commands
    STAT("STAT", "Status information"),

    // Transfer Mode Commands
    TYPE("TYPE", "Set transfer type"),
    MODE("MODE", "Set transfer mode"),

    // Connection Mode Commands
    PASV("PASV", "Enter passive mode"),
    PORT("PORT", "Specify client data port"),

    // Advanced File Commands
    REST("REST", "Restart file transfer"),
    ABOR("ABOR", "Abort transfer"),

    // Help Commands
    HELP("HELP", "Help information"),

    // System Commands
    SYST("SYST", "System type"),

    // Unknown command for error handling
    UNKNOWN("UNKNOWN", "Unknown or unsupported command");

    private final String command;
    private final String description;

    /**
     * Constructor for FTP command enum
     * @param command The command string
     * @param description Description of the command
     */
    FTPCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }

    /**
     * Get the command string
     * @return Command string
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get command description
     * @return Command description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse command string to FTPCommand enum
     * @param commandStr Command string from client
     * @return Corresponding FTPCommand enum or UNKNOWN if not found
     */
    public static FTPCommand fromString(String commandStr) {
        if (commandStr == null || commandStr.trim().isEmpty()) {
            return UNKNOWN;
        }

        String upperCommand = commandStr.trim().toUpperCase();
        for (FTPCommand cmd : FTPCommand.values()) {
            if (cmd.getCommand().equals(upperCommand)) {
                return cmd;
            }
        }
        return UNKNOWN;
    }

    /**
     * Check if command requires authentication
     * @return true if command requires authentication
     */
    public boolean requiresAuthentication() {
        return this != USER && this != PASS && this != QUIT && this != SYST && this != FEAT;
    }

    /**
     * Check if command requires parameters
     * @return true if command typically requires parameters
     */
    public boolean requiresParameters() {
        return this == USER || this == PASS || this == CWD || this == MKD ||
                this == RMD || this == RETR || this == STOR || this == DELE ||
                this == SIZE || this == MDTM || this == RNFR || this == RNTO;
    }

    @Override
    public String toString() {
        return command;
    }
}