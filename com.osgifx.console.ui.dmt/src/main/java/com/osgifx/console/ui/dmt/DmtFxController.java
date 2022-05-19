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
package com.osgifx.console.ui.dmt;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.data.provider.DataProvider;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class DmtFxController {

	private static final String ROOT_DMT_NODE = ".";

	@Log
	@Inject
	private FluentLogger     logger;
	@FXML
	private TreeView<String> dmtTree;
	@FXML
	private TextField        searchBox;
	@Inject
	@Named("is_connected")
	private boolean          isConnected;
	@Inject
	private DataProvider     dataProvider;

	@FXML
	public void initialize() {
		if (!isConnected) {
			return;
		}
		try {
			initTree();
			logger.atDebug().log("FXML controller has been initialized");
		} catch (final Exception e) {
			logger.atError().withException(e).log("FXML controller could not be initialized");
		}
	}

	private void initTree() {
		final var dmtNode = dataProvider.readDmtNode(ROOT_DMT_NODE);
		if (dmtNode == null) {
			return;
		}
		final var rootItem = new FilterableTreeItem<>(dmtNode.uri);

		addSearchBinding(rootItem);
		dmtTree.setRoot(rootItem);
		dmtTree.setShowRoot(false);
		initTree(dmtNode, rootItem);
		expandTreeView(rootItem);
	}

	private void addSearchBinding(final FilterableTreeItem<String> treeItem) {
		treeItem.predicateProperty().bind(Bindings.createObjectBinding(() -> {
			if (searchBox.getText() == null || searchBox.getText().isEmpty()) {
				return null;
			}
			return TreeItemPredicate.create(itemText -> StringUtils.containsIgnoreCase(itemText, searchBox.getText()));
		}, searchBox.textProperty()));
	}

	private void initTree(final XDmtNodeDTO dmtNode, final FilterableTreeItem<String> parent) {
		var node = parent;
		if (!ROOT_DMT_NODE.equals(dmtNode.uri)) {
			node = new FilterableTreeItem<>(initItemText(dmtNode));
			parent.getInternalChildren().add(node);
		}
		if (!dmtNode.children.isEmpty()) {
			for (final XDmtNodeDTO child : dmtNode.children) {
				initTree(child, node);
			}
		}
	}

	private String initItemText(final XDmtNodeDTO node) {
		final Map<String, String> properties = Maps.newHashMap();

		properties.computeIfAbsent("value", e -> node.value);
		properties.computeIfAbsent("format", e -> node.format);

		final var propertiesToString = Joiner.on(", ").withKeyValueSeparator(": ").join(properties);
		final var result             = new StringBuilder(node.uri);

		if (!propertiesToString.isEmpty()) {
			result.append(" [");
			result.append(propertiesToString);
			result.append("]");
		}
		return result.toString();
	}

	private void expandTreeView(final TreeItem<?> item) {
		if (item != null && !item.isLeaf()) {
			item.setExpanded(true);
			item.getChildren().forEach(this::expandTreeView);
		}
	}

}
