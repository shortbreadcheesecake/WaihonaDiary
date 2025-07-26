package com.mediatracker;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class FavoritesViewController {

    @FXML private Label titleLabel;
    @FXML private VBox contentBox;
    @FXML private TilePane tilePane;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    
    private HBox headerBox; // We will receive it programmatically

    private MainViewController mainViewController;
    private int userId;
    private int currentPage = 0;
    private int totalPages = 1;
    private boolean isLoading = false; // Флаг для предотвращения одновременной загрузки
    private static final int ITEMS_PER_PAGE = 10; // 5 columns x 2 rows

    @FXML
    public void initialize() {
        // Get a link to the HBox of the header
        headerBox = (HBox) titleLabel.getParent();
    }

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public void loadFavorites() {
        if (isLoading) {
            return; // Prevent simultaneous downloads
        }
        this.userId = SessionManager.getInstance().getCurrentUserId();
        this.currentPage = 0;
        loadPage();
    }

    private void loadPage() {
        if (isLoading) {
            return; // Prevent simultaneous downloads
        }
        isLoading = true;
        
        // Clear Current Items
        tilePane.getChildren().clear();
        // Remove possible empty list message
        contentBox.getChildren().removeIf(node -> node != tilePane);

        Task<Integer> countTask = new Task<>() {
            @Override
            protected Integer call() {
                return DatabaseManager.getFavoriteMediaItemCount(userId);
            }
        };

        countTask.setOnSucceeded(__ -> {
            int totalItems = countTask.getValue();
            totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE) : 1;

            if (totalItems == 0) {
                tilePane.setVisible(false);
                tilePane.setManaged(false);
                headerBox.setAlignment(Pos.CENTER);
                prevButton.setVisible(false);
                nextButton.setVisible(false);
                pageLabel.setVisible(false);
                titleLabel.setText("Пока здесь ничего нет");
                isLoading = false; // Loading completed
            } else {
                tilePane.setVisible(true);
                tilePane.setManaged(true);
                headerBox.setAlignment(Pos.CENTER_LEFT);
                prevButton.setVisible(true);
                nextButton.setVisible(true);
                pageLabel.setVisible(true);
                titleLabel.setText("Избранное (" + totalItems + ")");
                loadItemsForCurrentPage(); // This task will set isLoading = false
            }

            updatePaginationControls();
        });

        countTask.setOnFailed(__ -> {
            countTask.getException().printStackTrace();
            isLoading = false; // Reset flag on error
        });

        new Thread(countTask).start();
    }

    private void loadItemsForCurrentPage() {
        Task<List<MediaItem>> loadItemsTask = new Task<>() {
            @Override
            protected List<MediaItem> call() {
                return DatabaseManager.getFavoriteMediaItems(userId, currentPage, ITEMS_PER_PAGE);
            }
        };

        loadItemsTask.setOnSucceeded(__ -> {
            List<MediaItem> items = loadItemsTask.getValue();
            for (MediaItem item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("media-item-tile.fxml"));
                    VBox tile = loader.load();
                    MediaItemTileController controller = loader.getController();
                    controller.setMainViewController(mainViewController);
                    controller.setItem(item, "Избранное");
                    tilePane.getChildren().add(tile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            isLoading = false; // Loading completed
        });

        loadItemsTask.setOnFailed(__ -> {
            loadItemsTask.getException().printStackTrace();
            isLoading = false; // Reset flag on error
        });
        new Thread(loadItemsTask).start();
    }

    private void updatePaginationControls() {
        pageLabel.setText((currentPage + 1) + " / " + totalPages);
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadPage();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadPage();
        }
    }

    @FXML
    private void handleBack() {
        if (mainViewController != null) {
            mainViewController.returnToMain();
        }
    }
    
    /**
     * Обновляет список избранных элементов с принудительным сбросом состояния
     */
    public void refreshFavorites() {
        // Сбрасываем состояние перед загрузкой
        this.currentPage = 0;
        this.totalPages = 1;
        this.isLoading = false;
        
        // Очищаем текущие элементы
        Platform.runLater(() -> {
            tilePane.getChildren().clear();
            contentBox.getChildren().removeIf(node -> node != tilePane && node != titleLabel && node != headerBox);
            
            // Загружаем обновленные данные
            loadFavorites();
        });
    }
    
    /**
     * Обновляет заголовок избранного с текущим количеством элементов
     */
    public void updateFavoritesTitle() {
        Task<Integer> countTask = new Task<>() {
            @Override
            protected Integer call() {
                return DatabaseManager.getFavoriteMediaItemCount(userId);
            }
        };

        countTask.setOnSucceeded(__ -> {
            int totalItems = countTask.getValue();
            Platform.runLater(() -> {
                if (totalItems == 0) {
                    titleLabel.setText("Пока здесь ничего нет");
                } else {
                    titleLabel.setText("Избранное (" + totalItems + ")");
                }
            });
        });

        countTask.setOnFailed(__ -> {
            countTask.getException().printStackTrace();
        });

        new Thread(countTask).start();
    }
}
