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
package com.osgifx.console.ui.feature.dialog;

import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.io.File;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PrefixSelectionComboBox;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.preferences.Preference;
import org.eclipse.fx.core.preferences.Value;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.ui.feature.dialog.FeatureInstallDialog.SelectedFeaturesDTO;
import com.osgifx.console.update.FeatureAgent;
import com.osgifx.console.util.fx.FxDialog;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public final class InstallFeatureDialogController {

	@Log
	@Inject
	private FluentLogger                    logger;
	@Inject
	private ThreadSynchronize               threadSync;
	@Inject
	private FeatureAgent                    featureAgent;
	@Inject
	@Preference(nodePath = "osgi.fx.feature", key = "repos", defaultValue = "")
	private Value<String>                   preference;
	@FXML
	private PrefixSelectionComboBox<String> archiveUrlCombo;
	@FXML
	private Button                          analyzeButton;
	@FXML
	private Button                          localArchiveButton;
	@FXML
	private CheckListView<XFeatureDTO>      featuresList;
	@FXML
	private GridPane                        featureInstallPane;
	private File                            localArchive;
	private ProgressDialog                  progressDialog;

	@FXML
	public void initialize() {
		archiveUrlCombo.setEditable(true);
		initCombo();
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
				final var id     = feature.id;
				final var name   = feature.name;
				final var cellId = new StringBuilder().append(id.groupId).append(":").append(id.artifactId).append(":").append(id.version);
				if (name != null) {
					cellId.append(" ( ").append(name).append(" )");
				}
				return cellId.toString();
			}

		});
		analyzeButton.disableProperty().bind(Bindings.createBooleanBinding(() -> archiveUrlCombo.getEditor().getText().trim().isEmpty(),
		        archiveUrlCombo.getEditor().textProperty()));
		logger.atDebug().log("FXML controller has been initialized");
	}

	private void initCombo() {
		final String repos = preference.getValue();
		if (!repos.isEmpty()) {
			final var          gson         = new Gson();
			final List<String> repositories = gson.fromJson(repos, new TypeToken<ArrayList<String>>() {
											}.getType());
			archiveUrlCombo.setItems(FXCollections.observableList(repositories));
		}
	}

	@FXML
	private void processArchive(final ActionEvent event) {
		logger.atInfo().log("FXML controller 'processArchive(..)' event has been invoked");
		final var                   url       = archiveUrlCombo.getEditor().getText();
		final Map<File, FeatureDTO> features  = Maps.newHashMap();
		final var                   parsedURL = parseAsURL(url);
		try {
			if (parsedURL != null) {
				logger.atInfo().log("Web URL found - '%s'", url);
				logger.atInfo().log("Reading features from - '%s'", url);

				final Task<Void> task = new Task<>() {

					@Override
					protected Void call() throws Exception {
						try {
							features.putAll(featureAgent.readFeatures(parsedURL));
							return null;
						} catch (final InterruptedException e) {
							logger.atInfo().log("Feature retrieval task interrupted");
							threadSync.asyncExec(progressDialog::close);
							throw e;
						} catch (final Exception e) {
							logger.atError().withException(e).log("Cannot read features from archive - '%s'", parsedURL);
							threadSync.asyncExec(() -> {
								progressDialog.close();
								FxDialog.showExceptionDialog(e, getClass().getClassLoader());
							});
							throw e;
						}
					}

					@Override
					protected void succeeded() {
						threadSync.asyncExec(progressDialog::close);
					}
				};

				final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(task);
				progressDialog = FxDialog.showProgressDialog("External Feature Download", task, getClass().getClassLoader(),
				        () -> taskFuture.cancel(true));
			} else {
				logger.atInfo().log("Local archive found - '%s'", url);
				logger.atInfo().log("Reading features from - '%s'", url);

				localArchive = new File(url);
				features.putAll(featureAgent.readFeatures(localArchive));
			}
			if (features.isEmpty()) {
				featuresList.setItems(FXCollections.observableArrayList(new XFeatureDTO(null, null, true)));
				featuresList.setDisable(true);
				return;
			}
			updateList(features);
		} catch (final Exception e) {
			logger.atError().withException(e).log("Cannot process archive '%s'", url);
			FxDialog.showExceptionDialog(e, getClass().getClassLoader());
		}
	}

	@FXML
	private void chooseArchive(final ActionEvent event) {
		logger.atInfo().log("FXML controller 'chooseArchive(..)' event has been invoked");
		final var archiveChooser = new FileChooser();
		archiveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archive Files", "*.zip"));
		localArchive = archiveChooser.showOpenDialog(null);
		if (localArchive != null) {
			archiveUrlCombo.getEditor().setText(localArchive.getAbsolutePath());
		}
	}

	public SelectedFeaturesDTO getSelectedFeatures() {
		final var dto = new SelectedFeaturesDTO();

		final var selectedItems = featuresList.getCheckModel().getCheckedItems();
		dto.features   = selectedItems.stream().map(f -> new SimpleEntry<>(f.json, f.dto)).toList();
		dto.archiveURL = archiveUrlCombo.getEditor().getText();

		return dto;
	}

	private void registerDragAndDropSupport() {
		featureInstallPane.setOnDragOver(event -> {
			final var db         = event.getDragboard();
			final var isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".zip");
			if (db.hasFiles() && isAccepted) {
				featureInstallPane.setStyle("-fx-background-color: #C6C6C6");
				event.acceptTransferModes(TransferMode.COPY);
			} else {
				event.consume();
			}
		});
		featureInstallPane.setOnDragDropped(event -> {
			final var db      = event.getDragboard();
			var       success = false;
			if (db.hasFiles()) {
				success = true;
				// Only get the first file from the list
				final var file = db.getFiles().get(0);
				threadSync.asyncExec(() -> {
					archiveUrlCombo.setItems(FXCollections.observableArrayList(file.getAbsolutePath()));
					archiveUrlCombo.getSelectionModel().select(file.getAbsolutePath());
					localArchive = file;
					archiveUrlCombo.setTooltip(new Tooltip(localArchive.getName()));
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
                                                   .toList();
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
