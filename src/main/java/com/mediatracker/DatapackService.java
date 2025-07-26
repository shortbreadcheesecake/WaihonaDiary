package com.mediatracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DatapackService {

    public void createDatapack(int userId, File outputFile) throws IOException {
        Path tempDir = Files.createTempDirectory("datapack-");
        Path imagesDir = tempDir.resolve("images");
        Files.createDirectory(imagesDir);

        List<FolderData> folders = DatabaseManager.getAllFoldersForUser(userId);
        List<MediaItem> items = DatabaseManager.getAllMediaItemsForUser(userId);
        List<MediaItem> itemsForJson = new ArrayList<>();

        for (MediaItem item : items) {
            MediaItem jsonItem = new MediaItem(item);
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                try {
                    Path sourcePath = Paths.get(new java.net.URI(item.getImagePath()));
                    if (Files.exists(sourcePath)) {
                        Path fileName = sourcePath.getFileName();
                        Files.copy(sourcePath, imagesDir.resolve(fileName));
                        jsonItem.setImagePath(fileName.toString()); // Save only the file name
                    } else {
                        jsonItem.setImagePath(null);
                    }
                } catch (java.net.URISyntaxException | java.nio.file.InvalidPathException e) {
                    System.err.println("Skipping invalid image path: " + item.getImagePath());
                    jsonItem.setImagePath(null); // Clear invalid path
                }
            }
            itemsForJson.add(jsonItem);
        }

        Datapack datapack = new Datapack();
        datapack.setFolders(folders);
        datapack.setItems(itemsForJson);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(datapack);
        Path jsonFile = tempDir.resolve("data.json");
        Files.write(jsonFile, json.getBytes());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            zos.putNextEntry(new ZipEntry("data.json"));
            Files.copy(jsonFile, zos);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("images/"));
            zos.closeEntry();

            for (Path imageFile : Files.newDirectoryStream(imagesDir)) {
                zos.putNextEntry(new ZipEntry("images/" + imageFile.getFileName().toString()));
                Files.copy(imageFile, zos);
                zos.closeEntry();
            }
        }

        // Clean up temporary directory
        Files.walk(tempDir)
             .sorted(java.util.Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }

    public void loadDatapack(int userId, File inputFile) throws IOException {
        Path tempDir = Files.createTempDirectory("datapack-import-");
        Path imageStorageDir = Paths.get("./data/images");
        Files.createDirectories(imageStorageDir);

        try {
            // 1. Unzip the file
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(inputFile))) {
                java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    Path newPath = tempDir.resolve(zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(newPath);
                    } else {
                        if (newPath.getParent() != null) {
                            Files.createDirectories(newPath.getParent());
                        }
                        Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                    zipEntry = zis.getNextEntry();
                }
            }

            // 2. Read JSON
            Path jsonFile = tempDir.resolve("data.json");
            String json = new String(Files.readAllBytes(jsonFile));
            Gson gson = new Gson();
            Datapack datapack = gson.fromJson(json, Datapack.class);

            // 3. Process data
            Map<Integer, Integer> oldToNewFolderIdMap = new java.util.HashMap<>();

            for (FolderData folderData : datapack.getFolders()) {
                int existingFolderId = DatabaseManager.getFolderId(folderData.getName(), userId);
                if (existingFolderId == -1) {
                    DatabaseManager.createNewFolder(folderData.getName(), userId);
                    int newFolderId = DatabaseManager.getFolderId(folderData.getName(), userId);
                    oldToNewFolderIdMap.put(folderData.getId(), newFolderId);
                } else {
                    oldToNewFolderIdMap.put(folderData.getId(), existingFolderId);
                }
            }

            for (MediaItem item : datapack.getItems()) {
                // Copy image
                if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                    String imageName = item.getImagePath(); // Now only the file name is here
                    Path sourceImage = tempDir.resolve("images").resolve(imageName);
                    Path destImage = imageStorageDir.resolve(imageName);

                    if (Files.exists(sourceImage)) {
                        Files.copy(sourceImage, destImage, StandardCopyOption.REPLACE_EXISTING);
                        item.setImagePath(destImage.toUri().toString()); // Update path to full URI for DB
                    } else {
                        item.setImagePath(null); // If image is missing, set path to null
                    }
                }

                int newFolderId = oldToNewFolderIdMap.get(item.getFolderId());
                DatabaseManager.addItemToFolder(item, newFolderId, userId);
            }

        } finally {
            // 4. Clean up
            Files.walk(tempDir)
                 .sorted(java.util.Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }
}

class Datapack {
    private List<FolderData> folders;
    private List<MediaItem> items;

    public Datapack() {}

    public List<FolderData> getFolders() {
        return folders;
    }

    public void setFolders(List<FolderData> folders) {
        this.folders = folders;
    }

    public List<MediaItem> getItems() {
        return items;
    }

    public void setItems(List<MediaItem> items) {
        this.items = items;
    }
}

class FolderData {
    private final int id;
    private final String name;

    public FolderData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
