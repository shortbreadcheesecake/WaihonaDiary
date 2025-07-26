package com.mediatracker;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Метод для выравнивания кнопок по центру
 */
@SuppressWarnings("unused")
public class MainViewController {

    @FXML private Button minimizeButton;

    @FXML private Button closeButton;
    @FXML private Button deleteFolderButton;
    @FXML private HBox topBar;
    @FXML         private ListView<String> folderListView;


    @FXML private StackPane centerStackPane;
    @FXML private VBox mainContent;
    @FXML private VBox mainActionButtons;
    @FXML private Canvas gridCanvas;

            private ObservableList<String> folders;
    private Node folderContentView;
    private FolderContentController folderContentController;
    private Node addItemView;
    private AddItemViewController addItemViewController;
    private Node itemDetailsView;
    private ItemDetailsController itemDetailsController;
    private Node favoritesView;
    private FavoritesViewController favoritesViewController;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Настраиваем базовые элементы интерфейса
        setupWindowDragAndControls();

        // Инициализируем список папок
        folders = FXCollections.observableArrayList();
        folderListView.setItems(folders);

        // Настраиваем видимость кнопок
        mainActionButtons.setVisible(true);
        mainActionButtons.setManaged(true);

        // Принудительно устанавливаем выравнивание по центру
        StackPane.setAlignment(mainActionButtons, Pos.CENTER);

