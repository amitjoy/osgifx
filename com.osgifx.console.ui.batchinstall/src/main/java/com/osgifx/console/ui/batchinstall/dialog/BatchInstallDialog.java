/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.batchinstall.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonBar.ButtonData.OK_DONE;
import static javafx.scene.control.ButtonType.CANCEL;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.osgifx.console.ui.batchinstall.dialog.BatchInstallDialog.ArtifactDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class BatchInstallDialog extends Dialog<List<ArtifactDTO>> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @OSGiBundle
    private BundleContext context;
    private File          directory;

    public void init(final File directory) {
        this.directory = directory;

        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText(directory.getAbsolutePath());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/directory.png").toString()));

        final var installButtonType = new ButtonType("Install", OK_DONE);
        dialogPane.getButtonTypes().addAll(installButtonType, CANCEL);

        final var dialogContent = Fx.loadFXML(loader, context, "/fxml/batch-install-dialog.fxml");
        dialogPane.setContent(dialogContent);

        final var controller              = (BatchInstallDialogController) loader.getController();
        final var targetItemsProperty     = controller.targetItemsProperty();
        final var targetItemsListProperty = new SimpleListProperty<ArtifactDTO>();

        controller.initArtifacts(directory);

        targetItemsListProperty.bind(targetItemsProperty);
        final BooleanProperty isItemSelected = new SimpleBooleanProperty();

        isItemSelected.bind(targetItemsListProperty.emptyProperty());

        dialogPane.lookupButton(installButtonType).disableProperty().bind(isItemSelected);
        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == OK_DONE ? controller.getSelectedArtifacts() : null;
        });
    }

    public void traverseDirectoryForFiles() {
        final var controller = (BatchInstallDialogController) loader.getController();
        controller.initArtifacts(directory);
    }

    public static record ArtifactDTO(File file, boolean isConfiguration) {
    }

}
