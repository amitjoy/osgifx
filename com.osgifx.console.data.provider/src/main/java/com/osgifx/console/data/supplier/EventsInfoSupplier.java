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

import static com.osgifx.console.data.supplier.EventsInfoSupplier.EVENTS_ID;
import static com.osgifx.console.data.supplier.EventsInfoSupplier.PID;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.CLEAR_EVENTS_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.collections.FXCollections.observableArrayList;

import java.util.Collection;
import java.util.Set;

import org.eclipse.fx.core.ThreadSynchronize;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.google.common.collect.Sets;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@SupplierID(EVENTS_ID)
@Component(configurationPid = PID)
@EventTopics({ AGENT_DISCONNECTED_EVENT_TOPIC, CLEAR_EVENTS_TOPIC })
public final class EventsInfoSupplier implements RuntimeInfoSupplier, EventListener, EventHandler {

	static final String PID = "event.receive.topics";

	@interface Configuration {
		String[] topics();
	}

	public static final String EVENTS_ID = "events";

	private Configuration     configuration;
	@Reference
	private Supervisor        supervisor;
	@Reference
	private ThreadSynchronize threadSync;

	private final ObservableList<XEventDTO> events = observableArrayList();

	@Activate
	@Modified
	void init(final Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void retrieve() {
		// nothing to retrieve manually
	}

	@Override
	public ObservableList<?> supply() {
		return events;
	}

	@Override
	public synchronized void onEvent(final XEventDTO event) {
		events.add(event);
	}

	@Override
	public Collection<String> topics() {
		return configuration != null ? Sets.newHashSet(configuration.topics()) : Set.of();
	}

	@Override
	public void handleEvent(final Event event) {
		threadSync.asyncExec(events::clear);
	}

}
