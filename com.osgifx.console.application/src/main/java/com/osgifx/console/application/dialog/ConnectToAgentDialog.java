/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Maps;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class ConnectToAgentDialog extends Dialog<ButtonType> {

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
        final DialogPane dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.setPrefHeight(170);
        dialogPane.setPrefWidth(400);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Connect to Remote Agent");
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/connected.png").toString()));

        final ButtonType addConnectionButton    = new ButtonType("Add", ButtonBar.ButtonData.LEFT);
        final ButtonType removeConnectionButton = new ButtonType("Remove", ButtonBar.ButtonData.LEFT);

        dialogPane.getButtonTypes().addAll(ButtonType.OK);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);
        dialogPane.getButtonTypes().addAll(addConnectionButton);
        dialogPane.getButtonTypes().addAll(removeConnectionButton);

        buttonTypes.put(ActionType.ADD_CONNECTION, addConnectionButton);
        buttonTypes.put(ActionType.REMOVE_CONNECTION, removeConnectionButton);
        buttonTypes.put(ActionType.CONNECT, ButtonType.OK);

        final Node content = Fx.loadFXML(loader, context, "/fxml/connection-chooser-window.fxml");
        dialogPane.setContent(content);
    }

    public ButtonType getButtonType(final ActionType type) {
        return buttonTypes.get(type);
    }

}
