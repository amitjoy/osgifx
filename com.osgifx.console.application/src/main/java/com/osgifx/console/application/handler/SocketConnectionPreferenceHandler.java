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
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

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
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;
import com.osgifx.console.application.preference.ConnectionsProvider;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public final class SocketConnectionPreferenceHandler {

    @Log
    @Inject
    private FluentLogger        logger;
    @Inject
    @Preference(nodePath = "osgi.fx.connections", key = "socket.settings", defaultValue = "")
    private Value<String>       settings;
    @Inject
    private ConnectionsProvider connectionsProvider;

    @PostConstruct
    public void init() {
        connectionsProvider.addSocketConnections(getStoredValues());
    }

    @Execute
    public void execute(@Named("id") final String id,
                        @Named("name") final String name,
                        @Named("host") final String host,
                        @Named("port") final String port,
                        @Named("timeout") final String timeout,
                        @Named("type") final String type,
                        @Named("truststore") @Optional final String truststore,
                        @Named("truststorePassword") @Optional final String truststorePassword) {

        final var gson        = new Gson();
        final var connections = getStoredValues();
        final var dto         = new SocketConnectionSettingDTO(id, name, host, Ints.tryParse(port),
                                                               Ints.tryParse(timeout), truststore, truststorePassword);

        if ("ADD".equals(type)) {
            connections.add(dto);
            connectionsProvider.addSocketConnection(dto);
            logger.atInfo().log("New connection has been added: %s", dto);
        } else if ("EDIT".equals(type)) {
            // @formatter:off
            final var index = IntStream.range(0, connections.size())
                                       .filter(i -> connections.get(i).id.equals(dto.id))
                                       .findFirst()
                                       .orElse(-1);
            // @formatter:on
            if (index != -1) {
                connections.set(index, dto);
            }
            connectionsProvider.updateSocketConnection(dto);
            logger.atInfo().log("Existing connection has been updated: %s", dto);
        } else if ("REMOVE".equals(type)) {
            connections.remove(dto);
            connectionsProvider.removeSocketConnection(dto);
            logger.atInfo().log("Existing connection has been deleted: %s", dto);
        } else {
            logger.atWarning().log("Cannot execute command with type '%s'", type);
        }
        settings.publish(gson.toJson(connections));
    }

    private List<SocketConnectionSettingDTO> getStoredValues() {
        final var                        gson        = new Gson();
        List<SocketConnectionSettingDTO> connections = gson.fromJson(settings.getValue(),
                new TypeToken<List<SocketConnectionSettingDTO>>() {
                                                             }.getType());
        if (connections == null) {
            connections = Lists.newArrayList();
        }
        return connections;
    }

}
