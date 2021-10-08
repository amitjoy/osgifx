package in.bytehue.osgifx.console.ui.configurations;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.osgi.service.metatype.AttributeDefinition;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;

import in.bytehue.osgifx.console.agent.dto.XAttributeDefDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class ConfigurationEditorFxController implements Initializable {

    @FXML
    private BorderPane rootPanel;

    // @FXML
    // private Button saveConfigButton;
    //
    // @FXML
    // private Button deleteConfigButton;

    private FormRenderer form;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void initControls(final XConfigurationDTO config) {
        if (form != null) {
            rootPanel.getChildren().remove(form);
        }
        form = createForm(config);
        rootPanel.setCenter(form);
    }

    private List<Field<?>> initProperties(final XConfigurationDTO config) {
        if (config.ocd == null) {
            return Collections.emptyList();
        }
        final List<Field<?>> fields = new ArrayList<>();
        for (final XAttributeDefDTO ad : config.ocd.attributeDefs) {
            final Field<?> field = toFxField(ad, config);
            fields.add(field);
        }
        return fields;
    }

    private List<Field<?>> initGenericFields(final XConfigurationDTO config) {
        final String pid        = Optional.ofNullable(config.pid).orElse("No PID associated");
        final String factoryPID = Optional.ofNullable(config.factoryPid).orElse("No Factory PID associated");
        final String location   = Optional.ofNullable(config.location).orElse("No location bound");

        final Field<?> pidField        = Field.ofStringType(pid).label("PID").editable(false);
        final Field<?> factoryPidField = Field.ofStringType(factoryPID).label("PID").editable(false);
        final Field<?> locationField   = Field.ofStringType(location).label("Bundle Location").editable(false);

        return Arrays.asList(pidField, factoryPidField, locationField);
    }

    private Field<?> toFxField(final XAttributeDefDTO ad, final XConfigurationDTO config) {
        final Field<?> field = fromAdTypeToFieldType(ad.type, getValue(config, ad.id), null); // TODO
        return field.editable(true).label(ad.id).required(true).required(ad.id + " cannot be empty");
    }

    private Field<?> fromAdTypeToFieldType(final int type, Object currentValue, final Object defaultValue) {
        switch (type) {
            case AttributeDefinition.LONG:
            case AttributeDefinition.INTEGER:
                currentValue = currentValue != null ? currentValue : "4";
                return Field.ofIntegerType(Integer.parseInt(currentValue.toString()));
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                currentValue = currentValue != null ? currentValue : "4.0";
                return Field.ofDoubleType(Double.parseDouble(currentValue.toString()));
            case AttributeDefinition.BOOLEAN:
                currentValue = currentValue != null ? currentValue : "True";
                return Field.ofBooleanType(Boolean.parseBoolean(currentValue.toString()));
            case AttributeDefinition.PASSWORD:
                currentValue = currentValue != null ? currentValue : "DEFAULT";
                return Field.ofPasswordType(currentValue.toString());
            case AttributeDefinition.STRING:
            default: // TODO other types
                currentValue = currentValue != null ? currentValue : "DEFAULT";
                return Field.ofStringType(currentValue.toString());
        }
    }

    private FormRenderer createForm(final XConfigurationDTO config) {
        // @formatter:off
        final Form         form     = Form.of(
                                              Section.of(initGenericFields(config).toArray(new Field[0])).title("Generic Properties"),
                                              Section.of(initProperties(config).toArray(new Field[0])).title("Specific Properties")
                                              )
                                          .title("Configuration Properties");
        // @formatter:on
        final FormRenderer renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private Object getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

}
