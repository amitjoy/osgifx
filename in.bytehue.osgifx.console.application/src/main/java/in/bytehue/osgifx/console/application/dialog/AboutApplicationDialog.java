package in.bytehue.osgifx.console.application.dialog;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class AboutApplicationDialog extends Dialog<Void> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext context;

    public void init() {
        final DialogPane dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.setPrefHeight(200);
        dialogPane.setPrefWidth(400);
        dialogPane.getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());

        dialogPane.setHeaderText("Remote Bundle Install");
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/about.png").toString()));

        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final Node content = Fx.loadFXML(loader, context, "/fxml/about-application-dialog.fxml");

        dialogPane.setHeaderText("About OSGi.fx Console");
        dialogPane.setContent(content);
    }

}
