/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.application.preference;

import java.util.List;
import java.util.stream.IntStream;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.application.dialog.MqttConnectionSettingDTO;
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component(service = ConnectionsProvider.class)
public final class ConnectionsProvider {

    private final ObservableList<SocketConnectionSettingDTO> socketConnections = FXCollections.observableArrayList();
    private final ObservableList<MqttConnectionSettingDTO>   mqttConnections   = FXCollections.observableArrayList();

    public synchronized void addSocketConnection(final SocketConnectionSettingDTO connection) {
        socketConnections.add(connection);
    }

    public synchronized void updateSocketConnection(final SocketConnectionSettingDTO connection) {
     // @formatter:off
        final var index = IntStream.range(0, socketConnections.size())
                                   .filter(i -> socketConnections.get(i).id.equals(connection.id))
                                   .findFirst()
                                   .orElse(-1);
        // @formatter:on
        if (index != -1) {
            socketConnections.set(index, connection);
        }
    }

    public synchronized void removeSocketConnection(final SocketConnectionSettingDTO connection) {
        socketConnections.remove(connection);
    }

    public synchronized void addSocketConnections(final List<SocketConnectionSettingDTO> connections) {
        socketConnections.addAll(connections);
    }

    public synchronized void addMqttConnection(final MqttConnectionSettingDTO connection) {
        mqttConnections.add(connection);
    }

    public synchronized void updateMqttConnection(final MqttConnectionSettingDTO connection) {
        // @formatter:off
        final var index = IntStream.range(0, mqttConnections.size())
                                   .filter(i -> mqttConnections.get(i).id.equals(connection.id))
                                   .findFirst()
                                   .orElse(-1);
        // @formatter:on
        if (index != -1) {
            mqttConnections.set(index, connection);
        }
    }

    public synchronized void removeMqttConnection(final MqttConnectionSettingDTO connection) {
        mqttConnections.remove(connection);
    }

    public synchronized void addMqttConnections(final List<MqttConnectionSettingDTO> connections) {
        mqttConnections.addAll(connections);
    }

    public synchronized ObservableList<SocketConnectionSettingDTO> getSocketConnections() {
        return socketConnections;
    }

    public synchronized ObservableList<MqttConnectionSettingDTO> getMqttConnections() {
        return mqttConnections;
    }

}
