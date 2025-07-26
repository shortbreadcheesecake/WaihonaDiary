package com.mediatracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.Image;




import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        DatabaseManager.initializeDatabase(); // Initialize the database

        // Create data and images directory if they don't exist
        try {
            Path dataDir = Paths.get("data");
            Path imagesDir = Paths.get("data", "images");
            if (!Files.exists(dataDir)) {
                Files.createDirectory(dataDir);
            }
            if (!Files.exists(imagesDir)) {
                Files.createDirectory(imagesDir);
            }
        } catch (IOException e) {
            System.err.println("Could not create data/images directory: " + e.getMessage());
            // Optionally, show an alert to the user and exit
        }

        // Set application icon
        java.io.InputStream iconStream = Main.class.getResourceAsStream("/com/mediatracker/assets/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        } else {
            System.out.println("Icon not found, using default.");
        }

        int lastUserId = AppPreferences.getLastUserId();

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false); // For custom window control buttons
        
        if (lastUserId != -1 && DatabaseManager.userExists(lastUserId)) {
            // If the user exists, open the main screen
            SessionManager.getInstance().setCurrentUserId(lastUserId);
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setTitle("Media Tracker");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } else {
            // If the user does not exist, clear settings and open the login screen
            if (lastUserId != -1) {
                AppPreferences.clearLastUserId();
            }
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 500, 625);
            primaryStage.setTitle("Login");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
