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
package com.osgifx.console.ui.rsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class RsaDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private BorderPane   rootPanel;
    private FormRenderer formRenderer;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    public void initControls(final XRemoteServiceDTO rsaEntry) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(rsaEntry);
        rootPanel.setCenter(formRenderer);
    }

    private FormRenderer createForm(final XRemoteServiceDTO rsaEntry) {
        final var form = Form
                .of(Section.of(initBasicProperties(rsaEntry).toArray(new Field[0])).title("Endpoint Information"),
                        Section.of(initCustomProperties(rsaEntry.properties).toArray(new Field[0]))
                                .title("Endpoint Properties"))
                .title("Remote Service Details");

        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        Fx.addCopySupportToReadOnlyLabels(renderer);
        return renderer;
    }

    private List<Field<?>> initBasicProperties(final XRemoteServiceDTO rsaEntry) {
        final List<Field<?>> fields = new ArrayList<>();

        fields.add(Field.ofStringType(rsaEntry.id == null ? "" : rsaEntry.id).editable(false).label("Endpoint ID"));

        fields.add(Field.ofStringType(rsaEntry.direction == null ? "" : rsaEntry.direction.toString()).editable(false)
                .label("Direction"));

        fields.add(Field.ofStringType(rsaEntry.frameworkUUID == null ? "" : rsaEntry.frameworkUUID).editable(false)
                .label("Framework UUID"));

        if (rsaEntry.objectClass != null) {
            fields.add(Field.ofMultiSelectionType(rsaEntry.objectClass).label("Object Classes"));
        }

        if (rsaEntry.localServiceId != null) {
            fields.add(
                    Field.ofStringType(rsaEntry.localServiceId.toString()).editable(false).label("Local Service ID"));
        }

        if (rsaEntry.provider != null) {
            fields.add(Field.ofStringType(rsaEntry.provider).editable(false).label("Provider"));
        }

        if (rsaEntry.intents != null && !rsaEntry.intents.isEmpty()) {
            fields.add(Field.ofMultiSelectionType(rsaEntry.intents).label("Intents"));
        }

        return fields;
    }

    private List<Field<?>> initCustomProperties(final Map<String, Object> properties) {
        final List<Field<?>> fields = new ArrayList<>();

        if (properties != null) {
            properties.forEach((key, value) -> {
                final String strValue = value == null ? "" : value.toString();
                fields.add(Field.ofStringType(strValue).editable(false).label(key));
            });
        }

        return fields;
    }
}
