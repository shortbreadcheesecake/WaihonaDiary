package com.mediatracker;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;

@SuppressWarnings("unused")
public class RegistrationController {

    @FXML private AnchorPane rootPane;
    @FXML private StackPane stackRoot;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button closeButton;
    @FXML private Canvas gridCanvas;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
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
    private void handleRegisterButtonAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Имя пользователя и пароль не могут быть пустыми.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Пароли не совпадают.");
            return;
        }

        if (DatabaseManager.registerUser(username, password)) {
            // Successful registration
            System.out.println("User registered successfully: " + username);
            closeWindow();
        } else {
            // Registration error (e.g., username already exists)
            errorLabel.setText("Пользователь с таким именем уже существует.");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCloseButtonAction() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
