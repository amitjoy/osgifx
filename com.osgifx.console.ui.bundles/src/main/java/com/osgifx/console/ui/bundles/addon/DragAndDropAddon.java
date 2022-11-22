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
package com.osgifx.console.ui.bundles.addon;

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_INSTALLED_EVENT_TOPIC;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WWindow;
import org.osgi.service.event.Event;

import com.google.common.io.Files;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

public final class DragAndDropAddon {

    private static final int STARTLEVEL = 10;

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Optional
    private Supervisor        supervisor;
    @Inject
    private MApplication      application;
    @Inject
    private EModelService     modelService;
    @Inject
    private IEventBroker      eventBroker;

    @Inject
    @Optional
    public void onActivate(@EventTopic(UIEvents.UILifeCycle.ACTIVATE) final Event event) {
        try {
            final var window = (MWindow) modelService.find("com.osgifx.console.window.main", application);
            final var stage  = (Stage) ((WWindow<?>) window.getWidget()).getWidget();
            registerDragAndDropSupport(stage.getScene());
        } catch (final Exception e) {
            // ignore
        }
    }

    private void registerDragAndDropSupport(final Scene scene) {
        scene.setOnDragOver(event -> {
            final var db         = event.getDragboard();
            final var isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".jar") && supervisor != null
                    && supervisor.getAgent() != null;
            if (db.hasFiles() && isAccepted) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        scene.setOnDragDropped(event -> {
            final var agent = supervisor.getAgent();
            if (agent == null) {
                logger.atWarning().log("Remote agent cannot be connected");
                return;
            }
            final var db      = event.getDragboard();
            var       success = false;
            if (db.hasFiles()) {
                success = true;
                // only get the first file from the list
                final var file = db.getFiles().get(0);
                FxDialog.showConfirmationDialog("Bundle Installation", "Do you want to install " + file.getName(),
                        getClass().getClassLoader(), type -> {
                            if (type == ButtonType.CANCEL) {
                                return;
                            }
                            final Task<Void> installTask = new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    try {
                                        final var bundle = agent.installWithData(null, Files.toByteArray(file),
                                                STARTLEVEL);
                                        if (bundle == null) {
                                            logger.atError().log("Bundle cannot be installed or updated");
                                            return null;
                                        }
                                        logger.atInfo().log("Bundle has been installed or updated: %s", bundle);
                                        eventBroker.post(BUNDLE_INSTALLED_EVENT_TOPIC, bundle.symbolicName);
                                        threadSync.asyncExec(() -> Fx.showSuccessNotification("Remote Bundle Install",
                                                bundle.symbolicName + " successfully installed/updated"));
                                    } catch (final Exception e) {
                                        logger.atError().withException(e).log("Bundle cannot be installed or updated");
                                        threadSync.asyncExec(
                                                () -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                                    }
                                    return null;
                                }
                            };
                            CompletableFuture.runAsync(installTask);
                        });

            }
            event.setDropCompleted(success);
            event.consume();
        });
        logger.atInfo().log("Registered drag and drop support for bundles");
    }

}
