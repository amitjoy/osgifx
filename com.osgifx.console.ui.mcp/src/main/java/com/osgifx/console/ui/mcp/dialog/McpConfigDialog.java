package com.osgifx.console.ui.mcp.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

public final class McpConfigDialog extends Dialog<Void> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @OSGiBundle
    private BundleContext context;

    public void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);

        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);

        final var content = Fx.loadFXML(loader, context, "/fxml/mcp-config-dialog.fxml");
        dialogPane.setContent(content);
    }

}
