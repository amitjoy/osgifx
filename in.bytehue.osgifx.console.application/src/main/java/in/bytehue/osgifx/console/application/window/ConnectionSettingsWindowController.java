package in.bytehue.osgifx.console.application.fxml;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class ConnectionSettingsWindowController implements Initializable {

    @FXML
    private AnchorPane anchorPanel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final Scene stage = anchorPanel.getScene();
        anchorPanel.setAccessibleHelp("Hellp, I am help");
        System.out.println("====> 1 ====> " + stage);
    }

    public void doThis(final ActionEvent e) {
        final Scene stage = anchorPanel.getScene();
        System.out.println("====> 2 ====> " + stage);
    }
}
