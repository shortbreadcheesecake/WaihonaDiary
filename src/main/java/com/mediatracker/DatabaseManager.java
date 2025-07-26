package com.mediatracker;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DatabaseManager {

    private static final String DB_URL = "jdbc:h2:./data/media_tracker_db"; // Store DB in data subdirectory

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            // Create users table first, as other tables depend on it
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "username VARCHAR(255) NOT NULL UNIQUE, " +
                         "password VARCHAR(255) NOT NULL)");

            // Create folders table with a foreign key to users
            stmt.execute("CREATE TABLE IF NOT EXISTS folders (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "name VARCHAR(255) NOT NULL, " +
                         "user_id INT, " +
                         "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");

            // Create media_items table with foreign keys to folders and users
            stmt.execute("CREATE TABLE IF NOT EXISTS media_items (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "folder_id INT, " +
                         "user_id INT, " +
                         "title VARCHAR(255), " +
                         "type VARCHAR(50), " +
                         "status VARCHAR(50), " +
                         "rating DOUBLE, " +
                         "description TEXT, " +
                         "is_favorite BOOLEAN, " +
                         "image_path VARCHAR(1024), " +
                         "release_date VARCHAR(255), " +
                         "genre VARCHAR(255), " +
                         "duration_minutes INT, " +
                         "episodes INT, " +
                         "chapters INT, " +
                         "FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE, " +
                         "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean createNewFolder(String folderName, int userId) {
        // Sanitize folder name to be a valid table name
        String tableName = folderName.replaceAll("[^a-zA-Z0-9_]", "_").toUpperCase();

        // SQL to create the actual table for the items
        String createTableSql = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
            " id INT AUTO_INCREMENT PRIMARY KEY," +
            " title VARCHAR(255) NOT NULL," +
            " type VARCHAR(50)," +
            " status VARCHAR(50)," +
            " rating DOUBLE," +
            " description TEXT," +
            " is_favorite BOOLEAN DEFAULT FALSE," +
            " image_path VARCHAR(255)," +
            " release_date VARCHAR(50)," +
            " genre VARCHAR(255)," +

            " duration_minutes INT," +
            " episodes INT," +
            " chapters INT)", tableName);

        // SQL to insert the folder name into the FOLDERS table
        String insertFolderSql = "INSERT INTO folders (name, user_id) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertFolderSql)) {
            
            conn.setAutoCommit(false);
            stmt.executeUpdate(createTableSql);
            pstmt.setString(1, folderName);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            conn.commit();
            return true;

        } catch (SQLException e) {
            if (e.getErrorCode() == 23505) { // Unique constraint violation
                System.err.println("A folder with this name already exists: " + folderName);
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Retrieves a list of folder names for the given user.
     * 
     * @param userId the ID of the user who owns the folders
     * @return a list of folder names
     */
    public static List<String> getFolders(int userId) {
        List<String> folders = new ArrayList<>();
        String sql = "SELECT name FROM folders WHERE user_id = ? ORDER BY name";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                folders.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return folders;
    }

    /**
     * Retrieves the name of the folder with the given ID.
     * 
     * @param folderId the ID of the folder
     * @return the name of the folder, or null if the folder does not exist
     */
    public static String getFolderNameById(int folderId) {
        String sql = "SELECT name FROM folders WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, folderId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Retrieves the number of media items in the given folder for the given user.
     * 
     * @param userId the ID of the user who owns the folder
     * @param folderName the name of the folder
     * @return the number of media items in the folder
     */
    public static int getMediaItemCountInFolder(int userId, String folderName) {
        String sql = "SELECT COUNT(*) FROM media_items mi " +
                     "JOIN folders f ON mi.folder_id = f.id " +
                     "WHERE f.user_id = ? AND f.name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, folderName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public static int getFolderId(String folderName, int userId) {
        String sql = "SELECT id FROM folders WHERE name = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, folderName);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<MediaItem> getItemsForFolder(int folderId, int userId) {
        List<MediaItem> items = new ArrayList<>();
        String sql = "SELECT mi.*, f.name as folder_name FROM media_items mi JOIN folders f ON mi.folder_id = f.id WHERE mi.folder_id = ? AND mi.user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, folderId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new MediaItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getBoolean("is_favorite"),
                        rs.getString("image_path"),
                        rs.getString("release_date"),
                        rs.getString("genre"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("episodes"),
                        rs.getInt("chapters"),
                        rs.getInt("folder_id"),
                        rs.getString("folder_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static long getItemCountForFolder(int folderId, int userId) {
        String sql = "SELECT COUNT(*) FROM media_items WHERE folder_id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, folderId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<MediaItem> getItemsForFolder(int folderId, int userId, int pageIndex, int pageSize) {
        List<MediaItem> items = new ArrayList<>();
        String sql = "SELECT mi.*, f.name as folder_name FROM media_items mi JOIN folders f ON mi.folder_id = f.id WHERE mi.folder_id = ? AND mi.user_id = ? ORDER BY mi.id DESC LIMIT ? OFFSET ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, folderId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, pageSize);
            pstmt.setInt(4, pageIndex * pageSize);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new MediaItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getBoolean("is_favorite"),
                        rs.getString("image_path"),
                        rs.getString("release_date"),
                        rs.getString("genre"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("episodes"),
                        rs.getInt("chapters"),
                        rs.getInt("folder_id"),
                        rs.getString("folder_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static boolean addItemToFolder(MediaItem item, int folderId, int userId) {
        String sql = "INSERT INTO media_items(title, type, status, rating, is_favorite, image_path, release_date, genre, duration_minutes, episodes, chapters, folder_id, user_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getType());
            pstmt.setString(3, item.getStatus());
            pstmt.setDouble(4, item.getRating());
            pstmt.setBoolean(5, item.isFavorite());
            pstmt.setString(6, item.getImagePath());
            pstmt.setString(7, item.getReleaseDate());
            pstmt.setString(8, item.getGenre());
            pstmt.setInt(9, item.getDurationMinutes());
            pstmt.setInt(10, item.getEpisodes());
            pstmt.setInt(11, item.getChapters());
            pstmt.setInt(12, folderId);
            pstmt.setInt(13, userId);
            
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateItem(MediaItem item, int userId) {
        String sql = "UPDATE media_items SET title = ?, type = ?, status = ?, rating = ?, description = ?, is_favorite = ?, image_path = ?, release_date = ?, genre = ?, duration_minutes = ?, episodes = ?, chapters = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getType());
            pstmt.setString(3, item.getStatus());
            pstmt.setDouble(4, item.getRating());
            pstmt.setString(5, item.getDescription());
            pstmt.setBoolean(6, item.isFavorite());
            pstmt.setString(7, item.getImagePath());
            pstmt.setString(8, item.getReleaseDate());
            pstmt.setString(9, item.getGenre());
            pstmt.setInt(10, item.getDurationMinutes());
            pstmt.setInt(11, item.getEpisodes());
            pstmt.setInt(12, item.getChapters());
            pstmt.setInt(13, item.getId());
            pstmt.setInt(14, userId);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean registerUser(String username, String password) {
        // IMPORTANT: In a real application, passwords should be stored in hashed form!
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 23505) { // Unique constraint violation for username
                return false; // User already exists
            }
            e.printStackTrace();
            return false;
        }
    }

    public static int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return -1; // User not found
    }

    public static int getFavoriteMediaItemCount(int userId) {
        String sql = "SELECT COUNT(*) FROM media_items WHERE user_id = ? AND is_favorite = TRUE";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public static List<MediaItem> getFavoriteMediaItems(int userId, int pageIndex, int pageSize) {
        List<MediaItem> items = new ArrayList<>();
        String sql = "SELECT mi.*, f.name as folder_name FROM media_items mi " +
                     "JOIN folders f ON mi.folder_id = f.id " +
                     "WHERE mi.user_id = ? AND mi.is_favorite = TRUE ORDER BY mi.id LIMIT ? OFFSET ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, pageSize);
            pstmt.setInt(3, pageIndex * pageSize);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(new MediaItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getBoolean("is_favorite"),
                        rs.getString("image_path"),
                        rs.getString("release_date"),
                        rs.getString("genre"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("episodes"),
                        rs.getInt("chapters"),
                        rs.getInt("folder_id"),
                        rs.getString("folder_name")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return items;
    }

    public static List<MediaItem> getFavoriteMediaItems(int userId) {
        List<MediaItem> items = new ArrayList<>();
        String sql = "SELECT mi.*, f.name as folder_name FROM media_items mi JOIN folders f ON mi.folder_id = f.id WHERE mi.user_id = ? AND mi.is_favorite = TRUE";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new MediaItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getBoolean("is_favorite"),
                        rs.getString("image_path"),
                        rs.getString("release_date"),
                        rs.getString("genre"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("episodes"),
                        rs.getInt("chapters"),
                        rs.getInt("folder_id"),
                        rs.getString("folder_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static boolean setItemFavoriteStatus(int itemId, boolean isFavorite, int userId) {
        String sql = "UPDATE media_items SET is_favorite = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isFavorite);
            pstmt.setInt(2, itemId);
            pstmt.setInt(3, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    // IMPORTANT: In a real application, hash comparison should be done here
                    return storedPassword.equals(password);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // User not found or password mismatch
    }

    public static List<MediaItem> getAllMediaItemsForUser(int userId) {
        List<MediaItem> items = new ArrayList<>();
        String sql = "SELECT mi.*, f.name as folder_name FROM media_items mi JOIN folders f ON mi.folder_id = f.id WHERE mi.user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new MediaItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getBoolean("is_favorite"),
                        rs.getString("image_path"),
                        rs.getString("release_date"),
                        rs.getString("genre"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("episodes"),
                        rs.getInt("chapters"),
                        rs.getInt("folder_id"),
                        rs.getString("folder_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<FolderData> getAllFoldersForUser(int userId) {
        List<FolderData> folders = new ArrayList<>();
        String sql = "SELECT id, name FROM folders WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                folders.add(new FolderData(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return folders;
    }

    public static boolean userExists(int userId) {
        String sql = "SELECT id FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // true if a user with this ID exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void deleteFolder(String folderName, int userId) {
        int folderId = getFolderId(folderName, userId);
        if (folderId == -1) {
            System.out.println("Folder not found for deletion: " + folderName);
            return;
        }

        String deleteItemsSQL = "DELETE FROM media_items WHERE folder_id = ?";
        String deleteFolderSQL = "DELETE FROM folders WHERE id = ? AND user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmtItems = conn.prepareStatement(deleteItemsSQL);
             PreparedStatement pstmtFolder = conn.prepareStatement(deleteFolderSQL)) {

            // Step 1: Delete all media items associated with this folder
            pstmtItems.setInt(1, folderId);
            pstmtItems.executeUpdate();

            // Step 2: Delete the folder itself
            pstmtFolder.setInt(1, folderId);
            pstmtFolder.setInt(2, userId);
            pstmtFolder.executeUpdate();

            System.out.println("Folder '" + folderName + "' and all its contents have been successfully deleted.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
