package in.bytehue.osgifx.console.application.fxml.controller;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.HyperlinkLabel;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;
import org.osgi.framework.BundleContext;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public final class AboutApplicationDialogController {

    @Log
    @Inject
    private Logger         logger;
    @FXML
    private Text           appName;
    @FXML
    private ImageView      header;
    @FXML
    private GridPane       content;
    @FXML
    private HyperlinkLabel hyperlink;
    @Inject
    private Application    jfxApplication;
    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext  bundleContext;

    @FXML
    public void initialize() {
        appName.setText("OSGi.fx Console (Version " + bundleContext.getBundle().getVersion() + ")");
        hyperlink.setOnAction(event -> {
            final Hyperlink link           = (Hyperlink) event.getSource();
            final String    eclipseWebLink = link.getText();
            jfxApplication.getHostServices().showDocument(eclipseWebLink);
        });
        logger.info("FXML controller (" + getClass() + ") has been initialized");
    }

    public Node getHeader() {
        return header;
    }

    public Node getBody() {
        return content;
    }

}
