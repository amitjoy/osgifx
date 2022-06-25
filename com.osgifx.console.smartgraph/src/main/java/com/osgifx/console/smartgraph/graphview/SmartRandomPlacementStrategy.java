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
import java.util.Random;

import com.osgifx.console.smartgraph.graph.Graph;

/**
 * Scatters the vertices randomly.
 *
 * @see SmartPlacementStrategy
 */
public class SmartRandomPlacementStrategy implements SmartPlacementStrategy {

    @Override
    public <V, E> void place(final double width, final double height, final Graph<V, E> theGraph,
            final Collection<? extends SmartGraphVertex<V>> vertices) {

        final var rand = new Random();

        for (final SmartGraphVertex<V> vertex : vertices) {

            final var x = rand.nextDouble() * width;
            final var y = rand.nextDouble() * height;

            vertex.setPosition(x, y);

        }
    }

}
