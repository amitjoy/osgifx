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
package com.osgifx.console.application.ui;

import static java.util.Objects.requireNonNullElse;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.TRANSPARENT;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.StatusBar;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.core.di.annotations.Optional;

import com.osgifx.console.ui.ConsoleStatusBar;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;

public final class ConsoleStatusBarProvider implements ConsoleStatusBar {

	@Inject
	@Optional
	@Named("connected.agent")
	private String          connectedAgent;
	private final StatusBar statusBar = new StatusBar();

	@Override
	public void addTo(final BorderPane pane) {
		final var glyph = new Glyph("FontAwesome", "DESKTOP");
		glyph.useGradientEffect();
		glyph.useHoverEffect();

		final var button = new Button("", glyph);
		button.setBackground(new Background(new BackgroundFill(TRANSPARENT, new CornerRadii(2), new Insets(4))));

		statusBar.getLeftItems().clear();
		statusBar.getLeftItems().add(button);
		statusBar.getLeftItems().add(new Separator(VERTICAL));

		final String statusBarText;
		if (connectedAgent != null) {
			glyph.color(GREEN);
			statusBarText = "Connected to " + connectedAgent;
		} else {
			statusBarText = "Disconnected";
		}
		statusBar.setText(statusBarText);
		pane.setBottom(statusBar);
	}

	@Override
	public DoubleProperty progressProperty() {
		return statusBar.progressProperty();
	}

	@Override
	public void addToLeft(final Node node) {
		requireNonNullElse(node, "Specified node cannot be null");
		statusBar.getLeftItems().add(node);
	}

	@Override
	public void addToRight(final Node node) {
		requireNonNullElse(node, "Specified node cannot be null");
		statusBar.getRightItems().add(node);
	}

	@Override
	public void clearAllInLeft() {
		statusBar.getLeftItems().clear();
	}

	@Override
	public void clearAllInRight() {
		statusBar.getRightItems().clear();
	}

}
