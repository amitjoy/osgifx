/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.constants;

/**
 * This class defines constants used within the FX application.
 */
public final class FxConstants {

    /**
     * Prevents instantiation of this class.
     * Throws an IllegalAccessError if attempted.
     */
    private FxConstants() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /** Path to the root FXML file used in the application. */
    public static final String ROOT_FXML = "/fxml/tab-content.fxml";

    /** Path to the standard CSS file used in the application. */
    public static final String STANDARD_CSS = "/css/default.css";

}
