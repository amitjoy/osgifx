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

import com.osgifx.console.mcp.dto.McpLogEntryDTO;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class McpLogDTO {

    private final LongProperty   timestamp = new SimpleLongProperty();
    private final StringProperty type      = new SimpleStringProperty();
    private final StringProperty content   = new SimpleStringProperty();

    public McpLogDTO(final McpLogEntryDTO entry) {
        this.timestamp.set(entry.timestamp);
        this.type.set(entry.type.name());
        this.content.set(entry.content);
    }

    public LongProperty timestampProperty() {
        return timestamp;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty contentProperty() {
        return content;
    }

}
