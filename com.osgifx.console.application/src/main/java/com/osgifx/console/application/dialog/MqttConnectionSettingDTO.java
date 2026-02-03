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
package com.osgifx.console.application.dialog;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.MoreObjects;

public final class MqttConnectionSettingDTO {

    public String id;
    public String name;
    public String clientId;
    public String server;
    public int    port;
    public int    timeout;
    public String username;
    public String password;
    public String tokenConfig;
    public String pubTopic;
    public String subTopic;
    public String lwtTopic;

    public MqttConnectionSettingDTO() {
        // needed for GSON
    }

    public MqttConnectionSettingDTO(final String name,
                                    final String clientId,
                                    final String server,
                                    final int port,
                                    final int timeout,
                                    final String username,
                                    final String password,
                                    final String tokenConfig,
                                    final String pubTopic,
                                    final String subTopic,
                                    final String lwtTopic) {
        this(UUID.randomUUID().toString(), name, clientId, server, port, timeout, username, password, tokenConfig,
             pubTopic, subTopic, lwtTopic);
    }

    public MqttConnectionSettingDTO(final String id,
                                    final String name,
                                    final String clientId,
                                    final String server,
                                    final int port,
                                    final int timeout,
                                    final String username,
                                    final String password,
                                    final String tokenConfig,
                                    final String pubTopic,
                                    final String subTopic,
                                    final String lwtTopic) {
        this.id          = id;
        this.name        = name;
        this.clientId    = clientId;
        this.server      = server;
        this.port        = port;
        this.timeout     = timeout;
        this.username    = username;
        this.password    = password;
        this.tokenConfig = tokenConfig;
        this.pubTopic    = pubTopic;
        this.subTopic    = subTopic;
        this.lwtTopic    = lwtTopic;
    }

    @Override
    public int hashCode() {
        // @formatter:off
        return new HashCodeBuilder()
                         .append(id)
                         .append(name)
                         .append(clientId)
                         .append(server)
                         .append(port)
                         .append(timeout)
                         .append(username)
                         .append(password)
                         .append(tokenConfig)
                         .append(pubTopic)
                         .append(subTopic)
                         .append(lwtTopic)
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
        final var other = (MqttConnectionSettingDTO) obj;
        // @formatter:off
        return new EqualsBuilder()
                           .append(id, other.id)
                           .append(name, other.name)
                           .append(clientId, other.clientId)
                           .append(server, other.server)
                           .append(port, other.port)
                           .append(timeout, other.timeout)
                           .append(username, other.username)
                           .append(password, other.password)
                           .append(tokenConfig, other.tokenConfig)
                           .append(pubTopic, other.pubTopic)
                           .append(subTopic, other.subTopic)
                           .append(lwtTopic, other.lwtTopic)
                       .isEquals();
        // @formatter:on
    }

    @Override
    public String toString() {
        // @formatter:off
        return MoreObjects.toStringHelper(getClass())
                               .add("id", id)
                               .add("name", name)
                               .add("clientId", clientId)
                               .add("server", server)
                               .add("port", port)
                               .add("timeout", timeout)
                               .add("username", username)
                               .add("tokenConfig", tokenConfig)
                               .add("pubTopic", pubTopic)
                               .add("subTopic", subTopic)
                               .add("lwtTopic", lwtTopic)
                          .toString();
        // @formatter:on
    }

}
