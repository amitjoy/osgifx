package in.bytehue.osgifx.console.application.dialog;

import static in.bytehue.osgifx.console.constants.FxConstants.STANDARD_CSS;

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

public final class FeaturesViewDialog extends Dialog<Void> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext context;

    public void init() {
        final DialogPane dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Installed Feature(s)");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/features.png").toString()));

        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final Node dialogContent = Fx.loadFXML(loader, context, "/fxml/view-features-dialog.fxml");
        dialogPane.setContent(dialogContent);
    }

}
