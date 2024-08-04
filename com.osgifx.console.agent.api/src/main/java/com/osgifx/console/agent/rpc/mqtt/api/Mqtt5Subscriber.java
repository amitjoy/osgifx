/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc.mqtt.api;

import org.osgi.util.pushstream.PushStream;

/**
 * Represents a subscriber for MQTT version 5 messages. This interface allows for 
 * the implementation of custom functionality to consume messages from specified 
 * MQTT topics.
 * <p>
 * Implementations of this interface are responsible for subscribing to a given 
 * topic and providing a mechanism to receive and process messages asynchronously 
 * using a {@link PushStream}.
 * </p>
 */
public interface Mqtt5Subscriber {

    /**
     * Subscribes to the specified MQTT topic and returns a {@link PushStream} that 
     * allows asynchronous processing of incoming messages from that topic.
     * <p>
     * The topic parameter identifies the channel to which the subscriber will listen 
     * for incoming messages.
     * </p>
     *
     * @param topic the MQTT topic to subscribe to, must not be null or empty
     * @return a {@link PushStream} instance representing the stream of messages 
     *         received from the subscribed topic
     * @throws IllegalArgumentException if the topic is null or empty
     */
    PushStream<Mqtt5Message> subscribe(String topic);

}