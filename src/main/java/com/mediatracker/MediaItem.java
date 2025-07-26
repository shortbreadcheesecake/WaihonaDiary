package com.mediatracker;

import java.util.HashMap;
import java.util.Map;

public class MediaItem {
    private int id;
    private String title;
    private String type;
    private String status;
    private double rating;
    private String description;
    private boolean isFavorite;
    private String imagePath;
    private String releaseDate;
    private String genre;
    private int durationMinutes;
    private int episodes;
    private int chapters;
    private int folderId;
    private String folderName;
    private final Map<String, String> properties = new HashMap<>();

    // Constructor for loading from DB
    public MediaItem(int id, String title, String type, String status, double rating, String description, boolean isFavorite, String imagePath, String releaseDate, String genre, int durationMinutes, int episodes, int chapters, int folderId, String folderName) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.status = status;
        this.rating = rating;
        this.description = description;
        this.isFavorite = isFavorite;
        this.imagePath = imagePath;
        this.releaseDate = releaseDate;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.episodes = episodes;
        this.chapters = chapters;
        this.folderId = folderId;
        this.folderName = folderName;
    }

    // Empty constructor for creating new elements
    public MediaItem() {}

    // Copy constructor
    public MediaItem(MediaItem other) {
        this.id = other.id;
        this.title = other.title;
        this.type = other.type;
        this.status = other.status;
        this.rating = other.rating;
        this.description = other.description;
        this.isFavorite = other.isFavorite;
        this.imagePath = other.imagePath;
        this.releaseDate = other.releaseDate;
        this.genre = other.genre;
        this.durationMinutes = other.durationMinutes;
        this.episodes = other.episodes;
        this.chapters = other.chapters;
        this.folderId = other.folderId;
        this.folderName = other.folderName;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public double getRating() { return rating; }
    public String getDescription() { return description; }
    public boolean isFavorite() { return isFavorite; }
    public String getImagePath() { return imagePath; }
    public String getReleaseDate() { return releaseDate; }
    public String getGenre() { return genre; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getEpisodes() { return episodes; }
    public int getChapters() { return chapters; }
    public int getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; }
    public void setRating(double rating) { this.rating = rating; }
    public void setDescription(String description) { this.description = description; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setEpisodes(int episodes) { this.episodes = episodes; }
    public void setChapters(int chapters) { this.chapters = chapters; }
    public void setFolderId(int folderId) { this.folderId = folderId; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    
    // Additional properties methods
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        return properties.get(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
