/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.helper;

/**
 * Enum representing various OSGi Compendium Services. Each service is identified 
 * by a key and has a comprehensible name to describe its function within the 
 * OSGi framework.
 * <p>
 * The {@code OSGiCompendiumService} enum provides a standard list of OSGi services 
 * used in the framework, such as the Service Component Runtime (SCR), Configuration 
 * Admin (CM), Metatype, DMT Admin, User Admin, Logger Admin, and Event Admin.
 * </p>
 */
public enum OSGiCompendiumService {

    /** Service Component Runtime (SCR) service. */
    SCR("SCR"),

    /** Configuration Admin (CM) service. */
    CM("Config Admin"),

    /** Metatype service for managing metatype information. */
    METATYPE("Metatype"),

    /** Device Management Tree (DMT) Admin service. */
    DMT("DMT Admin"),

    /** User Admin service for managing user accounts and groups. */
    USER_ADMIN("User Admin"),

    /** Logger Admin service according to OSGi R7 specification. */
    LOGGER_ADMIN("R7 Logger Admin"),

    /** Event Admin service for managing event delivery. */
    EVENT_ADMIN("Event Admin"),

    /** HTTP Runtime service. */
    HTTP_RUNTIME("HTTP Runtime");

    /** The comprehensible name of the OSGi compendium service. */
    public final String comprehensibleName;

    /**
     * Constructs an {@code OSGiCompendiumService} with a comprehensible name.
     *
     * @param comprehensibleName the human-readable name of the OSGi service
     */
    OSGiCompendiumService(final String comprehensibleName) {
        this.comprehensibleName = comprehensibleName;
    }

    /**
     * Returns the comprehensible name of the OSGi compendium service.
     *
     * @return the human-readable name of the service
     */
    public String getComprehensibleName() {
        return comprehensibleName;
    }
}