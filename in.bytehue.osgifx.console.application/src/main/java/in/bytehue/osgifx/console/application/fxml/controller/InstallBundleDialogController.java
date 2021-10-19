package in.bytehue.osgifx.console.application.fxml.controller;

import java.io.File;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import in.bytehue.osgifx.console.application.dialog.InstallBundleDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

public final class InstallBundleDialogController {

    @FXML
    private Button       fileChooser;
    @FXML
    private ToggleSwitch startBundleToggle;
    @Log
    @Inject
    private Logger       logger;
    private File         bundle;

    @FXML
    public void initialize() {
        logger.info("FXML controller (" + getClass() + ") has been initialized");
    }

    @FXML
    private void chooseBundle(final ActionEvent event) {
        logger.debug("FXML controller (" + getClass() + ") 'chooseBundle(..)' event has been invoked");
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
