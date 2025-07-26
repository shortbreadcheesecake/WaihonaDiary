package com.mediatracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppPreferences {
    private static final String PREFS_FILE_NAME = "app_prefs.properties";
    private static final String LAST_USER_ID_KEY = "last_user_id";

    private static Properties getProperties() {
        Properties props = new Properties();
        File prefsFile = new File(PREFS_FILE_NAME);
        if (prefsFile.exists()) {
            try (FileInputStream in = new FileInputStream(prefsFile)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    private static void saveProperties(Properties props) {
        try (FileOutputStream out = new FileOutputStream(PREFS_FILE_NAME)) {
            props.store(out, "MediaTracker App Preferences");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveLastUserId(int userId) {
        Properties props = getProperties();
        props.setProperty(LAST_USER_ID_KEY, String.valueOf(userId));
        saveProperties(props);
    }

    public static int getLastUserId() {
        Properties props = getProperties();
        String userIdStr = props.getProperty(LAST_USER_ID_KEY);
        if (userIdStr != null) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public static void clearLastUserId() {
        Properties props = getProperties();
        props.remove(LAST_USER_ID_KEY);
        saveProperties(props);
    }
}
