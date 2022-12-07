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
package com.osgifx.console.ui.dto;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;
import org.osgi.annotation.bundle.Requirement;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osgifx.console.data.provider.DataProvider;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class DtoFxController {

    @Log
    @Inject
    private FluentLogger      logger;
    @FXML
    private Button            expandBtn;
    @FXML
    private Button            collapseBtn;
    @FXML
    private TreeView<String>  dtoTree;
    @FXML
    private TextField         searchBox;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;

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

    public void updateModel() {
        threadSync.asyncExec(this::initTree);
        logger.atInfo().log("DMT data model has been updated");
    }

    private void initTree() {
        final var runtimeDTO = dataProvider.readRuntimeDTO();
        if (runtimeDTO == null) {
            return;
        }
        final var content = new Gson().toJson(runtimeDTO);
        final var json    = JsonParser.parseString(content).getAsJsonObject();

        final var root = parseJSON("ROOT", json);
        dtoTree.setRoot(root);
        addSearchBinding(root);

        expandBtn.setOnMouseClicked(event -> expandOrCollapseTreeView(root, true));
        collapseBtn.setOnMouseClicked(event -> expandOrCollapseTreeView(root, false));
    }

    private void addSearchBinding(final FilterableTreeItem<String> treeItem) {
        treeItem.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            if (searchBox.getText() == null || searchBox.getText().isEmpty()) {
                return null;
            }
            return TreeItemPredicate.create(itemText -> StringUtils.containsIgnoreCase(itemText, searchBox.getText()));
        }, searchBox.textProperty()));
    }

    private static FilterableTreeItem<String> parseJSON(final String name, final Object json) {
        final var item = new FilterableTreeItem<>(name);
        if (json instanceof final JsonObject object) {
            object.entrySet().forEach(entry -> {
                final var    childName = entry.getKey();
                final Object childJson = entry.getValue();
                final var    child     = parseJSON(childName, childJson);
                item.getInternalChildren().add(child);
            });
        } else if (json instanceof final JsonArray array) {
            item.setValue(name);
            for (var i = 0; i < array.size(); i++) {
                final var    childName = String.valueOf(i);
                final Object childJson = array.get(i);
                final var    child     = parseJSON(childName, childJson);
                item.getInternalChildren().add(child);
            }
        } else {
            item.setValue(name + " : " + json);
        }
        return item;
    }

    private void expandOrCollapseTreeView(final TreeItem<?> item, final boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            item.getChildren().forEach(e -> expandOrCollapseTreeView(e, expand));
        }
    }

}
