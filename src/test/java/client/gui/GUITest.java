package client.gui;

import client.gui.models.ConnectionInfo;
import client.gui.models.FileItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Test class for GUI components
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class GUITest {

    @Test
    void testConnectionInfo() {
        ConnectionInfo connInfo = new ConnectionInfo();

        // Test default values
        assert "localhost".equals(connInfo.getHostname());
        assert connInfo.getPort() == 21;
        assert !connInfo.isConnected();
        assert !connInfo.isAuthenticated();

        // Test setters
        connInfo.setHostname("test.server.com");
        connInfo.setPort(2121);
        connInfo.setUsername("testuser");
        connInfo.setPassword("testpass");

        assert "test.server.com".equals(connInfo.getHostname());
        assert connInfo.getPort() == 2121;
        assert "testuser".equals(connInfo.getUsername());

        // Test validation
        assert connInfo.isValid();

        // Test connection status
        connInfo.setConnected(true);
        connInfo.setAuthenticated(true);
        assert connInfo.isConnected();
        assert connInfo.isAuthenticated();

        String connectionString = connInfo.getConnectionString();
        assert connectionString.contains("testuser@test.server.com:2121");

        System.out.println("ConnectionInfo test passed");
    }

    @Test
    void testFileItem() {
        // Test file item
        FileItem fileItem = new FileItem("test.txt", 1024, LocalDateTime.now(), false, "rw-r--r--");

        assert "test.txt".equals(fileItem.getName());
        assert fileItem.getSize() == 1024;
        assert !fileItem.isDirectory();
        assert !fileItem.isParentDirectory();

        // Test directory item
        FileItem dirItem = new FileItem("testdir", 0, LocalDateTime.now(), true, "drwxr-xr-x");

        assert "testdir".equals(dirItem.getName());
        assert dirItem.isDirectory();
        assert !dirItem.isParentDirectory();
        assert "<DIR>".equals(dirItem.getFormattedSize());

        // Test parent directory
        FileItem parentItem = FileItem.createParentDirectory();
        assert "..".equals(parentItem.getName());
        assert parentItem.isDirectory();
        assert parentItem.isParentDirectory();

        System.out.println("FileItem test passed");
    }

    /**
     * Manual test method to launch GUI
     * This is not a JUnit test but can be run manually
     */
    public static void launchGUI() {
        System.out.println("Launching FTP Client GUI...");
        FTPClientGUI.main(new String[0]);
    }
}