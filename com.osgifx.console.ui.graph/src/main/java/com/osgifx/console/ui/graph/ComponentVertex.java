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
package com.osgifx.console.ui.graph;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public record ComponentVertex(String name) {

    public static final Function<String, String> VERTEX_ID_FUNCTION = Function.identity();
    public static final UnaryOperator<String> DOT_ID_FUNCTION = name -> name.replaceAll("[^a-zA-Z0-9]", "_");

    @Override
    public String toString() {
        return name;
    }

    public String toDotID() {
        return DOT_ID_FUNCTION.apply(name);
    }

}
