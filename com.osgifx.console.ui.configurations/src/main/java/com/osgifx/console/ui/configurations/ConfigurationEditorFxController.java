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
package com.osgifx.console.ui.configurations;

import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.CHAR_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.CHAR_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.DOUBLE_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.DOUBLE_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.FLOAT_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.FLOAT_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.INTEGER_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.INTEGER_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.LONG_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.LONG_LIST;
import static com.osgifx.console.agent.dto.XAttributeDefType.STRING_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.STRING_LIST;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_DELETED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;
import static java.lang.System.lineSeparator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.TypeReference;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XComponentReferenceFilterDTO;
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
    @Optional
    private Supervisor             supervisor;
    @Inject
    private EventBroker            eventBroker;
    @FXML
    private Button                 cancelButton;
    @FXML
    private Button                 saveConfigButton;
    @FXML
    private Button                 deleteConfigButton;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                isSnapshotAgent;
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

        deleteConfigButton.setDisable(isSnapshotAgent || config.location == null || config.isFactory);
        deleteConfigButton.setOnAction(_ -> {
            logger.atInfo().log("Configuration delete request has been sent for PID '%s'", pid);
            deleteConfiguration(pid);
        });
        saveConfigButton.setOnAction(_ -> {
            final var properties   = prepareConfigurationProperties();
            String    effectivePID = null;
            if (pid == null && ocd != null) {
                effectivePID = ocd.pid;
            } else {
                effectivePID = pid;
            }
            if (config.isFactory) {
                final var ocdFactoryPid = ocd.factoryPid;
                logger.atInfo().log("Factory configuration create request has been sent for factory PID '%s'",
                        ocdFactoryPid);
                createFactoryConfiguration(ocdFactoryPid, properties);
                return;
            }
            logger.atInfo().log("Configuration create request has been sent for PID '%s'", effectivePID);
            createOrUpdateConfiguration(effectivePID, properties);
        });
        cancelButton.setOnAction(_ -> form.reset());

        final BooleanProperty isSnapshot        = new SimpleBooleanProperty(isSnapshotAgent);
        final var             isSnapshotBinding = new When(isSnapshot).then(true).otherwise(false);

        cancelButton.disableProperty().bind(isSnapshotBinding.or(form.changedProperty().not()));
        saveConfigButton.disableProperty().bind(isSnapshotBinding.or(form.validProperty().not()));
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
            FxDialog.showErrorDialog("Factory Configuration Creation Error", result.response,
                    getClass().getClassLoader());
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
            FxDialog.showErrorDialog("Configuration Creation/Updation Error", result.response,
                    getClass().getClassLoader());
        }
    }

    private FormRenderer createForm(final XConfigurationDTO config) {
        final var formGroups = createGroups(config);
        form = Form.of(formGroups.toArray(new Group[0])).title("Configuration Properties");
        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Group> createGroups(final XConfigurationDTO config) {
        final List<Group> formGroups = Lists.newArrayList();
        // @formatter:off
        final Group       genericPropertiesGroup  = Section.of(initGenericFields(config).toArray(new Field[0]))
                                                           .title("Generic Properties");
        final Group       specificPropertiesGroup = Section.of(initProperties(config).toArray(new Field[0]))
                                                           .title("Specific Properties");
        // @formatter:off

        formGroups.add(genericPropertiesGroup);
        formGroups.add(specificPropertiesGroup);

        if (config.componentReferenceFilters == null) {
            return formGroups;
        }

        final Map<String, List<XComponentReferenceFilterDTO>> componentReferenceFilters = config.componentReferenceFilters
                .stream().collect(Collectors.groupingBy(x -> x.componentName));

        for (final Entry<String, List<XComponentReferenceFilterDTO>> entry : componentReferenceFilters.entrySet()) {
            final var   componentName  = entry.getKey();
            final Group refFilterGroup = Section
                    .of(initComponentReferencesGroup(entry.getValue()).toArray(new Field[0]))
                    .title("Component References - " + componentName);
            formGroups.add(refFilterGroup);
        }
        return formGroups;
    }

    private List<Field<?>> initComponentReferencesGroup(final List<XComponentReferenceFilterDTO> refFilters) {
        final List<Field<?>> fields = Lists.newArrayList();
        for (final XComponentReferenceFilterDTO refFilter : refFilters) {
            // @formatter:off
            final Field<?> targetKeyField = Field.ofStringType(Strings.nullToEmpty(refFilter.targetFilter))
                                                 .label(refFilter.targetKey)
                                                 .editable(!isSnapshotAgent);
            // @formatter:on
            fields.add(targetKeyField);
            typeMappings.put(targetKeyField, XAttributeDefType.STRING.ordinal());
        }
        return fields;
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

            final Field<?> field = initFieldFromType(key, value, null, attrDefType, Integer.MAX_VALUE, null).label(key)
                    .editable(!isSnapshotAgent);

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
        final var pid        = java.util.Optional.ofNullable(config.pid).orElse("No PID associated");
        final var factoryPID = java.util.Optional.ofNullable(config.factoryPid).orElse("No Factory PID associated");
        final var location   = java.util.Optional.ofNullable(config.location).orElse("No location bound");

        final Field<?> pidField        = Field.ofStringType(pid).label("PID").editable(false);
        final Field<?> factoryPidField = Field.ofStringType(factoryPID).label("Factory PID").editable(false);
        final Field<?> locationField   = Field.ofStringType(location).label("Bundle Location").editable(false);

        final List<Field<?>> genericFields = Lists.newArrayList(pidField, factoryPidField, locationField);

        if (config.ocd != null) {
            // @formatter:off
            final Field<?> descLocationField = Field.ofStringType(config.ocd.descriptorLocation)
                                                    .label("Descriptor Location")
                                                    .editable(false);
            // @formatter:on
            genericFields.add(descLocationField);
        }
        return genericFields;
    }

    private Field<?> toFxField(final XAttributeDefDTO ad, final XConfigurationDTO config) {
        return fromAdTypeToFieldType(ad, getValue(config, ad.id)).editable(!isSnapshotAgent);
    }

    private Field<?> fromAdTypeToFieldType(final XAttributeDefDTO ad, final ConfigValue currentValue) {
        final var type       = XAttributeDefType.values()[ad.type];
        final var options    = ad.optionValues;
        final var defaultVal = ad.defaultValue;
        final var id         = ad.id;

        return initFieldFromType(id, currentValue, defaultVal, type, ad.cardinality, options).label(id)
                .labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final String key,
                                       final ConfigValue configValue,
                                       final List<String> defaultValue,
                                       final XAttributeDefType adType,
                                       final int cardinality,
                                       final List<String> options) {
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
                    field = Field.ofPasswordType(converter.convert(currentValue, String.class))
                            .render(new PeekablePasswordControl());
                } else {
                    field = Field.ofPasswordType(converter.convert(defaultValue, String.class))
                            .render(new PeekablePasswordControl());
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
                    field = Field.ofStringType(Character.toString(c))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
                } else {
                    final char c = converter.convert(defaultValue, char.class);
                    field = Field.ofStringType(Character.toString(c))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
                }
                break;
            case BOOLEAN_ARRAY:
                field = processArray(key, currentValue, defaultValue, boolean.class, BOOLEAN_ARRAY, cardinality);
                break;
            case BOOLEAN_LIST:
                field = processList(key, currentValue, defaultValue, BOOLEAN_LIST, cardinality);
                break;
            case DOUBLE_ARRAY:
                field = processArray(key, currentValue, defaultValue, double.class, DOUBLE_ARRAY, cardinality);
                break;
            case DOUBLE_LIST:
                field = processList(key, currentValue, defaultValue, DOUBLE_LIST, cardinality);
                break;
            case LONG_ARRAY:
                field = processArray(key, currentValue, defaultValue, long.class, LONG_ARRAY, cardinality);
                break;
            case LONG_LIST:
                field = processList(key, currentValue, defaultValue, LONG_LIST, cardinality);
                break;
            case INTEGER_ARRAY:
                field = processArray(key, currentValue, defaultValue, int.class, INTEGER_ARRAY, cardinality);
                break;
            case INTEGER_LIST:
                field = processList(key, currentValue, defaultValue, INTEGER_LIST, cardinality);
                break;
            case FLOAT_ARRAY:
                field = processArray(key, currentValue, defaultValue, float.class, FLOAT_ARRAY, cardinality);
                break;
            case FLOAT_LIST:
                field = processList(key, currentValue, defaultValue, FLOAT_LIST, cardinality);
                break;
            case CHAR_ARRAY:
                field = processArray(key, currentValue, defaultValue, char.class, CHAR_ARRAY, cardinality);
                break;
            case CHAR_LIST:
                field = processList(key, currentValue, defaultValue, CHAR_LIST, cardinality);
                break;
            case STRING_ARRAY:
                field = processArray(key, currentValue, defaultValue, String.class, STRING_ARRAY, cardinality);
                break;
            case STRING_LIST:
                field = processList(key, currentValue, defaultValue, STRING_LIST, cardinality);
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
                    field = Field.ofStringType(converter.convert(currentValue, String.class))
                            .multiline(convertedValue.length() > 100);
                } else {
                    final var convertedValue = converter.convert(defaultValue, String.class);
                    field = Field.ofStringType(converter.convert(defaultValue, String.class))
                            .multiline(convertedValue.length() > 100);
                }
                break;
        }
        if (field == null) {
            field = Field.ofStringType("");
        }
        return field;
    }

    private <T> Field<?> processArray(final String key,
                                      final Object currentValue,
                                      final List<String> defaultValue,
                                      final Class<T> clazz,
                                      final XAttributeDefType adType,
                                      final int cardinality) {
        final var    control = new MultipleCardinalityTextControl(key, adType, cardinality & 0xffffffff);
        List<String> convertedValue;
        T[]          effectiveValue;
        if (currentValue != null) {
            effectiveValue = converter.convert(currentValue, getArrayClass(clazz));
        } else {
            effectiveValue = converter.convert(defaultValue, getArrayClass(clazz));
        }
        if (adType == CHAR_ARRAY) {
            final List<Character> tempValue = converter.convert(effectiveValue, new TypeReference<List<Character>>() {
            });
            convertedValue = converter.convert(tempValue, new TypeReference<List<String>>() {
            });
        } else {
            convertedValue = converter.convert(effectiveValue, new TypeReference<List<String>>() {
            });
        }
        return Field.ofStringType(Joiner.on(lineSeparator()).join(convertedValue)).render(control).multiline(true);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T[]> getArrayClass(final Class<T> clazz) {
        return (Class<? extends T[]>) Array.newInstance(clazz, 0).getClass();
    }

    private Field<?> processList(final String key,
                                 final Object currentValue,
                                 final List<String> defaultValue,
                                 final XAttributeDefType adType,
                                 final int cardinality) {
        final var          control        = new MultipleCardinalityTextControl(key, adType, cardinality & 0xffffffff);
        final List<String> convertedValue = converter.convert(currentValue != null ? currentValue : defaultValue,
                new TypeReference<List<String>>() {
                                                  });
        return Field.ofStringType(Joiner.on(lineSeparator()).join(convertedValue)).render(control).multiline(true);
    }

    private List<ConfigValue> prepareConfigurationProperties() {
        final List<ConfigValue> properties = Lists.newArrayList();
        for (final Field<?> field : form.getFields()) {
            if (field instanceof final DataField<?, ?, ?> df) {
                if (!df.isEditable()) {
                    continue;
                }
                final var type = XAttributeDefType.values()[typeMappings.get(field)];
                if (df.getLabel().endsWith(".target") && df.getValue().toString().isBlank()) {
                    continue;
                }
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
