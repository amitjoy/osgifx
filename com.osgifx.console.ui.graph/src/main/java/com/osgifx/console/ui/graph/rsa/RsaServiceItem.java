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
package com.osgifx.console.ui.graph.rsa;

import com.osgifx.console.agent.dto.XRemoteServiceDTO;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class RsaServiceItem {

    private final XRemoteServiceDTO remoteService;
    private final BooleanProperty   selected = new SimpleBooleanProperty();

    public RsaServiceItem(final XRemoteServiceDTO remoteService) {
        this.remoteService = remoteService;
    }

    public XRemoteServiceDTO getRemoteService() {
        return remoteService;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(final boolean isSelected) {
        selected.set(isSelected);
    }

    @Override
    public String toString() {
        final var direction = remoteService.direction != null ? remoteService.direction.name() : "UNKNOWN";
        final var iface     = remoteService.objectClass != null && !remoteService.objectClass.isEmpty()
                ? remoteService.objectClass.get(0)
                : "unknown";
        return String.format("[%s] %s", direction, iface);
    }

}
