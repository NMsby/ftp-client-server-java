package client.gui.controllers;

import client.FTPClient;
import client.gui.models.ConnectionInfo;
import client.gui.models.FileItem;
import client.gui.utils.AlertUtils;
import client.gui.utils.FileTransferTask;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Main controller for the FTP Client GUI
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class MainController implements Initializable {
    private static final Logger logger = LogManager.getLogger("client");

    // FXML injected components
    @FXML
    private TextField hostnameField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox savePasswordCheck;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;

    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Label remotePathLabel;
    @FXML
    private Label localPathLabel;

    @FXML
    private TableView<FileItem> remoteFileTable;
    @FXML
    private TableColumn<FileItem, String> remoteNameColumn;
    @FXML
    private TableColumn<FileItem, String> remoteSizeColumn;
    @FXML
    private TableColumn<FileItem, String> remoteDateColumn;
    @FXML
    private TableColumn<FileItem, String> remotePermissionsColumn;

    @FXML
    private TableView<FileItem> localFileTable;
    @FXML
    private TableColumn<FileItem, String> localNameColumn;
    @FXML
    private TableColumn<FileItem, String> localSizeColumn;
    @FXML
    private TableColumn<FileItem, String> localDateColumn;

    @FXML
    private Button refreshRemoteButton;
    @FXML
    private Button refreshLocalButton;
    @FXML
    private Button uploadButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Button deleteRemoteButton;
    @FXML
    private Button deleteLocalButton;
    @FXML
    private Button newFolderRemoteButton;
    @FXML
    private Button newFolderLocalButton;

    @FXML
    private TextArea logTextArea;
    @FXML
    private ProgressBar transferProgressBar;
    @FXML
    private Label transferStatusLabel;
    @FXML
    private VBox transferProgressContainer;

    // Business objects
    private FTPClient ftpClient;
    private ConnectionInfo connectionInfo;
    private ObservableList<FileItem> remoteFiles;
    private ObservableList<FileItem> localFiles;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Main Controller");

        // Initialize business objects
        ftpClient = new FTPClient();
        connectionInfo = new ConnectionInfo();
        remoteFiles = FXCollections.observableArrayList();
        localFiles = FXCollections.observableArrayList();

        // Setup UI components
        setupConnectionPanel();
        setupFileTables();
        setupButtons();
        setupStatusLabels();
        setupProgressIndicators();

        // Load initial data
        refreshLocalFiles();

        logger.info("Main Controller initialized successfully");
    }

    /**
     * Setup connection panel bindings
     */
    private void setupConnectionPanel() {
        // Bind connection info to UI
        hostnameField.textProperty().bindBidirectional(connectionInfo.hostnameProperty());
        portField.textProperty().bindBidirectional(connectionInfo.portProperty(), new javafx.util.converter.NumberStringConverter());
        usernameField.textProperty().bindBidirectional(connectionInfo.usernameProperty());
        passwordField.textProperty().bindBidirectional(connectionInfo.passwordProperty());
        savePasswordCheck.selectedProperty().bindBidirectional(connectionInfo.savePasswordProperty());

        // Bind button states
        connectButton.disableProperty().bind(connectionInfo.connectedProperty());
        disconnectButton.disableProperty().bind(connectionInfo.connectedProperty().not());

        // Set initial values
        portField.setText("21");
    }

    /**
     * Setup file table configurations
     */
    private void setupFileTables() {
        // Setup remote file table
        remoteNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        remoteSizeColumn.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        remoteDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        remotePermissionsColumn.setCellValueFactory(new PropertyValueFactory<>("permissions"));

        remoteFileTable.setItems(remoteFiles);
        remoteFileTable.setRowFactory(tv -> createRemoteFileRow());

        // Setup local file table
        localNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        localSizeColumn.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        localDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));

        localFileTable.setItems(localFiles);
        localFileTable.setRowFactory(tv -> createLocalFileRow());

        // Enable multiple selection
        remoteFileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        localFileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Create remote file table row with double-click handling
     */
    private TableRow<FileItem> createRemoteFileRow() {
        TableRow<FileItem> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                FileItem item = row.getItem();
                if (item.isDirectory()) {
                    navigateRemoteDirectory(item.getName());
                } else {
                    downloadSelectedFiles();
                }
            }
        });
        return row;
    }

    /**
     * Create local file table row with double-click handling
     */
    private TableRow<FileItem> createLocalFileRow() {
        TableRow<FileItem> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                FileItem item = row.getItem();
                if (item.isDirectory()) {
                    navigateLocalDirectory(item.getName());
                } else {
                    uploadSelectedFiles();
                }
            }
        });
        return row;
    }

    /**
     * Setup button states and bindings
     */
    private void setupButtons() {
        // Remote operations require connection
        refreshRemoteButton.disableProperty().bind(connectionInfo.authenticatedProperty().not());
        deleteRemoteButton.disableProperty().bind(connectionInfo.authenticatedProperty().not());
        newFolderRemoteButton.disableProperty().bind(connectionInfo.authenticatedProperty().not());

        // Transfer operations require connection and selection
        uploadButton.disableProperty().bind(
                connectionInfo.authenticatedProperty().not()
                        .or(localFileTable.getSelectionModel().selectedItemProperty().isNull())
        );
        downloadButton.disableProperty().bind(
                connectionInfo.authenticatedProperty().not()
                        .or(remoteFileTable.getSelectionModel().selectedItemProperty().isNull())
        );
    }

    /**
     * Setup status labels
     */
    private void setupStatusLabels() {
        connectionStatusLabel.textProperty().bind(connectionInfo.connectedProperty()
                .asString("Connected: %s"));

        // Set initial paths
        updateLocalPath();
        updateRemotePath();
    }

    /**
     * Setup progress indicators
     */
    private void setupProgressIndicators() {
        transferProgressContainer.setVisible(false);
        transferProgressBar.setProgress(0);
        transferStatusLabel.setText("");
    }

    // Event handlers

    @FXML
    private void handleConnect() {
        if (!connectionInfo.isValid()) {
            AlertUtils.showError("Invalid Connection", "Please fill in all required fields.");
            return;
        }

        connectAsync();
    }

    @FXML
    private void handleDisconnect() {
        disconnectFromServer();
    }

    @FXML
    private void handleRefreshRemote() {
        refreshRemoteFiles();
    }

    @FXML
    private void handleRefreshLocal() {
        refreshLocalFiles();
    }

    @FXML
    private void handleUpload() {
        uploadSelectedFiles();
    }

    @FXML
    private void handleDownload() {
        downloadSelectedFiles();
    }

    @FXML
    private void handleDeleteRemote() {
        deleteSelectedRemoteFiles();
    }

    @FXML
    private void handleDeleteLocal() {
        deleteSelectedLocalFiles();
    }

    @FXML
    private void handleNewFolderRemote() {
        createRemoteFolder();
    }

    @FXML
    private void handleNewFolderLocal() {
        createLocalFolder();
    }

    @FXML
    private void handleClearLog() {
        logTextArea.clear();
    }

    // Business logic methods

    /**
     * Connect to FTP server asynchronously
     */
    private void connectAsync() {
        Task<Boolean> connectTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("Connecting to " + connectionInfo.getHostname() + "...");

                boolean connected = ftpClient.connect(connectionInfo.getHostname(), connectionInfo.getPort());
                if (!connected) {
                    return false;
                }

                updateMessage("Authenticating...");
                boolean authenticated = ftpClient.login(connectionInfo.getUsername(), connectionInfo.getPassword());
                return authenticated;
            }

            @Override
            protected void succeeded() {
                Boolean result = getValue();
                if (result) {
                    connectionInfo.setConnected(true);
                    connectionInfo.setAuthenticated(true);
                    appendLog("Connected and authenticated successfully");
                    refreshRemoteFiles();
                    updateRemotePath();
                } else {
                    AlertUtils.showError("Connection Failed", "Could not connect or authenticate to the server.");
                    appendLog("Connection failed");
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                String message = exception != null ? exception.getMessage() : "Unknown error";
                AlertUtils.showError("Connection Error", "Error connecting to server: " + message);
                appendLog("Connection error: " + message);
            }
        };

        connectTask.messageProperty().addListener((obs, oldMsg, newMsg) ->
                Platform.runLater(() -> appendLog(newMsg)));

        Thread connectThread = new Thread(connectTask);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    /**
     * Disconnect from FTP server
     */
    private void disconnectFromServer() {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
            appendLog("Disconnected from server");
        }

        connectionInfo.clearConnection();
        remoteFiles.clear();
        updateRemotePath();
    }

    /**
     * Refresh remote file listing
     */
    private void refreshRemoteFiles() {
        if (!ftpClient.isAuthenticated()) {
            return;
        }

        Task<String> listTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return ftpClient.listDirectory(null);
            }

            @Override
            protected void succeeded() {
                String listing = getValue();
                if (listing != null) {
                    parseRemoteFileListing(listing);
                    appendLog("Remote directory refreshed");
                    updateRemotePath();
                } else {
                    appendLog("Failed to refresh remote directory");
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                appendLog("Error refreshing remote files: " +
                        (exception != null ? exception.getMessage() : "Unknown error"));
            }
        };

        Thread listThread = new Thread(listTask);
        listThread.setDaemon(true);
        listThread.start();
    }

    /**
     * Refresh local file listing
     */
    private void refreshLocalFiles() {
        try {
            localFiles.clear();

            File currentDir = ftpClient.getCurrentLocalDirectory().toFile();
            File parentDir = currentDir.getParentFile();

            // Add parent directory entry if not at root
            if (parentDir != null) {
                localFiles.add(FileItem.createParentDirectory());
            }

            // Add files and directories
            File[] files = currentDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault()
                    );

                    FileItem item = new FileItem(
                            file.getName(),
                            file.length(),
                            lastModified,
                            file.isDirectory(),
                            file.canRead() ? "r" : "-" + file.canWrite() ? "w" : "-" + file.canExecute() ? "x" : "-"
                    );

                    localFiles.add(item);
                }
            }

            updateLocalPath();
            appendLog("Local directory refreshed");

        } catch (Exception e) {
            appendLog("Error refreshing local files: " + e.getMessage());
        }
    }

    /**
     * Parse remote file listing
     */
    private void parseRemoteFileListing(String listing) {
        remoteFiles.clear();

        // Add parent directory entry
        remoteFiles.add(FileItem.createParentDirectory());

        if (listing != null && !listing.trim().isEmpty()) {
            String[] lines = listing.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    // Parse Unix-style listing (simplified)
                    String[] parts = line.split("\\s+", 9);
                    if (parts.length >= 9) {
                        String permissions = parts[0];
                        boolean isDirectory = permissions.startsWith("d");
                        String name = parts[8];

                        // Skip . and .. entries
                        if (".".equals(name) || "..".equals(name)) {
                            continue;
                        }

                        long size = 0;
                        if (!isDirectory) {
                            try {
                                size = Long.parseLong(parts[4]);
                            } catch (NumberFormatException e) {
                                // Use 0 if size parsing fails
                            }
                        }

                        // Parse date (simplified - using current time for now)
                        LocalDateTime lastModified = LocalDateTime.now();

                        FileItem item = new FileItem(name, size, lastModified, isDirectory, permissions);
                        remoteFiles.add(item);
                    }
                } catch (Exception e) {
                    logger.debug("Error parsing file listing line: {}", line);
                }
            }
        }
    }

    /**
     * Navigate to remote directory
     */
    private void navigateRemoteDirectory(String directoryName) {
        if (!ftpClient.isAuthenticated()) {
            return;
        }

        Task<Boolean> navTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ftpClient.changeDirectory(directoryName);
            }

            @Override
            protected void succeeded() {
                Boolean result = getValue();
                if (result) {
                    refreshRemoteFiles();
                    appendLog("Changed to remote directory: " + directoryName);
                } else {
                    appendLog("Failed to change remote directory");
                }
            }
        };

        Thread navThread = new Thread(navTask);
        navThread.setDaemon(true);
        navThread.start();
    }

    /**
     * Navigate to local directory
     */
    private void navigateLocalDirectory(String directoryName) {
        try {
            if ("..".equals(directoryName)) {
                // Navigate to parent directory
                File currentDir = ftpClient.getCurrentLocalDirectory().toFile();
                File parentDir = currentDir.getParentFile();
                if (parentDir != null) {
                    ftpClient.setCurrentLocalDirectory(parentDir.getAbsolutePath());
                }
            } else {
                // Navigate to subdirectory
                File newDir = new File(ftpClient.getCurrentLocalDirectory().toFile(), directoryName);
                if (newDir.exists() && newDir.isDirectory()) {
                    ftpClient.setCurrentLocalDirectory(newDir.getAbsolutePath());
                }
            }

            refreshLocalFiles();
            appendLog("Changed to local directory: " + directoryName);

        } catch (Exception e) {
            appendLog("Error changing local directory: " + e.getMessage());
        }
    }

    /**
     * Upload selected files
     */
    private void uploadSelectedFiles() {
        List<FileItem> selectedItems = localFileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }

        for (FileItem item : selectedItems) {
            if (!item.isDirectory() && !item.isParentDirectory()) {
                uploadFile(item);
            }
        }
    }

    /**
     * Upload a single file
     */
    private void uploadFile(FileItem fileItem) {
        FileTransferTask uploadTask = new FileTransferTask(
                FileTransferTask.TransferType.UPLOAD,
                ftpClient,
                fileItem.getName(),
                fileItem.getName(),
                fileItem.getSize()
        );

        // Bind progress indicators
        transferProgressBar.progressProperty().bind(uploadTask.progressProperty());
        transferStatusLabel.textProperty().bind(uploadTask.messageProperty());
        transferProgressContainer.setVisible(true);

        uploadTask.setOnSucceeded(e -> {
            appendLog("Upload completed: " + fileItem.getName());
            refreshRemoteFiles();
            hideTransferProgress();
        });

        uploadTask.setOnFailed(e -> {
            Throwable exception = uploadTask.getException();
            appendLog("Upload failed: " + fileItem.getName() + " - " +
                    (exception != null ? exception.getMessage() : "Unknown error"));
            hideTransferProgress();
        });

        Thread uploadThread = new Thread(uploadTask);
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    /**
     * Download selected files
     */
    private void downloadSelectedFiles() {
        List<FileItem> selectedItems = remoteFileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }

        for (FileItem item : selectedItems) {
            if (!item.isDirectory() && !item.isParentDirectory()) {
                downloadFile(item);
            }
        }
    }

    /**
     * Download a single file
     */
    private void downloadFile(FileItem fileItem) {
        FileTransferTask downloadTask = new FileTransferTask(
                FileTransferTask.TransferType.DOWNLOAD,
                ftpClient,
                fileItem.getName(),
                fileItem.getName(),
                fileItem.getSize()
        );

        // Bind progress indicators
        transferProgressBar.progressProperty().bind(downloadTask.progressProperty());
        transferStatusLabel.textProperty().bind(downloadTask.messageProperty());
        transferProgressContainer.setVisible(true);

        downloadTask.setOnSucceeded(e -> {
            appendLog("Download completed: " + fileItem.getName());
            refreshLocalFiles();
            hideTransferProgress();
        });

        downloadTask.setOnFailed(e -> {
            Throwable exception = downloadTask.getException();
            appendLog("Download failed: " + fileItem.getName() + " - " +
                    (exception != null ? exception.getMessage() : "Unknown error"));
            hideTransferProgress();
        });

        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    /**
     * Delete selected remote files
     */
    private void deleteSelectedRemoteFiles() {
        List<FileItem> selectedItems = remoteFileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Remote Files");
        alert.setContentText("Are you sure you want to delete " + selectedItems.size() + " item(s)?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (FileItem item : selectedItems) {
                if (!item.isParentDirectory()) {
                    deleteRemoteFile(item);
                }
            }
        }
    }

    /**
     * Delete a remote file
     */
    private void deleteRemoteFile(FileItem fileItem) {
        Task<Boolean> deleteTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                if (fileItem.isDirectory()) {
                    return ftpClient.removeDirectory(fileItem.getName());
                } else {
                    return ftpClient.deleteFile(fileItem.getName());
                }
            }

            @Override
            protected void succeeded() {
                Boolean result = getValue();
                if (result) {
                    appendLog("Deleted remote file: " + fileItem.getName());
                    refreshRemoteFiles();
                } else {
                    appendLog("Failed to delete remote file: " + fileItem.getName());
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                appendLog("Error deleting remote file: " + fileItem.getName() + " - " +
                        (exception != null ? exception.getMessage() : "Unknown error"));
            }
        };

        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    /**
     * Delete selected local files
     */
    private void deleteSelectedLocalFiles() {
        List<FileItem> selectedItems = localFileTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Local Files");
        alert.setContentText("Are you sure you want to delete " + selectedItems.size() + " item(s)?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (FileItem item : selectedItems) {
                if (!item.isParentDirectory()) {
                    deleteLocalFile(item);
                }
            }
        }
    }

    /**
     * Delete a local file
     */
    private void deleteLocalFile(FileItem fileItem) {
        try {
            File file = new File(ftpClient.getCurrentLocalDirectory().toFile(), fileItem.getName());
            boolean deleted = file.delete();

            if (deleted) {
                appendLog("Deleted local file: " + fileItem.getName());
                refreshLocalFiles();
            } else {
                appendLog("Failed to delete local file: " + fileItem.getName());
            }

        } catch (Exception e) {
            appendLog("Error deleting local file: " + fileItem.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Create remote folder
     */
    private void createRemoteFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Remote Folder");
        dialog.setHeaderText("Create New Remote Folder");
        dialog.setContentText("Folder name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String folderName = result.get().trim();

            Task<Boolean> createTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return ftpClient.createDirectory(folderName);
                }

                @Override
                protected void succeeded() {
                    Boolean success = getValue();
                    if (success) {
                        appendLog("Created remote folder: " + folderName);
                        refreshRemoteFiles();
                    } else {
                        appendLog("Failed to create remote folder: " + folderName);
                    }
                }

                @Override
                protected void failed() {
                    Throwable exception = getException();
                    appendLog("Error creating remote folder: " + folderName + " - " +
                            (exception != null ? exception.getMessage() : "Unknown error"));
                }
            };

            Thread createThread = new Thread(createTask);
            createThread.setDaemon(true);
            createThread.start();
        }
    }

    /**
     * Create local folder
     */
    private void createLocalFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Local Folder");
        dialog.setHeaderText("Create New Local Folder");
        dialog.setContentText("Folder name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String folderName = result.get().trim();

            try {
                File newFolder = new File(ftpClient.getCurrentLocalDirectory().toFile(), folderName);
                boolean created = newFolder.mkdirs();

                if (created) {
                    appendLog("Created local folder: " + folderName);
                    refreshLocalFiles();
                } else {
                    appendLog("Failed to create local folder: " + folderName);
                }

            } catch (Exception e) {
                appendLog("Error creating local folder: " + folderName + " - " + e.getMessage());
            }
        }
    }

    /**
     * Hide transfer progress indicators
     */
    private void hideTransferProgress() {
        Platform.runLater(() -> {
            transferProgressContainer.setVisible(false);
            transferProgressBar.progressProperty().unbind();
            transferStatusLabel.textProperty().unbind();
            transferProgressBar.setProgress(0);
            transferStatusLabel.setText("");
        });
    }

    /**
     * Update remote path label
     */
    private void updateRemotePath() {
        if (ftpClient.isAuthenticated()) {
            String currentPath = ftpClient.getCurrentRemoteDirectory();
            remotePathLabel.setText("Remote: " + (currentPath != null ? currentPath : "/"));
        } else {
            remotePathLabel.setText("Remote: Not connected");
        }
    }

    /**
     * Update local path label
     */
    private void updateLocalPath() {
        localPathLabel.setText("Local: " + ftpClient.getCurrentLocalDirectory().toString());
    }

    /**
     * Append message to log
     */
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logTextArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
}