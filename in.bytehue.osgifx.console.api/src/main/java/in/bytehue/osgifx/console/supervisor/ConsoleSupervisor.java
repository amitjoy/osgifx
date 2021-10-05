package in.bytehue.osgifx.console.supervisor;

import org.osgi.annotation.versioning.ProviderType;

import in.bytehue.osgifx.console.agent.ConsoleAgent;

/**
 * A Supervisor handles the initiating side of a session with a remote agent.
 * The methods defined in this interface are intended to be called by the remote
 * agent, not the initiator. I.e. this is not the interface the initiator will
 * use to control the session.
 */
@ProviderType
public interface ConsoleSupervisor {

    /** The topic where an event will be sent after the agent gets connected */
    String AGENT_CONNECTED_EVENT_TOPIC = "fx/console/agent/connected";

    /**
     * Returns the associated agent instance to control the remote OSGi framework
     *
     * @return the associated agent instance
     */
    ConsoleAgent getAgent();

    /**
     * Connects to the specific host and port using the provided timeout in connection
     *
     * @param host the host name
     * @param port the port address
     * @param timeout the timeout in milliseconds
     *
     * @throws Exception if any issue occurs during connection
     */
    void connect(String host, int port, int timeout) throws Exception;

    /**
     * Returns the currently connected hostname
     *
     * @return the currently connected hostname
     */
    String getHost();

    /**
     * Returns the currently connected port
     *
     * @return the currently connected port
     */
    int getPort();

    /**
     * Returns the currently connected timeout
     *
     * @return the currently connected timeout
     */
    int getTimeout();

}