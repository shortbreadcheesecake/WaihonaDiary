package com.mediatracker;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.layout.StackPane;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class AddItemViewController {
    private int userId;

    @FXML private VBox rootPane;
    @FXML private TextField titleField;
    @FXML private StackPane imagePane;
    @FXML private ImageView imageView;
    @FXML private Label imagePlaceholderLabel;
    @FXML private HBox ratingBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker releaseDatePicker;
    @FXML private TextField genreField;

    @FXML private VBox dynamicFieldsPane;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private MainViewController mainViewController;
    private String currentFolderName;
    private String imagePath;
    private double currentRating = 0.0;
    private final List<FontAwesomeIconView> stars = new ArrayList<>();
    private MediaItem itemToEdit = null;
    private String returnView; // To know where to return (folder name or "Избранное")

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Dynamic fields
    private TextField durationField; // For Movie, Anime
    private TextField episodesField; // For Series, Anime
    private TextField chaptersField; // For Book, Manga

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public void setFolderData(String folderName) {
        this.currentFolderName = folderName;
        this.returnView = folderName; // Устанавливаем returnView перед сбросом формы
        resetForm(); // This also resets itemToEdit to null
    }

    public void setItemToEdit(MediaItem item, String fromView) {
        resetForm(); // Start with a clean slate
        this.itemToEdit = item;
        this.returnView = fromView;
        this.currentFolderName = item.getFolderName(); // Set folder context for saving

        titleField.setText(item.getTitle());
        
        // Get Russian translations if they exist, otherwise use the English values
        String type = item.getProperty("russianType");
        if (type == null) {
            type = convertTypeToRussian(item.getType());
        }
        typeComboBox.getSelectionModel().select(type);
        
        String status = item.getProperty("russianStatus");
        if (status == null && item.getStatus() != null) {
            status = convertStatusToRussian(item.getStatus());
        }
        statusComboBox.getSelectionModel().select(status);
        
        currentRating = item.getRating();
        setStars(currentRating);

        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            imagePath = item.getImagePath();
            try {
                imageView.setImage(new Image(imagePath));
                imagePlaceholderLabel.setVisible(false);
            } catch (Exception e) {
                System.err.println("Error loading image: " + imagePath);
                imageView.setImage(null);
                imagePlaceholderLabel.setVisible(true);
            }
        }

        if (item.getReleaseDate() != null && !item.getReleaseDate().isEmpty()) {
            try {
                releaseDatePicker.setValue(java.time.LocalDate.parse(item.getReleaseDate()));
            } catch (java.time.format.DateTimeParseException e) {
                System.err.println("Could not parse date: " + item.getReleaseDate());
                releaseDatePicker.setValue(null);
            }
        }
        
        genreField.setText(item.getGenre());
        descriptionArea.setText(item.getDescription());

        // Update dynamic fields based on the original type (not the Russian text)
        updateDynamicFields(item.getType());

        if (durationField != null && item.getDurationMinutes() > 0) {
            int hours = item.getDurationMinutes() / 60;
            int minutes = item.getDurationMinutes() % 60;
            durationField.setText(String.format("%02d:%02d", hours, minutes));
        }
        if (episodesField != null && item.getEpisodes() > 0) {
            episodesField.setText(String.valueOf(item.getEpisodes()));
        }
        if (chaptersField != null && item.getChapters() > 0) {
            chaptersField.setText(String.valueOf(item.getChapters()));
        }
    }

    @FXML
    public void initialize() {
        setupRatingStars();
        setupComboBoxes();
        setupDatePicker();

        setupDragAndDrop();

        typeComboBox.valueProperty().addListener((unusedObs, unusedOldVal, newVal) -> {
            updateDynamicFields(newVal);
        });
    }

    private void setupRatingStars() {
        // The label is now part of the FXML, so we just add stars
        for (int i = 0; i < 5; i++) {
            FontAwesomeIconView star = new FontAwesomeIconView(FontAwesomeIcon.STAR_ALT);
            star.setSize("30");
            star.setFill(Color.GRAY);
            final int starIndex = i;

            star.setOnMouseEntered(e -> {
                // Determine rating based on which half of the star is hovered
                boolean isRightHalf = e.getX() > star.getBoundsInLocal().getWidth() / 2;
                double hoverRating = starIndex + (isRightHalf ? 1.0 : 0.5);
                setStars(hoverRating);
            });
            star.setOnMouseExited(e -> setStars(currentRating)); // Revert to current rating
            star.setOnMouseClicked(e -> {
                boolean isRightHalf = e.getX() > star.getBoundsInLocal().getWidth() / 2;
                currentRating = starIndex + (isRightHalf ? 1.0 : 0.5);
                setStars(currentRating);
            });

            stars.add(star);
            ratingBox.getChildren().add(star);
        }
    }

    private void setStars(double rating) {
        for (int i = 0; i < stars.size(); i++) {
            FontAwesomeIconView star = stars.get(i);
            if (rating >= i + 1.0) {
                star.setIcon(FontAwesomeIcon.STAR);
                star.setFill(Color.web("#b33ede"));
            } else if (rating >= i + 0.5) {
                star.setIcon(FontAwesomeIcon.STAR_HALF_ALT);
                star.setFill(Color.web("#b33ede"));
            } else {
                star.setIcon(FontAwesomeIcon.STAR_ALT);
                star.setFill(Color.GRAY);
            }
        }
    }

    private void setupComboBoxes() {
    // Устанавливаем русские названия для типов
    typeComboBox.setItems(FXCollections.observableArrayList(
        "Фильм",       // Movie
        "Сериал",      // Series
        "Книга",       // Book
        "Манга",       // Manga
        "Аниме"        // Anime
    ));
    
    // Устанавливаем русские названия для статусов
    statusComboBox.setItems(FXCollections.observableArrayList(
        "В процессе",  // Ongoing
        "Вышло",       // Released
        "Анонсировано" // Announced
    ));
}
    
    private void setupDatePicker() {
        // Configure DatePicker to only allow date input (no time)
        releaseDatePicker.setShowWeekNumbers(false);
        releaseDatePicker.setEditable(false); // Prevent manual text input
        
        // Set a reasonable date range (e.g., from 1900 to current year + 10)
        java.time.LocalDate minDate = java.time.LocalDate.of(1900, 1, 1);
        java.time.LocalDate maxDate = java.time.LocalDate.now().plusYears(10);
        
        // Add a custom cell factory to disable dates outside the range
        releaseDatePicker.setDayCellFactory(unusedPicker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && (date.isBefore(minDate) || date.isAfter(maxDate))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // Light pink for disabled dates
                }
            }
        });
    }



    private void updateDynamicFields(String type) {
        dynamicFieldsPane.getChildren().clear();
        durationField = null;
        episodesField = null;
        chaptersField = null;

        if (type == null) return;
        
        // Преобразуем английский тип обратно в русский для отображения
            // Конвертируем английский тип в русский для отображения
            convertTypeToRussian(type);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        String labelStyle = "-fx-text-fill: #E0E0E0;";

        // Set column constraints for the grid
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int rowIndex = 0;

        // Используем английский тип для switch-case
        switch (type) {
            case "Movie":
                durationField = new TextField();
                durationField.setPromptText("ЧЧ:ММ");
                Label durationLabelMovie = new Label("Длительность фильма:");
                durationLabelMovie.setStyle(labelStyle);
                grid.add(durationLabelMovie, 0, rowIndex);
                grid.add(durationField, 1, rowIndex++);
                break;

            case "Series":
            case "Anime":
                episodesField = new TextField();
                episodesField.setPromptText("Количество эпизодов");
                Label episodesLabel = new Label("Количество эпизодов:");
                episodesLabel.setStyle(labelStyle);
                grid.add(episodesLabel, 0, rowIndex);
                grid.add(episodesField, 1, rowIndex++);

                durationField = new TextField();
                durationField.setPromptText("ЧЧ:ММ");
                Label durationLabelSeries = new Label("Длительность эпизода:");
                durationLabelSeries.setStyle(labelStyle);
                grid.add(durationLabelSeries, 0, rowIndex);
                grid.add(durationField, 1, rowIndex++);
                break;

            case "Book":
            case "Manga":
                chaptersField = new TextField();
                chaptersField.setPromptText("Количество глав");
                Label chaptersLabel = new Label("Количество глав:");
                chaptersLabel.setStyle(labelStyle);
                grid.add(chaptersLabel, 0, rowIndex);
                grid.add(chaptersField, 1, rowIndex++);
                break;
        }

        dynamicFieldsPane.getChildren().add(grid);
    }

    @FXML
    private void handleImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select cover");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            loadImage(selectedFile);
        }
    }

    private void setupDragAndDrop() {
        imagePane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
                
        // Handle drag and drop for images
        imagePane.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                if (!files.isEmpty()) {
                    // Use the first file that is a supported image
                    File file = files.stream()
                            .filter(f -> {
                                String name = f.getName().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".bmp");
                            })
                            .findFirst()
                            .orElse(null);

                    if (file != null) {
                        loadImage(file);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void loadImage(File file) {
        if (file != null) {
            try {
                // 1. Define the destination directory
                Path destDir = Paths.get("data", "images");

                // 2. Create a unique filename to avoid conflicts
                String originalFileName = file.getName();
                String fileExtension = "";
                int i = originalFileName.lastIndexOf('.');
                if (i > 0) {
                    fileExtension = originalFileName.substring(i);
                }
                String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                Path destPath = destDir.resolve(uniqueFileName);

                // 3. Copy the file
                Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                // 4. Store the new path (as a URI string) and update the ImageView
                imagePath = destPath.toUri().toString();
                Image image = new Image(imagePath);
                imageView.setImage(image);
                imagePlaceholderLabel.setVisible(false);

            } catch (IOException e) {
                CustomDialog.showErrorDialog(
                    mainViewController.getCenterStackPane(),
                    "Ошибка при загрузке изображения",
                    "Произошла ошибка при загрузке изображения. Пожалуйста, попробуйте снова."
                );
            }
        } else {
            CustomDialog.showInformationDialog(
                mainViewController.getCenterStackPane(),
                "Не выбрано изображение",
                "Пожалуйста, выберите изображение для загрузки."
            );
        }
    }

    // Helper method to convert Russian type to English for database storage
    private String convertTypeToEnglish(String russianType) {
        if (russianType == null) return null;
        return switch (russianType) {
            case "Фильм" -> "Movie";
            case "Сериал" -> "Series";
            case "Книга" -> "Book";
            case "Манга" -> "Manga";
            case "Аниме" -> "Anime";
            default -> russianType; // Return as is if not in our mapping
        };
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

    // Helper method to convert Russian status to English for database storage
    private String convertStatusToEnglish(String russianStatus) {
        if (russianStatus == null) return null;
        return switch (russianStatus) {
            case "В процессе" -> "Ongoing";
            case "Вышло" -> "Released";
            case "Анонсировано" -> "Announced";
            default -> russianStatus; // Return as is if not in our mapping
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

    @FXML
    private void handleSave() {
        System.out.println("=== handleSave started ===");
        System.out.println("Current folder: " + currentFolderName);
        
        // 1. Validation
        if (titleField.getText().trim().isEmpty() || typeComboBox.getValue() == null) {
            CustomDialog.showInformationDialog(
                mainViewController.getCenterStackPane(),
                "Ошибка при сохранении",
                "Пожалуйста, заполните все обязательные поля."
            );
            return;
        }

        // 2. Parse dynamic fields
        int duration = 0;
        if (durationField != null && durationField.getText() != null && !durationField.getText().trim().isEmpty()) {
            String[] parts = durationField.getText().trim().split(":");
            if (parts.length == 2) {
                try {
                    duration = Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing time: " + durationField.getText());
                }
            }
        }
        int episodes = 0;
        if (episodesField != null && !episodesField.getText().isEmpty()) {
            try {
                episodes = Integer.parseInt(episodesField.getText());
            } catch (NumberFormatException e) {
                System.err.println("Invalid number for episodes: " + episodesField.getText());
            }
        }

        int chapters = 0;
        if (chaptersField != null && !chaptersField.getText().isEmpty()) {
            try {
                chapters = Integer.parseInt(chaptersField.getText());
            } catch (NumberFormatException e) {
                System.err.println("Invalid number for chapters: " + chaptersField.getText());
            }
        }

        // 3. Decide whether to create or update
        boolean success;
        String successMessage;

        MediaItem item = (itemToEdit == null) ? new MediaItem() : itemToEdit;

        // Convert UI values to database values
        String type = convertTypeToEnglish(typeComboBox.getValue());
        String status = statusComboBox.getValue() != null ? 
                      convertStatusToEnglish(statusComboBox.getValue()) : null;
            
        item.setTitle(titleField.getText().trim());
        item.setType(type);
        item.setStatus(status);
        item.setRating(currentRating);
        item.setDescription(descriptionArea.getText().trim());
        item.setImagePath(imagePath);
        item.setReleaseDate(releaseDatePicker.getValue() != null ? releaseDatePicker.getValue().toString() : "");
        item.setGenre(genreField.getText().trim());
        item.setDurationMinutes(duration);
        item.setEpisodes(episodes);
        item.setChapters(chapters);
            
        // Store the Russian values for display purposes
        item.setProperty("russianType", typeComboBox.getValue());
        if (statusComboBox.getValue() != null) {
            item.setProperty("russianStatus", statusComboBox.getValue());
        }

        if (itemToEdit == null) {
            int folderId = DatabaseManager.getFolderId(currentFolderName, userId);
            if (folderId == -1) {
                // Handle error: folder not found
                return;
            }
            item.setFolderId(folderId);
            success = DatabaseManager.addItemToFolder(item, folderId, userId);
            System.out.println("Item added to folder " + folderId + ", success: " + success);
            successMessage = "Элемент '" + item.getTitle() + "' успешно добавлен.";
        } else {
            success = DatabaseManager.updateItem(item, userId);
            System.out.println("Item updated, success: " + success);
            successMessage = "Элемент '" + item.getTitle() + "' успешно обновлен.";
        }

        // 4. Handle result
        if (success) {
            CustomDialog.showInformationDialog(
                mainViewController.getCenterStackPane(),
                "Успех",
                successMessage
            );
            
            System.out.println("Returning to view: " + (returnView != null ? returnView : "null"));
            if (returnView == null) {
                System.out.println("Warning: returnView is null, using currentFolderName: " + currentFolderName);
                returnView = currentFolderName;
            }
            
            // If we're coming from favorites or editing a favorite item
            boolean shouldRefreshFavorites = "Favorites".equals(returnView) || 
                                          (itemToEdit != null && itemToEdit.isFavorite());
            
            // Add a small delay to ensure the database is updated
            PauseTransition initialDelay = new PauseTransition(javafx.util.Duration.millis(300));
            initialDelay.setOnFinished(e -> {
                // First, update the favorites title
                mainViewController.updateFavoritesTitle();
                
                if (shouldRefreshFavorites) {
                    System.out.println("Refreshing favorites view");
                    // Force a complete refresh of the favorites view
                    mainViewController.showFavoritesView(true);
                    
                    // If we need to return to a specific folder after refreshing favorites
                    if (returnView != null && !"Favorites".equals(returnView)) {
                        System.out.println("Showing folder content: " + returnView);
                        // Small delay to ensure favorites view is refreshed
                        PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(200));
                        pause.setOnFinished(ev -> mainViewController.showFolderContent(returnView));
                        pause.play();
                    }
                } else if (returnView != null) {
                    System.out.println("Showing folder content: " + returnView);
                    mainViewController.showFolderContent(returnView);
                    
                    // Refresh the folder content after a short delay to ensure data is updated
                    PauseTransition refreshPause = new PauseTransition(javafx.util.Duration.millis(300));
                    refreshPause.setOnFinished(ev -> mainViewController.showFolderContent(returnView));
                    refreshPause.play();
                } else {
                    // Return to main if nothing else is specified
                    mainViewController.returnToMain();
                }
            });
            initialDelay.play();
        } else {
            CustomDialog.showInformationDialog(
                mainViewController.getCenterStackPane(),
                "Ошибка",
                "Не удалось сохранить элемент. Произошла ошибка при сохранении данных в базу данных."
            );
        }
            
        if (mainViewController != null) {
            if (itemToEdit != null) {
                // If editing, return to the item details view
                mainViewController.showItemDetails(itemToEdit, returnView);
            } else {
                // If adding a new item, return to the folder content view
                mainViewController.showFolderContent(currentFolderName);
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (mainViewController != null) {
            if (itemToEdit != null) {
                // If editing, return to the item details view
                mainViewController.showItemDetails(itemToEdit, returnView);
            } else if (returnView != null) {
                // If adding a new item and we have a return view, go back to it
                if ("Favorites".equals(returnView)) {
                    mainViewController.showFavoritesView(false);
                } else {
                    mainViewController.showFolderContent(returnView);
                }
            } else {
                // Default to main view if no specific return view
                mainViewController.returnToMain();
            }
        }
    }

    private void resetForm() {
        titleField.clear();
        typeComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().clearSelection();
        releaseDatePicker.setValue(null);
        genreField.clear();
        descriptionArea.clear();
        imageView.setImage(null);
        imagePlaceholderLabel.setVisible(true);
        imagePath = null;
        currentRating = 0.0;
        setStars(0.0);
        dynamicFieldsPane.getChildren().clear();
        itemToEdit = null; // Ensure we are in 'add' mode
    }
}
