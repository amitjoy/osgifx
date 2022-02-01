/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.application.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.adapter.Adapt;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class ConnectToLocalAgentHandler {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    private Supervisor                 supervisor;
    @Inject
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;
    @Inject
    @ContextValue("connected.agent")
    private ContextBoundValue<String>  connectedAgent;
    @Inject
    @Named("local.agent.host")
    private String                     localAgentHost;
    @Inject
    @Adapt
    @Named("local.agent.port")
    private int                        localAgentPort;
    @Inject
    @Adapt
    @Named("local.agent.timeout")
    private int                        localAgentTimeout;
    private ProgressDialog             progressDialog;

    @Execute
    public void execute() {
        logger.atInfo().log("'%s'-'connectLocalAgent(..)' event has been invoked", getClass().getSimpleName());
        final Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Connecting to Local Agent on " + localAgentPort);
                    supervisor.connect(localAgentHost, localAgentPort, localAgentTimeout);
                    logger.atInfo().log("Successfully connected to Local Agent on %s:%s", localAgentHost, localAgentPort);
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot connect to Local Agent on %s:%s", localAgentHost, localAgentPort);
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                logger.atInfo().log("Agent connected event has been sent for Local Agent on %s:%s", localAgentHost, localAgentPort);

                final String connection = localAgentHost + ":" + localAgentPort;

                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, connection);
                connectedAgent.publish(connection);
                isConnected.publish(true);
            }
        };

        final Thread th = new Thread(connectTask);
        th.setDaemon(true);
        th.start();

        progressDialog = FxDialog.showProgressDialog("Local Agent Connection", connectTask, getClass().getClassLoader());
    }

    @CanExecute
    public boolean canExecute() {
        return !isConnected.getValue();
    }

    @Inject
    @org.eclipse.e4.core.di.annotations.Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event received");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

}
