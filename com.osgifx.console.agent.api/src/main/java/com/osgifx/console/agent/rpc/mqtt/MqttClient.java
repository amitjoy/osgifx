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
package com.osgifx.console.agent.rpc.mqtt;

import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;

public final class MqttClient {

    private final ServiceTracker<Mqtt5Publisher, Mqtt5Publisher>   publisherTracker;
    private final ServiceTracker<Mqtt5Subscriber, Mqtt5Subscriber> subscriberTracker;

    public MqttClient(final BundleContext bundleContext, final Consumer<Mqtt5Subscriber> subscriberCallback) {
        publisherTracker  = new ServiceTracker<>(bundleContext, Mqtt5Publisher.class, null);
        subscriberTracker = new ServiceTracker<Mqtt5Subscriber, Mqtt5Subscriber>(bundleContext, Mqtt5Subscriber.class,
                                                                                 null) {
                              @Override
                              public Mqtt5Subscriber addingService(final ServiceReference<Mqtt5Subscriber> reference) {
                                  final Mqtt5Subscriber service = super.addingService(reference);
                                  subscriberCallback.accept(service);
                                  return service;
                              }
                          };
    }

    public Optional<Mqtt5Publisher> pub() {
        return Optional.ofNullable(publisherTracker.getService());
    }

    public Optional<Mqtt5Subscriber> sub() {
        return Optional.ofNullable(subscriberTracker.getService());
    }

    public void open() {
        publisherTracker.open();
        subscriberTracker.open();
    }

    public void close() {
        publisherTracker.close();
        subscriberTracker.close();
    }
}
