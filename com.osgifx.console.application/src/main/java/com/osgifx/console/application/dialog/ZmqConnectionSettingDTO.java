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
package com.osgifx.console.application.dialog;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.MoreObjects;

public final class ZmqConnectionSettingDTO {

    public String id;
    public String name;
    public String host;
    public int    timeout;
    public int    eventPort;
    public int    commandPort;

    public ZmqConnectionSettingDTO() {
        // needed for GSON
    }

    public ZmqConnectionSettingDTO(final String name,
                                   final String host,
                                   final int commandPort,
                                   final int eventPort,
                                   final int timeout) {
        this(UUID.randomUUID().toString(), name, host, commandPort, eventPort, timeout);
    }

    public ZmqConnectionSettingDTO(final String id,
                                   final String name,
                                   final String host,
                                   final int commandPort,
                                   final int eventPort,
                                   final int timeout) {
        this.id          = id;
        this.name        = name;
        this.host        = host;
        this.commandPort = commandPort;
        this.eventPort   = eventPort;
        this.timeout     = timeout;
    }

    @Override
    public int hashCode() {
        // @formatter:off
        return new HashCodeBuilder()
                         .append(id)
                         .append(name)
                         .append(host)
                         .append(commandPort)
                         .append(eventPort)
                         .append(timeout)
                     .toHashCode();
        // @formatter:on
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final var other = (ZmqConnectionSettingDTO) obj;
        // @formatter:off
        return new EqualsBuilder()
                           .append(id, other.id)
                           .append(name, other.name)
                           .append(host, other.host)
                           .append(timeout, other.timeout)
                           .append(eventPort, other.eventPort)
                           .append(commandPort, other.commandPort)
                       .isEquals();
        // @formatter:on
    }

    @Override
    public String toString() {
        // @formatter:off
        return MoreObjects.toStringHelper(getClass())
                               .add("id", id)
                               .add("name", name)
                               .add("host", host)
                               .add("timeout", timeout)
                               .add("port", eventPort)
                               .add("port", commandPort)
                          .toString();
        // @formatter:on
    }

}
