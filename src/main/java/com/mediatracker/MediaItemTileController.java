package com.mediatracker;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import java.io.File;
import java.io.IOException;
import javafx.application.Platform;

@SuppressWarnings("unused")
public class MediaItemTileController {
    private MediaItem mediaItem;
    private String fromView; // Context where the item was opened from (folder name or favorites)
    private MainViewController mainViewController;

    @FXML
    private ImageView coverImageView;
    
    @FXML
    private ImageView defaultCoverImageView;

    @FXML
    private Text titleText;
    
    @FXML
    private Label typeLabel;
    
    @FXML
    private Label statusLabel;

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    // Method for FolderContentController
    /**
     * Sets the media item to display in this tile
     * @param item The media item to display
     */
    public void setItem(MediaItem item) {
        this.mediaItem = item;
        this.fromView = null;  // For items in folder, fromView is null
        if (item != null) {
            updateTile();
        }
    }

    /**
     * Sets the media item to display in this tile with context information
     * @param item The media item to display
     * @param fromView The context where this item is being displayed (e.g., folder name)
     */
    public void setItem(MediaItem item, String fromView) {
        this.mediaItem = item;
        this.fromView = fromView;
        if (item != null) {
            updateTile();
        }
    }

    // Helper method to convert English type to Russian for display
    private String convertTypeToRussian(String englishType) {
        if (englishType == null) return "";
        return switch (englishType) {
            case "Movie" -> "Фильм";
            case "Series" -> "Сериал";
            case "Book" -> "Книга";
            case "Manga" -> "Манга";
            case "Anime" -> "Аниме";
            default -> englishType;
        };
    }

    // Helper method to convert English status to Russian for display
    private String convertStatusToRussian(String englishStatus) {
        if (englishStatus == null) return "";
        return switch (englishStatus) {
            case "Ongoing" -> "В процессе";
            case "Released" -> "Вышло";
            case "Announced" -> "Анонсировано";
            default -> englishStatus;
        };
    }

    private static final String DEFAULT_COVER_PATH = "/com/mediatracker/images/default-cover.png";
    private Image defaultCover;

    @FXML
    private void initialize() {
        try {
            // Load default cover from resources
            defaultCover = new Image(getClass().getResourceAsStream(DEFAULT_COVER_PATH));
            defaultCoverImageView.setImage(defaultCover);
        } catch (Exception e) {
            System.err.println("Failed to load default cover: " + e.getMessage());
        }
    }

    private void updateTile() {
        if (mediaItem == null) {
            coverImageView.setVisible(false);
            defaultCoverImageView.setVisible(true);
            titleText.setText("");
            typeLabel.setText("");
            statusLabel.setText("");
            return;
        }

        // Update text fields
        titleText.setText(mediaItem.getTitle() != null ? mediaItem.getTitle() : "");
        
        // Convert type and status to Russian for display
        typeLabel.setText(convertTypeToRussian(mediaItem.getType()));
        statusLabel.setText(convertStatusToRussian(mediaItem.getStatus()));

        String imagePath = mediaItem.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            loadImageFromFile(imagePath);
        } else {
            showDefaultCover();
        }
    }
    
    private void loadImageFromFile(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists() || !file.isFile()) {
                throw new IOException("Image file not found: " + imagePath);
            }
            
            String url = file.toURI().toURL().toExternalForm();
            Image image = new Image(url, true); // Background loading
            
            // Error listener
            image.errorProperty().addListener((obs, wasError, isNowError) -> {
                if (isNowError) {
                    Platform.runLater(this::showDefaultCover);
                }
            });
            
            // Progress listener
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0 && !image.isError()) {
                    Platform.runLater(() -> {
                        coverImageView.setImage(image);
                        coverImageView.setVisible(true);
                        defaultCoverImageView.setVisible(false);
                    });
                }
            });
            
            // If image is already loaded (cached)
            if (image.getProgress() == 1.0 && !image.isError()) {
                coverImageView.setImage(image);
                coverImageView.setVisible(true);
                defaultCoverImageView.setVisible(false);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading image: " + imagePath + ", Error: " + e.getMessage());
            showDefaultCover();
        }
    }
    
    private void showDefaultCover() {
        coverImageView.setVisible(false);
        defaultCoverImageView.setVisible(true);
    }

    @FXML
    private void handleTileClick(MouseEvent event) {
        if (mainViewController != null && mediaItem != null) {
            mainViewController.showItemDetails(mediaItem, fromView);
        }
    }
}
