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
package com.osgifx.console.agent.rpc;

import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.tracker.ServiceTracker;

public final class PubSub {

    private final BundleContext                                            bundleContext;
    private final ServiceTracker<MessagePublisher, MessagePublisher>       publisherTracker;
    private final ServiceTracker<MessageSubscription, MessageSubscription> subscriberTracker;

    public PubSub(final BundleContext bundleContext, final Consumer<MessageSubscription> subscriberCallback) {
        this.bundleContext = bundleContext;
        publisherTracker   = new ServiceTracker<>(bundleContext, MessagePublisher.class, null);
        subscriberTracker  = new ServiceTracker<MessageSubscription, MessageSubscription>(bundleContext,
                                                                                          MessageSubscription.class,
                                                                                          null) {
                               @Override
                               public MessageSubscription addingService(final ServiceReference<MessageSubscription> reference) {
                                   final MessageSubscription service = super.addingService(reference);
                                   subscriberCallback.accept(service);
                                   return service;
                               }
                           };
    }

    public Optional<MessagePublisher> pub() {
        return Optional.ofNullable(publisherTracker.getService());
    }

    public Optional<MessageSubscription> sub() {
        return Optional.ofNullable(subscriberTracker.getService());
    }

    public Optional<MessageContextBuilder> msgCtx() {
        final ServiceReference<MessageContextBuilder> ref            = bundleContext
                .getServiceReference(MessageContextBuilder.class);
        final ServiceObjects<MessageContextBuilder>   serviceObjects = bundleContext.getServiceObjects(ref);
        return Optional.ofNullable(serviceObjects.getService());
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
