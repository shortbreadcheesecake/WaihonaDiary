package com.mediatracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class InformationDialogController {

    @FXML
    private Label headerLabel;

    @FXML
    private Label contentLabel;

    @FXML
    private Button okButton;

    private Runnable onOk;

    public void setHeaderText(String text) {
        headerLabel.setText(text);
    }

    public void setContentText(String text) {
        contentLabel.setText(text);
    }

    public void setOnOk(Runnable onOk) {
        this.onOk = onOk;
    }

    @FXML
    private void handleOk() {
        if (onOk != null) {
            onOk.run();
        }
    }
}
