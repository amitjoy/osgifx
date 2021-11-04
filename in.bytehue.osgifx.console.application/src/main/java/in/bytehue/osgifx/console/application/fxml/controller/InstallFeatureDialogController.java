package in.bytehue.osgifx.console.application.fxml.controller;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.IdDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

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
        featuresList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        featuresList.setCellFactory(param -> new ListCell<XFeatureDTO>() {
            @Override
            protected void updateItem(final XFeatureDTO feature, final boolean empty) {
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

    @PreDestroy
    public void deinitialize() {
        if (localArchive != null) {
            logger.atInfo().log("Deleting cached local feature archive '%s'", localArchive.getAbsoluteFile());
            localArchive.delete();
            localArchive = null;
        }
    }

    @FXML
    private void processArchive(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'processArchive(..)' event has been invoked");
        final String url = archiveUrlText.getText();
        if (isWebURL(url)) {
            logger.atInfo().log("Web URL found - '%s'", url);
            localArchive = downloadArchive(url);
        } else {
            logger.atInfo().log("Local archive found - '%s'", url);
            localArchive = new File(url);
        }
        logger.atInfo().log("Reading features from - '%s'", localArchive);
        final Map<String, FeatureDTO> features = updateAgent.readFeatures(localArchive);
        if (features.isEmpty()) {
            featuresList.setItems(FXCollections.observableArrayList(new XFeatureDTO(null, null, true)));
            featuresList.setDisable(true);
            return;
        }
        updateList(features);
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

    public List<File> getSelectedFeatures() {
        return featuresList.getSelectionModel().getSelectedItems().stream().map(f -> f.json).collect(toList());
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

    private void updateList(final Map<String, FeatureDTO> features) {
        // @formatter:off
        final Collection<XFeatureDTO> fs = features.entrySet()
                                                   .stream()
                                                   .map(e -> new XFeatureDTO(
                                                                   new File(localArchive, e.getKey()), e.getValue(), false))
                                                   .collect(toList());
        // @formatter:on
        featuresList.setItems(FXCollections.observableArrayList(fs));
    }

    private File downloadArchive(final String url) {
        logger.atInfo().log("Downloading archive from URL '%s'", url);

        final String tempDir    = System.getProperty("java.io.tmpdir");
        final String outputPath = tempDir + "/" + "archive.zip";

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            final FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (final MalformedURLException e) {
            logger.atError().withException(e).log("Invalid URL - '%s'", url);
        } catch (final IOException e) {
            logger.atError().withException(e).log("Download failed from '%s' to '%s'", url, outputPath);
        }
        logger.atInfo().log("Downloaded archive from URL '%s'", url);
        return new File(outputPath);
    }

    private boolean isWebURL(final String url) {
        try {
            new URL(url);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private static class XFeatureDTO {

        static final String PLACEHOLDER_TEXT = "No features found";

        boolean    isPlaceholder;
        File       json;
        FeatureDTO dto;

        public XFeatureDTO(final File json, final FeatureDTO dto, final boolean isPlaceholder) {
            this.dto           = dto;
            this.json          = json;
            this.isPlaceholder = isPlaceholder;
        }

    }
}
