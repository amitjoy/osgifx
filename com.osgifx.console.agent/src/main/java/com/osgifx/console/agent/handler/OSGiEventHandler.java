/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.agent.handler;

import static com.osgifx.console.agent.provider.AgentServer.PROPERTY_ENABLE_EVENTING;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.supervisor.Supervisor;

import jakarta.inject.Inject;

public final class OSGiEventHandler implements EventHandler {

    private final Supervisor    supervisor;
    private final BundleContext context;
    private final FluentLogger  logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public OSGiEventHandler(final BundleContext context, final Supervisor supervisor) {
        this.context    = context;
        this.supervisor = supervisor;
    }

    public ServiceRegistration<?> register() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(EVENT_TOPIC, "*");
        return context.registerService(EventHandler.class, this, properties);
    }

    @Override
    public void handleEvent(final Event event) {
        final boolean isLoggingEnabled = Boolean.getBoolean(PROPERTY_ENABLE_EVENTING);
        if (!isLoggingEnabled) {
            return;
        }
        final XEventDTO dto = new XEventDTO();

        dto.received   = System.currentTimeMillis();
        dto.properties = initProperties(event);
        dto.topic      = event.getTopic();

        supervisor.onOSGiEvent(dto);
    }

    private Map<String, String> initProperties(final Event event) {
        final Map<String, String> properties = new HashMap<>();

        for (final String propertyName : event.getPropertyNames()) {
            final Object propertyValue = event.getProperty(propertyName);
            try {
                properties.put(propertyName, processElement(propertyValue));
            } catch (final Exception e) {
                logger.atError().msg("Event properties cannot be converted").throwable(e).log();
            }
        }
        return properties;
    }

    private String processElement(final Object propertyValue) {
        if (propertyValue instanceof boolean[]) {
            return Arrays.toString((boolean[]) propertyValue);
        }
        if (propertyValue instanceof int[]) {
            return Arrays.toString((int[]) propertyValue);
        }
        if (propertyValue instanceof float[]) {
            return Arrays.toString((float[]) propertyValue);
        }
        if (propertyValue instanceof double[]) {
            return Arrays.toString((double[]) propertyValue);
        }
        if (propertyValue instanceof long[]) {
            return Arrays.toString((long[]) propertyValue);
        }
        if (propertyValue instanceof String[]) {
            return Arrays.toString((String[]) propertyValue);
        }
        if (propertyValue instanceof char[]) {
            return Arrays.toString((char[]) propertyValue);
        }
        return String.valueOf(propertyValue);
    }

}
