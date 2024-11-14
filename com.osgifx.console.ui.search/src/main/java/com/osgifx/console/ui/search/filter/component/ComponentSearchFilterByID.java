/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.ui.search.filter.component;

import static com.osgifx.console.ui.search.filter.SearchComponent.COMPONENTS;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;
import static com.osgifx.console.ui.search.filter.SearchOperation.IS_GREATER_THAN;
import static com.osgifx.console.ui.search.filter.SearchOperation.IS_LESS_THAN;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Component;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class ComponentSearchFilterByID implements SearchFilter {

    @Override
    public Predicate<XComponentDTO> predicate(final String input, final SearchOperation searchOperation) {
        final var cId = Longs.tryParse(input.strip());
        return switch (searchOperation) {
            case EQUALS_TO -> component -> component.id == cId;
            case IS_GREATER_THAN -> component -> component.id > cId;
            case IS_LESS_THAN -> component -> component.id < cId;
            default -> throw new VerifyException("no matching case found");
        };
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO, IS_GREATER_THAN, IS_LESS_THAN);
    }

    @Override
    public SearchComponent component() {
        return COMPONENTS;
    }

    @Override
    public String placeholder() {
        return "Component ID Number";
    }

    @Override
    public Validator<String> validator() {
        return CustomValidator.forPredicate(e -> Longs.tryParse(e.strip()) != null, "Invalid Long Number");
    }

    @Override
    public String toString() {
        return "ID";
    }

}
