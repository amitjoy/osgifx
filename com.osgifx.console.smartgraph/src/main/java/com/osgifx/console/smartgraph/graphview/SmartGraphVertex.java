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

import com.osgifx.console.smartgraph.graph.Graph;
import com.osgifx.console.smartgraph.graph.Vertex;

/**
 * Abstracts the internal representation and behavior of a visualized graph
 * vertex.
 *
 * @param <V> Type stored in the underlying vertex
 *
 * @see SmartGraphPanel
 */
public interface SmartGraphVertex<V> extends SmartStylableNode {

	/**
	 * Returns the underlying (stored reference) graph vertex.
	 *
	 * @return vertex reference
	 *
	 * @see Graph
	 */
	Vertex<V> getUnderlyingVertex();

	/**
	 * Sets the position of this vertex in panel coordinates.
	 *
	 * Apart from its usage in the {@link SmartGraphPanel}, this method should only
	 * be called when implementing {@link SmartPlacementStrategy}.
	 *
	 * @param x x-coordinate for the vertex
	 * @param y y-coordinate for the vertex
	 */
	void setPosition(double x, double y);

	/**
	 * Return the center x-coordinate of this vertex in panel coordinates.
	 *
	 * @return x-coordinate of the vertex
	 */
	double getPositionCenterX();

	/**
	 * Return the center y-coordinate of this vertex in panel coordinates.
	 *
	 * @return y-coordinate of the vertex
	 */
	double getPositionCenterY();

	/**
	 * Returns the circle radius used to represent this vertex.
	 *
	 * @return circle radius
	 */
	double getRadius();

	/**
	 * Returns the label node for further styling.
	 *
	 * @return the label node.
	 */
	SmartStylableNode getStylableLabel();
}
