package in.bytehue.osgifx.console.application.dialog;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.application.dialog.CheckForUpdatesDialog.SelectedFeaturesForUpdateDTO;
import in.bytehue.osgifx.console.application.fxml.controller.CheckForUpdatesDialogController;
import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class CheckForUpdatesDialog extends Dialog<SelectedFeaturesForUpdateDTO> {

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

        dialogPane.setHeaderText("Update Installed Feature(s)");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/feature-install.png").toString()));

        final ButtonType loginButtonType = new ButtonType("Update", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        final Node dialogContent = Fx.loadFXML(loader, context, "/fxml/check-for-updates-dialog.fxml");
        dialogPane.setContent(dialogContent);

        final CheckForUpdatesDialogController controller = (CheckForUpdatesDialogController) loader.getController();
        setResultConverter(dialogButton -> {
            final ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getSelectedFeatures() : null;
        });
    }

    public static class SelectedFeaturesForUpdateDTO {
        public List<FeatureDTO> features;
    }

    public void initFeaturesToBeUpdated(final Collection<FeatureDTO> tobeUpdatedFeatures) {
        final CheckForUpdatesDialogController controller = (CheckForUpdatesDialogController) loader.getController();
        controller.setFeaturesToBeUpdated(tobeUpdatedFeatures);
    }

}
