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
package com.osgifx.console.ui.components;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XReferenceDTO;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public final class ReferenceDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private TextField    nameLabel;
    @FXML
    private TextField    interfaceLabel;
    @FXML
    private TextField    cardinalityLabel;
    @FXML
    private TextField    policyLabel;
    @FXML
    private TextField    policyOptionLabel;
    @FXML
    private TextField    targetLabel;
    @FXML
    private TextField    unbindLabel;
    @FXML
    private TextField    updatedLabel;
    @FXML
    private TextField    fieldLabel;
    @FXML
    private TextField    fieldOptionLabel;
    @FXML
    private TextField    scopeLabel;
    @FXML
    private TextField    bindLabel;
    @FXML
    private TextField    parameterLabel;
    @FXML
    private TextField    collectionTypeLabel;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XReferenceDTO reference) {
        nameLabel.setText(reference.name);
        interfaceLabel.setText(reference.interfaceName);
        cardinalityLabel.setText(reference.cardinality);
        policyLabel.setText(reference.policy);
        policyOptionLabel.setText(reference.policyOption);
        targetLabel.setText(reference.target);
        unbindLabel.setText(reference.unbind);
        updatedLabel.setText(reference.updated);
        fieldLabel.setText(reference.field);
        fieldOptionLabel.setText(reference.fieldOption);
        scopeLabel.setText(reference.scope);
        bindLabel.setText(reference.bind);
        parameterLabel.setText(String.valueOf(reference.parameter));
        collectionTypeLabel.setText(reference.collectionType);
    }

}
