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
package com.osgifx.console.ui.graph.rsa;

import static com.osgifx.console.ui.graph.GraphHelper.generateDotFileName;
import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.Strings;
import org.controlsfx.control.MaskerPane;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.RemoteServiceDirection;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.graph.AbbreviationSettings;
import com.osgifx.console.ui.graph.AbbreviationSettingsDialog;
import com.osgifx.console.ui.graph.GraphController;
import com.osgifx.console.ui.graph.GraphEdge;
import com.osgifx.console.ui.graph.GraphJsonConverter;
import com.osgifx.console.ui.graph.WebGraphView;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

public final class GraphFxRsaController implements GraphController {

    @Log
    @Inject
    private FluentLogger             logger;
    @Inject
    private Executor                 executor;
    @FXML
    private TextField                searchText;
    @FXML
    private ListView<RsaServiceItem> servicesList;
    @FXML
    private ChoiceBox<String>        directionFilter;
    @FXML
    private ChoiceBox<String>        layoutSelection;
    @FXML
    private CheckBox                 showSelectedOnlyView;
    @FXML
    private BorderPane               graphPane;
    @Inject
    private DataProvider             dataProvider;
    @Inject
    @Named("is_connected")
    private boolean                  isConnected;
    @Inject
    private ThreadSynchronize        threadSync;

    private MaskerPane                     progressPane;
    private WebGraphView                   graphView;
    private Future<?>                      graphGenFuture;
    private Graph<RsaVertex, GraphEdge>    currentGraph;
    private ObservableList<RsaServiceItem> masterServiceList;
    private AbbreviationSettings           abbreviationSettings;

