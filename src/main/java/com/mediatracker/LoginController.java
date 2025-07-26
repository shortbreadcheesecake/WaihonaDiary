package com.mediatracker;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

import java.io.IOException;

@SuppressWarnings("unused")
public class LoginController {

    @FXML private VBox rootPane;
    @FXML private StackPane stackRoot;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button closeButton;
    @FXML private Label errorLabel;
    @FXML private Label emblemLabel;
    @FXML private Canvas gridCanvas;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        try {
            // Load custom font from resources
            Font customFont = Font.loadFont(getClass().getResourceAsStream("/fonts/DoDavidGothicRegular-7BEz4.ttf"), 36);
            if (customFont != null) {
                emblemLabel.setFont(customFont);
                emblemLabel.setTextFill(Color.rgb(138, 43, 226)); // Purple color #8A2BE2
                System.out.println("Custom font loaded successfully: " + customFont.getName());
            } else {
                System.err.println("Failed to load custom font. Using default font.");
            }
            
            // Set initial window size after the UI is shown
            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) rootPane.getScene().getWindow();
                    setLoginWindowSize(stage);
                } catch (Exception e) {
                    System.err.println("Error setting window size: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error loading custom font: " + e.getMessage());
            e.printStackTrace();
        }
        // Make the window draggable
        rootPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        // Grid background
        gridCanvas.widthProperty().bind(stackRoot.widthProperty());
        gridCanvas.heightProperty().bind(stackRoot.heightProperty());
        gridCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGrid());
        gridCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGrid());
        drawGrid();
    }

    private void drawGrid() {
        if (gridCanvas == null) return;
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.web("#8A2BE2"));
        gc.setLineWidth(0.5);
        int numVertical = 50;
        int numHorizontal = 50;
        double vStep = width / numVertical;
        double hStep = height / numHorizontal;
        for (int i = 1; i < numVertical; i++) {
            double x = i * vStep;
            gc.strokeLine(x, 0, x, height);
        }
        for (int i = 1; i < numHorizontal; i++) {
            double y = i * hStep;
            gc.strokeLine(0, y, width, y);
        }
    }

    @FXML
    private void handleLoginButtonAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Пожалуйста, введите email и пароль.");
            return;
        }

        if (DatabaseManager.validateUser(username, password)) {
            int userId = DatabaseManager.getUserIdByUsername(username);
            if (userId != -1) {
                SessionManager.getInstance().setCurrentUserId(userId);
                AppPreferences.saveLastUserId(userId); // Save user ID
                System.out.println("Login successful for user_id: " + userId);
                openMainWindow();
            } else {
                System.out.println("Login failed: Could not retrieve user ID.");
            }
        } else {
            errorLabel.setText("Неверный email или пароль.");
            System.out.println("Login failed: Invalid credentials.");
        }
    }

    @FXML
    private void handleRegisterLinkAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("registration-view.fxml"));
            Parent root = loader.load();
            Stage registrationStage = new Stage();
            registrationStage.setTitle("Registration");
            registrationStage.initStyle(StageStyle.UNDECORATED); // Make undecorated
            registrationStage.setScene(new Scene(root));
            registrationStage.initModality(Modality.APPLICATION_MODAL);
            registrationStage.initOwner((Stage) loginButton.getScene().getWindow());
            setLoginWindowSize(registrationStage);
            registrationStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setLoginWindowSize(Stage stage) {
        stage.setWidth(350);
        stage.setHeight(450);
        stage.centerOnScreen();
    }

    private void openMainWindow() {
        try {
            // Get the current window (stage)
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Load the main window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Parent root = loader.load();

            // Set the new scene in the existing window
            stage.setScene(new Scene(root));
            stage.setTitle("Media Tracker");
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Ошибка при загрузке главного окна.");
        }
    }

    @FXML
    private void handleCloseButtonAction() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
