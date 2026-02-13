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
package com.osgifx.console.application;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static javafx.scene.paint.Color.TRANSPARENT;

import java.util.concurrent.CompletableFuture;

import org.apache.aries.component.dsl.OSGi;
import org.eclipse.fx.ui.workbench.fx.DefaultJFXApp;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class ConsoleFxStage extends DefaultJFXApp {

    private static final String SPLASH_IMAGE = "/graphic/images/splash.png";
    private static final String SPLASH_CSS   = "/css/splash.css";

    private Stage        initStage;
    private StackPane    splashLayout;
    private ProgressBar  loadProgress;
    private final Bundle applicationBundle;

    private static final double SPLASH_WIDTH  = 695d;
    private static final double SPLASH_HEIGHT = 227d;

    public ConsoleFxStage() {
        applicationBundle = FrameworkUtil.getBundle(getClass());
    }

    @Override
    public void init() throws Exception {
        loadCustomFont();
        final var splash = new ImageView(new Image(SPLASH_IMAGE));

        loadProgress = new ProgressBar();
        loadProgress.setPrefWidth(SPLASH_WIDTH - 20);
        loadProgress.setPrefHeight(10);
        loadProgress.setProgress(0);

        loadProgress.setMaxWidth(Double.MAX_VALUE); // Let it fill the width completely within padding

        final var progressBox = new VBox();
        progressBox.getChildren().add(loadProgress);
        progressBox.setAlignment(Pos.BOTTOM_CENTER);
        progressBox.setMaxWidth(SPLASH_WIDTH - 20); // Constrain width slightly to ensure padding
        progressBox.setPadding(new Insets(0, 0, 2, 0)); // 2px from bottom
        progressBox.getStyleClass().add("progress-box"); // For gradient background

        splashLayout = new StackPane();
        splashLayout.getChildren().addAll(splash, progressBox);
        StackPane.setAlignment(progressBox, Pos.BOTTOM_CENTER);

        splashLayout.getStyleClass().add("splash-layout");
        splashLayout.setEffect(new DropShadow());
    }

    private void loadCustomFont() {
        Font.loadFont(applicationBundle.getResource("/font/Gill Sans SemiBold.ttf").toExternalForm(), 11);
    }

    @Override
    public void start(final Stage initStage) throws Exception {
        this.initStage = initStage;
        final Task<Void> friendTask = new Task<>() {

            @Override
            protected Void call() throws InterruptedException {
                updateMessage("Initializing Console . . .");
                // Fluid animation: 100 steps of 20ms = 2 seconds
                for (var i = 0; i < 100; i++) {
                    Thread.sleep(20);
                    updateProgress(i + 1L, 100);
                }
                Thread.sleep(200);
                updateMessage("Console Initialized.");
                return null;
            }
        };
        showSplash(initStage, friendTask, this::showFxConsoleStage);
        CompletableFuture.runAsync(friendTask);
        registerHostServices(applicationBundle.getBundleContext());
    }

    private void registerHostServices(final BundleContext context) {
        final var hostServices = getHostServices();
        OSGi.register(HostServices.class, hostServices, null).run(context);
    }

    private void showFxConsoleStage() {
        initialize();
        e4Application.jfxStart(e4Application.getApplicationContext(), this, initStage);
    }

    private void showSplash(final Stage initStage, final Task<?> task, final Runnable completionHandler) {
        loadProgress.progressProperty().bind(task.progressProperty());

        task.stateProperty().addListener((_, _, newState) -> {
            if (newState == SUCCEEDED) {
                loadProgress.progressProperty().unbind();
                loadProgress.setProgress(1);
                initStage.toFront();

                final var fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);

                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(_ -> initStage.hide());
                fadeSplash.play();

                completionHandler.run();
            }
        });

        final var splashScene = new Scene(splashLayout, TRANSPARENT);
        final var bounds      = Screen.getPrimary().getBounds();

        splashScene.getStylesheets().add(getClass().getResource(SPLASH_CSS).toExternalForm());

        initStage.setScene(splashScene);
        initStage.setX(bounds.getMinX() + bounds.getWidth() / 2d - SPLASH_WIDTH / 2d);
        initStage.setY(bounds.getMinY() + bounds.getHeight() / 2d - SPLASH_HEIGHT / 2d);
        initStage.initStyle(StageStyle.TRANSPARENT);
        initStage.setAlwaysOnTop(true);
        initStage.show();
    }

}
