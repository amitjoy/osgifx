/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import com.osgifx.console.smartgraph.graph.Edge;
import com.osgifx.console.smartgraph.graph.Vertex;

/**
 * A graph edge visually connects two {@link Vertex} of type <code>V</code>.
 * <br>
 * Concrete edge implementations used by {@link SmartGraphPanel} should
 * implement this interface as this type is the only one exposed to the user.
 *
 * @param <E> Type stored in the underlying edge
 * @param <V> Type of connecting vertex
 *
 * @see Vertex
 * @see SmartGraphPanel
 */
public interface SmartGraphEdge<E, V> extends SmartStylableNode {

	/**
	 * Returns the underlying (stored reference) graph edge.
	 *
	 * @return edge reference
	 *
	 * @see SmartGraphPanel
	 */
	Edge<E, V> getUnderlyingEdge();

	/**
	 * Returns the attached arrow of the edge, for styling purposes.
	 *
	 * The arrows are only used with directed graphs.
	 *
	 * @return arrow reference; null if does not exist.
	 */
	SmartStylableNode getStylableArrow();

	/**
	 * Returns the label node for further styling.
	 *
	 * @return the label node.
	 */
	SmartStylableNode getStylableLabel();
}
