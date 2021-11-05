package in.bytehue.osgifx.console.application.fxml.controller;

import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.controlsfx.dialog.ExceptionDialog;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.application.dialog.InstallFeatureDialog.SelectedFeaturesDTO;
import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.IdDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;

public final class InstallFeatureDialogController {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    private UpdateAgent                updateAgent;
    @FXML
    private TextField                  archiveUrlText;
    @FXML
    private Button                     analyzeButton;
    @FXML
    private Button                     localArchiveButton;
    @FXML
    private CheckListView<XFeatureDTO> featuresList;
    @FXML
    private GridPane                   featureInstallPane;
    private File                       localArchive;

    @FXML
    public void initialize() {
        registerDragAndDropSupport();
        featuresList.getSelectionModel().setSelectionMode(MULTIPLE);
        featuresList.setCellFactory(param -> new CheckBoxListCell<XFeatureDTO>(featuresList::getItemBooleanProperty) {
            @Override
            public void updateItem(final XFeatureDTO feature, final boolean empty) {
                super.updateItem(feature, empty);

                if (empty || feature == null) {
                    setText(null);
                } else if (!feature.isPlaceholder) {
                    setText(formatId(feature.dto));
                } else {
                    setText(XFeatureDTO.PLACEHOLDER_TEXT);
                }
            }

            private String formatId(final FeatureDTO feature) {
                final IdDTO         id     = feature.id;
                final String        name   = feature.name;
                final StringBuilder cellId = new StringBuilder().append(id.groupId).append(":").append(id.artifactId).append(":")
                        .append(id.version);
                if (name != null) {
                    cellId.append(" ( ").append(name).append(" )");
                }
                return cellId.toString();
            }

        });
        analyzeButton.disableProperty()
                .bind(Bindings.createBooleanBinding(() -> archiveUrlText.getText().trim().isEmpty(), archiveUrlText.textProperty()));
        logger.atInfo().log("FXML controller has been initialized");
    }

    @FXML
    private void processArchive(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'processArchive(..)' event has been invoked");
        final String          url       = archiveUrlText.getText();
        Map<File, FeatureDTO> features;
        final URL             parsedURL = parseAsURL(url);
        try {
            if (parsedURL != null) {
                logger.atInfo().log("Web URL found - '%s'", url);
                logger.atInfo().log("Reading features from - '%s'", url);

                features = updateAgent.readFeatures(parsedURL);
            } else {
                logger.atInfo().log("Local archive found - '%s'", url);
                logger.atInfo().log("Reading features from - '%s'", url);

                localArchive = new File(url);
                features     = updateAgent.readFeatures(localArchive);
            }
            if (features.isEmpty()) {
                featuresList.setItems(FXCollections.observableArrayList(new XFeatureDTO(null, null, true)));
                featuresList.setDisable(true);
                return;
            }
            updateList(features);
        } catch (final Exception e) {
            logger.atError().withException(e).log("Cannot process archive '%s'", url);
            final ExceptionDialog dialog = new ExceptionDialog(e);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
            dialog.show();
        }
        // TODO store the repo details in storage
    }

    @FXML
    private void chooseArchive(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'chooseArchive(..)' event has been invoked");
        final FileChooser archiveChooser = new FileChooser();
        archiveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archive Files", "*.zip"));
        localArchive = archiveChooser.showOpenDialog(null);
        if (localArchive != null) {
            archiveUrlText.setText(localArchive.getAbsolutePath());
        }
    }

    public SelectedFeaturesDTO getSelectedFeatures() {
        final SelectedFeaturesDTO dto = new SelectedFeaturesDTO();

        final ObservableList<XFeatureDTO> selectedItems = featuresList.getCheckModel().getCheckedItems();
        dto.features   = selectedItems.stream().map(f -> f.json).collect(toList());
        dto.archiveURL = archiveUrlText.getText();

        return dto;
    }

    private void registerDragAndDropSupport() {
        featureInstallPane.setOnDragOver(event -> {
            final Dragboard db         = event.getDragboard();
            final boolean   isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".zip");
            if (db.hasFiles() && isAccepted) {
                featureInstallPane.setStyle("-fx-background-color: #C6C6C6");
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        featureInstallPane.setOnDragDropped(event -> {
            final Dragboard db      = event.getDragboard();
            boolean         success = false;
            if (db.hasFiles()) {
                success = true;
                // Only get the first file from the list
                final File file = db.getFiles().get(0);
                threadSync.asyncExec(() -> {
                    archiveUrlText.setText(file.getName());
                    localArchive = file;
                    archiveUrlText.setTooltip(new Tooltip(localArchive.getName()));
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });
        logger.atInfo().log("Registered drag and drop support");
    }

    private void updateList(final Map<File, FeatureDTO> features) {
        // @formatter:off
        final Collection<XFeatureDTO> fs = features.entrySet()
                                                   .stream()
                                                   .map(e -> new XFeatureDTO(e.getKey(), e.getValue(), false))
                                                   .collect(toList());
        // @formatter:on
        featuresList.setItems(FXCollections.observableArrayList(fs));
    }

    private URL parseAsURL(final String url) {
        try {
            return new URL(url);
        } catch (final Exception e) {
            return null;
        }
    }

    private static class XFeatureDTO {

        static final String PLACEHOLDER_TEXT = "No features found";

        FeatureDTO dto;
        File       json;
        boolean    isPlaceholder;

        public XFeatureDTO(final File json, final FeatureDTO dto, final boolean isPlaceholder) {
            this.dto           = dto;
            this.json          = json;
            this.isPlaceholder = isPlaceholder;
        }

    }
}
