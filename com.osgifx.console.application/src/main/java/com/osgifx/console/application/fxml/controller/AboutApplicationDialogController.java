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
package com.osgifx.console.application.fxml.controller;

import static org.osgi.framework.Constants.BUNDLE_DOCURL;

import java.util.Map;

import javax.inject.Inject;

import org.controlsfx.control.HyperlinkLabel;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.SystemUtils;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

public final class AboutApplicationDialogController {

    @Log
    @Inject
    private FluentLogger   logger;
    @FXML
    private HyperlinkLabel appDetails;
    @FXML
    private Label          eclipseLink;
    @Inject
    private Application    jfxApplication;
    @FXML
    private Text           javaVersionTxt;
    @Inject
    @OSGiBundle
    private BundleContext  bundleContext;

    @FXML
    public void initialize() {
        appDetails.setText(replace(appDetails.getText()));
        javaVersionTxt.setText(replace(javaVersionTxt.getText()));
        appDetails.setOnAction(this::handleLinkOnClick);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void handleLinkOnClick(final ActionEvent event) {
        final var source = event.getSource();
        if (!(source instanceof final Hyperlink link)) {
            return;
        }
        final var eclipseWebLink = link.getText();
        jfxApplication.getHostServices().showDocument(eclipseWebLink);
    }

    public String replace(final String input) {
        final var headers       = bundleContext.getBundle().getHeaders();
        final var appVersion    = headers.get("OSGifx-Version");
        final var appLink       = headers.get(BUNDLE_DOCURL);
        final var javaVersion   = Runtime.version().toString();
        final var javafxVersion = String.valueOf(SystemUtils.getMajorFXVersion());

        final Map<String, String> substitutors = Map.of("appVersion", appVersion, "appLink", appLink, "javaVersion",
                javaVersion, "javafxVersion", javafxVersion);

        return substitutors.entrySet().stream().reduce(input, (s, e) -> s.replace("(" + e.getKey() + ")", e.getValue()),
                (s, s2) -> s);
    }

}
