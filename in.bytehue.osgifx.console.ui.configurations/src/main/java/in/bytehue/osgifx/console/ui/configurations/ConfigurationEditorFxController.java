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
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
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
import in.bytehue.osgifx.console.agent.dto.XAttributeDefType;
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

    @Log
    @Inject
    private FluentLogger           logger;
    @FXML
    private BorderPane             rootPanel;
    @FXML
    private Button                 cancelButton;
    @FXML
    private Button                 saveConfigButton;
    @FXML
    private Button                 deleteConfigButton;
    @Inject
    private CommandService         commandService;
    private Form                   form;
    private Converter              converter;
    private FormRenderer           formRenderer;
    private List<String>           uneditableProperties;
    private Map<Field<?>, Integer> typeMappings;

    @FXML
    public void initialize() {
        typeMappings         = new HashMap<>();
        uneditableProperties = Arrays.asList("service.pid", "service.factoryPid");
        converter            = Converters.standardConverter();
        logger.atDebug().log("FXML controller has been initialized");
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
        final String             pid = config.pid;
        final XObjectClassDefDTO ocd = config.ocd;

        deleteConfigButton.setDisable(config.properties == null || config.isFactory || config.pid == null);
        deleteConfigButton.setOnAction(event -> {
            logger.atInfo().log("Configuration delete request has been sent for PID '%s'", pid);
            commandService.execute(CONFIG_DELETE_COMMAND_ID, createCommandMap(pid, null, null));
        });
        saveConfigButton.setOnAction(event -> {
            final String properties   = convertFieldValuesToGson();
            String       effectivePID = null;
            if (pid == null && ocd != null) {
                effectivePID = ocd.pid;
            }
            if (config.isFactory) {
                final String ocdFactoryPid = ocd.factoryPid;
                logger.atInfo().log("Factory configuration create request has been sent for factory PID '%s'", ocdFactoryPid);
                commandService.execute(CONFIG_FACTORY_COMMAND_ID, createCommandMap(null, ocdFactoryPid, properties));
                return;
            }
            logger.atInfo().log("Configuration create request has been sent for PID '%s'", effectivePID);
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

            final XAttributeDefType attrDefType = XAttributeDefType.getType(value);
            final Field<?>          field       = initFieldFromType(value, null, attrDefType, null).label(id);

            if (uneditableProperties.contains(field.getLabel())) {
                continue;
            }
            fields.add(field);
            typeMappings.put(field, attrDefType.ordinal());
        }
        return fields;
    }

    private List<Field<?>> initPropertiesFromOCD(final XConfigurationDTO config) {
        final List<Field<?>> fields = new ArrayList<>();
        for (final XAttributeDefDTO ad : config.ocd.attributeDefs) {
            final Field<?> field = toFxField(ad, config);
            fields.add(field);
            typeMappings.put(field, ad.type);
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
        return fromAdTypeToFieldType(ad, getValue(config, ad.id)).editable(true);
    }

    private Field<?> fromAdTypeToFieldType(final XAttributeDefDTO ad, final Object currentValue) {
        final XAttributeDefType type       = XAttributeDefType.values()[ad.type];
        final List<String>      options    = ad.optionValues;
        final List<String>      defaultVal = ad.defaultValue;
        final String            id         = ad.id;

        return initFieldFromType(currentValue, defaultVal, type, options).label(id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final Object currentValue, final List<String> defaultValue, final XAttributeDefType adType,
            final List<String> options) {
        Field<?> field = null;
        switch (adType) {
            case LONG:
            case INTEGER:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final int selection = options.indexOf(currentValue);
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                        }), selection);
                    } else {
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofIntegerType(converter.convert(currentValue).to(int.class));
                } else {
                    field = Field.ofIntegerType(converter.convert(defaultValue).to(int.class));
                }
                break;
            case FLOAT:
            case DOUBLE:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final int selection = options.indexOf(currentValue);
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                        }), selection);
                    } else {
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofDoubleType(converter.convert(currentValue).to(double.class));
                } else {
                    field = Field.ofDoubleType(converter.convert(defaultValue).to(double.class));
                }
                break;
            case BOOLEAN:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final int selection = options.indexOf(currentValue);
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                        }), selection);
                    } else {
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofBooleanType(converter.convert(currentValue).to(boolean.class));
                } else {
                    field = Field.ofBooleanType(converter.convert(defaultValue).to(boolean.class));
                }
                break;
            case PASSWORD:
                if (currentValue != null) {
                    field = Field.ofPasswordType(converter.convert(currentValue).to(String.class));
                } else {
                    field = Field.ofPasswordType(converter.convert(defaultValue).to(String.class));
                }
                break;
            case CHAR:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final int selection = options.indexOf(currentValue);
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }), selection);
                    } else {
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofStringType(converter.convert(currentValue).to(String.class))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
                } else {
                    field = Field.ofStringType(converter.convert(defaultValue).to(String.class))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
                }
                break;
            case BOOLEAN_ARRAY:
            case BOOLEAN_LIST:
            case DOUBLE_ARRAY:
            case DOUBLE_LIST:
            case LONG_ARRAY:
            case LONG_LIST:
            case INTEGER_ARRAY:
            case INTEGER_LIST:
            case FLOAT_ARRAY:
            case FLOAT_LIST:
            case CHAR_ARRAY:
            case CHAR_LIST:
            case STRING_ARRAY:
            case STRING_LIST:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final List<Integer> selections = converter.convert(currentValue).to(new TypeReference<List<Integer>>() {
                        });
                        field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }), selections);
                    } else {
                        field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    final String convertedValue = converter.convert(currentValue).to(String.class);
                    field = Field.ofStringType(convertedValue).multiline(convertedValue.length() > 100);
                }
                break;
            case STRING:
            default:
                if (options != null && !options.isEmpty()) {
                    if (currentValue != null) {
                        final int selection = options.indexOf(currentValue);
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }), selection);
                    } else {
                        field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                        }));
                    }
                    break;
                }
                if (currentValue != null) {
                    final String convertedValue = converter.convert(currentValue).to(String.class);
                    field = Field.ofStringType(converter.convert(currentValue).to(String.class)).multiline(convertedValue.length() > 100);
                } else {
                    final String convertedValue = converter.convert(defaultValue).to(String.class);
                    field = Field.ofStringType(converter.convert(defaultValue).to(String.class)).multiline(convertedValue.length() > 100);
                }
                break;
        }
        if (field == null) {
            field = Field.ofStringType("");
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
                final Object originalType = convertToRequestedType(field, df.getValue());
                properties.put(field.getLabel(), originalType);
            }
        }
        return new Gson().toJson(properties);
    }

    private Object convertToRequestedType(final Field<?> field, final Object value) {
        // this controller cannot be loaded by FXMLLoader if the 'typeMappings' values are of type XAttributeDefType
        final XAttributeDefType type = XAttributeDefType.values()[typeMappings.get(field)];
        return converter.convert(value).to(XAttributeDefType.clazz(type));
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
