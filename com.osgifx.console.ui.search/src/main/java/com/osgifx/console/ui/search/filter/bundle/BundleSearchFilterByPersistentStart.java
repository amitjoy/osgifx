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
package com.osgifx.console.ui.search.filter.bundle;

import static com.osgifx.console.ui.search.filter.SearchComponent.BUNDLES;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.component.annotations.Component;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;
import com.google.common.base.VerifyException;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class BundleSearchFilterByPersistentStart implements SearchFilter {

    @Override
    public Predicate<XBundleDTO> predicate(final String input, final SearchOperation searchOperation) {
        return switch (searchOperation) {
            case EQUALS_TO -> bundle -> bundle.isPersistentlyStarted == BooleanUtils.toBoolean(input.strip());
            default -> throw new VerifyException("no matching case found");
        };
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO);
    }

    @Override
    public SearchComponent component() {
        return BUNDLES;
    }

    @Override
    public String placeholder() {
        return "Boolean Value (Case-Insensitive) (true or false)";
    }

    @Override
    public Validator<String> validator() {
        return CustomValidator.forPredicate(e -> toBooleanObject(e.strip()) != null, "Invalid Boolean Value");
    }

    @Override
    public String toString() {
        return "Is Persistently Started?";
    }

}
