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

/**
 * A node to which a {@link SmartLabel} can be attached.
 */
public interface SmartLabelledNode {

    /**
     * Own and bind the <code>label</code> position to the desired position.
     *
     * @param label text label node
     */
    void attachLabel(SmartLabel label);

    /**
     * Returns the attached text label, if any.
     *
     * @return the text label reference or null if no label is attached
     */
    SmartLabel getAttachedLabel();

}
