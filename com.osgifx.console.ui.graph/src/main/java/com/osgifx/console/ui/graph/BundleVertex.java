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
package com.osgifx.console.ui.graph;

import java.util.function.BiFunction;

public record BundleVertex(String symbolicName, long id) {

    public static final BiFunction<String, Long, String> VERTEX_ID_FUNCTION = (bsn, id) -> bsn + ":" + id;
    public static final BiFunction<String, Long, String> DOT_ID_FUNCTION = (bsn, id) -> (bsn + '_' + id)
            .replaceAll("[^a-zA-Z0-9]", "_");

    @Override
    public String toString() {
        return VERTEX_ID_FUNCTION.apply(symbolicName, id);
    }

    public String toDotID() {
        return DOT_ID_FUNCTION.apply(symbolicName, id);
    }

}
