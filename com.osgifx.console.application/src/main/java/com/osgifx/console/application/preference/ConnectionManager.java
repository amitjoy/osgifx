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

import static com.osgifx.console.constants.FxConstants.WORKSPACE_PROPERTY;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osgifx.console.application.dialog.MqttConnectionSettingDTO;
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;

/**
 * Manages persistence of socket and MQTT connection configurations to a JSON
 * file ({@code connections.json}) located in the workspace directory identified
 * by the {@code osgifx.ws} system property.
 *
 * <p>
 * Passwords are <em>not</em> persisted here; they remain handled by
 * {@link CredentialManager} in the encrypted {@code credentials.json} file.
 * </p>
 */
@Component(service = ConnectionManager.class)
public final class ConnectionManager {

    private static final Path CONNECTIONS_FILE = Paths.get(System.getProperty(WORKSPACE_PROPERTY), "connections.json");

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        logger.atInfo().log("ConnectionManager initialized - connections file: %s", CONNECTIONS_FILE);
    }

    /**
     * Saves the given socket connections to the {@code connections.json} file,
     * preserving any existing MQTT connections already stored in the file.
     *
     * @param socketConnections the socket connections to persist; must not be
     *            {@code null}
     */
    public void saveSocketConnections(final List<SocketConnectionSettingDTO> socketConnections) {
        try {
            final var dto = loadConnectionsDTO();
            dto.socketConnections = new ArrayList<>(socketConnections);
            persist(dto);
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to save socket connections");
        }
    }

    /**
     * Saves the given MQTT connections to the {@code connections.json} file,
     * preserving any existing socket connections already stored in the file.
     *
     * @param mqttConnections the MQTT connections to persist; must not be
     *            {@code null}
     */
    public void saveMqttConnections(final List<MqttConnectionSettingDTO> mqttConnections) {
        try {
            final var dto = loadConnectionsDTO();
            dto.mqttConnections = new ArrayList<>(mqttConnections);
            persist(dto);
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to save MQTT connections");
        }
    }

    /**
     * Loads and returns the socket connections from the {@code connections.json}
     * file. Returns an empty list if the file does not exist or is corrupt.
     *
     * @return mutable list of {@link SocketConnectionSettingDTO}; never
     *         {@code null}
     */
    public List<SocketConnectionSettingDTO> loadSocketConnections() {
        final var dto = loadConnectionsDTO();
        return dto.socketConnections != null ? dto.socketConnections : new ArrayList<>();
    }

    /**
     * Loads and returns the MQTT connections from the {@code connections.json}
     * file. Returns an empty list if the file does not exist or is corrupt.
     *
     * @return mutable list of {@link MqttConnectionSettingDTO}; never {@code null}
     */
    public List<MqttConnectionSettingDTO> loadMqttConnections() {
        final var dto = loadConnectionsDTO();
        return dto.mqttConnections != null ? dto.mqttConnections : new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ConnectionsDTO loadConnectionsDTO() {
        if (!Files.exists(CONNECTIONS_FILE)) {
            return new ConnectionsDTO();
        }
        try (var reader = Files.newBufferedReader(CONNECTIONS_FILE, StandardCharsets.UTF_8)) {
            final var dto = createGSON().get().fromJson(reader, ConnectionsDTO.class);
            return dto != null ? dto : new ConnectionsDTO();
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to read connections file - returning empty");
            return new ConnectionsDTO();
        }
    }

    private void persist(final ConnectionsDTO dto) throws Exception {
        Files.createDirectories(CONNECTIONS_FILE.getParent());
        try (var writer = Files.newBufferedWriter(CONNECTIONS_FILE, StandardCharsets.UTF_8)) {
            createGSON().get().toJson(dto, writer);
        }
        logger.atInfo().log("Connections persisted to %s", CONNECTIONS_FILE);
    }

    /**
     * Internal wrapper DTO representing the full contents of
     * {@code connections.json}.
     */
    private static final class ConnectionsDTO {
        List<SocketConnectionSettingDTO> socketConnections = new ArrayList<>();
        List<MqttConnectionSettingDTO>   mqttConnections   = new ArrayList<>();
    }

    private Supplier<Gson> createGSON() {
        return () -> new GsonBuilder().setPrettyPrinting().create();
    }

}
