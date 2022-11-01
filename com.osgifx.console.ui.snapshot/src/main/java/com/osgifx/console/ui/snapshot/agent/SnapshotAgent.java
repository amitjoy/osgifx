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
package com.osgifx.console.ui.snapshot.agent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapdumpDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRoleDTO.Type;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XSnapshotDTO;
import com.osgifx.console.agent.dto.XThreadDTO;

@Component
public final class SnapshotAgent implements Agent {

    @Override
    public BundleDTO installWithData(final String location, final byte[] data, final int startLevel) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO installWithMultipleData(final Collection<byte[]> data, final int startLevel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO installFromURL(final String location, final String url) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String start(final long... id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String stop(final long... id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String uninstall(final long... id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<BundleRevisionDTO> getBundleRevisons(final long... bundleId) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean redirect(final int port) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stdin(final String s) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String execGogoCommand(final String command) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String execCliCommand(final String command) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void abort() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean ping() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<XBundleDTO> getAllBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XComponentDTO> getAllComponents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XConfigurationDTO> getAllConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XPropertyDTO> getAllProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XServiceDTO> getAllServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XThreadDTO> getAllThreads() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XDmtNodeDTO readDmtNode(final String rootURI) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO updateDmtNode(final String uri, final Object value, final DmtDataType format) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO updateBundleLoggerContext(final String bsn, final Map<String, String> logLevels) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO enableComponentByName(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO enableComponentById(final long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO disableComponentByName(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO disableComponentById(final long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, XResultDTO> createOrUpdateConfigurations(final Map<String, Map<String, Object>> configurations) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO deleteConfiguration(final String pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO sendEvent(final String topic, final List<ConfigValue> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO postEvent(final String topic, final List<ConfigValue> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XMemoryInfoDTO getMemoryInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getGogoCommands() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO createRole(final String name, final Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO updateRole(final XRoleDTO dto) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XResultDTO removeRole(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XRoleDTO> getAllRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XHealthCheckDTO> getAllHealthChecks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XHealthCheckResultDTO> executeHealthChecks(final List<String> tags, final List<String> names) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> executeExtension(final String name, final Map<String, Object> context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<XBundleDTO> getClassloaderLeaks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XHttpComponentDTO> getHttpComponents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<XBundleLoggerContextDTO> getBundleLoggerContexts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XHeapUsageDTO getHeapUsage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XHeapdumpDTO heapdump() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XSnapshotDTO snapshot() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void gc() {
        // TODO Auto-generated method stub

    }

}
