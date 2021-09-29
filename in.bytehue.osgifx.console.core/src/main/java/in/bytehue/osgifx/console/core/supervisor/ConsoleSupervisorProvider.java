package in.bytehue.osgifx.console.core.supervisor;

import org.osgi.service.component.annotations.Component;

import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;

@Component
public final class ConsoleSupervisorProvider extends AgentSupervisor<Supervisor, ConsoleAgent>
        implements ConsoleSupervisor {

    @Override
    public void event(final Event e) throws Exception {
        super.connect(ConsoleAgent.class, this, "192.168.2.136", 1450, 5000);
    }

    @Override
    public boolean stdout(final String out) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    void active() {
        System.out.println(getAgent());
    }

}
