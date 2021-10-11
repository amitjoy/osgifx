package in.bytehue.osgifx.console.application.fxml.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import org.controlsfx.control.ToggleSwitch;

import in.bytehue.osgifx.console.application.dialog.InstallBundleDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

public final class InstallBundleDialogController implements Initializable {

    @FXML
    private Button fileChooser;

    @FXML
    private ToggleSwitch startBundleToggle;

    private File bundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    @FXML
    private void chooseBundle(final ActionEvent event) {
        final FileChooser bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        bundle = bundleChooser.showOpenDialog(null);
        if (bundle != null) {
            fileChooser.setText(bundle.getName());
        }
    }

    public InstallBundleDTO getInstallDTO() {
        if (bundle == null) {
            return null;
        }
        return new InstallBundleDTO(bundle, startBundleToggle.isSelected());
    }

}
