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

import static com.osgifx.console.event.topics.DmtActionEventTopics.DMT_UPDATED_EVENT_TOPIC;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

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
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @FXML
    private TreeView<String>  dmtTree;
    @FXML
    private TextField         searchBox;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    private Supervisor        supervisor;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;

    private final Map<FilterableTreeItem<String>, XDmtNodeDTO> items = Maps.newHashMap();

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
        threadSync.asyncExec(() -> initTree());
        logger.atInfo().log("DMT data model has been updated");
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
            addDoubleClickEvent();
            items.put(node, dmtNode);
            parent.getInternalChildren().add(node);
        }
        if (!dmtNode.children.isEmpty()) {
            for (final XDmtNodeDTO child : dmtNode.children) {
                initTree(child, node);
            }
        }
    }

    private void addDoubleClickEvent() {
        if (!isSnapshotAgent) {
            dmtTree.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2) {
                    final var item = dmtTree.getSelectionModel().getSelectedItem();
                    final var node = items.get(item);
                    if (node != null && !node.children.isEmpty()) {
                        return;
                    }
                    showDialog(node);
                }
            });
        }
    }

    private void showDialog(final XDmtNodeDTO node) {
        if (node.format == null) {
            logger.atInfo().log("DMT node update not allowed as the node format is null - '%s'", node.uri);
            return;
        }
        final var updateNodeDialog = new UpdateNodeDialog();
        ContextInjectionFactory.inject(updateNodeDialog, context);
        logger.atInfo().log("Injected update DMT node dialog to eclipse context");

        updateNodeDialog.init(node);

        final var result = updateNodeDialog.showAndWait();
        if (!result.isPresent()) {
            logger.atInfo().log("No button has been selected");
            return;
        }
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atError().log("Remote agent is not connected");
            return;
        }
        final var dto          = result.get();
        final var updateResult = agent.updateDmtNode(dto.uri(), dto.value(), dto.format());
        logger.atInfo().log("DMT node '%s' update request processed", node.uri);
        switch (updateResult.result) {
            case XResultDTO.SUCCESS:
                logger.atInfo().log("DMT node '%s' updated successfully", node.uri);
                threadSync.asyncExec(() -> Fx.showSuccessNotification("DMT Node Update",
                        node.uri + "has been updated successfully updated"));
                eventBroker.send(DMT_UPDATED_EVENT_TOPIC, node.uri);
                logger.atInfo().log("DMT node '%s' updated event sent", node.uri);
                break;
            case XResultDTO.ERROR:
                logger.atInfo().log("DMT node '%s' could not be updated", node.uri);
                threadSync.asyncExec(() -> Fx.showErrorNotification("DMT Node Update", updateResult.response));
                break;
            case XResultDTO.SKIPPED:
                logger.atInfo().log("DMT node '%s' update request has been skipped", node.uri);
                threadSync.asyncExec(() -> Fx.showSuccessNotification("DMT Node Update", updateResult.response));
                break;
            default:
                break;
        }
    }

    private String initItemText(final XDmtNodeDTO node) {
        final Map<String, String> properties = Maps.newHashMap();

        properties.computeIfAbsent("value", e -> node.value);
        properties.computeIfAbsent("format", e -> Optional.ofNullable(node.format).map(DmtDataType::name).orElse(null));

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
