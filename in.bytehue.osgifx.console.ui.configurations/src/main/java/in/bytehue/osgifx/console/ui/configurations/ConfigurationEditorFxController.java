package in.bytehue.osgifx.console.ui.configurations;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.osgi.framework.Constants;
import org.osgi.service.metatype.AttributeDefinition;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.collect.Lists;

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
            return initPropertiesFromConfiguration(config);
        }
        return initPropertiesFromOCD(config);
    }

    private List<Field<?>> initPropertiesFromConfiguration(final XConfigurationDTO config) {
        final List<Field<?>> fields = new ArrayList<>();

        final Map<String, Object> filteredProperties = new HashMap<>(config.properties);
        filteredProperties.remove(Constants.SERVICE_PID);

        for (final Entry<String, Object> entry : filteredProperties.entrySet()) {
            final String   id    = entry.getKey();
            final Object   value = entry.getValue();
            final Field<?> field = initFieldFromType(value, Clazz.valueOf(value.getClass()), id).label(id);
            fields.add(field);
        }
        return fields;
    }

    enum Clazz {
        STRING(String.class, AttributeDefinition.STRING),
        INTEGER(Integer.class, AttributeDefinition.INTEGER),
        INT(int.class, AttributeDefinition.INTEGER),
        DOUBLE(Double.class, AttributeDefinition.DOUBLE),
        Double(double.class, AttributeDefinition.DOUBLE),
        FLOAT(Float.class, AttributeDefinition.FLOAT),
        Float(float.class, AttributeDefinition.FLOAT),
        CHAR(Character.class, AttributeDefinition.CHARACTER),
        Char(String.class, AttributeDefinition.CHARACTER),
        LONG(Long.class, AttributeDefinition.LONG),
        Long(String.class, AttributeDefinition.LONG);

        final int      type;
        final Class<?> clazz;

        Clazz(final Class<?> clazz, final int type) {
            this.type  = type;
            this.clazz = clazz;
        }

        public static int valueOf(final Class<?> clazz) {
            for (final Clazz v : values()) {
                if (v.clazz.equals(clazz)) {
                    return v.type;
                }
            }
            return AttributeDefinition.STRING;
        }

    }

    private List<Field<?>> initPropertiesFromOCD(final XConfigurationDTO config) {
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
        final Field<?> factoryPidField = Field.ofStringType(factoryPID).label("Factory PID").editable(false);
        final Field<?> locationField   = Field.ofStringType(location).label("Bundle Location").editable(false);

        final List<Field<?>> genericFields = Lists.newArrayList(pidField, factoryPidField, locationField);

        if (config.ocd != null) {
            final Field<?> descLocationField = Field.ofStringType(config.ocd.descriptorLocation).label("Descriptor Location")
                    .editable(false);
            genericFields.add(descLocationField);
        }
        return genericFields;
    }

    private Field<?> toFxField(final XAttributeDefDTO ad, final XConfigurationDTO config) {
        Field<?> field = fromAdTypeToFieldType(ad, getValue(config, ad.id), null).editable(true);
        if (ad.cardinality == 1) { // TODO other cardinalities
            field = field.required(true).required(ad.id + " cannot be empty");
        }
        return field;
    }

    private Field<?> fromAdTypeToFieldType(final XAttributeDefDTO ad, Object currentValue, final Object defaultValue) {
        final int          type       = ad.type;
        final List<String> options    = ad.optionValues;
        final List<String> defaultVal = ad.defaultValue;
        Field<?>           field      = null;
        final String       id         = ad.id;
        if (options == null || options.isEmpty()) {
            currentValue = currentValue != null ? currentValue : defaultVal.get(0);
            field        = initFieldFromType(currentValue, type, id);
        } else if (ad.cardinality == 1) {
            field = Field.ofSingleSelectionType(options);
        } else {
            field = Field.ofMultiSelectionType(options);
        }
        return field.label(ad.id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final Object currentValue, final int type, final String id) {
        Field<?> field;
        switch (type) {
            case AttributeDefinition.LONG:
            case AttributeDefinition.INTEGER:
                field = Field.ofIntegerType(Integer.parseInt(currentValue.toString()));
                break;
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                field = Field.ofDoubleType(Double.parseDouble(currentValue.toString()));
                break;
            case AttributeDefinition.BOOLEAN:
                field = Field.ofBooleanType(Boolean.parseBoolean(currentValue.toString()));
                break;
            case AttributeDefinition.PASSWORD:
                field = Field.ofPasswordType(currentValue.toString());
                break;
            case AttributeDefinition.CHARACTER:
                field = Field.ofStringType(currentValue.toString()).validate(StringLengthValidator.exactly(1, id + "must be of length 1"));
                break;
            case AttributeDefinition.STRING:
            default: // TODO other types and cardinalities
                field = Field.ofStringType(currentValue.toString());
                break;
        }
        return field;
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
