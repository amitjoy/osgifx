/*******************************************************************************
 * COPYRIGHT 2021-2024 AMIT KUMAR MONDAL
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.dto;

import java.util.function.Predicate;
import org.osgi.dto.DTO;

/**
 * A Data Transfer Object (DTO) that encapsulates a search filter.
 * This DTO is used to represent a predicate and its description,
 * which can be applied to filter search results based on specified criteria.
 */
public class SearchFilterDTO extends DTO {

    /** The predicate used to filter search results. */
    public Predicate<?> predicate;

    /** The description of the search filter. */
    public String description;

}
