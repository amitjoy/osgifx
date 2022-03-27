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

import javax.inject.Inject;

import org.controlsfx.control.HyperlinkLabel;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;

public final class AboutApplicationDialogController {

	@Log
	@Inject
	private FluentLogger   logger;
	@FXML
	private HyperlinkLabel appLink;
	@FXML
	private HyperlinkLabel eclipseLink;
	@FXML
	private Text           appVersion;
	@Inject
	private Application    jfxApplication;
	@Inject
	@OSGiBundle
	private BundleContext  bundleContext;

	@FXML
	public void initialize() {
		appVersion.setText(bundleContext.getBundle().getVersion().toString());
		appLink.setOnAction(this::handleLinkOnClick);
		eclipseLink.setOnAction(this::handleLinkOnClick);
		logger.atInfo().log("FXML controller (%s) has been initialized", getClass());
	}

	private void handleLinkOnClick(final ActionEvent event) {
		final var link           = (Hyperlink) event.getSource();
		final var eclipseWebLink = link.getText();
		jfxApplication.getHostServices().showDocument(eclipseWebLink);
	}

}
