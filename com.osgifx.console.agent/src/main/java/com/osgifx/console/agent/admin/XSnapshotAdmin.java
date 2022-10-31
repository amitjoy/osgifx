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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.Agent.HEAPDUMP_LOCATION_KEY;

import java.io.File;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.SnapshotDTO;
import com.osgifx.console.agent.dto.XSnapshotDTO;
import com.osgifx.console.agent.provider.AgentServer;

import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;
import jakarta.inject.Inject;

public final class XSnapshotAdmin {

    private static final Encoder JSON_CODEC = new JSONCodec().enc();

    private final Agent agent;

    @Inject
    public XSnapshotAdmin(final AgentServer agent) {
        this.agent = agent;
    }

    public XSnapshotDTO snapshot() {
        final File   location     = getLocation();
        final String fileName     = "osgi_fx_" + System.currentTimeMillis() + ".json";
        final File   snapshotFile = new File(location, fileName);

        final SnapshotDTO dto = new SnapshotDTO();

        dto.bundles              = agent.getAllBundles();
        dto.components           = agent.getAllComponents();
        dto.configuations        = agent.getAllConfigurations();
        dto.properties           = agent.getAllProperties();
        dto.services             = agent.getAllServices();
        dto.threads              = agent.getAllThreads();
        dto.dmtNodes             = agent.readDmtNode(".");
        dto.memoryInfo           = agent.getMemoryInfo();
        dto.roles                = agent.getAllRoles();
        dto.healthChecks         = agent.getAllHealthChecks();
        dto.classloaderLeaks     = agent.getClassloaderLeaks();
        dto.httpComponents       = agent.getHttpComponents();
        dto.bundleLoggerContexts = agent.getBundleLoggerContexts();
        dto.heapUsage            = agent.getHeapUsage();

        try {
            JSON_CODEC.indent("\t").writeDefaults().to(snapshotFile).put(dto).close();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        final XSnapshotDTO snapshot = new XSnapshotDTO();

        snapshot.size     = snapshotFile.length();
        snapshot.location = snapshotFile.getAbsolutePath();

        return snapshot;

    }

    private static File getLocation() {
        final String externalLocation = System.getProperty(HEAPDUMP_LOCATION_KEY);
        if (externalLocation != null) {
            final File file = new File(externalLocation);
            file.mkdirs();
            return file;
        }
        final File file = new File("./snapshots");
        file.mkdirs();
        return file;
    }

}
