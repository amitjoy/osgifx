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
package com.osgifx.console.application;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.concurrent.Worker.State.SUCCEEDED;
import static javafx.scene.paint.Color.TRANSPARENT;

import org.eclipse.fx.ui.workbench.fx.DefaultJFXApp;
import org.osgi.framework.FrameworkUtil;

import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class ConsoleFxStage extends DefaultJFXApp {

	private static final String SPLASH_IMAGE = "/graphic/images/splash.png";

	private VBox        splashLayout;
	private ProgressBar loadProgress;
	private Label       progressText;
	private Stage       initStage;

	private static final double SPLASH_WIDTH  = 695d;
	private static final double SPLASH_HEIGHT = 227d;

	@Override
	public void init() throws Exception {
		loadCustomFont();
		final var splash = new ImageView(new Image(SPLASH_IMAGE));

		loadProgress = new ProgressBar();
		loadProgress.setPrefWidth(SPLASH_WIDTH - 20);
		progressText = new Label("");
		splashLayout = new VBox();
		splashLayout.setSpacing(2);

		splashLayout.getChildren().addAll(splash, loadProgress, progressText);
		progressText.setAlignment(Pos.CENTER);
		splashLayout.setStyle("-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: "
		        + "linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");
		splashLayout.setEffect(new DropShadow());
	}

	private void loadCustomFont() {
		final var bundle = FrameworkUtil.getBundle(getClass());
		Font.loadFont(bundle.getResource("/font/Gill Sans SemiBold.ttf").toExternalForm(), 11);
	}

	@Override
	public void start(final Stage initStage) throws Exception {
		this.initStage = initStage;
		final Task<Void> friendTask = new Task<>() {
			@Override
			protected Void call() throws InterruptedException {
				updateMessage("Initializing Console . . .");
				for (var i = 0; i < 5; i++) {
					Thread.sleep(400);
					updateProgress(i + 1L, 5);
				}
				Thread.sleep(400);
				updateMessage("Console Initialized.");
				return null;
			}
		};
		showSplash(initStage, friendTask, this::showFxConsoleStage);
		new Thread(friendTask).start();

		registerHostServices();
	}

	private void registerHostServices() {
		final var hostServices = getHostServices();
		final var context      = FrameworkUtil.getBundle(getClass()).getBundleContext();
		context.registerService(HostServices.class, hostServices, null);
	}

	private void showFxConsoleStage() {
		initialize();
		e4Application.jfxStart(e4Application.getApplicationContext(), this, initStage);
	}

	private void showSplash(final Stage initStage, final Task<?> task, final InitCompletionHandler initCompletionHandler) {
		progressText.textProperty().bind(task.messageProperty());
		loadProgress.progressProperty().bind(task.progressProperty());
		task.stateProperty().addListener((observableValue, oldState, newState) -> {
			if (newState == SUCCEEDED) {
				loadProgress.progressProperty().unbind();
				loadProgress.setProgress(1);
				initStage.toFront();

				final var fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);

				fadeSplash.setFromValue(1.0);
				fadeSplash.setToValue(0.0);
				fadeSplash.setOnFinished(actionEvent -> initStage.hide());
				fadeSplash.play();

				initCompletionHandler.complete();
			}
		});

		final var splashScene = new Scene(splashLayout, TRANSPARENT);
		final var bounds      = Screen.getPrimary().getBounds();

		splashScene.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

		initStage.setScene(splashScene);
		initStage.setX(bounds.getMinX() + bounds.getWidth() / 2d - SPLASH_WIDTH / 2d);
		initStage.setY(bounds.getMinY() + bounds.getHeight() / 2d - SPLASH_HEIGHT / 2d);
		initStage.initStyle(StageStyle.TRANSPARENT);
		initStage.setAlwaysOnTop(true);
		initStage.show();
	}

	@FunctionalInterface
	public interface InitCompletionHandler {
		void complete();
	}

}
