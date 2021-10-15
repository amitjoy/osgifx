package in.bytehue.osgifx.console.application.fxml.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;

public final class TopAreaDecorationController {

    @FXML
    private ToolBar decorationArea;

    @FXML
    public void handleClose(final ActionEvent event) {
        Platform.exit();
    }

    Stage getStage() {
        return (Stage) decorationArea.getScene().getWindow();
    }
}