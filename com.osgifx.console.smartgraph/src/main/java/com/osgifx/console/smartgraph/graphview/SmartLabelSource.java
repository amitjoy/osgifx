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
package com.osgifx.console.smartgraph.graphview;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Method annotation to override an element's label provider.
 *
 * The annotated method must return a value, otherwise the a reflection
 * exception will be thrown.
 *
 * By default the text label is obtained from the toString method if this
 * annotation is not present in any other class method; this is also the case
 * with String and other boxed-types, e.g., Integer, Double, etc.
 *
 * If multiple annotations exist, the behavior is undefined.
 */

@Target(METHOD)
@Retention(RUNTIME)
public @interface SmartLabelSource {

}
