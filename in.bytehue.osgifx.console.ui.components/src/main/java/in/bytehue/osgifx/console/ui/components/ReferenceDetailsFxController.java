package in.bytehue.osgifx.console.ui.components;

import java.net.URL;
import java.util.ResourceBundle;

import org.osgi.service.component.runtime.dto.ReferenceDTO;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public final class ReferenceDetailsFxController implements Initializable {

    @FXML
    private Label nameLabel;

    @FXML
    private Label interfaceLabel;

    @FXML
    private Label cardinalityLabel;

    @FXML
    private Label policyLabel;

    @FXML
    private Label policyOptionLabel;

    @FXML
    private Label targetLabel;

    @FXML
    private Label unbindLabel;

    @FXML
    private Label updatedLabel;

    @FXML
    private Label fieldLabel;

    @FXML
    private Label fieldOptionLabel;

    @FXML
    private Label scopeLabel;

    @FXML
    private Label bindLabel;

    @FXML
    private Label parameterLabel;

    @FXML
    private Label collectionTypeLabel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
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
