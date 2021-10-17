package in.bytehue.osgifx.console.ui.configurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.fx.core.command.CommandService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import in.bytehue.osgifx.console.agent.dto.XAttributeDefDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XObjectClassDefDTO;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class ConfigurationEditorFxController {

    private static final String CONFIG_DELETE_COMMAND_ID  = "in.bytehue.osgifx.console.application.command.configuration.delete";
    private static final String CONFIG_UPDATE_COMMAND_ID  = "in.bytehue.osgifx.console.application.command.configuration.update";
    private static final String CONFIG_FACTORY_COMMAND_ID = "in.bytehue.osgifx.console.application.command.configuration.factory";

    @FXML
    private BorderPane     rootPanel;
    @FXML
    private Button         cancelButton;
    @FXML
    private Button         saveConfigButton;
    @FXML
    private Button         deleteConfigButton;
    @Inject
    private CommandService commandService;
    private Form           form;
    private Converter      converter;
    private FormRenderer   formRenderer;
    private List<String>   uneditableProperties;

    @FXML
    public void initialize() {
        uneditableProperties = Arrays.asList("service.pid", "service.factoryPid");
        converter            = Converters.standardConverter();
    }

    void initControls(final XConfigurationDTO config) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(config);
        initButtons(config);
        rootPanel.setCenter(formRenderer);
    }

    private void initButtons(final XConfigurationDTO config) {
        final String             pid        = config.pid;
        final XObjectClassDefDTO ocd        = config.ocd;
        final String             properties = convertFieldValuesToGson();

        deleteConfigButton.setDisable(config.properties == null || config.isFactory || config.pid == null);
        deleteConfigButton.setOnAction(event -> {
            commandService.execute(CONFIG_DELETE_COMMAND_ID, createCommandMap(pid, null, null));
        });
        saveConfigButton.setOnAction(event -> {
            String effectivePID = null;
            if (pid == null && ocd != null) {
                effectivePID = ocd.pid;
            }
            if (config.isFactory) {
                final String ocdFactoryPid = ocd.factoryPid;
                commandService.execute(CONFIG_FACTORY_COMMAND_ID, createCommandMap(null, ocdFactoryPid, properties));
                return;
            }
            commandService.execute(CONFIG_UPDATE_COMMAND_ID, createCommandMap(effectivePID, null, properties));
        });
        cancelButton.setOnAction(e -> form.reset());
        saveConfigButton.disableProperty().bind(form.changedProperty().not().or(form.validProperty().not()));
    }

    private FormRenderer createForm(final XConfigurationDTO config) {
        // @formatter:off
        form     = Form.of(Section.of(initGenericFields(config).toArray(new Field[0])).title("Generic Properties"),
                           Section.of(initProperties(config).toArray(new Field[0])).title("Specific Properties"))
                       .title("Configuration Properties");
        // @formatter:on
        final FormRenderer renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initProperties(final XConfigurationDTO config) {
        if (config.ocd == null) {
            return initPropertiesFromConfiguration(config);
        }
        return initPropertiesFromOCD(config);
    }

    private List<Field<?>> initPropertiesFromConfiguration(final XConfigurationDTO config) {
        final List<Field<?>> fields = new ArrayList<>();

        for (final Entry<String, Object> entry : config.properties.entrySet()) {
            final String id    = entry.getKey();
            final Object value = entry.getValue();

            final Field<?> field = initFieldFromType(value, Clazz.valueOf(value.getClass()), id, null).label(id);
            if (uneditableProperties.contains(field.getLabel())) {
                continue;
            }
            fields.add(field);
        }
        return fields;
    }

    private List<Field<?>> initPropertiesFromOCD(final XConfigurationDTO config) {
        final List<Field<?>> fields = new ArrayList<>();
        for (final XAttributeDefDTO ad : config.ocd.attributeDefs) {
            final Field<?> field = toFxField(ad, config);
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
        return fromAdTypeToFieldType(ad, getValue(config, ad.id)).editable(true);
    }

    private Field<?> fromAdTypeToFieldType(final XAttributeDefDTO ad, Object currentValue) {
        final int          type       = ad.type;
        final List<String> options    = ad.optionValues;
        final List<String> defaultVal = ad.defaultValue;
        final String       id         = ad.id;

        currentValue = currentValue != null ? currentValue : defaultVal;
        return initFieldFromType(currentValue, type, id, options).label(ad.id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(Object currentValue, final int type, final String id, final List<String> options) {
        Field<?> field;
        switch (type) {
            case AttributeDefinition.LONG:
            case AttributeDefinition.INTEGER:
                if (options != null && !options.isEmpty()) {
                    if (currentValue instanceof List<?>) {
                        currentValue = converter.convert(currentValue).to(String.class);
                    }
                    final int selection = options.indexOf(currentValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                    }), selection);
                    break;
                }
                field = Field.ofIntegerType(converter.convert(currentValue).to(int.class));
                break;
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                if (options != null && !options.isEmpty()) {
                    if (currentValue instanceof List<?>) {
                        currentValue = converter.convert(currentValue).to(String.class);
                    }
                    final int selection = options.indexOf(currentValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                    }), selection);
                    break;
                }
                field = Field.ofDoubleType(converter.convert(currentValue).to(double.class));
                break;
            case AttributeDefinition.BOOLEAN:
                if (options != null && !options.isEmpty()) {
                    if (currentValue instanceof List<?>) {
                        currentValue = converter.convert(currentValue).to(String.class);
                    }
                    final int selection = options.indexOf(currentValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                    }), selection);
                    break;
                }
                field = Field.ofBooleanType(converter.convert(currentValue).to(boolean.class));
                break;
            case AttributeDefinition.PASSWORD:
                field = Field.ofPasswordType(currentValue.toString());
                break;
            case AttributeDefinition.CHARACTER:
                if (options != null && !options.isEmpty()) {
                    if (currentValue instanceof List<?>) {
                        currentValue = converter.convert(options).to(String.class);
                    }
                    final int selection = options.indexOf(currentValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selection);
                    break;
                }
                field = Field.ofStringType(converter.convert(currentValue).to(String.class))
                        .validate(StringLengthValidator.exactly(1, id + "must be of length 1"));
                break;
            case AttributeDefinition.STRING:
            default:
                if (options != null && !options.isEmpty()) {
                    if (currentValue instanceof List<?>) {
                        currentValue = converter.convert(currentValue).to(String.class);
                    }
                    final int selection = options.indexOf(currentValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selection);
                    break;
                }
                field = Field.ofStringType(converter.convert(currentValue).to(String.class));
                break;
        }
        return field;
    }

    private String convertFieldValuesToGson() {
        final Map<String, Object> properties = new HashMap<>();
        for (final Field<?> field : form.getFields()) {
            if (field instanceof DataField) {
                @SuppressWarnings("rawtypes")
                final DataField df = (DataField) field;
                if (!df.isEditable()) {
                    continue;
                }
                properties.put(field.getLabel(), df.getValue());
            }
        }
        return new Gson().toJson(properties);
    }

    private Map<String, Object> createCommandMap(final String pid, final String factoryPid, final String properties) {
        final Map<String, Object> props = new HashMap<>();
        props.computeIfAbsent("pid", key -> pid);
        props.computeIfAbsent("factoryPID", key -> factoryPid);
        props.computeIfAbsent("properties", key -> properties);
        return props;
    }

    private Object getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

}
