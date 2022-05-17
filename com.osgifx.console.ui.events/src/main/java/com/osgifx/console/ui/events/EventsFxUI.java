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
package com.osgifx.console.ui.events;

import static com.osgifx.console.event.topics.CommonEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.CommonEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.scene.paint.Color.TRANSPARENT;
import static org.controlsfx.control.PopOver.ArrowLocation.BOTTOM_CENTER;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.PopOver;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;

public final class EventsFxUI {

	@Log
	@Inject
	private FluentLogger      logger;
	@Inject
	@OSGiBundle
	private BundleContext     context;
	@Inject
	private ConsoleStatusBar  statusBar;
	@Inject
	private ConsoleMaskerPane progressPane;
	@Inject
	@Named("is_connected")
	private boolean           isConnected;
	@Inject
	@Named("subscribed_topics")
	private Set<String>       subscribedTopics;

	@PostConstruct
	public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
		createControls(parent, loader);
		logger.atDebug().log("Events part has been initialized");
	}

	@Inject
	@Optional
	private void updateOnEventReceiveStarted( //
	        @UIEventTopic(EVENT_RECEIVE_STARTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		createControls(parent, loader);
	}

	@Inject
	@Optional
	private void updateOnEventReceiveStopped( //
	        @UIEventTopic(EVENT_RECEIVE_STOPPED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		createControls(parent, loader);
	}

	@Inject
	@Optional
	private void updateOnAgentConnectedEvent( //
	        @UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		logger.atInfo().log("Agent connected event received");
		createControls(parent, loader);
	}

	@Inject
	@Optional
	private void updateOnAgentDisconnectedEvent( //
	        @UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		logger.atInfo().log("Agent disconnected event received");
		createControls(parent, loader);
	}

	private void createControls(final BorderPane parent, final FXMLLoader loader) {
		progressPane.setVisible(true);
		final Task<Void> task = new Task<>() {

			Node tabContent;

			@Override
			protected Void call() throws Exception {
				tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
				return null;
			}

			@Override
			protected void succeeded() {
				parent.getChildren().clear();
				parent.setCenter(tabContent);
				initStatusBar(parent);
				progressPane.setVisible(false);
			}
		};
		parent.getChildren().clear();
		progressPane.addTo(parent);
		initStatusBar(parent);
		CompletableFuture.runAsync(task);
	}

	private void initStatusBar(final BorderPane parent) {
		if (isConnected) {
			final var glyph = new Glyph("FontAwesome", "INFO");
			glyph.useGradientEffect();
			glyph.useHoverEffect();

			final var popOver = new PopOver(initPopOverNode(subscribedTopics));
			popOver.setArrowLocation(BOTTOM_CENTER);

			final var button = new Button("", glyph);
			button.setOnMouseEntered(mouseEvent -> popOver.show(button));
			button.setOnMouseExited(mouseEvent -> popOver.hide());
			button.setBackground(new Background(new BackgroundFill(TRANSPARENT, new CornerRadii(2), new Insets(4))));

			statusBar.clearAllInRight();
			statusBar.addToRight(button);
		} else {
			statusBar.clearAllInRight();
		}
		statusBar.addTo(parent);
	}

	private Node initPopOverNode(final Set<String> topics) {
		Node listV = null;
		if (!topics.isEmpty()) {
			final var listView = new ListView<String>();
			topics.forEach(t -> listView.getItems().add(t));
			listView.addEventFilter(KeyEvent.KEY_PRESSED, KeyEvent::consume);
			listView.setStyle("-fx-focus-color: transparent;");
			listV = listView;
		}
		VBox vBox;
		if (listV != null) {
			vBox = new VBox(new Label("Configured Topics"), listV);
		} else {
			vBox = new VBox(new Label("No configured topics"));
		}
		VBox.setMargin(vBox, new Insets(10, 10, 10, 10));
		vBox.setStyle("-fx-padding: 18;");

		return vBox;
	}

}
