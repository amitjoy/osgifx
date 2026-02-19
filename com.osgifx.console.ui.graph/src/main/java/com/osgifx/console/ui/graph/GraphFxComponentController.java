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
package com.osgifx.console.ui.graph;

import static com.osgifx.console.ui.graph.GraphHelper.generateDotFileName;
import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.Strings;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
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
    private CheckListView<XComponentDTO>        componentsList;
    @FXML
    private ChoiceBox<String>                   wiringSelection;
    @FXML
    private ChoiceBox<String>                   layoutSelection;
    @FXML
    private BorderPane                          graphPane;
    @Inject
    private DataProvider                        dataProvider;
    @Inject
    private ThreadSynchronize                   threadSync;
    @Inject
    private RuntimeComponentGraph               runtimeGraph;
    private MaskerPane                          progressPane;
    private WebGraphView                        graphView;
    private Future<?>                           graphGenFuture;
    private Graph<ComponentVertex, DefaultEdge> currentGraph;

    @FXML
    public void initialize() {
        try {
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
                    componentsList.getCheckModel().clearChecks();
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
        componentsList.getSelectionModel().setSelectionMode(MULTIPLE);
        componentsList.setCellFactory(_ -> new CheckBoxListCell<>(componentsList::getItemBooleanProperty) {
            @Override
            public void updateItem(final XComponentDTO component, final boolean empty) {
                threadSync.asyncExec(() -> super.updateItem(component, empty));
                if (empty || component == null) {
                    threadSync.asyncExec(() -> setText(null));
                } else {
                    threadSync.asyncExec(() -> setText(component.name));
                }
            }
        });
        final var components             = dataProvider.components();
        final var filteredComponentsList = initSearchFilter(components);
        componentsList.setItems(filteredComponentsList.sorted(Comparator.comparing(b -> b.name)));
        logger.atInfo().log("Components list has been initialized");
    }

    private FilteredList<XComponentDTO> initSearchFilter(final ObservableList<XComponentDTO> components) {
        final var filteredComponentsList = new FilteredList<>(components, Predicates.alwaysTrue());
        updateFilteredList(filteredComponentsList);
        searchText.textProperty().addListener(_ -> {
            updateFilteredList(filteredComponentsList);
            searchText.requestFocus();
        });
        return filteredComponentsList;
    }

    private void updateFilteredList(final FilteredList<XComponentDTO> filteredComponentsList) {
        final var filter = searchText.getText();
        if (filter == null || filter.isBlank()) {
            filteredComponentsList.setPredicate(Predicates.alwaysTrue());
        } else {
            filteredComponentsList
                    .setPredicate(s -> Stream.of(filter.split("\\|")).anyMatch(e -> Strings.CI.contains(s.name, e)));
        }
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for components");
        final var selection          = wiringSelection.getSelectionModel().getSelectedIndex();
        final var selectedComponents = Lists.newArrayList(componentsList.getCheckModel().getCheckedItems());
        if (selectedComponents.isEmpty() && selection == 0) {
            logger.atInfo().log("No component has been selected. Skipped graph generation.");
            return;
        }
        selectedComponents.removeIf(Predicates.isNull());
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);

                if (selection == 0) {
                    logger.atDebug().log("Generating all graph paths for service components that are required by '%s'",
                            selectedComponents);
                    final Collection<GraphPath<ComponentVertex, DefaultEdge>> dependencies = runtimeGraph
                            .getAllServiceComponentsThatAreRequiredBy(selectedComponents);
                    currentGraph = mergePathsToGraph(dependencies);
                } else {
                    logger.atDebug().log("Generating service component cycles");
                    currentGraph = runtimeGraph.getAllCycles();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                final var json = GraphJsonConverter.toJson(currentGraph, ComponentVertex::toDotID,
                        ComponentVertex::name);

                graphView = new WebGraphView();
                progressPane.setVisible(false);
                graphPane.setCenter(graphView);
                graphView.loadGraph(json);

                // Highlight initially selected components
                final List<String> selectedIds = selectedComponents.stream()
                        .map(c -> ComponentVertex.DOT_ID_FUNCTION.apply(c.name)).toList();
                graphView.markSelectedNodes(selectedIds);

                // Apply selected layout
                final var layoutIndex = layoutSelection.getSelectionModel().getSelectedIndex();
                graphView.setLayout(getLayoutName(layoutIndex));
            }
        };
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
        componentsList.getCheckModel().clearChecks();
    }

    private static Graph<ComponentVertex, DefaultEdge> mergePathsToGraph(final Collection<GraphPath<ComponentVertex, DefaultEdge>> graphPaths) {
        final Graph<ComponentVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (final GraphPath<ComponentVertex, DefaultEdge> path : graphPaths) {
            for (final DefaultEdge edge : path.getEdgeList()) {
                final var source = path.getGraph().getEdgeSource(edge);
                final var target = path.getGraph().getEdgeTarget(edge);
                graph.addVertex(source);
                graph.addVertex(target);
                if (!graph.containsEdge(source, target)) {
                    graph.addEdge(source, target);
                }
            }
        }
        return graph;
    }

}
