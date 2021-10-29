package in.bytehue.osgifx.console.data.manager;

import static in.bytehue.osgifx.console.util.fx.ConsoleFxHelper.makeNullSafe;

import java.util.function.Consumer;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.EvictingQueue;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import in.bytehue.osgifx.console.agent.dto.XThreadDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.ObservableQueue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
public final class RuntimeDataProvider implements DataProvider, Consumer<XEventDTO> {

    @Reference
    private LoggerFactory factory;
    @Reference
    private Supervisor    supervisor;
    private FluentLogger  logger;

    private final ObservableQueue<XEventDTO> events = new ObservableQueue<>(EvictingQueue.create(200));

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public synchronized ObservableList<XBundleDTO> bundles() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XBundleDTO> bundles = FXCollections.observableArrayList();
        bundles.addAll(makeNullSafe(agent.getAllBundles()));
        return bundles;
    }

    @Override
    public synchronized ObservableList<XServiceDTO> services() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XServiceDTO> services = FXCollections.observableArrayList();
        services.addAll(makeNullSafe(agent.getAllServices()));
        return services;
    }

    @Override
    public synchronized ObservableList<XComponentDTO> components() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XComponentDTO> components = FXCollections.observableArrayList();
        components.addAll(makeNullSafe(agent.getAllComponents()));
        return components;
    }

    @Override
    public synchronized ObservableList<XConfigurationDTO> configurations() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XConfigurationDTO> configurations = FXCollections.observableArrayList();
        configurations.addAll(makeNullSafe(agent.getAllConfigurations()));
        return configurations;
    }

    @Override
    public synchronized ObservableList<XEventDTO> events() {
        return events;
    }

    @Override
    public synchronized void accept(final XEventDTO event) {
        events.add(event);
    }

    @Override
    public synchronized ObservableList<XPropertyDTO> properties() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XPropertyDTO> properties = FXCollections.observableArrayList();
        properties.addAll(makeNullSafe(agent.getAllProperties()));
        return properties;
    }

    @Override
    public synchronized ObservableList<XThreadDTO> threads() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        final ObservableList<XThreadDTO> threads = FXCollections.observableArrayList();
        threads.addAll(makeNullSafe(agent.getAllThreads()));
        return threads;
    }

}