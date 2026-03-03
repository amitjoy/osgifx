/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.bundles;

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_INSTALLED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_BUNDLE_FILTER_EVENT_TOPIC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Resource;

import com.google.common.io.Files;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.batchinstall.dialog.ArtifactInstaller;
import com.osgifx.console.ui.batchinstall.dialog.BatchInstallDialog;
import com.osgifx.console.ui.bundles.dialog.BundleInstallDialog;
import com.osgifx.console.ui.bundles.dialog.BatchInstallIntroDialog;
import com.osgifx.console.ui.bundles.obr.bnd.ResourceBuilder;
import com.osgifx.console.ui.bundles.obr.bnd.XMLResourceGenerator;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import com.osgifx.console.util.io.IO;

import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

public final class BundlesFxController {

    private static final String BATCH_HEADER = "Installing artifacts from directory";

    @Log
    @Inject
    private FluentLogger          logger;
    @Inject
    @LocalInstance
    private FXMLLoader            loader;
    @FXML
    private TableView<XBundleDTO> table;
    @Inject
    @OSGiBundle
    private BundleContext         context;
    @Inject
    @Named("is_connected")
    private boolean               isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean               isSnapshotAgent;
    @Inject
    private DataProvider          dataProvider;
    @Inject
    private IEclipseContext       eclipseContext;
    @Inject
    private Executor              executor;
    @Inject
    @Optional
    private Supervisor            supervisor;
    @Inject
    private ThreadSynchronize     threadSync;
    @Inject
    private IEventBroker          eventBroker;
    @Inject
    @Optional
    private ArtifactInstaller     installer;
    @FXML
    private Button                installBundleButton;
    @FXML
    private Button                batchInstallButton;
    @FXML
    private Button                generateObrButton;

    private FilteredList<XBundleDTO>         filteredList;
    private TableRowDataFeatures<XBundleDTO> previouslyExpanded;

