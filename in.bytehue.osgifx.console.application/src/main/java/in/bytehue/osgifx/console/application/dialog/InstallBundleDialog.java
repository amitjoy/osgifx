package in.bytehue.osgifx.console.application.dialog;

import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.application.fxml.controller.InstallBundleDialogController;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class InstallBundleDialog extends Dialog<InstallBundleDTO> {

    public InstallBundleDialog(final FXMLLoader loader, final BundleContext context) {
        final DialogPane dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);

        dialogPane.setHeaderText("Remote Bundle Install");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/remote-install.png").toString()));

        final ButtonType loginButtonType = new ButtonType("Install", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        final Node dialogContent = Fx.loadFXML(loader, context, "/fxml/install-bundle-dialog.fxml");
        dialogPane.setContent(dialogContent);

        final InstallBundleDialogController controller = (InstallBundleDialogController) loader.getController();
        setResultConverter(dialogButton -> controller.getInstallDTO());
    }

}