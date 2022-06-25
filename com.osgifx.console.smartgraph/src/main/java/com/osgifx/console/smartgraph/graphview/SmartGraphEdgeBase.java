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
package com.osgifx.console.smartgraph.graphview;

/**
 * Used as a super-type for all different concrete edge implementations by
 * {@link SmartGraphPanel}, e.g., line and curves. <br>
 * An edge can have an attached arrow.
 *
 * @param <E> Type stored in the underlying edge
 * @param <V> Type of connecting vertex
 *
 * @see SmartArrow
 * @see SmartGraphEdge
 * @see SmartLabelledNode
 * @see SmartGraphPanel
 */
public interface SmartGraphEdgeBase<E, V> extends SmartGraphEdge<E, V>, SmartLabelledNode {

    /**
     * Attaches a {@link SmartArrow} to this edge, binding its position/rotation.
     *
     * @param arrow arrow to attach
     */
    void attachArrow(SmartArrow arrow);

    /**
     * Returns the attached {@link SmartArrow}, if any.
     *
     * @return reference of the attached arrow; null if none.
     */
    SmartArrow getAttachedArrow();

}
