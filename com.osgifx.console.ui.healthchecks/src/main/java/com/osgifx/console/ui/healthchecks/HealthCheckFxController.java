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
package com.osgifx.console.ui.healthchecks;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_HEALTHCHECKS_TOPIC;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.SegmentedButton;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO.ResultDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class HealthCheckFxController {

    @Log
    @Inject
    private FluentLogger          logger;
    @Inject
    private Executor              executor;
    @FXML
    private SegmentedButton       hcTypeButton;
    @FXML
    private TextField             searchText;
    @FXML
    private ToggleButton          nameHcButton;
    @FXML
    private ToggleButton          tagHcButton;
    @FXML
    private CheckListView<String> hcMetadataList;
    @FXML
    private Button                executeHcButton;
    @FXML
    private Button                deselectAllButton;
    @FXML
    private BorderPane            hcResultArea;
    @Inject
    private DataProvider          dataProvider;
    @Inject
    @Optional
    private Supervisor            supervisor;
    @Inject
    private ThreadSynchronize     threadSync;
    @Inject
    @Named("is_snapshot_agent")
    private boolean               isSnapshotAgent;
    private MaskerPane            progressPane;
    private Future<?>             hcExecFuture;

    @FXML
    public void initialize() {
        try {
            initHcList();
            progressPane = new MaskerPane();
            logger.atDebug().log("FXML controller has been initialized");
            initHcTypeButton();
            initButtons();
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private void initHcTypeButton() {
        hcTypeButton.getStyleClass().add(STYLE_CLASS_DARK);
        hcTypeButton.getToggleGroup().selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }

    private void initButtons() {
        nameHcButton.setOnMouseClicked(e -> initNames());
        tagHcButton.setOnMouseClicked(e -> initTags());
        executeHcButton.setDisable(isSnapshotAgent);
    }

    private void initNames() {
        final var metadata               = initMetadata(false);
        final var filteredHcMetadataList = initSearchFilter(metadata);
        hcMetadataList.setItems(filteredHcMetadataList.sorted());
    }

    private void initTags() {
        final var metadata               = initMetadata(true);
        final var filteredHcMetadataList = initSearchFilter(metadata);
        hcMetadataList.setItems(filteredHcMetadataList);
    }

    private ObservableList<String> initMetadata(final boolean isTag) {
        final var                    healthChecks = dataProvider.healthchecks();
        final ObservableList<String> metadata     = FXCollections.observableArrayList();
        for (final XHealthCheckDTO hc : healthChecks) {
            final var name = hc.name;
            final var tags = hc.tags;
            if (isTag) {
                for (final String tag : tags) {
                    if (!metadata.contains(tag)) {
                        metadata.add(tag);
                    }
                }
            } else if (!metadata.contains(name)) {
                metadata.add(name);
            }
        }
        return metadata;
    }

    private void initHcList() {
        hcMetadataList.getSelectionModel().setSelectionMode(MULTIPLE);
        initNames(); // the first time load only the names
        logger.atInfo().log("Heathcheck metadata list has been initialized");
    }

    private FilteredList<String> initSearchFilter(final ObservableList<String> metadata) {
        final var filteredMetadataList = new FilteredList<>(metadata);
        searchText.textProperty().addListener(obs -> {
            final var filter = searchText.getText();
            if (filter == null || filter.isBlank()) {
                filteredMetadataList.setPredicate(s -> true);
            } else {
                filteredMetadataList.setPredicate(
                        s -> Stream.of(filter.split("\\|")).anyMatch(e -> StringUtils.containsIgnoreCase(s, e)));
            }
            searchText.requestFocus();
        });
        return filteredMetadataList;
    }

    @FXML
    private void executeHc(final ActionEvent event) {
        final var selectedMetadata = hcMetadataList.getCheckModel().getCheckedItems();
        if (selectedMetadata.isEmpty()) {
            logger.atInfo().log("No healthcheck metadata has been selected. Skipped execution.");
            return;
        }
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                final var agent = supervisor.getAgent();
                if (agent == null) {
                    logger.atInfo().log("Agent not connected");
                    return null;
                }
                progressPane.setVisible(true);
                final var isName = nameHcButton.isSelected();
                if (isName) {
                    logger.atInfo().log("Executing healthchecks with names: %s", selectedMetadata);
                    final var hcResults = agent.executeHealthChecks(null, selectedMetadata);
                    addToOutputArea(hcResults);
                } else {
                    logger.atInfo().log("Executing healthchecks with tags: %s", selectedMetadata);
                    final var hcResults = agent.executeHealthChecks(selectedMetadata, null);
                    addToOutputArea(hcResults);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressPane.setVisible(false);
            }

            private void addToOutputArea(final List<XHealthCheckResultDTO> hcResults) {
                try {
                    threadSync.asyncExec(() -> {
                        hcResultArea.getChildren().clear();
                        final var content = new Accordion();
                        hcResultArea.setCenter(content);
                        hcResults.stream().map(this::formatResult).forEach(f -> content.getPanes().add(f));
                    });
                } catch (final Exception e) {
                    logger.atError().withException(e).log("The results could not be added to the output area");
                }
            }

            private TitledPane formatResult(final XHealthCheckResultDTO result) {
                final var form = Form
                        .of(Section.of(initGenericFields(result).toArray(new Field[0])).title("Generic Properties"),
                                Section.of(initResultEntryFields(result).toArray(new Field[0])).title("Results: "))
                        .title("Result");

                final var renderer = new FormRenderer(form);

                GridPane.setColumnSpan(renderer, 2);
                GridPane.setRowIndex(renderer, 3);
                GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
                GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

                final var content = new ScrollPane(renderer);
                content.setFitToHeight(true);
                content.setFitToWidth(true);
                content.setHbarPolicy(ScrollBarPolicy.NEVER);

                return new TitledPane(result.healthCheckName, content);
            }

            private List<Field<?>> initGenericFields(final XHealthCheckResultDTO result) {
                final Field<?> elapsedTimeField = Field.ofStringType(String.valueOf(result.elapsedTime) + " ms")
                        .label("Elapsed Time").editable(false);
                final Field<?> timeoutField     = Field.ofBooleanType(result.isTimedOut).label("Timeout")
                        .editable(false);

                return List.of(elapsedTimeField, timeoutField);
            }

            private List<Field<?>> initResultEntryFields(final XHealthCheckResultDTO result) {
                final List<Field<?>> allResultFields = Lists.newArrayList();

                var i = 0;
                for (final ResultDTO entry : result.results) {
                    i++;
                    final Field<?> separatorField = Field.ofStringType("").label("Result " + i).editable(false);
                    final Field<?> statusField    = Field.ofStringType(Strings.nullToEmpty(entry.status))
                            .label("Status").editable(false);
                    final Field<?> messageField   = Field.ofStringType(Strings.nullToEmpty(entry.message))
                            .label("Message").editable(false);
                    final Field<?> logLevelField  = Field.ofStringType(Strings.nullToEmpty(entry.logLevel))
                            .label("Log Level").editable(false);
                    final Field<?> exceptionField = Field.ofStringType(Strings.nullToEmpty(entry.exception))
                            .label("Exception").multiline(true).editable(false);

                    allResultFields
                            .addAll(List.of(separatorField, statusField, messageField, logLevelField, exceptionField));
                }
                return allResultFields;
            }

        };
        if (hcExecFuture != null) {
            hcExecFuture.cancel(true);
        }
        hcExecFuture = executor.runAsync(task);
    }

    @FXML
    private void deselectAll(final ActionEvent event) {
        hcMetadataList.getCheckModel().clearChecks();
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_HEALTHCHECKS_TOPIC) final String data) {
        if (nameHcButton.isSelected()) {
            initNames();
        } else {
            tagHcButton.setSelected(true);
            initTags();
        }
    }

}
