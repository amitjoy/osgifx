package in.bytehue.osgifx.console.ui.graph;

import static in.bytehue.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static in.bytehue.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.StatusBar;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import in.bytehue.osgifx.console.util.fx.FxDialog;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class GraphFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @Named("in.bytehue.osgifx.console.ui.graph")
    private BundleContext     context;
    @Inject
    private MPart             part;
    @Inject
    private EPartService      partService;
    @Inject
    private ThreadSynchronize threadSync;
    private final StatusBar   statusBar    = new StatusBar();
    private final MaskerPane  progressPane = new MaskerPane();

    private static final String BUNDLES_GRAPH_TYPE    = "Bundles";
    private static final String COMPONENTS_GRAPH_TYPE = "Components";

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
        logger.atDebug().log("Graph part has been initialized");
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent( //
            @UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data, //
            final BorderPane parent, //
            @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent connected event received");
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent disconnected event received");
        partService.hidePart(part);
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        Fx.initStatusBar(parent, statusBar);

        threadSync.asyncExec(() -> {
            FxDialog.showChoiceDialog("Select Graph Generation Type", getClass().getClassLoader(), "/graphic/images/graph.png", type -> {
                final Task<?> task = new Task<Void>() {
                    Node tabContent = null;

                    @Override
                    protected Void call() throws Exception {
                        threadSync.asyncExec(() -> parent.setCenter(progressPane));
                        if (BUNDLES_GRAPH_TYPE.equalsIgnoreCase(type)) {
                            tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content-for-bundles.fxml");
                        } else {
                            tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content-for-components.fxml");
                        }
                        progressPane.setVisible(false);
                        threadSync.asyncExec(() -> parent.setCenter(tabContent));
                        return null;
                    }
                };

                final Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            }, () -> partService.hidePart(part), BUNDLES_GRAPH_TYPE, BUNDLES_GRAPH_TYPE, COMPONENTS_GRAPH_TYPE);
        });

    }

}