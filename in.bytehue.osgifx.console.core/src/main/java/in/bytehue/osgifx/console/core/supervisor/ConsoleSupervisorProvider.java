package in.bytehue.osgifx.console.core.supervisor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;

@Component
public final class ConsoleSupervisorProvider extends AgentSupervisor<Supervisor, ConsoleAgent> implements ConsoleSupervisor {

    @Reference
    private EventAdmin eventAdmin;

    private static final int INITIAL_DELAY = 5;
    private static final int DELAY         = 20;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
        if (getAgent() != null) {
            executor.scheduleWithFixedDelay(this::checkConnection, INITIAL_DELAY, DELAY, TimeUnit.SECONDS);
        }
    }

    private void checkConnection() {
        final ConsoleAgent agent = getAgent();
        if (!agent.ping()) {
            final Map<String, Object> properties = new HashMap<>();
            properties.put(IEventBroker.DATA, LocalDateTime.now().toString());

            final org.osgi.service.event.Event event = new org.osgi.service.event.Event(DISCONNECTION_TOPIC, properties);
            eventAdmin.sendEvent(event);
        }
    }

}
