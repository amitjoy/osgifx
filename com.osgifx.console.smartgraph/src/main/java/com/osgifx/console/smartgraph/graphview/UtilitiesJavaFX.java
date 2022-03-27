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

import java.util.List;

import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Utility methods for JavaFX.
 */
public class UtilitiesJavaFX {

	private UtilitiesJavaFX() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	/**
	 * Determines the closest node that resides in the x,y scene position, if any.
	 * <br>
	 * Obtained from: http://fxexperience.com/2016/01/node-picking-in-javafx/
	 *
	 * @param node   parent node
	 * @param sceneX x-coordinate of picking point
	 * @param sceneY y-coordinate of picking point
	 *
	 * @return topmost node containing (sceneX, sceneY) point
	 */
	public static Node pick(final Node node, final double sceneX, final double sceneY) {
		var p = node.sceneToLocal(sceneX, sceneY, true /* rootScene */);

		// check if the given node has the point inside it, or else we drop out
		if (!node.contains(p)) {
			return null;
		}

		// at this point we know that _at least_ the given node is a valid
		// answer to the given point, so we will return that if we don't find
		// a better child option
		if (node instanceof Parent) {
			// we iterate through all children in reverse order, and stop when we find a
			// match.
			// We do this as we know the elements at the end of the list have a higher
			// z-order, and are therefore the better match, compared to children that
			// might also intersect (but that would be underneath the element).
			Node             bestMatchingChild = null;
			final List<Node> children          = ((Parent) node).getChildrenUnmodifiable();
			for (var i = children.size() - 1; i >= 0; i--) {
				final var child = children.get(i);
				p = child.sceneToLocal(sceneX, sceneY, true /* rootScene */);
				if (child.isVisible() && !child.isMouseTransparent() && child.contains(p)) {
					bestMatchingChild = child;
					break;
				}
			}

			if (bestMatchingChild != null) {
				return pick(bestMatchingChild, sceneX, sceneY);
			}
		}

		return node;
	}
}
