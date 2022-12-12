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
package com.osgifx.console.ui.search.filter;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SearchFilterManager.class)
public final class SearchFilterManager {

    @Reference
    private volatile List<SearchFilter> searchFilters;

    public List<SearchFilter> bundles() {
        return list(SearchComponent.BUNDLES);
    }

    public List<SearchFilter> components() {
        return list(SearchComponent.COMPONENTS);
    }

    public List<SearchFilter> configurations() {
        return list(SearchComponent.CONFIGURATIONS);
    }

    public List<SearchFilter> services() {
        return list(SearchComponent.SERVICES);
    }

    public List<SearchFilter> packages() {
        return list(SearchComponent.PACKAGES);
    }

    public Map<SearchComponent, List<SearchFilter>> allFilters() {
        return searchFilters.stream().collect(groupingBy(SearchFilter::component));
    }

    private List<SearchFilter> list(final SearchComponent component) {
        return searchFilters.stream().filter(filter -> filter.component() == component).toList();
    }

}
