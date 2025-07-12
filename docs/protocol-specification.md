# FTP Protocol Specification

## 1. Protocol Overview

This document specifies the FTP protocol implementation used in the client-server system, based on RFC 959 with custom extensions for educational purposes.

## 2. Connection Model

### 2.1 Control Connection
- **Port**: 21 (default) or configurable
- **Protocol**: TCP
- **Purpose**: Command and response exchange
- **Persistence**: Maintained throughout session

### 2.2 Data Connection
- **Port**: Ephemeral port assigned by server
- **Protocol**: TCP
- **Purpose**: File transfer and directory listings
- **Persistence**: Created per transfer, closed after completion

## 3. Command Set

### 3.1 Authentication Commands
- **USER <username>**: Specify username for authentication
- **PASS <password>**: Specify password for authentication

### 3.2 Directory Commands
- **LIST [path]**: List directory contents
- **CWD <path>**: Change working directory
- **PWD**: Print working directory
- **MKD <path>**: Create directory
- **RMD <path>**: Remove directory

### 3.3 File Transfer Commands
- **RETR <filename>**: Download file from server
- **STOR <filename>**: Upload file to server
- **DELE <filename>**: Delete file on server

### 3.4 File Information Commands
- **SIZE <filename>**: Get file size
- **MDTM <filename>**: Get file modification time

### 3.5 File Management Commands
- **RNFR <filename>**: Rename from (source filename)
- **RNTO <filename>**: Rename to (destination filename)

### 3.6 Connection Commands
- **QUIT**: Terminate connection gracefully
- **NOOP**: No operation (keep-alive)

## 4. Response Codes

### 4.1 Success Responses (2xx)
- **200**: Command successful
- **220**: Service ready
- **221**: Service closing control connection
- **226**: Closing data connection, file transfer successful
- **230**: User logged in successfully
- **250**: Requested file action okay, completed

### 4.2 Intermediate Responses (3xx)
- **331**: Username okay, need password
- **350**: Requested file action pending further information

### 4.3 Temporary Failure (4xx)
- **425**: Can't open data connection
- **426**: Connection closed, transfer aborted
- **450**: Requested file action not taken
- **451**: Requested action aborted, local error

### 4.4 Permanent Failure (5xx)
- **500**: Syntax error, command unrecognized
- **501**: Syntax error in parameters or arguments
- **502**: Command not implemented
- **503**: Bad sequence of commands
- **530**: Not logged in
- **550**: Requested action not taken, file unavailable

## 5. Message Format

### 5.1 Command Format
```
COMMAND [parameter1] [parameter2] ... [parameterN]\r\n
```

### 5.2 Response Format
```
CODE Message text\r\n
```

### 5.3 Multi-line Response Format
```
CODE-First line of message\r\n
CODE-Second line of message\r\n
CODE Last line of message\r\n
```

## 6. Data Transfer Modes

### 6.1 Binary Mode
- Default transfer mode
- Preserves file integrity
- Used for all file types

### 6.2 ASCII Mode
- Text mode transfer
- Handles line ending conversions
- Optional implementation

## 7. Error Handling

### 7.1 Command Errors
- Invalid syntax returns 501
- Unknown commands return 500
- Sequence errors return 503

### 7.2 File System Errors
- File not found returns 550
- Permission denied returns 550
- Disk full returns 451

### 7.3 Network Errors
- Connection timeout handling
- Socket errors result in connection termination
- Retry mechanisms for temporary failures

## 8. Session Management

### 8.1 Connection Lifecycle
1. Client connects to server
2. Server sends 220 welcome message
3. Client authenticates (USER/PASS)
4. Commands exchanged
5. Client sends QUIT
6. Server responds 221 and closes connection

### 8.2 Authentication States
- **Not authenticated**: Only USER command accepted
- **Username provided**: Only PASS command accepted
- **Authenticated**: All commands available

## 9. Implementation Notes

### 9.1 Threading Considerations
- Each client connection handled in separate thread
- File system operations must be thread-safe
- Proper synchronization for shared resources

### 9.2 Performance Optimizations
- Connection pooling for data connections
- Efficient file streaming with buffers
- Minimal memory footprint for large files

### 9.3 Security Considerations
- Input validation for all commands
- Path traversal prevention
- Resource limit enforcement
