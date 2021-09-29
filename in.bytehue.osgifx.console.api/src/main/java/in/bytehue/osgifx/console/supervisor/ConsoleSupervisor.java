package in.bytehue.osgifx.console.supervisor;

import org.osgi.annotation.versioning.ProviderType;

import aQute.remote.api.Supervisor;
import in.bytehue.osgifx.console.agent.ConsoleAgent;

/**
 * A Supervisor handles the initiating side of a session with a remote agent.
 * The methods defined in this interface are intended to be called by the remote
 * agent, not the initiator. I.e. this is not the interface the initiator will
 * use to control the session.
 */
@ProviderType
public interface ConsoleSupervisor extends Supervisor {

    /**
     * Returns the associated agent instance to control the remote OSGi framework
     *
     * @return the associated agent instance
     */
    ConsoleAgent getAgent();

}