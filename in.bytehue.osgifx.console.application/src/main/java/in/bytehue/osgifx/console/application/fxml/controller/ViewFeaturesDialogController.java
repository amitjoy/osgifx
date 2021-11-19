package in.bytehue.osgifx.console.application.fxml.controller;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.FxDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.update.UpdateAgent)")
public final class ViewFeaturesDialogController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @FXML
    private TableView<FeatureDTO>           featuresList;
    @FXML
    private TableColumn<FeatureDTO, String> idColumn;
    @FXML
    private TableColumn<FeatureDTO, String> nameColumn;
    @FXML
    private TableColumn<FeatureDTO, String> descriptionColumn;
    @FXML
    private TableColumn<FeatureDTO, String> vendorColumn;
    @FXML
    private TableColumn<FeatureDTO, String> licenseColumn;
    @Inject
    private UpdateAgent                     updateAgent;
    @Inject
    private IWorkbench                      workbench;
    @Inject
    private ThreadSynchronize               threadSync;
    private ObservableList<FeatureDTO>      features;

    @FXML
    public void initialize() {
        final Collection<FeatureDTO> installedFeatures = updateAgent.getInstalledFeatures();
        features = FXCollections.observableArrayList(installedFeatures);
        featuresList.setItems(features);
        logger.atInfo().log("FXML controller has been initialized");

        idColumn.setCellValueFactory(
                p -> new SimpleStringProperty(p.getValue().id.groupId + ":" + p.getValue().id.artifactId + ":" + p.getValue().id.version));
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        descriptionColumn.setCellValueFactory(new DTOCellValueFactory<>("description", String.class));
        vendorColumn.setCellValueFactory(new DTOCellValueFactory<>("vendor", String.class));
        licenseColumn.setCellValueFactory(new DTOCellValueFactory<>("license", String.class));

        initContextMenu();
    }

    private void initContextMenu() {
        final MenuItem item = new MenuItem("Uninstall");
        item.setOnAction(event -> {
            final FeatureDTO f = featuresList.getSelectionModel().getSelectedItem();
            try {
                final FeatureDTO removedfeature = updateAgent.remove(featureIdAsString(f));
                if (removedfeature != null) {
                    threadSync.asyncExec(() -> {
                        final String header = "Feature Uninstallation";
                        FxDialog.showInfoDialog(header, featureIdAsString(removedfeature) + " has been uninstalled",
                                getClass().getClassLoader());
                        features.clear();
                        features.addAll(updateAgent.getInstalledFeatures());
                        // show information about the restart of the application
                        FxDialog.showInfoDialog(header, "The application must be restarted, therefore, will be shut down right away",
                                getClass().getClassLoader(), btn -> workbench.restart());
                    });
                }
            } catch (final Exception e) {
                threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
            }
        });
        final ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        featuresList.setContextMenu(menu);
    }

    private String featureIdAsString(final FeatureDTO f) {
        return f.id.groupId + ":" + f.id.artifactId + ":" + f.id.version;
    }
}
