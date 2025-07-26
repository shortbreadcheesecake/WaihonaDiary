package com.mediatracker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class CustomDialog {

    // Alias for showInformationDialog to maintain backward compatibility
    public static void showErrorDialog(StackPane parent, String header, String content) {
        showInformationDialog(parent, header, content);
    }
    
    public static void showInformationDialog(StackPane parent, String header, String content) {
        try {
            FXMLLoader dialogLoader = new FXMLLoader(CustomDialog.class.getResource("custom-dialog.fxml"));
            StackPane dialogRoot = dialogLoader.load();

            FXMLLoader infoLoader = new FXMLLoader(CustomDialog.class.getResource("information-dialog.fxml"));
            Node infoContent = infoLoader.load();
            InformationDialogController controller = infoLoader.getController();

            controller.setHeaderText(header);
            controller.setContentText(content);
            controller.setOnOk(() -> parent.getChildren().remove(dialogRoot));

            VBox dialogContentVBox = (VBox) dialogRoot.lookup("#dialogContent");
            dialogContentVBox.setMaxHeight(VBox.USE_PREF_SIZE);
            dialogContentVBox.getChildren().add(infoContent);

            parent.getChildren().add(dialogRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void showChoiceDialog(StackPane parent, String header, String content, List<String> items, Consumer<String> onOk) {
        try {
            FXMLLoader dialogLoader = new FXMLLoader(CustomDialog.class.getResource("custom-dialog.fxml"));
            StackPane dialogRoot = dialogLoader.load();

            FXMLLoader choiceLoader = new FXMLLoader(CustomDialog.class.getResource("choice-dialog.fxml"));
            Node choiceContent = choiceLoader.load();
            ChoiceDialogController controller = choiceLoader.getController();

            controller.setHeaderText(header);
            controller.setContentText(content);
            controller.setItems(items);
            controller.setOnOk(result -> {
                parent.getChildren().remove(dialogRoot);
                onOk.accept(result);
            });
            controller.setOnCancel(() -> parent.getChildren().remove(dialogRoot));

            VBox dialogContentVBox = (VBox) dialogRoot.lookup("#dialogContent");
            dialogContentVBox.setMaxHeight(VBox.USE_PREF_SIZE);
            dialogContentVBox.getChildren().add(choiceContent);

            parent.getChildren().add(dialogRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void showConfirmationDialog(StackPane parent, String header, String content, Runnable onOk) {
        try {
            // Load the dialog wrapper
            FXMLLoader dialogLoader = new FXMLLoader(CustomDialog.class.getResource("custom-dialog.fxml"));
            StackPane dialogRoot = dialogLoader.load();

            // Load the confirmation dialog content
            FXMLLoader confirmationLoader = new FXMLLoader(CustomDialog.class.getResource("confirmation-dialog.fxml"));
            Node confirmationContent = confirmationLoader.load();
            ConfirmationDialogController controller = confirmationLoader.getController();

            // Set up the controller
            controller.setHeaderText(header);
            controller.setContentText(content);
            controller.setOnOk(() -> {
                parent.getChildren().remove(dialogRoot);
                onOk.run();
            });
            controller.setOnCancel(() -> parent.getChildren().remove(dialogRoot));

            // Insert the content into the wrapper
            VBox dialogContentVBox = (VBox) dialogRoot.lookup("#dialogContent");
            dialogContentVBox.setMaxHeight(VBox.USE_PREF_SIZE);
            dialogContentVBox.getChildren().add(confirmationContent);

            // Show the dialog
            parent.getChildren().add(dialogRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static <T> void showInputDialog(StackPane parent, String header, String prompt, Consumer<String> onOk) {
        try {
            // Load the dialog wrapper
            FXMLLoader dialogLoader = new FXMLLoader(CustomDialog.class.getResource("custom-dialog.fxml"));
            StackPane dialogRoot = dialogLoader.load();

            // Load the input dialog content
            FXMLLoader inputLoader = new FXMLLoader(CustomDialog.class.getResource("input-dialog.fxml"));
            Node inputContent = inputLoader.load();
            InputDialogController controller = inputLoader.getController();

            // Set up the controller
            controller.setHeaderText(header);
            controller.setPromptText(prompt);
            controller.setOnOk(result -> {
                parent.getChildren().remove(dialogRoot);
                onOk.accept(result);
            });
            controller.setOnCancel(() -> parent.getChildren().remove(dialogRoot));

            // Insert the content into the wrapper
            VBox dialogContentVBox = (VBox) dialogRoot.lookup("#dialogContent");
            dialogContentVBox.setMaxHeight(VBox.USE_PREF_SIZE);
            dialogContentVBox.getChildren().add(inputContent);

            // Show the dialog
            parent.getChildren().add(dialogRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
