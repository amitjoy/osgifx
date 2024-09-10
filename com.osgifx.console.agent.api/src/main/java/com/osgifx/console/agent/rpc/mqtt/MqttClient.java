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
package com.osgifx.console.agent.rpc.mqtt;

import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;

/**
 * Manages MQTT clients for publishing and subscribing to messages within the OSGi framework.
 * This class uses {@link ServiceTracker} to track instances of {@link Mqtt5Publisher} and 
 * {@link Mqtt5Subscriber} services, allowing for dynamic service binding and unbinding.
 */
public final class MqttClient {

    /** Tracks the {@link Mqtt5Publisher} service for MQTT message publishing. */
    private final ServiceTracker<Mqtt5Publisher, Mqtt5Publisher> publisherTracker;

    /** Tracks the {@link Mqtt5Subscriber} service for MQTT message subscribing. */
    private final ServiceTracker<Mqtt5Subscriber, Mqtt5Subscriber> subscriberTracker;

    /**
     * Constructs an MQTT client that tracks publisher and subscriber services.
     *
     * @param bundleContext       the OSGi {@link BundleContext} for service tracking
     * @param subscriberCallback  a callback to handle new {@link Mqtt5Subscriber} services
     */
    public MqttClient(final BundleContext bundleContext, final Consumer<Mqtt5Subscriber> subscriberCallback) {
        publisherTracker = new ServiceTracker<>(bundleContext, Mqtt5Publisher.class, null);
        subscriberTracker = new ServiceTracker<Mqtt5Subscriber, Mqtt5Subscriber>(bundleContext, Mqtt5Subscriber.class, null) {
            @Override
            public Mqtt5Subscriber addingService(final ServiceReference<Mqtt5Subscriber> reference) {
                final Mqtt5Subscriber service = super.addingService(reference);
                subscriberCallback.accept(service);
                return service;
            }
        };
    }

    /**
     * Retrieves an optional {@link Mqtt5Publisher} service.
     *
     * @return an {@link Optional} containing the {@link Mqtt5Publisher} if available, otherwise empty
     */
    public Optional<Mqtt5Publisher> pub() {
        return Optional.ofNullable(publisherTracker.getService());
    }

    /**
     * Retrieves an optional {@link Mqtt5Subscriber} service.
     *
     * @return an {@link Optional} containing the {@link Mqtt5Subscriber} if available, otherwise empty
     */
    public Optional<Mqtt5Subscriber> sub() {
        return Optional.ofNullable(subscriberTracker.getService());
    }

    /**
     * Opens the service trackers to begin tracking MQTT publisher and subscriber services.
     */
    public void open() {
        publisherTracker.open();
        subscriberTracker.open();
    }

    /**
     * Closes the service trackers, stopping tracking of MQTT publisher and subscriber services.
     */
    public void close() {
        publisherTracker.close();
        subscriberTracker.close();
    }
}