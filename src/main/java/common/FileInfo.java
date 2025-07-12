package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents file/directory metadata information
 * Used for file listings and information commands
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String path;
    private final long size;
    private final LocalDateTime lastModified;
    private final boolean isDirectory;
    private final boolean isReadable;
    private final boolean isWritable;
    private final String permissions;

    /**
     * Constructor for FileInfo
     * @param name File/directory name
     * @param path Full path
     * @param size File size in bytes (0 for directories)
     * @param lastModified Last modification time
     * @param isDirectory True if this is a directory
     * @param isReadable True if readable
     * @param isWritable True if writable
     */
    public FileInfo(String name, String path, long size, LocalDateTime lastModified,
                    boolean isDirectory, boolean isReadable, boolean isWritable) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
        this.permissions = buildPermissionString();
    }

    /**
     * Build permission string (Unix-style)
     * @return Permission string
     */
    private String buildPermissionString() {
        StringBuilder perm = new StringBuilder();

        // File type
        perm.append(isDirectory ? "d" : "-");

        // Owner permissions (simplified)
        perm.append(isReadable ? "r" : "-");
        perm.append(isWritable ? "w" : "-");
        perm.append(isWritable ? "x" : "-");

        // Group permissions (same as owner for simplicity)
        perm.append(isReadable ? "r" : "-");
        perm.append(isWritable ? "w" : "-");
        perm.append(isWritable ? "x" : "-");

        // Other permissions (read-only for simplicity)
        perm.append(isReadable ? "r" : "-");
        perm.append("--");

        return perm.toString();
    }

    /**
     * Get file/directory name
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get full path
     * @return Path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get file size in bytes
     * @return Size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Get last modified time
     * @return Last modified time
     */
    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Check if this is a directory
     * @return True if directory
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Check if file is readable
     * @return True if readable
     */
    public boolean isReadable() {
        return isReadable;
    }

    /**
     * Check if file is writable
     * @return True if writable
     */
    public boolean isWritable() {
        return isWritable;
    }

    /**
     * Get permission string
     * @return Permission string
     */
    public String getPermissions() {
        return permissions;
    }

    /**
     * Get human-readable file size
     * @return Formatted file size
     */
    public String getFormattedSize() {
        if (isDirectory) {
            return "<DIR>";
        }

        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Get formatted modification time
     * @return Formatted date/time string
     */
    public String getFormattedLastModified() {
        return lastModified.format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm"));
    }

    /**
     * Generate LIST command response format
     * @return LIST format string
     */
    public String toListFormat() {
        return String.format("%s %8s %s %s",
                permissions,
                isDirectory ? "<DIR>" : String.valueOf(size),
                getFormattedLastModified(),
                name);
    }

    /**
     * Generate detailed format for GUI display
     * @return Detailed format string
     */
    public String toDetailedFormat() {
        return String.format("Name: %s, Size: %s, Modified: %s, Type: %s",
                name,
                getFormattedSize(),
                getFormattedLastModified(),
                isDirectory ? "Directory" : "File");
    }

    @Override
    public String toString() {
        return toListFormat();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FileInfo fileInfo = (FileInfo) obj;
        return path.equals(fileInfo.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}