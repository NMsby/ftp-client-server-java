package common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration management for FTP client and server
 * Handles loading and accessing configuration properties
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPConfig {
    private static final String DEFAULT_CONFIG_FILE = "ftp-config.properties";
    private static FTPConfig instance;
    private Properties properties;

    // Default configuration values
    private static final int DEFAULT_SERVER_PORT = 21;
    private static final int DEFAULT_DATA_PORT_START = 20000;
    private static final int DEFAULT_DATA_PORT_END = 21000;
    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final String DEFAULT_SERVER_ROOT = "./server-files";
    private static final String DEFAULT_CLIENT_ROOT = "./client-files";

    /**
     * Private constructor for singleton pattern
     */
    private FTPConfig() {
        loadConfiguration();
    }

    /**
     * Get singleton instance
     * @return FTPConfig instance
     */
    public static synchronized FTPConfig getInstance() {
        if (instance == null) {
            instance = new FTPConfig();
        }
        return instance;
    }

    /**
     * Load configuration from properties file
     */
    private void loadConfiguration() {
        properties = new Properties();

        // Load default properties
        setDefaultProperties();

        // Try to load from classpath
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                System.out.println("Configuration loaded from " + DEFAULT_CONFIG_FILE);
            } else {
                System.out.println("Configuration file not found, using defaults");
            }
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.out.println("Using default configuration");
        }
    }

    /**
     * Set default configuration properties
     */
    private void setDefaultProperties() {
        properties.setProperty("server.port", String.valueOf(DEFAULT_SERVER_PORT));
        properties.setProperty("server.data.port.start", String.valueOf(DEFAULT_DATA_PORT_START));
        properties.setProperty("server.data.port.end", String.valueOf(DEFAULT_DATA_PORT_END));
        properties.setProperty("server.max.connections", String.valueOf(DEFAULT_MAX_CONNECTIONS));
        properties.setProperty("server.root.directory", DEFAULT_SERVER_ROOT);
        properties.setProperty("server.timeout", String.valueOf(DEFAULT_TIMEOUT));

        properties.setProperty("client.root.directory", DEFAULT_CLIENT_ROOT);
        properties.setProperty("client.timeout", String.valueOf(DEFAULT_TIMEOUT));

        properties.setProperty("transfer.buffer.size", String.valueOf(DEFAULT_BUFFER_SIZE));
        properties.setProperty("logging.level", "INFO");
    }

    /**
     * Get server control port
     * @return Server port number
     */
    public int getServerPort() {
        return getIntProperty("server.port", DEFAULT_SERVER_PORT);
    }

    /**
     * Get data port range start
     * @return Start of data port range
     */
    public int getDataPortStart() {
        return getIntProperty("server.data.port.start", DEFAULT_DATA_PORT_START);
    }

    /**
     * Get data port range end
     * @return End of data port range
     */
    public int getDataPortEnd() {
        return getIntProperty("server.data.port.end", DEFAULT_DATA_PORT_END);
    }

    /**
     * Get maximum concurrent connections
     * @return Maximum connections
     */
    public int getMaxConnections() {
        return getIntProperty("server.max.connections", DEFAULT_MAX_CONNECTIONS);
    }

    /**
     * Get server root directory
     * @return Server root directory path
     */
    public String getServerRootDirectory() {
        return getStringProperty("server.root.directory", DEFAULT_SERVER_ROOT);
    }

    /**
     * Get client root directory
     * @return Client root directory path
     */
    public String getClientRootDirectory() {
        return getStringProperty("client.root.directory", DEFAULT_CLIENT_ROOT);
    }

    /**
     * Get connection timeout in milliseconds
     * @return Timeout value
     */
    public int getTimeout() {
        return getIntProperty("server.timeout", DEFAULT_TIMEOUT);
    }

    /**
     * Get client timeout in milliseconds
     * @return Client timeout value
     */
    public int getClientTimeout() {
        return getIntProperty("client.timeout", DEFAULT_TIMEOUT);
    }

    /**
     * Get transfer buffer size
     * @return Buffer size in bytes
     */
    public int getBufferSize() {
        return getIntProperty("transfer.buffer.size", DEFAULT_BUFFER_SIZE);
    }

    /**
     * Get logging level
     * @return Logging level string
     */
    public String getLoggingLevel() {
        return getStringProperty("logging.level", "INFO");
    }

    /**
     * Get string property with default value
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value
     */
    private String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get integer property with default value
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value
     */
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for property " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get boolean property with default value
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Set property value
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Get all properties
     * @return Properties object
     */
    public Properties getProperties() {
        return new Properties(properties);
    }

    /**
     * Print current configuration
     */
    public void printConfiguration() {
        System.out.println("=== FTP Configuration ===");
        System.out.println("Server Port: " + getServerPort());
        System.out.println("Data Port Range: " + getDataPortStart() + "-" + getDataPortEnd());
        System.out.println("Max Connections: " + getMaxConnections());
        System.out.println("Server Root: " + getServerRootDirectory());
        System.out.println("Client Root: " + getClientRootDirectory());
        System.out.println("Timeout: " + getTimeout() + "ms");
        System.out.println("Buffer Size: " + getBufferSize() + " bytes");
        System.out.println("Logging Level: " + getLoggingLevel());
        System.out.println("========================");
    }
}