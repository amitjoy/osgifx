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

public final class OSGiEventAdminTopics {

	private OSGiEventAdminTopics() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static final String BUNDLE_EVENTS_TOPIC        = "org/osgi/framework/BundleEvent/*";
	public static final String SERVICE_EVENTS_TOPIC       = "org/osgi/framework/ServiceEvent/*";
	public static final String CONFIGURATION_EVENTS_TOPIC = "org/osgi/service/cm/ConfigurationEvent/*";

}