    @FXML
    public void initialize() {
        initButtonIcons();
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            updateButtonStates();
            return;
        }
        try {
            createControls();
            Fx.disableSelectionModel(table);
            updateButtonStates();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    @FXML
    public void installOrUpdateBundle() {
        final var dialog = new BundleInstallDialog();

        ContextInjectionFactory.inject(dialog, eclipseContext);
        logger.atInfo().log("Injected install bundle dialog to eclipse context");
        dialog.init();

        final var remoteInstall = dialog.showAndWait();
        if (remoteInstall.isPresent()) {
            final Task<Void> installTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto        = remoteInstall.get();
                        final var file       = dto.file();
                        final var startLevel = dto.startLevel();
                        if (file == null) {
                            return null;
                        }
                        logger.atInfo().log("Selected file to install or update as bundle: %s", file);

                        final var agent  = supervisor.getAgent();
                        final var bundle = agent.installWithData(null, Files.toByteArray(file), startLevel);
                        if (bundle == null) {
                            logger.atError().log("Bundle cannot be installed or updated");
                            return null;
                        }
                        logger.atInfo().log("Bundle has been installed or updated: %s", bundle);
                        if (dto.startBundle()) {
                            agent.start(bundle.id);
                            logger.atInfo().log("Bundle has been started: %s", bundle);
                        }
                        eventBroker.post(BUNDLE_INSTALLED_EVENT_TOPIC, bundle.symbolicName);
                        threadSync.asyncExec(() -> Fx.showSuccessNotification("Remote Bundle Install",
                                bundle.symbolicName + " successfully installed/updated"));
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Bundle cannot be installed or updated");
                        threadSync.asyncExec(() -> Fx.showErrorNotification("Remote Bundle Install",
                                "Bundle cannot be installed/updated"));
                    }
                    return null;
                }
            };
            executor.runAsync(installTask);
        }
    }

    @FXML
    public void batchInstall() {
        final var introDialog = new BatchInstallIntroDialog();
        ContextInjectionFactory.inject(introDialog, eclipseContext);
        logger.atDebug().log("Injected batch install intro dialog to eclipse context");
        introDialog.init();

        final var introResult = introDialog.showAndWait();
        if (introResult.isEmpty()) {
            return;
        }

        final var directory = introResult.get();

        if (directory != null) {
            final var dialog = new BatchInstallDialog();

            ContextInjectionFactory.inject(dialog, eclipseContext);
            logger.atDebug().log("Injected batch install dialog to eclipse context");
            dialog.init(directory);

            dialog.traverseDirectoryForFiles();
            final var selectedFeatures = dialog.showAndWait();
            if (selectedFeatures.isPresent()) {
                final Task<String> batchTask = new Task<>() {

                    @Override
                    protected String call() throws Exception {
                        return installer.installArtifacts(selectedFeatures.get());
                    }
                };
                batchTask.setOnSucceeded(_ -> {
                    final var result = batchTask.getValue();
                    if (result != null) {
                        if (!result.isEmpty()) {
                            threadSync.asyncExec(() -> {
                                FxDialog.showErrorDialog(BATCH_HEADER, result, getClass().getClassLoader());
                            });
                        } else {
                            eventBroker.post(BUNDLE_INSTALLED_EVENT_TOPIC, "");
                            eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, "");
                        }
                    }
                });
                final var taskFuture = executor.runAsync(batchTask).exceptionally(e -> {
                    threadSync.asyncExec(
                            () -> FxDialog.showExceptionDialog(e, BundlesFxController.class.getClassLoader()));
                    return null;
                });
                FxDialog.showProgressDialog(BATCH_HEADER, batchTask, getClass().getClassLoader(),
                        () -> taskFuture.cancel(true));
            }
        }
    }

    @FXML
    public void generateObr() {
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }
        exportOBR(location);
    }

    private void exportOBR(final File location) {
        final var resources = dataProvider.bundles().stream().map(this::toResource).toList();
        if (resources.isEmpty()) {
            logger.atInfo().log("Resources are empty");
            return;
        }
        final var xmlResourceGenerator = new XMLResourceGenerator();
        final var outputFile           = new File(location, IO.prepareFilenameFor("xml"));

        try (final OutputStream fileStream = new FileOutputStream(outputFile)) {
            xmlResourceGenerator.resources(resources);
            xmlResourceGenerator.save(fileStream);
            Fx.showSuccessNotification("OBR Successfully Generated", outputFile.getAbsolutePath());
            logger.atInfo().log("OBR XML has been successfully generated - '%s'", outputFile);
        } catch (final Exception e) {
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
            logger.atError().withException(e).log("OBR XML cannot be generated");
        }
    }

    private Resource toResource(final XBundleDTO bundle) {
        try {
            final var builder = new ResourceBuilder();
            builder.addCapabilities(bundle.bundleRevision.capabilities);
            builder.addRequirements(bundle.bundleRevision.requirements);
            return builder.build();
        } catch (final Exception e) {
            throw org.eclipse.fx.core.ExceptionUtils.wrap(e);
        }
    }

    private void initButtonIcons() {
        installBundleButton.setGraphic(createIcon("/graphic/icons/install.png"));
        batchInstallButton.setGraphic(createIcon("/graphic/icons/directory.png"));
        generateObrButton.setGraphic(createIcon("/graphic/icons/obr.png"));
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        installBundleButton.setDisable(disableActions);
        batchInstallButton.setDisable(disableActions);
        generateObrButton.setDisable(disableActions);
    }

    private ImageView createIcon(final String path) {
        final var image     = new Image(getClass().getResourceAsStream(path));
        final var imageView = new ImageView(image);
        imageView.setFitHeight(16.0);
        imageView.setFitWidth(16.0);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void createControls() {
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (BundleDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XBundleDTO>(current -> {
                                     if (previouslyExpanded != null
                                             && current.getValue() == previouslyExpanded.getValue()) {
                                         return expandedNode;
                                     }
                                     if (previouslyExpanded != null && previouslyExpanded.isExpanded()) {
                                         previouslyExpanded.toggleExpanded();
                                     }
                                     controller.initControls(current.getValue());
                                     previouslyExpanded = current;
                                     return expandedNode;
                                 });
        expanderColumn.setPrefWidth(48);
        expanderColumn.setMaxWidth(48);
        expanderColumn.setMinWidth(48);

        final var idColumn = new TableColumn<XBundleDTO, Integer>("ID");

        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", Integer.class));

        final var symbolicNameColumn = new TableColumn<XBundleDTO, String>("Symbolic Name");

        symbolicNameColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        Fx.addCellFactory(symbolicNameColumn, b -> b.isFragment, Color.SLATEBLUE, Color.BLACK);

        final var versionColumn = new TableColumn<XBundleDTO, String>("Version");

        versionColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));

        final var statusColumn = new TableColumn<XBundleDTO, String>("State");

        statusColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(idColumn);
        table.getColumns().add(symbolicNameColumn);
        table.getColumns().add(versionColumn);
        table.getColumns().add(statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredList = new FilteredList<>(dataProvider.bundles());
        threadSync.asyncExec(() -> {
            table.setItems(filteredList);
            TableFilter.forTableView(table).lazy(true).apply();
            table.getSortOrder().add(symbolicNameColumn);
            table.sort();
        });
    }

    @Inject
    @Optional
    @SuppressWarnings("unchecked")
    public void onFilterUpdateEvent(@UIEventTopic(UPDATE_BUNDLE_FILTER_EVENT_TOPIC) final SearchFilterDTO filter) {
        logger.atInfo().log("Update filter event received");
        filteredList.setPredicate((Predicate<XBundleDTO>) filter.predicate);
    }

}
