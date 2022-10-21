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
package com.osgifx.console.agent.starter;

import static com.osgifx.console.agent.Agent.AGENT_SERVER_PORT_KEY;
import static com.osgifx.console.agent.Agent.DEFAULT_PORT;
import static com.osgifx.console.agent.Agent.PORT_PATTERN;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.di.Module;
import com.osgifx.console.agent.link.RemoteRPC;
import com.osgifx.console.agent.provider.AgentServer;
import com.osgifx.console.agent.provider.ClassloaderLeakDetector;
import com.osgifx.console.supervisor.Supervisor;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator extends Thread implements BundleActivator {

    private Module                  module;
    private ServerSocket            server;
    private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

    @Override
    public void start(final BundleContext context) throws Exception {
        module = new Module(context);
        module.di().getInstance(ClassloaderLeakDetector.class).start();

        // Get the specified port in the framework properties
        String port = context.getProperty(AGENT_SERVER_PORT_KEY);
        if (port == null) {
            port = DEFAULT_PORT + "";
        }

        // Check if it matches the specification of host:port
        final Matcher m = PORT_PATTERN.matcher(port);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid port specification in property '" + AGENT_SERVER_PORT_KEY + "', expects [<host>:]<port> : " + port);
        }

        // if the host is not set, use localhost
        String host = m.group(1);
        if (host == null) {
            host = "localhost";
        } else {
            port = m.group(2);
        }

        System.err.println("OSGi.fx Agent Host: " + host + ":" + port);

        final int p = Integer.parseInt(port);
        server = "*".equals(host) ? new ServerSocket(p) : new ServerSocket(p, 3, InetAddress.getByName(host));
        start();
    }

    @Override
    public void run() {

        try {
            while (!isInterrupted()) {
                try {
                    final Socket socket = server.accept();
                    // timeout to get interrupts
                    socket.setSoTimeout(1000);

                    module.start();

                    // create a new agent, and link it up.
                    final AgentServer sa = new AgentServer(module.di());
                    agents.add(sa);

                    final RemoteRPC<Agent, Supervisor> remoteRPC = new RemoteRPC<Agent, Supervisor>(Supervisor.class, sa, socket) {
                        @Override
                        public void close() throws IOException {
                            agents.remove(sa);
                            module.stop();
                            super.close();
                        }
                    };
                    module.bindInstance(AgentServer.class, sa);
                    module.bindInstance(RemoteRPC.class, remoteRPC);
                    module.bindInstance(Supervisor.class, remoteRPC.getRemote());

                    sa.setEndpoint(remoteRPC);
                    remoteRPC.run();
                } catch (final Exception e) {
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (final Throwable t) {
            t.printStackTrace(System.err);
            throw t;
        } finally {
            close(server);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        interrupt();
        close(server);
        agents.forEach(this::close);
        module.di().getInstance(ClassloaderLeakDetector.class).stop();
    }

    private Throwable close(final Closeable in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (final Throwable e) {
            return e;
        }
        return null;
    }

}
