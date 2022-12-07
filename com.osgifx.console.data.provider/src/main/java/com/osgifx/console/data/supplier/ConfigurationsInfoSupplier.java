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
package com.osgifx.console.data.supplier;

import static com.osgifx.console.data.supplier.ConfigurationsInfoSupplier.CONFIGURATIONS_ID;
import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CONFIGURATIONS_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.util.fx.ConsoleFxHelper.makeNullSafe;
import static javafx.collections.FXCollections.observableArrayList;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.data.manager.RuntimeInfoSupplier;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@Component
@SupplierID(CONFIGURATIONS_ID)
@EventTopics({ AGENT_DISCONNECTED_EVENT_TOPIC, COMPONENT_ACTION_EVENT_TOPICS, CONFIGURATION_ACTION_EVENT_TOPICS })
public final class ConfigurationsInfoSupplier implements RuntimeInfoSupplier, EventHandler {

    public static final String CONFIGURATIONS_ID = "configurations";

    @Reference
    private LoggerFactory       factory;
    @Reference
    private Executor            executor;
    @Reference
    private EventAdmin          eventAdmin;
    @Reference
    private ThreadSynchronize   threadSync;
    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;
    private FluentLogger        logger;

    private final ObservableList<XConfigurationDTO> configurations = observableArrayList();

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public synchronized void retrieve() {
        logger.atInfo().log("Retrieving configurations info from remote runtime");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return;
        }
        configurations.setAll(makeNullSafe(agent.getAllConfigurations()));
        RuntimeInfoSupplier.sendEvent(eventAdmin, DATA_RETRIEVED_CONFIGURATIONS_TOPIC);
        logger.atInfo().log("Configurations info retrieved successfully");
    }

    @Override
    public ObservableList<?> supply() {
        return configurations;
    }

    @Override
    public void handleEvent(final Event event) {
        if (AGENT_DISCONNECTED_EVENT_TOPIC.equals(event.getTopic())) {
            threadSync.asyncExec(configurations::clear);
            return;
        }
        executor.runAsync(this::retrieve);
    }

}
