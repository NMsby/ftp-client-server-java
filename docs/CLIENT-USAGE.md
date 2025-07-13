# FTP Client Usage Guide

## Command-Line Interface

### Starting the Client

```bash
  # Interactive mode
  java Main client
    
  # Auto-connect mode
  java Main client <hostname> [port]
```

### Available Commands

#### Connection Commands
- `connect <host> [port]` - Connect to FTP server
- `login [username]` - Login to server (prompts for password)
- `quit` - Disconnect and exit
- `status` - Show connection status
- `noop` - Send keep-alive to server
- `system` - Get server system information

#### Remote Directory Commands
- `pwd` - Show current remote directory
- `cd <directory>` - Change remote directory
- `ls [path]` - List remote directory contents
- `mkdir <directory>` - Create remote directory
- `rmdir <directory>` - Remove remote directory

#### File Transfer Commands
- `get <remote-file> [local-file]` - Download file from server
- `put <local-file> [remote-file]` - Upload file to server
- `delete <file>` - Delete remote file
- `rename <old-name> <new-name>` - Rename remote file
- `size <file>` - Get remote file size

#### Local Directory Commands
- `lpwd` - Show current local directory
- `lcd <directory>` - Change local directory
- `lls [path]` - List local directory contents

#### Other Commands
- `help` - Show command help
- `?` - Alias for help

### Examples

```bash
  # Connect to server
  ftp> connect ftp.example.com 21
  ftp> login myuser

  # Navigate directories
  ftp> pwd
  ftp> ls
  ftp> cd public
  ftp> ls

  # Transfer files
  ftp> get important-file.txt
  ftp> put my-document.pdf
  ftp> put local-file.txt remote-name.txt

  # File management
  ftp> mkdir new-folder
  ftp> rename old-file.txt new-file.txt
  ftp> delete unwanted-file.txt
  ftp> size large-file.zip

  # Local operations
  ftp> lpwd
  ftp> lcd /home/user/downloads
  ftp> lls

  # Disconnect
  ftp> quit
```

## Batch Client

### Script Format

FTP scripts are text files containing FTP commands, one per line.

```bash
  # This is a comment
  connect localhost 21
  login admin admin123
  pwd
  list
  cd public
  get important-file.txt
  put my-upload.txt
  quit
```

### Running Scripts

```bash
  # Basic execution
  java Main batch script.ftp

  # Continue on errors
  java Main batch script.ftp --continue-on-error

  # Save execution log
  java Main batch script.ftp --log execution.log
```

### Script Commands

All interactive client commands are supported, plus:
- `sleep <seconds>` - Pause execution
- `echo <message>` - Print message to console
- `# comment` - Comment line (ignored)

## Configuration

Client configuration is managed through `ftp-config.properties`:

```properties
# Client settings
client.root.directory=./resources/client-files
client.timeout=30000
transfer.buffer.size=8192
```

## Error Handling

The client provides detailed error messages and logging:
- Connection errors are reported with specific reasons
- Authentication failures show server responses
- File transfer errors include progress information
- All operations are logged for debugging

## Tips and Best Practices

1. **Large File Transfers**: The client shows progress for file transfers
2. **Network Issues**: Use `noop` command to test connection
3. **Directory Navigation**: Use `pwd` and `ls` frequently to stay oriented
4. **Local vs Remote**: Remember `l` prefix for local commands (`ls` vs `lls`)
5. **Batch Scripts**: Test scripts interactively first
6. **Error Recovery**: Use `--continue-on-error` for robust batch operations