package server;

import common.FTPConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages FTP server deployment configurations and scripts
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class DeploymentManager {
    private static final Logger logger = LogManager.getLogger("server");

    private final FTPConfig config;
    private final Path deploymentDir;

    public DeploymentManager(FTPConfig config) {
        this.config = config;
        this.deploymentDir = Paths.get("deployment");
    }

    /**
     * Generate complete deployment package
     * @return true if deployment package created successfully
     */
    public boolean generateDeploymentPackage() {
        try {
            logger.info("Generating deployment package...");

            // Create deployment directory
            Files.createDirectories(deploymentDir);

            // Generate configuration files
            generateServerConfig();
            generateStartupScripts();
            generateDockerConfig();
            generateSystemdService();
            generateNetworkTestScript();
            generateDeploymentGuide();

            logger.info("Deployment package generated successfully in: {}", deploymentDir.toAbsolutePath());
            return true;

        } catch (Exception e) {
            logger.error("Failed to generate deployment package", e);
            return false;
        }
    }

    /**
     * Generate server configuration file for deployment
     */
    private void generateServerConfig() throws IOException {
        Path configFile = deploymentDir.resolve("ftp-server-production.properties");

        Properties deployConfig = new Properties();

        // Server settings
        deployConfig.setProperty("server.port", String.valueOf(config.getServerPort()));
        deployConfig.setProperty("server.data.port.start", String.valueOf(config.getDataPortStart()));
        deployConfig.setProperty("server.data.port.end", String.valueOf(config.getDataPortEnd()));
        deployConfig.setProperty("server.max.connections", String.valueOf(config.getMaxConnections()));
        deployConfig.setProperty("server.root.directory", "/opt/ftpserver/files");
        deployConfig.setProperty("server.timeout", String.valueOf(config.getTimeout()));

        // Security settings (production hardened)
        deployConfig.setProperty("security.enable.ip.banning", "true");
        deployConfig.setProperty("security.max.login.attempts", "3");
        deployConfig.setProperty("security.ban.duration.minutes", "30");
        deployConfig.setProperty("security.max.connections.per.ip", "3");
        deployConfig.setProperty("security.rate.limit.window.seconds", "60");
        deployConfig.setProperty("security.rate.limit.max.requests", "50");

        // Performance settings
        deployConfig.setProperty("performance.thread.pool.size", "20");
        deployConfig.setProperty("performance.enable.monitoring", "true");
        deployConfig.setProperty("performance.stats.interval.minutes", "10");

        // Logging settings
        deployConfig.setProperty("logging.level", "INFO");
        deployConfig.setProperty("logging.file.path", "/var/log/ftpserver");
        deployConfig.setProperty("logging.max.file.size", "100MB");
        deployConfig.setProperty("logging.max.files", "10");

        try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
            deployConfig.store(fos, "FTP Server Production Configuration");
        }

        logger.info("Generated production configuration: {}", configFile);
    }

    /**
     * Generate startup scripts for different platforms
     */
    private void generateStartupScripts() throws IOException {
        generateWindowsStartupScript();
        generateLinuxStartupScript();
        generateMacStartupScript();
    }

    /**
     * Generate Windows startup script
     */
    private void generateWindowsStartupScript() throws IOException {
        Path scriptFile = deploymentDir.resolve("start-ftp-server.bat");

        StringBuilder script = new StringBuilder();
        script.append("@echo off\n");
        script.append("title FTP Server\n");
        script.append("echo Starting FTP Server...\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Set JAVA_HOME if not set\n");
        script.append("if \"%JAVA_HOME%\"==\"\" (\n");
        script.append("    echo Warning: JAVA_HOME not set. Using java from PATH.\n");
        script.append("    set JAVA_CMD=java\n");
        script.append(") else (\n");
        script.append("    set JAVA_CMD=\"%JAVA_HOME%\\bin\\java\"\n");
        script.append(")\n");
        script.append("\n");
        script.append("REM Check if Java is available\n");
        script.append("%JAVA_CMD% -version >nul 2>&1\n");
        script.append("if errorlevel 1 (\n");
        script.append("    echo Error: Java not found. Please install Java 17 or later.\n");
        script.append("    pause\n");
        script.append("    exit /b 1\n");
        script.append(")\n");
        script.append("\n");
        script.append("REM Set server configuration\n");
        script.append("set CONFIG_FILE=ftp-server-production.properties\n");
        script.append("set LOG_CONFIG=log4j2-production.xml\n");
        script.append("set SERVER_JAR=ftp-server.jar\n");
        script.append("\n");
        script.append("REM Create directories\n");
        script.append("if not exist logs mkdir logs\n");
        script.append("if not exist files mkdir files\n");
        script.append("\n");
        script.append("REM Start server\n");
        script.append("echo Configuration: %CONFIG_FILE%\n");
        script.append("echo Log configuration: %LOG_CONFIG%\n");
        script.append("echo.\n");
        script.append("echo FTP Server is starting...\n");
        script.append("echo Press Ctrl+C to stop the server\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("%JAVA_CMD% -Dconfig.file=%CONFIG_FILE% -Dlog4j.configurationFile=%LOG_CONFIG% -jar %SERVER_JAR% server\n");
        script.append("\n");
        script.append("if errorlevel 1 (\n");
        script.append("    echo.\n");
        script.append("    echo Error: Server failed to start. Check the logs for details.\n");
        script.append("    pause\n");
        script.append(")\n");

        Files.write(scriptFile, script.toString().getBytes());
        logger.info("Generated Windows startup script: {}", scriptFile);
    }

    /**
     * Generate Linux startup script
     */
    private void generateLinuxStartupScript() throws IOException {
        Path scriptFile = deploymentDir.resolve("start-ftp-server.sh");

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("\n");
        script.append("# FTP Server Startup Script\n");
        script.append("# Usage: ./start-ftp-server.sh [config-file]\n");
        script.append("\n");
        script.append("set -e\n");
        script.append("\n");
        script.append("# Configuration\n");
        script.append("CONFIG_FILE=${1:-ftp-server-production.properties}\n");
        script.append("LOG_CONFIG=${LOG_CONFIG:-log4j2-production.xml}\n");
        script.append("SERVER_JAR=${SERVER_JAR:-ftp-server.jar}\n");
        script.append("JAVA_OPTS=${JAVA_OPTS:--Xmx512m -Xms256m}\n");
        script.append("\n");
        script.append("# Colors for output\n");
        script.append("RED='\\033[0;31m'\n");
        script.append("GREEN='\\033[0;32m'\n");
        script.append("YELLOW='\\033[1;33m'\n");
        script.append("NC='\\033[0m' # No Color\n");
        script.append("\n");
        script.append("echo -e \"${GREEN}FTP Server Startup${NC}\"\n");
        script.append("echo \"====================\"\n");
        script.append("\n");
        script.append("# Check Java installation\n");
        script.append("if ! command -v java &> /dev/null; then\n");
        script.append("    echo -e \"${RED}Error: Java not found. Please install Java 17 or later.${NC}\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Check Java version\n");
        script.append("JAVA_VERSION=$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | awk -F'.' '{print $1}')\n");
        script.append("if [ \"$JAVA_VERSION\" -lt 17 ]; then\n");
        script.append("    echo -e \"${YELLOW}Warning: Java version $JAVA_VERSION detected. Java 17+ recommended.${NC}\"\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Check required files\n");
        script.append("if [ ! -f \"$SERVER_JAR\" ]; then\n");
        script.append("    echo -e \"${RED}Error: Server JAR file not found: $SERVER_JAR${NC}\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("\n");
        script.append("if [ ! -f \"$CONFIG_FILE\" ]; then\n");
        script.append("    echo -e \"${YELLOW}Warning: Configuration file not found: $CONFIG_FILE${NC}\"\n");
        script.append("    echo \"Using default configuration...\"\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Create directories\n");
        script.append("mkdir -p logs files\n");
        script.append("\n");
        script.append("# Set file permissions\n");
        script.append("chmod 755 files\n");
        script.append("chmod 755 logs\n");
        script.append("\n");
        script.append("# Display configuration\n");
        script.append("echo \"Configuration file: $CONFIG_FILE\"\n");
        script.append("echo \"Log configuration: $LOG_CONFIG\"\n");
        script.append("echo \"Java options: $JAVA_OPTS\"\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Function to handle cleanup on exit\n");
        script.append("cleanup() {\n");
        script.append("    echo\n");
        script.append("    echo -e \"${YELLOW}Shutting down FTP Server...${NC}\"\n");
        script.append("    exit 0\n");
        script.append("}\n");
        script.append("\n");
        script.append("# Set up signal handlers\n");
        script.append("trap cleanup SIGINT SIGTERM\n");
        script.append("\n");
        script.append("# Start server\n");
        script.append("echo -e \"${GREEN}Starting FTP Server...${NC}\"\n");
        script.append("echo \"Press Ctrl+C to stop the server\"\n");
        script.append("echo\n");
        script.append("\n");
        script.append("java $JAVA_OPTS \\\n");
        script.append("    -Dconfig.file=\"$CONFIG_FILE\" \\\n");
        script.append("    -Dlog4j.configurationFile=\"$LOG_CONFIG\" \\\n");
        script.append("    -jar \"$SERVER_JAR\" server\n");
        script.append("\n");
        script.append("# Check exit status\n");
        script.append("if [ $? -ne 0 ]; then\n");
        script.append("    echo\n");
        script.append("    echo -e \"${RED}Error: Server failed to start. Check the logs for details.${NC}\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");

        Files.write(scriptFile, script.toString().getBytes());

        // Make script executable (if on Unix-like system)
        try {
            scriptFile.toFile().setExecutable(true);
        } catch (Exception e) {
            logger.debug("Could not set executable permission: {}", e.getMessage());
        }

        logger.info("Generated Linux startup script: {}", scriptFile);
    }

    /**
     * Generate macOS startup script
     */
    private void generateMacStartupScript() throws IOException {
        Path scriptFile = deploymentDir.resolve("start-ftp-server-mac.sh");

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("\n");
        script.append("# FTP Server Startup Script for macOS\n");
        script.append("# Usage: ./start-ftp-server-mac.sh [config-file]\n");
        script.append("\n");
        script.append("set -e\n");
        script.append("\n");
        script.append("# Configuration\n");
        script.append("CONFIG_FILE=${1:-ftp-server-production.properties}\n");
        script.append("LOG_CONFIG=${LOG_CONFIG:-log4j2-production.xml}\n");
        script.append("SERVER_JAR=${SERVER_JAR:-ftp-server.jar}\n");
        script.append("JAVA_OPTS=${JAVA_OPTS:--Xmx512m -Xms256m}\n");
        script.append("\n");
        script.append("echo \"🚀 FTP Server for macOS\"\n");
        script.append("echo \"======================\"\n");
        script.append("\n");
        script.append("# Check for Java (try different locations)\n");
        script.append("JAVA_CMD=\"java\"\n");
        script.append("\n");
        script.append("if [ -n \"$JAVA_HOME\" ]; then\n");
        script.append("    JAVA_CMD=\"$JAVA_HOME/bin/java\"\n");
        script.append("elif [ -f \"/usr/libexec/java_home\" ]; then\n");
        script.append("    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)\n");
        script.append("    if [ -n \"$JAVA_HOME\" ]; then\n");
        script.append("        JAVA_CMD=\"$JAVA_HOME/bin/java\"\n");
        script.append("    fi\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Verify Java installation\n");
        script.append("if ! command -v \"$JAVA_CMD\" &> /dev/null; then\n");
        script.append("    echo \"❌ Java not found. Please install Java 17 or later.\"\n");
        script.append("    echo \"   Download from: https://adoptium.net/\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Check required files\n");
        script.append("if [ ! -f \"$SERVER_JAR\" ]; then\n");
        script.append("    echo \"❌ Server JAR file not found: $SERVER_JAR\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Create directories\n");
        script.append("mkdir -p logs files\n");
        script.append("\n");
        script.append("# Check for macOS firewall\n");
        script.append("if /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate | grep -q \"enabled\"; then\n");
        script.append("    echo \"⚠️  macOS Firewall is enabled. You may need to allow the FTP server.\"\n");
        script.append("fi\n");
        script.append("\n");
        script.append("echo \"📁 Files directory: $(pwd)/files\"\n");
        script.append("echo \"📝 Logs directory: $(pwd)/logs\"\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Start server\n");
        script.append("echo \"🎯 Starting FTP Server...\"\n");
        script.append("echo \"   Press Ctrl+C to stop the server\"\n");
        script.append("echo\n");
        script.append("\n");
        script.append("\"$JAVA_CMD\" $JAVA_OPTS \\\n");
        script.append("    -Dconfig.file=\"$CONFIG_FILE\" \\\n");
        script.append("    -Dlog4j.configurationFile=\"$LOG_CONFIG\" \\\n");
        script.append("    -jar \"$SERVER_JAR\" server\n");

        Files.write(scriptFile, script.toString().getBytes());

        // Make script executable
        try {
            scriptFile.toFile().setExecutable(true);
        } catch (Exception e) {
            logger.debug("Could not set executable permission: {}", e.getMessage());
        }

        logger.info("Generated macOS startup script: {}", scriptFile);
    }

    /**
     * Generate Docker configuration
     */
    private void generateDockerConfig() throws IOException {
        generateDockerfile();
        generateDockerCompose();
        generateDockerIgnore();
    }

    /**
     * Generate Dockerfile
     */
    private void generateDockerfile() throws IOException {
        Path dockerFile = deploymentDir.resolve("Dockerfile");

        StringBuilder dockerfile = new StringBuilder();
        dockerfile.append("# FTP Server Docker Image\n");
        dockerfile.append("FROM openjdk:17-jre-slim\n");
        dockerfile.append("\n");
        dockerfile.append("# Metadata\n");
        dockerfile.append("LABEL maintainer=\"FTP Server Team\"\n");
        dockerfile.append("LABEL description=\"University FTP Server Project\"\n");
        dockerfile.append("LABEL version=\"1.0\"\n");
        dockerfile.append("\n");
        dockerfile.append("# Install required packages\n");
        dockerfile.append("RUN apt-get update && apt-get install -y \\\n");
        dockerfile.append("    curl \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n");
        dockerfile.append("\n");
        dockerfile.append("# Create application directory\n");
        dockerfile.append("WORKDIR /opt/ftpserver\n");
        dockerfile.append("\n");
        dockerfile.append("# Create non-root user\n");
        dockerfile.append("RUN groupadd -r ftpuser && useradd -r -g ftpuser ftpuser\n");
        dockerfile.append("\n");
        dockerfile.append("# Copy application files\n");
        dockerfile.append("COPY ftp-server.jar .\n");
        dockerfile.append("COPY ftp-server-production.properties .\n");
        dockerfile.append("COPY log4j2-production.xml .\n");
        dockerfile.append("\n");
        dockerfile.append("# Create directories\n");
        dockerfile.append("RUN mkdir -p /opt/ftpserver/files /opt/ftpserver/logs \\\n");
        dockerfile.append("    && chown -R ftpuser:ftpuser /opt/ftpserver\n");
        dockerfile.append("\n");
        dockerfile.append("# Switch to non-root user\n");
        dockerfile.append("USER ftpuser\n");
        dockerfile.append("\n");
        dockerfile.append("# Expose ports\n");
        dockerfile.append("EXPOSE ").append(config.getServerPort()).append("\n");
        dockerfile.append("EXPOSE ").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd()).append("\n");
        dockerfile.append("\n");
        dockerfile.append("# Health check\n");
        dockerfile.append("HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \\\n");
        dockerfile.append("    CMD curl -f http://localhost:").append(config.getServerPort()).append("/ || exit 1\n");
        dockerfile.append("\n");
        dockerfile.append("# Default command\n");
        dockerfile.append("CMD [\"java\", \"-Dconfig.file=ftp-server-production.properties\", \"-Dlog4j.configurationFile=log4j2-production.xml\", \"-jar\", \"ftp-server.jar\", \"server\"]\n");

        Files.write(dockerFile, dockerfile.toString().getBytes());
        logger.info("Generated Dockerfile: {}", dockerFile);
    }

    /**
     * Generate Docker Compose configuration
     */
    private void generateDockerCompose() throws IOException {
        Path composeFile = deploymentDir.resolve("docker-compose.yml");

        StringBuilder compose = new StringBuilder();
        compose.append("version: '3.8'\n");
        compose.append("\n");
        compose.append("services:\n");
        compose.append("  ftp-server:\n");
        compose.append("    build: .\n");
        compose.append("    container_name: ftp-server\n");
        compose.append("    ports:\n");
        compose.append("      - \"").append(config.getServerPort()).append(":").append(config.getServerPort()).append("\"\n");
        compose.append("      - \"").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd());
        compose.append(":").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd()).append("\"\n");
        compose.append("    volumes:\n");
        compose.append("      - ftp-files:/opt/ftpserver/files\n");
        compose.append("      - ftp-logs:/opt/ftpserver/logs\n");
        compose.append("      - ./ftp-server-production.properties:/opt/ftpserver/ftp-server-production.properties:ro\n");
        compose.append("    environment:\n");
        compose.append("      - JAVA_OPTS=-Xmx512m -Xms256m\n");
        compose.append("    restart: unless-stopped\n");
        compose.append("    networks:\n");
        compose.append("      - ftp-network\n");
        compose.append("    healthcheck:\n");
        compose.append("      test: [\"CMD\", \"java\", \"-cp\", \"ftp-server.jar\", \"utils.NetworkUtils\", \"localhost\", \"").append(config.getServerPort()).append("\"]\n");
        compose.append("      interval: 30s\n");
        compose.append("      timeout: 10s\n");
        compose.append("      retries: 3\n");
        compose.append("      start_period: 30s\n");
        compose.append("\n");
        compose.append("volumes:\n");
        compose.append("  ftp-files:\n");
        compose.append("    driver: local\n");
        compose.append("  ftp-logs:\n");
        compose.append("    driver: local\n");
        compose.append("\n");
        compose.append("networks:\n");
        compose.append("  ftp-network:\n");
        compose.append("    driver: bridge\n");

        Files.write(composeFile, compose.toString().getBytes());
        logger.info("Generated Docker Compose: {}", composeFile);
    }

    /**
     * Generate .dockerignore file
     */
    private void generateDockerIgnore() throws IOException {
        Path dockerIgnore = deploymentDir.resolve(".dockerignore");

        StringBuilder ignore = new StringBuilder();
        ignore.append("# Git\n");
        ignore.append(".git\n");
        ignore.append(".gitignore\n");
        ignore.append("\n");
        ignore.append("# IDE\n");
        ignore.append(".idea\n");
        ignore.append("*.iml\n");
        ignore.append(".vscode\n");
        ignore.append("\n");
        ignore.append("# Build\n");
        ignore.append("target/\n");
        ignore.append("*.class\n");
        ignore.append("\n");
        ignore.append("# Logs\n");
        ignore.append("logs/\n");
        ignore.append("*.log\n");
        ignore.append("\n");
        ignore.append("# Documentation\n");
        ignore.append("docs/\n");
        ignore.append("README.md\n");
        ignore.append("\n");
        ignore.append("# Test files\n");
        ignore.append("src/test/\n");
        ignore.append("screenshots/\n");

        Files.write(dockerIgnore, ignore.toString().getBytes());
        logger.info("Generated .dockerignore: {}", dockerIgnore);
    }

    /**
     * Generate systemd service file for Linux
     */
    private void generateSystemdService() throws IOException {
        Path serviceFile = deploymentDir.resolve("ftp-server.service");

        StringBuilder service = new StringBuilder();
        service.append("[Unit]\n");
        service.append("Description=FTP Server\n");
        service.append("After=network.target\n");
        service.append("Wants=network.target\n");
        service.append("\n");
        service.append("[Service]\n");
        service.append("Type=simple\n");
        service.append("User=ftpserver\n");
        service.append("Group=ftpserver\n");
        service.append("WorkingDirectory=/opt/ftpserver\n");
        service.append("ExecStart=/usr/bin/java -Dconfig.file=/opt/ftpserver/ftp-server-production.properties -Dlog4j.configurationFile=/opt/ftpserver/log4j2-production.xml -jar /opt/ftpserver/ftp-server.jar server\n");
        service.append("ExecReload=/bin/kill -HUP $MAINPID\n");
        service.append("KillMode=process\n");
        service.append("Restart=on-failure\n");
        service.append("RestartSec=10\n");
        service.append("\n");
        service.append("# Security settings\n");
        service.append("NoNewPrivileges=true\n");
        service.append("PrivateTmp=true\n");
        service.append("ProtectSystem=strict\n");
        service.append("ReadWritePaths=/opt/ftpserver/files /opt/ftpserver/logs\n");
        service.append("\n");
        service.append("# Resource limits\n");
        service.append("LimitNOFILE=65536\n");
        service.append("MemoryLimit=1G\n");
        service.append("\n");
        service.append("[Install]\n");
        service.append("WantedBy=multi-user.target\n");

        Files.write(serviceFile, service.toString().getBytes());
        logger.info("Generated systemd service: {}", serviceFile);
    }

    /**
     * Generate network connectivity test script
     */
    private void generateNetworkTestScript() throws IOException {
        generateNetworkTestBat();
        generateNetworkTestSh();
    }

    /**
     * Generate Windows network test script
     */
    private void generateNetworkTestBat() throws IOException {
        Path testScript = deploymentDir.resolve("test-network.bat");

        StringBuilder script = new StringBuilder();
        script.append("@echo off\n");
        script.append("title FTP Server Network Test\n");
        script.append("echo FTP Server Network Connectivity Test\n");
        script.append("echo ======================================\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("set SERVER_HOST=%1\n");
        script.append("set SERVER_PORT=%2\n");
        script.append("\n");
        script.append("if \"%SERVER_HOST%\"==\"\" set SERVER_HOST=localhost\n");
        script.append("if \"%SERVER_PORT%\"==\"\" set SERVER_PORT=").append(config.getServerPort()).append("\n");
        script.append("\n");
        script.append("echo Testing connection to %SERVER_HOST%:%SERVER_PORT%\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Test ping\n");
        script.append("echo Testing ping...\n");
        script.append("ping -n 4 %SERVER_HOST%\n");
        script.append("if errorlevel 1 (\n");
        script.append("    echo Warning: Ping failed. Host may not respond to ping.\n");
        script.append(") else (\n");
        script.append("    echo Ping successful.\n");
        script.append(")\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Test port connectivity\n");
        script.append("echo Testing port connectivity...\n");
        script.append("powershell -Command \"Test-NetConnection -ComputerName %SERVER_HOST% -Port %SERVER_PORT% -InformationLevel Detailed\"\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Test FTP connection\n");
        script.append("echo Testing FTP protocol...\n");
        script.append("echo Attempting to connect with FTP client...\n");
        script.append("echo (This will show FTP server response if available)\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Create temporary FTP script\n");
        script.append("echo open %SERVER_HOST% %SERVER_PORT% > temp_ftp.txt\n");
        script.append("echo quit >> temp_ftp.txt\n");
        script.append("\n");
        script.append("REM Try FTP connection\n");
        script.append("ftp -s:temp_ftp.txt\n");
        script.append("del temp_ftp.txt\n");
        script.append("\n");
        script.append("echo.\n");
        script.append("echo Network test completed.\n");
        script.append("echo If you see FTP server responses above, the server is accessible.\n");
        script.append("pause\n");

        Files.write(testScript, script.toString().getBytes());
        logger.info("Generated Windows network test script: {}", testScript);
    }

    /**
     * Generate Unix network test script
     */
    private void generateNetworkTestSh() throws IOException {
        Path testScript = deploymentDir.resolve("test-network.sh");

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("\n");
        script.append("# FTP Server Network Connectivity Test\n");
        script.append("# Usage: ./test-network.sh [host] [port]\n");
        script.append("\n");
        script.append("SERVER_HOST=${1:-localhost}\n");
        script.append("SERVER_PORT=${2:-").append(config.getServerPort()).append("}\n");
        script.append("\n");
        script.append("# Colors\n");
        script.append("RED='\\033[0;31m'\n");
        script.append("GREEN='\\033[0;32m'\n");
        script.append("YELLOW='\\033[1;33m'\n");
        script.append("BLUE='\\033[0;34m'\n");
        script.append("NC='\\033[0m'\n");
        script.append("\n");
        script.append("echo -e \"${BLUE}FTP Server Network Connectivity Test${NC}\"\n");
        script.append("echo \"======================================\"\n");
        script.append("echo\n");
        script.append("echo \"Testing connection to $SERVER_HOST:$SERVER_PORT\"\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Test 1: Ping\n");
        script.append("echo -e \"${YELLOW}1. Testing ping...${NC}\"\n");
        script.append("if ping -c 4 \"$SERVER_HOST\" > /dev/null 2>&1; then\n");
        script.append("    echo -e \"${GREEN}✓ Ping successful${NC}\"\n");
        script.append("else\n");
        script.append("    echo -e \"${YELLOW}⚠ Ping failed (host may not respond to ping)${NC}\"\n");
        script.append("fi\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Test 2: Port connectivity\n");
        script.append("echo -e \"${YELLOW}2. Testing port connectivity...${NC}\"\n");
        script.append("if command -v nc > /dev/null 2>&1; then\n");
        script.append("    if nc -z \"$SERVER_HOST\" \"$SERVER_PORT\" 2>/dev/null; then\n");
        script.append("        echo -e \"${GREEN}✓ Port $SERVER_PORT is accessible${NC}\"\n");
        script.append("    else\n");
        script.append("        echo -e \"${RED}✗ Port $SERVER_PORT is not accessible${NC}\"\n");
        script.append("    fi\n");
        script.append("elif command -v telnet > /dev/null 2>&1; then\n");
        script.append("    if timeout 5 telnet \"$SERVER_HOST\" \"$SERVER_PORT\" < /dev/null 2>/dev/null | grep -q \"Connected\\|220\"; then\n");
        script.append("        echo -e \"${GREEN}✓ Port $SERVER_PORT is accessible${NC}\"\n");
        script.append("    else\n");
        script.append("        echo -e \"${RED}✗ Port $SERVER_PORT is not accessible${NC}\"\n");
        script.append("    fi\n");
        script.append("else\n");
        script.append("    echo -e \"${YELLOW}⚠ No network testing tools available (nc/telnet)${NC}\"\n");
        script.append("fi\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Test 3: FTP protocol\n");
        script.append("echo -e \"${YELLOW}3. Testing FTP protocol...${NC}\"\n");
        script.append("if command -v ftp > /dev/null 2>&1; then\n");
        script.append("    echo \"Attempting FTP connection...\"\n");
        script.append("    (\n");
        script.append("        echo \"open $SERVER_HOST $SERVER_PORT\"\n");
        script.append("        sleep 2\n");
        script.append("        echo \"quit\"\n");
        script.append("    ) | ftp -n 2>&1 | head -10\n");
        script.append("else\n");
        script.append("    echo -e \"${YELLOW}⚠ FTP client not available${NC}\"\n");
        script.append("fi\n");
        script.append("echo\n");
        script.append("\n");
        script.append("# Test 4: Firewall check\n");
        script.append("echo -e \"${YELLOW}4. Checking local firewall...${NC}\"\n");
        script.append("if command -v ufw > /dev/null 2>&1; then\n");
        script.append("    if ufw status | grep -q \"Status: active\"; then\n");
        script.append("        echo -e \"${YELLOW}⚠ UFW firewall is active${NC}\"\n");
        script.append("        echo \"Check if port $SERVER_PORT is allowed:\"\n");
        script.append("        ufw status | grep \"$SERVER_PORT\" || echo \"Port $SERVER_PORT not found in UFW rules\"\n");
        script.append("    else\n");
        script.append("        echo -e \"${GREEN}✓ UFW firewall is inactive${NC}\"\n");
        script.append("    fi\n");
        script.append("elif command -v iptables > /dev/null 2>&1; then\n");
        script.append("    echo \"Checking iptables rules...\"\n");
        script.append("    if iptables -L INPUT | grep -q \"$SERVER_PORT\"; then\n");
        script.append("        echo -e \"${GREEN}✓ Found iptables rule for port $SERVER_PORT${NC}\"\n");
        script.append("    else\n");
        script.append("        echo -e \"${YELLOW}⚠ No specific iptables rule found for port $SERVER_PORT${NC}\"\n");
        script.append("    fi\n");
        script.append("else\n");
        script.append("    echo \"No firewall tools detected\"\n");
        script.append("fi\n");
        script.append("echo\n");
        script.append("\n");
        script.append("echo -e \"${BLUE}Network test completed.${NC}\"\n");
        script.append("echo \"If you see FTP server responses above, the server is accessible.\"\n");

        Files.write(testScript, script.toString().getBytes());

        // Make executable
        try {
            testScript.toFile().setExecutable(true);
        } catch (Exception e) {
            logger.debug("Could not set executable permission: {}", e.getMessage());
        }

        logger.info("Generated Unix network test script: {}", testScript);
    }

    /**
     * Generate comprehensive deployment guide
     */
    private void generateDeploymentGuide() throws IOException {
        Path guideFile = deploymentDir.resolve("DEPLOYMENT-GUIDE.md");

        StringBuilder guide = new StringBuilder();
        guide.append("# FTP Server Deployment Guide\n\n");

        guide.append("## Quick Start\n\n");
        guide.append("### 1. Local Testing\n");
        guide.append("```bash\n");
        guide.append("# Windows\n");
        guide.append("start-ftp-server.bat\n\n");
        guide.append("# Linux/macOS\n");
        guide.append("./start-ftp-server.sh\n");
        guide.append("```\n\n");

        guide.append("### 2. Network Testing\n");
        guide.append("```bash\n");
        guide.append("# Test connectivity\n");
        guide.append("./test-network.sh [hostname] [port]\n\n");
        guide.append("# Test from another machine\n");
        guide.append("./test-network.sh 192.168.1.100 ").append(config.getServerPort()).append("\n");
        guide.append("```\n\n");

        guide.append("## Deployment Options\n\n");

        guide.append("### Option 1: Direct Java Deployment\n\n");
        guide.append("**Requirements:**\n");
        guide.append("- Java 17 or later\n");
        guide.append("- Network access to required ports\n");
        guide.append("- Firewall configuration\n\n");

        guide.append("**Steps:**\n");
        guide.append("1. Copy the JAR file and configuration to target server\n");
        guide.append("2. Configure firewall rules\n");
        guide.append("3. Run the startup script\n\n");

        guide.append("### Option 2: Docker Deployment\n\n");
        guide.append("**Build and run:**\n");
        guide.append("```bash\n");
        guide.append("# Build image\n");
        guide.append("docker build -t ftp-server .\n\n");
        guide.append("# Run container\n");
        guide.append("docker run -d \\\n");
        guide.append("  --name ftp-server \\\n");
        guide.append("  -p ").append(config.getServerPort()).append(":").append(config.getServerPort()).append(" \\\n");
        guide.append("  -p ").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd());
        guide.append(":").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd()).append(" \\\n");
        guide.append("  -v ftp-files:/opt/ftpserver/files \\\n");
        guide.append("  ftp-server\n\n");
        guide.append("# Or use Docker Compose\n");
        guide.append("docker-compose up -d\n");
        guide.append("```\n\n");

        guide.append("### Option 3: Linux Service (systemd)\n\n");
        guide.append("**Setup:**\n");
        guide.append("```bash\n");
        guide.append("# Create user\n");
        guide.append("sudo useradd -r -s /bin/false ftpserver\n\n");
        guide.append("# Create directories\n");
        guide.append("sudo mkdir -p /opt/ftpserver/{files,logs}\n");
        guide.append("sudo chown -R ftpserver:ftpserver /opt/ftpserver\n\n");
        guide.append("# Copy files\n");
        guide.append("sudo cp ftp-server.jar /opt/ftpserver/\n");
        guide.append("sudo cp ftp-server-production.properties /opt/ftpserver/\n");
        guide.append("sudo cp log4j2-production.xml /opt/ftpserver/\n\n");
        guide.append("# Install service\n");
        guide.append("sudo cp ftp-server.service /etc/systemd/system/\n");
        guide.append("sudo systemctl daemon-reload\n");
        guide.append("sudo systemctl enable ftp-server\n");
        guide.append("sudo systemctl start ftp-server\n\n");
        guide.append("# Check status\n");
        guide.append("sudo systemctl status ftp-server\n");
        guide.append("```\n\n");

        guide.append("## Network Configuration\n\n");
        guide.append(NetworkConfiguration.generateDeploymentInstructions(config)).append("\n\n");

        guide.append("## Security Considerations\n\n");
        guide.append("### Firewall Configuration\n");
        guide.append("**Required ports:**\n");
        guide.append("- ").append(config.getServerPort()).append("/tcp (FTP Control)\n");
        guide.append("- ").append(config.getDataPortStart()).append("-").append(config.getDataPortEnd()).append("/tcp (FTP Data)\n\n");

        guide.append("### User Management\n");
        guide.append("**Default users (change for production):**\n");
        guide.append("- admin/admin123 (full access)\n");
        guide.append("- user/user123 (limited access)\n");
        guide.append("- test/test (read-only)\n\n");

        guide.append("**To add users:**\n");
        guide.append("1. Modify the user configuration in the properties file\n");
        guide.append("2. Restart the server\n");
        guide.append("3. Test new user credentials\n\n");

        guide.append("### Production Hardening\n");
        guide.append("1. **Change default passwords**\n");
        guide.append("2. **Enable IP-based restrictions**\n");
        guide.append("3. **Configure rate limiting**\n");
        guide.append("4. **Enable detailed logging**\n");
        guide.append("5. **Set up log rotation**\n");
        guide.append("6. **Monitor server performance**\n");
        guide.append("7. **Regular security updates**\n\n");

        guide.append("## Monitoring and Maintenance\n\n");
        guide.append("### Health Checks\n");
        guide.append("```bash\n");
        guide.append("# Check if server is responding\n");
        guide.append("telnet [server-ip] ").append(config.getServerPort()).append("\n\n");
        guide.append("# Check server logs\n");
        guide.append("tail -f logs/ftp-server.log\n\n");
        guide.append("# Monitor connections\n");
        guide.append("netstat -an | grep :").append(config.getServerPort()).append("\n");
        guide.append("```\n\n");

        guide.append("### Log Management\n");
        guide.append("**Log files:**\n");
        guide.append("- `logs/ftp-server.log` - Main server log\n");
        guide.append("- `logs/ftp-client.log` - Client operation log\n");
        guide.append("- `logs/security.log` - Security events\n\n");

        guide.append("**Log rotation (Linux):**\n");
        guide.append("```bash\n");
        guide.append("# Create logrotate configuration\n");
        guide.append("sudo tee /etc/logrotate.d/ftp-server << EOF\n");
        guide.append("/opt/ftpserver/logs/*.log {\n");
        guide.append("    daily\n");
        guide.append("    rotate 30\n");
        guide.append("    compress\n");
        guide.append("    delaycompress\n");
        guide.append("    missingok\n");
        guide.append("    notifempty\n");
        guide.append("    copytruncate\n");
        guide.append("}\n");
        guide.append("EOF\n");
        guide.append("```\n\n");

        guide.append("## Troubleshooting\n\n");
        guide.append("### Common Issues\n\n");
        guide.append("**1. Cannot connect to server**\n");
        guide.append("- Check if server is running\n");
        guide.append("- Verify firewall settings\n");
        guide.append("- Test network connectivity\n");
        guide.append("- Check server logs for errors\n\n");

        guide.append("**2. Authentication failures**\n");
        guide.append("- Verify username/password\n");
        guide.append("- Check user configuration\n");
        guide.append("- Review security logs\n\n");

        guide.append("**3. File transfer issues**\n");
        guide.append("- Check file permissions\n");
        guide.append("- Verify data port range\n");
        guide.append("- Test with different clients\n\n");

        guide.append("**4. Performance problems**\n");
        guide.append("- Monitor server resources\n");
        guide.append("- Check network bandwidth\n");
        guide.append("- Review connection limits\n");
        guide.append("- Analyze server statistics\n\n");

        guide.append("### Diagnostic Commands\n");
        guide.append("```bash\n");
        guide.append("# Check server process\n");
        guide.append("ps aux | grep ftp-server\n\n");
        guide.append("# Check listening ports\n");
        guide.append("netstat -tlnp | grep java\n\n");
        guide.append("# Check system resources\n");
        guide.append("top -p $(pgrep -f ftp-server)\n\n");
        guide.append("# Test FTP connection\n");
        guide.append("java Main client localhost ").append(config.getServerPort()).append("\n");
        guide.append("```\n\n");

        guide.append("## Performance Tuning\n\n");
        guide.append("### JVM Options\n");
        guide.append("**For production servers:**\n");
        guide.append("```bash\n");
        guide.append("JAVA_OPTS=\"-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200\"\n");
        guide.append("```\n\n");

        guide.append("### Configuration Tuning\n");
        guide.append("**High-load servers:**\n");
        guide.append("- Increase `server.max.connections`\n");
        guide.append("- Adjust `transfer.buffer.size`\n");
        guide.append("- Tune thread pool size\n");
        guide.append("- Configure connection timeouts\n\n");

        guide.append("## Backup and Recovery\n\n");
        guide.append("### Backup Strategy\n");
        guide.append("**What to backup:**\n");
        guide.append("1. Configuration files\n");
        guide.append("2. User data (files directory)\n");
        guide.append("3. Log files (for audit)\n");
        guide.append("4. User credentials\n\n");

        guide.append("**Backup script example:**\n");
        guide.append("```bash\n");
        guide.append("#!/bin/bash\n");
        guide.append("BACKUP_DIR=\"/backup/ftp-server-$(date +%Y%m%d)\"\n");
        guide.append("mkdir -p \"$BACKUP_DIR\"\n");
        guide.append("tar czf \"$BACKUP_DIR/config.tar.gz\" *.properties *.xml\n");
        guide.append("tar czf \"$BACKUP_DIR/files.tar.gz\" files/\n");
        guide.append("tar czf \"$BACKUP_DIR/logs.tar.gz\" logs/\n");
        guide.append("```\n\n");

        guide.append("## Support and Documentation\n\n");
        guide.append("**Additional resources:**\n");
        guide.append("- Server documentation: `docs/`\n");
        guide.append("- Client usage guide: `docs/CLIENT-USAGE.md`\n");
        guide.append("- GUI client guide: `docs/GUI-USAGE.md`\n");
        guide.append("- Advanced features: `docs/ADVANCED-FEATURES.md`\n\n");

        guide.append("**Getting help:**\n");
        guide.append("1. Check the logs for error messages\n");
        guide.append("2. Run network connectivity tests\n");
        guide.append("3. Review configuration settings\n");
        guide.append("4. Test with different clients\n");
        guide.append("5. Consult the troubleshooting section\n");

        Files.write(guideFile, guide.toString().getBytes());
        logger.info("Generated deployment guide: {}", guideFile);
    }

    /**
     * Get deployment directory
     * @return Deployment directory path
     */
    public Path getDeploymentDirectory() {
        return deploymentDir;
    }
}