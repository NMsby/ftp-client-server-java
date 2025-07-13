package client.gui.models;

import javafx.beans.property.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing a file item in the GUI
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FileItem {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Icons for different file types
    private static Image folderIcon;
    private static Image fileIcon;
    private static Image parentIcon;

    static {
        try {
            folderIcon = new Image(FileItem.class.getResourceAsStream("/images/folder.png"));
            fileIcon = new Image(FileItem.class.getResourceAsStream("/images/file.png"));
            parentIcon = new Image(FileItem.class.getResourceAsStream("/images/parent.png"));
        } catch (Exception e) {
            // Use default icons if images not found
            folderIcon = null;
            fileIcon = null;
            parentIcon = null;
        }
    }

    private final StringProperty name;
    private final LongProperty size;
    private final StringProperty formattedSize;
    private final ObjectProperty<LocalDateTime> lastModified;
    private final StringProperty formattedDate;
    private final BooleanProperty isDirectory;
    private final BooleanProperty isParentDirectory;
    private final StringProperty permissions;
    private final ObjectProperty<ImageView> icon;

    /**
     * Constructor for regular files and directories
     */
    public FileItem(String name, long size, LocalDateTime lastModified, boolean isDirectory, String permissions) {
        this.name = new SimpleStringProperty(name);
        this.size = new SimpleLongProperty(size);
        this.formattedSize = new SimpleStringProperty(formatFileSize(size));
        this.lastModified = new SimpleObjectProperty<>(lastModified);
        this.formattedDate = new SimpleStringProperty(lastModified != null ? lastModified.format(DATE_FORMATTER) : "");
        this.isDirectory = new SimpleBooleanProperty(isDirectory);
        this.isParentDirectory = new SimpleBooleanProperty(false);
        this.permissions = new SimpleStringProperty(permissions);
        this.icon = new SimpleObjectProperty<>(createIcon());
    }

    /**
     * Constructor for parent directory entry
     */
    public static FileItem createParentDirectory() {
        FileItem item = new FileItem("..", 0, null, true, "dr-xr-xr-x");
        item.isParentDirectory.set(true);
        item.icon.set(createParentIcon());
        return item;
    }

    /**
     * Create appropriate icon for this file item
     */
    private ImageView createIcon() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(16);
        imageView.setFitHeight(16);
        imageView.setPreserveRatio(true);

        if (isDirectory.get()) {
            if (folderIcon != null) {
                imageView.setImage(folderIcon);
            }
        } else {
            if (fileIcon != null) {
                imageView.setImage(fileIcon);
            }
        }

        return imageView;
    }

    /**
     * Create parent directory icon
     */
    private static ImageView createParentIcon() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(16);
        imageView.setFitHeight(16);
        imageView.setPreserveRatio(true);

        if (parentIcon != null) {
            imageView.setImage(parentIcon);
        }

        return imageView;
    }

    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (isDirectory.get() && !isParentDirectory.get()) {
            return "<DIR>";
        }

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

    // Property getters
    public StringProperty nameProperty() { return name; }
    public LongProperty sizeProperty() { return size; }
    public StringProperty formattedSizeProperty() { return formattedSize; }
    public ObjectProperty<LocalDateTime> lastModifiedProperty() { return lastModified; }
    public StringProperty formattedDateProperty() { return formattedDate; }
    public BooleanProperty isDirectoryProperty() { return isDirectory; }
    public BooleanProperty isParentDirectoryProperty() { return isParentDirectory; }
    public StringProperty permissionsProperty() { return permissions; }
    public ObjectProperty<ImageView> iconProperty() { return icon; }

    // Value getters
    public String getName() { return name.get(); }
    public long getSize() { return size.get(); }
    public String getFormattedSize() { return formattedSize.get(); }
    public LocalDateTime getLastModified() { return lastModified.get(); }
    public String getFormattedDate() { return formattedDate.get(); }
    public boolean isDirectory() { return isDirectory.get(); }
    public boolean isParentDirectory() { return isParentDirectory.get(); }
    public String getPermissions() { return permissions.get(); }
    public ImageView getIcon() { return icon.get(); }

    // Value setters
    public void setName(String name) { this.name.set(name); }
    public void setSize(long size) {
        this.size.set(size);
        this.formattedSize.set(formatFileSize(size));
    }
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified.set(lastModified);
        this.formattedDate.set(lastModified != null ? lastModified.format(DATE_FORMATTER) : "");
    }
    public void setIsDirectory(boolean isDirectory) {
        this.isDirectory.set(isDirectory);
        this.icon.set(createIcon());
    }
    public void setPermissions(String permissions) { this.permissions.set(permissions); }

    @Override
    public String toString() {
        return getName();
    }
}