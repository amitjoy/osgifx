package in.bytehue.osgifx.console.ui.components;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.fx.core.URLUtils;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class ComponentsFxUI {

    @Inject
    @Log
    private Logger logger;

    @PostConstruct
    public void postConstruct(final VBox parent, @LocalInstance final FXMLLoader loader) {
        loader.setLocation(
                URLUtils.createUrl("platform:/plugin/in.bytehue.osgifx.console.ui.components/fxml/sample.fxml"));
        try {
            final Node n = loader.load();
            parent.getChildren().add(n);
        } catch (final IOException e) {
            logger.error("Failed to load 'PersonForm.fxml'", e);
        }
    }

}