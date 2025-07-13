package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple user management for FTP server
 * In a production environment, this would integrate with a proper authentication system
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class UserManager {
    private static UserManager instance;
    private final Map<String, User> users;

    /**
     * Private constructor for singleton pattern
     */
    private UserManager() {
        users = new ConcurrentHashMap<>();
        initializeDefaultUsers();
    }

    /**
     * Get singleton instance
     * @return UserManager instance
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Initialize default users for testing
     */
    private void initializeDefaultUsers() {
        addUser("admin", "admin123", "/", true, true, true);
        addUser("user", "user123", "/public", true, true, false);
        addUser("test", "test", "/", true, false, false);
        addUser("guest", "", "/public", true, false, false);
    }

    /**
     * Add a user to the system
     * @param username Username
     * @param password Password
     * @param homeDirectory Home directory
     * @param canRead Can read files
     * @param canWrite Can write files
     * @param canDelete Can delete files
     */
    public void addUser(String username, String password, String homeDirectory,
                        boolean canRead, boolean canWrite, boolean canDelete) {
        User user = new User(username, password, homeDirectory, canRead, canWrite, canDelete);
        users.put(username, user);
    }

    /**
     * Authenticate user
     * @param username Username
     * @param password Password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    /**
     * Check if user exists
     * @param username Username
     * @return true if user exists
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Get user by username
     * @param username Username
     * @return User object or null if not found
     */
    public User getUser(String username) {
        return users.get(username);
    }

    /**
     * Remove user
     * @param username Username
     * @return true if user was removed
     */
    public boolean removeUser(String username) {
        return users.remove(username) != null;
    }

    /**
     * Get all usernames
     * @return Set of usernames
     */
    public java.util.Set<String> getAllUsernames() {
        return users.keySet();
    }

    /**
     * Print all users (for debugging)
     */
    public void printAllUsers() {
        System.out.println("=== Registered Users ===");
        for (User user : users.values()) {
            System.out.println(user);
        }
        System.out.println("=======================");
    }

    /**
     * Inner class representing a user
     */
    public static class User {
        private final String username;
        private final String password;
        private final String homeDirectory;
        private final boolean canRead;
        private final boolean canWrite;
        private final boolean canDelete;

        public User(String username, String password, String homeDirectory,
                    boolean canRead, boolean canWrite, boolean canDelete) {
            this.username = username;
            this.password = password;
            this.homeDirectory = homeDirectory;
            this.canRead = canRead;
            this.canWrite = canWrite;
            this.canDelete = canDelete;
        }

        public String getUsername() {
            return username;
        }

        public String getHomeDirectory() {
            return homeDirectory;
        }

        public boolean canRead() {
            return canRead;
        }

        public boolean canWrite() {
            return canWrite;
        }

        public boolean canDelete() {
            return canDelete;
        }

        public boolean checkPassword(String password) {
            if (this.password == null || this.password.isEmpty()) {
                return password == null || password.isEmpty();
            }
            return this.password.equals(password);
        }

        @Override
        public String toString() {
            return String.format("User{username='%s', home='%s', read=%s, write=%s, delete=%s}",
                    username, homeDirectory, canRead, canWrite, canDelete);
        }
    }
}