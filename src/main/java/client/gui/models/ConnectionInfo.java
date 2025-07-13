package client.gui.models;

import javafx.beans.property.*;

/**
 * Model class for FTP connection information
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ConnectionInfo {
    private final StringProperty hostname;
    private final IntegerProperty port;
    private final StringProperty username;
    private final StringProperty password;
    private final BooleanProperty savePassword;
    private final BooleanProperty connected;
    private final BooleanProperty authenticated;

    /**
     * Default constructor
     */
    public ConnectionInfo() {
        this.hostname = new SimpleStringProperty("localhost");
        this.port = new SimpleIntegerProperty(21);
        this.username = new SimpleStringProperty("");
        this.password = new SimpleStringProperty("");
        this.savePassword = new SimpleBooleanProperty(false);
        this.connected = new SimpleBooleanProperty(false);
        this.authenticated = new SimpleBooleanProperty(false);
    }

    /**
     * Constructor with parameters
     */
    public ConnectionInfo(String hostname, int port, String username, String password) {
        this();
        setHostname(hostname);
        setPort(port);
        setUsername(username);
        setPassword(password);
    }

    // Property getters
    public StringProperty hostnameProperty() { return hostname; }
    public IntegerProperty portProperty() { return port; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public BooleanProperty savePasswordProperty() { return savePassword; }
    public BooleanProperty connectedProperty() { return connected; }
    public BooleanProperty authenticatedProperty() { return authenticated; }

    // Value getters
    public String getHostname() { return hostname.get(); }
    public int getPort() { return port.get(); }
    public String getUsername() { return username.get(); }
    public String getPassword() { return password.get(); }
    public boolean isSavePassword() { return savePassword.get(); }
    public boolean isConnected() { return connected.get(); }
    public boolean isAuthenticated() { return authenticated.get(); }

    // Value setters
    public void setHostname(String hostname) { this.hostname.set(hostname); }
    public void setPort(int port) { this.port.set(port); }
    public void setUsername(String username) { this.username.set(username); }
    public void setPassword(String password) { this.password.set(password); }
    public void setSavePassword(boolean savePassword) { this.savePassword.set(savePassword); }
    public void setConnected(boolean connected) { this.connected.set(connected); }
    public void setAuthenticated(boolean authenticated) { this.authenticated.set(authenticated); }

    /**
     * Get connection string for display
     */
    public String getConnectionString() {
        if (isConnected()) {
            return String.format("%s@%s:%d", getUsername(), getHostname(), getPort());
        } else {
            return "Not connected";
        }
    }

    /**
     * Clear connection status
     */
    public void clearConnection() {
        setConnected(false);
        setAuthenticated(false);
    }

    /**
     * Validate connection parameters
     */
    public boolean isValid() {
        return getHostname() != null && !getHostname().trim().isEmpty() &&
                getPort() > 0 && getPort() <= 65535 &&
                getUsername() != null && !getUsername().trim().isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ConnectionInfo{hostname='%s', port=%d, username='%s', connected=%s}",
                getHostname(), getPort(), getUsername(), isConnected());
    }
}