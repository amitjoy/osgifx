package in.bytehue.osgifx.console.ui.components;

import javax.inject.Inject;

import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class ReferenceDetailsFxController {

    @Log
    @Inject
    private Logger logger;
    @FXML
    private Label  nameLabel;
    @FXML
    private Label  interfaceLabel;
    @FXML
    private Label  cardinalityLabel;
    @FXML
    private Label  policyLabel;
    @FXML
    private Label  policyOptionLabel;
    @FXML
    private Label  targetLabel;
    @FXML
    private Label  unbindLabel;
    @FXML
    private Label  updatedLabel;
    @FXML
    private Label  fieldLabel;
    @FXML
    private Label  fieldOptionLabel;
    @FXML
    private Label  scopeLabel;
    @FXML
    private Label  bindLabel;
    @FXML
    private Label  parameterLabel;
    @FXML
    private Label  collectionTypeLabel;

    @FXML
    public void initialize() {
        logger.info("FXML controller (" + getClass() + ") has been initialized");
    }

    void initControls(final ReferenceDTO reference) {
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
