# FTP Server API Documentation

## Overview

This document provides API documentation for the FTP server implementation, including all supported commands, response codes, and usage examples.

## FTP Protocol Implementation

### Supported Commands

#### Authentication Commands

##### USER
**Syntax**: `USER <username>`  
**Description**: Specify username for authentication  
**Parameters**: username - User identifier  
**Response Codes**: 331 (Username OK, need password), 530 (Not logged in)

**Example**:
```
C: USER admin
S: 331 User admin OK, need password
```

##### PASS
**Syntax**: `PASS <password>`  
**Description**: Specify password for authentication  
**Parameters**: password - User password  
**Response Codes**: 230 (User logged in), 530 (Login incorrect)  

**Example**:
```
C: PASS admin123
S: 230 User admin logged in successfully
```

#### Directory Commands

##### PWD
**Syntax**: `PWD`  
**Description**: Print working directory  
**Parameters**: None  
**Response Codes**: 257 (Pathname created)  

**Example**:
```
C: PWD
S: 257 "/" is current directory
```

##### CWD
**Syntax**: `CWD <directory>`  
**Description**: Change working directory  
**Parameters**: directory - Target directory path  
**Response Codes**: 250 (Directory changed), 550 (Directory not found)  

**Example**:
```
C: CWD public
S: 250 Directory changed to /public
```

##### LIST
**Syntax**: `LIST [path]`  
**Description**: List directory contents  
**Parameters**: path - Directory to list (optional)  
**Response Codes**: 150 (Opening data connection), 226 (Transfer complete)  

**Example**:
```
C: LIST
S: 150 Here comes the directory listing
S: drwxr-xr-x    0 Jan 13 10:30 public
S: -rw-r--r-- 1024 Jan 13 10:30 readme.txt
S: 226 Directory send OK
```

##### MKD
**Syntax**: `MKD <directory>`  
**Description**: Create directory  
**Parameters**: directory - Directory name to create  
**Response Codes**: 257 (Directory created), 550 (Cannot create)  

**Example**:
```
C: MKD newdir
S: 257 "newdir" directory created
```

##### RMD
**Syntax**: `RMD <directory>`  
**Description**: Remove directory  
**Parameters**: directory - Directory name to remove  
**Response Codes**: 250 (Directory removed), 550 (Cannot remove)  

**Example**:
```
C: RMD olddir
S: 250 Directory removed successfully
```

#### File Transfer Commands

##### RETR
**Syntax**: `RETR <filename>`  
**Description**: Download file from server  
**Parameters**: filename - File to download  
**Response Codes**: 150 (Opening data connection), 226 (Transfer complete), 550 (File not found)  

**Example**:
```
C: RETR document.txt
S: 150 Opening data connection for document.txt
S: 226 Transfer complete
```

##### STOR
**Syntax**: `STOR <filename>`  
**Description**: Upload file to server  
**Parameters**: filename - Target filename  
**Response Codes**: 150 (Ready to receive), 226 (Transfer complete), 550 (Cannot store)  

**Example**:
```
C: STOR upload.txt
S: 150 Ready to receive file upload.txt
S: 226 Transfer complete
```

##### DELE
**Syntax**: `DELE <filename>`  
**Description**: Delete file on server  
**Parameters**: filename - File to delete  
**Response Codes**: 250 (File deleted), 550 (File not found)  

**Example**:
```
C: DELE unwanted.txt
S: 250 File deleted successfully
```

#### File Management Commands

##### RNFR
**Syntax**: `RNFR <filename>`  
**Description**: Rename from (source filename)  
**Parameters**: filename - Source filename  
**Response Codes**: 350 (File exists, ready for destination)  

**Example**:
```
C: RNFR oldname.txt
S: 350 File exists, ready for destination name
```

##### RNTO
**Syntax**: `RNTO <filename>`  
**Description**: Rename to (destination filename)  
**Parameters**: filename - Destination filename  
**Response Codes**: 250 (Rename successful), 550 (Rename failed)  

