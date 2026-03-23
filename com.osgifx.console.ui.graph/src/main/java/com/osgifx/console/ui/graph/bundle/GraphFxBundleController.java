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
package com.osgifx.console.ui.graph.bundle;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_BUNDLES_TOPIC;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CAPABILITIES_TOPIC;
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
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
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

public final class GraphFxBundleController implements GraphController {

    @Log
    @Inject
    private FluentLogger                               logger;
    @Inject
    private Executor                                   executor;
    @FXML
    private TextField                                  searchText;
    @FXML
    private ListView<BundleItem>                       bundlesList;
    @FXML
    private ChoiceBox<String>                          wiringSelection;
    @FXML
    private ChoiceBox<String>                          layoutSelection;
    @FXML
    private CheckBox                                   transitiveView;
    @FXML
    private CheckBox                                   showSelectedOnlyView;
    @FXML
    private BorderPane                                 graphPane;
    @Inject
    private DataProvider                               dataProvider;
    @Inject
    @Named("is_connected")
    private boolean                                    isConnected;
    @Inject
    private ThreadSynchronize                          threadSync;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                                    isSnapshotAgent;
    @Inject
    private RuntimeBundleGraph                         runtimeGraph;
    private MaskerPane                                 progressPane;
    private WebGraphView                               graphView;
    private Future<?>                                  graphGenFuture;
    private Graph<BundleVertex, ? extends DefaultEdge> currentGraph;
    private ObservableList<BundleItem>                 masterBundleList;

