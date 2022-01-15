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
package com.osgifx.console.application.fxml.controller;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.HyperlinkLabel;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public final class AboutApplicationDialogController {

    @Log
    @Inject
    private FluentLogger   logger;
    @FXML
    private Text           appName;
    @FXML
    private ImageView      header;
    @FXML
    private GridPane       content;
    @FXML
    private HyperlinkLabel hyperlink;
    @Inject
    private Application    jfxApplication;
    @Inject
    @Named("com.osgifx.console.application")
    private BundleContext  bundleContext;

    @FXML
    public void initialize() {
        appName.setText("OSGi.fx (" + bundleContext.getBundle().getVersion() + ")");
        hyperlink.setOnAction(event -> {
            final Hyperlink link           = (Hyperlink) event.getSource();
            final String    eclipseWebLink = link.getText();
            jfxApplication.getHostServices().showDocument(eclipseWebLink);
        });
        logger.atInfo().log("FXML controller (%s) has been initialized", getClass());
    }

    public Node getHeader() {
        return header;
    }

    public Node getBody() {
        return content;
    }

}
