package in.bytehue.osgifx.console.application.decoration;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;

public class DecorationController implements Initializable {

    @FXML
    private ToolBar decorationArea;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        Platform.exit();
    }

    Stage getStage() {
        return (Stage) decorationArea.getScene().getWindow();
    }
}