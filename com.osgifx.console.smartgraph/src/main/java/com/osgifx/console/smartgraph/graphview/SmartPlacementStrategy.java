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

import java.util.Collection;

import com.osgifx.console.smartgraph.graph.Graph;

/**
 * Contains the method that should be implemented when creating new vertex
 * placement strategies.
 */
public interface SmartPlacementStrategy {

    /**
     * Implementations of placement strategies must implement this interface.
     *
     * Should use the {@link SmartGraphVertex#setPosition(double, double) } method
     * to place individual vertices.
     *
     *
     * @param <V>      Generic type for element stored at vertices.
     * @param <E>      Generic type for element stored at edges.
     * @param width    Width of the area in which to place the vertices.
     * @param height   Height of the area in which to place the vertices.
     * @param theGraph Reference to the {@link Graph} containing the graph model.
     *                 Can use methods to check for additional information
     *                 pertaining the model.
     *
     * @param vertices Collection of {@link SmartGraphVertex} to place.
     *
     */
    <V, E> void place(double width, double height, Graph<V, E> theGraph, Collection<? extends SmartGraphVertex<V>> vertices);
}
