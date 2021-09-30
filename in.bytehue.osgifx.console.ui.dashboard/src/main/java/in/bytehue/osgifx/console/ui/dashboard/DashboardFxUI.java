package in.bytehue.osgifx.console.ui.dashboard;

import java.io.IOException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;
import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class DashboardFxUI {

    @Log
    @Inject
    private Logger logger;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.dashboard")
    private BundleContext context;

    @PostConstruct
    public void postConstruct(final VBox parent, @LocalInstance final FXMLLoader loader) {
        final URL fxml = context.getBundle().getResource("/fxml/tab-content.fxml");
        loader.setLocation(fxml);
        try {
            final Node n = loader.load();
            parent.getChildren().add(n);
        } catch (final IOException e) {
            logger.error("Failed to load 'PersonForm.fxml'", e);
        }
    }

}