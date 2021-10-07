package in.bytehue.osgifx.console.ui.components;

import static in.bytehue.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPICS;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public final class ComponentsFxUI {

    @Inject
    @Named("in.bytehue.osgifx.console.ui.components")
    private BundleContext context;

    @PostConstruct
    public void postConstruct(final VBox parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Focus
    public void focus(final VBox parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateControlsOnEvent( //
            @UIEventTopic(COMPONENT_ACTION_EVENT_TOPICS) final String data, //
            final VBox parent, //
            @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    private void createControls(final VBox parent, final FXMLLoader loader) {
        parent.getChildren().clear();
        final Node tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
        parent.getChildren().add(tabContent);
    }

}