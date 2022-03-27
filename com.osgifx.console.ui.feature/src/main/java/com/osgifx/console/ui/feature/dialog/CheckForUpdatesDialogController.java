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

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.controlsfx.control.CheckTreeView;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.ui.feature.dialog.CheckForUpdatesDialog.SelectedFeaturesForUpdateDTO;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

public final class CheckForUpdatesDialogController {

	private static final String               PLACEHOLDER_ROOT = "<Updated Features>";
	@Log
	@Inject
	private FluentLogger                      logger;
	@FXML
	private BorderPane                        featuresPane;
	private CheckBoxTreeItem<String>          root;
	private CheckTreeView<String>             featuresView;
	private Map<TreeItem<String>, FeatureDTO> features;

	@FXML
	public void initialize() {
		root = new CheckBoxTreeItem<>(PLACEHOLDER_ROOT);
		root.setExpanded(true);

		featuresView = new CheckTreeView<>(root);
		featuresView.getSelectionModel().setSelectionMode(MULTIPLE);

		features = Maps.newHashMap();

		logger.atInfo().log("FXML controller has been initialized");
	}

	public SelectedFeaturesForUpdateDTO getSelectedFeatures() {
		final var dto = new SelectedFeaturesForUpdateDTO();
		dto.features = Lists.newArrayList();
		final var selectedItems = featuresView.getCheckModel().getCheckedItems();
		for (final TreeItem<String> treeItem : selectedItems) {
			final var parent = treeItem.getParent();
			if (parent == null || PLACEHOLDER_ROOT.equals(parent.getValue())) {
				continue;
			}
			final var feature = features.get(treeItem);
			dto.features.add(feature);
		}
		return dto;
	}

	public void setFeaturesToBeUpdated(final Collection<FeatureDTO> tobeUpdatedFeatures) {
		final Map<String, CheckBoxTreeItem<String>> featureRoots = Maps.newHashMap();
		for (final FeatureDTO feature : tobeUpdatedFeatures) {
			final var archiveURL = feature.archiveURL;
			if (featureRoots.containsKey(archiveURL)) {
				continue;
			}
			final var featureRoot = new CheckBoxTreeItem<>(archiveURL);
			featureRoot.setExpanded(true);
			root.getChildren().add(featureRoot);
			featureRoots.put(archiveURL, featureRoot);
		}
		for (final FeatureDTO feature : tobeUpdatedFeatures) {
			final var item = new CheckBoxTreeItem<>(featureAsString(feature));
			features.put(item, feature);
			final var featureRoot = featureRoots.get(feature.archiveURL);
			featureRoot.getChildren().add(item);
		}
		featuresPane.setCenter(featuresView);
	}

	private String featureAsString(final FeatureDTO feature) {
		return feature.id.groupId + ":" + feature.id.artifactId + ":" + feature.id.version;
	}
}
