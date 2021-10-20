package in.bytehue.osgifx.console.application.fxml.controller;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;

public final class TopAreaDecorationController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private ToolBar      decorationArea;

    @FXML
    public void initialize() {
        logger.atInfo().log("FXML controller has been initialized");
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        Platform.exit();
    }

    Stage getStage() {
        return (Stage) decorationArea.getScene().getWindow();
    }
}