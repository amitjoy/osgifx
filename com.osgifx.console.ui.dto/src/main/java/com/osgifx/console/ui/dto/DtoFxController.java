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
package com.osgifx.console.ui.dto;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;

public final class DtoFxController {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private Executor          executor;
    @FXML
    private Button            searchBtn;
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
        final var promise = dataProvider.readRuntimeDTO();
        if (promise == null) {
            return;
        }
        promise.thenAccept(dto -> {
            final var content = new Gson().toJson(dto);
            final var json    = JsonParser.parseString(content).getAsJsonObject();

            threadSync.asyncExec(() -> {
                final var root = parseJSON("ROOT", json);
                root.setExpanded(true);
                dtoTree.setRoot(root);

                searchBox.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        performSearch(root);
                    }
                });
                searchBtn.setOnMouseClicked(_ -> performSearch(root));
            });
        });
    }

    private void performSearch(final FilterableTreeItem<String> rootItem) {
        final Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                threadSync.asyncExec(() -> {
                    final var itemText = searchBox.getText();
                    if (StringUtils.isBlank(itemText)) {
                        rootItem.setPredicate(null);
                    } else {
                        rootItem.setPredicate(
                                TreeItemPredicate.create(item -> Strings.CI.contains(item, itemText.strip())));
                    }
                });
                return null;
            }
        };
        executor.runAsync(task);
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
                final var childName = String.valueOf(i);
                final var childJson = array.get(i);
                final var child     = parseJSON(childName, childJson);

                item.getInternalChildren().add(child);
            }
        } else {
            item.setValue(name + " : " + json);
        }
        return item;
    }

}
