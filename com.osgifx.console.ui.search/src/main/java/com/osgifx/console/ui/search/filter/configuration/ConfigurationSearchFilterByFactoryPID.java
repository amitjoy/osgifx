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
package com.osgifx.console.ui.search.filter.configuration;

import static com.osgifx.console.ui.search.filter.SearchComponent.CONFIGURATIONS;
import static com.osgifx.console.ui.search.filter.SearchOperation.CONTAINS;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class ConfigurationSearchFilterByFactoryPID implements SearchFilter {

    @Override
    public Predicate<XConfigurationDTO> predicate(final String input, final SearchOperation searchOperation)
            throws Exception {
        return switch (searchOperation) {
            case EQUALS_TO -> conf -> StringUtils.equalsIgnoreCase(conf.factoryPid, input.strip());
            case CONTAINS -> conf -> StringUtils.containsIgnoreCase(conf.factoryPid, input.strip());
            default -> throw new RuntimeException("does not match any matching case");
        };
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO, CONTAINS);
    }

    @Override
    public SearchComponent component() {
        return CONFIGURATIONS;
    }

    @Override
    public String placeholder() {
        return "Factory PID (Case-Insensitive)";
    }

    @Override
    public String toString() {
        return "Factory PID";
    }

}
