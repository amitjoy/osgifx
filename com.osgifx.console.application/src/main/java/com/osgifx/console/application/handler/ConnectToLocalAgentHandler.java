/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.REMOTE_RPC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.adapter.Adapt;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class ConnectToLocalAgentHandler {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private Executor                   executor;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    @Optional
    private Supervisor                 supervisor;
    @Inject
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean> isLocalAgent;
    @Inject
    @Optional
    @ContextValue("is_snapshot_agent")
    private ContextBoundValue<Boolean> isSnapshotAgent;
    @Inject
    @ContextValue("connected.agent")
    private ContextBoundValue<String>  connectedAgent;
    @Inject
    @Optional
    @Named("local.agent.host")
    private String                     localAgentHost;
    @Inject
    @Adapt
    @Optional
    @Named("local.agent.port")
    private int                        localAgentPort;
    @Inject
    @Adapt
    @Optional
    @Named("local.agent.timeout")
    private int                        localAgentTimeout;
    @Inject
    private SupervisorFactory          supervisorFactory;
    private ProgressDialog             progressDialog;

    @Execute
    public void execute() {
        final Task<Void> connectTask = new Task<>() {

            @Override
            protected Void call() throws Exception {
                try {
                    supervisorFactory.removeSupervisor(SNAPSHOT);
                    supervisorFactory.createSupervisor(REMOTE_RPC);
                    updateMessage("Connecting to Local Agent on " + localAgentPort);

                    // @formatter:off
                    final var socketConnection = SocketConnection
                            .builder()
                            .host(localAgentHost)
                            .port(localAgentPort)
                            .timeout(localAgentTimeout)
                            .build();
                    // @formatter:on

                    supervisor.connect(socketConnection);
                    logger.atInfo().log("Successfully connected to local agent on %s:%s", localAgentHost,
                            localAgentPort);
                    return null;
                } catch (final InterruptedException e) {
                    logger.atInfo().log("Connection task interrupted");
                    threadSync.asyncExec(progressDialog::close);
                    throw e;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot connect to local agent on %s:%s", localAgentHost,
                            localAgentPort);
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                logger.atInfo().log("Agent connected event has been sent for local agent on %s:%s", localAgentHost,
                        localAgentPort);

                final var connection = "[SOCKET] " + localAgentHost + ":" + localAgentPort;

                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, connection);
                connectedAgent.publish(connection);
                isConnected.publish(true);
                isLocalAgent.publish(true);
                isSnapshotAgent.publish(false);
            }
        };

        final var taskFuture = executor.runAsync(connectTask);
        progressDialog = FxDialog.showProgressDialog("Local Agent Connection", connectTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    @CanExecute
    public boolean canExecute() {
        return !isConnected.getValue();
    }

    @Inject
    @Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event received");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

}