    @FXML
    public void initialize() {
        try {
            if (!isConnected) {
                graphPane.setCenter(Fx.createDisconnectedPlaceholder());
                bundlesList.setPlaceholder(new Label("Agent not connected"));
                searchText.setDisable(true);
                bundlesList.setDisable(true);
                wiringSelection.setDisable(true);
                layoutSelection.setDisable(true);
                transitiveView.setDisable(true);
                showSelectedOnlyView.setDisable(true);
                return;
            }
            searchText.setDisable(false);
            bundlesList.setDisable(false);
            bundlesList.setPlaceholder(null);
            graphPane.setCenter(null);
            wiringSelection.setDisable(false);
            layoutSelection.setDisable(false);
            transitiveView.setDisable(false);
            showSelectedOnlyView.setDisable(false);

            addExportToDotContextMenu();
            initBundlesList();
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
        threadSync.asyncExec(this::initBundlesList);
        logger.atInfo().log("Graph bundle data model has been updated");
    }

    @Override
    public Type type() {
        return Type.BUNDLES;
    }

    private void initWiringSelection() {
        wiringSelection.getItems().addAll("Find all bundles that are required by", "Find all bundles that require",
                "Find all bundles that use services from", "Find all bundles that provide services to");
        wiringSelection.getSelectionModel().select(0);
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
        final var exporter = new DOTExporter<BundleVertex, DefaultEdge>(BundleVertex::toDotID);
        exporter.setEdgeAttributeProvider(e -> {
            final Map<String, Attribute> attributes = new HashMap<>();
            if (e instanceof GraphEdge) {
                attributes.put("label", DefaultAttribute.createAttribute(((GraphEdge) e).getLabel()));
            }
            return attributes;
        });
        try {
            final var dotFile = new File(location, generateDotFileName("Bundles"));
            // Raw cast for exporter compatibility (GraphEdge extends DefaultEdge)
            exporter.exportGraph((Graph) currentGraph, dotFile);
        } catch (final ExportException e) {
            logger.atError().withException(e).log("Cannot export the graph to '%s'", location);
            throw e;
        }
    }

    private void initBundlesList() {
        final List<XBundleDTO> bundles = threadSync.syncExec(() -> Lists.newArrayList(dataProvider.bundles()),
                new ArrayList<XBundleDTO>());
        if (masterBundleList == null) {
            masterBundleList = FXCollections.observableArrayList(bundles.stream().map(BundleItem::new).toList());
            bundlesList.getSelectionModel().setSelectionMode(MULTIPLE);
            bundlesList.setCellFactory(CheckBoxListCell.forListView(BundleItem::selectedProperty));

            final var filteredBundlesList = initSearchFilter(masterBundleList);
            bundlesList.setItems(filteredBundlesList.sorted(Comparator.comparing(b -> b.getBundle().symbolicName)));
        } else {
            masterBundleList.setAll(bundles.stream().map(BundleItem::new).toList());
        }
        logger.atInfo().log("Bundles list has been initialized");
    }

    private FilteredList<BundleItem> initSearchFilter(final ObservableList<BundleItem> bundles) {
        final var filteredBundlesList = new FilteredList<>(bundles, Predicates.alwaysTrue());
        updateFilteredList(filteredBundlesList);
        searchText.textProperty().addListener(_ -> {
            updateFilteredList(filteredBundlesList);
            searchText.requestFocus();
        });
        showSelectedOnlyView.selectedProperty().addListener((_, _, _) -> updateFilteredList(filteredBundlesList));
        return filteredBundlesList;
    }

    private void updateFilteredList(final FilteredList<BundleItem> filteredBundlesList) {
        final var                   filter           = searchText.getText();
        final var                   showSelectedOnly = showSelectedOnlyView.isSelected();
        final Predicate<BundleItem> predicate        = item -> {
                                                         final var isSelected = item.isSelected();
                                                         if (showSelectedOnly && !isSelected) {
                                                             return false;
                                                         }
                                                         if (filter == null || filter.isBlank()) {
                                                             return true;
                                                         }
                                                         return Stream.of(filter.split("\\|")).anyMatch(e -> Strings.CI
                                                                 .contains(item.getBundle().symbolicName, e));
                                                     };
        filteredBundlesList.setPredicate(predicate);
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for bundles");
        final var selection    = wiringSelection.getSelectionModel().getSelectedIndex();
        final var isTransitive = transitiveView.isSelected();

        final var selectedBundles = Lists.newArrayList(
                masterBundleList.stream().filter(BundleItem::isSelected).map(BundleItem::getBundle).toList());

        if (selectedBundles.isEmpty()) {
            logger.atInfo().log("No bundle has been selected. Skipped graph generation.");
            return;
        }
        selectedBundles.removeIf(Predicates.isNull());
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                switch (selection) {
                    case 0 -> {
                        logger.atInfo().log("Generating all graph paths for bundles that are required by '%s'",
                                selectedBundles);
                        currentGraph = runtimeGraph.getAllBundlesThatAreRequiredBy(selectedBundles, isTransitive);
                    }
                    case 1 -> {
                        logger.atInfo().log("Generating all graph paths for bundles that require '%s'",
                                selectedBundles);
                        currentGraph = runtimeGraph.getAllBundlesThatRequire(selectedBundles, isTransitive);
                    }
                    case 2 -> {
                        logger.atInfo().log("Generating all graph paths for bundles that use services from '%s'",
                                selectedBundles);
                        currentGraph = runtimeGraph.getAllBundlesThatUseServicesFrom(selectedBundles, isTransitive);
                    }
                    case 3 -> {
                        logger.atInfo().log("Generating all graph paths for bundles that provide services to '%s'",
                                selectedBundles);
                        currentGraph = runtimeGraph.getAllBundlesThatProvideServicesTo(selectedBundles, isTransitive);
                    }
                    default -> {
                    }
                }
                return null;
            }

            @Override
            @SuppressWarnings({ "rawtypes", "unchecked" })
            protected void succeeded() {
                final var selectedIds = selectedBundles.stream()
                        .map(b -> BundleVertex.DOT_ID_FUNCTION.apply(b.symbolicName, b.id)).toList();

                final var json = GraphJsonConverter.toJson((Graph) currentGraph, BundleVertex::toDotID,
                        BundleVertex::symbolicName, v -> selectedIds.contains(v.toDotID()));

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
        if (masterBundleList != null) {
            masterBundleList.forEach(b -> b.setSelected(false));
        }
    }

    @FXML
    private void selectAll(final ActionEvent event) {
        if (masterBundleList != null) {
            masterBundleList.forEach(b -> b.setSelected(true));
        }
    }

    @Inject
    @Optional
    private void updateOnCapabilitiesRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_CAPABILITIES_TOPIC) final String data) {
        if (bundlesList == null) {
            return;
        }
        threadSync.asyncExec(this::initialize);
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_BUNDLES_TOPIC) final String data) {
        threadSync.asyncExec(this::initBundlesList);
    }

}
