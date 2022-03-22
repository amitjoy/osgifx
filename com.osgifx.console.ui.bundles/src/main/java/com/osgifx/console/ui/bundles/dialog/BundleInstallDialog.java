/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.bundles.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class BundleInstallDialog extends Dialog<BundleInstallDTO> {

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

		dialogPane.setHeaderText("Remote Bundle Install");
		dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/remote-install.png").toString()));

		final var loginButtonType = new ButtonType("Install", ButtonData.OK_DONE);
		dialogPane.getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

		final var dialogContent = Fx.loadFXML(loader, context, "/fxml/install-bundle-dialog.fxml");
		dialogPane.setContent(dialogContent);

		final var controller = (BundleInstallDialogController) loader.getController();
		setResultConverter(dialogButton -> {
			final var data = dialogButton == null ? null : dialogButton.getButtonData();
			return data == ButtonData.OK_DONE ? controller.getInstallDTO() : null;
		});
	}

}
