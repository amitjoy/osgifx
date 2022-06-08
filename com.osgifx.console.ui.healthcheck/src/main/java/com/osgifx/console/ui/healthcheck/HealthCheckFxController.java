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
package com.osgifx.console.ui.healthcheck;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_HEALTHCHECKS_TOPIC;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.SegmentedButton;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO.ResultDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class HealthCheckFxController {

	@Log
	@Inject
	private FluentLogger          logger;
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
	private TextArea              hcResultArea;
	@Inject
	private DataProvider          dataProvider;
	@Inject
	private Supervisor            supervisor;
	@Inject
	private ThreadSynchronize     threadSync;
	private MaskerPane            progressPane;
	private ExecutorService       executor;
	private Future<?>             hcExecFuture;

	@FXML
	public void initialize() {
		try {
			initHcList();
			executor     = Executors.newSingleThreadExecutor(r -> new Thread(r, "hc-executor"));
			progressPane = new MaskerPane();
			logger.atDebug().log("FXML controller has been initialized");
			hcTypeButton.getStyleClass().add(STYLE_CLASS_DARK);
			logger.atDebug().log("FXML controller has been initialized");
			initButtons();
		} catch (final Exception e) {
			logger.atError().withException(e).log("FXML controller could not be initialized");
		}
	}

	private void initButtons() {
		nameHcButton.setOnMouseClicked(e -> initNames());
		tagHcButton.setOnMouseClicked(e -> initTags());
	}

	private void initNames() {
		final var metadata               = initMetadata(false);
		final var filteredHcMetadataList = initSearchFilter(metadata);
		hcMetadataList.setItems(filteredHcMetadataList.sorted(Comparator.comparing(Functions.identity())));
	}

	private void initTags() {
		final var metadata               = initMetadata(true);
		final var filteredHcMetadataList = initSearchFilter(metadata);
		hcMetadataList.setItems(filteredHcMetadataList.sorted(Comparator.comparing(Functions.identity())));
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

	@PreDestroy
	public void destroy() {
		executor.shutdownNow();
	}

	private void initHcList() {
		hcMetadataList.getSelectionModel().setSelectionMode(MULTIPLE);
		hcMetadataList.setCellFactory(param -> new CheckBoxListCell<>(hcMetadataList::getItemBooleanProperty) {
			@Override
			public void updateItem(final String hcMetadata, final boolean empty) {
				threadSync.syncExec(() -> super.updateItem(hcMetadata, empty));
				if (empty || hcMetadata == null) {
					threadSync.syncExec(() -> setText(null));
				} else {
					threadSync.syncExec(() -> setText(hcMetadata));
				}
			}
		});
		initNames(); // the first time load only the names
		logger.atInfo().log("Heathcheck metadata list has been initialized");
	}

	private FilteredList<String> initSearchFilter(final ObservableList<String> metadata) {
		final var filteredMetadataList = new FilteredList<>(metadata, s -> true);
		searchText.textProperty().addListener(obs -> {
			final var filter = searchText.getText();
			if (filter == null || filter.length() == 0) {
				filteredMetadataList.setPredicate(s -> true);
			} else {
				filteredMetadataList.setPredicate(s -> Stream.of(filter.split("\\|")).anyMatch(s::contains));
			}
		});
		return filteredMetadataList;
	}

	@FXML
	private void executeHc(final ActionEvent event) {
		logger.atInfo().log("Executing health checks");
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
					final var hcResults = agent.executeHealthChecks(null, selectedMetadata);
					addToOutputArea(hcResults);
				} else {
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
				hcResultArea.clear();
				hcResults.stream().map(this::formatResult).forEach(hcResultArea::appendText);
			}

			private String formatResult(final XHealthCheckResultDTO result) {
				final var output = new StringBuilder();

				final var genericInfoTable = new Table();

				genericInfoTable.setShowVerticalLines(true);
				genericInfoTable.setHeaders("Generic HC Info", "");

				genericInfoTable.addRow("Name", result.healthCheckName != null ? result.healthCheckName : "<NO NAME>");
				genericInfoTable.addRow("Tags", result.healthCheckTags != null ? result.healthCheckTags.toString() : "<NO TAGS>");
				genericInfoTable.addRow("Elapsed Time", String.valueOf(result.elapsedTime) + " ms");
				genericInfoTable.addRow("Finished At", new Date(result.finishedAt).toString());
				genericInfoTable.addRow("Timeout", String.valueOf(result.isTimedOut));

				output.append(genericInfoTable.print());

				var i = 0;
				for (final ResultDTO entry : result.results) {
					final var resultEntriesTable = new Table();

					i++;
					resultEntriesTable.setShowVerticalLines(true);
					resultEntriesTable.setHeaders("HC Result Entry: " + i, "");

					resultEntriesTable.addRow("Status", Strings.nullToEmpty(entry.status));
					resultEntriesTable.addRow("Message", Strings.nullToEmpty(entry.message));
					resultEntriesTable.addRow("Log Level", Strings.nullToEmpty(entry.logLevel));
					resultEntriesTable.addRow("Exception", Strings.nullToEmpty(entry.exception));

					output.append(resultEntriesTable.print());
				}
				return output.toString();
			}

		};
		if (hcExecFuture != null) {
			hcExecFuture.cancel(true);
		}
		hcExecFuture = executor.submit(task);
	}

	@FXML
	private void deselectAll(final ActionEvent event) {
		hcMetadataList.getCheckModel().clearChecks();
	}

	@Inject
	@Optional
	private void onUnderlyingDataUpdate(@EventTopic(DATA_RETRIEVED_HEALTHCHECKS_TOPIC) final String data) {
		if (nameHcButton.isSelected()) {
			initNames();
		} else {
			tagHcButton.setSelected(true);
			initTags();
		}
	}

}
