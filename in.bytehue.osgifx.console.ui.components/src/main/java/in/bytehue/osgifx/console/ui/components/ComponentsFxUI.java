package in.bytehue.osgifx.console.ui.components;

import static in.bytehue.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPICS;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.MaskerPane;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class ComponentsFxUI {

    @Inject
    @Named("in.bytehue.osgifx.console.ui.components")
    private BundleContext context;

    private final MaskerPane progressPane = new MaskerPane();

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Focus
    public void focus(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateControlsOnEvent( //
            @UIEventTopic(COMPONENT_ACTION_EVENT_TOPICS) final String data, //
            final BorderPane parent, //
            @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        final Task<?> task = new Task<Void>() {

            Node tabContent = null;

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);
                tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                parent.getChildren().clear();
                parent.setCenter(tabContent);
                progressPane.setVisible(false);
            }
        };
        parent.getChildren().clear();
        parent.setCenter(progressPane);

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

}