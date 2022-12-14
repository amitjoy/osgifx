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
package com.osgifx.console.ui;

import org.osgi.annotation.versioning.ProviderType;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * This service is responsible for providing custom masker pane which can be
 * added to any {@link Pane}
 */
@ProviderType
public interface ConsoleMaskerPane {

    /**
     * Adds the current masker pane to the specified {@link Pane}
     *
     * @param pane the parent pane (cannot be {@code null})
     * @throws NullPointerException if the specified pane is {@code null}
     */
    void addTo(Pane pane);

    /**
     * Changes the visibility of the masker pane
     *
     * @param isVisible set the flag for visibility
     */
    void setVisible(boolean isVisible);

    /**
     * Returns if the masker pane is visible
     */
    boolean isVisible();

    /**
     * Returns the internal masker pane
     *
     * @return the masker pane
     */
    Node getMaskerPane();

}
