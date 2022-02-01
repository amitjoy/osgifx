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
package com.osgifx.console.ui;

import org.osgi.annotation.versioning.ProviderType;

import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * This service is responsible for providing custom status bar which can be added to any {@link Pane}
 */
@ProviderType
public interface ConsoleStatusBar {

    /**
     * Adds the current status bar to the specified {@link Pane}
     *
     * @param pane the parent pane (cannot be {@code null})
     * @throws NullPointerException if the specified pane is {@code null}
     */
    void addTo(BorderPane pane);

    /**
     * The property used to store the progress, a value between 0 and 1. A negative
     * value causes the progress bar to show an indeterminate state.
     *
     * @return the property used to store the progress of a task
     */
    DoubleProperty progressProperty();

}
