package com.mediatracker;



import javafx.fxml.FXML;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class ItemDetailsController {

    @FXML private VBox rootPane;
    @FXML private Label titleLabel;
    @FXML private ImageView imageView;
    @FXML private Text typeValueText, statusValueText, ratingValueText, releaseDateValueText, genreValueText, durationValueText, episodesValueText, chaptersValueText, descriptionText;
    @FXML private TextFlow descriptionFlow;
    @FXML private Button backButton;
    @FXML private Button editButton;
    @FXML private FontAwesomeIconView favoriteIcon;

    private MainViewController mainViewController;
    private MediaItem currentItem;
    private String fromView; // Can be folder name or "Избранное"

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public void setItem(MediaItem item, String fromView) {
        this.currentItem = item;
        this.fromView = fromView;
        displayItemDetails();
        updateFavoriteIcon();
    }

    // Helper method to convert English type to Russian for display
    private String convertTypeToRussian(String englishType) {
        if (englishType == null) return null;
        return switch (englishType) {
            case "Movie" -> "Фильм";
            case "Series" -> "Сериал";
            case "Book" -> "Книга";
            case "Manga" -> "Манга";
            case "Anime" -> "Аниме";
            default -> englishType; // Return as is if not in our mapping
        };
    }

    // Helper method to convert English status to Russian for display
    private String convertStatusToRussian(String englishStatus) {
        if (englishStatus == null) return null;
        return switch (englishStatus) {
            case "Ongoing" -> "В процессе";
            case "Released" -> "Вышло";
            case "Announced" -> "Анонсировано";
            default -> englishStatus; // Return as is if not in our mapping
        };
    }

    private void displayItemDetails() {
        if (currentItem == null) return;

        titleLabel.setText(currentItem.getTitle());

        String imagePath = currentItem.getImagePath();
        Image imageToSet = null;

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Image image = new Image(imagePath, true);
                if (!image.isError()) {
                    imageToSet = image;
                } else {
                    System.err.println("Error loading image, falling back to default: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("Exception loading image, falling back to default: " + imagePath);
            }
        }

        if (imageToSet == null) {
            try {
                imageToSet = new Image(getClass().getResourceAsStream("/com/mediatracker/images/default-cover.png"));
            } catch (Exception e) {
                System.err.println("CRITICAL: Failed to load default cover image.");
            }
        }

        imageView.setImage(imageToSet);

        // Get Russian translations if they exist, otherwise convert from English
        String russianType = currentItem.getProperty("russianType");
        String russianStatus = currentItem.getProperty("russianStatus");
        
        typeValueText.setText(russianType != null ? russianType : convertTypeToRussian(currentItem.getType()));
        statusValueText.setText(russianStatus != null ? russianStatus : convertStatusToRussian(currentItem.getStatus()));
        
        ratingValueText.setText(String.format("%d", (int) (currentItem.getRating() * 2)));
        releaseDateValueText.setText(currentItem.getReleaseDate() != null ? currentItem.getReleaseDate().toString() : "N/A");
        genreValueText.setText(currentItem.getGenre() != null ? currentItem.getGenre() : "N/A");
        descriptionText.setText(currentItem.getDescription() != null && !currentItem.getDescription().isEmpty() ? currentItem.getDescription() : "не добавлено");

        boolean hasDuration = currentItem.getDurationMinutes() > 0;
        durationValueText.setText(hasDuration ? currentItem.getDurationMinutes() + " мин" : "");
        durationValueText.getParent().setVisible(hasDuration);
        durationValueText.getParent().setManaged(hasDuration);

        boolean hasEpisodes = currentItem.getEpisodes() > 0;
        episodesValueText.setText(hasEpisodes ? String.valueOf(currentItem.getEpisodes()) : "");
        episodesValueText.getParent().setVisible(hasEpisodes);
        episodesValueText.getParent().setManaged(hasEpisodes);

        boolean hasChapters = currentItem.getChapters() > 0;
        chaptersValueText.setText(hasChapters ? String.valueOf(currentItem.getChapters()) : "");
        chaptersValueText.getParent().setVisible(hasChapters);
        chaptersValueText.getParent().setManaged(hasChapters);
    }

    @FXML
    private void handleBack() {
        if (mainViewController != null) {
            // Добавляем небольшую задержку для плавности анимации
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            // Используем _ для неиспользуемого параметра события
            pause.setOnFinished(_ -> {
                if ("Избранное".equals(fromView)) {
                    mainViewController.showFavoritesView();
                } else if (fromView != null && !fromView.isEmpty()) {
                    // fromView содержит имя папки, из которой перешли
                    mainViewController.showFolderContent(fromView);
                } else {
                    // Если fromView не задан, возвращаемся к списку папок
                    mainViewController.returnToMain();
                }
            });
            pause.play();
        }
    }

    @FXML
    private void handleEdit() {
        if (mainViewController != null && currentItem != null) {
            mainViewController.showEditItemView(currentItem, fromView);
        }
    }

    @FXML
    private void toggleFavorite() {
        if (currentItem == null) return;

        boolean newFavoriteState = !currentItem.isFavorite();
        currentItem.setFavorite(newFavoriteState);
        
        int userId = SessionManager.getInstance().getCurrentUserId();
        DatabaseManager.setItemFavoriteStatus(currentItem.getId(), newFavoriteState, userId);

        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        if (currentItem.isFavorite()) {
            favoriteIcon.setFill(Color.web("#ff00f7"));
        } else {
            favoriteIcon.setFill(Color.WHITE);
        }
    }
}
