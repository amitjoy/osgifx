/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import javafx.geometry.Point2D;

/**
 * Class with utility methods for Point2D instances and force-based layout
 * computations.
 */
public class UtilitiesPoint2D {

    private UtilitiesPoint2D() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * Rotate a point around a pivot point by a specific degrees amount
     *
     * @param point point to rotate
     * @param pivot pivot point
     * @param angle_degrees rotation degrees
     * @return rotated point
     */
    public static Point2D rotate(final Point2D point, final Point2D pivot, final double angle_degrees) {
        final var angle = Math.toRadians(angle_degrees); // angle_degrees * (Math.PI/180); //to radians

        final var sin = Math.sin(angle);
        final var cos = Math.cos(angle);

        // translate to origin
        final var result = point.subtract(pivot);

        // rotate point
        final var rotatedOrigin = new Point2D(result.getX() * cos - result.getY() * sin,
                                              result.getX() * sin + result.getY() * cos);

        return rotatedOrigin.add(pivot);
    }

    /**
     * Computes the vector of the attractive force between two nodes.
     *
     * @param from Coordinates of the first node
     * @param to Coordinates of the second node
     * @param globalCount Global number of nodes
     * @param force Force factor to be used
     * @param scale Scale factor to be used
     *
     * @return Computed force vector
     */
    public static Point2D attractiveForce(final Point2D from,
                                          final Point2D to,
                                          final int globalCount,
                                          final double force,
                                          final double scale) {
        final var distance = from.distance(to);
        final var vec      = to.subtract(from).normalize();
        final var factor   = attractiveFunction(distance, globalCount, force, scale);

        return vec.multiply(factor);
    }

    /**
     * Computes the value of the scalar attractive force function based on the given
     * distance of a group of nodes.
     *
     * @param distance Distance between two nodes
     * @param globalCount Global number of nodes
     * @return Computed attractive force
     */
    static double attractiveFunction(double distance, final int globalCount, final double force, final double scale) {
        if (distance < 1) {
            distance = 1;
        }
        return force * Math.log(distance / scale) * 0.1;
    }

    /**
     * Computes the vector of the repelling force between two node.
     *
     * @param from Coordinates of the first node
     * @param to Coordinates of the second node
     * @param scale Scale factor to be used
     * @return Computed force vector
     */
    public static Point2D repellingForce(final Point2D from, final Point2D to, final double scale) {
        final var distance = from.distance(to);
        final var vec      = to.subtract(from).normalize();
        final var factor   = -repellingFunction(distance, scale);

        return vec.multiply(factor);
    }

    /**
     * Computes the value of the scalar repelling force function based on the given
     * distance of two nodes.
     *
     * @param distance Distance between two nodes
     * @return Computed repelling force
     */
    static double repellingFunction(double distance, final double scale) {
        if (distance < 1) {
            distance = 1;
        }
        return scale / (distance * distance);
    }

}
