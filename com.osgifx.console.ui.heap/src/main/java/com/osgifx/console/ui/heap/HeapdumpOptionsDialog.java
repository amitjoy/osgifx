/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.ui.heap;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonBar.ButtonData.OK_DONE;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.spi.payload.LargePayloadHandler;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

public final class HeapdumpOptionsDialog extends Dialog<HeapdumpOptionsDialog.HeapdumpOption> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @OSGiBundle
    private BundleContext context;

    public enum HeapdumpOption {
        USE_HANDLER,
        USE_RPC,
        STORE_LOCALLY
    }

    public static final class HeapdumpStatistics {
        public final long                estimatedUncompressedSize;
        public final long                estimatedCompressedSize;
        public final boolean             isMqttTransport;
        public final boolean             rpcAvailable;
        public final LargePayloadHandler handler;

        public HeapdumpStatistics(final long estimatedUncompressedSize,
                                  final long estimatedCompressedSize,
                                  final boolean isMqttTransport,
                                  final boolean rpcAvailable,
                                  final LargePayloadHandler handler) {
            this.estimatedUncompressedSize = estimatedUncompressedSize;
            this.estimatedCompressedSize   = estimatedCompressedSize;
            this.isMqttTransport           = isMqttTransport;
            this.rpcAvailable              = rpcAvailable;
            this.handler                   = handler;
        }
    }

    public void init(final HeapdumpStatistics stats) {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Heapdump Capture Options");

        final var captureButtonType = new ButtonType("Capture", OK_DONE);
        dialogPane.getButtonTypes().addAll(captureButtonType, ButtonType.CANCEL);

        final var dialogContent = Fx.loadFXML(loader, context, "/fxml/heapdump-options-dialog.fxml");
        dialogPane.setContent(dialogContent);

        final var controller = (HeapdumpOptionsDialogController) loader.getController();
        controller.initData(stats);

        dialogPane.lookupButton(captureButtonType).disableProperty().bind(controller.selectedOptionProperty().isNull());

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == OK_DONE ? controller.getSelectedOption() : null;
        });
    }
}
