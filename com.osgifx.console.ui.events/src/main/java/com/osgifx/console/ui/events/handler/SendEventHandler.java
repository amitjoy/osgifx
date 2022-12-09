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
package com.osgifx.console.ui.events.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.events.converter.EventManager;
import com.osgifx.console.ui.events.dialog.SendEventDialog;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class SendEventHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @Inject
    private Executor          executor;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private EventManager      eventManager;

    @Execute
    public void execute() {
        final var dialog = new SendEventDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected send event dialog to eclipse context");
        dialog.init();

        final var event = dialog.showAndWait();
        if (event.isPresent()) {
            final Task<Void> sendEventTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto        = event.get();
                        final var topic      = dto.topic();
                        final var isSync     = dto.isSync();
                        final var properties = dto.properties();

                        if (Strings.isNullOrEmpty(topic) || properties == null) {
                            return null;
                        }

                        XResultDTO result;
                        if (isSync) {
                            result = eventManager.sendEvent(topic, properties);
                        } else {
                            result = eventManager.postEvent(topic, properties);
                        }
                        if (result == null) {
                            return null;
                        }
                        switch (result.result) {
                            case XResultDTO.SUCCESS:
                                threadSync.asyncExec(() -> Fx.showSuccessNotification("Send Event", result.response));
                                logger.atInfo().log("Event sent successfully to '%s'", topic);
                                break;
                            case XResultDTO.ERROR:
                                threadSync.asyncExec(() -> Fx.showErrorNotification("Send Event", result.response));
                                logger.atError().log("Event could not be sent to '%s'", topic);
                                break;
                            case XResultDTO.SKIPPED:
                                threadSync.asyncExec(() -> Fx.showErrorNotification("Send Event", result.response));
                                logger.atError().log("Event could not be sent to '%s' because %s", topic,
                                        result.response);
                                break;
                            default:
                                break;
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Event could not be sent");
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(sendEventTask);
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
