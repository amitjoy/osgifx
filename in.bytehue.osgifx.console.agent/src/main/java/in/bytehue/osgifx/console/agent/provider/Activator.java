package in.bytehue.osgifx.console.agent.provider;

import static aQute.remote.api.Agent.AGENT_SERVER_PORT_KEY;
import static aQute.remote.api.Agent.PORT_P;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.File;
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

import aQute.lib.io.IO;
import aQute.remote.agent.AgentServer;
import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator extends Thread implements BundleActivator {

    private File          cache;
    private ServerSocket  server;
    private BundleContext context;

    private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

    @Override
    public void start(final BundleContext context) throws Exception {
        this.context = context;

        // Get the specified port in the framework properties
        String port = context.getProperty(AGENT_SERVER_PORT_KEY);
        if (port == null) {
            port = Agent.DEFAULT_PORT + "";
        }

        // Check if it matches the specification of host:port
        final Matcher m = PORT_P.matcher(port);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid port specification in property aQute.agent.server.port, expects [<host>:]<port> : "
                            + port);
        }

        // See if the host was set, otherwise use localhost for security reasons
        String host = m.group(1);
        if (host == null) {
            host = "localhost";
        } else {
            port = m.group(2);
        }
        System.err.println("Host " + host + " " + port);

        // Get the SHA cache root file, which will be shared by all agents for this process.
        cache = context.getDataFile("shacache");

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

                    // Use a time out so we get interrupts and can do some checks
                    socket.setSoTimeout(1000);

                    // Create a new agent, and link it up.
                    final AgentServer sa = new ConsoleAgentServer("<>", context, cache);
                    agents.add(sa);
                    final Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, sa, socket) {
                        @Override
                        public void close() throws IOException {
                            agents.remove(sa);
                            super.close();
                        }
                    };
                    sa.setLink(link);
                    link.run();
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

    /**
     * Shutdown any agents & the server socket.
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        interrupt();
        IO.close(server);
        for (final AgentServer sa : agents) {
            IO.close(sa);
        }
    }

}
