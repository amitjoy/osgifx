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
package com.osgifx.console.application.addon;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class AgentConnectedAddon {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private Executor                   executor;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    private DataProvider               dataProvider;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean> isLocalAgent;
    private ProgressDialog             progressDialog;

    @Inject
    @Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event has been received");
        final Task<Void> dataRetrievalTask = new Task<>() {

            @Override
            protected Void call() throws Exception {
                // we always load all the data when agent gets connected
                // later we will load only the respective data when we select a specific tab
                dataProvider.retrieveInfo(null, true);
                return null;
            }

            @Override
            protected void succeeded() {
                threadSync.asyncExec(progressDialog::close);
            }
        };

        final var agent  = Boolean.TRUE.equals(isLocalAgent.getValue()) ? "local" : "remote";
        final var header = "Retrieving information from " + agent + " agent";

        final var taskFuture = executor.runAsync(dataRetrievalTask);
        progressDialog = FxDialog.showProgressDialog(header, dataRetrievalTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

}
