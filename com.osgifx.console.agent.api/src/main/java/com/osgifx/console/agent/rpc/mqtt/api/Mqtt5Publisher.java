/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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

/**
 * Represents a publisher for MQTT version 5 messages. This interface allows for 
 * the implementation of custom functionality to publish MQTT messages to a specified 
 * topic.
 * <p>
 * Implementations of this interface are responsible for publishing a given message 
 * to the associated MQTT topic defined within the message.
 * </p>
 */
public interface Mqtt5Publisher {

    /**
     * Publishes the specified MQTT message to the topic defined in the 
     * {@link Mqtt5Message}.
     * <p>
     * The topic and other parameters necessary for the message are encapsulated 
     * within the {@link Mqtt5Message} object.
     * </p>
     *
     * @param message the {@link Mqtt5Message} to be published, containing the topic
     *                and message payload
     * @throws IllegalArgumentException if the message is null or invalid
     */
    void publish(Mqtt5Message message);

}