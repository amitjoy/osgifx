package in.bytehue.osgifx.console.ui.services;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public final class ServicesFxUI {

    @Inject
    @Named("in.bytehue.osgifx.console.ui.services")
    private BundleContext context;

    @PostConstruct
    public void postConstruct(final VBox parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Focus
    public void focus(final VBox parent, @LocalInstance final FXMLLoader loader) {
        parent.getChildren().clear();
        createControls(parent, loader);
    }

    private void createControls(final VBox parent, final FXMLLoader loader) {
        final Node tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
        parent.getChildren().add(tabContent);
    }

}