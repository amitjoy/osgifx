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
package com.osgifx.console.agent.provider;

import static com.osgifx.console.agent.Agent.AGENT_SERVER_PORT_KEY;
import static com.osgifx.console.agent.Agent.DEFAULT_PORT;
import static com.osgifx.console.agent.Agent.PORT_P;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.supervisor.Supervisor;

import aQute.lib.io.IO;
import aQute.remote.util.Link;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator extends Thread implements BundleActivator {
	private ServerSocket  server;
	private BundleContext context;

	private ClassloaderLeakDetector classloaderLeakDetector;

	private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

	@Override
	public void start(final BundleContext context) throws Exception {
		this.context            = context;
		classloaderLeakDetector = new ClassloaderLeakDetector();
		classloaderLeakDetector.start(context);

		// Get the specified port in the framework properties
		String port = context.getProperty(AGENT_SERVER_PORT_KEY);
		if (port == null) {
			port = DEFAULT_PORT + "";
		}

		// Check if it matches the specification of host:port
		final Matcher m = PORT_P.matcher(port);
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

	/**
	 * Main dispatcher loop
	 */
	@Override
	public void run() {

		try {
			while (!isInterrupted()) {
				try {
					final Socket socket = server.accept();

					// timeout to get interrupts
					socket.setSoTimeout(1000);

					// create a new agent, and link it up.
					final AgentServer sa = new AgentServer(context, classloaderLeakDetector);
					agents.add(sa);
					final Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, sa, socket) {
						@Override
						public void close() throws IOException {
							agents.remove(sa);
							super.close();
						}
					};
					sa.setLink(link);
					// initialize OSGi eventing if available
					final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
					if (isEventAdminAvailable) {
						final Dictionary<String, Object> properties = new Hashtable<>();
						properties.put("event.topics", "*");
						context.registerService("org.osgi.service.event.EventHandler", new OSGiEventHandler(link.getRemote()), properties);
					}
					// initialize OSGi logging if available
					final boolean isLogAvailable = PackageWirings.isLogWired(context);
					if (isLogAvailable) {
						final OSGiLogListener logListener = new OSGiLogListener(link.getRemote());
						trackLogReader(logListener);
					}
					link.run();
				} catch (final Exception e) {
				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace(System.err);
			throw t;
		} finally {
			IO.close(server);
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		interrupt();
		IO.close(server);
		agents.forEach(IO::close);
		classloaderLeakDetector.stop();
	}

	private void trackLogReader(final OSGiLogListener logListener) {
		final ServiceTracker<Object, Object> logReaderTracker = new ServiceTracker<Object, Object>(context,
		        "org.osgi.service.log.LogReaderService", null) {

			@Override
			public Object addingService(final ServiceReference<Object> reference) {
				final boolean isLogAvailable = PackageWirings.isLogWired(context);
				final Object  service        = super.addingService(reference);
				if (isLogAvailable) {
					XLogReaderAdmin.register(service, logListener);
				}
				return service;
			}

			@Override
			public void removedService(final ServiceReference<Object> reference, final Object service) {
				final boolean isLogAvailable = PackageWirings.isLogWired(context);
				if (isLogAvailable) {
					XLogReaderAdmin.unregister(service, logListener);
				}
			}
		};
		logReaderTracker.open();
	}

}
