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
package com.osgifx.console.agent.starter;

import static com.osgifx.console.agent.Agent.AGENT_SOCKET_PORT_KEY;
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_PORT_PATTERN;
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_SECURE_COMMUNICATION_KEY;
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;

public final class SocketContext {

    private String              host;
    private int                 port;
    private final BundleContext bundleContext;
    private final FluentLogger  logger = LoggerFactory.getFluentLogger(getClass());

    public SocketContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        extractSpec();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public ServerSocket getSocket() throws Exception {
        final String secureCommunicationEnabled = bundleContext.getProperty(AGENT_SOCKET_SECURE_COMMUNICATION_KEY);
        if (Boolean.parseBoolean(secureCommunicationEnabled)) {
            logger.atInfo().msg("Secure communication enabled").log();
            final String sslContextFilter = bundleContext
                    .getProperty(AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY);
            if (sslContextFilter == null) {
                logger.atWarn().msg("SSL context LDAP filter is not set for secure communication").log();
                final SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                return "*".equals(host) ? ssf.createServerSocket(port)
                        : ssf.createServerSocket(port, 3, InetAddress.getByName(host));
            }
            final Collection<ServiceReference<SSLContext>> sslContextRefs = bundleContext.getServiceReferences(
                    SSLContext.class,
                    "(" + AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY + "=" + sslContextFilter + ")");
            if (sslContextRefs == null || sslContextRefs.isEmpty()) {
                throw new RuntimeException("Custom SSLContext service for OSGi.fx secure agent communication is not available");
            }
            final ServiceReference<SSLContext> serviceRef = sslContextRefs.iterator().next();
            final SSLContext                   sslContext = bundleContext.getService(serviceRef);
            final SSLServerSocketFactory       ssf        = sslContext.getServerSocketFactory();

            return "*".equals(host) ? ssf.createServerSocket(port)
                    : ssf.createServerSocket(port, 3, InetAddress.getByName(host));
        }
        return "*".equals(host) ? new ServerSocket(port) : new ServerSocket(port, 3, InetAddress.getByName(host));
    }

    private void extractSpec() {
        final String portKey    = bundleContext.getProperty(AGENT_SOCKET_PORT_KEY);
        final String portKeyOld = bundleContext.getProperty("osgi.fx.agent.port"); // backward compatibility
        if (portKey == null && portKeyOld == null) {
            throw new IllegalArgumentException("Socket port not defined");
        }
        final String  portSpec = Optional.ofNullable(portKey).orElse(portKeyOld);
        final Matcher m        = AGENT_SOCKET_PORT_PATTERN.matcher(portSpec);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid port specification in property '" + AGENT_SOCKET_PORT_KEY
                    + "', expects [<host>:]<port> : " + port);
        }
        host = m.group(1);
        if (host == null) {
            host = "localhost";
        } else {
            port = Integer.parseInt(m.group(2));
        }
    }

}
