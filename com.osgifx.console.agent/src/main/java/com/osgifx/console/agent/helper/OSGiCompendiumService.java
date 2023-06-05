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
package com.osgifx.console.agent.helper;

public enum OSGiCompendiumService {

    SCR("SCR"),
    CM("Config Admin"),
    METATYPE("Metatype"),
    DMT("DMT Admin"),
    USER_ADMIN("User Admin"),
    LOGGER_ADMIN("R7 Logger Admin"),
    EVENT_ADMIN("Event Admin"),
    HTTP_RUNTIME("Event Admin");

    String comprehensibleName;

    OSGiCompendiumService(final String comprehensibleName) {
        this.comprehensibleName = comprehensibleName;
    }

}
