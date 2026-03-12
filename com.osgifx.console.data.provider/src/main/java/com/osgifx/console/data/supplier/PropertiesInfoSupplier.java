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
package com.osgifx.console.data.supplier;

import static com.osgifx.console.data.supplier.PropertiesInfoSupplier.PROPERTIES_ID;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_PROPERTIES_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.collections.FXCollections.observableArrayList;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;
import com.osgifx.console.data.manager.RuntimeInfoSupplier;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@Component
@ServiceRanking(105)
@SupplierID(PROPERTIES_ID)
@EventTopics(AGENT_DISCONNECTED_EVENT_TOPIC)
public final class PropertiesInfoSupplier implements RuntimeInfoSupplier, EventHandler {

    public static final String PROPERTIES_ID = "properties";

    @Reference
    private LoggerFactory       factory;
    @Reference
    private EventAdmin          eventAdmin;
    @Reference
    private ThreadSynchronize   threadSync;
    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor supervisor;
    private FluentLogger        logger;
    private SnapshotDecoder     decoder;

    private final ObservableList<XPropertyDTO> properties   = observableArrayList();
    private final ReentrantLock                retrieveLock = new ReentrantLock();

    @Activate
    void activate(final BundleContext context) {
        logger  = FluentLogger.of(factory.createLogger(getClass().getName()));
        decoder = new SnapshotDecoder(new BinaryCodec(context));
    }

    @Override
    public void retrieve() {
        retrieveLock.lock();
        try {
            final var agent = supervisor.getAgent();
            if (agent == null) {
                logger.atWarning().log("Agent not connected");
                return;
            }
            logger.atInfo().log("Retrieving properties info from remote runtime");
            final byte[]             snapshot = agent.properties();
            final List<XPropertyDTO> data     = decoder.decodeList(snapshot, XPropertyDTO.class);
            threadSync.asyncExec(() -> {
                properties.setAll(data);
                RuntimeInfoSupplier.sendEvent(eventAdmin, DATA_RETRIEVED_PROPERTIES_TOPIC);
                logger.atInfo().log("Properties info retrieved successfully");
            });
        } finally {
            retrieveLock.unlock();
        }
    }

    @Override
    public ObservableList<?> supply() {
        return properties;
    }

    @Override
    public void handleEvent(final Event event) {
        threadSync.asyncExec(properties::clear);
    }

}
