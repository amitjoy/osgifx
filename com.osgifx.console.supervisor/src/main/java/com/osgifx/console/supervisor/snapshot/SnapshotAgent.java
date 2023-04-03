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
package com.osgifx.console.supervisor.snapshot;

import static com.osgifx.console.supervisor.snapshot.SnapshotAgent.PID;

import java.io.FileReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.fx.core.ExceptionUtils;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRoleDTO.Type;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.dto.SnapshotDTO;

@Component(service = { SnapshotAgent.class, Agent.class }, configurationPid = PID)
public final class SnapshotAgent implements Agent {

    public static final String PID = "com.osgifx.console.snapshot";

    @interface Configuration {
        String location();
    }

    private volatile SnapshotDTO snapshotDTO;

    @Activate
    @Modified
    void init(final Configuration configuration) {
        if (configuration.location() == null) {
            return;
        }
        final var gson = new Gson();
        try {
            final var reader = new JsonReader(new FileReader(configuration.location()));
            snapshotDTO = gson.fromJson(reader, SnapshotDTO.class);
        } catch (final Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    @Override
    public BundleDTO installWithData(final String location, final byte[] data, final int startLevel) throws Exception {
        return null;
    }

    @Override
    public XResultDTO installWithMultipleData(final Collection<byte[]> data, final int startLevel) {
        return null;
    }

    @Override
    public BundleDTO installFromURL(final String location, final String url) throws Exception {
        return null;
    }

    @Override
    public String start(final long... id) throws Exception {
        return null;
    }

    @Override
    public String stop(final long... id) throws Exception {
        return null;
    }

    @Override
    public String uninstall(final long... id) throws Exception {
        return null;
    }

    @Override
    public List<BundleRevisionDTO> getBundleRevisons(final long... bundleId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public boolean redirect(final int port) throws Exception {
        return false;
    }

    @Override
    public boolean stdin(final String s) throws Exception {
        return false;
    }

    @Override
    public String execGogoCommand(final String command) throws Exception {
        return null;
    }

    @Override
    public String execCliCommand(final String command) {
        return null;
    }

    @Override
    public void abort() throws Exception {
    }

    @Override
    public boolean ping() {
        return true;
    }

    @Override
    public List<XBundleDTO> getAllBundles() {
        return snapshotDTO.bundles;
    }

    @Override
    public List<XComponentDTO> getAllComponents() {
        return snapshotDTO.components;
    }

    @Override
    public List<XConfigurationDTO> getAllConfigurations() {
        return snapshotDTO.configurations;
    }

    @Override
    public List<XPropertyDTO> getAllProperties() {
        return snapshotDTO.properties;
    }

    @Override
    public List<XServiceDTO> getAllServices() {
        return snapshotDTO.services;
    }

    @Override
    public List<XThreadDTO> getAllThreads() {
        return snapshotDTO.threads;
    }

    @Override
    public XDmtNodeDTO readDmtNode(final String rootURI) {
        return snapshotDTO.dmtNodes;
    }

    @Override
    public XResultDTO updateDmtNode(final String uri, final Object value, final DmtDataType format) {
        return null;
    }

    @Override
    public XResultDTO updateBundleLoggerContext(final String bsn, final Map<String, String> logLevels) {
        return null;
    }

    @Override
    public XResultDTO enableComponentByName(final String name) {
        return null;
    }

    @Override
    public XResultDTO enableComponentById(final long id) {
        return null;
    }

    @Override
    public XResultDTO disableComponentByName(final String name) {
        return null;
    }

    @Override
    public XResultDTO disableComponentById(final long id) {
        return null;
    }

    @Override
    public Map<String, XResultDTO> createOrUpdateConfigurations(final Map<String, Map<String, Object>> configurations) {
        return Collections.emptyMap();
    }

    @Override
    public XResultDTO createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
        return null;
    }

    @Override
    public XResultDTO deleteConfiguration(final String pid) {
        return null;
    }

    @Override
    public XResultDTO createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
        return null;
    }

    @Override
    public XResultDTO sendEvent(final String topic, final List<ConfigValue> properties) {
        return null;
    }

    @Override
    public XResultDTO postEvent(final String topic, final List<ConfigValue> properties) {
        return null;
    }

    @Override
    public XMemoryInfoDTO getMemoryInfo() {
        return snapshotDTO.memoryInfo;
    }

    @Override
    public Set<String> getGogoCommands() {
        return Collections.emptySet();
    }

    @Override
    public XResultDTO createRole(final String name, final Type type) {
        return null;
    }

    @Override
    public XResultDTO updateRole(final XRoleDTO dto) {
        return null;
    }

    @Override
    public XResultDTO removeRole(final String name) {
        return null;
    }

    @Override
    public List<XRoleDTO> getAllRoles() {
        return snapshotDTO.roles;
    }

    @Override
    public List<XHealthCheckDTO> getAllHealthChecks() {
        return snapshotDTO.healthChecks;
    }

    @Override
    public List<XHealthCheckResultDTO> executeHealthChecks(final List<String> tags, final List<String> names) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> executeExtension(final String name, final Map<String, Object> context) {
        return Collections.emptyMap();
    }

    @Override
    public Set<XBundleDTO> getClassloaderLeaks() {
        return snapshotDTO.classloaderLeaks;
    }

    @Override
    public List<XHttpComponentDTO> getHttpComponents() {
        return snapshotDTO.httpComponents;
    }

    @Override
    public List<XBundleLoggerContextDTO> getBundleLoggerContexts() {
        return snapshotDTO.bundleLoggerContexts;
    }

    @Override
    public XHeapUsageDTO getHeapUsage() {
        return snapshotDTO.heapUsage;
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
        return snapshotDTO.runtime;
    }

    @Override
    public byte[] heapdump() throws Exception {
        return null;
    }

    @Override
    public void gc() {
        // nothing to do
    }

    @Override
    public boolean isReceivingLogEnabled() {
        return false;
    }

    @Override
    public void enableReceivingLog() {
        // nothing to do
    }

    @Override
    public void disableReceivingLog() {
        // nothing to do
    }

    @Override
    public boolean isReceivingEventEnabled() {
        return false;
    }

    @Override
    public void enableReceivingEvent() {
        // nothing to do
    }

    @Override
    public void disableReceivingEvent() {
        // nothing to do
    }

}
