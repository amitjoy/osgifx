/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.application.dialog.ConnectionSettingDTO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component(service = ConnectionsProvider.class)
public final class ConnectionsProvider {

    private final ObservableList<ConnectionSettingDTO> connections = FXCollections.observableArrayList();

    public synchronized void addConnection(final ConnectionSettingDTO connection) {
        connections.add(connection);
    }

    public synchronized void removeConnection(final ConnectionSettingDTO connection) {
        connections.remove(connection);
    }

    public synchronized void addConnections(final List<ConnectionSettingDTO> connections) {
        this.connections.addAll(connections);
    }

    public synchronized ObservableList<ConnectionSettingDTO> getConnections() {
        return connections;
    }

}
