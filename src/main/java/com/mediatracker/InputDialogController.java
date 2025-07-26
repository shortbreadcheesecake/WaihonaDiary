package com.mediatracker;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.util.function.Consumer;

public class InputDialogController {

    @FXML private Label headerLabel;
    @FXML private TextField inputField;

    private Consumer<String> okConsumer;
    private Runnable cancelConsumer;

    public void setHeaderText(String text) {
        headerLabel.setText(text);
    }

    public void setPromptText(String text) {
        inputField.setPromptText(text);
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
            okConsumer.accept(inputField.getText());
        }
    }

    @FXML
    private void handleCancel() {
        if (cancelConsumer != null) {
            cancelConsumer.run();
        }
    }
}
