/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * This class collaborates with the Envoy part of this design. After the envoy
 * has installed the -runpath it will reflectively call this class to create a
 * framework and run an {@link AgentServer}.
 */
public class AgentDispatcher {

    //
    // We keep a descriptor for each created framework by its name.
    //
    static List<Descriptor> descriptors = new CopyOnWriteArrayList<>();

    // public because of testing
    public static class Descriptor implements Closeable {
        public AtomicBoolean            closed     = new AtomicBoolean(false);
        public List<AgentServer>        servers    = new CopyOnWriteArrayList<>();
        public Framework                framework;
        public Map<String, Object>      configuration;
        public File                     storage;
        public File                     shaCache;
        public String                   name;
        public List<BundleActivator>    activators = new ArrayList<>();
        public StartLevelRuntimeHandler startlevels;

        @Override
        public void close() throws IOException {
            if (closed.getAndSet(true)) {
                return;
            }

            for (final AgentServer as : servers) {
                try {
                    as.close();
                } catch (final Exception e) {
                    // ignore
                }
            }
            for (final BundleActivator ba : activators) {
                try {
                    ba.stop(framework.getBundleContext());
                } catch (final Exception e) {
                    // ignore
                }
            }
            try {
                framework.stop();
            } catch (final BundleException e) {
                // ignore
            }
        }
    }

    /**
     * Close
     */
    public static void close() throws IOException {
        for (final Descriptor descriptor : descriptors) {
            descriptor.close();
        }
        for (final Descriptor descriptor : descriptors) {
            try {
                descriptor.framework.waitForStop(2000);
            } catch (final InterruptedException e) {
                // ignore
            }
        }
    }

}
