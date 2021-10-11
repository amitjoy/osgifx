package in.bytehue.osgifx.console.core.supervisor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;

import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;

@Component
public final class ConsoleSupervisorProvider extends AgentSupervisor<Supervisor, ConsoleAgent> implements ConsoleSupervisor, Supervisor {

    private String host;
    private int    port;
    private int    timeout;

    private final List<Consumer<XEventDTO>> eventConsumers = new CopyOnWriteArrayList<>();

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
        super.connect(ConsoleAgent.class, this, host, port, timeout);
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;
        System.setProperty(CONNECTED_AGENT, host + ":" + port);
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void onOSGiEvent(final XEventDTO event) {
        eventConsumers.forEach(consumer -> consumer.accept(event));
    }

    @Override
    public void addOSGiEventConsumer(final Consumer<XEventDTO> eventConsumer) {
        eventConsumers.add(eventConsumer);
    }

    @Override
    public void removeOSGiEventConsumer(final Consumer<XEventDTO> eventConsumer) {
        eventConsumers.remove(eventConsumer);
    }

}
