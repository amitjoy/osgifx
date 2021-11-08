package in.bytehue.osgifx.console.application.fxml.controller;

import java.io.File;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.application.dialog.BundleInstallDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public final class InstallBundleDialogController {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private ThreadSynchronize threadSync;
    @FXML
    private Button            fileChooser;
    @FXML
    private ToggleSwitch      startBundleToggle;
    @FXML
    private GridPane          installBundleDialogPane;
    private File              bundle;

    @FXML
    public void initialize() {
        registerDragAndDropSupport();
        logger.atInfo().log("FXML controller has been initialized");
    }

    @FXML
    private void chooseBundle(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'chooseBundle(..)' event has been invoked");
        final FileChooser bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        bundle = bundleChooser.showOpenDialog(null);
        if (bundle != null) {
            fileChooser.setText(bundle.getName());
            fileChooser.setTooltip(new Tooltip(bundle.getName()));
        }
    }

    public BundleInstallDTO getInstallDTO() {
        if (bundle == null) {
            return null;
        }
        return new BundleInstallDTO(bundle, startBundleToggle.isSelected());
    }

    private void registerDragAndDropSupport() {
        installBundleDialogPane.setOnDragOver(event -> {
            final Dragboard db         = event.getDragboard();
            final boolean   isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".jar");
            if (db.hasFiles() && isAccepted) {
                installBundleDialogPane.setStyle("-fx-background-color: #C6C6C6");
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        installBundleDialogPane.setOnDragDropped(event -> {
            final Dragboard db      = event.getDragboard();
            boolean         success = false;
            if (db.hasFiles()) {
                success = true;
                // Only get the first file from the list
                final File file = db.getFiles().get(0);
                threadSync.asyncExec(() -> {
                    fileChooser.setText(file.getName());
                    bundle = file;
                    fileChooser.setTooltip(new Tooltip(bundle.getName()));
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });
        logger.atInfo().log("Registered drag and drop support");
    }

}
