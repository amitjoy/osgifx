package in.bytehue.osgifx.console.application.fxml.controller;

import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
    private UpdateAgent                     udpateAgent;

    @FXML
    public void initialize() {
        final Collection<FeatureDTO> installedFeatures = udpateAgent.getInstalledFeatures();
        featuresList.setItems(FXCollections.observableArrayList(installedFeatures));
        logger.atInfo().log("FXML controller has been initialized");

        idColumn.setCellValueFactory(
                p -> new SimpleStringProperty(p.getValue().id.groupId + ":" + p.getValue().id.artifactId + ":" + p.getValue().id.version));
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        descriptionColumn.setCellValueFactory(new DTOCellValueFactory<>("description", String.class));
        vendorColumn.setCellValueFactory(new DTOCellValueFactory<>("vendor", String.class));
        licenseColumn.setCellValueFactory(new DTOCellValueFactory<>("license", String.class));
    }
}