    @FXML
    public void initialize() {
        try {
            if (!isConnected) {
                graphPane.setCenter(Fx.createDisconnectedPlaceholder());
                servicesList.setPlaceholder(new Label("Agent not connected"));
                searchText.setDisable(true);
                servicesList.setDisable(true);
                directionFilter.setDisable(true);
                layoutSelection.setDisable(true);
                showSelectedOnlyView.setDisable(true);
                return;
            }
            searchText.setDisable(false);
            servicesList.setDisable(false);
            servicesList.setPlaceholder(null);
            graphPane.setCenter(null);
            directionFilter.setDisable(false);
            layoutSelection.setDisable(false);
            showSelectedOnlyView.setDisable(false);

            abbreviationSettings = new AbbreviationSettings();
            addExportToDotContextMenu();
            initServicesList();
            progressPane = new MaskerPane();
            initDirectionFilter();
            initLayoutSelection();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    @Override
    public void updateModel() {
        threadSync.asyncExec(this::initServicesList);
        logger.atInfo().log("Graph RSA data model has been updated");
    }

    @Override
    public Type type() {
        return Type.RSA;
    }

    private void initDirectionFilter() {
        directionFilter.getItems().addAll("All Endpoints", "Imports Only", "Exports Only");
        directionFilter.getSelectionModel().select(0);
        directionFilter.getSelectionModel().selectedIndexProperty().addListener((_, _, _) -> {
            if (masterServiceList != null) {
                initServicesList();
            }
        });
    }

    private void initLayoutSelection() {
        layoutSelection.getItems().addAll("Dagre (Hierarchical)", "Breadthfirst", "Cose (Force-Directed)", "Circle");
        layoutSelection.getSelectionModel().select(0);
        layoutSelection.getSelectionModel().selectedIndexProperty().addListener((_, _, newValue) -> {
            if (graphView != null) {
                final var layoutName = getLayoutName(newValue.intValue());
                graphView.setLayout(layoutName);
            }
        });
    }

    private String getLayoutName(final int index) {
        return switch (index) {
            case 0 -> "dagre";
            case 1 -> "breadthfirst";
            case 2 -> "cose";
            case 3 -> "circle";
            default -> "dagre";
        };
    }

    private void addExportToDotContextMenu() {
        final var exportDot = new MenuItem("Export to DOT");
        exportDot.setOnAction(_ -> {
            final var directoryChooser = new DirectoryChooser();
            final var location         = directoryChooser.showDialog(null);
            if (location == null) {
                return;
            }
            exportToDOT(location);
            threadSync.asyncExec(
                    () -> Fx.showSuccessNotification("DOT (GraphViz) Export", "Graph has been successfully exported"));
        });
        final var menu = new ContextMenu();
        menu.getItems().add(exportDot);
        graphPane.setOnContextMenuRequested(e -> menu.show(graphPane.getCenter(), e.getScreenX(), e.getScreenY()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void exportToDOT(final File location) {
        if (currentGraph == null) {
            return;
        }
        final var exporter = new DOTExporter<RsaVertex, GraphEdge>(RsaVertex::toDotID);
        exporter.setEdgeAttributeProvider(e -> {
            final Map<String, Attribute> attributes = new HashMap<>();
            attributes.put("label", DefaultAttribute.createAttribute(((GraphEdge) e).getLabel()));
            return attributes;
        });
        try {
            final var dotFile = new File(location, generateDotFileName("RSA"));
            exporter.exportGraph((Graph) currentGraph, dotFile);
        } catch (final ExportException e) {
            logger.atError().withException(e).log("Cannot export the graph to '%s'", location);
            throw e;
        }
    }

    private void initServicesList() {
        final List<XRemoteServiceDTO> allServices = threadSync
                .syncExec(() -> Lists.newArrayList(dataProvider.remoteServices()), new ArrayList<>());

        // Apply direction filter
        final var                     filterIndex      = directionFilter.getSelectionModel().getSelectedIndex();
        final List<XRemoteServiceDTO> filteredServices = switch (filterIndex) {
                                                           case 1 -> allServices.stream().filter(
                                                                   s -> s.direction == RemoteServiceDirection.IMPORT)
                                                                   .toList();
                                                           case 2 -> allServices.stream().filter(
                                                                   s -> s.direction == RemoteServiceDirection.EXPORT)
                                                                   .toList();
                                                           default -> allServices;
                                                       };

        if (masterServiceList == null) {
            masterServiceList = FXCollections
                    .observableArrayList(filteredServices.stream().map(RsaServiceItem::new).toList());
            servicesList.getSelectionModel().setSelectionMode(MULTIPLE);
            servicesList.setCellFactory(CheckBoxListCell.forListView(RsaServiceItem::selectedProperty));

            final var filteredServicesList = initSearchFilter(masterServiceList);
            servicesList.setItems(filteredServicesList.sorted(Comparator.comparing(RsaServiceItem::toString)));
        } else {
            masterServiceList.setAll(filteredServices.stream().map(RsaServiceItem::new).toList());
        }
        logger.atInfo().log("Remote services list has been initialized");
    }

    private FilteredList<RsaServiceItem> initSearchFilter(final ObservableList<RsaServiceItem> services) {
        final var filteredServicesList = new FilteredList<>(services, Predicates.alwaysTrue());
        updateFilteredList(filteredServicesList);
        searchText.textProperty().addListener(_ -> {
            updateFilteredList(filteredServicesList);
            searchText.requestFocus();
        });
        showSelectedOnlyView.selectedProperty().addListener((_, _, _) -> updateFilteredList(filteredServicesList));
        return filteredServicesList;
    }

    private void updateFilteredList(final FilteredList<RsaServiceItem> filteredServicesList) {
        final var                       filter           = searchText.getText();
        final var                       showSelectedOnly = showSelectedOnlyView.isSelected();
        final Predicate<RsaServiceItem> predicate        = item -> {
                                                             final var isSelected = item.isSelected();
                                                             if (showSelectedOnly && !isSelected) {
                                                                 return false;
                                                             }
                                                             if (filter == null || filter.isBlank()) {
                                                                 return true;
                                                             }
                                                             final var service       = item.getRemoteService();
                                                             final var searchTargets = Stream
                                                                     .of(service.id, service.frameworkUUID,
                                                                             service.objectClass != null
                                                                                     ? String.join(",",
                                                                                             service.objectClass)
                                                                                     : "",
                                                                             service.direction != null
                                                                                     ? service.direction.name()
                                                                                     : "")
                                                                     .filter(s -> s != null && !s.isBlank());
                                                             return Stream.of(filter.split("\\|"))
                                                                     .anyMatch(f -> searchTargets.anyMatch(
                                                                             target -> Strings.CI.contains(target, f)));
                                                         };
        filteredServicesList.setPredicate(predicate);
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for RSA");

        final var selectedServices = Lists.newArrayList(masterServiceList.stream().filter(RsaServiceItem::isSelected)
                .map(RsaServiceItem::getRemoteService).toList());

        if (selectedServices.isEmpty()) {
            logger.atInfo().log("No remote service has been selected. Skipped graph generation.");
            return;
        }

        final Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                currentGraph = RsaGraphGenerator.generateRsaGraph(selectedServices, "Local Framework");
                return null;
            }

            @Override
            @SuppressWarnings({ "rawtypes", "unchecked" })
            protected void succeeded() {
                // Select all vertices by default for RSA (simple topology view)
                final var json = GraphJsonConverter.toJson((Graph) currentGraph, RsaVertex::toDotID,
                        v -> abbreviationSettings.applyAbbreviations(v.toString()), _ -> true);

                graphView = new WebGraphView();
                progressPane.setVisible(false);
                graphPane.setCenter(graphView);

                graphView.loadGraph(json);

                final var layoutIndex = layoutSelection.getSelectionModel().getSelectedIndex();
                graphView.setLayout(getLayoutName(layoutIndex));
            }

            @Override
            protected void failed() {
                logger.atError().withException(getException()).log("Graph generation failed");
                progressPane.setVisible(false);
            }
        };
        progressPane.setVisible(true);
        graphPane.setCenter(progressPane);
        if (graphGenFuture != null) {
            graphGenFuture.cancel(true);
        }
        graphGenFuture = executor.runAsync(task);
    }

    @FXML
    private void zoomIn(final ActionEvent event) {
        if (graphView != null) {
            graphView.zoomIn();
        }
    }

    @FXML
    private void zoomOut(final ActionEvent event) {
        if (graphView != null) {
            graphView.zoomOut();
        }
    }

    @FXML
    private void fitToScreen(final ActionEvent event) {
        if (graphView != null) {
            graphView.fitToScreen();
        }
    }

    @FXML
    private void deselectAll(final ActionEvent event) {
        if (masterServiceList != null) {
            masterServiceList.forEach(s -> s.setSelected(false));
        }
    }

    @FXML
    private void selectAll(final ActionEvent event) {
        if (masterServiceList != null) {
            masterServiceList.forEach(s -> s.setSelected(true));
        }
    }

    @FXML
    private void openAbbreviationSettings(final ActionEvent event) {
        final var dialog = new AbbreviationSettingsDialog(abbreviationSettings.getRules());
        final var result = dialog.showAndWait();
        result.ifPresent(rules -> {
            abbreviationSettings.setRules(rules);
            if (currentGraph != null && graphView != null) {
                regenerateGraphView();
            }
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void regenerateGraphView() {
        // Select all vertices for RSA re-generation as it is a global view of selected services
        final var json = GraphJsonConverter.toJson((Graph) currentGraph, RsaVertex::toDotID,
                v -> abbreviationSettings.applyAbbreviations(v.toString()), _ -> true);

        graphView = new WebGraphView();
        graphPane.setCenter(graphView);
        graphView.loadGraph(json);

        final var layoutIndex = layoutSelection.getSelectionModel().getSelectedIndex();
        graphView.setLayout(getLayoutName(layoutIndex));
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic("osgi/fx/remoteServices/retrieved") final String data) {
        threadSync.asyncExec(this::initServicesList);
    }
}
