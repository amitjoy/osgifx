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
package com.osgifx.console.data.manager;

import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import javafx.collections.ObservableList;

public interface RuntimeInfoSupplier {

    String PROPERTY_ID = "supplier.id";

    /**
     * Retrieves from the remote runtime
     */
    void retrieve();

    /**
     * Returns the observable list
     */
    ObservableList<?> supply();

    static void sendEvent(final EventAdmin eventAdmin, final String topic) {
        final var event = new Event(topic, Map.of());
        eventAdmin.postEvent(event);
    }

}
