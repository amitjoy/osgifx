package in.bytehue.osgifx.console.core.data.provider;

import static in.bytehue.osgifx.console.util.fx.ConsoleFXHelper.makeNullSafe;

import java.util.function.Consumer;

import org.eclipse.fx.core.log.Logger;
import org.eclipse.fx.core.log.LoggerFactory;
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
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.ObservableQueue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
public final class RuntimeDataProvider implements DataProvider, Consumer<XEventDTO> {

    private Logger        logger;
    @Reference
    private Supervisor    supervisor;
    @Reference
    private LoggerFactory factory;

    private final ObservableList<XBundleDTO>        bundles        = FXCollections.observableArrayList();
    private final ObservableList<XServiceDTO>       services       = FXCollections.observableArrayList();
    private final ObservableList<XComponentDTO>     components     = FXCollections.observableArrayList();
    private final ObservableList<XConfigurationDTO> configurations = FXCollections.observableArrayList();
    private final ObservableList<XPropertyDTO>      properties     = FXCollections.observableArrayList();
    private final ObservableList<XThreadDTO>        threads        = FXCollections.observableArrayList();
    private final ObservableQueue<XEventDTO>        events         = new ObservableQueue<>(EvictingQueue.create(200));

    void activate() {
        logger = factory.createLogger(getClass().getName());
    }

    @Override
    public synchronized ObservableList<XBundleDTO> bundles() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        bundles.clear();
        bundles.addAll(makeNullSafe(agent.getAllBundles()));
        return bundles;
    }

    @Override
    public synchronized ObservableList<XServiceDTO> services() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        services.clear();
        services.addAll(makeNullSafe(agent.getAllServices()));
        return services;
    }

    @Override
    public synchronized ObservableList<XComponentDTO> components() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        components.clear();
        components.addAll(makeNullSafe(agent.getAllComponents()));
        return components;
    }

    @Override
    public synchronized ObservableList<XConfigurationDTO> configurations() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        configurations.clear();
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
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        properties.clear();
        properties.addAll(makeNullSafe(agent.getAllProperties()));
        return properties;
    }

    @Override
    public synchronized ObservableList<XThreadDTO> threads() {
        final Agent agent = supervisor.getAgent();
        if (agent == null) {
            logger.warning("Agent is not connected");
            return FXCollections.emptyObservableList();
        }
        threads.clear();
        threads.addAll(makeNullSafe(agent.getAllThreads()));
        return threads;
    }

}
