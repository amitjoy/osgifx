package in.bytehue.osgifx.console.core.supervisor;

import org.osgi.service.component.annotations.Component;

import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;

@Component
public final class ConsoleSupervisorProvider extends AgentSupervisor<Supervisor, ConsoleAgent> implements ConsoleSupervisor, Supervisor {

    @Override
    public void event(final Event e) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean stdout(final String out) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        return false;
    }

    @Override
    public void connect(final String host, final int port, final int timeout) throws Exception {
        // in the connection dialog, we have to show progress dialog while connecting to the runtime
        super.connect(ConsoleAgent.class, this, host, port, timeout);
    }

}
