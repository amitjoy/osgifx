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
package com.osgifx.console.agent.provider;

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.cm.ConfigurationEvent.CM_DELETED;
import static org.osgi.service.cm.ConfigurationEvent.CM_LOCATION_CHANGED;
import static org.osgi.service.cm.ConfigurationEvent.CM_UPDATED;
import static org.osgi.service.event.EventConstants.SERVICE_ID;
import static org.osgi.service.event.EventConstants.SERVICE_OBJECTCLASS;
import static org.osgi.service.event.EventConstants.SERVICE_PID;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

public final class EventAdminCmListener implements ConfigurationListener {

	private final ServiceTracker<Object, Object> eventAdminTracker;

	public EventAdminCmListener(final BundleContext bundleContext) {
		eventAdminTracker = new ServiceTracker<>(bundleContext, "org.osgi.service.event.EventAdmin", null);
		eventAdminTracker.open();
	}

	@Override
	public void configurationEvent(final ConfigurationEvent event) {
		if (eventAdminTracker.getService() != null) {
			final String topic;
			switch (event.getType()) {
			case CM_UPDATED:
				topic = "org/osgi/service/cm/ConfigurationEvent/CM_UPDATED";
				break;
			case CM_DELETED:
				topic = "org/osgi/service/cm/ConfigurationEvent/CM_DELETED";
				break;
			case CM_LOCATION_CHANGED:
				topic = "org/osgi/service/cm/ConfigurationEvent/CM_LOCATION_CHANGED";
				break;
			default:
				return;
			}

			final Dictionary<String, Object> properties = new Hashtable<>();
			final String                     factoryPid = event.getFactoryPid();
			if (factoryPid != null) {
				properties.put("cm.factoryPid", factoryPid);
			}
			properties.put("cm.pid", event.getPid());

			final ServiceReference<ConfigurationAdmin> eventRef = event.getReference();
			properties.put(SERVICE_ID, eventRef.getProperty(SERVICE_ID));

			final Object objectClass = eventRef.getProperty(OBJECTCLASS);
			if (!(objectClass instanceof String[]) || !Arrays.asList((String[]) objectClass).contains(ConfigurationAdmin.class.getName())) {
				throw new IllegalArgumentException("Bad objectclass: " + objectClass);
			}
			properties.put(SERVICE_OBJECTCLASS, objectClass);
			properties.put(SERVICE_PID, eventRef.getProperty(SERVICE_PID));

			final Object service = eventAdminTracker.getService();
			((EventAdmin) service).postEvent(new Event(topic, properties));

		}
	}
}