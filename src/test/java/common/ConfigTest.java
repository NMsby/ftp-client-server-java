package common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for configuration management
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class ConfigTest {

    @Test
    public void testConfigurationLoading() {
        FTPConfig config = FTPConfig.getInstance();
        assertNotNull(config);

        // Test default values
        assertEquals(21, config.getServerPort());
        assertTrue(config.getMaxConnections() > 0);
        assertNotNull(config.getServerRootDirectory());
        assertNotNull(config.getClientRootDirectory());
    }

    @Test
    public void testFTPCommandParsing() {
        // Test valid commands
        assertEquals(FTPCommand.USER, FTPCommand.fromString("USER"));
        assertEquals(FTPCommand.LIST, FTPCommand.fromString("list"));
        assertEquals(FTPCommand.QUIT, FTPCommand.fromString("QuIt"));

        // Test invalid commands
        assertEquals(FTPCommand.UNKNOWN, FTPCommand.fromString("INVALID"));
        assertEquals(FTPCommand.UNKNOWN, FTPCommand.fromString(""));
        assertEquals(FTPCommand.UNKNOWN, FTPCommand.fromString(null));
    }

    @Test
    public void testFTPMessage() {
        // Test command creation
        FTPMessage userMsg = new FTPMessage(FTPCommand.USER, "testuser");
        assertEquals(FTPCommand.USER, userMsg.getCommand());
        assertEquals(1, userMsg.getParameterCount());
        assertEquals("testuser", userMsg.getFirstParameter());

        // Test message parsing
        FTPMessage parsedMsg = new FTPMessage("LIST /home");
        assertEquals(FTPCommand.LIST, parsedMsg.getCommand());
        assertEquals("/home", parsedMsg.getFirstParameter());
    }

    @Test
    public void testFTPResponse() {
        FTPResponse response = FTPResponse.USER_LOGGED_IN;
        assertTrue(response.isSuccess());
        assertFalse(response.isError());
        assertEquals(230, response.getCode());

        String formatted = response.getFormattedResponse("Welcome!");
        assertTrue(formatted.contains("230"));
        assertTrue(formatted.contains("Welcome!"));
    }
}