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

public final class DataRetrievedEventTopics {

    private DataRetrievedEventTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String DATA_RETRIEVED_EVENT_TOPIC_PREFIX    = "com/osgifx/data/retrieved/";
    public static final String DATA_RETRIEVED_ALL_TOPIC             = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "all";
    public static final String DATA_RETRIEVED_BUNDLES_TOPIC         = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "bundles";
    public static final String DATA_RETRIEVED_COMPONENTS_TOPIC      = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "components";
    public static final String DATA_RETRIEVED_CONFIGURATIONS_TOPIC  = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "configurations";
    public static final String DATA_RETRIEVED_HEALTHCHECKS_TOPIC    = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "healthchecks";
    public static final String DATA_RETRIEVED_HTTP_TOPIC            = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "http";
    public static final String DATA_RETRIEVED_LEAKS_TOPIC           = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "leaks";
    public static final String DATA_RETRIEVED_LOGGER_CONTEXTS_TOPIC = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "loggerContexts";
    public static final String DATA_RETRIEVED_PACKAGES_TOPIC        = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "packages";
    public static final String DATA_RETRIEVED_PROPERTIES_TOPIC      = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "properties";
    public static final String DATA_RETRIEVED_ROLES_TOPIC           = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "roles";
    public static final String DATA_RETRIEVED_SERVICES_TOPIC        = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "services";
    public static final String DATA_RETRIEVED_THREADS_TOPIC         = DATA_RETRIEVED_EVENT_TOPIC_PREFIX + "threads";

}
