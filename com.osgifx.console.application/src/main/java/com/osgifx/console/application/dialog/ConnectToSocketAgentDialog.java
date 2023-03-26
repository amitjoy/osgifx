/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.application.dialog;

import static com.osgifx.console.application.dialog.ConnectToSocketAgentDialog.ActionType.ADD_CONNECTION;
import static com.osgifx.console.application.dialog.ConnectToSocketAgentDialog.ActionType.CONNECT;
import static com.osgifx.console.application.dialog.ConnectToSocketAgentDialog.ActionType.REMOVE_CONNECTION;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.CANCEL;
import static javafx.scene.control.ButtonType.OK;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Maps;
import com.osgifx.console.application.fxml.controller.ConnectionSettingsDialogController;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class ConnectToSocketAgentDialog extends Dialog<ButtonType> {

    public enum ActionType {
        CONNECT,
        ADD_CONNECTION,
        REMOVE_CONNECTION
    }

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @OSGiBundle
    private BundleContext context;

    private final Map<ActionType, ButtonType> buttonTypes = Maps.newHashMap();

    public void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.setPrefHeight(240);
        dialogPane.setPrefWidth(670);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Connect to Remote Socket Agent");
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/connected.png").toString()));

        final var addConnectionButton    = new ButtonType("Add", ButtonBar.ButtonData.LEFT);
        final var removeConnectionButton = new ButtonType("Remove", ButtonBar.ButtonData.LEFT);

        dialogPane.getButtonTypes().addAll(OK);
        dialogPane.getButtonTypes().addAll(CANCEL);
        dialogPane.getButtonTypes().addAll(addConnectionButton);
        dialogPane.getButtonTypes().addAll(removeConnectionButton);

        buttonTypes.put(ADD_CONNECTION, addConnectionButton);
        buttonTypes.put(REMOVE_CONNECTION, removeConnectionButton);
        buttonTypes.put(CONNECT, OK);

        final var content    = Fx.loadFXML(loader, context, "/fxml/connection-chooser-window.fxml");
        final var controller = (ConnectionSettingsDialogController) loader.getController();

        dialogPane.lookupButton(removeConnectionButton).disableProperty().bind(controller.selectedSettings().isNull());
        dialogPane.lookupButton(OK).disableProperty().bind(controller.selectedSettings().isNull());
        dialogPane.setContent(content);
    }

    public ButtonType getButtonType(final ActionType type) {
        return buttonTypes.get(type);
    }

}
