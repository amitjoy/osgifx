/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

public final class TableFilterUpdateTopics {

    private TableFilterUpdateTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String UPDATE_FILTER_EVENT_TOPIC_PREFIX        = "osgi/fx/console/table/filter/";
    public static final String UPDATE_FILTER_EVENT_TOPICS              = UPDATE_FILTER_EVENT_TOPIC_PREFIX + "*";
    public static final String UPDATE_BUNDLE_FILTER_EVENT_TOPIC        = UPDATE_FILTER_EVENT_TOPIC_PREFIX + "bundle";
    public static final String UPDATE_SERVICE_FILTER_EVENT_TOPIC       = UPDATE_FILTER_EVENT_TOPIC_PREFIX + "service";
    public static final String UPDATE_PACKAGE_FILTER_EVENT_TOPIC       = UPDATE_FILTER_EVENT_TOPIC_PREFIX + "package";
    public static final String UPDATE_COMPONENT_FILTER_EVENT_TOPIC     = UPDATE_FILTER_EVENT_TOPIC_PREFIX + "component";
    public static final String UPDATE_CONFIGURATION_FILTER_EVENT_TOPIC = UPDATE_FILTER_EVENT_TOPIC_PREFIX
            + "configuration";

}
