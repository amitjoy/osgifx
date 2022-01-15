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
package com.osgifx.console.smartgraph.graph;

/**
 * Error when using an invalid vertex in calls of methods in {@link Graph}
 * and {@link Digraph} implementations.
 *
 * @see Graph
 * @see Digraph
 */
public class InvalidVertexException extends RuntimeException {

    private static final long serialVersionUID = 991130343030333770L;

    public InvalidVertexException() {
        super("The vertex is invalid or does not belong to this graph.");
    }

    public InvalidVertexException(final String string) {
        super(string);
    }

}
