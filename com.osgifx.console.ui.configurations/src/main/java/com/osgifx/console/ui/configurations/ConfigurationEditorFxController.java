/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.configurations;

import static com.osgifx.console.agent.dto.XAttributeDefType.CHAR_ARRAY;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_DELETED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;
import static java.lang.System.lineSeparator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.TypeReference;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.configurations.control.MultipleCardinalityTextControl;
import com.osgifx.console.ui.configurations.control.PeekablePasswordControl;
import com.osgifx.console.util.converter.ValueConverter;
import com.osgifx.console.util.fx.FxDialog;

import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class ConfigurationEditorFxController {

    @Log
    @Inject
    private FluentLogger           logger;
    @FXML
    private BorderPane             rootPanel;
    @Inject
    private Supervisor             supervisor;
    @Inject
    private EventBroker            eventBroker;
    @FXML
    private Button                 cancelButton;
    @FXML
    private Button                 saveConfigButton;
    @FXML
    private Button                 deleteConfigButton;
    private Form                   form;
    private FormRenderer           formRenderer;
    private Map<Field<?>, Integer> typeMappings;
    private List<String>           uneditableProperties;
    private final ValueConverter   converter = new ValueConverter();

    @FXML
    public void initialize() {
        typeMappings         = Maps.newHashMap();
        uneditableProperties = Arrays.asList("service.pid", "service.factoryPid");
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
        final var pid = config.pid;
        final var ocd = config.ocd;

        deleteConfigButton.setDisable(config.properties == null || config.isFactory || config.pid == null);
        deleteConfigButton.setOnAction(event -> {
            logger.atInfo().log("Configuration delete request has been sent for PID '%s'", pid);
            deleteConfiguration(pid);
        });
        saveConfigButton.setOnAction(event -> {
            final var properties   = prepareConfigurationProperties();
            String    effectivePID = null;
            if (pid == null && ocd != null) {
                effectivePID = ocd.pid;
            } else {
                effectivePID = pid;
            }
            if (config.isFactory) {
                final var ocdFactoryPid = ocd.factoryPid;
                logger.atInfo().log("Factory configuration create request has been sent for factory PID '%s'", ocdFactoryPid);
                createFactoryConfiguration(ocdFactoryPid, properties);
                return;
            }
            logger.atInfo().log("Configuration create request has been sent for PID '%s'", effectivePID);
            createOrUpdateConfiguration(effectivePID, properties);
        });
        cancelButton.setOnAction(e -> form.reset());

        final BooleanProperty isPersisted              = new SimpleBooleanProperty(config.isPersisted);
        final var             isPersistedConfigBinding = new When(isPersisted).then(true).otherwise(false);

        saveConfigButton.disableProperty().bind(form.changedProperty().not().or(form.validProperty().not()).and(isPersistedConfigBinding));
    }

    private void deleteConfiguration(final String pid) {
        final var result = supervisor.getAgent().deleteConfiguration(pid);
        if (result.result == XResultDTO.SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(CONFIGURATION_DELETED_EVENT_TOPIC, pid);
        } else if (result.result == XResultDTO.SKIPPED) {
            logger.atWarning().log(result.response);
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Configuration Delete Error", result.response, getClass().getClassLoader());
        }
    }

    private void createFactoryConfiguration(final String factoryPID, final List<ConfigValue> properties) {
        final var result = supervisor.getAgent().createFactoryConfiguration(factoryPID, properties);
        if (result.result == XResultDTO.SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, factoryPID);
        } else if (result.result == XResultDTO.SKIPPED) {
            logger.atWarning().log(result.response);
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Factory Configuration Creation Error", result.response, getClass().getClassLoader());
        }
    }

    private void createOrUpdateConfiguration(final String pid, final List<ConfigValue> properties) {
        final var result = supervisor.getAgent().createOrUpdateConfiguration(pid, properties);
        if (result.result == XResultDTO.SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, pid);
        } else if (result.result == XResultDTO.SKIPPED) {
            logger.atWarning().log(result.response);
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Configuration Creation/Updation Error", result.response, getClass().getClassLoader());
        }
    }

    private FormRenderer createForm(final XConfigurationDTO config) {
        form = Form
                .of(Section.of(initGenericFields(config).toArray(new Field[0])).title("Generic Properties"),
                        Section.of(initProperties(config).toArray(new Field[0])).title("Specific Properties"))
                .title("Configuration Properties");
        final var renderer = new FormRenderer(form);

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
        final List<Field<?>> fields = Lists.newArrayList();

        for (final Entry<String, ConfigValue> entry : config.properties.entrySet()) {
            final var key         = entry.getKey();
            final var value       = entry.getValue();
            final var attrDefType = value.type;

            final Field<?> field = initFieldFromType(key, value, null, attrDefType, null, false).label(key);

            if (uneditableProperties.contains(field.getLabel())) {
                continue;
            }
            fields.add(field);
            typeMappings.put(field, attrDefType.ordinal());
        }
        return fields;
    }

    private List<Field<?>> initPropertiesFromOCD(final XConfigurationDTO config) {
        final List<Field<?>> fields = Lists.newArrayList();
        for (final XAttributeDefDTO ad : config.ocd.attributeDefs) {
            final Field<?> field = toFxField(ad, config);
            fields.add(field);
            typeMappings.put(field, ad.type);
        }
        return fields;
    }

    private List<Field<?>> initGenericFields(final XConfigurationDTO config) {
        final var pid        = Optional.ofNullable(config.pid).orElse("No PID associated");
        final var factoryPID = Optional.ofNullable(config.factoryPid).orElse("No Factory PID associated");
        final var location   = Optional.ofNullable(config.location).orElse("No location bound");

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

    private Field<?> fromAdTypeToFieldType(final XAttributeDefDTO ad, final ConfigValue currentValue) {
        final var type       = XAttributeDefType.values()[ad.type];
        final var options    = ad.optionValues;
        final var defaultVal = ad.defaultValue;
        final var id         = ad.id;

        return initFieldFromType(id, currentValue, defaultVal, type, options, true).label(id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final String key, final ConfigValue configValue, final List<String> defaultValue,
            final XAttributeDefType adType, final List<String> options, final boolean hasOCD) {
        final var currentValue = configValue != null ? configValue.value : null;
        Field<?>  field        = null;
        switch (adType) {
        case LONG, INTEGER:
            if (options != null && !options.isEmpty()) {
                String effectiveValue;
                if (currentValue != null) {
                    effectiveValue = converter.convert(currentValue, String.class);
                } else {
                    effectiveValue = converter.convert(defaultValue, String.class);
                }
                final var selection = options.indexOf(effectiveValue);
                field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<Double>>() {
                }), selection);
                break;
            }
            if (currentValue != null) {
                field = Field.ofIntegerType(converter.convert(currentValue, int.class));
            } else {
                field = Field.ofIntegerType(converter.convert(defaultValue, int.class));
            }
            break;
        case FLOAT, DOUBLE:
            if (options != null && !options.isEmpty()) {
                String effectiveValue;
                if (currentValue != null) {
                    effectiveValue = converter.convert(currentValue, String.class);
                } else {
                    effectiveValue = converter.convert(defaultValue, String.class);
                }
                final var selection = options.indexOf(effectiveValue);
                field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<Double>>() {
                }), selection);
                break;
            }
            if (currentValue != null) {
                field = Field.ofDoubleType(converter.convert(currentValue, double.class));
            } else {
                field = Field.ofDoubleType(converter.convert(defaultValue, double.class));
            }
            break;
        case BOOLEAN:
            if (options != null && !options.isEmpty()) {
                String effectiveValue;
                if (currentValue != null) {
                    effectiveValue = converter.convert(currentValue, String.class);
                } else {
                    effectiveValue = converter.convert(defaultValue, String.class);
                }
                final var selection = options.indexOf(effectiveValue);
                field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<Boolean>>() {
                }), selection);
                break;
            }
            if (currentValue != null) {
                field = Field.ofBooleanType(converter.convert(currentValue, boolean.class));
            } else {
                field = Field.ofBooleanType(converter.convert(defaultValue, boolean.class));
            }
            break;
        case PASSWORD:
            if (currentValue != null) {
                field = Field.ofPasswordType(converter.convert(currentValue, String.class)).render(new PeekablePasswordControl());
            } else {
                field = Field.ofPasswordType(converter.convert(defaultValue, String.class)).render(new PeekablePasswordControl());
            }
            break;
        case CHAR:
            if (options != null && !options.isEmpty()) {
                String effectiveValue;
                if (currentValue != null) {
                    final char c = converter.convert(currentValue, char.class);
                    effectiveValue = Character.toString(c);
                } else {
                    final char c = converter.convert(defaultValue, char.class);
                    effectiveValue = Character.toString(c);
                }
                final var selection = options.indexOf(effectiveValue);
                field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<String>>() {
                }), selection);
                break;
            }
            if (currentValue != null) {
                final char c = converter.convert(currentValue, char.class);
                field = Field.ofStringType(Character.toString(c)).validate(StringLengthValidator.exactly(1, "Length must be 1"));
            } else {
                final char c = converter.convert(defaultValue, char.class);
                field = Field.ofStringType(Character.toString(c)).validate(StringLengthValidator.exactly(1, "Length must be 1"));
            }
            break;
        case BOOLEAN_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, boolean.class, XAttributeDefType.BOOLEAN_ARRAY);
            break;
        case BOOLEAN_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Boolean.class, XAttributeDefType.BOOLEAN_LIST);
            break;
        case DOUBLE_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, double.class, XAttributeDefType.DOUBLE_ARRAY);
            break;
        case DOUBLE_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Double.class, XAttributeDefType.DOUBLE_LIST);
            break;
        case LONG_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, long.class, XAttributeDefType.LONG_ARRAY);
            break;
        case LONG_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Long.class, XAttributeDefType.LONG_LIST);
            break;
        case INTEGER_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, int.class, XAttributeDefType.INTEGER_ARRAY);
            break;
        case INTEGER_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Integer.class, XAttributeDefType.INTEGER_LIST);
            break;
        case FLOAT_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, float.class, XAttributeDefType.FLOAT_ARRAY);
            break;
        case FLOAT_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Float.class, XAttributeDefType.FLOAT_LIST);
            break;
        case CHAR_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, char.class, CHAR_ARRAY);
            break;
        case CHAR_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, Character.class, XAttributeDefType.CHAR_LIST);
            break;
        case STRING_ARRAY:
            field = processArray(key, currentValue, defaultValue, options, hasOCD, String.class, XAttributeDefType.STRING_ARRAY);
            break;
        case STRING_LIST:
            field = processList(key, currentValue, defaultValue, options, hasOCD, String.class, XAttributeDefType.STRING_LIST);
            break;
        case STRING:
        default:
            if (options != null && !options.isEmpty()) {
                String effectiveValue;
                if (currentValue != null) {
                    effectiveValue = converter.convert(currentValue, String.class);
                } else {
                    effectiveValue = converter.convert(defaultValue, String.class);
                }
                final var selection = options.indexOf(effectiveValue);
                field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<String>>() {
                }), selection);
                break;
            }
            if (currentValue != null) {
                final var convertedValue = converter.convert(currentValue, String.class);
                field = Field.ofStringType(converter.convert(currentValue, String.class)).multiline(convertedValue.length() > 100);
            } else {
                final var convertedValue = converter.convert(defaultValue, String.class);
                field = Field.ofStringType(converter.convert(defaultValue, String.class)).multiline(convertedValue.length() > 100);
            }
            break;
        }
        if (field == null) {
            field = Field.ofStringType("");
        }
        return field;
    }

    private <T> Field<?> processArray(final String key, final Object currentValue, final List<String> defaultValue,
            final List<String> options, final boolean hasOCD, final Class<T> clazz, final XAttributeDefType adType) {
        final Field<?> field;
        if (hasOCD) {
            T[] effectiveValue;
            if (currentValue != null) {
                effectiveValue = converter.convert(currentValue, getArrayClass(clazz));
            } else {
                effectiveValue = converter.convert(defaultValue, getArrayClass(clazz));
            }
            if (options != null && !options.isEmpty()) {
                final var selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }), selections);
            } else {
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }));
            }
        } else {
            final var    control = new MultipleCardinalityTextControl(key, adType);
            List<String> convertedValue;
            if (adType == CHAR_ARRAY) {
                final List<Character> tempValue = converter.convert(currentValue, new TypeReference<List<Character>>() {
                });
                convertedValue = converter.convert(tempValue, new TypeReference<List<String>>() {
                });
            } else {
                convertedValue = converter.convert(currentValue, new TypeReference<List<String>>() {
                });
            }
            field = Field.ofStringType(Joiner.on(lineSeparator()).join(convertedValue)).render(control).multiline(true);
        }
        return field;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T[]> getArrayClass(final Class<T> clazz) {
        return (Class<? extends T[]>) Array.newInstance(clazz, 0).getClass();
    }

    private <T> Field<?> processList(final String key, final Object currentValue, final List<String> defaultValue,
            final List<String> options, final boolean hasOCD, final Class<T> clazz, final XAttributeDefType adType) {
        Field<?> field;
        if (hasOCD) {
            List<T> effectiveValue;
            if (currentValue != null) {
                effectiveValue = converter.convert(currentValue, new TypeReference<List<T>>() {
                });
            } else {
                effectiveValue = converter.convert(defaultValue, new TypeReference<List<T>>() {
                });
            }
            if (options != null && !options.isEmpty()) {
                final var selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }), selections);
            } else {
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }));
            }
        } else {
            final var          control        = new MultipleCardinalityTextControl(key, adType);
            final List<String> convertedValue = converter.convert(currentValue, new TypeReference<List<String>>() {
                                              });
            field = Field.ofStringType(Joiner.on(lineSeparator()).join(convertedValue)).render(control).multiline(true);
        }
        return field;
    }

    private List<ConfigValue> prepareConfigurationProperties() {
        final List<ConfigValue> properties = Lists.newArrayList();
        for (final Field<?> field : form.getFields()) {
            if (field instanceof final DataField<?, ?, ?> df) {
                if (!df.isEditable()) {
                    continue;
                }
                final var    type         = XAttributeDefType.values()[typeMappings.get(field)];
                final Object currentValue = df.getValue();
                final var    configValue  = convertToRequestedType(currentValue, type);
                properties.add(ConfigValue.create(field.getLabel(), configValue, type));
            }
        }
        return properties;
    }

    private Object convertToRequestedType(final Object value, final XAttributeDefType type) {
        // this controller cannot be loaded by FXMLLoader if the 'typeMappings' values
        // are of type XAttributeDefType
        return converter.convert(value, type);
    }

    private ConfigValue getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

}
