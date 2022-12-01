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
package com.osgifx.console.ui.search.filter.component;

import static com.osgifx.console.ui.search.filter.SearchComponent.COMPONENTS;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class ComponentSearchFilterByConfigurationPolicy implements SearchFilter {

    @Override
    public Predicate<XComponentDTO> predicate(final String input, final SearchOperation searchOperation)
            throws Exception {
        return switch (searchOperation) {
            case EQUALS_TO -> component -> component.configurationPolicy.equalsIgnoreCase(input.trim());
            default -> throw new RuntimeException("does not match any matching case");
        };
    }

    @Override
    public String toString() {
        return "Configuration Policy";
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO);
    }

    @Override
    public SearchComponent component() {
        return COMPONENTS;
    }

}
