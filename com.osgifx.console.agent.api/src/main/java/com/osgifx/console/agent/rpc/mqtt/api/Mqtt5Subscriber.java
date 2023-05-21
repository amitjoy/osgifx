/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.agent.rpc.mqtt.api;

import org.osgi.util.pushstream.PushStream;

/**
 * The consumer interface to plugin custom functionality for consuming MQTT messages
 */
public interface Mqtt5Subscriber {

    /**
     * Subscribe to the specified topic
     *
     * @param topic the topic to subscribe to
     * @return {@link PushStream} instance for the subscription
     */
    PushStream<Mqtt5Message> subscribe(String channel);

}