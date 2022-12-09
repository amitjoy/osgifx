/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.SegmentedButton;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.osgifx.console.smartgraph.graphview.SmartGraphPanel;
import com.osgifx.console.smartgraph.graphview.SmartPlacementStrategy;
import com.osgifx.console.smartgraph.graphview.SmartRandomPlacementStrategy;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

public final class GraphFxBundleController implements GraphController {

    @Log
    @Inject
    private FluentLogger              logger;
    @Inject
    private Executor                  executor;
    @FXML
    private SegmentedButton           strategyButton;
    @FXML
    private TextField                 searchText;
    @FXML
    private ToggleButton              circularStrategyButton;
    @FXML
    private ToggleButton              randomStrategyButton;
    @FXML
    private CheckListView<XBundleDTO> bundlesList;
    @FXML
    private ChoiceBox<String>         wiringSelection;
    @FXML
    private BorderPane                graphPane;
    @Inject
    private DataProvider              dataProvider;
    @Inject
    private ThreadSynchronize         threadSync;
    @Inject
    private RuntimeBundleGraph        runtimeGraph;
    private MaskerPane                progressPane;
    private FxBundleGraph             fxGraph;
    private Future<?>                 graphGenFuture;

    @FXML
    public void initialize() {
        try {
            addExportToDotContextMenu();
            initBundlesList();
            progressPane = new MaskerPane();
            initStrategyButton();
            initWiringSelection();
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

    private void initStrategyButton() {
        strategyButton.getStyleClass().add(STYLE_CLASS_DARK);
        strategyButton.getToggleGroup().selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }

    private void initWiringSelection() {
        wiringSelection.getItems().addAll("Find all bundles that are required by", "Find all bundles that require");
        wiringSelection.getSelectionModel().select(0);
    }

    private void addExportToDotContextMenu() {
        final var item = new MenuItem("Export to DOT");
        item.setOnAction(event -> {
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
        menu.getItems().add(item);
        graphPane.setOnContextMenuRequested(e -> menu.show(graphPane.getCenter(), e.getScreenX(), e.getScreenY()));
    }

    private void exportToDOT(final File location) {
        final var exporter = new DOTExporter<BundleVertex, DefaultEdge>(BundleVertex::toDotID);
        try {
            final Graph<BundleVertex, DefaultEdge> jGraph  = GraphHelper.toJGraphT(fxGraph.graph);
            final var                              dotFile = new File(location, generateDotFileName("Bundles"));
            exporter.exportGraph(jGraph, dotFile);
        } catch (final ExportException e) {
            logger.atError().withException(e).log("Cannot export the graph to '%s'", location);
            throw e;
        }
    }

    private void initBundlesList() {
        bundlesList.getSelectionModel().setSelectionMode(MULTIPLE);
        bundlesList.setCellFactory(param -> new CheckBoxListCell<>(bundlesList::getItemBooleanProperty) {
            @Override
            public void updateItem(final XBundleDTO bundle, final boolean empty) {
                threadSync.syncExec(() -> super.updateItem(bundle, empty));
                if (empty || bundle == null) {
                    threadSync.syncExec(() -> setText(null));
                } else {
                    threadSync.syncExec(() -> setText(bundle.symbolicName));
                }
            }
        });
        final var bundles             = dataProvider.bundles();
        final var filteredBundlesList = initSearchFilter(bundles);
        bundlesList.setItems(filteredBundlesList.sorted(Comparator.comparing(b -> b.symbolicName)));
        logger.atInfo().log("Bundles list has been initialized");
    }

    private FilteredList<XBundleDTO> initSearchFilter(final ObservableList<XBundleDTO> bundles) {
        final var filteredBundlesList = new FilteredList<>(bundles, s -> true);
        searchText.textProperty().addListener(obs -> {
            final var filter = searchText.getText();
            if (filter == null || filter.isBlank()) {
                filteredBundlesList.setPredicate(s -> true);
            } else {
                filteredBundlesList.setPredicate(s -> Stream.of(filter.split("\\|"))
                        .anyMatch(e -> StringUtils.containsIgnoreCase(s.symbolicName, e)));
            }
        });
        return filteredBundlesList;
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for bundles");
        final var selectedBundles = bundlesList.getCheckModel().getCheckedItems();
        if (selectedBundles.isEmpty()) {
            logger.atInfo().log("No bundles has been selected. Skipped graph generation.");
            return;
        }
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);
                final var selection = wiringSelection.getSelectionModel().getSelectedIndex();

                final Collection<GraphPath<BundleVertex, DefaultEdge>> dependencies;
                if (selection == 0) {
                    logger.atInfo().log("Generating all graph paths for bundles that are required by '%s'",
                            selectedBundles);
                    dependencies = runtimeGraph.getAllBundlesThatAreRequiredBy(selectedBundles);
                } else {
                    logger.atInfo().log("Generating all graph paths for bundles that require '%s'", selectedBundles);
                    dependencies = runtimeGraph.getAllBundlesThatRequire(selectedBundles);
                }
                fxGraph = new FxBundleGraph(dependencies);
                return null;
            }

            @Override
            protected void succeeded() {
                final var graphView = new SmartGraphPanel<>(fxGraph.graph, getStrategy());
                graphView.setPrefSize(800, 520);
                progressPane.setVisible(false);
                graphPane.setCenter(graphView);
                graphView.init();
            }
        };
        graphPane.setCenter(progressPane);
        if (graphGenFuture != null) {
            graphGenFuture.cancel(true);
        }
        graphGenFuture = executor.runAsync(task);
    }

    @FXML
    private void deselectAll(final ActionEvent event) {
        bundlesList.getCheckModel().clearChecks();
    }

    private SmartPlacementStrategy getStrategy() {
        if (randomStrategyButton.isSelected()) {
            return new SmartRandomPlacementStrategy();
        }
        return new SmartCircularSortedPlacementStrategy();
    }

}
