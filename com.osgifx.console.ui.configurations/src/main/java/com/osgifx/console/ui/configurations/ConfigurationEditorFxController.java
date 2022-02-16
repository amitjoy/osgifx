/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_DELETED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XObjectClassDefDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.configurations.converter.ConfigurationConverter;
import com.osgifx.console.util.fx.FxDialog;

import javafx.beans.binding.BooleanBinding;
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
    @Inject
    private IEclipseContext        context;
    @Inject
    private ConfigurationConverter converter;
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
        final String             pid = config.pid;
        final XObjectClassDefDTO ocd = config.ocd;

        deleteConfigButton.setDisable(config.properties == null || config.isFactory || config.pid == null);
        deleteConfigButton.setOnAction(event -> {
            logger.atInfo().log("Configuration delete request has been sent for PID '%s'", pid);
            deleteConfiguration(pid);
        });
        saveConfigButton.setOnAction(event -> {
            final Map<String, Object> properties   = prepareConfigurationProperties();
            String                    effectivePID = null;
            if (pid == null && ocd != null) {
                effectivePID = ocd.pid;
            } else {
                effectivePID = pid;
            }
            if (config.isFactory) {
                final String ocdFactoryPid = ocd.factoryPid;
                logger.atInfo().log("Factory configuration create request has been sent for factory PID '%s'", ocdFactoryPid);
                createFactoryConfiguration(ocdFactoryPid, properties);
                return;
            }
            logger.atInfo().log("Configuration create request has been sent for PID '%s'", effectivePID);
            createOrUpdateConfiguration(effectivePID, properties);
        });
        cancelButton.setOnAction(e -> form.reset());

        final BooleanProperty isPersisted              = new SimpleBooleanProperty(config.isPersisted);
        final BooleanBinding  isPersistedConfigBinding = new When(isPersisted).then(true).otherwise(false);

        saveConfigButton.disableProperty().bind(form.changedProperty().not().or(form.validProperty().not()).and(isPersistedConfigBinding));
    }

    private void deleteConfiguration(final String pid) {
        final XResultDTO result = supervisor.getAgent().deleteConfiguration(pid);
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

    private void createFactoryConfiguration(final String factoryPID, final Map<String, Object> properties) {
        final XResultDTO result = supervisor.getAgent().createFactoryConfiguration(factoryPID, properties);
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

    private void createOrUpdateConfiguration(final String pid, final Map<String, Object> properties) {
        final XResultDTO result = supervisor.getAgent().createOrUpdateConfiguration(pid, properties);
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
        final List<Field<?>> fields = Lists.newArrayList();

        for (final Entry<String, Object> entry : config.properties.entrySet()) {
            final String key   = entry.getKey();
            final Object value = entry.getValue();

            final XAttributeDefType attrDefType = XAttributeDefType.getType(value);
            final Field<?>          field       = initFieldFromType(key, value, null, attrDefType, null, false).label(key);

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

        return initFieldFromType(id, currentValue, defaultVal, type, options, true).label(id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final String key, final Object currentValue, final List<String> defaultValue,
            final XAttributeDefType adType, final List<String> options, final boolean hasOCD) {
        Field<?> field = null;
        switch (adType) {
            case LONG, INTEGER:
                if (options != null && !options.isEmpty()) {
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue, String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue, String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
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
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue, String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue, String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
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
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue, String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue, String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
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
                    field = Field.ofPasswordType(converter.convert(currentValue, String.class));
                } else {
                    field = Field.ofPasswordType(converter.convert(defaultValue, String.class));
                }
                break;
            case CHAR:
                if (options != null && !options.isEmpty()) {
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue, String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue, String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<String>>() {
                    }), selection);
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofStringType(converter.convert(currentValue, String.class))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
                } else {
                    field = Field.ofStringType(converter.convert(defaultValue, String.class))
                            .validate(StringLengthValidator.exactly(1, "Length must be 1"));
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
                field = processArray(key, currentValue, defaultValue, options, hasOCD, char.class, XAttributeDefType.CHAR_ARRAY);
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
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue, String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue, String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options, new TypeReference<List<String>>() {
                    }), selection);
                    break;
                }
                if (currentValue != null) {
                    final String convertedValue = converter.convert(currentValue, String.class);
                    field = Field.ofStringType(converter.convert(currentValue, String.class)).multiline(convertedValue.length() > 100);
                } else {
                    final String convertedValue = converter.convert(defaultValue, String.class);
                    field = Field.ofStringType(converter.convert(defaultValue, String.class)).multiline(convertedValue.length() > 100);
                }
                break;
        }
        if (field == null) {
            field = Field.ofStringType("");
        }
        return field;
    }

    @SuppressWarnings("unchecked")
    private <T> Field<?> processArray(final String key, final Object currentValue, final List<String> defaultValue,
            final List<String> options, final boolean hasOCD, final Class<T> clazz, final XAttributeDefType adType) {
        Field<?> field;
        if (hasOCD) {
            T[] effectiveValue;
            if (currentValue != null) {
                effectiveValue = converter.convert(currentValue, getArrayClass(clazz));
            } else if (defaultValue != null) {
                effectiveValue = converter.convert(defaultValue, getArrayClass(clazz));
            } else {
                effectiveValue = (T[]) Array.newInstance(clazz, 0);
            }
            if (options != null && !options.isEmpty()) {
                final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }), selections);
            } else {
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }));
            }
        } else {
            final MultipleCardinalityTextControl control = new MultipleCardinalityTextControl(key, adType);
            ContextInjectionFactory.inject(control, context);

            final List<String> convertedValue = converter.convert(currentValue, new TypeReference<List<String>>() {
            });
            field = Field.ofStringType(String.join(",", convertedValue)).render(control);
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
            } else if (defaultValue != null) {
                effectiveValue = converter.convert(defaultValue, new TypeReference<List<T>>() {
                });
            } else {
                effectiveValue = List.of();
            }
            if (options != null && !options.isEmpty()) {
                final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }), selections);
            } else {
                field = Field.ofMultiSelectionType(converter.convert(options, new TypeReference<List<T>>() {
                }));
            }
        } else {
            final MultipleCardinalityTextControl control = new MultipleCardinalityTextControl(key, adType);
            ContextInjectionFactory.inject(control, context);

            final List<String> convertedValue = converter.convert(currentValue, new TypeReference<List<String>>() {
            });
            field = Field.ofStringType(String.join(",", convertedValue)).render(control);
        }
        return field;
    }

    private Map<String, Object> prepareConfigurationProperties() {
        final Map<String, Object> properties = Maps.newHashMap();
        for (final Field<?> field : form.getFields()) {
            if (field instanceof final DataField<?, ?, ?> df) {
                if (!df.isEditable()) {
                    continue;
                }
                final Object originalType = convertToRequestedType(field, df.getValue());
                properties.put(field.getLabel(), originalType);
            }
        }
        return properties;
    }

    private Object convertToRequestedType(final Field<?> field, final Object value) {
        // this controller cannot be loaded by FXMLLoader if the 'typeMappings' values are of type XAttributeDefType
        final XAttributeDefType type = XAttributeDefType.values()[typeMappings.get(field)];
        return converter.convert(value, type);
    }

    private Object getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

}
