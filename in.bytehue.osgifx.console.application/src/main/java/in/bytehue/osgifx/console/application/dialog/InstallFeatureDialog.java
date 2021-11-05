package in.bytehue.osgifx.console.application.dialog;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.application.dialog.InstallFeatureDialog.SelectedFeaturesDTO;
import in.bytehue.osgifx.console.application.fxml.controller.InstallFeatureDialogController;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class InstallFeatureDialog extends Dialog<SelectedFeaturesDTO> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext context;

    public void init() {
        final DialogPane dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());

        dialogPane.setHeaderText("External Feature Install");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/feature-install.png").toString()));

        final ButtonType loginButtonType = new ButtonType("Install", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        final Node dialogContent = Fx.loadFXML(loader, context, "/fxml/install-feature-dialog.fxml");
        dialogPane.setContent(dialogContent);

        final InstallFeatureDialogController controller = (InstallFeatureDialogController) loader.getController();
        setResultConverter(dialogButton -> {
            final ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getSelectedFeatures() : null;
        });
    }

    public static class SelectedFeaturesDTO {
        public List<File> features;
        public String     archiveURL;
    }

}
