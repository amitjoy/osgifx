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
package com.osgifx.console.ui.graph.component;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CAPABILITIES_TOPIC;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_COMPONENTS_TOPIC;
import static com.osgifx.console.ui.graph.GraphHelper.generateDotFileName;
import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.Future;
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
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.graph.GraphController;
import com.osgifx.console.ui.graph.GraphJsonConverter;
import com.osgifx.console.ui.graph.WebGraphView;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.value.ChangeListener;
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

public final class GraphFxComponentController implements GraphController {

    @Log
    @Inject
    private FluentLogger                        logger;
    @Inject
    private Executor                            executor;
    @FXML
    private TextField                           searchText;
    @FXML
    private ListView<ComponentItem>             componentsList;
    @FXML
    private ChoiceBox<String>                   wiringSelection;
    @FXML
    private ChoiceBox<String>                   layoutSelection;
    @FXML
    private CheckBox                            transitiveView;
    @FXML
    private CheckBox                            showSelectedOnlyView;
    @FXML
    private BorderPane                          graphPane;
    @Inject
    private DataProvider                        dataProvider;
    @Inject
    @Named("is_connected")
    private boolean                             isConnected;
    @Inject
    private ThreadSynchronize                   threadSync;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                             isSnapshotAgent;
    @Inject
    private RuntimeComponentGraph               runtimeGraph;
    private MaskerPane                          progressPane;
    private WebGraphView                        graphView;
    private Future<?>                           graphGenFuture;
    private Graph<ComponentVertex, DefaultEdge> currentGraph;
    private ObservableList<ComponentItem>       masterComponentList;

