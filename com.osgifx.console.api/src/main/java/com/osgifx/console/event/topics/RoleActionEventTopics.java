/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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

public final class RoleActionEventTopics {

    private RoleActionEventTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String ROLE_ACTION_EVENT_TOPIC_PREFIX = "osgi/fx/console/role/";
    public static final String ROLE_ACTION_EVENT_TOPICS       = ROLE_ACTION_EVENT_TOPIC_PREFIX + "*";
    public static final String ROLE_CREATED_EVENT_TOPIC       = ROLE_ACTION_EVENT_TOPIC_PREFIX + "created";
    public static final String ROLE_UPDATED_EVENT_TOPIC       = ROLE_ACTION_EVENT_TOPIC_PREFIX + "updated";
    public static final String ROLE_DELETED_EVENT_TOPIC       = ROLE_ACTION_EVENT_TOPIC_PREFIX + "deleted";

}
