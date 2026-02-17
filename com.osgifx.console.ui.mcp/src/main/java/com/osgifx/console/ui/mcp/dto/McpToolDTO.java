/*******************************************************************************
  * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.ui.mcp.dto;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class McpToolDTO {

    private final StringProperty name        = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty schema      = new SimpleStringProperty();

    public McpToolDTO(final String name, final String description, final String schema) {
        this.name.set(name);
        this.description.set(description);
        this.schema.set(schema);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty schemaProperty() {
        return schema;
    }

}
