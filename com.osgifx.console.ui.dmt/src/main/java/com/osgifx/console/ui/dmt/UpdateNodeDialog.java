/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.dmt;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.ui.dmt.UpdateNodeDialog.UpdateDialogDTO;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;

public final class UpdateNodeDialog extends Dialog<UpdateDialogDTO> {

    record UpdateDialogDTO(String uri, Object value, DmtDataType format) {
    }

    @Log
    @Inject
    private FluentLogger    logger;
    private Field<?>        valueField;
    private Form            form;
    private final Converter converter = Converters.standardConverter();

    private static final String TIME_FORMAT      = "hhmmss";
    private static final String DATE_FORMAT      = "CCYYMMDD";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:SS'Z'";

    public void init(final XDmtNodeDTO dmtNode) {
        final var formRenderer = createForm(dmtNode);
        if (formRenderer == null) {
            return;
        }
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Update DMT Node");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/node-update.png").toString()));

        final var updateButtonType = new ButtonType("Update", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        dialogPane.setContent(formRenderer);
        getDialogPane().lookupButton(updateButtonType).disableProperty()
                .bind(form.changedProperty().not().or(form.validProperty().not()));

        setResultConverter(dialogButton -> {
            try {
                return dialogButton == updateButtonType
                        ? new UpdateDialogDTO(dmtNode.uri, getValue(valueField), dmtNode.format)
                        : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("DMT node cannot be updated");
                throw e;
            }
        });
    }

    private Object getValue(final Field<?> field) {
        if (field instanceof final DataField<?, ?, ?> df) {
            if (!df.isEditable()) {
                return null;
            }
            return df.getValue();
        }
        return null;
    }

    private FormRenderer createForm(final XDmtNodeDTO dmtNode) {
        final var type = dmtNode.format;
        initNode(dmtNode);
        if (valueField == null) {
            logger.atInfo().log("Value field is null for '%s'", dmtNode.uri);
            return null;
        }
        form = Form.of(Section.of(valueField).title("URI: " + dmtNode.uri + " Type: " + type));
        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private void initNode(final XDmtNodeDTO dmtNode) {
        switch (dmtNode.format) {
            case STRING:
                valueField = Field.ofStringType(dmtNode.value).required(true);
                break;
            case BOOLEAN:
                valueField = Field.ofBooleanType(convert(dmtNode.value, boolean.class));
                break;
            case FLOAT:
                valueField = Field.ofDoubleType(convert(dmtNode.value, double.class)).required(true);
                break;
            case LONG, INTEGER:
                valueField = Field.ofIntegerType(convert(dmtNode.value, int.class)).required(true);
                break;
            case NULL:
                // no need to handle as the value will remain null
                break;
            case BINARY:
                // no need to handle as we are dealing with byte array
                break;
            case XML:
                valueField = Field.ofStringType(dmtNode.value).multiline(true).required(true);
                break;
            case BASE64:
                // no need to handle as we are dealing with byte array
                break;
            case DATE:
                valueField = Field.ofStringType(dmtNode.value).validate(CustomValidator.forPredicate(v -> {
                    try {
                        final var baseYear              = Year.now().minusYears(40).getValue();
                        final var originalDateFormatter = new DateTimeFormatterBuilder()
                                .appendValueReduced(YEAR, 2, 2, baseYear).appendPattern("MMdd").toFormatter();
                        final var date                  = LocalDate.parse(v, originalDateFormatter);

                        date.format(BASIC_ISO_DATE);
                        return true;
                    } catch (final Exception ex) {
                        return false;
                    }
                }, "DMT date must be in the format of '" + DATE_FORMAT + "'")).required(true);
                break;
            case DATE_TIME:
                valueField = Field.ofStringType(dmtNode.value).validate(CustomValidator.forPredicate(v -> {
                    try {
                        LocalDateTime.parse(v, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
                        return true;
                    } catch (final Exception ex) {
                        return false;
                    }
                }, "DMT date time must be in the format of '" + DATE_TIME_FORMAT + "'")).required(true);
                break;
            case TIME:
                valueField = Field.ofStringType(dmtNode.value).validate(CustomValidator.forPredicate(v -> {
                    try {
                        LocalTime.parse(v, DateTimeFormatter.ofPattern(TIME_FORMAT));
                        return true;
                    } catch (final Exception ex) {
                        return false;
                    }
                }, "DMT time must be in the format of '" + TIME_FORMAT + "'")).required(true);
                break;
            default:
                break;
        }
        if (valueField != null) {
            valueField.label("Value");
        }
    }

    private <T> T convert(final String value, final Class<T> type) {
        return converter.convert(value).to(type);
    }

}
