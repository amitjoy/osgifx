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
package com.osgifx.console.smartgraph.graphview;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.FrameworkUtil;

import com.google.common.primitives.Doubles;

/**
 * Properties used by {@link SmartGraphPanel}. Default file is given by the
 * {@link #DEFAULT_FILE} property.
 *
 * @see SmartGraphPanel
 * @see SmartGraphVertex
 * @see SmartGraphEdge
 */
public class SmartGraphProperties {

    private static final boolean DEFAULT_VERTEX_ALLOW_USER_MOVE  = true;
    private static final String  PROPERTY_VERTEX_ALLOW_USER_MOVE = "vertex.allow-user-move";

    private static final double DEFAULT_VERTEX_RADIUS  = 15;
    private static final String PROPERTY_VERTEX_RADIUS = "vertex.radius";

    private static final boolean DEFAULT_VERTEX_USE_TOOLTIP  = true;
    private static final String  PROPERTY_VERTEX_USE_TOOLTIP = "vertex.tooltip";

    private static final boolean DEFAULT_VERTEX_USE_LABEL  = true;
    private static final String  PROPERTY_VERTEX_USE_LABEL = "vertex.label";

    private static final boolean DEFAULT_EDGE_USE_TOOLTIP  = true;
    private static final String  PROPERTY_EDGE_USE_TOOLTIP = "edge.tooltip";

    private static final boolean DEFAULT_EDGE_USE_LABEL  = true;
    private static final String  PROPERTY_EDGE_USE_LABEL = "edge.label";

    private static final boolean DEFAULT_EDGE_USE_ARROW  = true;
    private static final String  PROPERTY_EDGE_USE_ARROW = "edge.arrow";

    private static final int    DEFAULT_ARROW_SIZE  = 5;
    private static final String PROPERTY_ARROW_SIZE = "edge.arrowsize";

    private static final double DEFAULT_REPULSION_FORCE  = 25000;
    private static final String PROPERTY_REPULSION_FORCE = "layout.repulsive-force";

    private static final double DEFAULT_ATTRACTION_FORCE  = 30;
    private static final String PROPERTY_ATTRACTION_FORCE = "layout.attraction-force";

    private static final double DEFAULT_ATTRACTION_SCALE  = 10;
    private static final String PROPERTY_ATTRACTION_SCALE = "layout.attraction-scale";

    private static final String DEFAULT_FILE = "smartgraph.properties";
    private final Properties    properties;

    /**
     * Uses default properties file.
     */
    public SmartGraphProperties() {
        properties = new Properties();

        try {
            final var bundle = FrameworkUtil.getBundle(getClass());
            try (final var resource = bundle.getEntry(DEFAULT_FILE).openStream()) {
                properties.load(resource);
            }
        } catch (final IOException ex) {
            final var msg = String.format("The default %s was not found. Using default values.", DEFAULT_FILE);
            Logger.getLogger(SmartGraphProperties.class.getName()).log(Level.WARNING, msg);
        }
    }

    /**
     * Reads properties from the desired input stream.
     *
     * @param inputStream input stream from where to read the properties
     */
    public SmartGraphProperties(final InputStream inputStream) {
        properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (final IOException ex) {
            final var msg = "The file provided by the input stream does not exist. Using default values.";
            Logger.getLogger(SmartGraphProperties.class.getName()).log(Level.WARNING, msg);
        }
    }

    public SmartGraphProperties(final String content) {
        properties = new Properties();
        try {
            final InputStream targetStream = new ByteArrayInputStream(content.getBytes());
            properties.load(targetStream);
        } catch (final IOException ex) {
            final var msg = "The string contents could not be loaded. Using default values.";
            Logger.getLogger(SmartGraphProperties.class.getName()).log(Level.WARNING, msg);
        }
    }

