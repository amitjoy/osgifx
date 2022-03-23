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
package com.osgifx.console.agent.provider;

import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class XEventAdmin {

	private final EventAdmin eventAdmin;

	public XEventAdmin(final Object eventAdmin) {
		this.eventAdmin = (EventAdmin) eventAdmin;
	}

	public void sendEvent(final String topic, final Map<String, Object> properties) {
		final Event event = new Event(topic, properties);
		eventAdmin.sendEvent(event);
	}

	public void postEvent(final String topic, final Map<String, Object> properties) {
		final Event event = new Event(topic, properties);
		eventAdmin.sendEvent(event);
	}

}
