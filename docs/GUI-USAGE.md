# FTP GUI Client Usage Guide

## Overview

The FTP GUI Client provides a user-friendly graphical interface for FTP operations, featuring dual-pane file management, drag-and-drop support, and visual progress tracking.

## Starting the GUI Client

```bash
# Using Maven
mvn javafx:run

# Using Main class
java Main gui

# Or use the provided scripts
./run-gui.sh    # Linux/Mac
run-gui.bat     # Windows
```

## Interface Overview

### Connection Panel
- **Host**: Server hostname or IP address
- **Port**: Server port number (default: 21)
- **Username**: FTP username
- **Password**: FTP password
- **Save Password**: Option to remember password
- **Connect/Disconnect**: Connection control buttons

### File Management
- **Left Panel**: Remote files (server)
- **Right Panel**: Local files (client)
- **Path Labels**: Show current directories
- **Refresh Buttons**: Update file listings
- **Folder Buttons**: Create new folders
- **Delete Buttons**: Remove selected files/folders

### Transfer Operations
- **Upload Button**: Transfer files from local to remote
- **Download Button**: Transfer files from remote to local
- **Progress Bar**: Shows transfer progress
- **Status Label**: Transfer status messages

### Log Panel
- **Log Area**: Shows connection and operation messages
- **Clear Log**: Clears the log display

## How to Use

### 1. Connecting to Server

1. Enter server details in the connection panel:
    - Host: `localhost` (or your server address)
    - Port: `21` (or your server port)
    - Username: `admin` (or your username)
    - Password: `admin123` (or your password)

2. Click **Connect**
3. Wait for connection confirmation
4. Remote file listing will load automatically

### 2. File Navigation

**Remote Files (Left Panel)**:
- Double-click folders to navigate
- Double-click ".." to go to parent directory
- Click Refresh to update listing

**Local Files (Right Panel)**:
- Double-click folders to navigate
- Double-click ".." to go to parent directory
- Click Refresh to update listing

### 3. File Transfers

**Uploading Files**:
1. Select files in the local panel (right side)
2. Click **Upload →** button
3. Watch progress in the progress bar
4. Check log for completion status

**Downloading Files**:
1. Select files in the remote panel (left side)
2. Click **← Download** button
3. Watch progress in the progress bar
4. Check log for completion status

**Quick Transfer**:
- Double-click files to transfer them
- Double-click local files to upload
- Double-click remote files to download

### 4. File Management

**Creating Folders**:
1. Click **New Folder** button (local or remote)
2. Enter folder name in the dialog
3. Click OK to create

**Deleting Files**:
1. Select files/folders to delete
2. Click **Delete** button
3. Confirm deletion in the dialog

### 5. Multiple File Selection

- **Ctrl+Click**: Select multiple individual files
- **Shift+Click**: Select range of files
- **Ctrl+A**: Select all files

## Features

### Visual Indicators
- **Folder Icons**: Different icons for files and folders
- **File Sizes**: Human-readable size display
- **Timestamps**: Last modified dates
- **Permissions**: File permission display (remote files)

### Progress Tracking
- **Transfer Progress**: Visual progress bar
- **Speed Calculation**: Transfer speed display
- **Time Estimation**: Estimated completion time
- **Status Messages**: Detailed transfer status

### Error Handling
- **Connection Errors**: Clear error messages
- **Transfer Errors**: Detailed error information
- **File Conflicts**: Overwrite confirmations
- **Network Issues**: Automatic retry suggestions

### Accessibility
- **Keyboard Navigation**: Full keyboard support
- **Tooltips**: Helpful hover information
- **Status Updates**: Screen reader friendly
- **High Contrast**: Good color contrast

## Keyboard Shortcuts

### File Operations
- **Enter**: Open folder or transfer file
- **Delete**: Delete selected files
- **F2**: Rename file (future feature)
- **F5**: Refresh file listing
- **Ctrl+A**: Select all files

### Connection
- **Ctrl+Shift+C**: Connect
- **Ctrl+Shift+D**: Disconnect
- **Ctrl+Shift+R**: Refresh both panels

### Transfer
- **Ctrl+U**: Upload selected files
- **Ctrl+D**: Download selected files
- **Ctrl+N**: New folder

## Tips and Best Practices

### Connection Tips
1. **Test Connection**: Use command-line client first if GUI fails
2. **Firewall**: Ensure FTP ports are not blocked
3. **Credentials**: Double-check username and password
4. **Server Status**: Verify server is running

### Transfer Tips
1. **Large Files**: Monitor progress for large transfers
2. **Multiple Files**: Select multiple files for batch transfers
3. **Bandwidth**: Large transfers may take time on slow connections
4. **Interruptions**: GUI handles network interruptions gracefully

### File Management Tips
1. **Navigation**: Use .. folder to go up one level
2. **Organization**: Create folders for better organization
3. **Permissions**: Check file permissions before operations
4. **Backups**: Download important files for backup

### Performance Tips
1. **Connection**: Close unused connections
2. **Memory**: Restart GUI for very long sessions
3. **Logging**: Clear log regularly for better performance
4. **Files**: Avoid transferring very large numbers of files at once

## Troubleshooting

### Connection Issues
- **Cannot Connect**: Check hostname, port, and network
- **Authentication Failed**: Verify username and password
- **Timeout**: Check server responsiveness and firewall

### Transfer Issues
- **Upload Failed**: Check remote directory permissions
- **Download Failed**: Check local directory permissions
- **Slow Transfer**: Check network bandwidth and server load

### GUI Issues
- **Interface Frozen**: Check for long-running operations
- **Display Problems**: Try resizing the window
- **Missing Files**: Click Refresh to update listings

### Log Messages
- **Green Messages**: Successful operations
- **Red Messages**: Errors and failures
- **Blue Messages**: Information and status updates

## Configuration

The GUI client uses the same configuration as the command-line client:

```properties
# Client settings in ftp-config.properties
client.root.directory=./resources/client-files
client.timeout=30000
transfer.buffer.size=8192
```

## Future Enhancements

Planned features for future versions:
- Drag and drop file transfers
- File preview capabilities
- Advanced search functionality
- Bookmark management
- Theme customization
- Multi-language support