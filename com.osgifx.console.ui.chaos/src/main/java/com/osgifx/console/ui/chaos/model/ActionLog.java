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
package com.osgifx.console.ui.chaos.model;

public final class ActionLog {

    private final String time;
    private final String icon;
    private final String targetName;
    private final String state;

    public ActionLog(final String time, final String icon, final String targetName, final String state) {
        this.time       = time;
        this.icon       = icon;
        this.targetName = targetName;
        this.state      = state;
    }

    public String getTime() {
        return time;
    }

    public String getIcon() {
        return icon;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getState() {
        return state;
    }

}
