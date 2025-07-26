package com.mediatracker;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ConfirmationDialogController {

    @FXML private Label headerLabel;
    @FXML private Label contentLabel;

    private Runnable okConsumer;
    private Runnable cancelConsumer;

    public void setHeaderText(String text) {
        headerLabel.setText(text);
    }

    public void setContentText(String text) {
        contentLabel.setText(text);
    }

    public void setOnOk(Runnable consumer) {
        this.okConsumer = consumer;
    }

    public void setOnCancel(Runnable consumer) {
        this.cancelConsumer = consumer;
    }

    @FXML
    private void handleOk() {
        if (okConsumer != null) {
            okConsumer.run();
        }
    }

    @FXML
    private void handleCancel() {
        if (cancelConsumer != null) {
            cancelConsumer.run();
        }
    }
}