    /**
     * Returns a property that indicates whether a vertex can be moved freely by the
     * user.
     *
     * @return corresponding property value
     */
    public boolean getVertexAllowUserMove() {
        return getBooleanProperty(PROPERTY_VERTEX_ALLOW_USER_MOVE, DEFAULT_VERTEX_ALLOW_USER_MOVE);
    }

    /**
     * Returns a property that indicates the radius of each vertex.
     *
     * @return corresponding property value
     */
    public double getVertexRadius() {
        return getDoubleProperty(PROPERTY_VERTEX_RADIUS, DEFAULT_VERTEX_RADIUS);
    }

    /**
     * Returns a property that indicates the repulsion force to use in the automatic
     * force-based layout.
     *
     * @return corresponding property value
     */
    public double getRepulsionForce() {
        return getDoubleProperty(PROPERTY_REPULSION_FORCE, DEFAULT_REPULSION_FORCE);
    }

    /**
     * Returns a property that indicates the attraction force to use in the
     * automatic force-based layout.
     *
     * @return corresponding property value
     */
    public double getAttractionForce() {
        return getDoubleProperty(PROPERTY_ATTRACTION_FORCE, DEFAULT_ATTRACTION_FORCE);
    }

    /**
     * Returns a property that indicates the attraction scale to use in the
     * automatic force-based layout.
     *
     * @return corresponding property value
     */
    public double getAttractionScale() {
        return getDoubleProperty(PROPERTY_ATTRACTION_SCALE, DEFAULT_ATTRACTION_SCALE);
    }

    /**
     * Returns a property that indicates whether a vertex has a tooltip installed.
     *
     * @return corresponding property value
     */
    public boolean getUseVertexTooltip() {
        return getBooleanProperty(PROPERTY_VERTEX_USE_TOOLTIP, DEFAULT_VERTEX_USE_TOOLTIP);
    }

    /**
     * Returns a property that indicates whether a vertex has a {@link SmartLabel}
     * attached to it.
     *
     * @return corresponding property value
     */
    public boolean getUseVertexLabel() {
        return getBooleanProperty(PROPERTY_VERTEX_USE_LABEL, DEFAULT_VERTEX_USE_LABEL);
    }

    /**
     * Returns a property that indicates whether an edge has a tooltip installed.
     *
     * @return corresponding property value
     */
    public boolean getUseEdgeTooltip() {
        return getBooleanProperty(PROPERTY_EDGE_USE_TOOLTIP, DEFAULT_EDGE_USE_TOOLTIP);
    }

    /**
     * Returns a property that indicates whether an edge has a {@link SmartLabel}
     * attached to it.
     *
     * @return corresponding property value
     */
    public boolean getUseEdgeLabel() {
        return getBooleanProperty(PROPERTY_EDGE_USE_LABEL, DEFAULT_EDGE_USE_LABEL);
    }

    /**
     * Returns a property that indicates whether a {@link SmartArrow} should be
     * attached to an edge.
     *
     * @return corresponding property value
     */
    public boolean getUseEdgeArrow() {
        return getBooleanProperty(PROPERTY_EDGE_USE_ARROW, DEFAULT_EDGE_USE_ARROW);
    }

    /**
     * Returns a property that indicates the size of the {@link SmartArrow}.
     *
     * @return corresponding property value
     */
    public double getEdgeArrowSize() {
        return getDoubleProperty(PROPERTY_ARROW_SIZE, DEFAULT_ARROW_SIZE);
    }

    private double getDoubleProperty(final String propertyName, final double defaultValue) {
        final var    property     = properties.getProperty(propertyName, Double.toString(defaultValue));
        final Double parsedDouble = Doubles.tryParse(property);
        return parsedDouble == null ? defaultValue : parsedDouble;
    }

    private boolean getBooleanProperty(final String propertyName, final boolean defaultValue) {
        final var p = properties.getProperty(propertyName, Boolean.toString(defaultValue));
        try {
            return Boolean.parseBoolean(p);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }
}
