# FTP Client-Server User Manual

## Table of Contents
1. [Getting Started](#getting-started)
2. [Server Administration](#server-administration)
3. [Command-Line Client](#command-line-client)
4. [GUI Client](#gui-client)
5. [Batch Operations](#batch-operations)
6. [Troubleshooting](#troubleshooting)
7. [FAQ](#frequently-asked-questions)

## Getting Started

### System Requirements
- **Java**: Version 17 or later
- **Operating System**: Windows 10+, Linux (Ubuntu 18.04+), macOS 10.14+
- **Memory**: Minimum 512MB RAM
- **Network**: TCP/IP connectivity
- **Disk Space**: 100MB for installation

### Quick Start Guide

#### 1. Download and Setup
```bash
# Extract the installation package
unzip ftp-server-package.zip
cd ftp-server-package

# Make scripts executable (Linux/macOS)
chmod +x *.sh
```

#### 2. Start the FTP Server
```bash
# Windows
start-ftp-server.bat

# Linux/macOS
./start-ftp-server.sh
```

#### 3. Connect with a Client
```bash
# Command-line client
java Main client localhost 21

# GUI client
java Main gui
```

#### 4. Login and Test
```
Username: admin
Password: admin123
```

## Server Administration

### Starting the Server

#### Windows
Double-click `start-ftp-server.bat` or run from command prompt:
```cmd
start-ftp-server.bat
```

#### Linux/macOS
```bash
./start-ftp-server.sh
```

#### Advanced Options
```bash
# Custom configuration file
./start-ftp-server.sh custom-config.properties

# Specify Java options
JAVA_OPTS="-Xmx1g -Xms512m" ./start-ftp-server.sh
```

### Server Configuration

#### Configuration File Location
- Windows: `ftp-server-production.properties`
- Linux: `/opt/ftpserver/ftp-server-production.properties`

#### Key Configuration Settings
```properties
# Server port (default: 21)
server.port=21

# Maximum concurrent connections (default: 50)
server.max.connections=50

# File storage directory
server.root.directory=./files

# Security settings
security.max.login.attempts=3
security.ban.duration.minutes=15

# Performance settings
performance.thread.pool.size=20
transfer.buffer.size=8192
```

### User Management

#### Default User Accounts
| Username | Password | Access Level | Description |
|----------|----------|--------------|-------------|
| admin | admin123 | Full | Complete server access |
| user | user123 | Standard | Read/write access |
| test | test | Read-only | View and download only |

⚠️ **Security Warning**: Change default passwords before production use!

#### Managing Users
Edit the configuration file and restart the server:
```properties
# Add new user
user.newuser.password=newpassword
user.newuser.home=/home/newuser
user.newuser.permissions=read,write

# Disable user
user.olduser.enabled=false
```

### Administrative Interface

When the server is running, you can access the admin interface:

```
admin> help                    # Show available commands
admin> status                  # Server status summary
admin> stats                   # Detailed statistics
admin> connections             # Active connections
admin> performance             # Performance metrics
admin> security                # Security status
admin> shutdown                # Stop server
```

### Monitoring and Logs

#### Log Files
- **Server Log**: `logs/ftp-server.log`
- **Security Log**: `logs/ftp-security.log`
- **Performance Log**: `logs/ftp-performance.log`

#### Monitoring Commands
```bash
# Watch server logs
tail -f logs/ftp-server.log

# Check server status
java -cp ftp-server.jar utils.HealthChecker localhost 21 admin admin123

# Monitor connections
netstat -an | grep :21
```

## Command-Line Client

### Starting the CLI Client

#### Direct Connection
```bash
java Main client [hostname] [port]
# Example: java Main client ftp.example.com 21
```

#### Interactive Mode
```bash
java Main client
```

### Basic Commands

#### Connection Management
```
connect <hostname> [port]      # Connect to server
login [username]               # Authenticate
quit                          # Disconnect and exit
status                        # Show connection status
```

#### File Operations
```
get <remote-file> [local-file]    # Download file
put <local-file> [remote-file]    # Upload file
delete <file>                     # Delete remote file
rename <old> <new>                # Rename remote file
size <file>                       # Get file size
```

#### Directory Operations
```
pwd                              # Show current remote directory
cd <directory>                   # Change remote directory
ls [path]                        # List remote directory
mkdir <directory>                # Create remote directory
rmdir <directory>                # Remove remote directory
```

#### Local Operations
```
lpwd                             # Show current local directory
lcd <directory>                  # Change local directory
lls [path]                       # List local directory
```

#### Other Commands
```
help                             # Show command help
noop                             # Send keep-alive
system                           # Get server system info
```

### Example Session
```
$ java Main client localhost 21
FTP> connect localhost 21
Connected to localhost
FTP> login admin
Password: admin123
Login successful
Current directory: /

FTP> ls
Directory listing:
drwxr-xr-x   <DIR>   Jan 13 10:30  public
-rw-r--r--   1024    Jan 13 10:30  readme.txt

FTP> cd public
Directory changed to /public

FTP> put myfile.txt
Uploading myfile.txt...
Upload completed

FTP> get serverfile.txt
Downloading serverfile.txt...
Download completed

FTP> quit
Goodbye!
```

## GUI Client

### Starting the GUI Client
```bash
java Main gui
```

### Interface Overview

#### Connection Panel
- **Host**: Server hostname or IP address
- **Port**: Server port (usually 21)
- **Username**: Your username
- **Password**: Your password
- **Save Password**: Remember password (optional)

#### File Browser
- **Left Panel**: Remote files (server)
- **Right Panel**: Local files (your computer)
- **Path Display**: Shows current directories
- **Toolbar**: Action buttons for operations

#### Transfer Panel
- **Upload Button**: Transfer files to server
- **Download Button**: Transfer files from server
- **Progress Bar**: Shows transfer progress
- **Status Display**: Transfer status messages

### Common Operations

#### Connecting to Server
1. Enter server details in the connection panel
2. Click "Connect"
3. Wait for connection confirmation
4. Both file panels will show current directories

#### Uploading Files
1. Navigate to desired local files (right panel)
2. Select files to upload
3. Click "Upload →" button
4. Watch progress in the progress bar
5. Files appear in remote panel when complete

#### Downloading Files
1. Navigate to desired remote files (left panel)
2. Select files to download
3. Click "← Download" button
4. Watch progress in the progress bar
5. Files appear in local panel when complete

#### Quick Transfer (Double-Click)
- Double-click local files to upload them
- Double-click remote files to download them
- Double-click folders to navigate into them

#### Creating Folders
1. Click "New Folder" button
2. Enter folder name
3. Press OK to create

#### Deleting Files
1. Select files to delete
2. Click "Delete" button
3. Confirm deletion in dialog

### Keyboard Shortcuts
- **Enter**: Open folder or transfer file
- **Delete**: Delete selected files
- **F5**: Refresh file listing
- **Ctrl+A**: Select all files
- **Ctrl+U**: Upload selected files
- **Ctrl+D**: Download selected files
- **Ctrl+Shift+C**: Connect to server
- **Ctrl+Shift+D**: Disconnect from server

### Tips for GUI Usage
1. **Multiple Selection**: Hold Ctrl and click to select multiple files
2. **Range Selection**: Hold Shift and click to select a range
3. **Navigation**: Use ".." folder to go to parent directory
4. **File Information**: Hover over files to see details
5. **Progress Monitoring**: Large transfers show detailed progress

## Batch Operations

### Creating Batch Scripts

Batch scripts contain FTP commands to execute automatically:

```bash
# Save as: backup-script.ftp
connect ftp.myserver.com 21
login myusername mypassword
cd backup
put important-file.txt
put database-backup.sql
mkdir daily-backup-2025-01-13
cd daily-backup-2025-01-13
put logs.tar.gz
quit
```

### Running Batch Scripts
```bash
java Main batch backup-script.ftp
```

### Advanced Batch Options
```bash
# Continue on errors
java Main batch backup-script.ftp --continue-on-error

# Save execution log
java Main batch backup-script.ftp --log backup.log
```

### Batch Script Commands
All interactive commands are supported in batch scripts:
- Connection: `connect`, `login`, `quit`
- File operations: `get`, `put`, `delete`, `rename`
- Directory operations: `cd`, `mkdir`, `rmdir`, `ls`
- Utility: `sleep`, `echo`, `#` (comments)

### Example: Automated Backup
```bash
#!/bin/bash
# Daily backup script

# Create backup script
cat > daily-backup.ftp << EOF
# Daily backup to FTP server
connect backup.company.com 21
login backup_user backup_pass
cd daily-backups
mkdir backup-$(date +%Y%m%d)
cd backup-$(date +%Y%m%d)
put /var/backups/database.sql
put /var/backups/files.tar.gz
put /var/log/application.log
quit
EOF

# Execute backup
java Main batch daily-backup.ftp --log backup-$(date +%Y%m%d).log

# Cleanup
rm daily-backup.ftp
```

## Troubleshooting

### Common Connection Issues

#### "Cannot connect to server"
**Possible Causes:**
- Server not running
- Wrong hostname/IP address
- Firewall blocking connection
- Network connectivity issues

**Solutions:**
1. Verify server is running: `netstat -an | grep :21`
2. Test network connectivity: `ping hostname`
3. Check firewall settings
4. Try telnet test: `telnet hostname 21`

#### "Connection timeout"
**Possible Causes:**
- Network latency
- Server overload
- Firewall interference

**Solutions:**
1. Increase timeout in configuration
2. Check server load
3. Test from different network

#### "Connection refused"
**Possible Causes:**
- Server not listening on port
- Wrong port number
- Service not started

**Solutions:**
1. Check server configuration
2. Verify port number
3. Restart FTP server

### Authentication Problems

#### "Login incorrect"
**Possible Causes:**
- Wrong username/password
- Account disabled
- Case sensitivity

**Solutions:**
1. Verify credentials
2. Check user configuration
3. Try different case combinations

#### "Too many login failures"
**Possible Causes:**
- Multiple failed attempts
- IP temporarily banned

**Solutions:**
1. Wait for ban period to expire
2. Check security logs
3. Use correct credentials

### File Transfer Issues

#### "Upload failed"
**Possible Causes:**
- Insufficient permissions
- Disk space full
- Network interruption

**Solutions:**
1. Check file permissions
2. Verify disk space
3. Retry transfer
4. Check network stability

#### "Download failed"
**Possible Causes:**
- File not found
- Permission denied
- Connection lost

**Solutions:**
1. Verify file exists
2. Check read permissions
3. Re-establish connection

#### "Transfer very slow"
**Possible Causes:**
- Network bandwidth limitation
- Server overload
- Large file size
- Network congestion

**Solutions:**
1. Check network bandwidth: `speedtest-cli`
2. Monitor server resources
3. Transfer during off-peak hours
4. Split large files into smaller chunks

### Performance Issues

#### "Server responds slowly"
**Possible Causes:**
- High server load
- Insufficient memory
- Too many connections

**Solutions:**
1. Check server resources: `top`, `htop`
2. Review server logs for errors
3. Increase memory allocation
4. Limit concurrent connections

#### "GUI client freezes"
**Possible Causes:**
- Large directory listings
- Network timeout
- Insufficient memory

**Solutions:**
1. Increase Java heap size: `-Xmx1g`
2. Close and restart GUI client
3. Check network connectivity
4. Navigate to smaller directories

### Configuration Problems

#### "Cannot start server"
**Possible Causes:**
- Port already in use
- Invalid configuration
- Permission issues

**Solutions:**
1. Check port usage: `netstat -an | grep :21`
2. Validate configuration file
3. Run with appropriate permissions
4. Change server port if needed

#### "Configuration not loading"
**Possible Causes:**
- File not found
- Invalid format
- Permission denied

**Solutions:**
1. Check file path and existence
2. Validate property syntax
3. Verify file permissions
4. Use absolute paths

## Frequently Asked Questions

### General Questions

**Q: What FTP commands are supported?**  
A: The server supports all standard FTP commands including USER, PASS, LIST, RETR, STOR, DELE, MKD, RMD, RNFR, RNTO, SIZE, MDTM, plus advanced commands like FEAT, MLST, MLSD, OPTS, and STAT.

**Q: How many concurrent connections can the server handle?**  
A: The default limit is 50 concurrent connections, but this can be configured up to several hundred depending on system resources.

**Q: Is the server secure for production use?**  
A: The server includes security features like IP banning, rate limiting, and access control. However, for production use, consider additional security measures like SSL/TLS encryption and network firewalls.

**Q: Can I use this with existing FTP clients?**  
A: Yes, the server implements standard FTP protocol and works with most FTP clients including FileZilla, WinSCP, and command-line ftp clients.

### Technical Questions

**Q: What Java version is required?**  
A: Java 17 or later is required. The application has been tested with Java 17 LTS and Java 21 LTS.

**Q: How do I change the server port?**  
A: Edit the `server.port` property in the configuration file and restart the server. Remember to update firewall rules.

**Q: Can I integrate this with external authentication systems?**  
A: The current implementation uses simple file-based authentication. For LDAP or database integration, you would need to extend the UserManager class.

**Q: How do I enable logging for debugging?**  
A: Set `logging.level=DEBUG` in the configuration file. Logs are written to the `logs/` directory.

**Q: Is clustering or load balancing supported?**  
A: The current implementation is single-server. For clustering, you would need to implement session sharing and load balancing externally.

### Configuration Questions

**Q: How do I add new users?**  
A: Add user properties to the configuration file:
```properties
user.newuser.password=password123
user.newuser.home=/home/newuser
user.newuser.permissions=read,write
```

**Q: Can I restrict users to specific directories?**  
A: Yes, set the `user.username.home` property to define the user's home directory. Users cannot navigate above their home directory.

**Q: How do I enable anonymous access?**  
A: Add an anonymous user configuration:
```properties
user.anonymous.password=
user.anonymous.home=/public
user.anonymous.permissions=read
```

**Q: How do I backup the server configuration?**  
A: Backup these files:
- Configuration files (`*.properties`)
- User data directory
- Log files (for audit trail)

### Deployment Questions

**Q: How do I deploy on a VPS/cloud server?**  
A: Use the provided deployment scripts:
1. Copy deployment package to server
2. Run `./start-ftp-server.sh`
3. Configure firewall rules
4. Test connectivity

**Q: Can I run this in Docker?**  
A: Yes, use the provided Docker configuration:
```bash
docker build -t ftp-server .
docker run -p 21:21 -p 20000-21000:20000-21000 ftp-server
```

**Q: How do I set up automatic startup?**  
A: Use the provided systemd service file:
```bash
sudo cp ftp-server.service /etc/systemd/system/
sudo systemctl enable ftp-server
sudo systemctl start ftp-server
```

**Q: What firewall ports need to be open?**  
A: Open TCP port 21 for control connections and TCP ports 20000-21000 for data connections (or your configured range).

### Troubleshooting Questions

**Q: The GUI client won't start, what's wrong?**  
A: Check that JavaFX is available:
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar ftp-client.jar
```

**Q: File transfers are very slow, how to improve performance?**  
A: Try these optimizations:
- Increase buffer size in configuration
- Use wired connection instead of WiFi
- Check network bandwidth
- Reduce concurrent connections

**Q: How do I reset a user's password?**  
A: Edit the configuration file:
```properties
user.username.password=newpassword
```
Then restart the server.

**Q: The server uses too much memory, how to reduce it?**  
A: Adjust JVM settings:
```bash
JAVA_OPTS="-Xmx512m -Xms256m" ./start-ftp-server.sh
```

**Q: How do I migrate to a new server?**  
A:
1. Stop the old server
2. Copy all files and configuration
3. Install Java on new server
4. Start server with same configuration
5. Update DNS/firewall rules

### Advanced Usage Questions

**Q: Can I customize the FTP responses?**  
A: Yes, modify the FTPResponse enum to customize response messages.

**Q: How do I add custom FTP commands?**  
A: Extend the CommandProcessor class and add new command handlers.

**Q: Can I integrate with cloud storage?**  
A: You would need to modify the file handling layer to interface with cloud storage APIs.

**Q: How do I monitor server performance?**  
A: Use the built-in performance monitoring:
```bash
# Access admin interface
admin> performance

# Use health checker
java -cp ftp-server.jar utils.HealthChecker localhost 21
```

**Q: Can I set up automated backups?**  
A: Yes, use batch scripts with cron (Linux) or Task Scheduler (Windows):
```bash
# Add to crontab
0 2 * * * /path/to/backup-script.sh
```

## Support and Resources

### Getting Help
1. **Documentation**: Check all documentation files in the `docs/` directory
2. **Logs**: Review server logs in `logs/` directory for error details
3. **Health Check**: Use built-in health checking tools
4. **Community**: Consult FTP protocol documentation (RFC 959)

### Additional Resources
- **FTP Protocol Reference**: RFC 959
- **Java Documentation**: Oracle Java Documentation
- **JavaFX Guide**: OpenJFX Documentation
- **Maven Reference**: Apache Maven Documentation

### Best Practices
1. **Security**: Change default passwords before production use
2. **Monitoring**: Set up regular health checks and log monitoring
3. **Backups**: Implement regular backup procedures
4. **Updates**: Keep Java and system updates current
5. **Testing**: Test all functionality after configuration changes

This user manual provides comprehensive guidance for using the FTP client-server system effectively and safely.