**Example**:
```
C: RNTO newname.txt
S: 250 Rename successful
```

##### SIZE
**Syntax**: `SIZE <filename>`  
**Description**: Get file size  
**Parameters**: filename - File to check  
**Response Codes**: 213 (File size), 550 (File not found)  

**Example**:
```
C: SIZE document.txt
S: 213 2048
```

##### MDTM
**Syntax**: `MDTM <filename>`  
**Description**: Get file modification time  
**Parameters**: filename - File to check  
**Response Codes**: 213 (Modification time), 550 (File not found)  

**Example**:
```
C: MDTM document.txt
S: 213 20250113120000
```

#### Advanced Commands

##### FEAT
**Syntax**: `FEAT`  
**Description**: List server features  
**Parameters**: None  
**Response Codes**: 211 (Features list)  

**Example**:
```
C: FEAT
S: 211-Features:
S:  SIZE
S:  MDTM
S:  MLST type*;size*;modify*;
S:  MLSD
S:  UTF8
S: 211 End
```

##### MLST
**Syntax**: `MLST [path]`  
**Description**: Machine-readable file listing (single file)  
**Parameters**: path - File or directory path (optional)  
**Response Codes**: 250 (File information)  

**Example**:
```
C: MLST document.txt
S: 250 type=file;size=2048;modify=20250113120000;perm=r; document.txt
```

##### MLSD
**Syntax**: `MLSD [path]`  
**Description**: Machine-readable directory listing  
**Parameters**: path - Directory path (optional)  
**Response Codes**: 150 (Directory listing), 226 (Transfer complete)  

**Example**:
```
C: MLSD
S: 150 Here comes the directory listing
S: type=dir;modify=20250113100000;perm=flcdmp; public
S: type=file;size=1024;modify=20250113120000;perm=r; readme.txt
S: 226 Directory send OK
```

##### OPTS
**Syntax**: `OPTS <option> <value>`  
**Description**: Set server options  
**Parameters**: option - Option name, value - Option value  
**Response Codes**: 200 (Option set), 501 (Option not supported)  

**Example**:
```
C: OPTS UTF8 ON
S: 200 UTF8 set to on
```

##### STAT
**Syntax**: `STAT [path]`  
**Description**: Server or file status  
**Parameters**: path - File path (optional)  
**Response Codes**: 211 (System status), 213 (File status)  

**Example**:
```
C: STAT
S: 211-Status of server:
S: Connected from: 192.168.1.100:12345
S: Logged in as: admin
S: Current directory: /
S: 211 End of status
```

#### Connection Commands

##### SYST
**Syntax**: `SYST`  
**Description**: System type identification  
**Parameters**: None  
**Response Codes**: 215 (System type)  

**Example**:
```
C: SYST
S: 215 UNIX Type: L8 (Windows 10)
```

##### NOOP
**Syntax**: `NOOP`  
**Description**: No operation (keep-alive)  
**Parameters**: None  
**Response Codes**: 200 (Command successful)  

**Example**:
```
C: NOOP
S: 200 NOOP command successful
```

##### QUIT
**Syntax**: `QUIT`  
**Description**: Terminate connection  
**Parameters**: None  
**Response Codes**: 221 (Service closing)  

**Example**:
```
C: QUIT
S: 221 Goodbye
```

## Response Codes

### Success Responses (2xx)
- **200**: Command okay  
- **211**: System status or help reply  
- **213**: File status  
- **215**: System type  
- **220**: Service ready for new user  
- **221**: Service closing control connection  
- **226**: Closing data connection, transfer successful  
- **230**: User logged in successfully  
- **250**: Requested file action okay, completed  
- **257**: Pathname created  

### Intermediate Responses (3xx)
- **331**: User name okay, need password
- **350**: Requested file action pending further information

### Transient Failure (4xx)
- **425**: Can't open data connection
- **426**: Connection closed, transfer aborted
- **450**: Requested file action not taken
- **451**: Requested action aborted, local error

