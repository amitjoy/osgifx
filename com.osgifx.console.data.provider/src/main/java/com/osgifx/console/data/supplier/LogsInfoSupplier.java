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

import static com.osgifx.console.data.supplier.LogsInfoSupplier.LOGS_ID;
import static com.osgifx.console.event.topics.LogReceiveEventTopics.CLEAR_LOGS_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.collections.FXCollections.observableArrayList;

import org.eclipse.fx.core.ThreadSynchronize;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@Component
@SupplierID(LOGS_ID)
@EventTopics({ AGENT_DISCONNECTED_EVENT_TOPIC, CLEAR_LOGS_TOPIC })
public final class LogsInfoSupplier implements RuntimeInfoSupplier, LogEntryListener, EventHandler {

    public static final String LOGS_ID = "logs";

    @Reference
    private Supervisor        supervisor;
    @Reference
    private ThreadSynchronize threadSync;

    private final ObservableList<XLogEntryDTO> logs = observableArrayList();

    @Override
    public void retrieve() {
        // nothing to retrieve manually
    }

    @Override
    public ObservableList<?> supply() {
        return logs;
    }

    @Override
    public synchronized void logged(final XLogEntryDTO logEntry) {
        logs.add(logEntry);
    }

    @Override
    public void handleEvent(final Event event) {
        threadSync.asyncExec(logs::clear);
    }

}
