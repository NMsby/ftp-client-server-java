# FTP Client-Server Implementation

A comprehensive FTP (File Transfer Protocol) client-server system implementation in Java, demonstrating distributed systems concepts.

## Features

- Multi-threaded FTP server supporting concurrent clients
- Command-line FTP client with full functionality
- GUI-based FTP client for user-friendly file management
- Support for standard FTP commands (USER, PASS, LIST, RETR, STOR, etc.)
- Local and remote operation capabilities
- Comprehensive logging and error handling

## Technology Stack

- **Language**: Java 17.0.14 LTS
- **Build Tool**: Maven
- **Testing**: JUnit 5
- **Logging**: Log4j2
- **GUI**: JavaFX (to be implemented)

## Project Structure

```
ftp-client-server-java/
├── src/main/java/
│   ├── server/          # FTP server implementation
│   ├── client/          # FTP client implementations
│   ├── common/          # Shared protocol definitions
│   └── utils/           # Utility classes
├── docs/                # Project documentation
├── resources/           # Configuration and test files
└── screenshots/         # Application screenshots
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Git

### Installation

1. Clone the repository:
```bash
  git clone https://github.com/nmsby/ftp-client-server-java.git
  cd ftp-client-server-java
```

2. Build the project:
```bash
  mvn clean compile
```

3. Run tests:
```bash
  mvn test
```

## Usage

*Usage instructions will be added as implementation progresses.*

## Documentation

- [Requirements Specification](docs/requirements.md)
- [System Architecture](docs/architecture.md)
- [Protocol Specification](docs/protocol-specification.md)

## Contributing

Contributions are welcome, but please note that this is primarily an educational project.
If you have suggestions or improvements, feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Nelson Masbayi — Distributed Systems (Strathmore University)

## Acknowledgments

- FTP Protocol RFC specifications
- Java networking documentation