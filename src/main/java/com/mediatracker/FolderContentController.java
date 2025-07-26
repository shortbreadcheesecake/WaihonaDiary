package com.mediatracker;



import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import javafx.concurrent.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

public class FolderContentController {
    private int userId;

    @FXML
    private Label folderNameLabel;
    @FXML
    private TilePane tilePane;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label pageLabel;


    private String currentFolderName;
    private int currentPageIndex = 0;
    private int totalPages = 1;
    private int folderId;
    private MainViewController mainViewController;
    private static final int ITEMS_PER_PAGE = 10; // 5 columns * 2 rows

    @FXML
    public void initialize() {

        

    }

    public void setMainController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setFolderName(String name) {
        this.currentFolderName = name;
        // Сначала обновляем заголовок, чтобы избежать мигания "null"
        folderNameLabel.setText(String.format("%s (загрузка...)", name));
        
        // Запускаем загрузку данных в фоновом потоке
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int itemCount = DatabaseManager.getMediaItemCountInFolder(userId, name);
                
                Platform.runLater(() -> {
                    folderNameLabel.setText(String.format("%s (%d)", name, itemCount));
                });
                
                folderId = DatabaseManager.getFolderId(name, userId);
                if (folderId == -1) {
                    Platform.runLater(() -> {
                        tilePane.setVisible(false);
                        currentPageIndex = 0;
                        totalPages = 1;
                        updatePaginationButtons();
                    });
                    return null;
                }
                
                int totalItems = (int) DatabaseManager.getItemCountForFolder(folderId, userId);
                totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
                if (totalPages == 0) totalPages = 1;
                
                currentPageIndex = 0;
                
                Platform.runLater(() -> {
                    updatePaginationButtons();
                    tilePane.setVisible(totalItems > 0);
                    createPage(0);
                });
                
                return null;
            }
        };
        
        new Thread(loadTask).start();
    }

    private void createPage(int pageIndex) {
        if (folderId == -1) return;
        
        // Показываем индикатор загрузки
        tilePane.getChildren().clear();
        Label loadingLabel = new Label("Загрузка...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        tilePane.getChildren().add(loadingLabel);

        Task<List<Node>> task = new Task<>() {
            @Override
            protected List<Node> call() throws Exception {
                List<MediaItem> items = DatabaseManager.getItemsForFolder(folderId, userId, pageIndex, ITEMS_PER_PAGE);
                List<Node> tiles = new ArrayList<>();
                
                for (MediaItem item : items) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("media-item-tile.fxml"));
                        VBox tileNode = loader.load();
                        MediaItemTileController controller = loader.getController();
                        controller.setMainViewController(mainViewController);
                        // Pass the current folder name as the fromView context
                        controller.setItem(item, currentFolderName);
                        tiles.add(tileNode);
                    } catch (IOException e) {
                        System.err.println("Error loading media item tile: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                return tiles;
            }
        };

        // Используем _ для неиспользуемого параметра события
        task.setOnSucceeded(_ -> {
            List<Node> tiles = task.getValue();
            if (tiles != null && !tiles.isEmpty()) {
                tilePane.getChildren().setAll(tiles);
            } else {
                Label noItemsLabel = new Label("В этой папке пока нет элементов");
                noItemsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                tilePane.getChildren().setAll(noItemsLabel);
            }
        });

        // Используем _ для неиспользуемого параметра события
        task.setOnFailed(_ -> {
            Throwable exception = task.getException();
            System.err.println("Error loading page " + pageIndex + ": " + exception.getMessage());
            exception.printStackTrace();
            
            Label errorLabel = new Label("Ошибка при загрузке элементов");
            errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
            tilePane.getChildren().setAll(errorLabel);
        });

        // Запускаем задачу в отдельном потоке
        new Thread(task).start();
    }

    @FXML
    public void handleAddItem() {
        mainViewController.showAddItemView(currentFolderName);
    }

    @FXML
    public void handlePrevPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            createPage(currentPageIndex);
            updatePaginationButtons();
        }
    }

    @FXML
    public void handleNextPage() {
        if (currentPageIndex < totalPages - 1) {
            currentPageIndex++;
            createPage(currentPageIndex);
            updatePaginationButtons();
        }
    }

    private void updatePaginationButtons() {
        // Update page label
        pageLabel.setText((currentPageIndex + 1) + " / " + totalPages);
        
        // Update button states
        prevButton.setDisable(currentPageIndex == 0);
        nextButton.setDisable(currentPageIndex >= totalPages - 1);
    }

    @FXML
    private void handleBack() {
        if (mainViewController != null) {
            mainViewController.returnToMain();
        }
    }
}
