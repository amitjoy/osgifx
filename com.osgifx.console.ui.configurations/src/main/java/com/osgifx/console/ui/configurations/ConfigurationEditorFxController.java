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
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.controls.SimpleCheckBoxControl;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XObjectClassDefDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.supervisor.Supervisor;
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
    private Converter              converter;
    private FormRenderer           formRenderer;
    private Map<Field<?>, Integer> typeMappings;
    private List<String>           uneditableProperties;

    @FXML
    public void initialize() {
        typeMappings         = Maps.newHashMap();
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

        return initFieldFromType(currentValue, defaultVal, type, options).label(id).labelDescription(ad.description);
    }

    private Field<?> initFieldFromType(final Object currentValue, final List<String> defaultValue, final XAttributeDefType adType,
            final List<String> options) {
        Field<?> field = null;
        switch (adType) {
            case LONG, INTEGER:
                if (options != null && !options.isEmpty()) {
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                    }), selection);
                    break;
                }
                if (currentValue != null) {
                    field = Field.ofIntegerType(converter.convert(currentValue).to(int.class));
                } else {
                    field = Field.ofIntegerType(converter.convert(defaultValue).to(int.class));
                }
                break;
            case FLOAT, DOUBLE:
                if (options != null && !options.isEmpty()) {
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                    }), selection);
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
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                    }), selection);
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
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selection);
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
                if (options != null && !options.isEmpty()) {
                    boolean[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(boolean[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(boolean[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Boolean> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Boolean>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue);
                }
                break;
            case BOOLEAN_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Boolean> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Boolean>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Boolean>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Boolean>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Boolean> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Boolean>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue);
                }
                break;
            case DOUBLE_ARRAY:
                if (options != null && !options.isEmpty()) {
                    double[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(double[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(double[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Double> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Double>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue);
                }
                break;
            case DOUBLE_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Double> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Double>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Double>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Double> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Double>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue);
                }
                break;
            case LONG_ARRAY:
                if (options != null && !options.isEmpty()) {
                    long[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(long[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(long[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Double>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Double> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Double>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case LONG_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Long> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Long>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Long>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Long>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Long> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Long>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case INTEGER_ARRAY:
                if (options != null && !options.isEmpty()) {
                    int[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(int[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(int[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                    }), selections);
                    break;
                }
                if (currentValue != null) {
                    final List<Integer> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Integer>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case INTEGER_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Integer> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Integer>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Integer>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Integer>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Integer> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Integer>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case FLOAT_ARRAY:
                if (options != null && !options.isEmpty()) {
                    float[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(float[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(float[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Float>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Float> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Float>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case FLOAT_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Float> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Float>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Float>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Float>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Float> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Float>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case CHAR_ARRAY:
                if (options != null && !options.isEmpty()) {
                    char[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(char[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(char[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Character>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Character> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Character>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case CHAR_LIST:
                if (options != null && !options.isEmpty()) {
                    List<Character> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<Character>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<Character>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<Character>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<Character> convertedValue = converter.convert(currentValue).to(new TypeReference<List<Character>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case STRING_ARRAY:
                if (options != null && !options.isEmpty()) {
                    String[] effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String[].class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String[].class);
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<String> convertedValue = converter.convert(currentValue).to(new TypeReference<List<String>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case STRING_LIST:
                if (options != null && !options.isEmpty()) {
                    List<String> effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(new TypeReference<List<String>>() {
                        });
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(new TypeReference<List<String>>() {
                        });
                    }
                    final List<Integer> selections = Stream.of(effectiveValue).map(v -> options.indexOf(v.toString())).toList();
                    field = Field.ofMultiSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selections).render(new SimpleCheckBoxControl<>());
                    break;
                }
                if (currentValue != null) {
                    final List<String> convertedValue = converter.convert(currentValue).to(new TypeReference<List<String>>() {
                    });
                    field = Field.ofMultiSelectionType(convertedValue).render(new SimpleCheckBoxControl<>());
                }
                break;
            case STRING:
            default:
                if (options != null && !options.isEmpty()) {
                    String effectiveValue = null;
                    if (currentValue != null) {
                        effectiveValue = converter.convert(currentValue).to(String.class);
                    } else {
                        effectiveValue = converter.convert(defaultValue).to(String.class);
                    }
                    final int selection = options.indexOf(effectiveValue);
                    field = Field.ofSingleSelectionType(converter.convert(options).to(new TypeReference<List<String>>() {
                    }), selection);
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
        return switch (type) {
            case STRING_ARRAY -> converter.convert(value).to(String[].class);
            case STRING_LIST -> converter.convert(value).to(new TypeReference<List<String>>() {
            });
            case INTEGER_ARRAY -> converter.convert(value).to(int[].class);
            case INTEGER_LIST -> converter.convert(value).to(new TypeReference<List<Integer>>() {
            });
            case BOOLEAN_ARRAY -> converter.convert(value).to(boolean[].class);
            case BOOLEAN_LIST -> converter.convert(value).to(new TypeReference<List<Boolean>>() {
            });
            case DOUBLE_ARRAY -> converter.convert(value).to(double[].class);
            case DOUBLE_LIST -> converter.convert(value).to(new TypeReference<List<Double>>() {
            });
            case FLOAT_ARRAY -> converter.convert(value).to(float[].class);
            case FLOAT_LIST -> converter.convert(value).to(new TypeReference<List<Float>>() {
            });
            case CHAR_ARRAY -> converter.convert(value).to(char[].class);
            case CHAR_LIST -> converter.convert(value).to(new TypeReference<List<Character>>() {
            });
            case LONG_ARRAY -> converter.convert(value).to(long[].class);
            case LONG_LIST -> converter.convert(value).to(new TypeReference<List<Long>>() {
            });
            default -> converter.convert(value).to(XAttributeDefType.clazz(type));
        };
    }

    private Object getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

}
