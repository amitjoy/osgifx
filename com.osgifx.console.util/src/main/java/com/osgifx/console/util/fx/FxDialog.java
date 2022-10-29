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
package com.osgifx.console.util.fx;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.function.Consumer;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class FxDialog {

    private FxDialog() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Alert showInfoDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader, null);
    }

    public static Alert showInfoDialog(final String header,
                                       final String content,
                                       final ClassLoader cssResLoader,
                                       final Consumer<ButtonType> actionOk) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader, actionOk);
    }

    public static Alert showWarningDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.WARNING, "", header, content, cssResLoader, null);
    }

    public static Alert showErrorDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.ERROR, "", header, content, cssResLoader, null);
    }

    public static Alert showConfirmationDialog(final String header,
                                               final String content,
                                               final ClassLoader cssResLoader,
                                               final Consumer<ButtonType> action) {
        return showDialog(AlertType.CONFIRMATION, "", header, content, cssResLoader, action);
    }

    public static ExceptionDialog showExceptionDialog(final Throwable throwable, final ClassLoader cssResLoader) {
        final var dialog = new ExceptionDialog(throwable);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(final String header,
                                                    final Task<?> task,
                                                    final ClassLoader cssResLoader,
                                                    final Runnable cancellationRunnable) {
        final var progressDialog = new ProgressDialog(task);

        progressDialog.setHeaderText(header);
        progressDialog.initStyle(StageStyle.UNDECORATED);

        final var dialogPane = progressDialog.getDialogPane();
        dialogPane.getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());
        dialogPane.getButtonTypes().setAll(ButtonType.CANCEL);

        task.setOnCancelled(succeeded -> progressDialog.close());
        progressDialog.setOnCloseRequest(closeRequest -> {
            if (cancellationRunnable != null) {
                cancellationRunnable.run();
                task.cancel(true);
            }
        });
        progressDialog.show();
        return progressDialog;
    }

    public static ChoiceDialog<String> showChoiceDialog(final String header,
                                                        final ClassLoader resLoader,
                                                        final String graphicPath,
                                                        final Consumer<String> actionOk,
                                                        final Runnable actionCancel,
                                                        final String defaultChoice,
                                                        final String... choices) {
        final var choiceDialog = new ChoiceDialog<>(defaultChoice, choices);

        choiceDialog.setHeaderText(header);
        choiceDialog.initStyle(StageStyle.UNDECORATED);
        choiceDialog.getDialogPane().getStylesheets().add(resLoader.getResource(STANDARD_CSS).toExternalForm());
        choiceDialog.setGraphic(new ImageView(resLoader.getResource(graphicPath).toString()));

        final var returnVal = choiceDialog.showAndWait();
        if (actionOk != null && returnVal.isPresent()) {
            actionOk.accept(returnVal.get());
        } else if (actionCancel != null) {
            actionCancel.run();
        }
        return choiceDialog;
    }

    public static Alert showDialog(final AlertType type,
                                   final String title,
                                   final String header,
                                   final String content,
                                   final ClassLoader cssResLoader,
                                   final Consumer<ButtonType> action) {

        final var alert = new Alert(type);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        final var returnVal = alert.showAndWait();
        if (action != null && returnVal.isPresent()) {
            action.accept(returnVal.get());
        }
        return alert;
    }

}
