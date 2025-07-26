package com.mediatracker;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import java.util.List;
import java.util.function.Consumer;

public class ChoiceDialogController {

    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private ChoiceBox<String> choiceBox;

    private Consumer<String> okConsumer;
    private Runnable cancelConsumer;

    public void setHeaderText(String text) {
        headerLabel.setText(text);
    }

    public void setContentText(String text) {
        contentLabel.setText(text);
    }

    public void setItems(List<String> items) {
        choiceBox.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            choiceBox.setValue(items.get(0));
        }
    }

    public void setOnOk(Consumer<String> consumer) {
        this.okConsumer = consumer;
    }

    public void setOnCancel(Runnable consumer) {
        this.cancelConsumer = consumer;
    }

    @FXML
    private void handleOk() {
        if (okConsumer != null) {
            okConsumer.accept(choiceBox.getValue());
        }
    }

    @FXML
    private void handleCancel() {
        if (cancelConsumer != null) {
            cancelConsumer.run();
        }
    }
}
