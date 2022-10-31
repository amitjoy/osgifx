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

import static com.osgifx.console.data.supplier.BundlesInfoSupplier.BUNDLES_ID;
import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_BUNDLES_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.util.fx.ConsoleFxHelper.makeNullSafe;
import static javafx.collections.FXCollections.observableArrayList;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.CompletableFuture;

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

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.manager.RuntimeInfoSupplier;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@Component
@SupplierID(BUNDLES_ID)
@EventTopics({ AGENT_DISCONNECTED_EVENT_TOPIC, BUNDLE_ACTION_EVENT_TOPICS })
public final class BundlesInfoSupplier implements RuntimeInfoSupplier, EventHandler {

    public static final String BUNDLES_ID = "bundles";

    @Reference
    private LoggerFactory       factory;
    @Reference
    private EventAdmin          eventAdmin;
    @Reference
    private ThreadSynchronize   threadSync;
    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;
    private FluentLogger        logger;

    private final ObservableList<XBundleDTO> bundles = observableArrayList();

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public synchronized void retrieve() {
        logger.atInfo().log("Retrieving bundles info from remote runtime");
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return;
        }
        bundles.setAll(makeNullSafe(agent.getAllBundles()));
        RuntimeInfoSupplier.sendEvent(eventAdmin, DATA_RETRIEVED_BUNDLES_TOPIC);
        logger.atInfo().log("Bundles info retrieved successfully");
    }

    @Override
    public ObservableList<?> supply() {
        return bundles;
    }

    @Override
    public void handleEvent(final Event event) {
        if (AGENT_DISCONNECTED_EVENT_TOPIC.equals(event.getTopic())) {
            threadSync.asyncExec(bundles::clear);
            return;
        }
        CompletableFuture.runAsync(this::retrieve);
    }

}
