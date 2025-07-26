package com.mediatracker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionManager {
    private static SessionManager instance;
    private int currentUserId = -1;
    private static final String SESSION_FILE = "session.dat";

    private SessionManager() {
        loadSession();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        saveSession();
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public void clearSession() {
        currentUserId = -1;
        deleteSessionFile();
    }

    private void saveSession() {
        try (FileWriter writer = new FileWriter(SESSION_FILE)) {
            writer.write(String.valueOf(currentUserId));
        } catch (IOException e) {
            System.err.println("Error saving session: " + e.getMessage());
        }
    }

    private void loadSession() {
        Path sessionPath = Paths.get(SESSION_FILE);
        if (Files.exists(sessionPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(SESSION_FILE))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    currentUserId = Integer.parseInt(line.trim());
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error loading session: " + e.getMessage());
                currentUserId = -1;
            }
        }
    }

    private void deleteSessionFile() {
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (IOException e) {
            System.err.println("Error deleting session file: " + e.getMessage());
        }
    }
}
