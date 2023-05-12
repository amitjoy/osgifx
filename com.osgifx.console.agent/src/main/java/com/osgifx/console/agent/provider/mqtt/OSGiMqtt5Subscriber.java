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
package com.osgifx.console.agent.provider.mqtt;

import org.osgi.framework.BundleContext;
import org.osgi.util.pushstream.PushStream;

import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Message;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;

public final class OSGiMqtt5Subscriber implements Mqtt5Subscriber {

    private final BundleContext bundleContext;

    public OSGiMqtt5Subscriber(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public PushStream<Mqtt5Message> subscribe(final String channel) {
        // TODO Auto-generated method stub
        return null;
    }

}
