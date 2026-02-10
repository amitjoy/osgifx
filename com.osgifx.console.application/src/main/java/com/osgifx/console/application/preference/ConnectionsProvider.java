/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.application.dialog.MqttConnectionSettingDTO;
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component(service = ConnectionsProvider.class)
public final class ConnectionsProvider {

    private final ReentrantLock                               lock              = new ReentrantLock();
    private final ObservableList<SocketConnectionSettingDTO> socketConnections = FXCollections.observableArrayList();
    private final ObservableList<MqttConnectionSettingDTO>   mqttConnections   = FXCollections.observableArrayList();

    public void addSocketConnection(final SocketConnectionSettingDTO connection) {
        lock.lock();
        try {
            socketConnections.add(connection);
        } finally {
            lock.unlock();
        }
    }

    public void updateSocketConnection(final SocketConnectionSettingDTO connection) {
        lock.lock();
        try {
            // @formatter:off
            final var index = IntStream.range(0, socketConnections.size())
                                       .filter(i -> socketConnections.get(i).id.equals(connection.id))
                                       .findFirst()
                                       .orElse(-1);
            // @formatter:on
            if (index != -1) {
                socketConnections.set(index, connection);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeSocketConnection(final SocketConnectionSettingDTO connection) {
        lock.lock();
        try {
            socketConnections.remove(connection);
        } finally {
            lock.unlock();
        }
    }

    public void addSocketConnections(final List<SocketConnectionSettingDTO> connections) {
        lock.lock();
        try {
            socketConnections.addAll(connections);
        } finally {
            lock.unlock();
        }
    }

    public void addMqttConnection(final MqttConnectionSettingDTO connection) {
        lock.lock();
        try {
            mqttConnections.add(connection);
        } finally {
            lock.unlock();
        }
    }

    public void updateMqttConnection(final MqttConnectionSettingDTO connection) {
        lock.lock();
        try {
            // @formatter:off
            final var index = IntStream.range(0, mqttConnections.size())
                                       .filter(i -> mqttConnections.get(i).id.equals(connection.id))
                                       .findFirst()
                                       .orElse(-1);
            // @formatter:on
            if (index != -1) {
                mqttConnections.set(index, connection);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeMqttConnection(final MqttConnectionSettingDTO connection) {
        lock.lock();
        try {
            mqttConnections.remove(connection);
        } finally {
            lock.unlock();
        }
    }

    public void addMqttConnections(final List<MqttConnectionSettingDTO> connections) {
        lock.lock();
        try {
            mqttConnections.addAll(connections);
        } finally {
            lock.unlock();
        }
    }

    public ObservableList<SocketConnectionSettingDTO> getSocketConnections() {
        lock.lock();
        try {
            return socketConnections;
        } finally {
            lock.unlock();
        }
    }

    public ObservableList<MqttConnectionSettingDTO> getMqttConnections() {
        lock.lock();
        try {
            return mqttConnections;
        } finally {
            lock.unlock();
        }
    }

}
