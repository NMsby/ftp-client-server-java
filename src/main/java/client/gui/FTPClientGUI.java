package client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main JavaFX Application for FTP Client GUI
 *
 * @author Nelson Masbayi
 * @version 1.0
 */
public class FTPClientGUI extends Application {
    private static final Logger logger = LogManager.getLogger("client");

    private static final String TITLE = "FTP Client - University Project";
    private static final int MIN_WIDTH = 1000;
    private static final int MIN_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting FTP Client GUI");

            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();

            // Configure scene
            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Configure stage
            primaryStage.setTitle(TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);

            // Set application icon
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/ftp-icon.png")));
            } catch (Exception e) {
                logger.debug("Could not load application icon: {}", e.getMessage());
            }

            // Handle window close
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application closing");
                Platform.exit();
                System.exit(0);
            });

            // Show window
            primaryStage.show();
            logger.info("FTP Client GUI started successfully");

        } catch (Exception e) {
            logger.error("Failed to start FTP Client GUI", e);
            showErrorAndExit("Failed to start application: " + e.getMessage());
        }
    }

    /**
     * Show error message and exit application
     * @param message Error message
     */
    private void showErrorAndExit(String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start FTP Client");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Critical error: " + message);
        }
        Platform.exit();
        System.exit(1);
    }

    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Launching FTP Client GUI application");
        launch(args);
    }
}