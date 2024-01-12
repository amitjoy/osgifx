/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc.mqtt;

import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Message;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;

public final class SimpleMqtt5Publisher implements Mqtt5Publisher {

    private final BundleContext                                      bundleContext;
    private final ServiceTracker<MessagePublisher, MessagePublisher> publisherTracker;

    public SimpleMqtt5Publisher(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        publisherTracker   = new ServiceTracker<>(bundleContext, MessagePublisher.class, null);
        publisherTracker.open();
    }

    @Override
    public void publish(final Mqtt5Message message) {
        Optional.ofNullable(publisherTracker.getService()).ifPresent(pub -> {
            final Optional<MessageContextBuilder> msgCtx = newMessageContextBuilder();
            if (!msgCtx.isPresent()) {
                throw new IllegalStateException("Required service 'MessageContextBuilder' is unavailable");
            }
            final Message msg = msgCtx.get().channel(message.channel).content(message.payload).buildMessage();
            pub.publish(msg);
        });
    }

    private Optional<MessageContextBuilder> newMessageContextBuilder() {
        final ServiceReference<MessageContextBuilder> ref            = bundleContext
                .getServiceReference(MessageContextBuilder.class);
        final ServiceObjects<MessageContextBuilder>   serviceObjects = bundleContext.getServiceObjects(ref);
        return Optional.ofNullable(serviceObjects.getService());
    }

}
