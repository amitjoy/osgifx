/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.supervisor.snapshot;

import static com.osgifx.console.supervisor.snapshot.SnapshotSupervisor.CONDITION_ID_VALUE;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.io.IOException;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.MqttConnection;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.Supervisor;

@Component
@SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CONDITION_ID_VALUE + ")")
public final class SnapshotSupervisor implements Supervisor {

    public static final String  CONDITION_ID_VALUE  = "snapshot-agent";
    private static final String NOT_IMPLEMENTED_LOG = "Snapshot supervisor doesn't require this functionality";

    @Reference
    private SnapshotAgent agent;
    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Reference
    private ConfigurationAdmin configAdmin;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Deactivate
    void deactivate() throws IOException {
        final var configuration = configAdmin.getConfiguration(SnapshotAgent.PID, "?");
        configuration.delete();
    }

    @Override
    public boolean stdout(final String out) throws Exception {
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        return false;
    }

    @Override
    public void connect(final SocketConnection socketConnection) throws Exception {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void connect(final MqttConnection mqttConnection) throws Exception {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void onOSGiEvent(final XEventDTO event) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void logged(final XLogEntryDTO event) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void addOSGiEventListener(final EventListener eventListener) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void removeOSGiEventListener(final EventListener eventListener) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void addOSGiLogListener(final LogEntryListener logEntryListener) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public void removeOSGiLogListener(final LogEntryListener logEntryListener) {
        logger.atInfo().log(NOT_IMPLEMENTED_LOG);
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

}
