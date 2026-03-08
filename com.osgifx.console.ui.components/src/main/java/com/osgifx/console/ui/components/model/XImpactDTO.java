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
package com.osgifx.console.ui.components.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class XImpactDTO {

    private final StringProperty affectedItem = new SimpleStringProperty();
    private final StringProperty impactType   = new SimpleStringProperty();
    private final StringProperty description  = new SimpleStringProperty();

    public XImpactDTO(final String affectedItem, final String impactType, final String description) {
        setAffectedItem(affectedItem);
        setImpactType(impactType);
        setDescription(description);
    }

    public String getAffectedItem() {
        return affectedItem.get();
    }

    public void setAffectedItem(final String affectedItem) {
        this.affectedItem.set(affectedItem);
    }

    public StringProperty affectedItemProperty() {
        return affectedItem;
    }

    public String getImpactType() {
        return impactType.get();
    }

    public void setImpactType(final String impactType) {
        this.impactType.set(impactType);
    }

    public StringProperty impactTypeProperty() {
        return impactType;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(final String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

}
