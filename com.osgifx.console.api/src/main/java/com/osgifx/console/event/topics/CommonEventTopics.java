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
package com.osgifx.console.event.topics;

public final class CommonEventTopics {

	private CommonEventTopics() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static final String EVENT_RECEIVE_EVENT_TOPICS        = "osgi/fx/event/receive/*";
	public static final String EVENT_RECEIVE_STARTED_EVENT_TOPIC = "osgi/fx/event/receive/started";
	public static final String EVENT_RECEIVE_STOPPED_EVENT_TOPIC = "osgi/fx/event/receive/stopped";

	public static final String LOG_RECEIVE_EVENT_TOPICS        = "osgi/fx/log/receive/*";
	public static final String LOG_RECEIVE_STARTED_EVENT_TOPIC = "osgi/fx/log/receive/started";
	public static final String LOG_RECEIVE_STOPPED_EVENT_TOPIC = "osgi/fx/log/receive/stopped";

	public static final String CLEAR_EVENTS_TOPIC = "com/osgifx/clear/events";
	public static final String CLEAR_LOGS_TOPIC   = "com/osgifx/clear/logs";

}
