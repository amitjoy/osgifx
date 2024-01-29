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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Message;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;

public final class SimpleMqtt5Subscriber implements Mqtt5Subscriber {

    private final BundleContext bundleContext;

    public SimpleMqtt5Subscriber(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public PushStream<Mqtt5Message> subscribe(final String channel) {
        final PushStreamProvider                  provider = new PushStreamProvider();
        final SimplePushEventSource<Mqtt5Message> source   = acquirePushEventSource(provider);

        final ServiceTracker<MessageSubscription, MessageSubscription> subscriberTracker = new ServiceTracker<MessageSubscription, MessageSubscription>(bundleContext,
                                                                                                                                                        MessageSubscription.class,
                                                                                                                                                        null) {
            @Override
            public MessageSubscription addingService(final ServiceReference<MessageSubscription> reference) {
                final MessageSubscription service = super.addingService(reference);
                service.subscribe(channel).forEach(msg -> {
                    final Mqtt5Message message = convertMessage(msg);
                    source.publish(message);
                });
                return service;
            }
        };
        subscriberTracker.open();
        return provider.createStream(source);
    }

    private Mqtt5Message convertMessage(final Message msg) {
        final Mqtt5Message message = new Mqtt5Message();

        message.payload = msg.payload();
        message.channel = msg.getContext().getChannel();

        return message;
    }

    private SimplePushEventSource<Mqtt5Message> acquirePushEventSource(final PushStreamProvider provider) {
        return InterruptSafe.execute(() -> provider.createSimpleEventSource(Mqtt5Message.class));
    }

}
