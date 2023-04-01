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
package com.osgifx.console.application.handler;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.preferences.Preference;
import org.eclipse.fx.core.preferences.Value;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osgifx.console.application.dialog.MqttConnectionSettingDTO;
import com.osgifx.console.application.preference.ConnectionsProvider;

public final class MqttConnectionPreferenceHandler {

    @Log
    @Inject
    private FluentLogger        logger;
    @Inject
    @Preference(nodePath = "osgi.fx.connections", key = "mqtt.settings", defaultValue = "")
    private Value<String>       settings;
    @Inject
    private ConnectionsProvider connectionsProvider;

    @PostConstruct
    public void init() {
        connectionsProvider.addMqttConnections(getStoredValues());
    }

    @Execute
    public void execute(@Named("name") final String name,
                        @Named("clientId") final String clientId,
                        @Named("server") final String server,
                        @Named("port") final String port,
                        @Named("timeout") final String timeout,
                        @Named("type") final String type,
                        @Named("username") @Optional final String username,
                        @Named("password") @Optional final String password,
                        @Named("pubTopic") @Optional final String pubTopic,
                        @Named("subTopic") @Optional final String subTopic) {

        final var gson        = new Gson();
        final var connections = getStoredValues();
        final var dto         = new MqttConnectionSettingDTO(name, clientId, server, Ints.tryParse(port),
                                                             Ints.tryParse(timeout), username, password, pubTopic,
                                                             subTopic);

        if ("ADD".equals(type)) {
            connections.add(dto);
            connectionsProvider.addMqttConnection(dto);
            logger.atInfo().log("New connection has been added: %s", dto);
        } else if ("REMOVE".equals(type)) {
            connections.remove(dto);
            connectionsProvider.removeMqttConnection(dto);
            logger.atInfo().log("Existing connection has been deleted: %s", dto);
        } else {
            logger.atWarning().log("Cannot execute command with type '%s'", type);
        }
        settings.publish(gson.toJson(connections));
    }

    private List<MqttConnectionSettingDTO> getStoredValues() {
        final var                      gson        = new Gson();
        List<MqttConnectionSettingDTO> connections = gson.fromJson(settings.getValue(),
                new TypeToken<List<MqttConnectionSettingDTO>>() {
                                                           }.getType());
        if (connections == null) {
            connections = Lists.newArrayList();
        }
        return connections;
    }

}