    @FXML
    public void initialize() {
        try {
            if (!isConnected) {
                graphPane.setCenter(Fx.createDisconnectedPlaceholder());
                searchText.setDisable(true);
                componentsList.setPlaceholder(new Label("Agent not connected"));
                componentsList.setDisable(true);
                wiringSelection.setDisable(true);
                layoutSelection.setDisable(true);
                transitiveView.setDisable(true);
                showSelectedOnlyView.setDisable(true);
                return;
            }
            if (!isCapabilityAvailable("SCR")) {
                graphPane.setCenter(Fx.createFeatureUnavailablePlaceholder("Declarative Services"));
                searchText.setDisable(true);
                componentsList.setDisable(true);
                wiringSelection.setDisable(true);
                layoutSelection.setDisable(true);
                transitiveView.setDisable(true);
                showSelectedOnlyView.setDisable(true);
                return;
            }
            searchText.setDisable(false);
            componentsList.setDisable(false);
            componentsList.setPlaceholder(null);
            graphPane.setCenter(null);
            wiringSelection.setDisable(false);
            layoutSelection.setDisable(false);
            transitiveView.setDisable(false);
            showSelectedOnlyView.setDisable(false);

            addExportToDotContextMenu();
            initComponentsList();
            progressPane = new MaskerPane();
            initWiringSelection();
            initLayoutSelection();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private boolean isCapabilityAvailable(final String capabilityId) {
        return dataProvider.runtimeCapabilities().stream().anyMatch(c -> capabilityId.equals(c.id) && c.isAvailable);
    }

    @Override
    public void updateModel() {
        threadSync.asyncExec(this::initComponentsList);
        logger.atInfo().log("Graph component data model has been updated");
    }

    @Override
    public Type type() {
        return Type.COMPONENTS;
    }

    private void initWiringSelection() {
        wiringSelection.getItems().addAll("Find all components that are required by", "Find all component cycles");
        wiringSelection.getSelectionModel().select(0);
        wiringSelection.getSelectionModel().selectedIndexProperty()
                .addListener((ChangeListener<Number>) (_, _, newValue) -> {
                    final var condition = newValue.intValue() == 1;

                    searchText.setDisable(condition);
                    componentsList.setDisable(condition);
                    if (masterComponentList != null) {
                        masterComponentList.forEach(c -> c.setSelected(false));
                    }
                    transitiveView.setDisable(condition);
                    showSelectedOnlyView.setDisable(condition);
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

    private void exportToDOT(final File location) {
        if (currentGraph == null) {
            return;
        }
        final var exporter = new DOTExporter<ComponentVertex, DefaultEdge>(ComponentVertex::toDotID);
        try {
            final var dotFile = new File(location, generateDotFileName("Components"));
            exporter.exportGraph(currentGraph, dotFile);
        } catch (final ExportException e) {
            logger.atError().withException(e).log("Cannot export the graph to '%s'", location);
            throw e;
        }
    }

    private void initComponentsList() {
        final var components = dataProvider.components();
        if (masterComponentList == null) {
            masterComponentList = FXCollections
                    .observableArrayList(components.stream().map(ComponentItem::new).toList());
            componentsList.getSelectionModel().setSelectionMode(MULTIPLE);
            componentsList.setCellFactory(CheckBoxListCell.forListView(ComponentItem::selectedProperty));

            final var filteredComponentsList = initSearchFilter(masterComponentList);
            componentsList.setItems(filteredComponentsList.sorted(Comparator.comparing(b -> b.getComponent().name)));
        } else {
            masterComponentList.setAll(components.stream().map(ComponentItem::new).toList());
        }
        logger.atInfo().log("Components list has been initialized");
    }

    private FilteredList<ComponentItem> initSearchFilter(final ObservableList<ComponentItem> components) {
        final var filteredComponentsList = new FilteredList<>(components, Predicates.alwaysTrue());
        updateFilteredList(filteredComponentsList);
        searchText.textProperty().addListener(_ -> {
            updateFilteredList(filteredComponentsList);
            searchText.requestFocus();
        });
        showSelectedOnlyView.selectedProperty().addListener((_, _, _) -> updateFilteredList(filteredComponentsList));
        return filteredComponentsList;
    }

    private void updateFilteredList(final FilteredList<ComponentItem> filteredComponentsList) {
        final var filter           = searchText.getText();
        final var showSelectedOnly = showSelectedOnlyView.isSelected();
        final var predicate        = new java.util.function.Predicate<ComponentItem>() {
                                       @Override
                                       public boolean test(final ComponentItem item) {
                                           final var isSelected = item.isSelected();
                                           if (showSelectedOnly && !isSelected) {
                                               return false;
                                           }
                                           if (filter == null || filter.isBlank()) {
                                               return true;
                                           }
                                           return Stream.of(filter.split("\\|"))
                                                   .anyMatch(e -> Strings.CI.contains(item.getComponent().name, e));
                                       }
                                   };
        filteredComponentsList.setPredicate(predicate);
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for components");
        final var selection    = wiringSelection.getSelectionModel().getSelectedIndex();
        final var isTransitive = transitiveView.isSelected();

        final var selectedComponents = Lists.newArrayList(masterComponentList.stream().filter(ComponentItem::isSelected)
                .map(ComponentItem::getComponent).toList());

        if (selectedComponents.isEmpty() && selection == 0) {
            logger.atInfo().log("No component has been selected. Skipped graph generation.");
            return;
        }
        selectedComponents.removeIf(Predicates.isNull());
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {

                if (selection == 0) {
                    logger.atDebug().log("Generating all graph paths for service components that are required by '%s'",
                            selectedComponents);
                    currentGraph = runtimeGraph.getAllServiceComponentsThatAreRequiredBy(selectedComponents,
                            isTransitive);
                } else {
                    logger.atDebug().log("Generating service component cycles");
                    currentGraph = runtimeGraph.getAllCycles();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                final var selectedIds = selectedComponents.stream()
                        .map(c -> ComponentVertex.DOT_ID_FUNCTION.apply(c.name)).toList();

                final var json = GraphJsonConverter.toJson(currentGraph, ComponentVertex::toDotID,
                        ComponentVertex::name, v -> selectedIds.contains(v.toDotID()));

                graphView = new WebGraphView();
                progressPane.setVisible(false);
                graphPane.setCenter(graphView);
                graphView.loadGraph(json);

                // Apply selected layout
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
        if (masterComponentList != null) {
            masterComponentList.forEach(c -> c.setSelected(false));
        }
    }

    @FXML
    private void selectAll(final ActionEvent event) {
        if (masterComponentList != null) {
            masterComponentList.forEach(c -> c.setSelected(true));
        }
    }

    @Inject
    @Optional
    private void updateOnCapabilitiesRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_CAPABILITIES_TOPIC) final String data) {
        threadSync.asyncExec(this::initialize);
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_COMPONENTS_TOPIC) final String data) {
        threadSync.asyncExec(this::initComponentsList);
    }

}