        // Настраиваем слушатель выбора папки
        folderListView.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
            updateUiState();
        });

        // Загружаем папки
        loadFolders();

        // Настраиваем привязку размеров канвы
        gridCanvas.widthProperty().bind(centerStackPane.widthProperty());
        gridCanvas.heightProperty().bind(centerStackPane.heightProperty());

        // Рисуем сетку
        drawGrid();

        // Обновляем сетку при изменении размеров
        gridCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGrid());
        gridCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGrid());

        // Устанавливаем начальные и минимальные размеры окна
        Platform.runLater(() -> {
            if (mainActionButtons.getScene() != null) {
                Stage stage = (Stage) mainActionButtons.getScene().getWindow();
                
                // Минимальные размеры
                stage.setMinWidth(1000);
                stage.setMinHeight(700);
                
                // Начальные размеры (если еще не установлены)
                if (stage.getWidth() < 1000) stage.setWidth(1000);
                if (stage.getHeight() < 700) stage.setHeight(700);
                
                // Центрируем окно на экране
                stage.centerOnScreen();
            }
            
            // Загружаем папки
            loadFolders();
            
            // Выравниваем кнопки по центру
            alignButtons();
            
            // Обновляем макет
            centerStackPane.requestLayout();
            mainActionButtons.requestLayout();
            
            // Дополнительное обновление после короткой задержки
            PauseTransition pause = new PauseTransition(Duration.millis(50));
            pause.setOnFinished(evt -> {
                alignButtons();
                centerStackPane.requestLayout();
                mainActionButtons.requestLayout();
            });
            pause.play();
        });
    }

    /**
     * Метод для выравнивания кнопок по центру
     * Теперь не влияет на размеры окна
     */
    private void alignButtons() {
        if (mainActionButtons == null || centerStackPane == null) {
            return;
        }
        
        // Устанавливаем выравнивание
        Platform.runLater(() -> {
            // Сохраняем текущие размеры окна
            double currentWidth = 0;
            double currentHeight = 0;
            
            if (mainActionButtons.getScene() != null && mainActionButtons.getScene().getWindow() != null) {
                currentWidth = mainActionButtons.getScene().getWindow().getWidth();
                currentHeight = mainActionButtons.getScene().getWindow().getHeight();
            }
            
            // Устанавливаем выравнивание
            centerStackPane.setAlignment(Pos.CENTER);
            StackPane.setAlignment(mainActionButtons, Pos.CENTER);
            
            // Принудительно обновляем стили и макет
            mainActionButtons.applyCss();
            mainActionButtons.layout();
            centerStackPane.requestLayout();
            
            // Восстанавливаем размеры окна, если они изменились
            if (mainActionButtons.getScene() != null && mainActionButtons.getScene().getWindow() != null) {
                mainActionButtons.getScene().getWindow().setWidth(currentWidth);
                mainActionButtons.getScene().getWindow().setHeight(currentHeight);
            }
        });
    }

    @FXML
    private void loadFolders() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId == -1) {
            System.err.println("No user logged in, cannot load folders.");
            return;
        }
        folders.clear();
        List<String> dbFolders = DatabaseManager.getFolders(userId);
        folders.addAll(dbFolders);
        updateUiState(); // Update button state after loading folders
    }

    private void setActiveView(Node activeView) {
        // Deactivate all views
        if (folderContentView != null) {
            folderContentView.setVisible(false);
            folderContentView.setManaged(false);
        }
        if (addItemView != null) {
            addItemView.setVisible(false);
            addItemView.setManaged(false);
        }
        if (itemDetailsView != null) {
            itemDetailsView.setVisible(false);
            itemDetailsView.setManaged(false);
        }
        if (favoritesView != null) {
            favoritesView.setVisible(false);
            favoritesView.setManaged(false);
        }

        // Show grid only on main menu
        boolean showGrid = (activeView == null);
        gridCanvas.setVisible(showGrid);
        gridCanvas.setManaged(showGrid);

        // Show/hide mainActionButtons and mainContent mutually exclusive
        mainActionButtons.setVisible(activeView == null);
        mainActionButtons.setManaged(activeView == null);
        mainContent.setVisible(activeView != null);
        mainContent.setManaged(activeView != null);

        if (activeView != null) {
            activeView.setVisible(true);
            activeView.setManaged(true);
            if (!mainContent.getChildren().contains(activeView)) {
                mainContent.getChildren().add(activeView);
            }
            mainContent.setAlignment(Pos.TOP_CENTER);
            activeView.toFront();
            // Fade in
            FadeTransition ft = new FadeTransition(Duration.millis(300), activeView);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } else {
            // Fade in mainActionButtons
            FadeTransition ft = new FadeTransition(Duration.millis(300), mainActionButtons);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    private void updateUiState() {
        String selectedFolder = folderListView.getSelectionModel().getSelectedItem();
        boolean isFolderSelected = selectedFolder != null;

        // The delete button should be disabled only if there are no folders.
        deleteFolderButton.setDisable(folders.isEmpty());

        if (isFolderSelected) {
            showFolderContent(selectedFolder);
        } else {
            // If no folder is selected, show the main action buttons and hide other views.
            setActiveView(null);
        }
    }



    /**
     * Показывает представление избранного
     * @param forceRefresh если true, принудительно обновляет список избранного
     */
    public void showFavoritesView(boolean forceRefresh) {
        try {
            // Clear folder selection so it can be clicked again
            folderListView.getSelectionModel().clearSelection();
            
            if (favoritesView == null || forceRefresh) {
                // If forcing refresh, remove the old view first
                if (favoritesView != null) {
                    centerStackPane.getChildren().remove(favoritesView);
                    favoritesView = null;
                }
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("favorites-view.fxml"));
                favoritesView = loader.load();
                favoritesViewController = loader.getController();
                favoritesViewController.setMainViewController(this);
                centerStackPane.getChildren().add(favoritesView);
            }
            
            // Show the view first
            setActiveView(favoritesView);
            
            // Update data with a small delay to ensure UI is ready
            Platform.runLater(() -> {
                if (favoritesViewController != null) {
                    // Always update the title first
                    favoritesViewController.updateFavoritesTitle();
                    
                    // Then load or refresh the content
                    if (forceRefresh) {
                        favoritesViewController.refreshFavorites();
                    } else {
                        favoritesViewController.loadFavorites();
                    }
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            // Show error to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось загрузить избранное");
            alert.setContentText("Попробуйте обновить страницу или перезапустить приложение.");
            alert.showAndWait();
        }
    }
    
    /**
     * Перегруженный метод для обратной совместимости
     */
    public void showFavoritesView() {
        showFavoritesView(false);
    }

    public void returnToMain() {
        folderListView.getSelectionModel().clearSelection();
        // setActiveView(null) correctly handles showing the main action buttons and hiding other views.
        setActiveView(null);
    }

            public void returnToFolderView(String folderName) {
        folderListView.getSelectionModel().select(folderName);
    }

    public void showFolderContent(String folderName) {
        try {
            if (folderContentView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("folder-content-view.fxml"));
                folderContentView = loader.load();
                folderContentController = loader.getController();
                folderContentController.setMainController(this);
                folderContentController.setUserId(SessionManager.getInstance().getCurrentUserId());
                folderContentController.setFolderName(folderName);
                centerStackPane.getChildren().add(folderContentView);
                StackPane.setAlignment(folderContentView, Pos.TOP_CENTER);
            } else {
                folderContentController.setFolderName(folderName);
            }


            setActiveView(folderContentView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAddItemView(String folderName) {
        try {
            if (addItemView == null) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("add-item-view.fxml"));
                addItemView = fxmlLoader.load();
                addItemViewController = fxmlLoader.getController(); // Assign to the class field
                addItemViewController.setMainViewController(this);
                addItemViewController.setUserId(SessionManager.getInstance().getCurrentUserId());
                centerStackPane.getChildren().add(addItemView);
            }

            // Set data every time
            addItemViewController.setFolderData(folderName);

            setActiveView(addItemView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showItemDetails(MediaItem item, String fromView) {
        try {
            if (itemDetailsView == null) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("item-details-view.fxml"));
                itemDetailsView = fxmlLoader.load();
                itemDetailsController = fxmlLoader.getController();
                itemDetailsController.setMainViewController(this);
                centerStackPane.getChildren().add(itemDetailsView);
            }

            itemDetailsController.setItem(item, fromView);

            setActiveView(itemDetailsView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showEditItemView(MediaItem item, String fromView) {
        try {
            // Ensure the add/edit view is loaded, same as showAddItemView
            if (addItemView == null) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("add-item-view.fxml"));
                addItemView = fxmlLoader.load();
                addItemViewController = fxmlLoader.getController();
                addItemViewController.setMainViewController(this);
                addItemViewController.setUserId(SessionManager.getInstance().getCurrentUserId());
                centerStackPane.getChildren().add(addItemView);
            }

            // Determine the folder name if coming from favorites
            String folderName = fromView;
            if ("Favorites".equals(fromView) && item.getFolderId() > 0) {
                folderName = DatabaseManager.getFolderNameById(item.getFolderId());
            }

            // Set the context and the item to edit
            addItemViewController.setFolderData(folderName); // Sets folder context and resets form
            addItemViewController.setItemToEdit(item, fromView); // Fills form with item data

            // Hide other views
            if (folderContentView != null) {
                folderContentView.setVisible(false);
                folderContentView.setManaged(false);
            }
            if (itemDetailsView != null) {
                itemDetailsView.setVisible(false);
                itemDetailsView.setManaged(false);
            }
            if (favoritesView != null) {
                favoritesView.setVisible(false);
                favoritesView.setManaged(false);
            }

            // Show the add/edit view
            setActiveView(addItemView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupWindowDragAndControls() {
                        topBar.setOnMousePressed(mouseEvent -> {
            xOffset = mouseEvent.getSceneX();
            yOffset = mouseEvent.getSceneY();
        });

                        topBar.setOnMouseDragged(mouseEvent -> {
            Stage stage = (Stage) topBar.getScene().getWindow();
            stage.setX(mouseEvent.getScreenX() - xOffset);
            stage.setY(mouseEvent.getScreenY() - yOffset);
        });

                minimizeButton.setOnAction(_ -> handleMinimizeButtonAction());

                closeButton.setOnAction(_ -> handleCloseButtonAction());
    }

    @FXML
    private void handleMinimizeButtonAction() {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }



    @FXML
    private void handleCloseButtonAction() {
        SessionManager.getInstance().clearSession();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleAddFolder() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId == -1) {
            CustomDialog.showInformationDialog(centerStackPane, "Ошибка", "Необходимо войти в систему для создания папки.");
            return;
        }

        CustomDialog.showInputDialog(
            centerStackPane,
            "Создание новой папки",
            "Введите имя папки",
            folderName -> {
                if (folderName == null || folderName.trim().isEmpty()) {
                    return;
                }

                // Check if adding a new folder will cause the scrollbar to appear
                folderListView.getItems().add(folderName); // Temporarily add
                folderListView.layout(); // Force layout update

                ScrollBar horizontalScrollBar = (ScrollBar) folderListView.lookup(".scroll-bar:horizontal");
                boolean scrollBarVisible = horizontalScrollBar != null && horizontalScrollBar.isVisible();

                folderListView.getItems().remove(folderName); // Immediately remove

                if (scrollBarVisible) {
                    CustomDialog.showInformationDialog(centerStackPane, "Достигнут лимит папок",
                        "Вы достигли лимита использования места для папок.\n\n" +
                        "Для создания новых папок:\n" +
                        "• Удалите некоторые существующие папки\n" +
                        "• Или создайте новый аккаунт");
                    return;
                }

                // If everything is fine, create the folder
                if (DatabaseManager.createNewFolder(folderName, userId)) {
                    loadFolders();
                } else {
                    CustomDialog.showInformationDialog(centerStackPane, "Ошибка", "Папка с таким именем уже существует.");
                }
            }
        );
    }

    @FXML
    private void showFavorites() {
        showFavoritesView();
    }

    @FXML
    private void handleLoadDatapack() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId == -1) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Load datapack");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ZIP Archives", "*.zip"));

        java.io.File file = fileChooser.showOpenDialog(centerStackPane.getScene().getWindow());

        if (file != null) {
            DatapackService datapackService = new DatapackService();
            try {
                datapackService.loadDatapack(userId, file);
                loadFolders(); // Refresh folder list
                CustomDialog.showInformationDialog(centerStackPane, "Success", "Datapack loaded successfully.");
            } catch (java.io.IOException e) {
                e.printStackTrace();
                CustomDialog.showInformationDialog(centerStackPane, "Error", "Failed to load datapack.");
            }
        }
    }

    @FXML
    private void handleCreateDatapack() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId == -1) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save datapack");
        fileChooser.setInitialFileName("datapack_waihona.zip");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ZIP Archives", "*.zip"));

        java.io.File file = fileChooser.showSaveDialog(centerStackPane.getScene().getWindow());

        if (file != null) {
            DatapackService datapackService = new DatapackService();
            try {
                datapackService.createDatapack(userId, file);
                CustomDialog.showInformationDialog(centerStackPane, "Success", "Datapack created successfully.");
            } catch (java.io.IOException e) {
                e.printStackTrace();
                CustomDialog.showInformationDialog(centerStackPane, "Error", "Failed to create datapack.");
            }
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        AppPreferences.clearLastUserId(); // Clear user ID on logout

        try {
            // Load the login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent loginRoot = loader.load();

            // Create a new stage for login
            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.setResizable(false);
            
            // Set the scene with explicit size
            Scene scene = new Scene(loginRoot, 500, 625);
            loginStage.setScene(scene);
            loginStage.setTitle("Login");
            loginStage.centerOnScreen();
            
            // Close the current window
            Stage currentStage = (Stage) centerStackPane.getScene().getWindow();
            currentStage.close();
            
            // Show the login window
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void handleDeleteFolder() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId == -1) return;

        List<String> userFolders = DatabaseManager.getFolders(userId);
        if (userFolders.isEmpty()) {
            CustomDialog.showInformationDialog(centerStackPane, "Нет папок", "У вас еще нет папок для удаления.");
            return;
        }

        CustomDialog.showChoiceDialog(
            centerStackPane,
            "Удаление папки",
            "Выберите папку, которую хотите удалить:",
            userFolders,
            folderName -> {
                if (folderName != null) {
                    CustomDialog.showConfirmationDialog(
                        centerStackPane,
                        "Удалить папку '" + folderName + "'?",
                        "Все медиа-элементы в этой папке также будут удалены. Это действие необратимо.",
                        () -> {
                            DatabaseManager.deleteFolder(folderName, userId);
                            loadFolders();
                        }
                    );
                }
            }
        );
    }

    private void drawGrid() {
        if (gridCanvas == null) return;
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.web("#8A2BE2"));
        gc.setLineWidth(2);
        int numVertical = 20; // 20 columns
        int numHorizontal = 20; // 20 rows
        double vStep = width / numVertical;
        double hStep = height / numHorizontal;
        // Draw vertical lines
        for (int i = 1; i < numVertical; i++) {
            double x = i * vStep;
            gc.strokeLine(x, 0, x, height);
        }
        // Draw horizontal lines
        for (int i = 1; i < numHorizontal; i++) {
            double y = i * hStep;
            gc.strokeLine(0, y, width, y);
        }
    }
    
    /**
     * Returns the center stack pane used for displaying dialogs
     * @return The center StackPane
     */
    public StackPane getCenterStackPane() {
        return centerStackPane;
    }
    
    /**
     * Updates the favorites title in the UI
     */
    public void updateFavoritesTitle() {
        if (favoritesViewController != null) {
            favoritesViewController.updateFavoritesTitle();
        }
    }
}
