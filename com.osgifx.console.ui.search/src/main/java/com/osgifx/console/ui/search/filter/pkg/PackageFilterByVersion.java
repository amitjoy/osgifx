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
package com.osgifx.console.ui.search.filter.pkg;

import static com.osgifx.console.ui.search.filter.SearchComponent.PACKAGES;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;
import static com.osgifx.console.ui.search.filter.SearchOperation.IS_GREATER_THAN;
import static com.osgifx.console.ui.search.filter.SearchOperation.IS_LESS_THAN;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;

import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class PackageFilterByVersion implements SearchFilter {

    @Override
    public Predicate<PackageDTO> predicate(final String input, final SearchOperation searchOperation) throws Exception {
        final var version = new Version(input.strip());

        return switch (searchOperation) {
            case EQUALS_TO -> pkg -> {
                final var bVersion = new Version(pkg.version);
                return bVersion.compareTo(version) == 0;
            };
            case IS_GREATER_THAN -> pkg -> {
                final var bVersion = new Version(pkg.version);
                return bVersion.compareTo(version) > 0;
            };
            case IS_LESS_THAN -> pkg -> {
                final var bVersion = new Version(pkg.version);
                return bVersion.compareTo(version) < 0;
            };
            default -> throw new RuntimeException("does not match any matching case");
        };
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO, IS_GREATER_THAN, IS_LESS_THAN);
    }

    @Override
    public SearchComponent component() {
        return PACKAGES;
    }

    @Override
    public String placeholder() {
        return "Conformant Version Format";
    }

    @Override
    public String toString() {
        return "Version";
    }

}
