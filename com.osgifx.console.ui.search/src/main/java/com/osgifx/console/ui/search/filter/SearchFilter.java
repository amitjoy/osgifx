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
package com.osgifx.console.ui.search.filter;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.dlsc.formsfx.model.validators.Validator;
import com.google.common.base.Predicates;

public interface SearchFilter {

    Predicate<?> predicate(String input, SearchOperation operation);

    Collection<SearchOperation> supportedOperations();

    SearchComponent component();

    String placeholder();

    Validator<String> validator();

    SearchFilter DUMMY = new SearchFilter() {

        @Override
        public Predicate<Object> predicate(final String input, final SearchOperation operation) {
            return Predicates.alwaysTrue();
        }

        @Override
        public Collection<SearchOperation> supportedOperations() {
            return List.of();
        }

        @Override
        public SearchComponent component() {
            return SearchComponent.DUMMY;
        }

        @Override
        public String placeholder() {
            return null;
        }

        @Override
        public Validator<String> validator() {
            return null;
        }
    };

}
