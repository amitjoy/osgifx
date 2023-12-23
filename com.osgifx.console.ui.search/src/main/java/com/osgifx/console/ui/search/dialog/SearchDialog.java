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
package com.osgifx.console.ui.search.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static com.osgifx.console.ui.search.filter.SearchComponent.BUNDLES;
import static com.osgifx.console.ui.search.filter.SearchComponent.COMPONENTS;
import static com.osgifx.console.ui.search.filter.SearchComponent.CONFIGURATIONS;
import static com.osgifx.console.ui.search.filter.SearchComponent.PACKAGES;
import static com.osgifx.console.ui.search.filter.SearchComponent.SERVICES;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchFilterManager;
import com.osgifx.console.ui.search.filter.SearchOperation;
import com.osgifx.console.util.fx.FxDialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;

public final class SearchDialog extends Dialog<FilterDTO> {

    @Log
    @Inject
    private FluentLogger                          logger;
    @Inject
    private SearchFilterManager                   filterManager;
    private Form                                  form;
    private FormRenderer                          formRenderer;
    private SingleSelectionField<SearchComponent> actorTypeField;
    private StringField                           userInputField;
    private SingleSelectionField<SearchOperation> operationTypeField;
    private SingleSelectionField<SearchFilter>    searchFilterTypeField;

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Search");
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/search.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setMinWidth(500);

        formRenderer = createForm();
        dialogPane.setContent(formRenderer);
        // @formatter:off
        dialogPane.lookupButton(ButtonType.OK)
                  .disableProperty()
                  .bind(userInputField.valueProperty().isEmpty()
                            .or(operationTypeField.selectionProperty().isNull()
                            .or(actorTypeField.selectionProperty().isNull()
                            .or(searchFilterTypeField.selectionProperty().isNull()))));
        // @formatter:on

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            try {
                if (data == ButtonData.OK_DONE) {
                    final var component = actorTypeField.getSelection();
                    final var filter    = searchFilterTypeField.getSelection();
                    final var operation = operationTypeField.getSelection();
                    final var userInput = userInputField.getValue();

                    final var dto = new SearchFilterDTO();

                    dto.predicate   = filter.predicate(userInput, operation);
                    dto.description = prepareDescription(component, filter, operation, userInput);

                    return new FilterDTO(dto, filter.component());
                }
                return null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Search cannot be performed");
                FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                return null;
            }
        });
    }

    private FormRenderer createForm() {
        form = Form.of(Section.of(initFields().toArray(new Field[0])).title("Search Parameters"));
        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        actorTypeField.selectionProperty().addListener((options, oldValue, newValue) -> {
            final var allFilters = filterManager.allFilters();
            final var list       = allFilters.get(newValue).stream()
                    .sorted(Comparator.comparing(SearchFilter::toString)).toList();
            searchFilterTypeField.items(list);
        });
        searchFilterTypeField.selectionProperty().addListener((options, oldValue, newValue) -> {
            final var list = newValue.supportedOperations().stream().toList();
            operationTypeField.items(list);
            userInputField.placeholder(newValue.placeholder());
            userInputField.validate(newValue.validator());
        });
        return renderer;
    }

    private List<Field<?>> initFields() {
        actorTypeField        = Field
                .ofSingleSelectionType(List.of(BUNDLES, COMPONENTS, CONFIGURATIONS, PACKAGES, SERVICES))
                .label("Search for");
        searchFilterTypeField = Field.ofSingleSelectionType(List.of(SearchFilter.DUMMY)).label("having");
        operationTypeField    = Field.ofSingleSelectionType(List.of(SearchOperation.DUMMY)).label("that");
        userInputField        = Field.ofStringType("").label("Search Input").multiline(true);

        searchFilterTypeField.items(List.of());
        operationTypeField.items(List.of());

        return List.of(actorTypeField, searchFilterTypeField, operationTypeField, userInputField);
    }

    private String prepareDescription(final SearchComponent component,
                                      final SearchFilter filter,
                                      final SearchOperation operation,
                                      final String userInput) {
        return "Search for '" + component + "' having '" + filter + "' that '" + operation + "' " + userInput;
    }

}