### Permanent Failure (5xx)
- **500**: Syntax error, command unrecognized
- **501**: Syntax error in parameters or arguments
- **502**: Command not implemented
- **503**: Bad sequence of commands
- **530**: Not logged in
- **550**: Requested action not taken, file unavailable
- **553**: Requested action not taken, file name not allowed

## Authentication System

### User Management
The server supports multiple user accounts with different privilege levels:

```java
// User configuration example
admin:admin123  - Full access (read/write/delete)
user:user123    - Limited access (read/write)
test:test       - Read-only access
guest:          - Anonymous access (if enabled)
```

### Security Features
1. **Failed Login Protection**: Automatic IP banning after failed attempts
2. **Rate Limiting**: Request rate limiting per IP address
3. **Session Management**: Automatic session timeout
4. **Access Control**: Directory-based permissions

## Configuration API

### Server Configuration
```properties
# Core server settings
server.port=21
server.max.connections=50
server.root.directory=/opt/ftpserver/files

# Security settings
security.max.login.attempts=3
security.ban.duration.minutes=15
security.rate.limit.max.requests=100

# Performance settings
performance.thread.pool.size=20
transfer.buffer.size=8192
```

### Runtime Configuration
Access server configuration programmatically:

```java
FTPConfig config = FTPConfig.getInstance();
int port = config.getServerPort();
String rootDir = config.getServerRootDirectory();
```

## Administrative Interface

### Server Management Commands
When running the server with admin interface:

```
admin> status          # Show server status
admin> stats           # Detailed statistics
admin> connections     # Connection information
admin> performance     # Performance metrics
admin> security        # Security statistics
admin> shutdown        # Graceful shutdown
```

### Performance Monitoring
```java
PerformanceMonitor monitor = PerformanceMonitor.getInstance();
String stats = monitor.getPerformanceStats();
monitor.recordConnection();
monitor.recordUpload(bytes);
```

### Security Management
```java
SecurityManager security = SecurityManager.getInstance();
boolean allowed = security.isConnectionAllowed(clientAddress);
security.recordFailedLogin(clientAddress);
```

## Error Handling

### Exception Hierarchy
```
FTPException
├── AuthenticationException
├── FileNotFoundException
├── PermissionDeniedException
└── NetworkException
```

### Error Response Format
All error responses follow the standard FTP format:
```
<code> <message>
```

Where:
- `code`: Three-digit response code
- `message`: Human-readable error description

## Client API Usage

### Basic Client Operations
```java
FTPClient client = new FTPClient();

// Connect and authenticate
client.connect("hostname", 21);
client.login("username", "password");

// File operations
client.uploadFile("local.txt", "remote.txt");
client.downloadFile("remote.txt", "local.txt");
client.deleteFile("unwanted.txt");

// Directory operations
client.changeDirectory("subdirectory");
String listing = client.listDirectory();
client.createDirectory("newdir");

// Disconnect
client.disconnect();
```

### Advanced Client Features
```java
// Get file information
long size = client.getFileSize("file.txt");

// Rename files
client.renameFile("old.txt", "new.txt");

// Server information
String systemInfo = client.getSystemInfo();
boolean alive = client.noop();
```

## Best Practices

### Server Administration
1. **Regular Monitoring**: Use health checks and performance monitoring
2. **Security Updates**: Keep user credentials secure and updated
3. **Log Management**: Implement log rotation and analysis
4. **Backup Procedures**: Regular backup of configuration and data

### Client Development
1. **Error Handling**: Always check return values and handle exceptions
2. **Connection Management**: Properly close connections and clean up resources
3. **Progress Tracking**: Implement progress callbacks for large transfers
4. **Timeout Handling**: Set appropriate timeouts for network operations

### Performance Optimization
1. **Buffer Sizes**: Tune buffer sizes for optimal transfer performance
2. **Connection Pooling**: Reuse connections when possible
3. **Concurrent Operations**: Use multiple threads for parallel transfers
4. **Network Configuration**: Optimize network settings for throughput

This API documentation provides complete reference for interacting with the FTP server system programmatically and understanding its capabilities.