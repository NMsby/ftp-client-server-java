package utils;

import common.FileInfo;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file operations
 * Provides common file handling functionality for FTP operations
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FileUtils {

    /**
     * Check if path is safe (prevents directory traversal attacks)
     * @param basePath Base directory path
     * @param requestedPath Requested file/directory path
     * @return true if path is safe
     */
    public static boolean isSafePath(String basePath, String requestedPath) {
        try {
            Path base = Paths.get(basePath).normalize().toAbsolutePath();
            Path requested = base.resolve(requestedPath).normalize().toAbsolutePath();
            return requested.startsWith(base);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve path relative to base directory
     * @param basePath Base directory
     * @param relativePath Relative path
     * @return Resolved absolute path
     * @throws IOException if path resolution fails
     */
    public static Path resolvePath(String basePath, String relativePath) throws IOException {
        Path base = Paths.get(basePath).normalize().toAbsolutePath();
        Path resolved = base.resolve(relativePath).normalize().toAbsolutePath();

        if (!resolved.startsWith(base)) {
            throw new IOException("Access denied: Path outside base directory");
        }

        return resolved;
    }

    /**
     * Get file information for a given path
     * @param filePath Path to file or directory
     * @return FileInfo object or null if file doesn't exist
     */
    public static FileInfo getFileInfo(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return null;
            }

            String name = filePath.getFileName().toString();
            String path = filePath.toString();
            long size = Files.isDirectory(filePath) ? 0 : Files.size(filePath);

            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(filePath).toInstant(),
                    ZoneId.systemDefault()
            );

            boolean isDirectory = Files.isDirectory(filePath);
            boolean isReadable = Files.isReadable(filePath);
            boolean isWritable = Files.isWritable(filePath);

            return new FileInfo(name, path, size, lastModified, isDirectory, isReadable, isWritable);

        } catch (IOException e) {
            System.err.println("Error getting file info for " + filePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * List directory contents
     * @param directoryPath Path to directory
     * @return List of FileInfo objects
     * @throws IOException if directory cannot be read
     */
    public static List<FileInfo> listDirectory(Path directoryPath) throws IOException {
        List<FileInfo> fileList = new ArrayList<>();

        if (!Files.exists(directoryPath)) {
            throw new IOException("Directory does not exist: " + directoryPath);
        }

        if (!Files.isDirectory(directoryPath)) {
            throw new IOException("Path is not a directory: " + directoryPath);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath)) {
            for (Path entry : stream) {
                FileInfo fileInfo = getFileInfo(entry);
                if (fileInfo != null) {
                    fileList.add(fileInfo);
                }
            }
        }

        return fileList;
    }

    /**
     * Create directory if it doesn't exist
     * @param directoryPath Path to directory
     * @throws IOException if directory creation fails
     */
    public static void ensureDirectoryExists(Path directoryPath) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
    }

    /**
     * Copy file with progress callback
     * @param source Source file path
     * @param target Target file path
     * @param progressCallback Progress callback (can be null)
     * @throws IOException if copy fails
     */
    public static void copyFile(Path source, Path target, ProgressCallback progressCallback) throws IOException {
        long fileSize = Files.size(source);
        long bytesTransferred = 0;

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;

                if (progressCallback != null) {
                    progressCallback.onProgress(bytesTransferred, fileSize);
                }
            }
        }
    }

    /**
     * Delete file or directory
     * @param path Path to delete
     * @throws IOException if deletion fails
     */
    public static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            // Delete directory contents first
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    delete(entry);
                }
            }
        }
        Files.delete(path);
    }

    /**
     * Check if filename is valid
     * @param filename Filename to validate
     * @return true if filename is valid
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        // Check for invalid characters (Windows + Unix)
        String invalidChars = "<>:\"/\\|?*";
        for (char c : invalidChars.toCharArray()) {
            if (filename.indexOf(c) >= 0) {
                return false;
            }
        }

        // Check for reserved names (Windows)
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
                "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2",
                "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

        String upperFilename = filename.toUpperCase();
        for (String reserved : reservedNames) {
            if (upperFilename.equals(reserved) || upperFilename.startsWith(reserved + ".")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get file extension
     * @param filename Filename
     * @return File extension (without dot) or empty string if no extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }

        return "";
    }

    /**
     * Format file size in human-readable format
     * @param bytes Size in bytes
     * @return Formatted size string
     */
    public static String formatFileSize(long bytes) {
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
     * Convert Windows path separators to Unix style
     * @param path Input path
     * @return Normalized path with forward slashes
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace('\\', '/');
    }

    /**
     * Join path components
     * @param components Path components
     * @return Joined path
     */
    public static String joinPath(String... components) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            if (components[i] != null && !components[i].isEmpty()) {
                if (result.length() > 0 && !result.toString().endsWith("/")) {
                    result.append("/");
                }
                result.append(components[i]);
            }
        }
        return result.toString();
    }

    /**
     * Progress callback interface for file operations
     */
    public interface ProgressCallback {
        void onProgress(long bytesTransferred, long totalBytes);
    }
}